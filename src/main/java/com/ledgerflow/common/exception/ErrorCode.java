package com.ledgerflow.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 400 Bad Request
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "Invalid request argument"),
    INVALID_CURRENCY(HttpStatus.BAD_REQUEST, "Invalid currency format"),
    CURRENCY_MISMATCH(HttpStatus.BAD_REQUEST, "Currency mismatch across transaction items"),
    INVALID_ORDER_STATE(HttpStatus.BAD_REQUEST, "Invalid order state transition"),
    INVALID_PAYMENT_STATE(HttpStatus.BAD_REQUEST, "Invalid payment state transition"),
    EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST, "Order must contain at least one item"),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "Insufficient stock for requested product"),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.BAD_REQUEST, "Invalid or missing webhook signature"),
    REFUND_EXCEEDS_PAYMENT(HttpStatus.BAD_REQUEST, "Refund amount exceeds original payment amount"),

    // 401 / 403 Security
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),

    // 404 Not Found
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "Customer not found"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Order not found"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Payment not found"),
    LEDGER_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Ledger transaction not found"),

    // 409 Conflict
    ORDER_ALREADY_PAID(HttpStatus.CONFLICT, "The order has already been paid"),
    ORDER_CANCELLED(HttpStatus.CONFLICT, "The order has been cancelled"),
    PAYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "Payment already exists for this order"),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.CONFLICT, "Idempotency key reused with different request payload"),
    IDEMPOTENCY_KEY_IN_PROGRESS(HttpStatus.CONFLICT, "A request with this idempotency key is currently processing"),
    CONCURRENT_MUTATION_CONFLICT(HttpStatus.CONFLICT, "Concurrent modification detected. Please retry."),
    DUPLICATE_WEBHOOK_EVENT(HttpStatus.CONFLICT, "Webhook event has already been processed"),

    // 422 Unprocessable Entity
    LEDGER_IMBALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "Ledger transaction is unbalanced: debits must equal credits"),
    PAYMENT_DECLINED(HttpStatus.UNPROCESSABLE_ENTITY, "Payment declined by external provider"),

    // 429 Too Many Requests
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please back off."),

    // 500 / 502 / 504 Provider & Internal
    PAYMENT_PROVIDER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "Payment provider request timed out"),
    PAYMENT_PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Payment provider temporarily unavailable"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
