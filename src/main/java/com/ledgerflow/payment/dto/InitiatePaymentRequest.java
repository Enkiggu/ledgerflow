package com.ledgerflow.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InitiatePaymentRequest(
        @NotBlank(message = "orderId must not be blank")
        @Schema(description = "Order UUID to pay", example = "o0000001-0000-0000-0000-000000000001")
        String orderId,

        @NotNull(message = "amount must not be null")
        @Min(value = 1, message = "amount must be positive in minor units (cents)")
        @Schema(description = "Payment amount in minor currency units (cents)", example = "4999")
        Long amount,

        @NotBlank(message = "currency must not be blank")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a valid 3-letter ISO-4217 uppercase code")
        @Schema(description = "ISO-4217 currency code", example = "EUR")
        String currency,

        @Schema(description = "Simulated provider outcome for testing (SUCCESS, DECLINED, TIMEOUT, SYSTEM_ERROR)", example = "SUCCESS")
        String simulatedOutcome
) {}
