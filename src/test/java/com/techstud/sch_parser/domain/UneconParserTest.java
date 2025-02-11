package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.UneconParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import com.techstud.sch_parser.service.impl.UneconServiceImpl;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Disabled
public class UneconParserTest {

    private MappingServiceRef<List<Document>> mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new UneconServiceImpl();
    }

    @Test
    public void testParseSchedule1() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("13643");
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule(parsingTask));
    }

    @Test
    public void testParseSchedule2() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("13645");
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule(parsingTask));
    }

    @Test
    public void testParseSchedule3() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("13648");
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule(parsingTask));
    }

    @Test
    public void testParseSchedule4() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("13650");
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule(parsingTask));
    }

    @Test
    public void testParseSchedule5() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("13652");
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule(parsingTask));
    }
}
