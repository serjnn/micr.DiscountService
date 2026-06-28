package com.serjnn.DiscountService.kafka.producer;

import com.serjnn.DiscountService.dto.DiscountChangesDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSender {
    private final KafkaTemplate<String, DiscountChangesDto> kafkaTemplate;

    public Mono<Void> sendNewDiscount(String topicName, DiscountChangesDto discountChangesDto) {
        log.info("Sending discount changes to topic {}: {}", topicName, discountChangesDto);
        return Mono.fromFuture(() -> kafkaTemplate.send(topicName, discountChangesDto))
                .doOnError(e -> log.error("Failed to send discount changes to topic {}: {}", topicName, discountChangesDto, e))
                .then();
    }
}
