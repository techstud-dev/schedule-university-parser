package com.techstud.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;

@Getter
@SuppressWarnings("unused")
public class SseuLessonType implements Serializable {

    private Integer id;
    private String name;
    private String certificationType;
}
