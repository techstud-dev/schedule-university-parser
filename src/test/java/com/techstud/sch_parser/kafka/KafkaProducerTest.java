package com.techstud.sch_parser.kafka;

import com.techstud.sch_parser.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("dev")
public class KafkaProducerTest {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Value("${kafka.topic.parsing-failure}")
    private String failureTopic;

    @Value("${kafka.topic.parsing-result}")
    private String successTopic;

    @Autowired
    private GlobalExceptionHandler exceptionHandler;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @Disabled
    @DisplayName("Test with opened kafka in docker")
    void testFailureSendToKafka() {
        String id = "schedulerParser_test_failure_send_kafka";
        Map<String, String> response = new LinkedHashMap<>();
        response.put("systemName", "sch-parser");
        response.put("serviceName", "tchs");
        response.put("messageId", KafkaConsumer.getCurrentMessageId());

    }
}
