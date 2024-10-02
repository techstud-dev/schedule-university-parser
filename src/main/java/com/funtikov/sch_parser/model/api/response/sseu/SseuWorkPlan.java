package com.funtikov.sch_parser.model.api.response.sseu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class SseuWorkPlan implements Serializable {

    private Integer id;

    private Integer hours;

    private SseuDiscipline discipline;

    private SseuLessonType lessonTypes;

    private SseuGroup group;

    private SseuHalfYear halfYear;

    private SseuYearOfStudy yearOfStudy;

    private String practic;
}
