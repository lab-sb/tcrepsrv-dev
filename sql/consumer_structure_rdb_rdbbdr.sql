--
-- TCapture slave structure rdb schema
--
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
#
--ALTER SCHEMA _rdb_bdr OWNER TO rdbbdr_user;
#
SET default_tablespace = '';
#
SET default_with_oids = false;

DROP EVENT TRIGGER IF EXISTS  ddl_event_trigger;
DROP EVENT TRIGGER IF EXISTS  sqldrop_event_trigger;
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



CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master (
    wid bigint NOT NULL,
    lsn pg_lsn NOT NULL,
    xid bigint NOT NULL,
    data text,
    dateop timestamp without time zone DEFAULT current_timestamp,
    current_xid bigint
	-- CONSTRAINT walq__master_pkey PRIMARY KEY (wid)
)
 partition by LIST (substring(lsn::text,1,position('/' in lsn::text)-1 ) );

#
--ALTER TABLE _rdb_bdr.walq__master OWNER TO postgres;
#
--CREATE INDEX walq__master_idx ON _rdb_bdr.walq__master USING btree (xid, lsn);
#
CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_conflicts (
    wid bigint NOT NULL,
    xid bigint NOT NULL,
    schemaf character varying(30) NOT NULL,
    tablef character varying(30) NOT NULL,
    opdml character(1),
    state text,
    message text,
    detail text,
    hint text,
    context text,
    dateop timestamp without time zone DEFAULT current_timestamp
);
#
--ALTER TABLE _rdb_bdr.walq__master_conflicts OWNER TO rdbbdr_user;
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
--ALTER TABLE _rdb_bdr.walq__master_log OWNER TO postgres;
#
CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_offset (
    src_topic_id character varying NOT NULL,
    last_offset bigint NOT NULL,
    xid_offset bigint NOT NULL,
    lsn_offset pg_lsn NOT NULL,
    dateop timestamp without time zone DEFAULT current_timestamp,
    CONSTRAINT walq__master_offset_pkey PRIMARY KEY (src_topic_id)
);


--CREATE TABLE IF NOT EXISTS _rdb_bdr.walq__master_offset (
--    src_topic_id character varying NOT NULL,
--    last_offset bigint,
--    xid_offset bigint,
--    dateop timestamp without time zone DEFAULT current_timestamp,
--    xid_lasttrunc bigint,
--	CONSTRAINT walq__master_offset_pkey PRIMARY KEY (src_topic_id)
--);
#
--ALTER TABLE _rdb_bdr.walq__master_offset OWNER TO postgres;
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
CREATE or replace FUNCTION _rdb_bdr.monitor_master_to_(slv varchar ) RETURNS integer AS $$
 DECLARE
    myrec RECORD;
BEGIN
RAISE NOTICE 'Monitoring % <-- master ',slv ;
FOR myrec IN
select   attivo.exists as is_active,n_state ,n_shouldbe,n_dateop,n_operation,n_type,q_xid,q_lsn,q_dateop,state, check_dateop,xid_offset
 from _rdb_bdr.tc_monit m , _rdb_bdr.tc_process p ,
 ( select exists (select  1 from   pg_stat_activity where  query_start > now() - interval '3 seconds' and   substring(query,2,38) = 'select distinct xid,dateop from walq__' ) ) attivo
  where p.n_mstr = 'master' and m.n_mstr = 'master'
  LOOP

		RAISE NOTICE 'Slave %  ',slv;
        RAISE NOTICE 'is_active:% | state:% | shouldbe:% | dateop:% | op:% | type:%',
                     myrec.is_active,
                     quote_ident(myrec.n_state),
                     quote_ident(myrec.n_shouldbe),
					 myrec.n_dateop,
					 quote_ident(myrec.n_operation),
					 quote_ident(myrec.n_type)
					 ;
       -- EXECUTE format('Monitoring %I.%I', myrec.n_dateop, myrec.n_operation);
    END LOOP;
RAISE NOTICE 'Queue master on node % ',slv ;
FOR myrec IN
 select
last_offset as wid, xid_offset as xid , dateop, '<< CURRENT MARKER >> ********************************************************************************' as sintesi
from _rdb_bdr.walq__master_offset
union
(select
wid,xid,dateop,substring(data,1,100) as sintesi
from _rdb_bdr.walq__master where wid >= (select last_offset from _rdb_bdr.walq__master_offset )
order by wid limit 10  )
union
 (select wid,xid,dateop,substring(data,1,100) as sintesi
from _rdb_bdr.walq__master order by wid desc limit 10  )
union
  	select foo.wid, foo.xid,  foo.dateop  ,foo.sintesi
	from (  select q.wid,c.xid, substring(message,1,200) as sintesi, substring(detail,1,200)  ,c.dateop
			from
				_rdb_bdr.walq__master_conflicts c,  _rdb_bdr.walq__master q
				where c.xid > (select xid_offset from _rdb_bdr.walq__master_offset)
				and q.xid=c.xid) as foo  order by wid desc
	LOOP
		RAISE NOTICE 'wid:% | xid:% |  dateop:% | sintesi:% ',
                     myrec.wid,
                     myrec.xid,
					 myrec.dateop,
					 myrec.sintesi
					 ;
		 END LOOP;
    RAISE NOTICE 'Done Monitoring  % <-- master',slv;
RETURN 1;
END;
$$ LANGUAGE plpgsql;


CREATE SEQUENCE IF NOT EXISTS _rdb_bdr.ddl_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;

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
CREATE OR REPLACE FUNCTION _rdb_bdr.ddl_event()
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

