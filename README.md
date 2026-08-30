# UC4 Batch Monitor

Spring Batch application for copying team-owned Automic UC4 workload data into a reporting database, detecting runtime anomalies, and sending daily HTML reports.

The application assumes Oracle for both the UC4 source and the target reporting database.
Target tables intentionally do not use foreign keys.

## Jobs

Run one job at a time with `--jobName`. The application disables Spring Boot's default batch launcher so that a parameterless startup cannot run all jobs.

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--jobName=uc4MetadataSyncJob"
mvn spring-boot:run -Dspring-boot.run.arguments="--jobName=uc4RuntimeAnomalyDetectionJob --businessDate=2026-08-30"
mvn spring-boot:run -Dspring-boot.run.arguments="--jobName=uc4DailyReportMailJob --businessDate=2026-08-30"
```

## Data Flow

- `uc4MetadataSyncJob`: deletes and reloads `uc4_job_definition` and `uc4_job_run_history` in one transaction.
- `uc4RuntimeAnomalyDetectionJob`: reads the last 30 days from `uc4_job_run_history` and writes anomalies to `uc4_job_anomaly_log`.
- `uc4DailyReportMailJob`: reads active rows from `uc4_report_subscription`, builds a daily report, and includes anomaly records for the same business date.

## Configuration

UC4 source SQL is kept in separate resource files and referenced from `src/main/resources/application.yml`:

- `src/main/resources/sql/uc4-job-definitions.sql`
- `src/main/resources/sql/uc4-run-history.sql`

The source SQL uses these Automic tables:

- `OH` for object definitions
- `JPP` for workflow/schedule task membership
- `OFS` for folder paths
- `AH` for archived run history and parent/top workflow run information
- `RT` for the latest report line stored in the database

Target insert SQL is also kept in resource files:

- `src/main/resources/sql/target/insert-job-definition.sql`
- `src/main/resources/sql/target/insert-run-history.sql`
- `src/main/resources/sql/target/insert-anomaly-log.sql`

Target select/delete SQL is kept in resource files too:

- `src/main/resources/sql/target/select-runs-on-date.sql`
- `src/main/resources/sql/target/select-duration-baselines.sql`
- `src/main/resources/sql/target/select-active-definition-names.sql`
- `src/main/resources/sql/target/delete-anomalies-for-date.sql`
- `src/main/resources/sql/target/select-anomalies-on-date.sql`
- `src/main/resources/sql/target/select-active-report-subscriptions.sql`
- `src/main/resources/sql/target/delete-run-history.sql`
- `src/main/resources/sql/target/delete-job-definition.sql`

Keep the aliases used by the app:

- Definition query: `uc4_object_id`, `job_name`, `object_type`, `plan_name`, `folder_path`, `team_code`, `active`
- Run history query: `uc4_run_id`, `job_name`, `plan_name`, `start_time`, `end_time`, `duration_seconds`, `status`, `return_code`, `last_report`, `business_date`

The reporting scope is data-driven. Add or remove active rows in `uc4_report_subscription`:

```sql
insert into uc4_report_subscription (scope_type, scope_value, active)
values ('JOB', 'MY_JOB', 1), ('PLAN', 'MY_DAILY_PLAN', 1);
```

If there are no active report subscriptions, the mail job does not include every job by default. Missing-run anomaly detection is also disabled by default because it needs reliable schedule/calendar data to avoid false positives.
