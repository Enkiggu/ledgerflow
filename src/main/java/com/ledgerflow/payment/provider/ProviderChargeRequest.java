package com.ledgerflow.payment.provider;

public record ProviderChargeRequest(
        String paymentId,
        String orderId,
        long amountCents,
        String currency,
        String customerId,
        String simulatedOutcome
) {}
