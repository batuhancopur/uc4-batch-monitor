# UC4 Batch Monitor

Spring Batch application for copying team-owned Automic UC4 workload data into a reporting database, detecting runtime anomalies, and sending daily HTML reports.

## Jobs

Run one job at a time with `spring.batch.job.name`.

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.name=uc4MetadataSyncJob"
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.name=uc4RuntimeAnomalyDetectionJob"
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.batch.job.name=uc4DailyReportMailJob"
```

## Data Flow

- `uc4MetadataSyncJob`: truncates and reloads `uc4_job_definition` and `uc4_job_run_history`.
- `uc4RuntimeAnomalyDetectionJob`: reads the last 30 days from `uc4_job_run_history` and writes anomalies to `uc4_job_anomaly_log`.
- `uc4DailyReportMailJob`: reads active rows from `uc4_report_subscription`, builds a daily report, and includes anomaly records for the same business date.

## Configuration

UC4 source SQL is kept in separate resource files and referenced from `src/main/resources/application.yml`:

- `src/main/resources/sql/uc4-job-definitions.sql`
- `src/main/resources/sql/uc4-run-history.sql`

Target insert SQL is also kept in resource files:

- `src/main/resources/sql/target/insert-job-definition.sql`
- `src/main/resources/sql/target/insert-run-history.sql`
- `src/main/resources/sql/target/insert-anomaly-log.sql`

Keep the aliases used by the app:

- Definition query: `uc4_object_id`, `job_name`, `object_type`, `plan_name`, `folder_path`, `team_code`, `active`
- Run history query: `uc4_run_id`, `job_name`, `plan_name`, `start_time`, `end_time`, `duration_seconds`, `status`, `return_code`, `last_report`, `business_date`

The reporting scope is data-driven. Add or remove active rows in `uc4_report_subscription`:

```sql
insert into uc4_report_subscription (scope_type, scope_value, active)
values ('JOB', 'MY_JOB', true), ('PLAN', 'MY_DAILY_PLAN', true);
```
