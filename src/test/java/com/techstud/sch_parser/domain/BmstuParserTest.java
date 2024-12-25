package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.BmstuParser;
import com.techstud.sch_parser.domain.impl.SseuParser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class BmstuParserTest {

    private CloseableHttpClient closeableHttpClient;
    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        closeableHttpClient = new DefaultHttpClient();
        mappingService = new MappingServiceImpl();
    }

    @Test
    public void parseScheduleTest() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("815c1ebd-bc7e-11ee-b32d-df9b99f124c0");
        Parser parser = new BmstuParser(closeableHttpClient, mappingService);
        Schedule schedule = parser.parseSchedule(parsingTask);
        System.out.println(schedule.toString());
        Assertions.assertNotNull(schedule.toString());

    }

    @Test
    public void parseScheduleTestAnother() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("815c1ebd-bc7e-11ee-b32d-df9b99f124c0");
        Parser parser = new BmstuParser(closeableHttpClient, mappingService);
        Schedule schedule = parser.parseSchedule(parsingTask);
        System.out.println(schedule.toString());
        Assertions.assertNotNull(schedule.toString());
    }
}
