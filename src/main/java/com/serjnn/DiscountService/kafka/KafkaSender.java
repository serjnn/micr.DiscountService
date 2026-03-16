package com.serjnn.DiscountService.kafka;


import com.serjnn.DiscountService.dto.DiscountChangesDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaSender {
    private final KafkaTemplate<String, DiscountChangesDto> kafkaTemplate;

    public void sendNewDiscount(String topicName, DiscountChangesDto discountChangesDto) {
        log.info("Sending discount changes to topic {}: {}", topicName, discountChangesDto);
        kafkaTemplate.send(topicName, discountChangesDto);
    }
}
