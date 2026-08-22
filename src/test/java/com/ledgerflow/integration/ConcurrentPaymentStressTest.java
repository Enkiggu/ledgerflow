package com.ledgerflow.integration;

import com.ledgerflow.order.domain.Order;
import com.ledgerflow.order.domain.OrderStatus;
import com.ledgerflow.payment.dto.InitiatePaymentRequest;
import com.ledgerflow.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Concurrent Payment Stress & Race Condition Invariant Tests")
class ConcurrentPaymentStressTest extends BaseIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    @DisplayName("Invariant 2: 10 concurrent threads attempting to charge the same order -> Exactly 1 succeeds, 0 double-charges")
    void concurrentPaymentAttemptsMustSucceedExactlyOnce() throws InterruptedException {
        Order order = orderRepository.save(Order.builder()
                .id(UUID.randomUUID().toString())
                .customerId(testCustomer.getId())
                .currency("EUR")
                .totalAmountCents(10000L)
                .status(OrderStatus.CREATED)
                .version(0L)
                .build());

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await(); // Synchronize all threads to execute at the exact same instant
                    InitiatePaymentRequest request = new InitiatePaymentRequest(
                            order.getId(),
                            10000L,
                            "EUR",
                            "SUCCESS"
                    );
                    paymentService.processPayment(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        // Release latch to start all threads concurrently
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verification of Invariant 2
        assertEquals(1, successCount.get(), "Invariant 2: Exactly one payment must succeed across concurrent attempts");
        assertEquals(threadCount - 1, failureCount.get(), "All other concurrent payment attempts must be rejected");

        // Verify Database Invariants
        assertEquals(1, paymentRepository.findByOrderId(order.getId()).size(), "Only one payment record in DB");
        assertEquals(1, ledgerTransactionRepository.count(), "Only one ledger transaction in DB");
        assertEquals(OrderStatus.PAID, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }
}
