package com.ledgerflow.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.common.model.ApiResponse;
import com.ledgerflow.common.model.PageResponse;
import com.ledgerflow.idempotency.dto.IdempotencyResult;
import com.ledgerflow.idempotency.service.IdempotencyService;
import com.ledgerflow.idempotency.service.RequestHasher;
import com.ledgerflow.order.domain.OrderStatus;
import com.ledgerflow.order.dto.CreateOrderRequest;
import com.ledgerflow.order.dto.OrderResponse;
import com.ledgerflow.order.service.OrderService;
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

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order lifecycle management and queries")
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final RequestHasher requestHasher;
    private final ObjectMapper objectMapper;

    public OrderController(OrderService orderService,
                           IdempotencyService idempotencyService,
                           RequestHasher requestHasher,
                           ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.idempotencyService = idempotencyService;
        this.requestHasher = requestHasher;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Supports Idempotency-Key header for duplicate request protection.")
    public ResponseEntity<?> createOrder(
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", description = "Unique client request idempotency key")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        String requestHash = requestHasher.computeSha256(request.toString());
        IdempotencyResult idempResult = idempotencyService.startOrCheck(idempotencyKey, requestHash, "ORDER");

        if (!idempResult.shouldProceed()) {
            return ResponseEntity.status(idempResult.record().getResponseStatus())
                    .header("X-Cache-Replay", "true")
                    .body(idempResult.record().getResponseBody());
        }

        try {
            OrderResponse response = orderService.createOrder(request);
            try {
                String json = objectMapper.writeValueAsString(ApiResponse.ok(response, "Order created successfully"));
                idempotencyService.recordSuccess(idempotencyKey, response.id(), HttpStatus.CREATED.value(), json);
            } catch (Exception e) {
                // Ignore serialization error for idempotency cache logging
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Order created successfully"));
        } catch (RuntimeException ex) {
            idempotencyService.recordFailure(idempotencyKey);
            throw ex;
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable String id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List and filter orders with server-side pagination")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<OrderResponse> page = PageResponse.from(orderService.getOrders(customerId, status, pageable));
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable String id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Order cancelled successfully"));
    }
}
