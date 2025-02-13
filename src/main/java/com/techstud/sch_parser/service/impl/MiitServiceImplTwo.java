package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.service.MappingServiceTwo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.swing.text.Document;
import java.util.List;

@Slf4j
@Service
public class MiitServiceImplTwo implements MappingServiceTwo<List<Document>> {

    @Override
    public Schedule map(List<Document> source) throws EmptyScheduleException {
        // some mapping logic here

        return null;
    }

    // some mapping logic here
}
