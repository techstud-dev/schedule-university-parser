package com.techstud.sch_parser.model.kafka.request;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ParsingTask implements Serializable {

    private String universityName;
    private String groupId;

}
