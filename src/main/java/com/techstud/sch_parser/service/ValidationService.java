package com.techstud.sch_parser.service;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.exception.RequestException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;

import java.util.Set;

public interface ValidationService {

    void validateSchedule(Schedule schedule) throws EmptyScheduleException;

    void validateRequest(ParsingTask request, Set<String> parserList) throws RequestException;
}
