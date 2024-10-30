package com.techstud.sch_parser.model.kafka.request;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class ParsingTask implements Serializable {

    private String universityName;
    private String groupId;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
