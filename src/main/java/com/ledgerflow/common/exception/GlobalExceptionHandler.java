package com.ledgerflow.common.exception;

import com.ledgerflow.common.filter.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("Domain exception occurred [traceId: {}]: {} - {}", traceId, errorCode.name(), ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                errorCode.getHttpStatus().value(),
                errorCode.name(),
                ex.getMessage(),
                traceId
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Validation failed [traceId: {}]: {}", traceId, errors);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_ARGUMENT.name(),
                "Request validation failed",
                traceId,
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        log.warn("Optimistic lock conflict [traceId: {}]: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                ErrorCode.CONCURRENT_MUTATION_CONFLICT.name(),
                "The resource was modified concurrently by another transaction. Please retry.",
                traceId
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        log.warn("Unreadable HTTP message [traceId: {}]: {}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorCode.INVALID_ARGUMENT.name(),
                "Malformed JSON request body",
                traceId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        String traceId = getTraceId();
        log.error("Unhandled server exception [traceId: {}]: ", traceId, ex);

        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_SERVER_ERROR.name(),
                "An unexpected internal error occurred. Please contact support.",
                traceId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : "N/A";
    }
}
