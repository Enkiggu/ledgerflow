package com.ledgerflow.integration;

import com.ledgerflow.ledger.domain.AccountType;
import com.ledgerflow.ledger.domain.EntryType;
import com.ledgerflow.ledger.domain.LedgerTransaction;
import com.ledgerflow.ledger.domain.ReferenceType;
import com.ledgerflow.order.domain.OrderStatus;
import com.ledgerflow.order.dto.CreateOrderRequest;
import com.ledgerflow.order.dto.OrderItemRequest;
import com.ledgerflow.outbox.domain.OutboxEvent;
import com.ledgerflow.payment.domain.PaymentStatus;
import com.ledgerflow.payment.dto.InitiatePaymentRequest;
import com.ledgerflow.payment.dto.RefundRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("End-to-End Order, Payment, and Ledger Lifecycle Integration Test")
class OrderPaymentE2ETest extends BaseIntegrationTest {

    @Test
    @DisplayName("Complete Happy Path: Order Creation -> Payment Charge -> Balanced Ledger -> Refund")
    void completeOrderPaymentAndLedgerFlow() throws Exception {
        // Step 1: Create Order
        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                testCustomer.getId(),
                "EUR",
                List.of(new OrderItemRequest(testProduct.getId(), 2, 4999L))
        );

        MvcResult orderResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.totalAmountCents").value(9998))
                .andExpect(jsonPath("$.data.currency").value("EUR"))
                .andReturn();

        String orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asText();
        assertNotNull(orderId);

        // Step 2: Pay Order
        InitiatePaymentRequest paymentRequest = new InitiatePaymentRequest(
                orderId,
                9998L,
                "EUR",
                "SUCCESS"
        );

        MvcResult paymentResult = mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "pay-test-key-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.amountCents").value(9998))
                .andExpect(jsonPath("$.data.providerReference").isNotEmpty())
                .andReturn();

        String paymentId = objectMapper.readTree(paymentResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Step 3: Verify Order Aggregate is updated to PAID
        assertEquals(OrderStatus.PAID, orderRepository.findById(orderId).orElseThrow().getStatus());

        // Step 4: Invariants 3 & 4: Verify Immutable Balanced Ledger Transaction
        LedgerTransaction ledgerTx = ledgerTransactionRepository
                .findByReferenceTypeAndReferenceId(ReferenceType.PAYMENT, paymentId)
                .orElseThrow(() -> new AssertionError("Ledger transaction must exist for succeeded payment"));

        assertEquals(2, ledgerTx.getEntries().size());
        long debitTotal = ledgerTx.getEntries().stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .mapToLong(e -> e.getAmountCents())
                .sum();
        long creditTotal = ledgerTx.getEntries().stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .mapToLong(e -> e.getAmountCents())
                .sum();

        assertEquals(9998L, debitTotal);
        assertEquals(9998L, creditTotal);
        assertEquals(debitTotal, creditTotal, "Invariant 4: Debits must equal credits");

        // Step 5: Invariant 6: Verify Outbox Events
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertFalse(outboxEvents.isEmpty());
        assertTrue(outboxEvents.stream().anyMatch(e -> "OrderPaid".equals(e.getEventType())));
        assertTrue(outboxEvents.stream().anyMatch(e -> "PaymentSucceeded".equals(e.getEventType())));

        // Step 6: Process Partial Refund
        RefundRequest refundRequest = new RefundRequest(4999L, "EUR", "Customer returned 1 item");
        mockMvc.perform(post("/api/payments/{paymentId}/refunds", paymentId)
                        .header("Idempotency-Key", "refund-key-2001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amountCents").value(4999))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Step 7: Verify Refund Ledger Transaction
        assertEquals(2, ledgerTransactionRepository.count());
    }
}
