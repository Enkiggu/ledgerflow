package com.ledgerflow.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotBlank(message = "productId must not be blank")
        @Schema(description = "Product UUID", example = "p0000001-0000-0000-0000-000000000001")
        String productId,

        @NotNull(message = "quantity must not be null")
        @Min(value = 1, message = "quantity must be at least 1")
        @Schema(description = "Quantity of product", example = "2")
        Integer quantity,

        @NotNull(message = "unitPrice must not be null")
        @Min(value = 1, message = "unitPrice must be positive in minor units (cents)")
        @Schema(description = "Unit price in minor units (cents)", example = "4999")
        Long unitPrice
) {}
