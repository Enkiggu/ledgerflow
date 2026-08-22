-- V7: Processed Events for Idempotent Consumers
CREATE TABLE IF NOT EXISTS processed_events (
    id VARCHAR(36) PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_event_consumer UNIQUE (event_id, consumer_name)
);

CREATE INDEX idx_processed_events_lookup ON processed_events(event_id, consumer_name);
