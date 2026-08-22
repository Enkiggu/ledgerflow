package com.ledgerflow.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String traceId,
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(int status, String code, String message, String traceId) {
        return new ErrorResponse(Instant.now(), status, code, message, traceId, null);
    }

    public static ErrorResponse of(int status, String code, String message, String traceId, Map<String, String> validationErrors) {
        return new ErrorResponse(Instant.now(), status, code, message, traceId, validationErrors);
    }
}
