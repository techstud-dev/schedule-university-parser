package com.techstud.sch_parser.model;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ScheduleDayOfWeekParse {
    public static DayOfWeek staticParseDayOfWeek(String dayName) {
        return switch (dayName.toLowerCase()) {
            case "понедельник" -> java.time.DayOfWeek.MONDAY;
            case "вторник" -> java.time.DayOfWeek.TUESDAY;
            case "среда" -> java.time.DayOfWeek.WEDNESDAY;
            case "четверг" -> java.time.DayOfWeek.THURSDAY;
            case "пятница" -> java.time.DayOfWeek.FRIDAY;
            case "суббота" -> java.time.DayOfWeek.SATURDAY;
            case "воскресенье" -> java.time.DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Неизвестный день недели: " + dayName);
        };
    }

    public static List<DayOfWeek> returnListDayOfTheWeek() {
        return Arrays.asList(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        );
    }

    public static Comparator<DayOfWeek> getDayOfWeekComparator() {
        return Comparator.comparingInt(d -> returnListDayOfTheWeek().indexOf(d));
    }
}
