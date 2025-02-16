package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.*;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuApiResponse;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuGroup;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuSchedule;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class TItsuServiceImpl implements MappingServiceRef<List<TltsuApiResponse>> {
    /**
     * @param source list of answers from API TLSU, containing schedule data for even and odd weeks
     * @return schedule object{@link Schedule}, containing data for even and odd weeks
     * @throws EmptyScheduleException an exception thrown out if the schedule is empty
     */
    @Override
    public Schedule map(List<TltsuApiResponse> source) throws EmptyScheduleException {
        TltsuApiResponse oddSchedule = source.get(0);
        TltsuApiResponse evenSchedule = source.get(1);

        if (oddSchedule.getSchedules().isEmpty() && evenSchedule.getSchedules().isEmpty()) {
            return null;
        }
        Schedule schedule = new Schedule();
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = new LinkedHashMap<>();
        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = new LinkedHashMap<>();
        if (!oddSchedule.getSchedules().isEmpty()) {
            oddWeekSchedule = getWeekScheduleFromTltsu(oddSchedule);
        }

        if (!evenSchedule.getSchedules().isEmpty()) {
            evenWeekSchedule = getWeekScheduleFromTltsu(evenSchedule);
        }

        schedule.setOddWeekSchedule(oddWeekSchedule);
        schedule.setEvenWeekSchedule(evenWeekSchedule);
        return schedule;
    }

    /**
     * @param tltsuApiResponse the answer from the API TLSU containing the schedule for one week.
     * @return the structure {@link Map}, where the key is the {@link DayOfWeek}, and the value is an object {@link ScheduleDay} containing a schedule for this day
     */
    private Map<DayOfWeek, ScheduleDay> getWeekScheduleFromTltsu(TltsuApiResponse tltsuApiResponse) {
        Map<DayOfWeek, ScheduleDay> weekSchedule = new LinkedHashMap<>();
        tltsuApiResponse.getSchedules().forEach(schedule -> {
            DayOfWeek currentDayOfWeek = getDayOfWeekTltsu(schedule);
            weekSchedule.put(currentDayOfWeek, addScheduleFromTltsu(weekSchedule.get(currentDayOfWeek), schedule));
        });

        return weekSchedule;
    }

    /**
     * @param scheduleDay the object of the schedule day in which the element is added. If null, a new object is created
     * @param schedule the schedule element that needs to be added
     * @return updated object {@Link ScheduleDay} containing an added schedule element
     */
    private ScheduleDay addScheduleFromTltsu(ScheduleDay scheduleDay, TltsuSchedule schedule) {
        if (scheduleDay == null) {
            scheduleDay = new ScheduleDay();
        }
        TimeSheet timeSheet = getSseuTimeSheet(schedule);
        Map<TimeSheet, List<ScheduleObject>> lessons = scheduleDay.getLessons();

        if (lessons == null) {
            lessons = new LinkedHashMap<>();
        }
        List<ScheduleObject> scheduleObjects = lessons.computeIfAbsent(timeSheet, k -> new ArrayList<>());
        scheduleObjects.add(getScheduleObjectFromTltsuSchedule(schedule));
        lessons.put(timeSheet, scheduleObjects);
        scheduleDay.setLessons(lessons);
        scheduleDay.setDate(parseTltsuDate(schedule.getDate()));
        return scheduleDay;
    }

    /**
     * @param schedule schedule element from API TLSU
     * @return object {@link ScheduleObject}, containing information about the discipline, audience, teacher and groups
     */
    private ScheduleObject getScheduleObjectFromTltsuSchedule(TltsuSchedule schedule) {
        ScheduleObject scheduleObject = new ScheduleObject();
        scheduleObject.setName(schedule.getDisciplineName());
        scheduleObject.setPlace(schedule.getClassroom().getName());
        if (schedule.getTeacher() != null) {
            scheduleObject.setTeacher(schedule.getTeacher().getLastName() + " " + schedule.getTeacher().getName() + schedule.getTeacher().getPatronymic());
        }
        scheduleObject.setGroups(schedule.getGroupsList()
                .stream()
                .map(TltsuGroup::getName)
                .toList());
        scheduleObject.setType(getScheduleTypeFromTltsuSchedule(schedule));
        return scheduleObject;
    }

    /**
     * @param schedule schedule element from API TLSU.
     * @return the corresponding type of lesson {@link ScheduleType}. If the type is unknown, returns {@link ScheduleType#UNKNOWN}.
     */
    private ScheduleType getScheduleTypeFromTltsuSchedule(TltsuSchedule schedule) {
        if (schedule.getType() == null) {
            return ScheduleType.UNKNOWN;
        }
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "Лек", ScheduleType.LECTURE,
                "Пр", ScheduleType.PRACTICE,
                "СР", ScheduleType.INDEPENDENT_WORK,
                "ЛР", ScheduleType.LAB
        );
        return scheduleTypeMap.getOrDefault(schedule.getType(), ScheduleType.UNKNOWN);
    }

    /**
     * @param date date line in format "yyyy.MM.dd".
     * @return object {@link Date}, representing the date. If Parsing failed, null returns
     */
    private Date parseTltsuDate(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            log.warn("Error while parsing date {} TLTSU", date);
            return null;
        }
    }

    /**
     * @param schedule schedule element from the API TLSU
     * @return day of the week {@link DayOfWeek}, corresponding to the date from the schedule element.
     */
    private DayOfWeek getDayOfWeekTltsu(TltsuSchedule schedule) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        LocalDate date = LocalDate.parse(schedule.getDate(), formatter);
        return date.getDayOfWeek();
    }

    /**
     * @param schedule schedule element from the API TLSU
     * @return object {@link TimeSheet}, containing the time and end of the lesson
     */
    private TimeSheet getSseuTimeSheet(TltsuSchedule schedule) {
        Instant dateFrom = Instant.parse(schedule.getFromTime());
        Instant dateTo = Instant.parse(schedule.getToTime());
        LocalDateTime localDateTimeFrom = LocalDateTime.ofInstant(dateFrom, ZoneOffset.UTC);
        LocalDateTime localDateTimeTo = LocalDateTime.ofInstant(dateTo, ZoneOffset.UTC);
        return new TimeSheet(localDateTimeFrom.toLocalTime(), localDateTimeTo.toLocalTime());
    }
}
