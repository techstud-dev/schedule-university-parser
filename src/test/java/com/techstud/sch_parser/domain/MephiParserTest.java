package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.MephiParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class MephiParserTest {

    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MappingServiceImpl();
    }


    @Test
    public void checkIsParserReturningNormalHtmlStructure1() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("19468");
        Parser underTest = new MephiParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }

    @Test
    public void checkIsParserReturningNormalHtmlStructure2() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("19389");
        Parser underTest = new MephiParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }

    @Test
    public void checkIsParserReturningNormalHtmlStructure3() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("19323");
        Parser underTest = new MephiParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }

    @Test
    public void checkIsParserReturningNormalHtmlStructure4() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("19267");
        Parser underTest = new MephiParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }

    @Test
    public void checkIsParserReturningNormalHtmlStructure5() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("19266");
        Parser underTest = new MephiParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }
}
