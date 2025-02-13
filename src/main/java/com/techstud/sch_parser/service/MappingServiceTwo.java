package com.techstud.sch_parser.service;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.Schedule;

public interface MappingServiceTwo<T> {
    Schedule map(T source) throws EmptyScheduleException;
}
