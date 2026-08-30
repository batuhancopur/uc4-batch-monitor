package com.example.uc4monitor.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("uc4-report")
public record Uc4ReportProperties(
        @NotBlank String from,
        @NotEmpty List<String> recipients,
        @NotBlank String subjectPrefix
) {
}
