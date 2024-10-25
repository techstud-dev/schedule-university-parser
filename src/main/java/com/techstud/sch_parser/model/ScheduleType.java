package com.techstud.sch_parser.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(force = true)
@Getter
public enum ScheduleType {

    LECTURE("Лекция"),
    PRACTICE("Практика"),
    LAB("Лабораторная работа"),
    EXAM("Экзамен/зачет"),
    CONSULTATION("Консультация"),
    UNKNOWN("Другое");

    private final String ruName;

    public static ScheduleType returnTypeByRuName(String ruName) {
        return switch (ruName) {
            case "Лекция" -> ScheduleType.LECTURE;
            case "Практика" -> ScheduleType.PRACTICE;
            case "Лабораторная работа", "Лабораторная" -> ScheduleType.LAB;
            case "Экзамен/зачет" -> ScheduleType.EXAM;
            case "Консультация" -> ScheduleType.CONSULTATION;
            default -> ScheduleType.UNKNOWN;
        };
    }
}
