package com.ledgerflow.payment.provider;

public record ProviderChargeResponse(
        ProviderStatus status,
        String providerReference,
        String errorCode,
        String errorMessage,
        long latencyMs
) {
    public static ProviderChargeResponse success(String providerReference, long latencyMs) {
        return new ProviderChargeResponse(ProviderStatus.SUCCESS, providerReference, null, null, latencyMs);
    }

    public static ProviderChargeResponse declined(String reason, long latencyMs) {
        return new ProviderChargeResponse(ProviderStatus.DECLINED, null, "CARD_DECLINED", reason, latencyMs);
    }

    public static ProviderChargeResponse error(ProviderStatus status, String errorCode, String errorMessage, long latencyMs) {
        return new ProviderChargeResponse(status, null, errorCode, errorMessage, latencyMs);
    }
}
