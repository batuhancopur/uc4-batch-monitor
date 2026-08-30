select
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
from uc4_job_run_history
where business_date = :businessDate
order by coalesce(plan_name, ''), job_name, start_time
