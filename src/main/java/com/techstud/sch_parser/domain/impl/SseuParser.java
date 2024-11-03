package com.techstud.sch_parser.domain.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Component("SSEU")
@Slf4j
public class SseuParser implements Parser {
    private final String apiUrl = "https://lms3.sseu.ru/api/v1/schedule-board/by-group?groupId={0}&scheduleWeek={1}&date={2}";

    private final CloseableHttpClient httpClient;

    private final MappingService mappingService;

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
        return mappingService.mapSseuToSchedule(sseuApiResponseList);
    }

    private String getSchduleJsonAsString(HttpGet getRequest) throws IOException {
        try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
            if (response.getEntity() != null) {
                return EntityUtils.toString(response.getEntity());
            }
        }
        return null;
    }
}
