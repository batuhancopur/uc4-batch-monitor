insert into uc4_job_definition (
  uc4_object_id,
  job_name,
  object_type,
  plan_name,
  folder_path,
  team_code,
  active
) values (
  :uc4ObjectId,
  :jobName,
  :objectType,
  :planName,
  :folderPath,
  :teamCode,
  :active
)
