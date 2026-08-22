package com.ledgerflow.outbox.controller;

import com.ledgerflow.common.model.ApiResponse;
import com.ledgerflow.common.model.PageResponse;
import com.ledgerflow.outbox.domain.OutboxStatus;
import com.ledgerflow.outbox.dto.OutboxEventResponse;
import com.ledgerflow.outbox.repository.OutboxEventRepository;
import com.ledgerflow.outbox.service.OutboxPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/outbox")
@Tag(name = "Admin - Outbox", description = "Inspection and manual triggering for Transactional Outbox")
@SecurityRequirement(name = "ApiKeyAuth")
public class AdminOutboxController {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublisher outboxPublisher;

    public AdminOutboxController(OutboxEventRepository outboxEventRepository, OutboxPublisher outboxPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxPublisher = outboxPublisher;
    }

    @GetMapping
    @Operation(summary = "List outbox events with pagination and status filter")
    public ResponseEntity<ApiResponse<PageResponse<OutboxEventResponse>>> getOutboxEvents(
            @RequestParam(required = false) OutboxStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<OutboxEventResponse> page;
        if (status != null) {
            page = PageResponse.from(outboxEventRepository.findByStatus(status, pageable).map(OutboxEventResponse::from));
        } else {
            page = PageResponse.from(outboxEventRepository.findAll(pageable).map(OutboxEventResponse::from));
        }
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @PostMapping("/publish-now")
    @Operation(summary = "Trigger immediate publishing run for pending outbox events")
    public ResponseEntity<ApiResponse<String>> triggerPublishing() {
        outboxPublisher.publishPendingEvents();
        return ResponseEntity.ok(ApiResponse.ok("Outbox publishing triggered successfully"));
    }
}
