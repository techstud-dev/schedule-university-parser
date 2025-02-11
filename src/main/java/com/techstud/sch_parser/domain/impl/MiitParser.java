package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import com.techstud.sch_parser.service.impl.MiitServiceImpl;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("MIIT")
@RequiredArgsConstructor
public class MiitParser implements Parser {

    @Qualifier("miitServiceImpl")
    private final MappingServiceRef<List<Document>> mappingService;

    @Override
    public Schedule parseSchedule(ParsingTask task) throws IOException, EmptyScheduleException {
        log.info("Parsing schedule started for group: {}", task.getGroupId());

        String url = String.format("https://www.miit.ru/timetable/%s", task.getGroupId());
        log.info("Connecting to MIIT timetable page: {}", url);

        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch (IOException e) {
            log.error("Error while connection to url: {}", url, e);
            throw new EmptyScheduleException("Cannot connect to MIIT timetable page");
        }

        Element oddWeekElement = document.select("#week-1").first();
        Element evenWeekElement = document.select("#week-2").first();

        if (oddWeekElement == null || evenWeekElement == null) {
            throw new EmptyScheduleException("MIIT Schedule is empty");
        }

        List<Document> weekDocuments = new ArrayList<>();
        weekDocuments.add(Jsoup.parse(oddWeekElement.html()));
        weekDocuments.add(Jsoup.parse(evenWeekElement.html()));

        Schedule schedule = mappingService.map(weekDocuments);

        log.info("Finished parsing schedule for group: {}", task.getGroupId());
        return schedule;
    }
}
