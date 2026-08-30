insert into uc4_job_run_history (
  uc4_run_id,
  job_name,
  plan_name,
  start_time,
  end_time,
  duration_seconds,
  status,
  return_code,
  last_report,
  business_date
) values (
  :uc4RunId,
  :jobName,
  :planName,
  :startTime,
  :endTime,
  :durationSeconds,
  :status,
  :returnCode,
  :lastReport,
  :businessDate
)
