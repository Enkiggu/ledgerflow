package com.ledgerflow.idempotency.service;

import com.ledgerflow.common.exception.DomainException;
import com.ledgerflow.common.exception.ErrorCode;
import com.ledgerflow.idempotency.domain.IdempotencyRecord;
import com.ledgerflow.idempotency.domain.IdempotencyStatus;
import com.ledgerflow.idempotency.dto.IdempotencyResult;
import com.ledgerflow.idempotency.repository.IdempotencyRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private final IdempotencyRecordRepository repository;

    @Value("${ledgerflow.idempotency.ttl-hours:24}")
    private int ttlHours;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Checks if the key was already used or initiates a new in-progress idempotency record.
     * Uses REQUIRES_NEW so the lock state is committed independently.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyResult startOrCheck(String key, String requestHash, String resourceType) {
        if (key == null || key.isBlank()) {
            return IdempotencyResult.proceed(null);
        }

        Optional<IdempotencyRecord> existingOpt = repository.findByIdempotencyKey(key);
        if (existingOpt.isPresent()) {
            IdempotencyRecord existing = existingOpt.get();

            if (existing.isExpired()) {
                log.info("Idempotency key {} has expired. Refreshing record.", key);
                repository.delete(existing);
            } else {
                // Key exists and is valid
                if (!existing.getRequestHash().equals(requestHash)) {
                    log.warn("Idempotency conflict for key {}: existing hash {} vs current hash {}",
                            key, existing.getRequestHash(), requestHash);
                    throw new DomainException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                            String.format("Idempotency key '%s' was already used with a different request payload.", key));
                }

                if (existing.getStatus() == IdempotencyStatus.PROCESSING) {
                    log.warn("Concurrent duplicate request in progress for key {}", key);
                    throw new DomainException(ErrorCode.IDEMPOTENCY_KEY_IN_PROGRESS,
                            String.format("A request with idempotency key '%s' is currently processing.", key));
                }

                if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
                    log.info("Returning cached idempotent response for key {}", key);
                    return IdempotencyResult.cached(existing);
                }
            }
        }

        // Create new record in PROCESSING status
        try {
            IdempotencyRecord newRecord = IdempotencyRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .idempotencyKey(key)
                    .requestHash(requestHash)
                    .resourceType(resourceType)
                    .status(IdempotencyStatus.PROCESSING)
                    .expiresAt(Instant.now().plusSeconds((long) ttlHours * 3600))
                    .build();

            IdempotencyRecord saved = repository.saveAndFlush(newRecord);
            return IdempotencyResult.proceed(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition on idempotency key insertion: {}", key);
            throw new DomainException(ErrorCode.IDEMPOTENCY_KEY_IN_PROGRESS,
                    String.format("Concurrent request detected with key '%s'", key));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String key, String resourceId, int httpStatus, String responseBody) {
        if (key == null || key.isBlank()) {
            return;
        }
        repository.findByIdempotencyKey(key).ifPresent(record -> {
            record.setResourceId(resourceId);
            record.setResponseStatus(httpStatus);
            record.setResponseBody(responseBody);
            record.setStatus(IdempotencyStatus.COMPLETED);
            repository.save(record);
            log.info("Idempotency record completed for key: {}", key);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        repository.findByIdempotencyKey(key).ifPresent(record -> {
            log.warn("Deleting failed idempotency record for key: {} to allow safe retry", key);
            repository.delete(record);
        });
    }
}
