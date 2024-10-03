package com.funtikov.sch_parser.model.api.response.sseu;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SseuScheduleRow implements Serializable {

    private String name;

    private Map<String, List<SseuLessonDay>> daySchedule = new LinkedHashMap<>();

    // Метод для добавления неизвестных полей (например, дней недели) в карту
    @JsonAnySetter
    public void setDaySchedule(String day, List<SseuLessonDay> lessons) {
        daySchedule.put(day, lessons);
    }
}
