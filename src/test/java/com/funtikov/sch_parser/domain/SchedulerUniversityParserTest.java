package com.funtikov.sch_parser.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SchedulerUniversityParserTest {

    @Test
    public void testParse() throws IOException {
        Parser parser = new SamaraUniversityParser();
        System.out.println(parser.parseSchedule(604071802L));
    }
}
