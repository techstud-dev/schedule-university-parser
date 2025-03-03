package com.techstud.sch_parser.domain.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component("SSEU")
@Slf4j
public class SseuParser implements Parser {
    private final String apiUrl = "https://lms3.sseu.ru/api/v1/schedule-board/by-group?groupId={0}&scheduleWeek={1}&date={2}";

    private final CloseableHttpClient httpClient;

    private final MappingServiceRef<List<SseuApiResponse>> mappingServiceRef;

    /**
     * @param httpClient HTTP-client to execute requests
     * @param mappingServiceRef mapping service implemented through Spring. Annotation {@link Qualifier} indicates the specific implementation of the service
     */
    public SseuParser(
            CloseableHttpClient httpClient,
            @Qualifier("sseuServiceImpl") MappingServiceRef<List<SseuApiResponse>> mappingServiceRef){
        this.httpClient = httpClient;
        this.mappingServiceRef = mappingServiceRef;
    }

    /**
     * @param task parsing task containing the necessary parameters (for example, group identifier).
     * @return schedule object {@link Schedule}, received after the mapping data from API
     * @throws Exception the exception that may occur when performing HTTP request or data mapping
     */
    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.format(formatter);
        String[] currentWeekUrlParams;
        String[] nextWeekUrlParams;

        currentWeekUrlParams = new String[]{task.getGroupId(), "CURRENT", formattedDate};
        nextWeekUrlParams = new String[]{task.getGroupId(), "NEXT", formattedDate};

        String currentWeekUrl = MessageFormat.format(apiUrl, currentWeekUrlParams[0], currentWeekUrlParams[1], currentWeekUrlParams[2]);
        String nextWeekUrl = MessageFormat.format(apiUrl, nextWeekUrlParams[0], nextWeekUrlParams[1], nextWeekUrlParams[2]);

        HttpGet getOddRequest = new HttpGet(currentWeekUrl);
        HttpGet getEvenRequest = new HttpGet(nextWeekUrl);
        log.info("Connect to SSEU API: evenUrl: {}, oddEven: {}", getEvenRequest, getOddRequest);
        ObjectMapper mapper = new ObjectMapper();
        SseuApiResponse currentWeekResponse = mapper.readValue(getSchduleJsonAsString(getOddRequest), SseuApiResponse.class);
        SseuApiResponse nextWeekResponse = mapper.readValue(getSchduleJsonAsString(getEvenRequest), SseuApiResponse.class);
        List<SseuApiResponse> sseuApiResponseList = List.of(currentWeekResponse, nextWeekResponse);

        log.info("Successfully fetching data from SSEU API");
        return mappingServiceRef.map(sseuApiResponseList);
    }

    /**
     * @param getRequest HTTP request type GET
     * @return JSON line containing a response from the server. If the answer is empty, null returns
     * @throws IOException an exception that may occur when performing an HTTP request
     */
    private String getSchduleJsonAsString(HttpGet getRequest) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            if (response.getEntity() != null) {
                return EntityUtils.toString(response.getEntity());
            }
        }
        return null;
    }
}
