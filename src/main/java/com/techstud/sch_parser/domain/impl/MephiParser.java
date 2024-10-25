package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.service.MappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class MephiParser implements Parser {

    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(String groupId) throws Exception {
        String evenUrl = "https://home.mephi.ru/study_groups/" + groupId + "/schedule?period=0";
        String oddUrl = "https://home.mephi.ru/study_groups/" + groupId + "/schedule?period=1";

        Document evenDoc = Jsoup.connect(evenUrl).get();
        Document oddDoc = Jsoup.connect(oddUrl).get();

        return mappingService.mapMephiToSchedule(List.of(evenDoc, oddDoc));
    }
}
