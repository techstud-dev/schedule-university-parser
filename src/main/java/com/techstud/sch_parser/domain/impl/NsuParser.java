package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingService;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component("NSU")
public class NsuParser implements Parser {

    private final MappingServiceRef<Document> mappingService;

    public NsuParser(@Qualifier("nsuServiceImpl") MappingServiceRef<Document> mappingService) {
        this.mappingService = mappingService;
    }

    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        String baseUrl = "https://table.nsu.ru/group/{0}";
        String url = MessageFormat.format(baseUrl, task.getGroupId());

        log.info("Connect to NSU API: {}", url);

        Document doc = Jsoup.connect(url).get();
        Element parityElement = doc.selectFirst("div.parity");
        if (parityElement == null) {
            throw new Exception("Не удалось найти информацию о четности недели на странице.");
        }

        log.info("Successfully fetching data from NSU API");
        return mappingService.map(doc);
    }
}
