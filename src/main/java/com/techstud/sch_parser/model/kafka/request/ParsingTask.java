package com.techstud.sch_parser.model.kafka.request;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ParsingTask implements Serializable {

    /**
     * Название университета аббревиатурой
     */
    private String universityName;

    /**
     * Id группы (можно найти в урле)
     */
    private String groupId;

    /**
     * Номер подгруппы (нужен для некоторых университетов)
     */
    private String subGroupId;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
