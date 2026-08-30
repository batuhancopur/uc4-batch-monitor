create table if not exists uc4_job_definition (
    id bigserial primary key,
    uc4_object_id varchar(128) not null,
    job_name varchar(255) not null,
    object_type varchar(32) not null,
    plan_name varchar(255),
    folder_path varchar(1024),
    team_code varchar(64) not null,
    active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create unique index if not exists ux_uc4_job_definition_object
    on uc4_job_definition (uc4_object_id);

create index if not exists ix_uc4_job_definition_name
    on uc4_job_definition (job_name);

create table if not exists uc4_job_run_history (
    id bigserial primary key,
    uc4_run_id varchar(128) not null,
    job_name varchar(255) not null,
    plan_name varchar(255),
    start_time timestamp not null,
    end_time timestamp,
    duration_seconds bigint,
    status varchar(64),
    return_code integer,
    last_report text,
    business_date date not null,
    inserted_at timestamp not null default current_timestamp
);

create unique index if not exists ux_uc4_job_run_history_run
    on uc4_job_run_history (uc4_run_id);

create index if not exists ix_uc4_job_run_history_business_date
    on uc4_job_run_history (business_date);

create index if not exists ix_uc4_job_run_history_name_date
    on uc4_job_run_history (job_name, business_date);

create table if not exists uc4_job_anomaly_log (
    id bigserial primary key,
    job_name varchar(255) not null,
    plan_name varchar(255),
    run_id varchar(128),
    business_date date not null,
    anomaly_type varchar(64) not null,
    actual_duration_seconds bigint,
    avg_duration_seconds numeric(18, 2),
    min_duration_seconds bigint,
    max_duration_seconds bigint,
    deviation_percent numeric(8, 2),
    description text not null,
    created_at timestamp not null default current_timestamp
);

create index if not exists ix_uc4_job_anomaly_log_business_date
    on uc4_job_anomaly_log (business_date);

create table if not exists uc4_report_subscription (
    id bigserial primary key,
    scope_type varchar(16) not null check (scope_type in ('JOB', 'PLAN')),
    scope_value varchar(255) not null,
    active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create unique index if not exists ux_uc4_report_subscription_scope
    on uc4_report_subscription (scope_type, scope_value);
