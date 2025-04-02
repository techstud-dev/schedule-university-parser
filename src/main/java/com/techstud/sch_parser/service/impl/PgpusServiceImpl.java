package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.*;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.log4j.Log4j2;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.*;

import static com.techstud.sch_parser.util.ScheduleDayOfWeekParse.parseDayOfWeek;

@Service
@Log4j2
public class PgpusServiceImpl implements MappingServiceRef<List<Document>> {

    /**
     * <p>Maps a list of PGUPS schedule documents to a {@link Schedule} object.</p>
     *
     * @param source a list of {@link Document} objects representing PGUPS schedule data;
     *               expected to contain at least two elements:
     *               the first for even weeks and the second for odd weeks.
     * @return a {@link Schedule} object containing parsed schedule data.
     * @throws EmptyScheduleException if the provided schedule data is empty or invalid.
     */
    @Override
    public Schedule map(List<Document> source) throws EmptyScheduleException {
        log.info("Start mapping PGUPS data to schedule");

        Schedule schedule = new Schedule();
        schedule.setSnapshotDate(new Date());
        schedule.setEvenWeekSchedule(parsePgupsSchedule(source.get(0)));
        schedule.setOddWeekSchedule(parsePgupsSchedule(source.get(1)));

        log.info("Mapping PGUPS data to schedule {} finished", schedule);
        return schedule;
    }

    /**
     * <p>Parses a PGUPS schedule document into a map of scheduled days.</p>
     *
     * @param document the {@link Document} containing the schedule data in HTML format.
     * @return a {@link Map} where keys are {@link DayOfWeek} representing weekdays,
     *         and values are {@link ScheduleDay} objects containing schedule details for each day.
     */
    private Map<DayOfWeek, ScheduleDay> parsePgupsSchedule(Document document) {
        Map<DayOfWeek, ScheduleDay> schedule = new LinkedHashMap<>();
        Elements scheduleDays = document.getElementsByTag("tbody");

        scheduleDays.forEach(day -> {
            DayOfWeek dayOfWeek = parseDayOfWeek(day.getElementsByClass("kt-font-dark").text().toLowerCase());
            schedule.put(dayOfWeek, parsePgupsScheduleDay(day));
        });

        return schedule;
    }

    /**
     * <p>Parses an HTML element representing a single day's schedule into a {@link ScheduleDay} object.</p>
     *
     * @param dayElement the {@link Element} containing schedule data for a specific day.
     * @return a {@link ScheduleDay} object with mapped time slots and associated schedule entries.
     */
    private ScheduleDay parsePgupsScheduleDay(Element dayElement) {
        Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();

        dayElement.getElementsByTag("tr").forEach(lesson -> {
            String[] timeRange = lesson.getElementsByClass("text-center kt-shape-font-color-4")
                    .text()
                    .split("—");
            TimeSheet timeSheet = new TimeSheet(timeRange[0].trim(), timeRange[1].trim());
            lessons.put(timeSheet, parsePgupsScheduleObjects(lesson));
        });

        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setLessons(lessons);
        return scheduleDay;
    }

    /**
     * Parses an HTML element representing a lesson into a list of {@link ScheduleObject}.
     *
     * @param lesson the {@link Element} containing information about a single lesson.
     * @return a list of {@link ScheduleObject}, typically containing one parsed lesson.
     */
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
