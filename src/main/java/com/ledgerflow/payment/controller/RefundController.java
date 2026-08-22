package com.ledgerflow.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.common.model.ApiResponse;
import com.ledgerflow.idempotency.dto.IdempotencyResult;
import com.ledgerflow.idempotency.service.IdempotencyService;
import com.ledgerflow.idempotency.service.RequestHasher;
import com.ledgerflow.payment.dto.RefundRequest;
import com.ledgerflow.payment.dto.RefundResponse;
import com.ledgerflow.payment.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments/{paymentId}/refunds")
@Tag(name = "Refunds", description = "Refund processing and compensating ledger transactions")
public class RefundController {

    private final RefundService refundService;
    private final IdempotencyService idempotencyService;
    private final RequestHasher requestHasher;
    private final ObjectMapper objectMapper;

    public RefundController(RefundService refundService,
                            IdempotencyService idempotencyService,
                            RequestHasher requestHasher,
                            ObjectMapper objectMapper) {
        this.refundService = refundService;
        this.idempotencyService = idempotencyService;
        this.requestHasher = requestHasher;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Process a full or partial refund", description = "Supports Idempotency-Key header and creates compensating double-entry ledger entries.")
    public ResponseEntity<?> processRefund(
            @PathVariable String paymentId,
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", description = "Unique refund idempotency key")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RefundRequest request) {

        String requestHash = requestHasher.computeSha256(paymentId + ":" + request.toString());
        IdempotencyResult idempResult = idempotencyService.startOrCheck(idempotencyKey, requestHash, "REFUND");

        if (!idempResult.shouldProceed()) {
            return ResponseEntity.status(idempResult.record().getResponseStatus())
                    .header("X-Cache-Replay", "true")
                    .body(idempResult.record().getResponseBody());
        }

        try {
            RefundResponse response = refundService.processRefund(paymentId, request);
            try {
                String json = objectMapper.writeValueAsString(ApiResponse.ok(response, "Refund processed successfully"));
                idempotencyService.recordSuccess(idempotencyKey, response.id(), HttpStatus.CREATED.value(), json);
            } catch (Exception e) {
                // Ignore serialization error for idempotency cache logging
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Refund processed successfully"));
        } catch (RuntimeException ex) {
            idempotencyService.recordFailure(idempotencyKey);
            throw ex;
        }
    }

    @GetMapping
    @Operation(summary = "Get all refunds for a payment")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefunds(@PathVariable String paymentId) {
        List<RefundResponse> refunds = refundService.getRefundsForPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.ok(refunds));
    }
}
