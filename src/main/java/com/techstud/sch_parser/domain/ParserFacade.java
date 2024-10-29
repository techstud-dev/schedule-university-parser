package com.techstud.sch_parser.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.techstud.sch_parser.handler.GlobalExceptionHandler;
import com.techstud.sch_parser.kafka.KafkaProducer;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ParserFacade {

    private final ParserFactory parserFactory;
    private final KafkaProducer kafkaProducer;

    public void parseSchedule(String messageKey, ParsingTask task) throws Exception {
        log.info("Received task: {}", task);
        Parser parser = parserFactory.getParser(task);
            Schedule schedule = parser.parseSchedule(task);
            kafkaProducer.sendSuccess(messageKey, schedule);
    }
}
