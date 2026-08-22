package com.ledgerflow.unit;

import com.ledgerflow.idempotency.service.RequestHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Request Hasher Fingerprint Tests")
class RequestHasherTest {

    private final RequestHasher hasher = new RequestHasher();

    @Test
    @DisplayName("Should produce consistent deterministic SHA-256 hex string")
    void shouldProduceDeterministicHash() {
        String payload = "{\"orderId\":\"123\",\"amount\":5000}";
        String hash1 = hasher.computeSha256(payload);
        String hash2 = hasher.computeSha256(payload);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Should produce different hashes for different payloads")
    void shouldDifferentiatePayloads() {
        String hash1 = hasher.computeSha256("{\"amount\":5000}");
        String hash2 = hasher.computeSha256("{\"amount\":5001}");

        assertNotEquals(hash1, hash2);
    }
}
