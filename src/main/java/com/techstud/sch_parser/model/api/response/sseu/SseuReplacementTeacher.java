package com.techstud.sch_parser.model.api.response.sseu;

import lombok.Getter;

import java.io.Serializable;
import java.util.List;

@Getter
@SuppressWarnings("unused")
public class SseuReplacementTeacher implements Serializable {

    private Integer id;
    private Integer orionId;
    private String name;
    private String patronymic;
    private String surname;
    private String status;
    private Integer section;
    private Integer company;
    private String birthDate;
    private String teacher;
    private String teacherCode;
    private String position;
    private String code;
    private String codeFlOneCZp;
    private String codeLms;
    private String rank;
    private String scienceDegree;
    private String fio;
    private String currentAudience;
    private String uuid;
    private String codeFiz;
    private Boolean isUpdate;
    private List<SseuTeacherCode> teacherCodes;
    private String user;
    private boolean teacherLinkedThroughJournal;
}
