package com.techstud.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
public class SseuSubject implements Serializable {

    private String name;

    private String link;

    private String subGroups;

    private String invitationCode;

    private List<SseuAudience> audiences;

    private List<SseuReplacementTeacher> replacementTeachers;
}
