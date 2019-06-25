-- Create table
create table EAF_USER_RECORD
(
  eaf_id        VARCHAR2(200) not null,
  eaf_db_name   VARCHAR2(200),
  eaf_name      VARCHAR2(200),
  eaf_loginname VARCHAR2(200)
)
tablespace TIEAF_CUS
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

  create table EAF_ORG_RECORD
(
  eaf_id        VARCHAR2(200) not null,
  eaf_db_name   VARCHAR2(200),
  eaf_name      VARCHAR2(200),--部门名称
  BIM_NUM VARCHAR2(200) --部门编号
)
tablespace TIEAF_CUS
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
