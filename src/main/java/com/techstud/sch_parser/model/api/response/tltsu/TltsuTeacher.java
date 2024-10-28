package com.techstud.sch_parser.model.api.response.tltsu;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class TltsuTeacher implements Serializable {

    private String id;
    private String name;
    private String lastName;
    private String patronymic;

}
