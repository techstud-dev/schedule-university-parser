package com.techstud.sch_parser.configuration;

import com.google.gson.Gson;
import com.techstud.sch_parser.handler.GlobalExceptionHandler;
import com.techstud.sch_parser.kafka.KafkaConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
@RequiredArgsConstructor
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.ssl.trust-store-location}")
    private String trustStoreLocation;

    @Value("${spring.kafka.ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${spring.kafka.ssl.key-store-location}")
    private String keyStoreLocation;

    @Value("${spring.kafka.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${spring.kafka.ssl.key-password}")
    private String keyPassword;

    @Value("${spring.kafka.security.protocol}")
    private String securityProtocol;

    @Value("${spring.kafka.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${spring.kafka.ssl.trust-store-type}")
    private String trustStoreType;

    @Value("${kafka.topic.parsing-failure}")
    private String errorTopic;

    private final GlobalExceptionHandler globalExceptionHandler;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configureSsl(configProps);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configureSsl(configProps);
        log.info("Kafka configs: {}", configProps);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(KafkaTemplate<String, String> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate));
        return factory;
    }

    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 0);
        return new DefaultErrorHandler((record, exception) -> {
            Map<String, String> errorResponseMap = globalExceptionHandler.handleException(KafkaConsumer.getCurrentMessageId(), exception);
            String errorResponse = new Gson().toJson(errorResponseMap);
            kafkaTemplate.send(errorTopic, errorResponse);
        }, fixedBackOff);
    }

    private void configureSsl(Map<String, Object> configProps) {
        configProps.put("security.protocol", securityProtocol);
        configProps.put("ssl.truststore.location", trustStoreLocation);
        configProps.put("ssl.truststore.password", trustStorePassword);
        configProps.put("ssl.truststore.type", trustStoreType);
        configProps.put("ssl.keystore.location", keyStoreLocation);
        configProps.put("ssl.keystore.password", keyStorePassword);
        configProps.put("ssl.key.password", keyPassword);
        configProps.put("ssl.keystore.type", keyStoreType);
    }
}
