package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.MephiParser;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class MephiParserTest {

    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MappingServiceImpl();
    }

    /**
     * ТУТ 5 ГРУПП 5 РАЗНЫХ КУРСОВ, ОНИ ОБОЗНАЧЕНЫ ПО НОМЕРУ (1, 2, 3, 4, 5)
     */

    @Test
    public void checkIsParserReturningNormalHtmlStructure1() throws Exception {
        Parser underTest = new MephiParser(mappingService);
        System.out.println(underTest.parseSchedule(19468L));
    }

    @Test
    public void checkIsParserReturningNormalHtmlStructure2() throws Exception {
        Parser underTest = new MephiParser(mappingService);
        System.out.println(underTest.parseSchedule(19389L));
    }

    @Test
    public void checkIsParserReturningNormalHtmlStructure3() throws Exception {
        Parser underTest = new MephiParser(mappingService);
        System.out.println(underTest.parseSchedule(19323L));
    }

    @Test
    public void checkIsParserReturningNormalHtmlStructure4() throws Exception {
        Parser underTest = new MephiParser(mappingService);
        System.out.println(underTest.parseSchedule(19267L));
    }

    @Test
    public void checkIsParserReturningNormalHtmlStructure5() throws Exception {
        Parser underTest = new MephiParser(mappingService);
        System.out.println(underTest.parseSchedule(19266L));
    }
}
