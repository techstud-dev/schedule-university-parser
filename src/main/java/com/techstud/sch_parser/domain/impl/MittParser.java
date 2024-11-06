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

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component("MIIT")
@RequiredArgsConstructor
@Slf4j
public class MittParser implements Parser {

    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        String scheduleUrl = "URL_РАСПИСАНИЯ_МИИТ";

        log.info("Connecting to MIIT timetable page: {}", scheduleUrl);

        Document document = Jsoup.connect(scheduleUrl).get();

        log.info("Successfully fetched timetable page from MIIT");

        boolean isCurrentWeekEven = isWeekEven(LocalDate.now());

        Element currentWeekElement = document.select("#week-1").first();
        Element nextWeekElement = document.select("#week-2").first();

        List<Document> weekDocuments = new ArrayList<>();

        if (isCurrentWeekEven) {
            assert currentWeekElement != null;
            weekDocuments.add(parseHtmlContent(currentWeekElement));
            assert nextWeekElement != null;
            weekDocuments.add(parseHtmlContent(nextWeekElement));
        } else {
            assert nextWeekElement != null;
            weekDocuments.add(parseHtmlContent(nextWeekElement));
            assert currentWeekElement != null;
            weekDocuments.add(parseHtmlContent(currentWeekElement));
        }

        return mappingService.mapMiitToSchedule(weekDocuments);
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
