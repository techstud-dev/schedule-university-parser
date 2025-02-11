package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.MiitParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import com.techstud.sch_parser.service.impl.MiitServiceImpl;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Disabled
public class MiitParserTest {

    private MappingServiceRef<List<Document>> mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MiitServiceImpl();
    }

    @Test
    public void miitCheck1() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("189053");
        Parser underTest = new MiitParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }

    @Test
    public void miitCheck2() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("185350");
        Parser underTest = new MiitParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }

    @Test
    public void miitCheck3() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("189530");
        Parser underTest = new MiitParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }

    @Test
    public void miitCheck4() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("122671");
        Parser underTest = new MiitParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }

    @Test
    public void miitCheck5() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("187146");
        Parser underTest = new MiitParser(mappingService);
        underTest.parseSchedule(parsingTask);
    }
}
