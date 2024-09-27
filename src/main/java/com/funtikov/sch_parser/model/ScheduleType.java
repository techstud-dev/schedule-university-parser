package com.funtikov.sch_parser.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public enum ScheduleType {

    LECTURE("Лекция"),
    PRACTICE("Практика"),
    LAB("Лабораторная работа"),
    EXAM("Экзамен/зачет"),
    CONSULTATION("Консультация"),
    UNKNOWN("Другое");

    private final String name;
}
