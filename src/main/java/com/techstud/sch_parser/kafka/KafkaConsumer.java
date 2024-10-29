package com.techstud.sch_parser.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.domain.ParserFacade;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final ThreadLocal<String> messageIdHolder = new ThreadLocal<>();

    private final ParserFacade parserFacade;

    @KafkaListener(topics = "#{'${kafka.topic.parsing-queue}'}", concurrency = "${spring.kafka.listener.concurrency}",
            autoStartup = "true", groupId = "parser_group")
    public void listen(ConsumerRecord<String, String> consumerRecord) throws Exception {
        final String id = consumerRecord.key();
        final String parseTaskAsString = consumerRecord.value();
        messageIdHolder.set(id);
        log.info("Received message  key: {}, value {}", id, parseTaskAsString);
        final ParsingTask parseTask = objectMapper.readValue(parseTaskAsString, ParsingTask.class);
        parserFacade.parseSchedule(id, parseTask);

    }

    public static String getCurrentMessageId() {
        return messageIdHolder.get();
    }

}
