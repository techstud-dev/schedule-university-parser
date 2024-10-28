package com.techstud.sch_parser.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducer {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendToKafka(String id, String topic, Object objectMessage) {
        String parserResultAsString;

        try {
            parserResultAsString = mapper.writeValueAsString(objectMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return;
        }
        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, id, parserResultAsString);
        //TODO: Добавить заголовки в ответ
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(producerRecord);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                handleSuccess(id, topic);
            } else {
                handleFailure(id, ex, topic);
            }
        });
    }

    private void handleFailure(String id, Throwable ex, String topic) {
        String msg = String.format("Event id = %s unable to send into kafka topic %s", id, topic);
        log.error(msg, ex);
    }

    private void handleSuccess(String id, String topic) {
        log.info("Event id = {} sent into kafka topic {} successfully", id, topic);
    }
}
