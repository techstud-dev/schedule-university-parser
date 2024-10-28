package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.service.MappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
@RequiredArgsConstructor
public class NsuParser implements Parser {
    private final MappingService mappingService;

    @Override
    public Schedule parseSchedule(String groupId) throws Exception {
        String baseUrl = "https://table.nsu.ru/group/{0}";
        String url = MessageFormat.format(baseUrl, groupId);

        log.info("Parsing URL: " + url);

        Document doc = Jsoup.connect(url).get();

        log.info("Document loaded");

        Element parityElement = doc.selectFirst("div.parity");
        if (parityElement == null) {
            throw new Exception("Не удалось найти информацию о четности недели на странице.");
        }

        String parityText = parityElement.text();
        boolean isEvenWeek = parityText.contains("Чётная");

        log.info("Detected week type: " + (isEvenWeek ? "Чётная" : "Нечётная"));

        return mappingService.mapNsuToSchedule(doc);
    }
}
