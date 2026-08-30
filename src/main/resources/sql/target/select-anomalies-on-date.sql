select
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
from uc4_job_anomaly_log
where business_date = :businessDate
order by job_name, anomaly_type
