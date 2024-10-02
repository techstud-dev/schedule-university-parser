package com.funtikov.sch_parser.domain;

import org.junit.jupiter.api.Test;

public class SchedulerUniversityParserTest {

    @Test
    public void testParse() throws Exception {
        Parser parser = new SamaraUniversityParser();
        System.out.println(parser.parseSchedule(604071802L));
    }
}
