package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.service.MappingServiceTwo;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MiitServiceImplTwo implements MappingServiceTwo<List<Document>> {

    @Override
    public Schedule map(List<Document> source) {
        // some mapping logic here

        return null;
    }

    // some mapping logic here
}
