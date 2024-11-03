package com.techstud.sch_parser.domain;

import com.techstud.sch_parser.model.Schedule;
import com.techstud.sch_parser.model.kafka.request.ParsingTask;
import org.springframework.cache.annotation.Cacheable;

public interface Parser {

    String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3";
    String referrer = "http://www.google.com";

    @Cacheable(value = "schedule", key = "#task.universityName + #task.groupId + #task.subGroupId")
    Schedule parseSchedule(ParsingTask task) throws Exception;

}
