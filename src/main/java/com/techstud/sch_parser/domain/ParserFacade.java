package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.handler.GlobalExceptionHandler;
import com.techstud.sch_parser.kafka.KafkaProducer;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ParserFacade {

    private final ParserFactory parserFactory;
    private final KafkaProducer kafkaProducer;
    private final GlobalExceptionHandler globalExceptionHandler;
    private final ValidationService validationService;

    @Async("taskExecutor")
    public void parseSchedule(String messageKey, ParsingTask task) {
        log.info("Received task: {}", task);
        Schedule schedule;
        try {
            validationService.validateRequest(task, parserFactory.getParserList());
            Parser parser = parserFactory.getParser(task);
            schedule = parser.parseSchedule(task);
            validationService.validateSchedule(schedule);
        } catch (Exception e) {
            log.error("Error while parsing schedule", e);
            kafkaProducer.sendFailure(messageKey, globalExceptionHandler.handleException(messageKey, e));
            return;
        }
        if (schedule != null) {
            kafkaProducer.sendSuccess(messageKey, schedule);
        }
    }
}
