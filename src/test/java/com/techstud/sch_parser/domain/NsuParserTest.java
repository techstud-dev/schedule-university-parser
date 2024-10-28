package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.NsuParser;
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
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(String.valueOf(24513.2)));
    }

    @Test
    public void nsuCheck2() throws Exception {
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(String.valueOf(24514.1)));
    }

    @Test
    public void nsuCheck3() throws Exception {
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(String.valueOf(23514.2)));
    }

    @Test
    public void nsuCheck4() throws Exception {
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(String.valueOf(21504.1)));
    }

    @Test
    public void nsuCheck5() throws Exception {
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(String.valueOf(23503.2)));
    }
}
