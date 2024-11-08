package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component("MIIT")
@RequiredArgsConstructor
@Slf4j
public class MiitParser implements Parser {

    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        log.info("Parsing schedule started for group: {}", task.getGroupId());

        String scheduleUrl = "https://www.miit.ru/timetable/{0}";
        String url = MessageFormat.format(scheduleUrl, task.getGroupId());

        log.info("Connecting to MIIT timetable page: {}", url);
        Document document = Jsoup.connect(url).get();

        boolean isCurrentWeekEven = isWeekEven(LocalDate.now());

        Element currentWeekElement = document.select("#week-1").first();
        Element nextWeekElement = document.select("#week-2").first();

        List<Document> weekDocuments = new ArrayList<>();

        if (isCurrentWeekEven) {
            weekDocuments.add(parseHtmlContent(currentWeekElement));
            weekDocuments.add(parseHtmlContent(nextWeekElement));
        } else {
            weekDocuments.add(parseHtmlContent(nextWeekElement));
            weekDocuments.add(parseHtmlContent(currentWeekElement));
        }

        Schedule schedule = mappingService.mapMiitToSchedule(weekDocuments);

        log.info("Finished parsing schedule for group: {}", task.getGroupId());
        return schedule;
    }

    private boolean isWeekEven(LocalDate date) {
        LocalDate startSemesterDate = LocalDate.of(2024, Month.SEPTEMBER, 2);
        long weeksBetween = ChronoUnit.WEEKS.between(startSemesterDate, date);
        return weeksBetween % 2 == 0;
    }

    private Document parseHtmlContent(Element weekElement) {
        return Jsoup.parse(weekElement.html());
    }
}
