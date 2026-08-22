package com.ledgerflow.idempotency.dto;

import com.ledgerflow.idempotency.domain.IdempotencyRecord;

public record IdempotencyResult(
        boolean shouldProceed,
        IdempotencyRecord record
) {
    public static IdempotencyResult proceed(IdempotencyRecord record) {
        return new IdempotencyResult(true, record);
    }

    public static IdempotencyResult cached(IdempotencyRecord record) {
        return new IdempotencyResult(false, record);
    }
}
