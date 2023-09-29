--
-- TCapture rdb_bdr structure rdb schema
--
#
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;
#
CREATE SCHEMA IF NOT EXISTS _rdb_bdr;

DROP EVENT TRIGGER IF EXISTS  ddl_event_trigger;
DROP EVENT TRIGGER IF EXISTS  sqldrop_event_trigger;
#
--ALTER SCHEMA _rdb_bdr OWNER TO rdbbdr_user;
#
CREATE OR REPLACE FUNCTION _rdb_bdr.hex2dec(text) RETURNS bigint
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
--ALTER FUNCTION _rdb_bdr.hex2dec(text) OWNER TO rdbbdr_user;
#
CREATE SEQUENCE IF NOT EXISTS _rdb_bdr.walq__master_wid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
#
--ALTER TABLE _rdb_bdr.walq__master_wid_seq OWNER TO rdbbdr_user;
#
SET default_tablespace = '';
#
#

CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_xid (
    xid_from_queue bigint NOT NULL,
    xid_current bigint NOT NULL,
	lsn pg_lsn NOT NULL, --AGGIUNTO
    dateop timestamp without time zone DEFAULT current_timestamp
	--CONSTRAINT walq__master_xid_pkey PRIMARY KEY (xid_current)
)
partition by LIST (substring(lsn::text,1,position('/' in lsn::text)-1 ) );

#
#
--ALTER TABLE _rdb_bdr.walq__master_xid OWNER TO rdbbdr_user;
#
CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master (
    wid bigint DEFAULT nextval('_rdb_bdr.walq__master_wid_seq'::regclass) NOT NULL,
    lsn pg_lsn NOT NULL,
    xid bigint NOT NULL,
    data text NOT NULL,
    dateop timestamp without time zone DEFAULT current_timestamp,
    current_xid bigint
	--CONSTRAINT walq__master_pkey PRIMARY KEY (wid)
)
 partition by LIST (substring(lsn::text,1,position('/' in lsn::text)-1 ) );

#--CREATE INDEX walq__master_idx ON _rdb_bdr.walq__master USING btree (xid, lsn);
#
--ALTER TABLE _rdb_bdr.walq__master OWNER TO rdbbdr_user;
#
#
CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_filtro (
    schemaf character varying(30) NOT NULL,
    tablef character varying(30) NOT NULL,
    opdml character(3),
    dateop timestamp without time zone,
	 CONSTRAINT walq__master_filtro_pkey PRIMARY KEY (schemaf, tablef)
);
#
--ALTER TABLE _rdb_bdr.walq__master_filtro OWNER TO rdbbdr_user;
#
CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_log (
    wid bigint,
    lsn pg_lsn,
    xid bigint,
    data text,
    dateop timestamp without time zone DEFAULT current_timestamp,
    datecommit timestamp without time zone DEFAULT current_timestamp
);
#
--ALTER TABLE _rdb_bdr.walq__master_log OWNER TO rdbbdr_user;
#
CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_offset (
    src_topic_id character varying NOT NULL,
    last_offset bigint NOT NULL,
    xid_offset bigint NOT NULL,
    lsn_offset pg_lsn NOT NULL,
    dateop timestamp without time zone DEFAULT current_timestamp
);
#
--ALTER TABLE _rdb_bdr.walq__master_offset OWNER TO rdbbdr_user;
#
create table if not exists _rdb_bdr.walq__master_mon
(
db_xid_last_committed bigint,
db_last_committed_dateop timestamp without time zone,
wal_lsn pg_lsn,
q_xid bigint,
q_dateop  timestamp without time zone,
q_lsn pg_lsn,
state  boolean,
check_dateop timestamp without time zone default current_timestamp
);
#
--ALTER TABLE _rdb_bdr.walq__master_mon OWNER TO rdbbdr_user;
#

CREATE SEQUENCE IF NOT EXISTS _rdb_bdr.tc_process_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

