package com.techstud.sch_parser.model.api.response.bmstu;


import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
public class BmstuData implements Serializable {

    private String type;
    private String uuid;
    private String title;
    private List<BmstuScheduleItem> schedule;

}
