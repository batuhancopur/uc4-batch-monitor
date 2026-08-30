package com.example.uc4monitor.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("uc4-anomaly")
public record Uc4AnomalyProperties(
        @Min(1) int lookbackDays,
        @Min(1) int durationThresholdPercent,
        @Min(1) int minimumBaselineRuns,
        boolean detectMissingRuns
) {
}
