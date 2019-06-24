-- Create table
create table EAF_TB_RECORD
(
  eaf_id              CHAR(32) not null primary key,
  eaf_db_name         VARCHAR2(20),
  eaf_tb_name         VARCHAR2(20),
  eaf_du_col          VARCHAR2(20),
  eaf_du_colv         VARCHAR2(20),
  eaf_re_col          VARCHAR2(20),
  eaf_re_colv         VARCHAR2(20)
)
tablespace BIMDISP_SYS
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

