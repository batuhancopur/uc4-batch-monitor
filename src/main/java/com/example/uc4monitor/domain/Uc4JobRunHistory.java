package com.example.uc4monitor.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Uc4JobRunHistory(
        String uc4RunId,
        String jobName,
        String planName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationSeconds,
        String status,
        Integer returnCode,
        String lastReport,
        LocalDate businessDate
) {
}
