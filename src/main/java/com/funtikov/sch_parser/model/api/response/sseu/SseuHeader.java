package com.funtikov.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class SseuHeader implements Serializable {

    private String text;

    private String align;

    private boolean sortable;

    private String value;
}
