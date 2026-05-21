package com.example.Parallel.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;


@Slf4j
@Aspect
@Component
public class TracingAspect {

    private static final String TRACE_ID_KEY    = "traceId";
    private static final String TRACE_HEADER    = "X-Trace-Id";



    @Pointcut("within(com.example.Parallel.controller..*)")
    public void controllers() {}
    @Pointcut("(within(com.example.Parallel.service..*)" +
            " || within(com.example.Parallel.repository..*))" +
            " && !within(com.example.Parallel.service.UserDetailsServiceImpl)")
    public void innerLayers() {}

    @Around("controllers()")
    public Object traceRequest(ProceedingJoinPoint pjp) throws Throwable {
        String traceId = extractIncomingTraceId();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        MDC.put(TRACE_ID_KEY, traceId);

        attachResponseHeader(traceId);

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        log.info("[TRACE] ▶ {}.{}  traceId={}",
                sig.getDeclaringType().getSimpleName(), sig.getName(), traceId);

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("[TRACE] ◀ {}.{}  traceId={}  elapsed={}ms  status=OK",
                    sig.getDeclaringType().getSimpleName(), sig.getName(),
                    traceId, System.currentTimeMillis() - start);
            return result;

        } finally {

            MDC.remove(TRACE_ID_KEY);
        }
    }

    @Around("innerLayers()")
    public Object traceInnerLayer(ProceedingJoinPoint pjp) throws Throwable {
        String traceId = MDC.get(TRACE_ID_KEY);

        if (traceId == null) {
            return pjp.proceed();
        }


        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String label = sig.getDeclaringType().getSimpleName() + "." + sig.getName();

        log.info("[TRACE] → {}  traceId={}", label, traceId);
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            log.info("[TRACE] ← {}  traceId={}  {}ms"
                    , label, traceId, System.currentTimeMillis() - start);
            return result;

        } catch (Throwable ex) {
            log.info("[TRACE] ✗ {}  traceId={}  {}ms  {}",
                     label, traceId,
                    System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName());
            throw ex;

        }
    }


    private String extractIncomingTraceId() {
        try {
            //gets Request
            ServletRequestAttributes attr =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr == null) return null;
            HttpServletRequest req = attr.getRequest();
            return req.getHeader(TRACE_HEADER);
        } catch (Exception e) {
            return null;
        }
    }

    private void attachResponseHeader(String traceId) {
        try {
            ServletRequestAttributes attr =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                attr.getResponse().setHeader(TRACE_HEADER, traceId);
            }
        } catch (Exception ignored) {}
    }

}