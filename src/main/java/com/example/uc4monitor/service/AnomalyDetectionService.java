package com.example.uc4monitor.service;

import com.example.uc4monitor.config.Uc4AnomalyProperties;
import com.example.uc4monitor.domain.AnomalyType;
import com.example.uc4monitor.domain.DurationBaseline;
import com.example.uc4monitor.domain.Uc4JobAnomaly;
import com.example.uc4monitor.domain.Uc4JobRunHistory;
import com.example.uc4monitor.repository.Uc4TargetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final Uc4TargetRepository repository;
    private final Uc4AnomalyProperties properties;

    public AnomalyDetectionService(Uc4TargetRepository repository, Uc4AnomalyProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public List<Uc4JobAnomaly> detect() {
        LocalDate businessDate = repository.currentBusinessDate();
        LocalDate baselineStart = businessDate.minusDays(properties.lookbackDays());
        Map<BaselineKey, DurationBaseline> baselines = repository
                .findDurationBaselines(baselineStart, businessDate, properties.minimumBaselineRuns())
                .stream()
                .collect(Collectors.toMap(BaselineKey::from, Function.identity()));
        List<Uc4JobRunHistory> todaysRuns = repository.findRunsOn(businessDate);

        List<Uc4JobAnomaly> anomalies = new ArrayList<>();
        for (Uc4JobRunHistory run : todaysRuns) {
            if (isFailed(run)) {
                anomalies.add(failedAnomaly(run, businessDate));
                continue;
            }
            DurationBaseline baseline = baselines.get(BaselineKey.from(run));
            if (baseline != null && run.durationSeconds() != null) {
                durationAnomaly(run, baseline, businessDate).ifPresent(anomalies::add);
            }
        }

        if (properties.detectMissingRuns()) {
            Map<String, DurationBaseline> baselinesByJobName = baselines.values().stream()
                    .collect(Collectors.toMap(DurationBaseline::jobName, Function.identity(), (first, ignored) -> first));
            HashSet<String> ranToday = todaysRuns.stream()
                    .map(Uc4JobRunHistory::jobName)
                    .collect(Collectors.toCollection(HashSet::new));
            repository.findActiveDefinitionNames().stream()
                    .filter(baselinesByJobName::containsKey)
                    .filter(jobName -> !ranToday.contains(jobName))
                    .map(jobName -> missingRunAnomaly(jobName, baselinesByJobName.get(jobName), businessDate))
                    .forEach(anomalies::add);
        }

        repository.deleteAnomaliesForDate(businessDate);
        repository.insertAnomalies(anomalies);
        log.info("Anomaly detection completed for {}. anomalies={}", businessDate, anomalies.size());
        return anomalies;
    }

    private java.util.Optional<Uc4JobAnomaly> durationAnomaly(
            Uc4JobRunHistory run,
            DurationBaseline baseline,
            LocalDate businessDate
    ) {
        BigDecimal average = baseline.avgDurationSeconds();
        if (average == null || average.signum() == 0) {
            return java.util.Optional.empty();
        }
        BigDecimal actual = BigDecimal.valueOf(run.durationSeconds());
        BigDecimal deviation = actual.subtract(average)
                .multiply(BigDecimal.valueOf(100))
                .divide(average, 2, RoundingMode.HALF_UP);
        BigDecimal threshold = BigDecimal.valueOf(properties.durationThresholdPercent());

        if (deviation.compareTo(threshold.negate()) < 0) {
            return java.util.Optional.of(durationAnomaly(run, baseline, businessDate, AnomalyType.SHORT_DURATION, deviation));
        }
        if (deviation.compareTo(threshold) > 0) {
            return java.util.Optional.of(durationAnomaly(run, baseline, businessDate, AnomalyType.LONG_DURATION, deviation));
        }
        return java.util.Optional.empty();
    }

    private Uc4JobAnomaly durationAnomaly(
            Uc4JobRunHistory run,
            DurationBaseline baseline,
            LocalDate businessDate,
            AnomalyType type,
            BigDecimal deviation
    ) {
        String description = "%s duration anomaly. Actual=%ss, average=%ss, deviation=%s%%"
                .formatted(run.jobName(), run.durationSeconds(), baseline.avgDurationSeconds(), deviation);
        return new Uc4JobAnomaly(
                run.jobName(),
                run.planName(),
                run.uc4RunId(),
                businessDate,
                type,
                run.durationSeconds(),
                baseline.avgDurationSeconds(),
                baseline.minDurationSeconds(),
                baseline.maxDurationSeconds(),
                deviation,
                description
        );
    }

    private Uc4JobAnomaly failedAnomaly(Uc4JobRunHistory run, LocalDate businessDate) {
        return new Uc4JobAnomaly(
                run.jobName(),
                run.planName(),
                run.uc4RunId(),
                businessDate,
                AnomalyType.FAILED,
                run.durationSeconds(),
                null,
                null,
                null,
                null,
                "%s failed with status=%s returnCode=%s".formatted(run.jobName(), run.status(), run.returnCode())
        );
    }

    private Uc4JobAnomaly missingRunAnomaly(String jobName, DurationBaseline baseline, LocalDate businessDate) {
        return new Uc4JobAnomaly(
                jobName,
                null,
                null,
                businessDate,
                AnomalyType.MISSING_RUN,
                null,
                baseline.avgDurationSeconds(),
                baseline.minDurationSeconds(),
                baseline.maxDurationSeconds(),
                null,
                "%s has baseline history but no run on %s".formatted(jobName, businessDate)
        );
    }

    private boolean isFailed(Uc4JobRunHistory run) {
        if (run.returnCode() != null && run.returnCode() != 0) {
            return true;
        }
        return run.status() != null && (
                run.status().equalsIgnoreCase("FAILED")
                        || run.status().equalsIgnoreCase("ABENDED")
                        || run.status().startsWith("18")
        );
    }

    private record BaselineKey(String jobName, String planName) {
        private static BaselineKey from(DurationBaseline baseline) {
            return new BaselineKey(baseline.jobName(), baseline.planName());
        }

        private static BaselineKey from(Uc4JobRunHistory run) {
            return new BaselineKey(run.jobName(), run.planName());
        }
    }
}
