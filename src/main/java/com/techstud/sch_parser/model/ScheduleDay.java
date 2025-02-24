package com.techstud.sch_parser.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@EqualsAndHashCode(of = "date")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleDay implements Serializable {

    private Date date; // Старый Date

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate localDate; // Для перехода на LocalDate с 6 Java

    private Map<TimeSheet, List<ScheduleObject>> lessons = new LinkedHashMap<>();

    public void setDateAsString(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        dateFormat.setLenient(false);
        try {
            this.date = dateFormat.parse(date);
        } catch (ParseException ignored) {
        }
    }

    public ScheduleDay(LocalDate date) {
        this.localDate = date;
    }
}
