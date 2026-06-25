package com.serjnn.DiscountService.kafka;

import com.serjnn.DiscountService.dto.DiscountChangesDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConfiguration {

    @Bean
    DefaultKafkaProducerFactory<String, DiscountChangesDto> discountChangesDtoProducerFactory(
            KafkaProperties properties,
            ObjectProvider<KafkaConnectionDetails> connectionDetailsProvider,
            ObjectProvider<SslBundles> sslBundles) {
        Map<String, Object> producerProperties = properties.buildProducerProperties(sslBundles.getIfAvailable());
        KafkaConnectionDetails connectionDetails = connectionDetailsProvider.getIfAvailable();
        if (connectionDetails != null) {
            producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getBootstrapServers());
        }
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerProperties);
    }

    @Bean
    KafkaTemplate<String, DiscountChangesDto> discountChangesDtoKafkaTemplate(
            DefaultKafkaProducerFactory<String, DiscountChangesDto> discountChangesDtoProducerFactory) {
        return new KafkaTemplate<>(discountChangesDtoProducerFactory);
    }

    @Bean
    ConsumerFactory<String, DiscountChangesDto> discountChangesDtoConsumerFactory(
            KafkaProperties properties,
            ObjectProvider<KafkaConnectionDetails> connectionDetailsProvider,
            ObjectProvider<SslBundles> sslBundles) {
        Map<String, Object> consumerProperties = properties.buildConsumerProperties(sslBundles.getIfAvailable());
        KafkaConnectionDetails connectionDetails = connectionDetailsProvider.getIfAvailable();
        if (connectionDetails != null) {
            consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getBootstrapServers());
        }
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProperties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.serjnn.DiscountService.dto");
        consumerProperties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.serjnn.DiscountService.dto.DiscountChangesDto");
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, DiscountChangesDto> kafkaListenerContainerFactory(
            ConsumerFactory<String, DiscountChangesDto> discountChangesDtoConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, DiscountChangesDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(discountChangesDtoConsumerFactory);
        return factory;
    }

}
