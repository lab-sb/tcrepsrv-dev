--
-- TCapture master structure rdb schema
--
#
SET statement_timeout = 0;
SET lock_timeout = 0;
--SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;
#
CREATE SCHEMA IF NOT EXISTS dba;
#
DROP EVENT TRIGGER IF EXISTS  ddl_event_trigger;
DROP EVENT TRIGGER IF EXISTS  sqldrop_event_trigger;
--ALTER SCHEMA _rdb_bdr OWNER TO rdbbdr_user;
#
#
SET default_tablespace = '';
#
SET default_with_oids = false;

CREATE OR REPLACE FUNCTION dba.hex2dec(text) RETURNS bigint
    LANGUAGE sql
    AS $_$
SELECT sum(digit * 16 ^ (length($1)-pos)) ::bigint
FROM (SELECT case
when digit between '0' and '9' then ascii(digit) - 48
when digit between 'A' and 'F' then ascii(digit) - 55
end,
pos as i
FROM (SELECT substring(c from x for 1), x
FROM (values(upper($1))) as a(c),
generate_series(1,length($1)) as t(x))
as u(digit, pos)
) as v(digit, pos);
$_$;

#
--CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_xid (
--    xid_from_queue bigint NOT NULL,
--    xid_current bigint NOT NULL,
--    dateop timestamp without time zone DEFAULT current_timestamp,
--	CONSTRAINT walq__master_xid_pkey PRIMARY KEY (xid_current)
--);
#
--ALTER TABLE _rdb_bdr.walq__master_xid OWNER TO rdbbdr_user;
#
CREATE SEQUENCE IF NOT EXISTS dba.ddl_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;
#
--ALTER SEQUENCE _rdb_bdr.ddl_id_seq
--    OWNER TO rdbbdr_user;
#
--CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_ddl
CREATE TABLE IF NOT EXISTS dba.__events_ddl
(
    ddl_id bigint NOT NULL DEFAULT nextval('dba.ddl_id_seq'::regclass),
    ddl_origin character varying(10) not null default 'master',
    wal_lsn pg_lsn NOT NULL,
    wal_txid bigint NOT NULL,
    ddl_user text COLLATE pg_catalog."default",
    ddl_object text COLLATE pg_catalog."default" NOT NULL,
    ddl_type character varying(50) COLLATE pg_catalog."default" NOT NULL,
    ddl_command text COLLATE pg_catalog."default" NOT NULL,
    creation_timestamp timestamp with time zone NOT NULL,
    CONSTRAINT ddl_master_pkey PRIMARY KEY (ddl_id,ddl_origin)
);
#
CREATE or replace FUNCTION dba.ddl_event()
    RETURNS event_trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
DECLARE
 ddl_command RECORD;
BEGIN
 FOR ddl_command IN SELECT * FROM pg_event_trigger_ddl_commands()
 LOOP
 IF ddl_command.object_type = 'table'
 THEN
 INSERT INTO dba.__events_ddl
 (wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp)
 VALUES (pg_current_xlog_location(),txid_current(), CURRENT_USER, ddl_command.object_identity, 'DDL', ddl_command.command_tag, NOW());
 END IF;
 END LOOP;
END;
$BODY$;
#
--ALTER FUNCTION _rdb_bdr.ddl_event()
--    OWNER TO rdbbdr_user;
#
CREATE EVENT TRIGGER ddl_event_trigger ON DDL_COMMAND_END
 WHEN TAG IN ('CREATE TABLE', 'ALTER TABLE',  'DROP TABLE')
EXECUTE PROCEDURE dba.ddl_event();
#

CREATE or replace FUNCTION dba.sql_event()
    RETURNS event_trigger
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE NOT LEAKPROOF
AS $BODY$
DECLARE
 ddl_command RECORD;
BEGIN
 FOR ddl_command IN SELECT * FROM pg_event_trigger_dropped_objects()
 LOOP
 IF ddl_command.object_type = 'table'
 THEN
 INSERT INTO dba.__events_ddl
 (wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp)
 VALUES (pg_current_xlog_location(),txid_current(), CURRENT_USER, ddl_command.object_identity, 'DDL', 'DROP TABLE', NOW());
 END IF;
 END LOOP;
END;
$BODY$;

CREATE EVENT TRIGGER sqldrop_event_trigger ON SQL_DROP
EXECUTE PROCEDURE dba.sql_event();



--INSERT INTO _rdb_bdr.walq__master_ddl (ddl_id, wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp )
INSERT INTO dba.__events_ddl (ddl_id, wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp )
VALUES
(-1,pg_current_xlog_location(), txid_current(), CURRENT_USER, 'NO_ACTIVITY_LSN_ACK', 'DML', 'UPSERT', NOW()) ON CONFLICT (ddl_id,ddl_origin) DO UPDATE SET wal_lsn=pg_current_xlog_location(), creation_timestamp=NOW();
#
--CREATE TABLE IF NOT EXISTS _rdb_bdr.tc_monit
-- ( mstr_id  VARCHAR NOT NULL,
--   tx_id NUMERIC,
--   tx_src_dateop TIMESTAMP WITHOUT TIME ZONE,
--   tx_target_dateop  TIMESTAMP WITHOUT TIME ZONE,
--   tx_lsn pg_lsn,
--   CONSTRAINT tc_monit_pkey PRIMARY KEY (mstr_id) ) WITH (OIDS=FALSE);
#
--INSERT INTO _rdb_bdr.tc_monit   ("mstr_id", "tx_id","tx_src_dateop","tx_lsn") values ('master',1,current_timestamp, pg_current_wal_lsn() )
--ON CONFLICT (mstr_id) DO UPDATE SET tx_id=1, tx_src_dateop=current_timestamp ,tx_lsn=pg_current_wal_lsn() ;
#
--INSERT INTO dba.__events_ddl ( wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp )
--VALUES
--(pg_current_wal_lsn(), txid_current(), CURRENT_USER, 'master','MASTER', 'CREATE NODE',  NOW()) ON CONFLICT (ddl_id) DO UPDATE SET wal_lsn=pg_current_wal_lsn(), creation_timestamp=NOW();

grant all on dba.__events_ddl to public;
grant all on dba.ddl_id_seq to public;
grant usage on schema dba to public;

