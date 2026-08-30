insert into uc4_job_anomaly_log (
  job_name,
  plan_name,
  run_id,
  business_date,
  anomaly_type,
  actual_duration_seconds,
  avg_duration_seconds,
  min_duration_seconds,
  max_duration_seconds,
  deviation_percent,
  description
) values (
  :jobName,
  :planName,
  :runId,
  :businessDate,
  :anomalyType,
  :actualDurationSeconds,
  :avgDurationSeconds,
  :minDurationSeconds,
  :maxDurationSeconds,
  :deviationPercent,
  :description
)
