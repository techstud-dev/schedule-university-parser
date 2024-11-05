package com.techstud.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;

@Getter
@SuppressWarnings("unused")
public class SseuGroup implements Serializable {

    private Integer id;
    private String status;
    private String name;
    private String course;
    private String direction;
    private String directionCode;
    private String faculty;
    private String formOfTraining;
    private String groupCode;
    private String numberOfStudents;
    private String program;
    private String trainingPeriod;
    private Integer scheduleType;
    private String typesEducation;
    private String courseNum;
    private String semester;
}
