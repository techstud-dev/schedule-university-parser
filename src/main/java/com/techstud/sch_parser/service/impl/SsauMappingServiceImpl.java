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

@Slf4j
@Service
public class SsauMappingServiceImpl implements MappingServiceRef<List<Document>> {

    @Override
    public Schedule map(List<Document> source) throws EmptyScheduleException {
        log.info("Start mapping SSAU data to schedule");
        Element evenElement = source.get(0)
                .getElementsByClass("schedule")
                .first();

        Element oddElement = source.get(1)
                .getElementsByClass("schedule")
                .first();

        if (evenElement == null && oddElement == null) {
            return null;
        }

        Map<DayOfWeek, ScheduleDay> evenSchedule = getSsauSchedule(evenElement, true);
        Map<DayOfWeek, ScheduleDay> oddSchedule = getSsauSchedule(oddElement, false);
        Schedule schedule = new Schedule();
        schedule.setEvenWeekSchedule(evenSchedule);
        schedule.setOddWeekSchedule(oddSchedule);
        schedule.setSnapshotDate(new Date());
        log.info("Mapping SSAU data to schedule {} finished", schedule);
        return schedule;
    }

    private Map<DayOfWeek, ScheduleDay> getSsauSchedule(Element scheduleElement, boolean isEvenWeek) {
        Map<DayOfWeek, ScheduleDay> scheduleDayMap = new LinkedHashMap<>();

        LocalDate baseWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        final LocalDate startOfWeek = isEvenWeek ? baseWeekStart : baseWeekStart.plusWeeks(1);

        for (DayOfWeek day : DayOfWeek.values()) {
            if (day != DayOfWeek.SUNDAY) {
                LocalDate date = startOfWeek.with(day);
                scheduleDayMap.put(day, getSsauScheduleDay(day, scheduleElement, date));
            }
        }
        return scheduleDayMap;
    }

    private ScheduleDay getSsauScheduleDay(DayOfWeek dayOfWeek, Element element, LocalDate date) {
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
        scheduleDay.setLocalDate(date);
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
