package com.example.Parallel.aspect;

import com.example.Parallel.exception.BadRequestException;
import com.example.Parallel.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
public class ExceptionAspect {


    @Pointcut("(within(com.example.Parallel.service..*)" +
            " || within(com.example.Parallel.repository..*))" +
            " && !within(com.example.Parallel.service.UserDetailsServiceImpl)")
    public void allLayers() {
    }


    @AfterThrowing(pointcut = "allLayers()", throwing = "ex")
    public void logException(JoinPoint jp, Throwable ex) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        String location = sig.getDeclaringType().getSimpleName()
                + "." + sig.getName();
        String exType = ex.getClass().getSimpleName();
        String message = ex.getMessage();

        if (ex instanceof BadRequestException) {
            log.warn("[EX] WARN  {} — {} → {}", location, exType, message);

        } else if (ex instanceof ResourceNotFoundException) {
            log.warn("[EX] WARN  {} — {} → {}", location, exType, message);

        } else {
            log.error("[EX] ERROR {} — {} → {}", location, exType, message, ex);
        }
    }
}
