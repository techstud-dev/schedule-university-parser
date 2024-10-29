package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ParserFactory {

    private final Map<String, Parser> parsers;

    public Parser getParser(ParsingTask task) {
        Parser parser = parsers.get(task.getUniversityName());
        if (parser == null) {
            throw new IllegalArgumentException("Parser for university " + task.getUniversityName() + " not found");
        }
        return parser;
    }
}
