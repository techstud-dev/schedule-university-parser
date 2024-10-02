package com.funtikov.sch_parser.model.api.response.sseu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
public class SseuApiResponse implements Serializable {

    private String week;

    private List<SseuHeader> headers;

    private List<SseuScheduleRow> body;

    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }
}
