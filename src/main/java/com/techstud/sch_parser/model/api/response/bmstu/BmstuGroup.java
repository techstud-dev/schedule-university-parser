package com.techstud.sch_parser.model.api.response.bmstu;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class BmstuGroup implements Serializable {

    private String name;
    private String uuid;
}
