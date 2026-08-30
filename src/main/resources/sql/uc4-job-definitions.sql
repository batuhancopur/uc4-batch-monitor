select
  cast(oh.oh_idnr as varchar(128)) as uc4_object_id,
  oh.oh_name as job_name,
  oh.oh_otype as object_type,
  cast(null as varchar(255)) as plan_name,
  cast(null as varchar(1024)) as folder_path,
  :teamCode as team_code,
  case when oh.oh_deleteflag = 0 then true else false end as active
from oh
where oh.oh_name like :teamNamePattern
  and oh.oh_otype in ('JOBS', 'JOBP', 'JSCH')
