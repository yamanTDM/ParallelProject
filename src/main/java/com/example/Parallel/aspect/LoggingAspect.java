package com.example.Parallel.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;


@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "password", "token", "secret", "authorization", "credential"
    );



    @Pointcut("within(com.example.Parallel.service..*) " +
            "&& !within(com.example.Parallel.service.UserDetailsServiceImpl)")
    public void allServices() {}

    @Pointcut("allServices()")
    public void allApplicationCode() {}


    @Around("allApplicationCode()")
    public Object logMethodCall(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig  = (MethodSignature) pjp.getSignature();
        String className     = sig.getDeclaringType().getSimpleName();
        String methodName    = sig.getName();
        String[] paramNames  = sig.getParameterNames();
        Object[] paramValues = pjp.getArgs();

        String args = buildArgString(paramNames, paramValues);

        log.info("[LOG] → {}.{}({})", className, methodName, args);
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            log.info("[LOG] ← {}.{} returned in {}ms",
                    className, methodName, System.currentTimeMillis() - start);
            return result;

        } catch (Throwable ex) {
            log.warn("[LOG] ✗ {}.{} threw {} after {}ms — {}",
                    className, methodName,
                    ex.getClass().getSimpleName(),
                    System.currentTimeMillis() - start,
                    ex.getMessage());
            throw ex;
        }
    }


    private String buildArgString(String[] names, Object[] values) {
        if (names == null || names.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            if (i > 0) sb.append(", ");

            String name = names[i] != null ? names[i].toLowerCase() : "";

            boolean isSensitive = SENSITIVE_PARAMS.stream().anyMatch(name::contains);
            if (isSensitive) {
                sb.append(names[i]).append("=[HIDDEN]");
            } else {
                sb.append(names[i]).append("=").append(formatValue(values[i]));
            }
        }
        return sb.toString();
    }

    private String formatValue(Object value) {
        if (value == null)              return "null";
        if (value.getClass().isArray()) {
            return value.getClass().getComponentType().getSimpleName()
                    + "[" + java.lang.reflect.Array.getLength(value) + "]";
        }

        if (value instanceof java.util.Collection<?> col) {
            return value.getClass().getSimpleName() + "[size=" + col.size() + "]";
        }
        if (value.getClass().isPrimitive()
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>) {
            return "\"" + value + "\"";
        }

        return formatObject(value);
    }

    private String formatObject(Object obj) {
        StringBuilder sb = new StringBuilder();
        if (obj instanceof java.time.temporal.TemporalAccessor) {
            return "\"" + obj + "\"";
        }
        sb.append(obj.getClass().getSimpleName()).append("(");

        java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].setAccessible(true);
            String fieldName = fields[i].getName().toLowerCase();

            if (i > 0) sb.append(", ");
            sb.append(fields[i].getName()).append("=");

            boolean isSensitive = SENSITIVE_PARAMS.stream().anyMatch(fieldName::contains);
            if (isSensitive) {
                sb.append("[HIDDEN]");
            } else {
                try {
                    sb.append(fields[i].get(obj));
                } catch (IllegalAccessException e) {
                    sb.append("[UNREADABLE]");
                }
            }
        }

        sb.append(")");
        return sb.toString();
    }
}
