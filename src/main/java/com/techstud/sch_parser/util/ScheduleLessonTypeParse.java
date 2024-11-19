package com.techstud.sch_parser.util;

import com.techstud.sch_parser.model.ScheduleType;

import java.io.Serializable;
import java.util.Map;

public class ScheduleLessonTypeParse {

    public static ScheduleType mapSseuLessonTypeToScheduleType(String lessonType) {
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "Лекции", ScheduleType.LECTURE,
                "Практические", ScheduleType.PRACTICE,
                "Лабораторные", ScheduleType.LAB,
                "Пересдача Зачет", ScheduleType.EXAM,
                "Пересдача Экзамен", ScheduleType.EXAM);
        return scheduleTypeMap.get(lessonType);
    }

    public static ScheduleType mapMephiLessonTypeToScheduleType(String lessonType) {
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "Лек", ScheduleType.LECTURE,
                "Пр", ScheduleType.PRACTICE,
                "Лаб", ScheduleType.LAB,
                "Резерв", ScheduleType.UNKNOWN
        );
        return scheduleTypeMap.getOrDefault(lessonType, ScheduleType.UNKNOWN);
    }

    public static ScheduleType mapNsuLessonTypeToScheduleType(String lessonType) {
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "пр", ScheduleType.PRACTICE,
                "лек", ScheduleType.LECTURE,
                "лаб", ScheduleType.LAB);
        return scheduleTypeMap.getOrDefault(lessonType, ScheduleType.UNKNOWN);
    }

    public static ScheduleType mapMiitLessonTypeToScheduleType(String lessonType) {
        if (lessonType == null) return ScheduleType.UNKNOWN;

        lessonType = lessonType.trim().toLowerCase();

        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "практическое занятие", ScheduleType.PRACTICE,
                "практическое занятие практическое занятие", ScheduleType.PRACTICE,
                "лекция", ScheduleType.LECTURE,
                "лабораторная работа", ScheduleType.LAB
        );

        return scheduleTypeMap.getOrDefault(lessonType, ScheduleType.UNKNOWN);
    }

    /**
     * @param spbstuType входная строка
     * @return ScheduleType возвращаемый тип занятия
     */
    public static ScheduleType returnScheduleTypeSpbstu(String spbstuType){
        Map<String, ScheduleType> scheduleTypeMap = Map.of(
                "Практика", ScheduleType.PRACTICE,
                "Лекции", ScheduleType.LECTURE,
                "Лабораторные", ScheduleType.LAB);
        return scheduleTypeMap.getOrDefault(spbstuType, ScheduleType.UNKNOWN);
    }
}
