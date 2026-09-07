package com.serjnn.discountservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(
        @DefaultValue Scheduler scheduler,
        @DefaultValue Cleanup cleanup
) {
    public OutboxProperties {
        if (scheduler == null) {
            scheduler = new Scheduler(500, 50, 5, 5000, 30000, 1000, 60000, 2.0);
        }
        if (cleanup == null) {
            cleanup = new Cleanup("0 0 2 * * *", 7);
        }
    }

    public record Scheduler(
            @DefaultValue("500") long intervalMs,
            @DefaultValue("50") int batchSize,
            @DefaultValue("5") int maxRetries,
            @DefaultValue("5000") long publishTimeoutMs,
            @DefaultValue("30000") long leaseTimeMs,
            @DefaultValue("1000") long initialBackoffMs,
            @DefaultValue("60000") long maxBackoffMs,
            @DefaultValue("2.0") double backoffMultiplier
    ) {}

    public record Cleanup(
            @DefaultValue("0 0 2 * * *") String cron,
            @DefaultValue("7") int retentionDays
    ) {}
}
