package com.funtikov.sch_parser.model;

import lombok.Data;

import java.time.LocalTime;

@Data
public class TimeSheet {

    private LocalTime from;

    private LocalTime to;

}
