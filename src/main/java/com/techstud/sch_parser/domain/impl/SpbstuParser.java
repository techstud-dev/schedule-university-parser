package com.techstud.sch_parser.domain.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Component("SPBSTU")
@RequiredArgsConstructor
@Slf4j
public class SpbstuParser implements Parser {

    private final MappingService mappingService;

    /**
     * @param task таска на парсинг расписания
     * @return возвращает объект расписания
     * @throws Exception исключения в случае если парсинг вернет какие-либо ошибки
     */
    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        String baseScheduleUrl = "https://ruz.spbstu.ru/faculty/123/groups/{0}";
        String scheduleUrl = MessageFormat.format(baseScheduleUrl, task.getGroupId());
        log.info("Connecting to SPBSTU timetable page: {}", scheduleUrl);

        log.info("Successfully parsing data from NSU API");
        return mappingService.mapSpbstuScheduleByScheduleDay(returnListWeek(scheduleUrl));
    }

    /**
     * @param scheduleUrl
     * @return List<Document> возвращает html документы для дальнейшего парсинга
     * @throws IOException в случае если произошли какие то ошибки при парсинге
     */
    public List<Document> returnListWeek(String scheduleUrl) throws IOException {
        Document document = Jsoup.connect(scheduleUrl).get();
        log.info("Successfully fetched timetable page from SPBSTU");

        List<Document> docs = new ArrayList<>();

        JsonNode returnJsonString = document.stream()
                .map(element -> element.html().trim())
                .filter(scriptContent -> scriptContent.startsWith("window.__INITIAL_STATE__ ="))
                .findFirst()
                .map(element -> {
                    try {
                        return new ObjectMapper().readTree(element
                                .replace("window.__INITIAL_STATE__ =", "")
                                .replaceAll(";$", "")
                                .trim()).path("lessons").path("week");
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).orElseThrow(() -> new RuntimeException("Null response body from API."));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Week week = objectMapper.treeToValue(returnJsonString, Week.class);

        String oddURL = scheduleUrl + "?date=" + week.dateStart;
        String evenURL = scheduleUrl + "?date=" + week.dateEnd.plusDays(1L);

        log.info("Successfully fetched timetable page from SPBSTU {}", oddURL);
        log.info("Successfully fetched timetable page from SPBSTU {}", evenURL);

        Document oddDocument = Jsoup.connect(oddURL).get();
        Document evenDocument = Jsoup.connect(evenURL).get();

        docs.add(oddDocument);
        docs.add(evenDocument);

        return docs;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Week{
        @JsonFormat(pattern = "yyyy.MM.dd")
        @JsonProperty("date_start")
        private LocalDate dateStart;

        @JsonFormat(pattern = "yyyy.MM.dd")
        @JsonProperty("date_end")
        private LocalDate dateEnd;

        @JsonProperty("is_odd")
        private boolean idOdd;
    }
}