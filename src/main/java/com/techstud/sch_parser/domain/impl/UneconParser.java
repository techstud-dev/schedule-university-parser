package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component("UNECON")
@Slf4j
@RequiredArgsConstructor
public class UneconParser implements Parser {

    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        String apiRequest = "https://rasp.unecon.ru/raspisanie_grp.php?g={0}&w={1}";

        String[] weekNumbers = getCurrentWeekNumbers();

        String currentWeekUrl = MessageFormat.format(apiRequest, task.getGroupId(), weekNumbers[0]);
        String nextWeekUrl = MessageFormat.format(apiRequest, task.getGroupId(), weekNumbers[1]);

        log.info("Connect to UNECON API: currentWeek: {}, nextWeek: {}",  currentWeekUrl, nextWeekUrl);

        Document currentWeekDoc = Jsoup.connect(currentWeekUrl).get();
        Document nextWeekDoc = Jsoup.connect(nextWeekUrl).get();

        log.info("Successfully parsing data from UNECON API");
        return mappingService.mapUneconToSchedule(List.of(currentWeekDoc, nextWeekDoc));
    }

    private String[] getCurrentWeekNumbers() {
        LocalDate startSemesterDate = LocalDate.of(2024, Month.SEPTEMBER, 2);
        LocalDate currentDate = LocalDate.now();

        if (currentDate.isBefore(startSemesterDate)) {
            return new String[]{"0", "0"};
        }

        long daysBetween = ChronoUnit.DAYS.between(startSemesterDate, currentDate);
        long weeksBetween = daysBetween / 7;
        int currentWeek = (int) weeksBetween + 1;

        if (daysBetween % 7 != 0) {
            currentWeek += 1;
        }

        if (currentWeek % 2 == 0) {
            return new String[]{String.valueOf(currentWeek), String.valueOf(currentWeek + 1)};
        } else {
            return new String[]{String.valueOf(currentWeek - 1), String.valueOf(currentWeek)};
        }
    }
}
