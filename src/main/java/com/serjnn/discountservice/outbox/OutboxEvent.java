package com.serjnn.discountservice.outbox;

import java.time.Instant;

public record OutboxEvent(
        Long id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        String status,
        int retryCount,
        String errorMessage,
        Instant createdAt,
        Instant processedAt
) {
    public static OutboxEvent pending(String aggregateType, String aggregateId, String eventType, String payload) {
        return new OutboxEvent(null, aggregateType, aggregateId, eventType, payload, "PENDING", 0, null, null, null);
    }
}
