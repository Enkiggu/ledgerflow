package com.ledgerflow.admin.controller;

import com.ledgerflow.common.model.ApiResponse;
import com.ledgerflow.messaging.consumer.DlqConsumer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dlq")
@Tag(name = "Admin - DLQ", description = "Dead-Letter Queue message inspection and management")
@SecurityRequirement(name = "ApiKeyAuth")
public class AdminDlqController {

    private final DlqConsumer dlqConsumer;

    public AdminDlqController(DlqConsumer dlqConsumer) {
        this.dlqConsumer = dlqConsumer;
    }

    @GetMapping
    @Operation(summary = "Get recent dead-lettered poison messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDlqMessages() {
        List<Map<String, Object>> messages = dlqConsumer.getDlqMessages();
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    @PostMapping("/clear")
    @Operation(summary = "Clear the DLQ message inspection log")
    public ResponseEntity<ApiResponse<String>> clearDlq() {
        dlqConsumer.clearDlqMessages();
        return ResponseEntity.ok(ApiResponse.ok("DLQ history cleared"));
    }
}
