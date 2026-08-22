package com.ledgerflow.payment.dto;

import com.ledgerflow.payment.domain.PaymentAttempt;

import java.time.Instant;

public record PaymentAttemptResponse(
        String id,
        int attemptNumber,
        String provider,
        String status,
        String errorMessage,
        Long durationMs,
        Instant createdAt
) {
    public static PaymentAttemptResponse from(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getProvider(),
                attempt.getStatus(),
                attempt.getErrorMessage(),
                attempt.getDurationMs(),
                attempt.getCreatedAt()
        );
    }
}
