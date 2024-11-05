package com.techstud.sch_parser.model.api.response.sseu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;

@SuppressWarnings("unused")
@Getter
public class SseuTeacherCode implements Serializable {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("code")
    private String code;
}
