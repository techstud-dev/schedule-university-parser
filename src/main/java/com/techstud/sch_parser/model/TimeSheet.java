package com.techstud.sch_parser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimeSheet {

    private LocalTime from;

    private LocalTime to;

    public TimeSheet(String from, String to) {
        this.from = LocalTime.parse(from);
        this.to = LocalTime.parse(to);
    }

    public TimeSheet(String from) {
        this.from = LocalTime.parse(from);
    }
}
