package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.SsauParser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class SsauParserTest {

    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MappingServiceImpl();
    }

    @Test
    public void checkReturnCorrectTimetableMastersEighteenHoursEven() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("531075164");
        Parser underTest = new SsauParser(mappingService);
        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(parsingTask));
        log.info(result);
        assertThat(result).isNotEmpty();
    }

    @Test
    public void checkReturnCorrectTimetableBachelorEighteenHours() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("1274100747");
        Parser underTest = new SsauParser(mappingService);
        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(parsingTask));
        log.info(result);
        assertThat(result).isNotNull();
    }

    @Test
    public void checkReturnCorrectTimetableBachelorEighteenAndEleventhHoursEven() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("531052818");
        Parser underTest = new SsauParser(mappingService);
        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(parsingTask));
        log.info(result);
        assertThat(result).isNotNull();
    }
}
