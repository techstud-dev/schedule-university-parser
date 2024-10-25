package com.techstud.sch_parser.model.api.response.bmstu;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
public class BmstuScheduleItem implements Serializable {

    private int day;
    private int time;
    private String week;
    private List<BmstuGroup> groups;
    private String stream;
    private String endTime;
    private List<BmstuTeacher> teachers;
    private List<BmstuAudience> audiences;
    private String startTime;
    private BmstuDiscipline discipline;
    private String permission;

}
