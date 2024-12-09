package com.techstud.sch_parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstud.sch_parser.model.api.response.mapping.LessonDto;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schedule implements Serializable {

    private Map<String, Map<String, LessonDto>> evenWeekSchedule;

    private Map<String, Map<String, LessonDto>> oddWeekSchedule;

    private Date snapshotDate = new Date();


    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
