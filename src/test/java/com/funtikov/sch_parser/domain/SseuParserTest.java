package com.funtikov.sch_parser.domain;

import com.funtikov.sch_parser.service.MappingService;
import com.funtikov.sch_parser.service.impl.MappingServiceImpl;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        Parser parser = new SseuParser(closeableHttpClient, mappingService);
        System.out.println(parser.parseSchedule(804L));
    }
}
