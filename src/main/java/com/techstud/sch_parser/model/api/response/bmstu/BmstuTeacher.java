package com.techstud.sch_parser.model.api.response.bmstu;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class BmstuTeacher implements Serializable {

    private String uuid;
    private String lastName;
    private String firstName;
    private String middleName;

}
