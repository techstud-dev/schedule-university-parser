package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;

@Component("PGUPS")
@Slf4j
public class PgupsParser implements Parser {

    private final MappingServiceRef<List<Document>> mappingService;

    public PgupsParser(@Qualifier("pgupsMappingServiceImpl") MappingServiceRef<List<Document>> mappingService) {
        this.mappingService = mappingService;
    }

    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        String urlFormat = "https://rasp.pgups.ru/schedule/group/{0}/{1}?odd={2}";
        String oddUrl = MessageFormat.format(urlFormat,task.getGroupId(), task.getSubGroupId() == null ? "" : task.getSubGroupId(), "1");
        String evenUrl = MessageFormat.format(urlFormat,task.getGroupId(), task.getSubGroupId() == null ? "" : task.getSubGroupId(), "0");
        log.info("Connect to PGUPS API: evenUrl: {}, oddEven: {}", evenUrl, oddUrl);
        Document evenWeekDocument = Jsoup.connect(evenUrl)
                .userAgent(userAgent)
                .referrer(referrer)
                .get();
        Document oddWeekDocument = Jsoup.connect(oddUrl).userAgent(userAgent).referrer(referrer).get();
        log.info("Successfully fetching data from PGSPU API");
        return mappingService.map(List.of(evenWeekDocument, oddWeekDocument));
    }
}
