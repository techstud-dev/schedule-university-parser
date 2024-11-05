package com.techstud.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;

@Getter
@SuppressWarnings("unused")
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
