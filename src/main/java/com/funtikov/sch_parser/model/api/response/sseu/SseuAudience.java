package com.funtikov.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class SseuAudience implements Serializable {

    private Long id;
    private String name;
    private String type;
    private String typeName;
    private Long capacity;
    private String code;
    private String typeAudiences;
    private String house;
    private String itemName;
    private Map<String, String> accessPointIdToNameMap = new LinkedHashMap<>();
}
