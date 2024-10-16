package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.service.MappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SsauParser implements Parser {

    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(Long groupId) throws IOException {

        final String[] evenParameters = {String.valueOf(groupId), "2"};
        final String[] oddParameters = {String.valueOf(groupId), "1"};

        String samaraUniversityScheduleUrl = "https://ssau.ru/rasp?groupId={0}&selectedWeek={1}";

        final String evenUrl = MessageFormat.format(samaraUniversityScheduleUrl, evenParameters[0], evenParameters[1]);
        final String oddUrl = MessageFormat.format(samaraUniversityScheduleUrl, oddParameters[0], oddParameters[1]);
        try {
            Document evenDoc = Jsoup.connect(evenUrl).userAgent(userAgent).referrer(referrer).get();

            Document oddDoc = Jsoup.connect(oddUrl).userAgent(userAgent).referrer(referrer).get();

            return mappingService.mapSsauToSchedule(List.of(evenDoc, oddDoc));
        } catch (HttpStatusException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
