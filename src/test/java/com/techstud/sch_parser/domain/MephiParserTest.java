package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.MephiParser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
public class MephiParserTest {

    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MappingServiceImpl();
    }

    @Test
    public void checkIsParserReturningNormalHtmlStructure() throws Exception {
        Parser underTest = new MephiParser(mappingService);
        System.out.println(underTest.parseSchedule(19468L));
    }
}
