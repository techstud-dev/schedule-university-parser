package com.techstud.sch_parser.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ScheduleObject {

    private ScheduleType type;
    private String name;
    private String teacher;
    private String place;
    private List<String> groups = new ArrayList<>();

}
