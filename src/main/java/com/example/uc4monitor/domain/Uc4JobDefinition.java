package com.example.uc4monitor.domain;

public record Uc4JobDefinition(
        String uc4ObjectId,
        String jobName,
        String objectType,
        String planName,
        String folderPath,
        String teamCode,
        boolean active
) {
}
