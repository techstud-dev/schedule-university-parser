package com.techstud.sch_parser.domain;

import com.google.gson.Gson;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ParserFactory {

    private final Map<String, Parser> parsers;

    @PostConstruct
    public void init() {
        Gson gson = new Gson();
        log.info("Bean parserFactory sucess loaded with parsers: {}", gson.toJson(parsers.keySet()));
    }

    public Parser getParser(ParsingTask task) {
        Parser parser = parsers.get(task.getUniversityName());
        if (parser == null) {
            throw new IllegalArgumentException("Parser for university " + task.getUniversityName() + " not found");
        }
        return parser;
    }

    public Set<String> getParserList() {
        return parsers.keySet();
    }
}
