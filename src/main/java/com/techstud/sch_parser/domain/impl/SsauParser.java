package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.exception.EmptyScheduleException;
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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

@Component("SSAU")
@Slf4j
public class SsauParser implements Parser {

    private final MappingServiceRef<List<Document>> mappingService;

    public SsauParser(@Qualifier("ssauMappingServiceImpl") MappingServiceRef<List<Document>> mappingService) {
        this.mappingService = mappingService;
    }

    @Override
    public Schedule parseSchedule(ParsingTask task) throws IOException, EmptyScheduleException {
        String[] parseWeeks = getCurrentWeekNumbers(TimeZone.getTimeZone("Europe/Samara"));
        final String[] evenParameters = {String.valueOf(task.getGroupId()), parseWeeks[0]};
        final String[] oddParameters = {String.valueOf(task.getGroupId()), parseWeeks[1]};

        String samaraUniversityScheduleUrl = "https://ssau.ru/rasp?groupId={0}&selectedWeek={1}";

        final String evenUrl = MessageFormat.format(samaraUniversityScheduleUrl, evenParameters[0], evenParameters[1]);
        final String oddUrl = MessageFormat.format(samaraUniversityScheduleUrl, oddParameters[0], oddParameters[1]);
        log.info("Connect to SSAU API: evenUrl: {}, oddEven: {}", evenUrl, oddUrl);
        Document evenDoc = Jsoup.connect(evenUrl).userAgent(userAgent).referrer(referrer).get();

        Document oddDoc = Jsoup.connect(oddUrl).userAgent(userAgent).referrer(referrer).get();
        log.info("Successfully fetching data from SSAU API");
        return mappingService.map(List.of(evenDoc, oddDoc));
    }

    private String[] getCurrentWeekNumbers(TimeZone timeZone) {
        String[] parseWeeks;
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeZone(timeZone);
        Calendar startEtudeCalendar;

        if (currentCalendar.get(Calendar.MONTH) >= 9) {
            startEtudeCalendar = new GregorianCalendar(currentCalendar.get(Calendar.YEAR), Calendar.SEPTEMBER, 1);

        } else {
            startEtudeCalendar = new GregorianCalendar(currentCalendar.get(Calendar.YEAR), Calendar.FEBRUARY, 1);
        }
        startEtudeCalendar.setTimeZone(timeZone);
        int currentGlobalNumberWeek = currentCalendar.get(Calendar.WEEK_OF_YEAR);
        int startGlobalNumberWeek = startEtudeCalendar.get(Calendar.WEEK_OF_YEAR);
        int currentEtudeWeek = currentGlobalNumberWeek - startGlobalNumberWeek;

        if (currentEtudeWeek % 2 != 0) {
            parseWeeks = new String[]{String.valueOf(currentEtudeWeek), String.valueOf(currentEtudeWeek + 1)};
        } else {
            parseWeeks = new String[]{String.valueOf(currentEtudeWeek - 1), String.valueOf(currentEtudeWeek)};
        }
        return parseWeeks;
    }
}
