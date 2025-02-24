package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.PgupsParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import com.techstud.sch_parser.service.impl.PgupsMappingServiceImpl;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@ActiveProfiles("dev")
@Disabled
public class PgupsParseTest {

    private MappingServiceRef<List<Document>> mappingService;

    @BeforeEach
    public void setUp() {
        mappingService = new PgupsMappingServiceImpl();
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
