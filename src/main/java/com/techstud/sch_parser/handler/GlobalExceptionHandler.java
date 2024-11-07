package com.techstud.sch_parser.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.systemName}")
    private String systemName;

    public Map<String, String> handleException(String messageKey, Exception exception) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("systemName", systemName);
        response.put("serviceName", applicationName);
        response.put("messageId", messageKey);
        response.put("message", exception.getMessage());
        return response;
    }
}
