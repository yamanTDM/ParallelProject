package com.example.Parallel.aspect;

import com.example.Parallel.monitoring.RequestTimingReport;
import com.example.Parallel.monitoring.RequestTimingService;
import com.example.Parallel.monitoring.ServiceCallTiming;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;


@Slf4j
@Aspect
@Component
@Order(1)   // outermost — sets MDC first on entry, clears it last on exit
@RequiredArgsConstructor
public class TracingAspect {

    private static final String TRACE_ID_KEY    = "traceId";
    private static final String TRACE_HEADER    = "X-Trace-Id";

    private final RequestTimingService requestTimingService;


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

        requestTimingService.startRequest(traceId, buildEndpointLabel());

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("[TRACE] ◀ {}.{}  traceId={}  elapsed={}ms  status=OK",
                    sig.getDeclaringType().getSimpleName(), sig.getName(),
                    traceId, System.currentTimeMillis() - start);
            return result;

        } finally {
            long elapsed = System.currentTimeMillis() - start;
            RequestTimingReport report = requestTimingService.finishRequest(traceId, elapsed);
            logTimingBreakdown(report);

            MDC.remove(TRACE_ID_KEY);
        }
    }

    @Around("innerLayers()")
    public Object traceInnerLayer(ProceedingJoinPoint pjp) throws Throwable {
        String traceId = MDC.get(TRACE_ID_KEY);

        // No HTTP context (e.g. direct test call) — generate a short synthetic trace ID
        boolean locallyGenerated = false;
        if (traceId == null || traceId.isBlank()) {
            traceId = "test-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            MDC.put(TRACE_ID_KEY, traceId);
            locallyGenerated = true;
        }

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String label = sig.getDeclaringType().getSimpleName() + "." + sig.getName();
        String layer = sig.getDeclaringType().getPackageName().contains(".repository")
                ? "REPOSITORY" : "SERVICE";

        // A call with no HTTP context is its own "root" request for timing purposes
        if (locallyGenerated) {
            requestTimingService.startRequest(traceId, label + " (direct call, no HTTP context)");
        }
        requestTimingService.enterCall();

        log.info("[TRACE] → {}  traceId={}{}", label, traceId,
                locallyGenerated ? "  (synthetic — no HTTP context)" : "");
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[TRACE] ← {}  traceId={}  {}ms", label, traceId, elapsed);
            requestTimingService.recordCall(traceId, layer, label, elapsed, false);
            return result;

        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.info("[TRACE] ✗ {}  traceId={}  {}ms  {}",
                     label, traceId, elapsed, ex.getClass().getSimpleName());
            requestTimingService.recordCall(traceId, layer, label, elapsed, true);
            throw ex;

        } finally {
            requestTimingService.exitCall();
            if (locallyGenerated) {
                RequestTimingReport report = requestTimingService.finishRequest(
                        traceId, System.currentTimeMillis() - start);
                logTimingBreakdown(report);
                MDC.remove(TRACE_ID_KEY);
            }
        }
    }


    private String extractIncomingTraceId() {
        try {
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

    private String buildEndpointLabel() {
        try {
            ServletRequestAttributes attr =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr == null) return "(no HTTP context)";
            HttpServletRequest req = attr.getRequest();
            return req.getMethod() + " " + req.getRequestURI();
        } catch (Exception e) {
            return "(unknown)";
        }
    }


    private void logTimingBreakdown(RequestTimingReport report) {
        if (report == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[TIMING] %s traceId=%s total=%dms",
                report.getEndpoint(), report.getTraceId(), report.getTotalTimeMs()));

        for (ServiceCallTiming call : report.getCalls()) {
            sb.append("\n");
            sb.append("    ".repeat(call.getDepth() + 1));
            sb.append(call.getLayer().equals("REPOSITORY") ? "[DB] " : "");
            sb.append(call.getName())
              .append(": ").append(call.getDurationMs()).append("ms");
            if (call.isFailed()) {
                sb.append("  ✗ FAILED");
            }
        }

        if (report.getCalls().isEmpty()) {
            sb.append("\n    (no service/repository calls recorded)");
        }

        log.info(sb.toString());
    }

}
