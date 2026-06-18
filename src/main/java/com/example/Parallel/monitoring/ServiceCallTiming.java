package com.example.Parallel.monitoring;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCallTiming {

    private String layer;

    private String name;

    private long durationMs;

    private int depth;

    private boolean failed;
}
