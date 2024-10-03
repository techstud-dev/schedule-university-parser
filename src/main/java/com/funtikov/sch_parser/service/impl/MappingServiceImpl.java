package com.funtikov.sch_parser.service.impl;

import com.funtikov.sch_parser.model.Schedule;
import com.funtikov.sch_parser.model.ScheduleDay;
import com.funtikov.sch_parser.model.ScheduleObject;
import com.funtikov.sch_parser.model.TimeSheet;
import com.funtikov.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.funtikov.sch_parser.service.MappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

@Service
@Slf4j
public class MappingServiceImpl implements MappingService {
    @Override
    public Schedule mapSseuToSchedule(List<SseuApiResponse> weekSseuSchedules) {
        SseuApiResponse oddWeekSseuSchedule = weekSseuSchedules.get(0).getWeek().equals("ODD") ? weekSseuSchedules.get(0) : weekSseuSchedules.get(1);
        SseuApiResponse evenWeekSseuSchedule = weekSseuSchedules.get(0).getWeek().equals("EVEN") ? weekSseuSchedules.get(0) : weekSseuSchedules.get(1);
        Schedule schedule = new Schedule();

        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = createWeekScheduleWithoutLessons(evenWeekSseuSchedule);
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = createWeekScheduleWithoutLessons(oddWeekSseuSchedule);

        fillSchedule(evenWeekSseuSchedule, evenWeekSchedule);
        fillSchedule(oddWeekSseuSchedule, oddWeekSchedule);

        schedule.setSnapshotDate(new Date());
        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        return schedule;
    }

    private Map<DayOfWeek, ScheduleDay> createWeekScheduleWithoutLessons(SseuApiResponse sseuSchedule) {
        Map<DayOfWeek, ScheduleDay> weekSchedule = new LinkedHashMap<>();
        sseuSchedule.getHeaders().forEach(header -> {
            if (!header.getValue().equals("name")) {
                ScheduleDay scheduleDay = new ScheduleDay();
                scheduleDay.setDate(getSseuDate(header.getText()));
                weekSchedule.put(DayOfWeek.valueOf(header.getValue().toUpperCase()), scheduleDay);
            }
        });
        return weekSchedule;
    }

    private Date getSseuDate(String dateString) {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("EEEE dd.MM.yyyy")
                .optionalStart()
                .appendLiteral('г')
                .optionalEnd()
                .optionalStart()
                .appendLiteral('.')
                .optionalEnd()
                .toFormatter(new Locale("ru"));
        LocalDate localDate = LocalDate.parse(dateString, formatter);
        Instant instant = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    private void fillSchedule(SseuApiResponse sseuSchedule, Map<DayOfWeek, ScheduleDay> weekSchedule) {
        sseuSchedule.getBody().forEach(body -> {
            weekSchedule.forEach((day, scheduleDay) -> {
                        if (day.name().equalsIgnoreCase(body.getDaySchedule().keySet().iterator().next())) {
                            Map<TimeSheet, List<ScheduleObject>> lessons = scheduleDay.getLessons();
                            if(lessons.containsKey(new TimeSheet(LocalTime.parse(body.getName(), DateTimeFormatter.ofPattern("HH:mm")), null))) {
                                scheduleDay.getLessons().put(new TimeSheet(LocalTime.parse(body.getName(), DateTimeFormatter.ofPattern("HH:mm")), null),
                                        null); //FIXME: Переделать на добавление урока
                            }
                        }
                    }
            );
        });
    }
}
