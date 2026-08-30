package com.example.uc4monitor.repository;

import com.example.uc4monitor.domain.DurationBaseline;
import com.example.uc4monitor.domain.ReportSubscription;
import com.example.uc4monitor.domain.Uc4JobAnomaly;
import com.example.uc4monitor.domain.Uc4JobDefinition;
import com.example.uc4monitor.domain.Uc4JobRunHistory;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class Uc4TargetRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final String insertJobDefinitionSql;
    private final String insertRunHistorySql;
    private final String insertAnomalyLogSql;

    public Uc4TargetRepository(
            JdbcTemplate targetJdbcTemplate,
            NamedParameterJdbcTemplate targetNamedJdbcTemplate,
            SqlResourceLoader sqlResourceLoader
    ) {
        this.jdbcTemplate = targetJdbcTemplate;
        this.namedJdbcTemplate = targetNamedJdbcTemplate;
        this.insertJobDefinitionSql = sqlResourceLoader.read("classpath:sql/target/insert-job-definition.sql");
        this.insertRunHistorySql = sqlResourceLoader.read("classpath:sql/target/insert-run-history.sql");
        this.insertAnomalyLogSql = sqlResourceLoader.read("classpath:sql/target/insert-anomaly-log.sql");
    }

    public void replaceDefinitionsAndHistory(
            List<Uc4JobDefinition> definitions,
            List<Uc4JobRunHistory> histories
    ) {
        jdbcTemplate.execute("truncate table uc4_job_run_history");
        jdbcTemplate.execute("truncate table uc4_job_definition");
        insertDefinitions(definitions);
        insertRunHistories(histories);
    }

    public List<Uc4JobRunHistory> findRunsOn(LocalDate businessDate) {
        return namedJdbcTemplate.query("""
                select uc4_run_id, job_name, plan_name, start_time, end_time, duration_seconds,
                       status, return_code, last_report, business_date
                from uc4_job_run_history
                where business_date = :businessDate
                order by coalesce(plan_name, ''), job_name, start_time
                """, Map.of("businessDate", Date.valueOf(businessDate)), (rs, rowNum) -> new Uc4JobRunHistory(
                rs.getString("uc4_run_id"),
                rs.getString("job_name"),
                rs.getString("plan_name"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time") == null ? null : rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getObject("duration_seconds", Long.class),
                rs.getString("status"),
                rs.getObject("return_code", Integer.class),
                rs.getString("last_report"),
                rs.getDate("business_date").toLocalDate()
        ));
    }

    public List<DurationBaseline> findDurationBaselines(LocalDate from, LocalDate to, int minimumRuns) {
        return namedJdbcTemplate.query("""
                select job_name,
                       count(*) as run_count,
                       avg(duration_seconds) as avg_duration_seconds,
                       min(duration_seconds) as min_duration_seconds,
                       max(duration_seconds) as max_duration_seconds
                from uc4_job_run_history
                where business_date >= :from
                  and business_date < :to
                  and duration_seconds is not null
                  and coalesce(status, '') not in ('FAILED', 'ABENDED')
                group by job_name
                having count(*) >= :minimumRuns
                """, Map.of(
                "from", Date.valueOf(from),
                "to", Date.valueOf(to),
                "minimumRuns", minimumRuns
        ), (rs, rowNum) -> new DurationBaseline(
                rs.getString("job_name"),
                rs.getLong("run_count"),
                rs.getBigDecimal("avg_duration_seconds"),
                rs.getLong("min_duration_seconds"),
                rs.getLong("max_duration_seconds")
        ));
    }

    public List<String> findActiveDefinitionNames() {
        return jdbcTemplate.queryForList(
                "select job_name from uc4_job_definition where active = true",
                String.class
        );
    }

    public void deleteAnomaliesForDate(LocalDate businessDate) {
        namedJdbcTemplate.update(
                "delete from uc4_job_anomaly_log where business_date = :businessDate",
                Map.of("businessDate", Date.valueOf(businessDate))
        );
    }

    public void insertAnomalies(List<Uc4JobAnomaly> anomalies) {
        namedJdbcTemplate.batchUpdate(insertAnomalyLogSql, anomalies.stream()
                .map(anomaly -> new MapSqlParameterSource()
                        .addValue("jobName", anomaly.jobName())
                        .addValue("planName", anomaly.planName())
                        .addValue("runId", anomaly.runId())
                        .addValue("businessDate", Date.valueOf(anomaly.businessDate()))
                        .addValue("anomalyType", anomaly.anomalyType().name())
                        .addValue("actualDurationSeconds", anomaly.actualDurationSeconds())
                        .addValue("avgDurationSeconds", anomaly.avgDurationSeconds())
                        .addValue("minDurationSeconds", anomaly.minDurationSeconds())
                        .addValue("maxDurationSeconds", anomaly.maxDurationSeconds())
                        .addValue("deviationPercent", anomaly.deviationPercent())
                        .addValue("description", anomaly.description()))
                .toArray(MapSqlParameterSource[]::new));
    }

    public List<Uc4JobAnomaly> findAnomaliesOn(LocalDate businessDate) {
        return namedJdbcTemplate.query("""
                select job_name, plan_name, run_id, business_date, anomaly_type,
                       actual_duration_seconds, avg_duration_seconds, min_duration_seconds,
                       max_duration_seconds, deviation_percent, description
                from uc4_job_anomaly_log
                where business_date = :businessDate
                order by job_name, anomaly_type
                """, Map.of("businessDate", Date.valueOf(businessDate)), (rs, rowNum) -> new Uc4JobAnomaly(
                rs.getString("job_name"),
                rs.getString("plan_name"),
                rs.getString("run_id"),
                rs.getDate("business_date").toLocalDate(),
                com.example.uc4monitor.domain.AnomalyType.valueOf(rs.getString("anomaly_type")),
                rs.getObject("actual_duration_seconds", Long.class),
                rs.getBigDecimal("avg_duration_seconds"),
                rs.getObject("min_duration_seconds", Long.class),
                rs.getObject("max_duration_seconds", Long.class),
                rs.getBigDecimal("deviation_percent"),
                rs.getString("description")
        ));
    }

    public List<ReportSubscription> findActiveReportSubscriptions() {
        return jdbcTemplate.query("""
                select scope_type, scope_value
                from uc4_report_subscription
                where active = true
                order by scope_type, scope_value
                """, (rs, rowNum) -> new ReportSubscription(
                rs.getString("scope_type"),
                rs.getString("scope_value")
        ));
    }

    private void insertDefinitions(List<Uc4JobDefinition> definitions) {
        namedJdbcTemplate.batchUpdate(insertJobDefinitionSql, definitions.stream()
                .map(definition -> new MapSqlParameterSource()
                        .addValue("uc4ObjectId", definition.uc4ObjectId())
                        .addValue("jobName", definition.jobName())
                        .addValue("objectType", definition.objectType())
                        .addValue("planName", definition.planName())
                        .addValue("folderPath", definition.folderPath())
                        .addValue("teamCode", definition.teamCode())
                        .addValue("active", definition.active()))
                .toArray(MapSqlParameterSource[]::new));
    }

    private void insertRunHistories(List<Uc4JobRunHistory> histories) {
        namedJdbcTemplate.batchUpdate(insertRunHistorySql, histories.stream()
                .map(history -> new MapSqlParameterSource()
                        .addValue("uc4RunId", history.uc4RunId())
                        .addValue("jobName", history.jobName())
                        .addValue("planName", history.planName())
                        .addValue("startTime", Timestamp.valueOf(history.startTime()))
                        .addValue("endTime", history.endTime() == null ? null : Timestamp.valueOf(history.endTime()))
                        .addValue("durationSeconds", history.durationSeconds())
                        .addValue("status", history.status())
                        .addValue("returnCode", history.returnCode())
                        .addValue("lastReport", history.lastReport())
                        .addValue("businessDate", Date.valueOf(history.businessDate())))
                .toArray(MapSqlParameterSource[]::new));
    }
}
