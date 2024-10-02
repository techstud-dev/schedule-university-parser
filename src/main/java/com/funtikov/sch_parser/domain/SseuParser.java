package com.funtikov.sch_parser.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.funtikov.sch_parser.model.Schedule;
import com.funtikov.sch_parser.model.api.response.sseu.SseuApiResponse;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
public class SseuParser implements Parser {
    private final String apiUrl = "https://lms3.sseu.ru/api/v1/schedule-board/by-group?groupId={0}&scheduleWeek={1}&date={2}";

    private final CloseableHttpClient httpClient;

    @Override
    public Schedule parseSchedule(Long groupId) throws Exception {

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.format(formatter);

        String[] oddWeekUrlParams = {groupId.toString(), "CURRENT", formattedDate.split("-")[0] + "-09-03"};
        String[] evenWeekUrlParams = {groupId.toString(), "NEXT", formattedDate.split("-")[0] + "-09-03"};

        String oddWeekUrl = MessageFormat.format(apiUrl, oddWeekUrlParams[0], oddWeekUrlParams[1], oddWeekUrlParams[2]);
        String evenWeekUrl = MessageFormat.format(apiUrl, evenWeekUrlParams[0], evenWeekUrlParams[1], evenWeekUrlParams[2]);

        HttpGet getOddRequest = new HttpGet(oddWeekUrl);
        HttpGet getEvenRequest = new HttpGet(evenWeekUrl);

        ObjectMapper mapper = new ObjectMapper();
        SseuApiResponse oddWeekResponse = mapper.readValue(getSchduleJsonAsString(getOddRequest), SseuApiResponse.class);
        SseuApiResponse evenWeekResponse = mapper.readValue(getSchduleJsonAsString(getEvenRequest), SseuApiResponse.class);

        return null;
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
