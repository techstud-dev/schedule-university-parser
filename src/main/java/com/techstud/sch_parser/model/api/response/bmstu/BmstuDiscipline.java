package com.techstud.sch_parser.model.api.response.bmstu;

import lombok.Getter;

import java.io.Serializable;

@Getter
@SuppressWarnings("unused")
public class BmstuDiscipline implements Serializable {

    private String abbr;
    private String actType;
    private String fullName;
    private String shortName;

}
