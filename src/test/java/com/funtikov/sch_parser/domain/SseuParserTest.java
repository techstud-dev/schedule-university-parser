package com.funtikov.sch_parser.domain;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SseuParserTest {

    private CloseableHttpClient closeableHttpClient;
    @BeforeEach
    public void setUp() {
         closeableHttpClient = new DefaultHttpClient();
    }
    @Test
    public void parseScheduleTest() throws Exception {
        Parser parser = new SseuParser(closeableHttpClient);
        parser.parseSchedule(804L);
    }
}
