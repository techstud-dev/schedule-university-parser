package com.techstud.sch_parser.domain;


import com.techstud.sch_parser.domain.impl.SpbstuParser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@Log4j2
@Disabled
public class SpbstuParserTest {

    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MappingServiceImpl();
    }

    @Test
    public void testReturnSomethingDataByGroupId() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("41067");
        Parser underTest = new SpbstuParser(mappingService);

        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(parsingTask));
        log.info(result);

        assertThat(result).isNotEmpty();
    }

}