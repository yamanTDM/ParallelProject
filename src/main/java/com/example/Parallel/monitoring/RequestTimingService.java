package com.example.Parallel.monitoring;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RequestTimingService {

    private final Map<String, RequestTimingReport> active = new ConcurrentHashMap<>();

    private final Map<String, RequestTimingReport> recent =
            Collections.synchronizedMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, RequestTimingReport> eldest) {
                    return size() > MAX_RECENT;
                }
            });

    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    private static final int MAX_RECENT = 200;
    private static final int MAX_ACTIVE = 1000;


    public void startRequest(String traceId, String endpoint) {
        active.put(traceId, new RequestTimingReport(traceId, endpoint, System.currentTimeMillis()));
        depth.set(0);
    }

    public void recordCall(String traceId, String layer, String name, long durationMs, boolean failed) {
        RequestTimingReport report = active.get(traceId);
        if (report == null) {
            if (active.size() >= MAX_ACTIVE) {

                return;
            }
            report = active.computeIfAbsent(traceId,
                    id -> new RequestTimingReport(id, "(no request context)", System.currentTimeMillis()));
        }

        int currentDepth = Math.max(0, depth.get() - 1);

        report.addCall(new ServiceCallTiming(layer, name, durationMs, currentDepth, failed));
    }

    public int enterCall() {
        int d = depth.get();
        depth.set(d + 1);
        return d;
    }

    public void exitCall() {
        depth.set(Math.max(0, depth.get() - 1));
    }


    public RequestTimingReport finishRequest(String traceId, long totalTimeMs) {
        RequestTimingReport report = active.remove(traceId);
        if (report == null) {
            report = new RequestTimingReport(traceId, "(unknown)", System.currentTimeMillis());
        }
        report.setTotalTimeMs(totalTimeMs);
        recent.put(traceId, report);
        depth.remove();
        return report;
    }

    public RequestTimingReport getReport(String traceId) {
        RequestTimingReport report = recent.get(traceId);
        if (report != null) return report;
        return active.get(traceId);
    }

    public List<RequestTimingReport> getRecent(int limit) {
        synchronized (recent) {
            List<RequestTimingReport> all = new ArrayList<>(recent.values());
            Collections.reverse(all);
            if (limit > 0 && limit < all.size()) {
                return all.subList(0, limit);
            }
            return all;
        }
    }

    public void clearRecent() {
        recent.clear();
    }
}
