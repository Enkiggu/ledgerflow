package com.ledgerflow.messaging.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.Instant;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface DomainEvent extends Serializable {
    String getEventId();
    String getAggregateId();
    String getEventType();
    Instant getOccurredAt();
}
