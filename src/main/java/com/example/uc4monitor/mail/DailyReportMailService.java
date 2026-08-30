package com.example.uc4monitor.mail;

import com.example.uc4monitor.config.Uc4ReportProperties;
import com.example.uc4monitor.domain.ReportSubscription;
import com.example.uc4monitor.domain.Uc4JobAnomaly;
import com.example.uc4monitor.domain.Uc4JobRunHistory;
import com.example.uc4monitor.repository.Uc4TargetRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class DailyReportMailService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Uc4TargetRepository repository;
    private final JavaMailSender mailSender;
    private final Uc4ReportProperties properties;

    public DailyReportMailService(
            Uc4TargetRepository repository,
            JavaMailSender mailSender,
            Uc4ReportProperties properties
    ) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendDailyReport(LocalDate businessDate) {
        List<ReportSubscription> subscriptions = repository.findActiveReportSubscriptions();
        List<Uc4JobRunHistory> runs = filter(repository.findRunsOn(businessDate), subscriptions);
        List<Uc4JobAnomaly> anomalies = filterAnomalies(repository.findAnomaliesOn(businessDate), subscriptions);

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(properties.from());
            helper.setTo(properties.recipients().toArray(String[]::new));
            helper.setSubject("%s UC4 Daily Report - %s".formatted(properties.subjectPrefix(), businessDate));
            helper.setText(renderHtml(businessDate, runs, anomalies), true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("Could not build UC4 daily report email", ex);
        }
    }

    private List<Uc4JobRunHistory> filter(List<Uc4JobRunHistory> runs, List<ReportSubscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return runs;
        }
        Set<String> jobNames = valuesFor(subscriptions, "JOB");
        Set<String> planNames = valuesFor(subscriptions, "PLAN");
        return runs.stream()
                .filter(run -> jobNames.contains(run.jobName()) || (run.planName() != null && planNames.contains(run.planName())))
                .sorted(Comparator.comparing(Uc4JobRunHistory::jobName))
                .toList();
    }

    private List<Uc4JobAnomaly> filterAnomalies(List<Uc4JobAnomaly> anomalies, List<ReportSubscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return anomalies;
        }
        Set<String> jobNames = valuesFor(subscriptions, "JOB");
        Set<String> planNames = valuesFor(subscriptions, "PLAN");
        return anomalies.stream()
                .filter(anomaly -> jobNames.contains(anomaly.jobName())
                        || (anomaly.planName() != null && planNames.contains(anomaly.planName())))
                .toList();
    }

    private Set<String> valuesFor(List<ReportSubscription> subscriptions, String scopeType) {
        return subscriptions.stream()
                .filter(subscription -> subscription.scopeType().equals(scopeType))
                .map(ReportSubscription::scopeValue)
                .collect(Collectors.toSet());
    }

    private String renderHtml(LocalDate businessDate, List<Uc4JobRunHistory> runs, List<Uc4JobAnomaly> anomalies) {
        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#202734;">
                  <div style="max-width:980px;margin:0 auto;padding:24px;">
                    <h2 style="margin:0 0 4px 0;color:#172033;">UC4 Daily Report</h2>
                    <p style="margin:0 0 18px 0;color:#5d6778;">Business date: %s</p>
                    %s
                    %s
                  </div>
                </body>
                </html>
                """.formatted(businessDate, renderSummary(runs, anomalies), renderRuns(runs) + renderAnomalies(anomalies));
    }

    private String renderSummary(List<Uc4JobRunHistory> runs, List<Uc4JobAnomaly> anomalies) {
        long failed = runs.stream().filter(run -> run.returnCode() != null && run.returnCode() != 0).count();
        return """
                <table role="presentation" style="border-collapse:collapse;margin-bottom:18px;">
                  <tr>
                    <td style="background:#ffffff;border:1px solid #dfe5ef;padding:12px 18px;"><b>Total Runs</b><br>%d</td>
                    <td style="background:#ffffff;border:1px solid #dfe5ef;padding:12px 18px;"><b>Failed Runs</b><br>%d</td>
                    <td style="background:#ffffff;border:1px solid #dfe5ef;padding:12px 18px;"><b>Anomalies</b><br>%d</td>
                  </tr>
                </table>
                """.formatted(runs.size(), failed, anomalies.size());
    }

    private String renderRuns(List<Uc4JobRunHistory> runs) {
        String rows = runs.stream()
                .map(run -> """
                        <tr>
                          <td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td>
                        </tr>
                        """.formatted(
                        escape(run.jobName()),
                        escape(run.planName()),
                        run.startTime() == null ? "" : TIME_FORMATTER.format(run.startTime()),
                        run.endTime() == null ? "" : TIME_FORMATTER.format(run.endTime()),
                        formatDuration(run.durationSeconds()),
                        escape(run.status()),
                        escape(run.lastReport())
                ))
                .collect(Collectors.joining());
        return """
                <h3 style="margin:18px 0 8px 0;color:#172033;">Runs</h3>
                <table style="width:100%%;border-collapse:collapse;background:#ffffff;border:1px solid #dfe5ef;">
                  <thead>
                    <tr style="background:#e9eef7;text-align:left;">
                      <th>Job</th><th>Plan</th><th>Start</th><th>End</th><th>Duration</th><th>Status</th><th>Last Report</th>
                    </tr>
                  </thead>
                  <tbody>%s</tbody>
                </table>
                """.formatted(rows.isBlank() ? "<tr><td colspan=\"7\">No runs found.</td></tr>" : rows);
    }

    private String renderAnomalies(List<Uc4JobAnomaly> anomalies) {
        String rows = anomalies.stream()
                .map(anomaly -> """
                        <tr>
                          <td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td>
                        </tr>
                        """.formatted(
                        escape(anomaly.jobName()),
                        escape(anomaly.planName()),
                        anomaly.anomalyType(),
                        anomaly.deviationPercent() == null ? "" : anomaly.deviationPercent() + "%",
                        escape(anomaly.description())
                ))
                .collect(Collectors.joining());
        return """
                <h3 style="margin:22px 0 8px 0;color:#8a2a2a;">Anomalies</h3>
                <table style="width:100%%;border-collapse:collapse;background:#ffffff;border:1px solid #ebc7c7;">
                  <thead>
                    <tr style="background:#f8e7e7;text-align:left;">
                      <th>Job</th><th>Plan</th><th>Type</th><th>Deviation</th><th>Description</th>
                    </tr>
                  </thead>
                  <tbody>%s</tbody>
                </table>
                """.formatted(rows.isBlank() ? "<tr><td colspan=\"5\">No anomalies detected.</td></tr>" : rows);
    }

    private String formatDuration(Long durationSeconds) {
        if (durationSeconds == null) {
            return "";
        }
        long hours = durationSeconds / 3600;
        long minutes = (durationSeconds % 3600) / 60;
        long seconds = durationSeconds % 60;
        return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
