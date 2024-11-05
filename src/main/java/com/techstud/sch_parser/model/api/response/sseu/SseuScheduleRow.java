package com.techstud.sch_parser.model.api.response.sseu;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@SuppressWarnings("unused")
public class SseuScheduleRow implements Serializable {

    private String name;
    private Map<String, List<SseuLessonDay>> daySchedule = new LinkedHashMap<>();
    @JsonAnySetter
    public void setDaySchedule(String day, List<SseuLessonDay> lessons) {
        daySchedule.put(day, lessons);
    }
}
