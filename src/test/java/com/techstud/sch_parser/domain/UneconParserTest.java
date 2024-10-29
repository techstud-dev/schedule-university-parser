package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.UneconParser;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UneconParserTest {

    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MappingServiceImpl();
    }

    @Test
    public void testParseSchedule1() throws Exception {
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule("13643"));
    }

    @Test
    public void testParseSchedule2() throws Exception {
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule("13645"));
    }

    @Test
    public void testParseSchedule3() throws Exception {
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule("13648"));
    }

    @Test
    public void testParseSchedule4() throws Exception {
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule("13650"));
    }

    @Test
    public void testParseSchedule5() throws Exception {
        Parser parser = new UneconParser(mappingService);
        System.out.println(parser.parseSchedule("13652"));
    }
}