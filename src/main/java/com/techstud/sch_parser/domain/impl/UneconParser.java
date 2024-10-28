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
import java.util.Calendar;

@Component
@Slf4j
@RequiredArgsConstructor
public class UneconParser implements Parser {

    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(String groupId) throws Exception {
        String genericRequestUrl = "https://rasp.unecon.ru/raspisanie_grp.php?g={0}&semestr={1}";
        String requestUrl = MessageFormat.format(genericRequestUrl, groupId,
                Calendar.getInstance().get(Calendar.MONTH) >= 9 ? "1" : "2");

        Document document = Jsoup
                .connect(requestUrl)
                .userAgent(userAgent)
                .referrer(referrer)
                .get();

        return mappingService.mapUneconToSchedule(document);
    }
}
