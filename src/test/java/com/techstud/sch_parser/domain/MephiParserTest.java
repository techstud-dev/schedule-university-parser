package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.MephiParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import com.techstud.sch_parser.service.impl.MephiServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
@Disabled
public class MephiParserTest {

    private MappingServiceRef<List<Document>> mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MephiServiceImpl();
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
