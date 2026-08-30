with candidate_runs as (
  select
    ah.ah_client,
    ah.ah_idnr,
    ah.ah_name,
    ah.ah_otype,
    ah.ah_parentprc,
    ah.ah_topnr,
    ah.ah_timestamp1,
    ah.ah_timestamp2,
    ah.ah_timestamp3,
    ah.ah_timestamp4,
    ah.ah_runtime,
    ah.ah_status,
    ah.ah_retcode
  from ah
  where ah.ah_name like :teamNamePattern
    and ah.ah_otype in ('JOBS', 'JOBP', 'JSCH')
    and nvl(ah.ah_deleteflag, 0) = 0
    and nvl(ah.ah_timestamp2, ah.ah_timestamp1) >= :lookbackStart
    and (:uc4Client is null or ah.ah_client = :uc4Client)
),
last_report_lines as (
  select
    rt_ah_idnr,
    last_report
  from (
    select
      rt.rt_ah_idnr,
      coalesce(dbms_lob.substr(rt.rt_content, 4000, 1), rt.rt_msginsert) as last_report,
      row_number() over (partition by rt.rt_ah_idnr order by rt.rt_lnr desc) as row_number_desc
    from rt
    join candidate_runs
      on candidate_runs.ah_idnr = rt.rt_ah_idnr
  )
  where row_number_desc = 1
)
select
  cast(candidate_runs.ah_idnr as varchar2(128)) as uc4_run_id,
  candidate_runs.ah_name as job_name,
  coalesce(parent_run.ah_name, top_run.ah_name) as plan_name,
  nvl(candidate_runs.ah_timestamp2, candidate_runs.ah_timestamp1) as start_time,
  coalesce(candidate_runs.ah_timestamp4, candidate_runs.ah_timestamp3) as end_time,
  nvl(
    candidate_runs.ah_runtime,
    round(
      (cast(coalesce(candidate_runs.ah_timestamp4, candidate_runs.ah_timestamp3) as date)
        - cast(nvl(candidate_runs.ah_timestamp2, candidate_runs.ah_timestamp1) as date)) * 86400
    )
  ) as duration_seconds,
  cast(candidate_runs.ah_status as varchar2(64)) as status,
  candidate_runs.ah_retcode as return_code,
  last_report_lines.last_report as last_report,
  trunc(nvl(candidate_runs.ah_timestamp2, candidate_runs.ah_timestamp1)) as business_date
from candidate_runs
left join ah parent_run
  on parent_run.ah_idnr = candidate_runs.ah_parentprc
 and parent_run.ah_client = candidate_runs.ah_client
 and parent_run.ah_otype in ('JOBP', 'JSCH')
left join ah top_run
  on top_run.ah_idnr = candidate_runs.ah_topnr
 and top_run.ah_client = candidate_runs.ah_client
 and top_run.ah_otype in ('JOBP', 'JSCH')
left join last_report_lines
  on last_report_lines.rt_ah_idnr = candidate_runs.ah_idnr
