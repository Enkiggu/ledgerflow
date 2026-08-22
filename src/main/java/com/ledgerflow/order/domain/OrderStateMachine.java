package com.ledgerflow.order.domain;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<OrderStatus, Set<OrderStatus>> map = new EnumMap<>(OrderStatus.class);
        map.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED));
        map.put(OrderStatus.PAYMENT_PENDING, EnumSet.of(OrderStatus.PAID, OrderStatus.PAYMENT_FAILED, OrderStatus.CANCELLED));
        map.put(OrderStatus.PAYMENT_FAILED, EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED));
        map.put(OrderStatus.PAID, EnumSet.of(OrderStatus.REFUNDED));
        map.put(OrderStatus.CANCELLED, Collections.emptySet());
        map.put(OrderStatus.REFUNDED, Collections.emptySet());
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(map);
    }

    private OrderStateMachine() {}

    public static void validateTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        if (!allowed.contains(targetStatus)) {
            if (currentStatus == OrderStatus.PAID && targetStatus == OrderStatus.PAYMENT_PENDING) {
                throw new DomainException(ErrorCode.ORDER_ALREADY_PAID, "Order is already PAID and cannot transition to PAYMENT_PENDING");
            }
            if (currentStatus == OrderStatus.CANCELLED) {
                throw new DomainException(ErrorCode.ORDER_CANCELLED, "Order is CANCELLED and cannot be modified");
            }
            throw new DomainException(ErrorCode.INVALID_ORDER_STATE,
                    String.format("Invalid order state transition from %s to %s", currentStatus, targetStatus));
        }
    }
}
