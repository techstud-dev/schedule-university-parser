package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.domain.impl.TltsuParser;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuApiResponse;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import com.techstud.sch_parser.service.impl.TItsuServiceImpl;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

@Disabled
public class TltsuParserTest {

    private CloseableHttpClient closeableHttpClient;
    private MappingServiceRef<List<TltsuApiResponse>> mappingServiceRef;

    @BeforeEach
    public void setUp() {
        closeableHttpClient = new DefaultHttpClient();
        mappingServiceRef = new TItsuServiceImpl();
    }

    @Test
    public void testParseSchedule() throws Exception {
        ParsingTask parsingTask = new ParsingTask();
        parsingTask.setGroupId("31790");
        Parser parser = new TltsuParser(mappingServiceRef, closeableHttpClient);
        System.out.println(parser.parseSchedule(parsingTask));
    }
}
