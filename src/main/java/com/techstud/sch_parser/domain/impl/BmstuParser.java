package com.techstud.sch_parser.domain.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuApiResponse;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.swing.text.Document;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

@Component("BMSTU")
@Slf4j
public class BmstuParser implements Parser {

    private final CloseableHttpClient httpClient;

    private final MappingServiceRef<BmstuApiResponse> mappingServiceRef;

    /**
     * @param mappingServiceRef service Mapping, introduced through Spring. Annotation {@link Qualifier} indicates the specific implementation of the service
     */
    public BmstuParser(
            @Qualifier("bmstuServiceImpl") MappingServiceRef<BmstuApiResponse> mappingServiceRef
    ){
        this.mappingServiceRef = mappingServiceRef;
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * @param task parsing task containing the necessary parameters (for example, group identifier)
     * @return schedule object {@link Schedule}, received after the data mapping from the API
     * @throws Exception the exception that may occur when performing HTTP request or data mapping
     */
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
        return mappingServiceRef.map(bmstuApiResponse);
    }


    /**
     * @param getRequest HTTP-type request GET
     * @return string JSON, containing the response from the server. If the answer is empty, Null returns
     * @throws IOException the exception that may occur when performing HTTP-request
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
