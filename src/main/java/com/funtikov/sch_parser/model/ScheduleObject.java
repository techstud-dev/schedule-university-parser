package com.funtikov.sch_parser.model;

import lombok.Data;

import java.util.List;

@Data
public class ScheduleObject {

    private ScheduleType type;
    private String name;
    private String teacher;
    private String place;
    private List<String> groups;

}
