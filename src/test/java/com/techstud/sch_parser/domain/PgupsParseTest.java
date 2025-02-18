package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.PgupsParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.impl.MappingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("dev")
@Disabled
public class PgupsParseTest {

    private MappingService mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new MappingServiceImpl();
    }

    @Test
    public void pgupsCheck1() throws Exception {
        ParsingTask task = new ParsingTask();
        task.setGroupId("1804106313720092287");
        task.setUniversityName("PGUPS");
        Parser underTest = new PgupsParser(mappingService);
        System.out.println(underTest.parseSchedule(task));
    }
}
