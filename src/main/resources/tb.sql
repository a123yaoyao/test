-- Create table
create table EAF_USER_RECORD
(
  eaf_id        VARCHAR2(200) not null,
  eaf_db_name   VARCHAR2(200),
  eaf_name      VARCHAR2(200),
  eaf_loginname VARCHAR2(200)
)


  create table EAF_ORG_RECORD
(
  eaf_id        VARCHAR2(200) not null,
  eaf_db_name   VARCHAR2(200),
  eaf_name      VARCHAR2(200),--部门名称
  BIM_NUM VARCHAR2(200) --部门编号
)

-- Create table
create table WF_EVENT_JOB_DETAILS
(
  job_name          VARCHAR2(80) not null,
  job_group         VARCHAR2(80) not null,
  description       VARCHAR2(120),
  job_class_name    VARCHAR2(128) not null,
  is_durable        VARCHAR2(1) not null,
  is_volatile       VARCHAR2(1) not null,
  requests_recovery VARCHAR2(1) not null,
  job_data          LONG RAW
)
tablespace TIEAF_SYS
  pctfree 10
  initrans 1
  maxtrans 255
  storage
  (
    initial 64K
    next 1M
    minextents 1
    maxextents unlimited
  );
-- Create/Recreate primary, unique and foreign key constraints
create table WF_EVENT_JOB_DETAILS
(
  job_name          VARCHAR2(80) not null,
  job_group         VARCHAR2(80) not null,
  description       VARCHAR2(120),
  job_class_name    VARCHAR2(128) not null,
  is_durable        VARCHAR2(1) not null,
  is_volatile       VARCHAR2(1) not null,
  requests_recovery VARCHAR2(1) not null,
  job_data          LONG RAW
)
alter table WF_EVENT_JOB_DETAILS
  add primary key (JOB_NAME, JOB_GROUP)

  create table WF_EVENT_TRIGGERS
(
  trigger_name   VARCHAR2(80) not null,
  trigger_group  VARCHAR2(80) not null,
  job_name       VARCHAR2(80) not null,
  job_group      VARCHAR2(80) not null,
  is_volatile    VARCHAR2(1) not null,
  description    VARCHAR2(120),
  next_fire_time NUMBER(13),
  prev_fire_time NUMBER(13),
  trigger_state  VARCHAR2(16) not null,
  trigger_type   VARCHAR2(8) not null,
  start_time     NUMBER(13) not null,
  end_time       NUMBER(13),
  calendar_name  VARCHAR2(80),
  misfire_instr  NUMBER(2)
)
alter table WF_EVENT_TRIGGERS
  add primary key (TRIGGER_NAME, TRIGGER_GROUP)



