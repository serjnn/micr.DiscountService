package com.serjnn.DiscountService.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.topic.discount-changes}")
    private String discountChangesTopic;

    @Value("${spring.kafka.topic.partitions}")
    private int partitions;

    @Bean
    public NewTopic newDiscountTopic() {
        return TopicBuilder.name(discountChangesTopic)
                .partitions(partitions)
                .build();
    }
}
