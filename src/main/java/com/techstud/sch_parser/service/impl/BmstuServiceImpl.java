package com.techstud.sch_parser.service.impl;

import com.techstud.sch_parser.exception.EmptyScheduleException;
import com.techstud.sch_parser.model.*;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuApiResponse;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuScheduleItem;
import com.techstud.sch_parser.model.api.response.bmstu.BmstuTeacher;
import com.techstud.sch_parser.service.MappingServiceRef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BmstuServiceImpl implements MappingServiceRef<BmstuApiResponse> {
    /**
     * @param source the response from the API BMSTU containing the schedule data
     * @return object of schedule {@link Schedule}, containing data for even and odd weeks
     * @throws EmptyScheduleException an exception thrown out if the schedule is empty
     */
    @Override
    public Schedule map(BmstuApiResponse source) throws EmptyScheduleException {
        log.info("Start mapping BMSTU data to schedule");
        Schedule schedule = new Schedule();
        Map<DayOfWeek, ScheduleDay> evenWeekSchedule = new LinkedHashMap<>();
        Map<DayOfWeek, ScheduleDay> oddWeekSchedule = new LinkedHashMap<>();

        for (BmstuScheduleItem scheduleItem : source.getData().getSchedule()) {
            DayOfWeek dayOfWeek = DayOfWeek.of(scheduleItem.getDay());
            switch (scheduleItem.getWeek()) {
                case "all" -> {
                    addBmstuToSchedule(evenWeekSchedule, dayOfWeek, scheduleItem);
                    addBmstuToSchedule(oddWeekSchedule, dayOfWeek, scheduleItem);
                }
                case "ch" -> addBmstuToSchedule(evenWeekSchedule, dayOfWeek, scheduleItem);
                case "zn" -> addBmstuToSchedule(oddWeekSchedule, dayOfWeek, scheduleItem);
                default -> {
                }
            }
        }

        schedule.setEvenWeekSchedule(evenWeekSchedule);
        schedule.setOddWeekSchedule(oddWeekSchedule);
        log.info("Mapping BMSTU data to schedule {} finished", schedule);
        return schedule;
    }

    /**
     * @param weekSchedule structure containing a schedule for a certain week (even or odd).
     * @param dayOfWeek the day of the week to which the schedule element belongs
     * @param scheduleItem the schedule element that needs to be added
     */
    private void addBmstuToSchedule(Map<DayOfWeek, ScheduleDay> weekSchedule, DayOfWeek dayOfWeek, BmstuScheduleItem scheduleItem) {
        ScheduleDay scheduleDay = weekSchedule.computeIfAbsent(dayOfWeek, k -> new ScheduleDay());
        addBmstuScheduleItemToDay(scheduleDay, scheduleItem);
    }

    /**
     * @param scheduleDay the object of the day in which the schedule is added
     * @param scheduleItem the schedule element that needs to be added
     */
    private void addBmstuScheduleItemToDay(ScheduleDay scheduleDay, BmstuScheduleItem scheduleItem) {
        TimeSheet timeSheet = new TimeSheet(scheduleItem.getStartTime(), scheduleItem.getEndTime());
        ScheduleObject scheduleObject = createBmstuScheduleObject(scheduleItem);

        Map<TimeSheet, List<ScheduleObject>> lessons = scheduleDay.getLessons();
        if (lessons == null) {
            lessons = new LinkedHashMap<>();
            scheduleDay.setLessons(lessons);
        }

        List<ScheduleObject> scheduleObjects = lessons.computeIfAbsent(timeSheet, k -> new ArrayList<>());
        scheduleObjects.add(scheduleObject);
    }

    /**
     * @param scheduleItem schedule element from the API BMSTU
     * @return object {@link ScheduleObject}, containing information about the discipline, groups, audience and teacher.
     */
    private ScheduleObject createBmstuScheduleObject(BmstuScheduleItem scheduleItem) {
        ScheduleObject scheduleObject = new ScheduleObject();
        scheduleObject.setName(scheduleItem.getDiscipline().getFullName());

        List<String> groups = Arrays.stream(scheduleItem.getStream().split(";"))
                .map(String::trim)
                .collect(Collectors.toList());
        scheduleObject.setGroups(groups);

        if (!scheduleItem.getAudiences().isEmpty()) {
            scheduleObject.setPlace(scheduleItem.getAudiences().get(0).getName());
        }

        if (!scheduleItem.getTeachers().isEmpty()) {
            BmstuTeacher teacher = scheduleItem.getTeachers().get(0);
            scheduleObject.setTeacher(String.format("%s %s %s",
                    teacher.getLastName(),
                    teacher.getFirstName(),
                    teacher.getMiddleName()).trim());
        }

        scheduleObject.setType(scheduleTypeByBmstuType(scheduleItem.getDiscipline().getActType()));
        return scheduleObject;
    }

    /**
     * @param bmstuType type of lesson in bmstu format
     * @return the corresponding type of lesson {@link ScheduleType}. If the type is unknown, returns {@link ScheduleType#UNKNOWN}.
     */
    private ScheduleType scheduleTypeByBmstuType(String bmstuType) {
        if (bmstuType == null) {
            return ScheduleType.UNKNOWN;
        }

        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "lecture", ScheduleType.LECTURE,
                "seminar", ScheduleType.PRACTICE,
                "lab", ScheduleType.LAB,
                "pk", ScheduleType.UNKNOWN
        );

        return scheduleTypeMap.getOrDefault(bmstuType, ScheduleType.UNKNOWN);
    }
}
