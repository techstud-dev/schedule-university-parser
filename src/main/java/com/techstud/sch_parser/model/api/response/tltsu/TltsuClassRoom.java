package com.techstud.sch_parser.model.api.response.tltsu;

import lombok.Getter;

import java.io.Serializable;

@Getter
@SuppressWarnings("unused")
public class TltsuClassRoom implements Serializable {

    private String id;
    private String name;
    private String number;
    private String subgroup;
    private String eduform;

}
