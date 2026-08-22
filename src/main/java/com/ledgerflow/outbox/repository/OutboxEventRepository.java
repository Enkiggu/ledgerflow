package com.ledgerflow.outbox.repository;

import com.ledgerflow.outbox.domain.OutboxEvent;
import com.ledgerflow.outbox.domain.OutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query(value = """
        SELECT * FROM outbox_events
        WHERE status IN ('PENDING', 'FAILED')
          AND next_attempt_at <= :now
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> findPendingEventsForPublishingWithSkipLocked(
            @Param("now") Instant now,
            @Param("limit") int limit
    );

    @Query("""
        SELECT e FROM OutboxEvent e
        WHERE (e.status = com.ledgerflow.outbox.domain.OutboxStatus.PENDING OR e.status = com.ledgerflow.outbox.domain.OutboxStatus.FAILED)
          AND e.nextAttemptAt <= :now
        ORDER BY e.createdAt ASC
        """)
    List<OutboxEvent> findPendingEventsFallback(
            @Param("now") Instant now,
            Pageable pageable
    );

    Page<OutboxEvent> findByStatus(OutboxStatus status, Pageable pageable);

    long countByStatus(OutboxStatus status);
}
