package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.SseuParser;
import com.techstud.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import com.techstud.sch_parser.service.impl.SseuServiceImpl;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Disabled
public class SseuParserTest {

    private CloseableHttpClient closeableHttpClient;
    private MappingServiceRef<List<SseuApiResponse>> mappingServiceRef;

    @BeforeEach
    public void setUp() {
        closeableHttpClient = new DefaultHttpClient();
        mappingServiceRef = new SseuServiceImpl();
    }

    @Test
    public void parseScheduleTest() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("804");
        Parser parser = new SseuParser(closeableHttpClient, mappingServiceRef);
        System.out.println(parser.parseSchedule(parsingTask));
    }
}
