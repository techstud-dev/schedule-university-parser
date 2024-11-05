package com.techstud.sch_parser.model.api.response.tltsu;

import lombok.Getter;

import java.io.Serializable;

@Getter
@SuppressWarnings("unused")
public class TltsuTeacher implements Serializable {

    private String id;
    private String name;
    private String lastName;
    private String patronymic;

}
