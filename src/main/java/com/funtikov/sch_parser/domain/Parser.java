package com.funtikov.sch_parser.domain;

import com.funtikov.sch_parser.model.Schedule;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

public interface Parser {

    String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3";
    String referrer = "http://www.google.com";

    Schedule parserSchedule(String url) throws IOException;

}
