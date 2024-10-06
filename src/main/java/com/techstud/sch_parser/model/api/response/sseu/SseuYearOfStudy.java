package com.techstud.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class SseuYearOfStudy implements Serializable {

    private Integer id;

    private String name;

    private Integer current;

    private String yearStart;

    private String yearEnd;
}
