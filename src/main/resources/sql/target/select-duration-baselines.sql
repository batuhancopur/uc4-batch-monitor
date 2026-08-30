select
  job_name,
  plan_name,
  count(*) as run_count,
  avg(duration_seconds) as avg_duration_seconds,
  min(duration_seconds) as min_duration_seconds,
  max(duration_seconds) as max_duration_seconds
from uc4_job_run_history
where business_date >= :from
  and business_date < :to
  and duration_seconds is not null
  and coalesce(status, '') not in ('FAILED', 'ABENDED')
group by job_name, plan_name
having count(*) >= :minimumRuns
