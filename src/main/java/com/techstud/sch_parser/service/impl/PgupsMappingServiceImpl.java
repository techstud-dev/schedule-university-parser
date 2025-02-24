package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.*;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static com.techstud.sch_parser.util.ScheduleDayOfWeekParse.parseDayOfWeek;

@Slf4j
@Service
public class PgupsMappingServiceImpl implements MappingServiceRef<List<Document>> {

    @Override
    public Schedule map(List<Document> source) throws EmptyScheduleException {
        log.info("Start mapping PGUPS data to schedule");

        Schedule schedule = new Schedule();
        schedule.setSnapshotDate(new Date());
        schedule.setEvenWeekSchedule(parsePgupsSchedule(source.get(0), true));
        schedule.setOddWeekSchedule(parsePgupsSchedule(source.get(1), false));

        log.info("Mapping PGUPS data to schedule {} finished", schedule);
        return schedule;
    }

    private Map<DayOfWeek, ScheduleDay> parsePgupsSchedule(Document document, boolean isEvenWeek) {
        Map<DayOfWeek, ScheduleDay> schedule = new LinkedHashMap<>();
        Elements scheduleDays = document.getElementsByTag("tbody");

        LocalDate baseWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        final LocalDate weekStart = isEvenWeek ? baseWeekStart : baseWeekStart.plusWeeks(1);

        scheduleDays.forEach(day -> {
            DayOfWeek dayOfWeek = parseDayOfWeek(day.getElementsByClass("kt-font-dark").text().toLowerCase());
            LocalDate date = weekStart.with(dayOfWeek);
            schedule.put(dayOfWeek, parsePgupsScheduleDay(day, date));
        });

        return schedule;
    }

    private ScheduleDay parsePgupsScheduleDay(Element dayElement, LocalDate date) {
        Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();

        dayElement.getElementsByTag("tr").forEach(lesson -> {
            String[] timeRange = lesson.getElementsByClass("text-center kt-shape-font-color-4")
                    .text()
                    .split("—");
            TimeSheet timeSheet = new TimeSheet(timeRange[0].trim(), timeRange[1].trim());
            lessons.put(timeSheet, parsePgupsScheduleObjects(lesson));
        });

        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setLocalDate(date);
        scheduleDay.setLessons(lessons);
        return scheduleDay;
    }

    private List<ScheduleObject> parsePgupsScheduleObjects(Element lesson) {
        List<ScheduleObject> scheduleObjects = new ArrayList<>();

        ScheduleObject scheduleObject = new ScheduleObject();
        scheduleObject.setName(lesson.getElementsByClass("mr-1").text().trim());
        scheduleObject.setPlace(lesson.select("a[href^=https://rasp.pgups.ru/schedule/room]").text().trim());
        scheduleObject.setType(ScheduleType.returnTypeByPgupsName(lesson.select("span[class^=badge]").text().trim()));
        scheduleObject.setTeacher(lesson.select("div > a[href^=https://rasp.pgups.ru/schedule/teacher].kt-link").text().trim());

        scheduleObjects.add(scheduleObject);
        return scheduleObjects;
    }

}
