package com.example.uc4monitor.domain;

import java.math.BigDecimal;

public record DurationBaseline(
        String jobName,
        long runCount,
        BigDecimal avgDurationSeconds,
        long minDurationSeconds,
        long maxDurationSeconds
) {
}
