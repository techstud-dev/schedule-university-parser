package com.techstud.sch_parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode(of = "date")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScheduleDay implements Serializable {

    private Date date;

    @Setter
    private Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();

    public void setDate(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        dateFormat.setLenient(false);
        try {
            this.date = dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
