package com.techstud.sch_parser.domain.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuApiResponse;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class BmstuParser implements Parser {

    private final String apiUrl = "https://lks.bmstu.ru/lks-back/api/v1/schedules/groups/{0}/public";

    private final CloseableHttpClient httpClient;

    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(String groupId) throws Exception {
        String[] urlParams = new String[]{groupId};
        String url = MessageFormat.format(apiUrl, urlParams[0]);
        HttpGet getScheduleRequest = new HttpGet(url);
        String scheduleJson = getSchduleJsonAsString(getScheduleRequest);
        ObjectMapper objectMapper = new ObjectMapper();
        BmstuApiResponse bmstuApiResponse = objectMapper.readValue(scheduleJson, BmstuApiResponse.class);
        log.info(bmstuApiResponse.toString());
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
