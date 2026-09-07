package com.serjnn.discountservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "spring.kafka.topic")
public record KafkaTopicProperties(
        @DefaultValue("discountChangesTopic") String discountChanges,
        @DefaultValue("3") int partitions
) {}
