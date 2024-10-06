package com.techstud.sch_parser.model.api.response.sseu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SseuTeacherCode {
    @JsonProperty("id")
    private Long id;
    @JsonProperty("code")
    private String code;
}
