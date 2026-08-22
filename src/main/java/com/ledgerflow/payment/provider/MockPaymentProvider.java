package com.ledgerflow.payment.provider;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class MockPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProvider.class);
    private final Timer providerTimer;

    @Value("${ledgerflow.payment.provider.timeout-ms:3000}")
    private long configuredTimeoutMs;

    public MockPaymentProvider(MeterRegistry meterRegistry) {
        this.providerTimer = meterRegistry.timer("payment.provider.latency");
    }

    @Override
    public ProviderChargeResponse charge(ProviderChargeRequest request) {
        long startTime = System.currentTimeMillis();

        return providerTimer.record(() -> {
            log.info("Executing external provider charge [paymentId: {}, amount: {} {}, simulatedOutcome: {}]",
                    request.paymentId(), request.amountCents(), request.currency(), request.simulatedOutcome());

            String outcome = request.simulatedOutcome() != null ? request.simulatedOutcome().toUpperCase() : "SUCCESS";

            // Simulated chaos scenarios:
            switch (outcome) {
                case "TIMEOUT" -> {
                    sleepSilently(configuredTimeoutMs + 500);
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Payment provider call timed out after {} ms", duration);
                    throw new DomainException(ErrorCode.PAYMENT_PROVIDER_TIMEOUT,
                            String.format("Payment provider timed out after %d ms", duration));
                }
                case "SYSTEM_ERROR", "500" -> {
                    sleepSilently(100);
                    long duration = System.currentTimeMillis() - startTime;
                    log.error("Payment provider returned 500 Internal Server Error");
                    throw new DomainException(ErrorCode.PAYMENT_PROVIDER_UNAVAILABLE,
                            "External payment gateway returned 500 Service Unavailable");
                }
                case "DECLINED" -> {
                    sleepSilently(80);
                    long duration = System.currentTimeMillis() - startTime;
                    log.warn("Payment was declined by issuing bank (insufficient funds)");
                    return ProviderChargeResponse.declined("Declined by issuing bank: Insufficient funds", duration);
                }
                case "EXPIRED_CARD" -> {
                    sleepSilently(50);
                    long duration = System.currentTimeMillis() - startTime;
                    return ProviderChargeResponse.declined("Declined: Card has expired", duration);
                }
                default -> {
                    // Standard realistic simulated network latency (30-80ms)
                    sleepSilently(50);
                    long duration = System.currentTimeMillis() - startTime;
                    String providerRef = "ch_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                    log.info("Payment provider charge succeeded with reference: {} in {}ms", providerRef, duration);
                    return ProviderChargeResponse.success(providerRef, duration);
                }
            }
        });
    }

    private void sleepSilently(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
