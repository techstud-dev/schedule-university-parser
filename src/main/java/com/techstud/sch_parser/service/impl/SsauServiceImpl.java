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
import java.util.*;

@Service
@Slf4j
public class SsauServiceImpl implements MappingServiceRef<List<Document>> {
    /**
     * <p>Maps the given list of SSAU documents to a {@link Schedule} object.</p>
     * <p>This method processes the provided documents, extracting the even and odd week schedules
     * and returns a populated {@link Schedule}. If no schedule data is found, it returns {@code null}.</p>
     *
     * @param source the list of {@link Document} objects, where the first element contains data for the even week
     *               and the second element contains data for the odd week.
     * @return a populated {@link Schedule} object containing the parsed even and odd week schedules,
     *         or {@code null} if no valid schedule data is found.
     * @throws EmptyScheduleException if no valid schedule data can be extracted from the documents.
     */
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

        Map<DayOfWeek, ScheduleDay> evenSchedule = getSsauSchedule(evenElement);
        Map<DayOfWeek, ScheduleDay> oddSchedule = getSsauSchedule(oddElement);
        Schedule schedule = new Schedule();
        schedule.setEvenWeekSchedule(evenSchedule);
        schedule.setOddWeekSchedule(oddSchedule);
        schedule.setSnapshotDate(new Date());
        log.info("Mapping SSAU data to schedule {} finished", schedule);
        return schedule;
    }

    /**
     * <p>Extracts the schedule for all days of the week (except Sunday) from the provided SSAU schedule element.</p>
     *
     * @param scheduleElement the root element containing the SSAU schedule.
     * @return a map where the key is a {@link DayOfWeek} and the value is the corresponding {@link ScheduleDay}.
     */
    private Map<DayOfWeek, ScheduleDay> getSsauSchedule(Element scheduleElement) {
        Map<DayOfWeek, ScheduleDay> scheduleDayMap = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day != DayOfWeek.SUNDAY) {
                scheduleDayMap.put(day, getSsauScheduleDay(day, scheduleElement));
            }
        }
        return scheduleDayMap;
    }

    /**
     * <p>Extracts the schedule for a specific day of the week from the provided SSAU schedule element.</p>
     *
     * @param dayOfWeek the day of the week to extract the schedule for.
     * @param element the root element containing the SSAU schedule.
     * @return a {@link ScheduleDay} representing the schedule for the given day.
     */
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

    /**
     * <p>Extracts the lessons for a specific SSAU schedule item element.</p>
     *
     * @param element the SSAU schedule item element to extract lessons from.
     * @return a list of {@link ScheduleObject} representing the lessons.
     */
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

    /**
     * <p>Extracts the group names from the provided SSAU schedule group element.</p>
     *
     * @param scheduleGroupsElement the SSAU schedule group element containing group names.
     * @return a list of group names as strings.
     */
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
