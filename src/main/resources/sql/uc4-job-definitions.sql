with workflow_memberships as (
  select distinct
    parent_oh.oh_client as client_id,
    jpp.jpp_object as job_name,
    jpp.jpp_otype as object_type,
    parent_oh.oh_name as plan_name
  from jpp
  join oh parent_oh
    on parent_oh.oh_idnr = jpp.jpp_oh_idnr
   and parent_oh.oh_otype in ('JOBP', 'JSCH')
   and nvl(parent_oh.oh_deleteflag, 0) = 0
  where (:uc4Client is null or parent_oh.oh_client = :uc4Client)
),
folder_paths as (
  select
    ofs.ofs_oh_idnr_o as oh_idnr,
    listagg(folder_oh.oh_name, '/') within group (order by ofs.ofs_level) as folder_path
  from ofs
  join oh folder_oh
    on folder_oh.oh_idnr = ofs.ofs_oh_idnr_f
  where nvl(ofs.ofs_link, 0) = 0
  group by ofs.ofs_oh_idnr_o
)
select distinct
  cast(object_oh.oh_idnr as varchar2(128)) as uc4_object_id,
  object_oh.oh_name as job_name,
  object_oh.oh_otype as object_type,
  workflow_memberships.plan_name as plan_name,
  folder_paths.folder_path as folder_path,
  :teamCode as team_code,
  case when nvl(object_oh.oh_deleteflag, 0) = 0 then 1 else 0 end as active
from oh object_oh
left join workflow_memberships
  on workflow_memberships.job_name = object_oh.oh_name
 and workflow_memberships.object_type = object_oh.oh_otype
 and workflow_memberships.client_id = object_oh.oh_client
left join folder_paths
  on folder_paths.oh_idnr = object_oh.oh_idnr
where object_oh.oh_name like :teamNamePattern
  and object_oh.oh_otype in ('JOBS', 'JOBP', 'JSCH')
  and nvl(object_oh.oh_deleteflag, 0) = 0
  and (:uc4Client is null or object_oh.oh_client = :uc4Client)
