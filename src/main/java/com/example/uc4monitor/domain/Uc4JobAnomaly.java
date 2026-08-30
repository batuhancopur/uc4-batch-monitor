package com.example.uc4monitor.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Uc4JobAnomaly(
        String jobName,
        String planName,
        String runId,
        LocalDate businessDate,
        AnomalyType anomalyType,
        Long actualDurationSeconds,
        BigDecimal avgDurationSeconds,
        Long minDurationSeconds,
        Long maxDurationSeconds,
        BigDecimal deviationPercent,
        String description
) {
}
