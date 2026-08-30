package com.example.uc4monitor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.uc4monitor.config.Uc4AnomalyProperties;
import com.example.uc4monitor.domain.AnomalyType;
import com.example.uc4monitor.domain.DurationBaseline;
import com.example.uc4monitor.domain.Uc4JobRunHistory;
import com.example.uc4monitor.repository.Uc4TargetRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnomalyDetectionServiceTest {

    private final Uc4TargetRepository repository = org.mockito.Mockito.mock(Uc4TargetRepository.class);
    private final AnomalyDetectionService service = new AnomalyDetectionService(
            repository,
            new Uc4AnomalyProperties(30, 40, 5, false)
    );

    @Test
    void detectsLongDurationAnomaly() {
        LocalDate businessDate = LocalDate.of(2026, 8, 30);
        when(repository.findDurationBaselines(any(), any(), anyInt())).thenReturn(List.of(
                new DurationBaseline("JOB_A", "PLAN_A", 10, BigDecimal.valueOf(100), 80, 120)
        ));
        when(repository.findRunsOn(businessDate)).thenReturn(List.of(
                new Uc4JobRunHistory(
                        "123",
                        "JOB_A",
                        "PLAN_A",
                        LocalDateTime.now(),
                        LocalDateTime.now().plusSeconds(160),
                        160L,
                        "ENDED_OK",
                        0,
                        "OK",
                        businessDate
                )
        ));
        when(repository.findActiveDefinitionNames()).thenReturn(List.of("JOB_A"));

        var anomalies = service.detectFor(businessDate);

        assertThat(anomalies).hasSize(1);
        assertThat(anomalies.get(0).anomalyType()).isEqualTo(AnomalyType.LONG_DURATION);
        verify(repository).deleteAnomaliesForDate(businessDate);
        ArgumentCaptor<List<com.example.uc4monitor.domain.Uc4JobAnomaly>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).insertAnomalies(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }
}
