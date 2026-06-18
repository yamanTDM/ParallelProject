package com.example.Parallel.monitoring;

import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


@Data
public class RequestTimingReport {

    private final String traceId;
    private final String endpoint;
    private final long startedAtEpochMs;

    private long totalTimeMs = -1;

    private final List<ServiceCallTiming> calls = new CopyOnWriteArrayList<>();

    public RequestTimingReport(String traceId, String endpoint, long startedAtEpochMs) {
        this.traceId = traceId;
        this.endpoint = endpoint;
        this.startedAtEpochMs = startedAtEpochMs;
    }

    public void addCall(ServiceCallTiming call) {
        calls.add(call);
    }


    public List<ServiceCallTiming> getTopLevelCalls() {
        return calls.stream().filter(c -> c.getDepth() == 0).toList();
    }
}
