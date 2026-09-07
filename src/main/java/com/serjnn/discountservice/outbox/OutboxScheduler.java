package com.serjnn.discountservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serjnn.discountservice.config.OutboxProperties;
import com.serjnn.discountservice.dto.DiscountChangesDto;
import com.serjnn.discountservice.kafka.KafkaSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final OutboxProperties outboxProperties;

    @Scheduled(fixedDelayString = "${outbox.scheduler.interval-ms:500}")
    public void processOutboxEvents() {
        OutboxProperties.Scheduler schedulerConfig = outboxProperties.scheduler();
        List<OutboxEvent> claimedEvents = outboxRepository.claimPendingEvents(
                schedulerConfig.batchSize(),
                schedulerConfig.leaseTimeMs()
        );
        if (claimedEvents.isEmpty()) {
            return;
        }

        log.debug("Claimed {} pending outbox events to process", claimedEvents.size());

        for (OutboxEvent event : claimedEvents) {
            try {
                if ("DISCOUNT_CHANGED".equals(event.eventType())) {
                    DiscountChangesDto dto = objectMapper.readValue(event.payload(), DiscountChangesDto.class);
                    kafkaSender.sendDiscountChangesSync(dto, schedulerConfig.publishTimeoutMs());
                } else {
                    log.warn("Unknown outbox event type: {}", event.eventType());
                }
                outboxRepository.markAsProcessed(event.id());
                log.info("Successfully processed and published outbox event id: {}", event.id());
            } catch (Exception ex) {
                long backoffMs = calculateBackoff(event.retryCount(), schedulerConfig);
                log.error("Failed to publish outbox event id: {} (attempt {}/{}, next retry in {} ms)",
                        event.id(), event.retryCount() + 1, schedulerConfig.maxRetries(), backoffMs, ex);
                outboxRepository.markAsFailed(event.id(), ex.getMessage(), schedulerConfig.maxRetries(), backoffMs);
            }
        }
    }

    @Scheduled(cron = "${outbox.cleanup.cron:0 0 2 * * *}")
    public void purgeOldProcessedEvents() {
        try {
            int retentionDays = outboxProperties.cleanup().retentionDays();
            log.info("Starting scheduled purge of processed outbox events older than {} days", retentionDays);
            int deleted = outboxRepository.deleteOldProcessedEvents(retentionDays);
            log.info("Finished scheduled purge: removed {} old outbox events", deleted);
        } catch (Exception ex) {
            log.error("Failed to purge old processed outbox events", ex);
        }
    }

    private long calculateBackoff(int retryCount, OutboxProperties.Scheduler config) {
        long backoff = (long) (config.initialBackoffMs() * Math.pow(config.backoffMultiplier(), retryCount));
        return Math.min(backoff, config.maxBackoffMs());
    }
}
