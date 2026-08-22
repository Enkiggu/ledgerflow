package com.ledgerflow.payment.dto;

import com.ledgerflow.payment.domain.Refund;

import java.time.Instant;

public record RefundResponse(
        String id,
        String paymentId,
        long amountCents,
        String currency,
        String reason,
        String status,
        Instant createdAt
) {
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getAmountCents(),
                refund.getCurrency(),
                refund.getReason(),
                refund.getStatus(),
                refund.getCreatedAt()
        );
    }
}
