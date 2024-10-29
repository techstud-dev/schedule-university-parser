package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.NsuParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NsuParserTest {

    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MappingServiceImpl();
    }


    @Test
    public void nsuCheck1() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("24513.2");
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(parsingTask));
    }

    @Test
    public void nsuCheck2() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("24514.1");
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(parsingTask));
    }

    @Test
    public void nsuCheck3() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("23514.2");
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(parsingTask));
    }

    @Test
    public void nsuCheck4() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("21504.1");
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(parsingTask));
    }

    @Test
    public void nsuCheck5() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("23503.2");
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(parsingTask));
    }
}
