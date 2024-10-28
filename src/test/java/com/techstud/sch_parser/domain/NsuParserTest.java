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
    public void nsuCheck() throws Exception {
        Parser underTest = new NsuParser(mappingService);
        System.out.println(underTest.parseSchedule(String.valueOf(24511)));
    }
}
