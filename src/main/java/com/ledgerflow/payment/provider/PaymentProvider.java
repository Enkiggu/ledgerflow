package com.ledgerflow.payment.provider;

public interface PaymentProvider {
    ProviderChargeResponse charge(ProviderChargeRequest request);
}
