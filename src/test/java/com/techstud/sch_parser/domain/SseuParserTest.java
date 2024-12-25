package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.SseuParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class SseuParserTest {

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
        parsingTask.setGroupId("804");
        Parser parser = new SseuParser(closeableHttpClient, mappingService);
        System.out.println(parser.parseSchedule(parsingTask));
    }
}
