package com.techstud.sch_parser.model.api.response.sseu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;

@Getter
@SuppressWarnings("unused")
public class SseuDiscipline implements Serializable {

    private Integer id;
    private String name;
    private String codeOneC;
    private String fullName;
    @JsonProperty("isPractice")
    private boolean isPractice;
}
