package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.SsauParser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import com.techstud.sch_parser.service.impl.SsauServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@Disabled
public class SsauParserTest {

    private MappingServiceRef<List<Document>> mappingServiceRef;

    @BeforeEach
    public void setUp() {
        mappingServiceRef = new SsauServiceImpl();
    }

    @Test
    public void checkReturnCorrectTimetableMastersEighteenHoursEven() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("531075164");
        Parser underTest = new SsauParser(mappingServiceRef);
        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(parsingTask));
        assertThat(result).isNotNull();
    }

    @Test
    public void checkReturnCorrectTimetableBachelorEighteenHours() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("1274100747");
        Parser underTest = new SsauParser(mappingServiceRef);
        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(parsingTask));
        assertThat(result).isNotNull();
    }

    @Test
    public void checkReturnCorrectTimetableBachelorEighteenAndEleventhHoursEven() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("531052818");
        Parser underTest = new SsauParser(mappingServiceRef);
        Optional<Schedule> result = Optional.ofNullable(
                underTest.parseSchedule(parsingTask));
        assertThat(result).isNotNull();
    }
}
