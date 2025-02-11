package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

@Component("MEPHI")
@Slf4j
@RequiredArgsConstructor
public class MephiParser implements Parser {

    @Qualifier("mephiServiceImpl")
    private final MappingServiceRef<List<Document>> mappingService;

    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        String[] parseWeeks = getCurrentWeekNumbers(TimeZone.getTimeZone("Europe/Moscow"));
        final String[] evenParameters = {String.valueOf(task.getGroupId()), parseWeeks[0]};
        final String[] oddParameters = {String.valueOf(task.getGroupId()), parseWeeks[1]};

        String mephiScheduleUrl = "https://home.mephi.ru/study_groups/{0}/schedule?period={1}";

        final String evenUrl = MessageFormat.format(mephiScheduleUrl, evenParameters[0], evenParameters[1]);
        final String oddUrl = MessageFormat.format(mephiScheduleUrl, oddParameters[0], oddParameters[1]);

        log.info("Connect to MEPHI API: evenUrl: {}, oddEven: {}", evenUrl, oddUrl);

        Document evenDoc = Jsoup.connect(evenUrl).get();
        Document oddDoc = Jsoup.connect(oddUrl).get();

        log.info("Successfully fetching data from MEPHI API");
        return mappingService.map(List.of(evenDoc, oddDoc));
    }

    private String[] getCurrentWeekNumbers(TimeZone timeZone) {
        log.info(timeZone.toString());
        String[] parseWeeks;
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTimeZone(timeZone);
        Calendar startEtudeCalendar;

        if (currentCalendar.get(Calendar.MONTH) >= Calendar.SEPTEMBER) {
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
