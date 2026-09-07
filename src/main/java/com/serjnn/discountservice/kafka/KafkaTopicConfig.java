package com.serjnn.discountservice.kafka;

import com.serjnn.discountservice.config.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private final KafkaTopicProperties topicProperties;

    @Bean
    public NewTopic newDiscountTopic() {
        return TopicBuilder.name(topicProperties.discountChanges())
                .partitions(topicProperties.partitions())
                .build();
    }
}
