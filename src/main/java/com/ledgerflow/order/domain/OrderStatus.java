package com.ledgerflow.order.domain;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    CANCELLED,
    REFUNDED
}
