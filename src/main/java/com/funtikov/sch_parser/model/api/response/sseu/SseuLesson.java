package com.funtikov.sch_parser.model.api.response.sseu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
public class SseuLesson implements Serializable {

    private SseuWorkPlan workPlan;

    private List<SseuSubject> subject;
}
