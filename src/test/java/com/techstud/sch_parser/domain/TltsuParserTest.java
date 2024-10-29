package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.TltsuParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TltsuParserTest {

    private CloseableHttpClient closeableHttpClient;
    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        closeableHttpClient = new DefaultHttpClient();
        mappingService = new MappingServiceImpl();
    }

    @Test
    public void testParseSchedule() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("31790");
        Parser parser = new TltsuParser(mappingService, closeableHttpClient);
        System.out.println(parser.parseSchedule(parsingTask));
    }
}
