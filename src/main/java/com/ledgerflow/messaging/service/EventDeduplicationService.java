package com.ledgerflow.messaging.service;

import com.ledgerflow.messaging.domain.ProcessedEvent;
import com.ledgerflow.messaging.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EventDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(EventDeduplicationService.class);
    private final ProcessedEventRepository processedEventRepository;

    public EventDeduplicationService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String eventId, String consumerName) {
        return processedEventRepository.existsByEventIdAndConsumerName(eventId, consumerName);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessed(String eventId, String consumerName, String eventType) {
        try {
            ProcessedEvent event = ProcessedEvent.builder()
                    .id(UUID.randomUUID().toString())
                    .eventId(eventId)
                    .consumerName(consumerName)
                    .eventType(eventType)
                    .build();

            processedEventRepository.saveAndFlush(event);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("Event {} has already been processed by consumer {}", eventId, consumerName);
            return false;
        }
    }
}
