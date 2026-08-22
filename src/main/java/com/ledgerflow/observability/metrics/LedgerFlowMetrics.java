package com.ledgerflow.observability.metrics;

import com.ledgerflow.outbox.domain.OutboxStatus;
import com.ledgerflow.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class LedgerFlowMetrics {

    public LedgerFlowMetrics(MeterRegistry meterRegistry, OutboxEventRepository outboxEventRepository) {
        Gauge.builder("outbox.pending.events", outboxEventRepository,
                repo -> repo.countByStatus(OutboxStatus.PENDING))
                .description("Number of pending transactional outbox events waiting to be published")
                .register(meterRegistry);

        Gauge.builder("outbox.failed.events", outboxEventRepository,
                repo -> repo.countByStatus(OutboxStatus.FAILED))
                .description("Number of permanently failed outbox events")
                .register(meterRegistry);
    }
}
