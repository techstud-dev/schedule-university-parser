package com.funtikov.sch_parser.service.impl;

import com.funtikov.sch_parser.model.*;
import com.funtikov.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.funtikov.sch_parser.model.api.response.sseu.SseuLessonDay;
import com.funtikov.sch_parser.model.api.response.sseu.SseuSubject;
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
        for (int i = 0; i < sseuSchedule.getBody().size(); i++) {
            LocalTime from = LocalTime.parse(sseuSchedule.getBody().get(i).getName());

            int finalI = i;
            sseuSchedule.getBody().get(i).getDaySchedule().forEach((day, daySchedule) -> {
                if (weekSchedule.containsKey(DayOfWeek.valueOf(day.toUpperCase()))) {
                    DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
                    Map<TimeSheet, List<ScheduleObject>> lessons = weekSchedule.get(dayOfWeek).getLessons();
                    TimeSheet timeSheet = getTimeSheet(from, lessons.keySet());
                    if (timeSheet == null) {
                        timeSheet = new TimeSheet();
                        timeSheet.setFrom(from);
                        timeSheet.setTo(null);
                        lessons.put(timeSheet, new ArrayList<>());
                    }
                    List<ScheduleObject> scheduleObjects = lessons.get(timeSheet);
                    scheduleObjects.add(mapSseuLessonToScheduleObject(sseuSchedule.getBody().get(finalI).getDaySchedule().get(day)));
                }
            });
        }
    }

    private ScheduleObject mapSseuLessonToScheduleObject(List<SseuLessonDay> daySchedule) {
        ScheduleObject scheduleObject = new ScheduleObject();
        if (daySchedule == null || daySchedule.isEmpty()) {
            // Handle the case when daySchedule is empty
            return null; // or return an empty ScheduleObject, depending on your requirements
        }

        SseuLessonDay lessonDay = daySchedule.get(0);

        if (lessonDay.getWorkPlan() != null && lessonDay.getWorkPlan().getDiscipline() != null) {
            scheduleObject.setName(lessonDay.getWorkPlan().getDiscipline().getName());
        }

        if (lessonDay.getWorkPlan() != null && lessonDay.getWorkPlan().getLessonTypes() != null) {
            scheduleObject.setType(mapSseuLessonTypeToScheduleType(lessonDay.getWorkPlan().getLessonTypes().getName()));
        }

        if (lessonDay.getSubject() != null && !lessonDay.getSubject().isEmpty()) {
            SseuSubject subject = lessonDay.getSubject().get(0);

            if (subject.getName() != null) {
                scheduleObject.setTeacher(subject.getName());
            }

            if (subject.getAudiences() != null && !subject.getAudiences().isEmpty()) {
                scheduleObject.setPlace(subject.getAudiences().get(0).getItemName());
            }
        }

        if (lessonDay.getWorkPlan() != null && lessonDay.getWorkPlan().getGroup() != null) {
            String groupName = lessonDay.getWorkPlan().getGroup().getName();
            if (!scheduleObject.getGroups().contains(groupName)) {
                scheduleObject.getGroups().add(groupName);
            }
        }

        return scheduleObject;
    }

    private TimeSheet getTimeSheet(LocalTime from, Set<TimeSheet> keySet) {
        return keySet.stream()
                .filter(timeSheet -> timeSheet.getFrom().equals(from))
                .findFirst()
                .orElse(null);
    }

    private ScheduleType mapSseuLessonTypeToScheduleType(String lessonType) {
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "Лекции", ScheduleType.LECTURE,
                "Практические", ScheduleType.PRACTICE,
                "Пересдача Зачет", ScheduleType.EXAM,
                "Пересдача Экзамен", ScheduleType.EXAM);
        return scheduleTypeMap.get(lessonType);
    }


}
