package com.techstud.sch_parser.model.api.response.tltsu;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
public class TltsuSchedule implements Serializable {

    private String id;
    private String disciplineName;
    private String type;
    private String pairNumber;
    private String date;
    private String fromTime;
    private String toTime;
    private String note;
    private String subgroup;
    private String eduform;
    private String link;
    private TltsuTeacher teacher;
    private TltsuClassRoom classroom;
    private List<TltsuGroup> groupsList;

}