CREATE TABLE IF NOT EXISTS _rdb_bdr.tc_process
(
    n_id integer NOT NULL DEFAULT nextval('_rdb_bdr.tc_process_id_seq'::regclass),
    n_name character varying COLLATE pg_catalog."default" NOT NULL,
    n_shouldbe character varying COLLATE pg_catalog."default" NOT NULL,
    n_state character varying COLLATE pg_catalog."default" NOT NULL,
    n_operation character varying COLLATE pg_catalog."default" NOT NULL,
    n_type character(1) COLLATE pg_catalog."default" NOT NULL,
    n_mstr character varying COLLATE pg_catalog."default" NOT NULL,
    n_dateop timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    n_datecrea timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    n_pid integer,
    CONSTRAINT tc_process_pkey PRIMARY KEY (n_id, n_type)
);
#
--ALTER TABLE _rdb_bdr.tc_process
--    OWNER to rdbbdr_user;
#
CREATE TABLE IF NOT EXISTS _rdb_bdr.tc_monit
(
    db_xid_last_committed bigint,
    db_last_committed_dateop timestamp without time zone,
    wal_lsn pg_lsn,
    q_xid bigint,
    q_dateop timestamp without time zone,
    q_lsn pg_lsn,
    state boolean,
    check_dateop timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    n_mstr character varying(20) COLLATE pg_catalog."default" NOT NULL,
    n_slv character varying(20) COLLATE pg_catalog."default" NOT NULL,
    flushed_lsn pg_lsn,
    xid_offset bigint,
    CONSTRAINT tc_monit_pkey PRIMARY KEY (n_mstr, n_slv)
);
#
--ALTER TABLE _rdb_bdr.tc_monit
 --   OWNER to rdbbdr_user;
#
#
--ALTER TABLE _rdb_bdr.walq__master_xid OWNER TO rdbbdr_user;
#

CREATE OR REPLACE FUNCTION _rdb_bdr.upd_walq__master_hist() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
 IF (TG_OP = 'INSERT') THEN
 INSERT INTO walq__master_log (wid, lsn, xid, data ) VALUES (NEW.wid, NEW.lsn, NEW.xid, NEW.data);
 RETURN NEW;
 END IF;
 IF (TG_OP = 'UPDATE') THEN
  INSERT INTO walq__master_log (wid, lsn, xid, data ) VALUES (NEW.wid, NEW.lsn, NEW.xid, NEW.data);
  RETURN NEW;
 END IF;
END
$$;
#
#
--CREATE TRIGGER walq_trigger AFTER INSERT OR UPDATE ON _rdb_bdr.walq__master FOR EACH ROW EXECUTE PROCEDURE _rdb_bdr.upd_walq__master_hist();
#
CREATE SEQUENCE IF NOT EXISTS _rdb_bdr.ddl_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;
#
--ALTER SEQUENCE _rdb_bdr.ddl_id_seq
 --   OWNER TO rdbbdr_user;
#
--CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_ddl
CREATE TABLE IF NOT EXISTS _rdb_bdr.tc_event_ddl
(
    ddl_id bigint NOT NULL DEFAULT nextval('_rdb_bdr.ddl_id_seq'::regclass),
    wal_lsn pg_lsn NOT NULL,
    wal_txid bigint NOT NULL,
    ddl_user text COLLATE pg_catalog."default",
    ddl_object text COLLATE pg_catalog."default" NOT NULL,
    ddl_type character varying(50) COLLATE pg_catalog."default" NOT NULL,
    ddl_command text COLLATE pg_catalog."default" NOT NULL,
    creation_timestamp timestamp with time zone NOT NULL,
    CONSTRAINT ddl_master_pkey PRIMARY KEY (ddl_id)
);
#
CREATE OR REPLACE  FUNCTION _rdb_bdr.ddl_event()
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
 INSERT INTO _rdb_bdr.tc_event_ddl
 (wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp)
 VALUES (pg_current_wal_lsn(),txid_current(), CURRENT_USER, ddl_command.object_identity, 'DDL', ddl_command.command_tag, NOW());
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
EXECUTE PROCEDURE _rdb_bdr.ddl_event();
#

CREATE or replace FUNCTION _rdb_bdr.sql_event()
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
 INSERT INTO _rdb_bdr.tc_event_ddl
 (wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp)
 VALUES (pg_current_wal_lsn(),txid_current(), CURRENT_USER, ddl_command.object_identity, 'DDL', 'DROP TABLE', NOW());
 END IF;
 END LOOP;
END;
$BODY$;
#

CREATE EVENT TRIGGER sqldrop_event_trigger ON SQL_DROP
EXECUTE PROCEDURE _rdb_bdr.sql_event();



--INSERT INTO _rdb_bdr.walq__master_ddl (ddl_id, wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp )
--VALUES
--(-1,pg_current_wal_lsn(), txid_current(), CURRENT_USER, 'NO_ACTIVITY_LSN_ACK', 'DML', 'UPSERT', NOW()) ON CONFLICT (ddl_id) DO UPDATE SET wal_lsn=pg_current_wal_lsn(), creation_timestamp=NOW();
#

