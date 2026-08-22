package com.ledgerflow.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "customerId must not be blank")
        @Schema(description = "Customer UUID", example = "c0000001-0000-0000-0000-000000000001")
        String customerId,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a valid 3-letter uppercase ISO-4217 code")
        @Schema(description = "ISO-4217 3-letter currency code", example = "EUR")
        String currency,

        @NotEmpty(message = "items list must not be empty")
        @Valid
        @Schema(description = "List of items in the order")
        List<OrderItemRequest> items
) {}
