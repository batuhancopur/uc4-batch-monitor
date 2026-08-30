package com.example.uc4monitor.batch;

import java.time.LocalDate;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Component
public class JobParameterSupport {

    public LocalDate resolveBusinessDate(ChunkContext chunkContext) {
        Object value = chunkContext.getStepContext().getJobParameters().get("businessDate");
        if (value == null || value.toString().isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(value.toString());
    }
}
