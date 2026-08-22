package com.ledgerflow.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.common.model.ApiResponse;
import com.ledgerflow.common.model.PageResponse;
import com.ledgerflow.idempotency.dto.IdempotencyResult;
import com.ledgerflow.idempotency.service.IdempotencyService;
import com.ledgerflow.idempotency.service.RequestHasher;
import com.ledgerflow.payment.domain.PaymentStatus;
import com.ledgerflow.payment.dto.InitiatePaymentRequest;
import com.ledgerflow.payment.dto.PaymentAttemptResponse;
import com.ledgerflow.payment.dto.PaymentResponse;
import com.ledgerflow.payment.repository.PaymentAttemptRepository;
import com.ledgerflow.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment authorization, capture, and inspection endpoints")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentAttemptRepository attemptRepository;
    private final IdempotencyService idempotencyService;
    private final RequestHasher requestHasher;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentService paymentService,
                             PaymentAttemptRepository attemptRepository,
                             IdempotencyService idempotencyService,
                             RequestHasher requestHasher,
                             ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.attemptRepository = attemptRepository;
        this.idempotencyService = idempotencyService;
        this.requestHasher = requestHasher;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Initiate and process a payment", description = "Atomic execution with double-entry ledger settlement, outbox publication, and Idempotency-Key support.")
    public ResponseEntity<?> initiatePayment(
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", description = "Unique client transaction idempotency key")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {

        String requestHash = requestHasher.computeSha256(request.toString());
        IdempotencyResult idempResult = idempotencyService.startOrCheck(idempotencyKey, requestHash, "PAYMENT");

        if (!idempResult.shouldProceed()) {
            return ResponseEntity.status(idempResult.record().getResponseStatus())
                    .header("X-Cache-Replay", "true")
                    .body(idempResult.record().getResponseBody());
        }

        try {
            PaymentResponse response = paymentService.processPayment(request);
            try {
                String json = objectMapper.writeValueAsString(ApiResponse.ok(response, "Payment completed successfully"));
                idempotencyService.recordSuccess(idempotencyKey, response.id(), HttpStatus.CREATED.value(), json);
            } catch (Exception e) {
                // Ignore serialization error for idempotency cache logging
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Payment completed successfully"));
        } catch (RuntimeException ex) {
            idempotencyService.recordFailure(idempotencyKey);
            throw ex;
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment details by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable String id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List and filter payments with pagination")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String currency,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<PaymentResponse> page = PageResponse.from(paymentService.getPayments(status, currency, pageable));
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{id}/attempts")
    @Operation(summary = "Get payment provider gateway attempt history for a payment")
    public ResponseEntity<ApiResponse<List<PaymentAttemptResponse>>> getPaymentAttempts(@PathVariable String id) {
        List<PaymentAttemptResponse> attempts = attemptRepository.findByPaymentIdOrderByAttemptNumberAsc(id)
                .stream()
                .map(PaymentAttemptResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(attempts));
    }
}
