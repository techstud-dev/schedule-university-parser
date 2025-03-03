package com.techstud.sch_parser.domain.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuApiResponse;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuSchedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Component("TLTSU")
@Slf4j
public class TltsuParser implements Parser {

    private final MappingServiceRef<List<TltsuApiResponse>> mappingServiceRef;

    private final CloseableHttpClient httpClient;


    /**
     * @param mappingServiceRef mapping service implemented through Spring. Annotation{@link Qualifier} indicates the specific implementation of the service
     * @param httpClient HTTP-client to execute requests
     */
    public TltsuParser(
            @Qualifier("TItsuServiceImpl") MappingServiceRef<List<TltsuApiResponse>> mappingServiceRef,
            CloseableHttpClient httpClient) {
        this.mappingServiceRef = mappingServiceRef;
        this.httpClient = httpClient;
    }

    /**
     * @param task parsing task containing the necessary parameters (for example, group identifier)
     * @return schedule object {@link Schedule}, received after the mapping data from API
     * @throws Exception the exception that may occur when performing HTTP-call or data mapping
     */
    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        String[] dates = getRequestDates();
        String genericUrl = "https://its.tltsu.ru/api/schedule/group?groupId={0}&fromDate={1}&toDate={2}";
        String oddUrl = MessageFormat.format(genericUrl, task.getGroupId(), dates[0], dates[1]);
        String evenUrl = MessageFormat.format(genericUrl, task.getGroupId(), dates[2], dates[3]);

        HttpGet getOddRequest = new HttpGet(oddUrl);
        HttpGet getEvenRequest = new HttpGet(evenUrl);

        log.info("Connect to TLTSU API: evenUrl: {}, oddEven: {}", getEvenRequest, getOddRequest);
        String oddJson = getSchduleJsonAsString(getOddRequest);
        String evenJson = getSchduleJsonAsString(getEvenRequest);

        ObjectMapper mapper = new ObjectMapper();
        List<TltsuSchedule> oddResponse = mapper.readValue(oddJson, new TypeReference<>() {
        });
        List<TltsuSchedule> evenResponse = mapper.readValue(evenJson, new TypeReference<>() {
        });
        TltsuApiResponse oddResponseApi = new TltsuApiResponse();
        TltsuApiResponse evenResponseApi = new TltsuApiResponse();
        oddResponseApi.setSchedules(oddResponse);
        evenResponseApi.setSchedules(evenResponse);

        log.info("Successfully fetching data from TLTSU API");
        return mappingServiceRef.map(List.of(oddResponseApi, evenResponseApi));
    }

    /**
     * <p>Dates are adjusted taking into account the temporary zone Samara (UTC+4)</p>
     * @return the array of lines containing dates in a format suitable for requests for the API
     */
    private String[] getRequestDates() {
        //У ТГУ очень странно формируется расписание, оно как бы в UTC+0, но с +04:00
        ZoneId samaraZone = ZoneId.of("Europe/Samara");
        DateTimeFormatter formatterCurrent = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'20:00:00.000X")
                .withZone(samaraZone);

        DateTimeFormatter formatterNext = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'19:59:59.999X")
                .withZone(samaraZone);

        Instant now = Instant.now();
        String currentDateStart = formatterCurrent.format(now);

        Instant nextWeek = now.plus(7, ChronoUnit.DAYS);
        String currentDateEnd = formatterNext.format(nextWeek);
        String nextWeekDateStart = formatterCurrent.format(nextWeek);

        Instant nextWeekEnd = nextWeek.plus(7, ChronoUnit.DAYS);
        String nextWeekDateEnd = formatterNext.format(nextWeekEnd);

        return Arrays.stream(new String[]{currentDateStart, currentDateEnd, nextWeekDateStart, nextWeekDateEnd})
                .map(date -> date.replace("+04", "Z")).toArray(String[]::new);
    }


    /**
     * @param getRequest HTTP-request type GET.
     * @return string JSON,containing the response from the server. If the answer is empty, null returns
     * @throws IOException the exception that may occur when performing HTTP request
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
