package com.techstud.sch_parser.aspect;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class ProfilingAspect {

    @Around("@annotation(com.techstud.sch_parser.annotation.Profiling)")
    public Object profileMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Object result = joinPoint.proceed();

        stopwatch.stop();
        long elapsedTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        log.info("Method {} executed in {} ms", joinPoint.getSignature().toShortString(), elapsedTime);

        return result;
    }
}
