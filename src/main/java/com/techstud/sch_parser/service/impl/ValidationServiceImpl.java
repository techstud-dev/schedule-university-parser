package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.exception.RequestException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.ScheduleDay;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import com.techstud.sch_parser.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ValidationServiceImpl implements ValidationService {

    @Override
    public void validateSchedule(Schedule schedule) throws EmptyScheduleException {
        if (isScheduleEmpty(schedule)) {
            logAndThrowEmptyScheduleException();
        }

        Set<DayOfWeek> emptyEvenWeekDays = findEmptyDays(schedule.getEvenWeekSchedule());
        Set<DayOfWeek> emptyOddWeekDays = findEmptyDays(schedule.getOddWeekSchedule());

        if (isCompletelyEmpty(emptyEvenWeekDays, schedule.getOddWeekSchedule().keySet()) &&
                isCompletelyEmpty(emptyOddWeekDays, schedule.getEvenWeekSchedule().keySet())) {
            logAndThrowEmptyScheduleException();
        }
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

    private boolean isScheduleEmpty(Schedule schedule) {
        return schedule == null ||
                (schedule.getEvenWeekSchedule().isEmpty() && schedule.getOddWeekSchedule().isEmpty());
    }

    private void logAndThrowEmptyScheduleException() throws EmptyScheduleException {
        log.error("Empty schedule");
        throw new EmptyScheduleException("Empty schedule");
    }

    private Set<DayOfWeek> findEmptyDays(Map<DayOfWeek, ScheduleDay> weekSchedule) {
        return weekSchedule.entrySet().stream()
                .filter(entry -> isScheduleDayEmpty(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private boolean isScheduleDayEmpty(ScheduleDay scheduleDay) {
        return scheduleDay == null ||
                scheduleDay.getLessons() == null ||
                scheduleDay.getLessons().values().stream().allMatch(List::isEmpty);
    }

    private boolean isCompletelyEmpty(Set<DayOfWeek> emptyDays, Set<DayOfWeek> weekDays) {
        return emptyDays.containsAll(weekDays);
    }
}
