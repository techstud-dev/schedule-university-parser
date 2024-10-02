package com.funtikov.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class SseuLessonType implements Serializable {

    private Integer id;

    private String name;

    private String certificationType;
}
