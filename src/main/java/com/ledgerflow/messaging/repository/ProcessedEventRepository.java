package com.ledgerflow.messaging.repository;

import com.ledgerflow.messaging.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    Optional<ProcessedEvent> findByEventIdAndConsumerName(String eventId, String consumerName);
    boolean existsByEventIdAndConsumerName(String eventId, String consumerName);
}
