package com.serjnn.discountservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serjnn.discountservice.dto.DiscountChangesDto;
import com.serjnn.discountservice.kafka.KafkaSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final KafkaSender kafkaSender;
    private final ObjectMapper objectMapper;

    @Value("${outbox.scheduler.batch-size:50}")
    private int batchSize;

    @Value("${outbox.scheduler.max-retries:5}")
    private int maxRetries;

    @Value("${outbox.scheduler.publish-timeout-ms:5000}")
    private long publishTimeoutMs;

    @Value("${outbox.scheduler.lease-time-ms:30000}")
    private long leaseTimeMs;

    @Value("${outbox.scheduler.initial-backoff-ms:1000}")
    private long initialBackoffMs;

    @Value("${outbox.scheduler.max-backoff-ms:60000}")
    private long maxBackoffMs;

    @Value("${outbox.scheduler.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    @Value("${outbox.cleanup.retention-days:7}")
    private int retentionDays;

    @Scheduled(fixedDelayString = "${outbox.scheduler.interval-ms:500}")
    public void processOutboxEvents() {
        List<OutboxEvent> claimedEvents = outboxRepository.claimPendingEvents(batchSize, leaseTimeMs);
        if (claimedEvents.isEmpty()) {
            return;
        }

        log.debug("Claimed {} pending outbox events to process", claimedEvents.size());

        for (OutboxEvent event : claimedEvents) {
            try {
                if ("DISCOUNT_CHANGED".equals(event.eventType())) {
                    DiscountChangesDto dto = objectMapper.readValue(event.payload(), DiscountChangesDto.class);
                    kafkaSender.sendDiscountChangesSync(dto, publishTimeoutMs);
                } else {
                    log.warn("Unknown outbox event type: {}", event.eventType());
                }
                outboxRepository.markAsProcessed(event.id());
                log.info("Successfully processed and published outbox event id: {}", event.id());
            } catch (Exception ex) {
                long backoffMs = calculateBackoff(event.retryCount());
                log.error("Failed to publish outbox event id: {} (attempt {}/{}, next retry in {} ms)",
                        event.id(), event.retryCount() + 1, maxRetries, backoffMs, ex);
                outboxRepository.markAsFailed(event.id(), ex.getMessage(), maxRetries, backoffMs);
            }
        }
    }

    @Scheduled(cron = "${outbox.cleanup.cron:0 0 2 * * *}")
    public void purgeOldProcessedEvents() {
        try {
            log.info("Starting scheduled purge of processed outbox events older than {} days", retentionDays);
            int deleted = outboxRepository.deleteOldProcessedEvents(retentionDays);
            log.info("Finished scheduled purge: removed {} old outbox events", deleted);
        } catch (Exception ex) {
            log.error("Failed to purge old processed outbox events", ex);
        }
    }

    private long calculateBackoff(int retryCount) {
        long backoff = (long) (initialBackoffMs * Math.pow(backoffMultiplier, retryCount));
        return Math.min(backoff, maxBackoffMs);
    }
}
