package com.techstud.sch_parser.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.annotation.Profiling;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.techstud.sch_parser.service.MappingService;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RequiredArgsConstructor
@Component
public class SseuParser implements Parser {
    private final String apiUrl = "https://lms3.sseu.ru/api/v1/schedule-board/by-group?groupId={0}&scheduleWeek={1}&date={2}";

    private final CloseableHttpClient httpClient;

    private final MappingService mappingService;

    @Override
    @Profiling
    public Schedule parseSchedule(Long groupId) throws Exception {

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.format(formatter);
        String[] currentWeekUrlParams;
        String[] nextWeekUrlParams;

        currentWeekUrlParams = new String[]{groupId.toString(), "CURRENT", formattedDate};
        nextWeekUrlParams = new String[]{groupId.toString(), "NEXT", formattedDate};

        String currentWeekUrl = MessageFormat.format(apiUrl,  currentWeekUrlParams[0], currentWeekUrlParams[1],  currentWeekUrlParams[2]);
        String nextWeekUrl = MessageFormat.format(apiUrl, nextWeekUrlParams[0], nextWeekUrlParams[1], nextWeekUrlParams[2]);

        HttpGet getOddRequest = new HttpGet(currentWeekUrl);
        HttpGet getEvenRequest = new HttpGet(nextWeekUrl);

        ObjectMapper mapper = new ObjectMapper();
        SseuApiResponse currentWeekResponse = mapper.readValue(getSchduleJsonAsString(getOddRequest), SseuApiResponse.class);
        SseuApiResponse nextWeekResponse = mapper.readValue(getSchduleJsonAsString(getEvenRequest), SseuApiResponse.class);
        List<SseuApiResponse> sseuApiResponseList = List.of(currentWeekResponse, nextWeekResponse);

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
