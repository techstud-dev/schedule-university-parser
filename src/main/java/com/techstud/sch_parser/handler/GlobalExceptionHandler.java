package com.techstud.sch_parser.handler;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.application.systemName}")
    private String systemName;
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception exception, HttpRequest request) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("systemName", systemName);
        response.put("serviceName", applicationName);
        response.put("message", exception.getMessage());
        response.put("callId", request.getHeaders().getFirst("callId"));
        log.error(exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(HttpStatusException.class)
    public ResponseEntity<Map<String, String>> handleHttpStatusException(HttpStatusException exception, HttpRequest request) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("systemName", systemName);
        response.put("serviceName", applicationName);
        response.put("message", exception.getMessage());
        response.put("callId", request.getHeaders().getFirst("callId"));
        log.error(exception.getMessage(), exception);
        return ResponseEntity.status(exception.getStatusCode()).body(response);
    }

}
