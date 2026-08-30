package com.example.uc4monitor.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("uc4")
public record Uc4Properties(
        @NotBlank String teamCode,
        @Min(1) int lookbackDays,
        @Valid Sync sync
) {
    public String teamNamePattern() {
        return "%" + teamCode + "%";
    }

    public record Sync(
            @NotBlank String definitionQuery,
            @NotBlank String runHistoryQuery
    ) {
    }
}
