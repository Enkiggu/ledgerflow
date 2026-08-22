package com.ledgerflow.payment.domain;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<PaymentStatus, Set<PaymentStatus>> map = new EnumMap<>(PaymentStatus.class);
        map.put(PaymentStatus.CREATED, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.FAILED, PaymentStatus.CANCELLED));
        map.put(PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED, PaymentStatus.CANCELLED));
        map.put(PaymentStatus.SUCCEEDED, EnumSet.of(PaymentStatus.REFUNDED));
        map.put(PaymentStatus.FAILED, Collections.emptySet());
        map.put(PaymentStatus.CANCELLED, Collections.emptySet());
        map.put(PaymentStatus.REFUNDED, Collections.emptySet());
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private PaymentStateMachine() {}

    public static void validateTransition(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        Set<PaymentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        if (!allowed.contains(targetStatus)) {
            throw new DomainException(ErrorCode.INVALID_PAYMENT_STATE,
                    String.format("Invalid payment state transition from %s to %s", currentStatus, targetStatus));
        }
    }
}
