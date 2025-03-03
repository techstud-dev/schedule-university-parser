package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.ScheduleDay;
import com.techstud.sch_parser.model.ScheduleObject;
import com.techstud.sch_parser.model.TimeSheet;
import com.techstud.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.techstud.sch_parser.model.api.response.sseu.SseuLessonDay;
import com.techstud.sch_parser.model.api.response.sseu.SseuSubject;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static com.techstud.sch_parser.util.ScheduleLessonTypeParse.mapSseuLessonTypeToScheduleType;

@Service
@Slf4j
public class SseuServiceImpl implements MappingServiceRef<List<SseuApiResponse>> {

    /**
     * @param source the list of answers from the API of the Samara State Economic University contains HTML elements
     * @return Schedule returns the formed schedule for sseu
     * @throws EmptyScheduleException an error of empty schedule
     */
    @Override
    public Schedule map(List<SseuApiResponse> source) throws EmptyScheduleException {
        log.info("Start mapping SSEU data to schedule");
        SseuApiResponse oddWeekSseuSchedule = source.get(0).getWeek().equals("ODD") ? source.get(0) : source.get(1);
        SseuApiResponse evenWeekSseuSchedule = source.get(0).getWeek().equals("EVEN") ? source.get(0) : source.get(1);
        Schedule schedule = new Schedule();

        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = createSseuWeekScheduleWithoutLessons(evenWeekSseuSchedule);
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = createSseuWeekScheduleWithoutLessons(oddWeekSseuSchedule);

        evenWeekSchedule = fillSseuSchedule(evenWeekSseuSchedule, evenWeekSchedule);
        oddWeekSchedule = fillSseuSchedule(oddWeekSseuSchedule, oddWeekSchedule);

        schedule.setSnapshotDate(new Date());
        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        log.info("Mapping SSEU data to schedule {} finished", schedule);
        return schedule;
    }

    /**
     * @param sseuSchedule the response from the API of the Samara State Economic University contains HTML elements
     * @return Map<DayOfWeek, ScheduleDay> returns a week when DayOfWeek is day of the week, ScheduleDay is schedule for a specific day
     */
    private Map<DayOfWeek, ScheduleDay> createSseuWeekScheduleWithoutLessons(SseuApiResponse sseuSchedule) {
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

    /**
     * @param dateString text presentation of the date
     * @return Date returns the date after parsing
     */
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

    /**
     * @param sseuSchedule the response from the API of the Samara State Economic University contains HTML elements
     * @param weekSchedule a week where DayOfWeek is day of the week and ScheduleDay is schedule for a specific day
     * @return Map<DayOfWeek, ScheduleDay>
     */
    private Map<DayOfWeek, ScheduleDay> fillSseuSchedule(SseuApiResponse sseuSchedule, Map<DayOfWeek, ScheduleDay> weekSchedule) {
        for (int i = 0; i < sseuSchedule.getBody().size(); i++) {
            LocalTime from = LocalTime.parse(sseuSchedule.getBody().get(i).getName());

            int finalI = i;
            sseuSchedule.getBody().get(i).getDaySchedule().forEach((day, daySchedule) -> {
                if (weekSchedule.containsKey(DayOfWeek.valueOf(day.toUpperCase()))) {
                    DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
                    Map<TimeSheet, List<ScheduleObject>> lessons = weekSchedule.get(dayOfWeek).getLessons();
                    TimeSheet timeSheet = getSseuTimeSheet(from, lessons.keySet());
                    if (timeSheet == null) {
                        timeSheet = new TimeSheet();
                        timeSheet.setFrom(from);
                        timeSheet.setTo(null);
                        lessons.put(timeSheet, new ArrayList<>());
                    }
                    List<ScheduleObject> scheduleObjects = lessons.get(timeSheet);
                    ScheduleObject addedScheduleObject = mapSseuLessonToScheduleObject(sseuSchedule.getBody().get(finalI).getDaySchedule().get(day));
                    assert addedScheduleObject != null;
                    if (addedScheduleObject.getName() != null && addedScheduleObject.getType() != null) {
                        if(addedScheduleObject.getPlace() == null){
                            addedScheduleObject.setPlace("No audience specified");
                        }
                        scheduleObjects.add(addedScheduleObject);
                    }
                }
            });
        }
        Map<DayOfWeek, ScheduleDay> result = new LinkedHashMap<>();
        weekSchedule.forEach((day, scheduleDay) -> {
            Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();
            List<TimeSheet> sortedTimeSheets = new ArrayList<>(scheduleDay.getLessons().keySet());
            sortedTimeSheets.sort(Comparator.comparing(TimeSheet::getFrom));

            TimeSheet previous = null;
            for (TimeSheet current : sortedTimeSheets) {
                if (previous != null) {
                    previous.setTo(current.getFrom());
                }
                lessons.put(current, scheduleDay.getLessons().get(current));
                previous = current;
            }
            if (previous != null) {
                previous.setTo(previous.getFrom().plusHours(1).plusMinutes(45));
            }

            ScheduleDay newScheduleDay = new ScheduleDay();
            newScheduleDay.setDate(scheduleDay.getDate());
            newScheduleDay.setLessons(lessons);
            result.put(day, newScheduleDay);
        });

        if (!result.containsKey(DayOfWeek.SUNDAY)) {
            Map<TimeSheet, List<ScheduleObject>> scheduleTimeSheetMap = result.get(DayOfWeek.SATURDAY).getLessons();
            scheduleTimeSheetMap.replaceAll((key, value) -> new ArrayList<>());
            ScheduleDay sunDayScheduleDay = new ScheduleDay();
            Date saturdayDate = result.get(DayOfWeek.SATURDAY).getDate();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(saturdayDate);
            calendar.add(Calendar.HOUR_OF_DAY, 24);
            sunDayScheduleDay.setDate(calendar.getTime());
            sunDayScheduleDay.setLessons(scheduleTimeSheetMap);
            result.put(DayOfWeek.SUNDAY, sunDayScheduleDay);
        }
        return result;
    }

    /**
     * @param daySchedule information on lessons in SSEU
     * @return ScheduleObject returns an element of the lesson
     */
    private ScheduleObject mapSseuLessonToScheduleObject(List<SseuLessonDay> daySchedule) {
        ScheduleObject scheduleObject = new ScheduleObject();
        if (daySchedule == null || daySchedule.isEmpty()) {
            return null;
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

    /**
     * @param from date from search for TimeSheet
     * @param keySet TimeSheet list for search
     * @return TimeSheet returns the found TimeSheet otherwise null
     */
    private TimeSheet getSseuTimeSheet(LocalTime from, Set<TimeSheet> keySet) {
        return keySet.stream()
                .filter(timeSheet -> timeSheet.getFrom().equals(from))
                .findFirst()
                .orElse(null);
    }
}
