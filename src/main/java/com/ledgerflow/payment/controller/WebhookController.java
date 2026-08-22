package com.ledgerflow.payment.controller;

import com.ledgerflow.common.model.ApiResponse;
import com.ledgerflow.payment.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@Tag(name = "Webhooks", description = "Provider webhook listener with HMAC signature verification and replay protection")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/payment-provider")
    @Operation(summary = "Receive payment gateway webhook notifications", description = "Requires X-Signature header matching HMAC-SHA256 of the raw body payload.")
    public ResponseEntity<ApiResponse<String>> handleProviderWebhook(
            @Parameter(in = ParameterIn.HEADER, name = "X-Signature", required = true, description = "HMAC-SHA256 signature")
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String rawPayload) {

        webhookService.processWebhook(rawPayload, signature, "MOCK_GATEWAY");
        return ResponseEntity.ok(ApiResponse.ok("Webhook received and verified"));
    }
}
