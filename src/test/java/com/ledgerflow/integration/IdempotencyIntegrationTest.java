package com.ledgerflow.integration;

import com.ledgerflow.order.domain.Order;
import com.ledgerflow.order.domain.OrderStatus;
import com.ledgerflow.payment.dto.InitiatePaymentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Idempotency Replay and Conflict Detection Integration Tests")
class IdempotencyIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Invariant 1: Identical request retried with same Idempotency-Key returns cached response without duplicate execution")
    void shouldReplayCachedResponseOnDuplicateKey() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .id(UUID.randomUUID().toString())
                .customerId(testCustomer.getId())
                .currency("EUR")
                .totalAmountCents(4999L)
                .status(OrderStatus.CREATED)
                .version(0L)
                .build());

        InitiatePaymentRequest request = new InitiatePaymentRequest(
                order.getId(),
                4999L,
                "EUR",
                "SUCCESS"
        );

        String idempotencyKey = "idemp-key-" + UUID.randomUUID();

        // 1. Initial Request
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(header().doesNotExist("X-Cache-Replay"));

        assertEquals(1, paymentRepository.count());
        assertEquals(1, ledgerTransactionRepository.count());

        // 2. Exact Duplicate Request (Replay)
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Cache-Replay", "true"));

        // Verify Invariant: Still exactly 1 payment and 1 ledger transaction
        assertEquals(1, paymentRepository.count(), "Must not create duplicate payment");
        assertEquals(1, ledgerTransactionRepository.count(), "Must not create duplicate ledger entry");
    }

    @Test
    @DisplayName("Invariant 1: Reusing same Idempotency-Key with different payload must return 409 Conflict")
    void shouldRejectKeyReuseWithDifferentPayload() throws Exception {
        Order order1 = orderRepository.save(Order.builder()
                .id(UUID.randomUUID().toString())
                .customerId(testCustomer.getId())
                .currency("EUR")
                .totalAmountCents(4999L)
                .status(OrderStatus.CREATED)
                .version(0L)
                .build());

        Order order2 = orderRepository.save(Order.builder()
                .id(UUID.randomUUID().toString())
                .customerId(testCustomer.getId())
                .currency("EUR")
                .totalAmountCents(4999L)
                .status(OrderStatus.CREATED)
                .version(0L)
                .build());

        String sharedKey = "shared-conflict-key-999";

        InitiatePaymentRequest req1 = new InitiatePaymentRequest(order1.getId(), 4999L, "EUR", "SUCCESS");
        InitiatePaymentRequest req2 = new InitiatePaymentRequest(order2.getId(), 4999L, "EUR", "SUCCESS");

        // First request succeeds
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", sharedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Second request with DIFFERENT payload but SAME key must be rejected with 409 Conflict
        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", sharedKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }
}
