package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.exception.RequestException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.api.response.mapping.LessonDto;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class ValidationServiceImpl implements ValidationService {

    @Override
    public void validateSchedule(Schedule schedule) throws EmptyScheduleException {
        if (isScheduleCompletelyEmpty(schedule)) {
            logAndThrowEmptyScheduleException();
        }

        validateWeekDays(schedule.getEvenWeekSchedule(), "even");
        validateWeekDays(schedule.getOddWeekSchedule(), "odd");
    }

    @Override
    public void validateRequest(ParsingTask request, Set<String> parserList) throws RequestException {
        if (request == null) {
            log.error("Empty request");
            throw new RequestException("Empty request");
        }

        if (request.getUniversityName() == null || request.getGroupId() == null) {
            log.error("Empty university name or group id");
            throw new RequestException("Empty university name or group id");
        }

        if (!parserList.contains(request.getUniversityName())) {
            log.error("Parser for university {} not found or not supported", request.getUniversityName());
            throw new RequestException("Parser for university " + request.getUniversityName() + " not found or not supported");
        }
    }

    private boolean isScheduleCompletelyEmpty(Schedule schedule) {
        return isWeekScheduleEmpty(schedule.getEvenWeekSchedule()) &&
                isWeekScheduleEmpty(schedule.getOddWeekSchedule());
    }

    private void validateWeekDays(Map<String, Map<String, LessonDto>> weekSchedule, String weekType) throws EmptyScheduleException {
        if (isWeekScheduleEmpty(weekSchedule)) {
            log.warn("Schedule for {} week is empty", weekType);
        }
    }

    private boolean isWeekScheduleEmpty(Map<String, Map<String, LessonDto>> weekSchedule) {
        return weekSchedule == null || weekSchedule.isEmpty() ||
                weekSchedule.values().stream()
                        .allMatch(day -> day == null || day.isEmpty());
    }

    private void logAndThrowEmptyScheduleException() throws EmptyScheduleException {
        log.error("Schedule is completely empty");
        throw new EmptyScheduleException("Schedule is completely empty");
    }
}
