package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.service.MappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class UneconParser implements Parser {

    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(String groupId) throws Exception {
        String apiRequest = "https://rasp.unecon.ru/raspisanie_grp.php?g={0}&w={1}";

        int currentWeek = calculateWeekParameter(LocalDate.now());
        int nextWeek = currentWeek + 1;

        log.info("Current week: {}", currentWeek);
        log.info("Next week: {}", nextWeek);

        String currentWeekUrl = MessageFormat.format(apiRequest, groupId, currentWeek);
        String nextWeekUrl = MessageFormat.format(apiRequest, groupId, nextWeek);

        Document currentWeekDoc = Jsoup.connect(currentWeekUrl).get();
        Document nextWeekDoc = Jsoup.connect(nextWeekUrl).get();

        return mappingService.mapUneconToSchedule(List.of(currentWeekDoc, nextWeekDoc));
    }

    private int calculateWeekParameter(LocalDate date) {
        LocalDate knownEvenWeekDate = LocalDate.of(2024, 10, 28);

        long daysBetween = ChronoUnit.DAYS.between(knownEvenWeekDate, date);

        long weeksBetween = daysBetween / 7;

        return (int) weeksBetween + 10;
    }
}
