package com.techstud.sch_parser.handler;

import com.techstud.sch_parser.kafka.KafkaConsumer;
import com.techstud.sch_parser.kafka.KafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.systemName}")
    private String systemName;

    @Value("${kafka.topic.parsing-failure}")
    private String errorKafkaTopic;

    private final KafkaProducer kafkaProducer;

    @ExceptionHandler(Exception.class)
    public void handleException(Exception exception) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("systemName", systemName);
        response.put("serviceName", applicationName);
        response.put("messageId", KafkaConsumer.getCurrentMessageId());
        response.put("message", exception.getMessage());
        log.error(exception.getMessage(), exception);
        kafkaProducer.sendToKafka(KafkaConsumer.getCurrentMessageId(), errorKafkaTopic, response);
    }

}
