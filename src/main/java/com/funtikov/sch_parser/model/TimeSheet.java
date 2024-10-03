package com.funtikov.sch_parser.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
public class TimeSheet {

    private LocalTime from;

    private LocalTime to;

}
