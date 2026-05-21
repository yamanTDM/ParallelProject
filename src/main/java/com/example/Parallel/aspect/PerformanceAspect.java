package com.example.Parallel.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    private static final long WARN_THRESHOLD_MS  =  500;
    private static final long ERROR_THRESHOLD_MS = 2000;


    @Pointcut("within(com.example.Parallel.service..*)"+
            "&& !within(com.example.Parallel.service.UserDetailsServiceImpl)")
    public void allServices() {}


    @Around("allServices()")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            return pjp.proceed();
        } finally {

            long elapsed = System.currentTimeMillis() - start;
            ServletRequestAttributes attr =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attr != null) {
                HttpServletRequest request = attr.getRequest();
                request.setAttribute("responseTimeMs", elapsed);
            }
            logPerformance(pjp, elapsed);
        }
    }


    private void logPerformance(ProceedingJoinPoint pjp, long elapsedMs) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String label = sig.getDeclaringType().getSimpleName()
                + "." + sig.getName();

        if (elapsedMs > ERROR_THRESHOLD_MS) {
            log.error("[PERF] {} → {}ms  ✗ VERY SLOW (>{}ms)",
                    label, elapsedMs, ERROR_THRESHOLD_MS);

        } else if (elapsedMs > WARN_THRESHOLD_MS) {
            log.warn("[PERF] {} → {}ms  ⚠ SLOW (>{}ms)",
                    label, elapsedMs, WARN_THRESHOLD_MS);

        } else {
            log.debug("[PERF] {} → {}ms  ✓", label, elapsedMs);
        }
    }
}
