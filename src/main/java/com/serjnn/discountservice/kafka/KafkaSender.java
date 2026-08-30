package com.serjnn.discountservice.kafka;

import com.serjnn.discountservice.dto.DiscountChangesDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaSender {

    private final KafkaTemplate<String, DiscountChangesDto> kafkaTemplate;
    private final String topicName;

    public KafkaSender(
            KafkaTemplate<String, DiscountChangesDto> kafkaTemplate,
            @Value("${spring.kafka.topic.discount-changes}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public CompletableFuture<SendResult<String, DiscountChangesDto>> sendDiscountChanges(DiscountChangesDto discountChangesDto) {
        String key = String.valueOf(discountChangesDto.productId());
        log.info("Sending discount changes with key {} to topic {}: {}", key, topicName, discountChangesDto);
        CompletableFuture<SendResult<String, DiscountChangesDto>> future = kafkaTemplate.send(topicName, key, discountChangesDto);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Successfully sent discount changes for product {} to topic {}", 
                        discountChangesDto.productId(), topicName);
            } else {
                log.error("Failed to send discount changes for product {} to topic {}", 
                        discountChangesDto.productId(), topicName, ex);
            }
        });
        return future;
    }

    public void sendDiscountChangesSync(DiscountChangesDto discountChangesDto, long timeoutMs) throws Exception {
        sendDiscountChanges(discountChangesDto).get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
}
