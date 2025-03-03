package com.techstud.sch_parser.domain.impl;

import com.techstud.sch_parser.domain.Parser;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;

@Component("PGUPS")
@Slf4j
public class PgupsParser implements Parser {

    private final MappingServiceRef<List<Document>> pgpusService;

    /**
     * <p>Constructs a {@link PgupsParser} with the specified PGUPS mapping service.</p>
     *
     * @param pgpusService the {@link MappingServiceRef} responsible for processing
     *                     PGUPS schedule data, expected to handle {@code List<Document>}.
     */
    public PgupsParser(
            @Qualifier("pgpusServiceImpl") MappingServiceRef<List<Document>> pgpusService) {
        this.pgpusService = pgpusService;
    }


    /**
     * <p>Fetches and parses the schedule from the PGUPS API based on the given task parameters.</p>
     *
     * @param task the {@link ParsingTask} containing group and subgroup identifiers.
     * @return a {@link Schedule} object parsed from the retrieved HTML documents.
     * @throws Exception if an error occurs while connecting to the API or parsing the response.
     */
    @Override
    public Schedule parseSchedule(ParsingTask task) throws Exception {
        String urlFormat = "https://rasp.pgups.ru/schedule/group/{0}/{1}?odd={2}";
        String oddUrl = MessageFormat.format(urlFormat,task.getGroupId(), task.getSubGroupId() == null ? "" : task.getSubGroupId(), "1");
        String evenUrl = MessageFormat.format(urlFormat,task.getGroupId(), task.getSubGroupId() == null ? "" : task.getSubGroupId(), "0");
        log.info("Connect to PGUPS API: evenUrl: {}, oddEven: {}", evenUrl, oddUrl);
        Document evenWeekDocument = Jsoup.connect(evenUrl)
                .userAgent(userAgent)
                .referrer(referrer)
                .get();
        Document oddWeekDocument = Jsoup.connect(oddUrl).userAgent(userAgent).referrer(referrer).get();
        log.info("Successfully fetching data from PGSPU API");
        return pgpusService.map(List.of(evenWeekDocument, oddWeekDocument));
    }
}
