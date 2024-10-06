package com.techstud.sch_parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode(of = "date")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ScheduleDay {

    private Date date;

    private Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();

}
