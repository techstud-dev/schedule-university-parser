package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.ScheduleDay;
import com.techstud.sch_parser.model.ScheduleObject;
import com.techstud.sch_parser.model.TimeSheet;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.techstud.sch_parser.util.ScheduleLessonTypeParse.mapNsuLessonTypeToScheduleType;

@Slf4j
@Service
public class NsuServiceImpl implements MappingServiceRef<Document> {

    @Override
    public Schedule map(Document source) throws EmptyScheduleException {
        log.info("Start mapping NSU data to schedule");
        Schedule schedule = new Schedule();
        TimeSheet lastTimeSheet = null;

        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = new LinkedHashMap<>();
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = new LinkedHashMap<>();
        DayOfWeek[] daysOfWeek = DayOfWeek.values();

        for (DayOfWeek day : daysOfWeek) {
            evenWeekSchedule.put(day, new ScheduleDay());
            oddWeekSchedule.put(day, new ScheduleDay());
        }

        Elements rows = source.select("table.time-table tbody tr");

        for (Element row : rows) {
            lastTimeSheet = getNsuScheduleDay(row, evenWeekSchedule, oddWeekSchedule, daysOfWeek, lastTimeSheet);
        }

        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        log.info("Mapping NSU data to schedule {} finished", schedule);
        return schedule;
    }

    private TimeSheet getNsuScheduleDay(Element row, Map<DayOfWeek, ScheduleDay> evenWeekSchedule,
                                        Map<DayOfWeek, ScheduleDay> oddWeekSchedule,
                                        DayOfWeek[] daysOfWeek, TimeSheet lastTimeSheet) {
        Element timeCell = row.select("td").first();
        if (timeCell != null) {
            String timeText = timeCell.text().trim();
            TimeSheet currentSheet = new TimeSheet(timeText);

            if (lastTimeSheet != null) {
                lastTimeSheet.setTo(currentSheet.getFrom());
            }

            if (row.nextElementSibling() == null) {
                LocalTime fromTime = currentSheet.getFrom();
                LocalTime toTime = fromTime.plusMinutes(110);
                currentSheet.setTo(LocalTime.parse(toTime.format(DateTimeFormatter.ofPattern("HH:mm"))));
            }

            lastTimeSheet = currentSheet;

            for (DayOfWeek day : daysOfWeek) {
                evenWeekSchedule.get(day).getLessons()
                        .computeIfAbsent(currentSheet, k -> new ArrayList<>());
                oddWeekSchedule.get(day).getLessons()
                        .computeIfAbsent(currentSheet, k -> new ArrayList<>());
            }

            Elements dayCells = row.select("td:not(:first-child)");
            int dayIndex = 0;

            for (Element dayCell : dayCells) {
                Elements lessonCells = dayCell.select("div.cell");

                for (Element lessonCell : lessonCells) {
                    List<ScheduleObject> scheduleObjects = getNsuScheduleObjects(lessonCell);
                    String weekIndicator = lessonCell.select(".week").text().trim();

                    for (ScheduleObject scheduleObject : scheduleObjects) {
                        if (weekIndicator.isEmpty()) {
                            evenWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .get(currentSheet).add(scheduleObject);
                            oddWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .get(currentSheet).add(scheduleObject);
                        } else if (weekIndicator.equals("Четная")) {
                            evenWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .get(currentSheet).add(scheduleObject);
                        } else if (weekIndicator.equals("Нечетная")) {
                            oddWeekSchedule.get(daysOfWeek[dayIndex]).getLessons()
                                    .get(currentSheet).add(scheduleObject);
                        }
                    }
                }
                dayIndex++;
            }
        }
        return lastTimeSheet;
    }

    private List<ScheduleObject> getNsuScheduleObjects(Element element) {
        List<ScheduleObject> scheduleObjects = new ArrayList<>();
        Elements lessons = element.select("div.cell");

        for (Element lesson : lessons) {
            String type = lesson.select(".type").text().trim();
            String subject = lesson.select(".subject").text().trim();
            String teacher = lesson.select(".tutor").text().trim();
            String place = lesson.select(".room a").text().trim();

            ScheduleObject scheduleObject = new ScheduleObject();
            scheduleObject.setType(mapNsuLessonTypeToScheduleType(type));
            scheduleObject.setName(subject.isEmpty() ? null : subject);
            scheduleObject.setPlace(place.isEmpty() ? null : place);
            scheduleObject.setTeacher(teacher.isEmpty() ? null : teacher);

            scheduleObjects.add(scheduleObject);
        }

        return scheduleObjects;
    }



}
