package com.techstud.sch_parser.handler;

import com.google.gson.Gson;
import com.techstud.sch_parser.kafka.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.systemName}")
    private String systemName;

    public String handleException(ConsumerRecord<?, ?> record, Exception exception) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("systemName", systemName);
        response.put("serviceName", applicationName);
        response.put("messageId", KafkaConsumer.getCurrentMessageId());
        response.put("message", exception.getMessage());
        Gson gson = new Gson();
        return gson.toJson(response);
    }
}
