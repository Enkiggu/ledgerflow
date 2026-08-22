package com.ledgerflow.payment.dto;

import com.ledgerflow.payment.domain.Payment;
import com.ledgerflow.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.List;

public record PaymentResponse(
        String id,
        String orderId,
        long amountCents,
        String currency,
        PaymentStatus status,
        String provider,
        String providerReference,
        String failureReason,
        Long version,
        List<PaymentAttemptResponse> attempts,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getProviderReference(),
                payment.getFailureReason(),
                payment.getVersion(),
                payment.getAttempts() != null
                        ? payment.getAttempts().stream().map(PaymentAttemptResponse::from).toList()
                        : List.of(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
