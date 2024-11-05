package com.techstud.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;

@Getter
@SuppressWarnings("unused")
public class SseuHeader implements Serializable {

    private String text;
    private String align;
    private boolean sortable;
    private String value;
}
