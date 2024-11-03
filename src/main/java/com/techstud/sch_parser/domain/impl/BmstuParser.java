package com.techstud.sch_parser.domain.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuApiResponse;
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

@Component("BMSTU")
@RequiredArgsConstructor
@Slf4j
public class BmstuParser implements Parser {

    private final CloseableHttpClient httpClient;

    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        String[] urlParams = new String[]{task.getGroupId()};
        String apiUrl = "https://lks.bmstu.ru/lks-back/api/v1/schedules/groups/{0}/public";
        String url = MessageFormat.format(apiUrl, urlParams[0]);
        HttpGet getScheduleRequest = new HttpGet(url);
        log.info("Connect to BMSTU API: {}", getScheduleRequest);
        String scheduleJson = getSchduleJsonAsString(getScheduleRequest);
        ObjectMapper objectMapper = new ObjectMapper();
        if (scheduleJson == null) {
            return null;
        }
        BmstuApiResponse bmstuApiResponse = objectMapper.readValue(scheduleJson, BmstuApiResponse.class);
        log.info("Successfully fetching data from BMSTU API");
        return mappingService.mapBmstuToSchedule(bmstuApiResponse);
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
