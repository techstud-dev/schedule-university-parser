package com.techstud.sch_parser.mapper;

import com.techstud.sch_parser.model.ScheduleDay;
import com.techstud.sch_parser.model.ScheduleObject;
import com.techstud.sch_parser.model.TimeSheet;
import com.techstud.sch_parser.model.api.response.mapping.LessonDto;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScheduleMapper {

    public static Map<String, Map<String, LessonDto>> mapToFrontendFormat(Map<DayOfWeek, ScheduleDay> internalSchedule, boolean isEvenWeek) {
        Map<String, Map<String, LessonDto>> frontendSchedule = new LinkedHashMap<>();

        for (Map.Entry<DayOfWeek, ScheduleDay> entry : internalSchedule.entrySet()) {
            String dayOfWeek = capitalizeFirst(entry.getKey().name().toLowerCase());
            Map<String, LessonDto> lessonsMap = new LinkedHashMap<>();

            Map<TimeSheet, List<ScheduleObject>> lessons = entry.getValue().getLessons();
            int lessonCounter = 1;

            for (Map.Entry<TimeSheet, List<ScheduleObject>> lessonEntry : lessons.entrySet()) {
                TimeSheet timeSheet = lessonEntry.getKey();
                List<ScheduleObject> scheduleObjects = lessonEntry.getValue();

                for (ScheduleObject scheduleObject : scheduleObjects) {
                    String lessonKey = "lesson" + lessonCounter++;
                    LessonDto lessonDto = new LessonDto(
                            String.valueOf(isEvenWeek), // "true" или "false"
                            timeSheet.getFrom() + " - " + timeSheet.getTo(),
                            scheduleObject.getType().name(), // Преобразуем Enum в строку
                            scheduleObject.getName(),
                            scheduleObject.getTeacher(),
                            scheduleObject.getPlace(),
                            scheduleObject.getGroups()
                    );
                    lessonsMap.put(lessonKey, lessonDto);
                }
            }

            frontendSchedule.put(dayOfWeek, lessonsMap);
        }

        return frontendSchedule;
    }

    private static String capitalizeFirst(String input) {
        if (input == null || input.isEmpty()) return input;
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
