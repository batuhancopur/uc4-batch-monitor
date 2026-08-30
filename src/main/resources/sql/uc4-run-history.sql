select
  cast(ah.ah_idnr as varchar(128)) as uc4_run_id,
  ah.ah_name as job_name,
  cast(null as varchar(255)) as plan_name,
  ah.ah_timestamp1 as start_time,
  ah.ah_timestamp2 as end_time,
  extract(epoch from (ah.ah_timestamp2 - ah.ah_timestamp1)) as duration_seconds,
  cast(ah.ah_status as varchar(64)) as status,
  ah.ah_retcode as return_code,
  cast(null as varchar(4000)) as last_report,
  cast(ah.ah_timestamp1 as date) as business_date
from ah
where ah.ah_name like :teamNamePattern
  and ah.ah_timestamp1 >= :lookbackStart
