package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.PgupsParser;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import com.techstud.sch_parser.service.impl.PgpusServiceImpl;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@ActiveProfiles("dev")
@Disabled
public class PgupsParseTest {

    private MappingServiceRef<List<Document>> mappingServiceRef;

    @BeforeEach
    public void setUp() {
        mappingServiceRef = new PgpusServiceImpl();
    }

    @Test
    public void pgupsCheck1() throws Exception {
        ParsingTask task = new ParsingTask();
        task.setGroupId("1803720390255793791");
        task.setUniversityName("PGUPS");
        Parser underTest = new PgupsParser(mappingServiceRef);
        System.out.println(underTest.parseSchedule(task));
    }
}
