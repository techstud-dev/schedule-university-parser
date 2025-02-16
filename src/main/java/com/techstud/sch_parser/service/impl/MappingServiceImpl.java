package com.techstud.sch_parser.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.model.*;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuApiResponse;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuScheduleItem;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuTeacher;
import com.techstud.sch_parser.model.api.response.sseu.SseuApiResponse;
import com.techstud.sch_parser.model.api.response.sseu.SseuLessonDay;
import com.techstud.sch_parser.model.api.response.sseu.SseuSubject;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuApiResponse;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuGroup;
import com.techstud.sch_parser.model.api.response.tltsu.TltsuSchedule;
import com.techstud.sch_parser.service.MappingService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.techstud.sch_parser.util.ScheduleDayOfWeekParse.*;
import static com.techstud.sch_parser.util.ScheduleLessonTypeParse.*;

@Service
@Slf4j
public class MappingServiceImpl implements MappingService {

    @Override
    public Schedule mapSsauToSchedule(List<Document> documents) {
        log.info("Start mapping SSAU data to schedule");
        Element evenElement = documents.get(0)
                .getElementsByClass("schedule")
                .first();

        Element oddElement = documents.get(1)
                .getElementsByClass("schedule")
                .first();

        if (evenElement == null && oddElement == null) {
            return null;
        }

        Map<DayOfWeek, ScheduleDay> evenSchedule = getSsauSchedule(evenElement);
        Map<DayOfWeek, ScheduleDay> oddSchedule = getSsauSchedule(oddElement);
        Schedule schedule = new Schedule();
        schedule.setEvenWeekSchedule(evenSchedule);
        schedule.setOddWeekSchedule(oddSchedule);
        schedule.setSnapshotDate(new Date());
        log.info("Mapping SSAU data to schedule {} finished", schedule);
        return schedule;
    }

    @Override
    public Schedule mapPgupsToSchedule(List<Document> documents) {
        log.info("Start mapping PGUPS data to schedule");

        Schedule schedule = new Schedule();
        schedule.setSnapshotDate(new Date());
        schedule.setEvenWeekSchedule(parsePgupsSchedule(documents.get(0)));
        schedule.setOddWeekSchedule(parsePgupsSchedule(documents.get(1)));

        log.info("Mapping PGUPS data to schedule {} finished", schedule);
        return schedule;
    }

    private Map<DayOfWeek, ScheduleDay> parsePgupsSchedule(Document document) {
        Map<DayOfWeek, ScheduleDay> schedule = new LinkedHashMap<>();
        Elements scheduleDays = document.getElementsByTag("tbody");

        scheduleDays.forEach(day -> {
            DayOfWeek dayOfWeek = parseDayOfWeek(day.getElementsByClass("kt-font-dark").text().toLowerCase());
            schedule.put(dayOfWeek, parsePgupsScheduleDay(day));
        });

        return schedule;
    }

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

    private Map<DayOfWeek, ScheduleDay> getSsauSchedule(Element scheduleElement) {
        Map<DayOfWeek, ScheduleDay> scheduleDayMap = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day != DayOfWeek.SUNDAY) {
                scheduleDayMap.put(day, getSsauScheduleDay(day, scheduleElement));
            }
        }
        return scheduleDayMap;
    }

    private ScheduleDay getSsauScheduleDay(DayOfWeek dayOfWeek, Element element) {
        Elements scheduleItemElements = element.getElementsByClass("schedule__item");
        List<Element> ssauTimeSheets = element.getElementsByClass("schedule__time");
        List<Element> ssauDayOfWeek = scheduleItemElements.subList(1, 7);
        List<Element> ssauLessons = scheduleItemElements.subList(7, scheduleItemElements.size());

        ScheduleDay scheduleDay = new ScheduleDay();
        Map<TimeSheet, List<ScheduleObject>> timeSheetListMap = new LinkedHashMap<>();

        for (int i = 0; i < ssauTimeSheets.size(); i++) {
            String startTime = ssauTimeSheets.get(i).getElementsByClass("schedule__time-item").get(0).text();
            String endTime = ssauTimeSheets.get(i).getElementsByClass("schedule__time-item").get(1).text();
            TimeSheet timeSheet = new TimeSheet(startTime, endTime);

            int currentElement = (i * ssauDayOfWeek.size()) + dayOfWeek.getValue() - 1;

            List<ScheduleObject> scheduleObjects;
            if (currentElement < ssauLessons.size()) {
                scheduleObjects = getSsauScheduleObject(ssauLessons.get(currentElement));
            } else {
                scheduleObjects = new ArrayList<>();
            }

            timeSheetListMap.put(timeSheet, scheduleObjects);
        }
        scheduleDay.setDateAsString(ssauDayOfWeek.get(1).getElementsByClass("schedule__head-date").text());
        scheduleDay.setLessons(timeSheetListMap);

        return scheduleDay;
    }

    private List<ScheduleObject> getSsauScheduleObject(Element element) {
        List<ScheduleObject> scheduleObjects = new ArrayList<>();
        List<Element> scheduleLessons = element.getElementsByClass("schedule__lesson");
        for (Element scheduleLesson : scheduleLessons) {
            ScheduleObject scheduleObject = new ScheduleObject();
            scheduleObject.setType(ScheduleType.returnTypeByRuName(scheduleLesson.getElementsByClass("schedule__lesson-type-chip").text()));
            scheduleObject.setName(scheduleLesson.getElementsByClass("schedule__discipline").text());
            scheduleObject.setPlace(scheduleLesson.getElementsByClass("schedule__place").text());
            scheduleObject.setTeacher(scheduleLesson.getElementsByClass("schedule__teacher").text());
            scheduleObject.setGroups(getSsauScheduleGroups(scheduleLesson.getElementsByClass("schedule__groups")));
            scheduleObjects.add(scheduleObject);
        }
        return scheduleObjects;
    }

    private List<String> getSsauScheduleGroups(Elements scheduleGroupsElement) {
        List<String> groupNames = new ArrayList<>();
        List<Element> aElements = scheduleGroupsElement.get(0).getElementsByTag("a");
        List<Element> spanElements = scheduleGroupsElement.get(0).getElementsByTag("span");
        if (aElements.isEmpty()) {
            for (Element spanElement : spanElements) {
                groupNames.add(spanElement.text());
            }
        } else {
            for (Element aElement : aElements) {
                groupNames.add(aElement.text());
            }
        }
        return groupNames;
    }

}
