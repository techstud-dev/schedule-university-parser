package com.techstud.sch_parser.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "#{'${kafka.topic.parsing-queue}'}", concurrency = "${spring.kafka.listener.concurrency}",
            autoStartup = "true", groupId = "parser_group")
    public void listen(ConsumerRecord<String, String> consumerRecord) {
        final String id = consumerRecord.key();
        final String parseTaskAsString = consumerRecord.value();
        log.info("Received message  key: {}, value {}", id, parseTaskAsString);
        //TODO: После утверждения схемы сообщения реализовать логику
    }
}
