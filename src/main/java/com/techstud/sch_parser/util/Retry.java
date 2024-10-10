package com.techstud.sch_parser.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
public class Retry {

    private final static Long maxRetries = 3L;
    private final static Long multiplier = 2L;

    public static <T> T retry(Callable<T> action) throws Exception {

        long delay = 1000L;
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                return action.call();  // Выполнение действия
            } catch (Exception e) {
                lastException = e;
                log.error("Error call the action = {}, retrying number = {}", action, i);

                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                    delay *= multiplier;
                }
            }
        }
        throw lastException;
    }
}
