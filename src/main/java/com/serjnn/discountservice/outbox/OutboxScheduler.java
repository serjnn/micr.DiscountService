package com.serjnn.discountservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serjnn.discountservice.dto.DiscountChangesDto;
import com.serjnn.discountservice.kafka.KafkaSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Scheduled(fixedDelayString = "${outbox.scheduler.interval-ms:500}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.fetchAndLockPendingEvents(batchSize);
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox events to process", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
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
                log.error("Failed to publish outbox event id: {}", event.id(), ex);
                outboxRepository.markAsFailed(event.id(), ex.getMessage(), maxRetries);
            }
        }
    }
}
