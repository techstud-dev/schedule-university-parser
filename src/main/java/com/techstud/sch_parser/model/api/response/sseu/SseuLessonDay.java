package com.techstud.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
@SuppressWarnings("unused")
public class SseuLessonDay implements Serializable {

    private SseuWorkPlan workPlan;
    private List<SseuSubject> subject;
}
