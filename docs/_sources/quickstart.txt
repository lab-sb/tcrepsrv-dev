.. _quickstart:

Quickstart
=======================

First, complete the :ref:`installation` process.


Tutorial TCapture
-----------------

Learn how to use the T-Capture technology to replicate data over tables queues. 
	This tutorial explains how to set up and run T-Capture Replication Server .

Learning objectives
-------------------
After you complete this tutorial, you will know how to:

    Create and use replication slot over logical decoding rdblogdec , queues tables, and publication/subscription. 

    Enable a Postgresql database for replication.

    Set up and operate the T Capture and T Apply programs.

    Configure the T Capture and T Apply programs to use the queues.

    Create a TCapture subscription to map a source table to a target table.


Test table definition
---------------------



.. code-block:: sql

	prd_db=# create table prova (aa serial, ab varchar(20) default 'prd' , ac timestamp default now(), ad bigint default txid_current());
	prd_db=# alter table prova add primary key (aa,ab);

	qas_db=# create table prova (aa serial, ab varchar(20) default 'qas' , ac timestamp default now(), ad bigint default txid_current());
	qas_db=# alter table prova add primary key (aa,ab);

	prd_db=# insert into prova (ab) values ('first');
	prd_db=# insert into prova (aa) select generate_series(max(aa)+1, max(aa)+1)  from prova;
	prd_db=# \watch -n1

	qas_db=# insert into prova (aa) select generate_series(max(aa)+1, max(aa)+1)  from prova;
	qas_db=# \watch -n1


Steps to configure a mmr between 2 nodes in vdc located on milan and newyork
----------------------------------------------------------------------------

.. code-block:: sh 

	-- milan 	host (milhost) db mil_db node mila
	-- newyork 	host (nychost) db nyc_db node nyci

- deploy tc custom logical decoder to PGSQL lib 

.. code-block:: sh 

	milhost:cp ./lib/rdb_decoder/rdblogdec.so /usr/pgsql-10/lib/rdblogdec.so

- add to postgresql.conf 

.. code-block:: sh 

	wal_level = logical
	max_wal_senders = 10            # max number of walsender processes
	max_replication_slots = 18
	track_commit_timestamp = on

- reload 

.. code-block:: sh 

	milhost:pg_ctl stop -D /var/lib/pgsql/10/data/ -m immediate; pg_ctl start -D /var/lib/pgsql/10/data/
 
- uncompress TCRepSrv sw

.. code-block:: sh 

	milhost:tar -zxvf tcrepsrv-0.9.1.rhel7.x86_64.tar.gz

	milhost:cd rdbdbr

- edit .rdbbdr_env.sh 

.. code-block:: sh 

	adapt line to your path export RDBBDR=/var/lib/pgsql/scripts/mycode/rdbbdr

source it

.. code-block:: sh 

	milhost:. .rdbbdr_env.sh
	milhost:echo $RDBBDR

- create replication user

.. code-block:: sh 

 	create user : create user rdbbdr_user  superuser inherit login password 'rdbbdr_pwd';

- database containing replication tables structures 

.. code-block:: sh 

	create database rdb_db__mila;

- add a primary node mila (ie milan location)

.. code-block:: sh 

	milhost:sh bin/TC_srvctl.sh --config --type producer --node mila --host milhost --port 5432 --user rdbbdr_user --passwd rdbbdr_pwd --db mil_db -rhost milhost --ruser postgres --rpasswd apwd -rport 5432

this will create the 
a)_rdb_bdr schema structure on master mila 
b) replication slot rdb_mila_bdr on mil_db 
c) parameter file for node mila under $RDBBDR/conf/mila_rdb_bdr.conf
d) mila_publ  

- do the same on newyork host

.. code-block:: sh 

	nychost:sh bin/TC_srvctl.sh --config --type producer --node nyci --host nychost --port 5432 --user rdbbdr_user --passwd rdbbdr_pwd --db nyc_db -rhost nychost --ruser postgres --rpasswd apwd -rport 5432

-  add a remote primary node conf 

.. code-block:: sh 

	nychost: sh bin/TC_srvctl.sh --config --type consumer --node mila --host milhost --port 5432 --user rdbbdr_user --passwd rdbbdr_pwd --db mil_db -rhost milhost --ruser postgres --rpasswd apwd -rport 5432

this will populate the parameter file for remote node nyci 

do the same in milan for remote primary nyci

.. code-block:: sh 

	milhost: sh bin/TC_srvctl.sh --config --type consumer --node nyci --host nychost --port 5432 --user rdbbdr_user --passwd rdbbdr_pwd --db nyc_db -rhost nychost --ruser postgres --rpasswd apwd -rport 5432

- we can setup producer mila

.. code-block:: sh

        milhost: bin/TC_srvctl.sh --setup --node mila --type producer

- we can setup producer nyci

.. code-block:: sh

        nychost: bin/TC_srvctl.sh --setup --node nyci --type producer


- we can now join the remote node (ie became a slave for the specified nyci)

.. code-block:: sh 

	milhost: bin/TC_srvctl.sh --setup --node mila --type consumer --producer nyci

- and can now join the remote node (ie became a slave for the specified mila)

.. code-block:: sh 

	nychost: bin/TC_srvctl.sh --setup --node nyci --type consumer --producer mila

this will create the 
a) mila_subs_nyci and mila_publ_nyci slot for nyci subscription 
a) nyci_subs_mila and nyci_publ_mila slot for mila subscription 


we are ready to run T-Capture program on each master:

.. code-block:: sh 

	export NODEMST=nyci
	sh bin/runTCRepSrv.sh -n nyci

running for node: nyci

.. code-block:: sh 

	Logging startup messages to : TCapture_nyci_2019-06-20-08:58:01.log

	press a to tail the log :

	/var/lib/pgsql/scripts/mycode/rdbbdr/log/nyci_rdb_bdr.log

  	[2019-06-20 09:00:29 462] [INFO   ] Start logging
  	[2019-06-20 09:00:29 947] [INFO   ] Queue XID at restart: 9855220
  	[2019-06-20 09:00:29 960] [INFO   ] Xlog Position: LSN{9/D9BEA30}
  	[2019-06-20 09:00:29 961] [INFO   ] Xlog Current : LSN{9/1363D360}
  	[2019-06-20 09:00:29 962] [INFO   ] Slot Name: rdb_nyci_bdr
  	[2019-06-20 09:00:29 962] [INFO   ]  Check LSn from stream > LSn from queue  9/D9BEA30 vs 9/D9BEA30
  	[2019-06-20 09:00:29 963] [INFO   ] LESS - stay in the loop9/D9BEA30-9/D9BEA30
  	[2019-06-20 09:00:30 068] [INFO   ]  Check LSn from stream > LSn from queue  9/1363BAB8 vs 9/D9BEA30
  	[2019-06-20 09:00:30 068] [INFO   ]  ok go on :9/1363BAB8-9/D9BEA30
  	[2019-06-20 09:00:30 070] [INFO   ] ----------------------------------------------------------------------------------------------------------------------------------------------
  	[2019-06-20 09:00:30 088] [INFO   ] <Begin Txid> :9855480 <LSN> :9/1363BAB8


- stop replication

.. code-block:: sh 

	sh bin/stopTCRepSrv.sh -n nyci

	postgres 62992 62990  0 Jun17 ?        00:07:01 /usr/java/jdk1.8.0_201-amd64/bin/java -cp ../lib/postgresql-42.2.19.jar:../pgjdbc/pgjdbc/src/:. com.edslab.RunWal
 

A full example
--------------

--preparation

.. code-block:: sh

		
		postgres@hostxx-adbdb.edslab.it

		pg_ctl start -D 10/data/weby

		10/data/weby/postgresql.conf

		max_replication_slots = 10
		max_wal_senders = 10
		###shared_preload_libraries = 'pglogical'
		track_commit_timestamp = on # 9.5+ only
		wal_level= logical

		10/data/weby/pg_hba.conf

		pg_dump -p 5433 webx -Fc --schema-only -v > webx-schema.dmp

		pg_dumpall -p 5433 -g > webx-globals.dmp

		psql -p 5434 < webx-globals.dmp

		pg_restore -p 5434 -Fc -d weby webx-schema.dmp

		vacuumdb --all --analyze-in-stages

--configure

.. code-block:: sh

		hostxx-bdbdb.edslab.it

		sh bin/TC_srvctl.sh --config --type producer --node weby --host hostxx-adbdb --port 5434 --user statusr --passwd statpwd --db weby -rhost hostxx-bdbdb --ruser postgres --rpasswd grespost -rport 5432

--setup producer

.. code-block:: sh

		bin/TC_srvctl.sh --setup --node weby --type producer

--Setup consumer

.. code-block:: sh

		sh bin/TC_srvctl.sh --setup --node webx --type consumer --producer weby

--Run TCRepSrv primary

.. code-block:: sh

		sh bin/runTCRepSrv.sh -n weby

--Primary Db
		
.. code-block:: sh

		weby=# select * from pg_replication_slots ;
		slot_name | plugin | slot_type | datoid | database | temporary | active | active_pid | xmin | catalog_xmin | restart_lsn | confirmed_flush_lsn
		--------------+-----------+-----------+--------+----------+-----------+--------+------------+------+--------------+-------------+---------------------
		rdb_weby_bdr | rdblogdec | logical | 33403 | weby | f | f | | | 26118 | 0/76D9840 | 0/76D99A8

--dba.__events_ddl

.. code-block:: sh

		weby-# ;
		ddl_id | wal_lsn | wal_txid | ddl_user | ddl_object | ddl_type | ddl_command | creation_timestamp
		--------+-----------+----------+----------+---------------------+----------+-------------+-------------------------------
		1 | 0/76CD1E0 | 25992 | statusr | weby | MASTER | CREATE NODE | 2021-03-17 16:00:54.089933+01
		-1 | 0/76D9A68 | 26120 | statusr | NO_ACTIVITY_LSN_ACK | DML | UPSERT | 2021-03-17 16:09:18.436825+01

--Primary DB RDB

.. code-block:: sh

		[ 14:06:11 Thu Mar 18 ] [ postgres@hostxx-bdbdb.edslab.it ] [ ~ ]
		(1001)$ psql -p 5432
		rdb_db__weby=# \dt _rdb_bdr.

--List of relations

.. code-block:: sh

		Schema | Name | Type | Owner
		----------+-------------------+-------+----------
		_rdb_bdr | tc_event_ddl | table | postgres
		_rdb_bdr | tc_monit | table | postgres
		_rdb_bdr | tc_process | table | postgres
		_rdb_bdr | walq__weby | table | postgres
		_rdb_bdr | walq__weby_0 | table | postgres
		_rdb_bdr | walq__weby_1 | table | postgres
		_rdb_bdr | walq__weby_2 | table | postgres
		_rdb_bdr | walq__weby_filtro | table | postgres
		_rdb_bdr | walq__weby_log | table | postgres
		_rdb_bdr | walq__weby_mon | table | postgres
		_rdb_bdr | walq__weby_offset | table | postgres
		_rdb_bdr | walq__weby_xid | table | postgres
		_rdb_bdr | walq__weby_xid_0 | table | postgres
		_rdb_bdr | walq__weby_xid_1 | table | postgres
		_rdb_bdr | walq__weby_xid_2 | table | postgres

_rdb_bdr.tc_process

.. code-block:: sh

		rdb_db__weby=# table;
		n_id | n_name | n_shouldbe | n_state | n_operation | n_type | n_mstr | n_dateop | n_datecrea | n_pid
		------+--------+------------+----------+-------------+--------+--------+----------------------------+----------------------------+-------
		0 | weby | up | shutdown | managed | C | weby | 2021-03-17 15:09:11.440533 | 2021-03-17 15:00:47.49168 | 99320
		0 | weby | up | shutdown | managed | M | weby | 2021-03-17 15:09:12.44276 | 2021-03-17 15:00:47.492616 | 366
		0 | weby | up | shutdown | managed | H | weby | 2021-03-17 15:09:13.445581 | 2021-03-17 15:00:47.493022 | 99194
		(3 rows)

--Primary RDB Publication

.. code-block:: sh

		rdb_db__weby=# \dRp+
		Publication weby_publ
		Owner | All tables | Inserts | Updates | Deletes
		----------+------------+---------+---------+---------
		postgres | f | t | f | f
		Tables:
		"_rdb_bdr.walq__weby_0"
		"_rdb_bdr.walq__weby_1"
		"_rdb_bdr.walq__weby_2"

--Primary RDB Server replication slots

.. code-block:: sh

		rdb_db__weby=# select * from pg_replication_slots ;
		slot_name | plugin | slot_type | datoid | database | temporary | active | active_pid | xmin | catalog_xmin | restart_lsn | confirmed_flush_lsn
		----------------+----------+-----------+-----------+--------------+-----------+--------+------------+------+--------------+-------------+---------------------
		webp_publ_webx | pgoutput | logical | 75365 | rdb_db__webp | f | t | 20203 | | 15484548 | 2B/C41BAFE0 | 2B/C41F5E18
		weby_publ_webx | pgoutput | logical | 158418762 | rdb_db__weby | f | t | 6353 | | 15484548 | 2B/C41BAFE0 | 2B/C41F5E18

--Secondary Db

.. code-block:: sh

		dba.__events_ddl
		webx=# table dba.__events_ddl
		webx-# ;
		ddl_id | wal_lsn | wal_txid | ddl_user | ddl_object | ddl_type | ddl_command | creation_timestamp
		--------+---------------+------------+----------+---------------------+----------+-------------+-------------------------------
		-1 | 150/C3200920 | 2398631719 | statusr | NO_ACTIVITY_LSN_ACK | DML | UPSERT | 2021-03-17 16:02:00.505278+01
		48 | 150/C320BAF0 | 11211039 | postgres | webx<-weby | SLAVE | CREATE NODE | 2021-03-17 16:02:00.518704+01

--Secondary DB RDB

.. code-block:: sh

		rdb_db__webx=# \dt _rdb_bdr.
-- List of relations

.. code-block:: sh

		List of relations
		Schema | Name | Type | Owner
		----------+----------------------+-------+----------
		_rdb_bdr | tc_event_ddl | table | postgres
		_rdb_bdr | tc_monit | table | postgres
		_rdb_bdr | tc_process | table | postgres
		_rdb_bdr | walq__webp | table | postgres
		_rdb_bdr | walq__webp_4b57 | table | postgres
		_rdb_bdr | walq__webp_4b58 | table | postgres
		_rdb_bdr | walq__webp_4b59 | table | postgres
		_rdb_bdr | walq__webp_4b5a | table | postgres
		_rdb_bdr | walq__webp_cmanaged | table | postgres
		_rdb_bdr | walq__webp_conflicts | table | postgres
		_rdb_bdr | walq__webp_crules | table | postgres
		_rdb_bdr | walq__webp_filtro | table | postgres
		_rdb_bdr | walq__webp_log | table | postgres
		_rdb_bdr | walq__webp_offset | table | postgres
		_rdb_bdr | walq__weby | table | postgres
		_rdb_bdr | walq__weby_0 | table | postgres
		_rdb_bdr | walq__weby_1 | table | postgres
		_rdb_bdr | walq__weby_2 | table | postgres
		_rdb_bdr | walq__weby_conflicts | table | postgres
		_rdb_bdr | walq__weby_filtro | table | postgres
		_rdb_bdr | walq__weby_log | table | postgres
		_rdb_bdr | walq__weby_offset | table | postgres
		rdb_db__webx=# select * from pg_replication_slots ;
		slot_name | plugin | slot_type | datoid | database | temporary | active | active_pid | xmin | catalog_xmin | restart_lsn | confirmed_flush_lsn
		-----------+--------+-----------+--------+----------+-----------+--------+------------+------+--------------+-------------+---------------------
		(0 rows)

List of subscriptions

.. code-block:: sh

		rdb_db__webx=# \dRs+
		List of subscriptions
		Name | Owner | Enabled | Publication | Synchronous commit | Conninfo
		----------------+----------+---------+-------------+--------------------+---------------------------------------------------------------------------------------
		webx_subs_webp | postgres | t | {webp_publ} | off | host=hostxx-bdbdb.edslab.it port=5432 user=postgres password=xxx dbname=rdb_db__webp
		webx_subs_weby | postgres | t | {weby_publ} | off | host=hostxx-bdbdb port=5432 user=postgres password=xxx dbname=rdb_db__weby

_rdb_bdr.tc_process;

.. code-block:: sh

		rdb_db__webx=# table _rdb_bdr.tc_process;
		n_id | n_name | n_shouldbe | n_state | n_operation | n_type | n_mstr | n_dateop | n_datecrea | n_pid
		------+--------+------------+---------+-------------+--------+--------+----------------------------+----------------------------+-------
		0 | webx | up | start | managed | S | webp | 2021-03-17 15:07:25.396114 | 2021-03-08 10:12:26.708807 | 10720
		1 | webx | up | stop | managed | S | weby | 2021-03-18 13:48:58.875954 | 2021-03-18 13:48:58.875954 | -1

_rdb_bdr.walq__weby_offset;

.. code-block:: sh

		rdb_db__webx=# table _rdb_bdr.walq__weby_offset;
		src_topic_id | last_offset | xid_offset | lsn_offset | dateop
		--------------+-------------+------------+-------------+----------------------------
		webx | 0 | 0 | 2E/E16FA000 | 2021-03-17 15:01:54.067753
		
Run TCRepSrv secondary

.. code-block:: sh

		sh bin/runTCRepSrv.sh -n webx
		Log Secondary
		INFO | 2021-03-18 13:50:35.199 | [main] edslab.TCRepSrv (TCRepSrv.java:129) - ***********************************************************************
		INFO | 2021-03-18 13:50:35.200 | [main] edslab.TCRepSrv (TCRepSrv.java:130) - Running TCapture Replication Server for node :webx
		INFO | 2021-03-18 13:50:35.200 | [main] edslab.TCRepSrv (TCRepSrv.java:131) - ***********************************************************************
		INFO | 2021-03-18 13:50:35.266 | [main] edslab.TCRepSrv (TCRepSrv.java:645) - com.edslab.TCRepSrv is in running state
		INFO | 2021-03-18 13:50:35.279 | [main] edslab.TCRepSrv (TCRepSrv.java:404) - Running consumer thread for node slave: webx having master: webp and node id 0
		INFO | 2021-03-18 13:50:35.281 | [main] edslab.TAppl (TAppl.java:554) - Running TAppl consumer webx for node webp
		INFO | 2021-03-18 13:50:35.284 | [TA-webp] edslab.TAppl (TAppl.java:211) - TA_webp_10820 is in running state
		INFO | 2021-03-18 13:50:35.285 | [main] edslab.TCRepSrv (TCRepSrv.java:404) - Running consumer thread for node slave: webx having master: weby and node id 1
		INFO | 2021-03-18 13:50:35.285 | [main] edslab.TAppl (TAppl.java:554) - Running TAppl consumer webx for node weby
		INFO | 2021-03-18 13:50:35.288 | [TA-weby] edslab.TAppl (TAppl.java:211) - TA_weby_11259 is in running state
		INFO | 2021-03-18 13:50:35.305 | [TA-weby] edslab.TAppl (TAppl.java:369) - TA_weby_11259:Managing xid 26051
		INFO | 2021-03-18 13:50:35.312 | [TA-webp] edslab.TAppl (TAppl.java:369) - TA_webp_10820:Managing xid 2409993294
		INFO | 2021-03-18 13:50:35.324 | [TA-weby] edslab.TAppl (TAppl.java:499) - TA_weby_11259:Scanned: 1
		INFO | 2021-03-18 13:50:35.349 | [TA-webp] edslab.TAppl (TAppl.java:369) - TA_webp_10820:Managing xid 2409993293

Topology for Secondary

.. code-block:: sh

		sh bin/TC_srvctl.sh --topology --node webx --detail
		Launching..
		Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/webx_rdb_bdr.conf review
		Primary Database:
		node webx
		db webx
		host hostxx-adbdb.edslab.it
		port 5433
		user postgres
		pwd grespost
		RDB database:
		rnode null
		rdb rdb_db__webx
		rhost hostxx-bdbdb.edslab.it
		rport 5433
		ruser postgres
		rpwd grespost
		Thu Mar 18 13:59:21 UTC 2021
		---------- Show ReplSrvr for node webx ----------
		Check Schema _rdb_bdr exist on db rdb_db__webx :true
		---------- Show Consumers for node webx ----------
		---------- Show Producers for node webx ----------
		Node webp is an enabled producer of webx since enable status is true for subscription 'webx_subs_webp'
		Node weby is an enabled producer of webx since enable status is true for subscription 'webx_subs_weby'

Topology for primary

.. code-block:: sh

		sh bin/TC_srvctl.sh --topology --node weby --detail
		Launching..
		Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/weby_rdb_bdr.conf review
		Primary Database:
		node weby
		db weby
		host hostxx-adbdb
		port 5434
		user statusr
		pwd statpwd
		RDB database:
		rnode null
		rdb rdb_db__weby
		rhost hostxx-bdbdb
		rport 5432
		ruser postgres
		rpwd grespost
		Thu Mar 18 14:00:32 UTC 2021
		---------- Show ReplSrvr for node weby ----------
		Check Schema _rdb_bdr exist on db rdb_db__weby :true
		Node weby is a MASTER NODE since has a replication slot 'rdb_weby_bdr' on its primary database
		Node weby have TCapture Replication Server running since active status is true for replication slot 'rdb_weby_bdr'
		---------- Show Consumers for node weby ----------
		Node webx is an active consumer of weby since active status is true for replication slot 'weby_publ_webx'
		---------- Show Producers for node weby ----------

Monitor gap

.. code-block:: sh

		rdb_db__weby=# select *, q_xid - xid_offset as gap from _rdb_bdr.tc_monit;
		db_xid_last_committed | db_last_committed_dateop | wal_lsn | q_xid | q_dateop | q_lsn | state | check_dateop | n_mstr | n_slv | flushed_lsn | xid_offset | gap
		-----------------------+----------------------------+-----------+-------+--------------------------+-----------+-------+----------------------------+--------+-------+-------------+------------+-----
		| | 0/76FC168 | 26493 | 2021-03-18 14:03:15.6436 | 0/76FC168 | t | 2021-03-18 14:03:17.785988 | weby | webx | 2B/E9610B48 | 26493 | 0
		26496 | 2021-03-18 14:03:24.232896 | 0/76FC488 | 26493 | 2021-03-18 14:03:15.6436 | 0/76FC168 | t | 2021-03-18 14:03:17.77952 | weby | weby | 0/76FC238 | |

Monitor xacts flusso

	
.. code-block:: sh

		select count(*) , xid from _rdb_bdr.walq__webp where wid >= (select max(wid)-333 from _rdb_bdr.walq__webp) group by xid;
		\watch –n1

Monitor queue xid near offset
	
.. code-block:: sh
	
		 Master weby

		rdb_db__webx=# select count(*),xid from _rdb_bdr.walq__weby where xid >= (select xid_offset from _rdb_bdr.walq__weby_offset ) and xid < (select xid_offset + 100 from _rdb_bdr.walq__weby_offset ) group by xid order by xid desc limit 40;
		count | xid
		-------+-------
		1 | 26798

.. code-block:: sh

		 Master webp

		rdb_db__webx=# select count(*),xid from _rdb_bdr.walq__webp where xid >= (select xid_offset from _rdb_bdr.walq__webp_offset ) and xid < (select xid_offset + 100 from _rdb_bdr.walq__webp_offset ) group by xid order by xid desc limit 40;

log verbose

.. code-block:: sh

		edit out/log4j2.xml

		da info ad all
		<AppenderRef ref="rollingFile" level="all"/>

		And re-cycle TCRepSrv

		1038)$ sh bin/stopTCRepSrv.sh -n weby
		Still Shutting down..
		Still Shutting down..
		Still Shutting down..
		Node weby shutdown !!
		[ ~/scripts/mycode/tcrepsrv-dev ]
		sh bin/stopTCRepSrv.sh -n weby
		Node weby not running
		[ ~/scripts/mycode/tcrepsrv-dev ]
		(1039)$ sh bin/runTCRepSrv.sh -n weby

Other examples
	

.. code-block:: sh

		(1054)$ sh bin/TC_srvctl.sh --config --type consumer --node weby --host hostxx-adbdb --port 5434 --user statusr --passwd statpwd --db weby -rhost hostxx-bdbdb --ruser postgres --rpasswd grespost -rport 5432
		Launching..
		rdbbdrconf:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/weby_bdr_rdb.conf
		[ 16:02:11 Thu Mar 18 ] [ postgres@hostxx-bdbdb.edslab.it ] [ ~/scripts/mycode/tcrepsrv-dev ]

		(1055)$ sh bin/TC_srvctl.sh --setup --node weby --type consumer --producer webx
		Launching..
		Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/weby_bdr_rdb.conf review
		Primary Database:
		node weby
		db weby
		host hostxx-adbdb
		port 5434
		user statusr
		pwd statpwd
		RDB database:
		rnode null
		rdb rdb_db__weby
		rhost hostxx-bdbdb
		rport 5432
		ruser postgres
		rpwd grespost
		Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/webx_rdb_bdr.conf review
		Primary Database:
		node webx
		db webx
		host hostxx-adbdb.edslab.it
		port 5433
		user postgres
		pwd grespost
		RDB database:
		rnode webx
		rdb rdb_db__webx
		rhost hostxx-bdbdb.edslab.it
		rport 5433
		ruser postgres
		rpwd grespost
		> Checking existance of database weby
		< weby exists!
		> Checking existance of database rdb_db__weby
		< rdb_db__weby exists!
		> Checking existance of schema _rdb_bdr in database weby
		> Checking existance tables _rdb_bdr.walq__weby% in database weby exists!
		> Checking existance tables _rdb_bdr.walq__webx% in database rdb_db__weby exists!
		> Checking existance tables _rdb_bdr.walq__weby% in database rdb_db__weby exists!
		< Schema _rdb_bdr tables walq__weby% in database rdb_db__weby exists!
		Do you wish to proceed anyway ? (Y/N): Y
		skipping creation (already exists) on schema _rdb_bdr in database rdb_db__weby
		---------------------------------------------------------------------------------------
		You are going to set consumer node weby from producer webx
		Do you wish to proceed ? (Y/N): Y
		>> Going to set node weby as consumer for producer webx
		Check Schema _rdb_bdr exist on db weby :false
		--Going to create _rdb_bdr structure on database weby ..
		--Going to create _rdb_bdr structure on database rdb_db__weby ..
		--Going to create partitioned _rdb_bdr.walq__weby_XX tables on database rdb_db__weby ..
		Coordinate consumer weby slave of webx - Current WAL LSN:15A- NOT EXIST
		Coordinate consumer weby slave of webx - Current WAL LSN:15B- NOT EXIST
		Coordinate consumer weby slave of webx - Current WAL LSN:15C- NOT EXIST
		Coordinate consumer weby slave of webx - Current WAL LSN:15D- NOT EXIST
		--Going to create subscriptionweby_subs_webx on database rdb_db__weby ..

Webx is master for weby and slave for webp and weby:
	
.. code-block:: sh

		sh bin/TC_srvctl.sh --topology --node webx --detail
		Launching..
		Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/webx_rdb_bdr.conf review
		Primary Database:
		node webx
		db webx
		host hostxx-adbdb.edslab.it
		port 5433
		user postgres
		pwd grespost
		RDB database:
		rnode null
		rdb rdb_db__webx
		rhost hostxx-bdbdb.edslab.it
		rport 5433
		ruser postgres
		rpwd grespost
		Thu Mar 18 15:24:30 UTC 2021
		---------- Show ReplSrvr for node webx ----------
		Check Schema _rdb_bdr exist on db rdb_db__webx :true
		Node webx is a MASTER NODE since has a replication slot 'rdb_webx_bdr' on its primary database
		Node webx have TCapture Replication Server running since active status is true for replication slot 'rdb_webx_bdr'
		---------- Show Consumers for node webx ----------
		Node weby is an active consumer of webx since active status is true for replication slot 'webx_publ_weby'
		---------- Show Producers for node webx ----------
		Node webp is an enabled producer of webx since enable status is true for subscription 'webx_subs_webp'
		Node weby is an enabled producer of webx since enable status is true for subscription 'webx_subs_weby'

		Node is running?

.. code-block:: sh

		sh bin/statusTCRepSrv.sh -n webx

		postgres 17831 17786 9 15:56 pts/0 00:03:12 \_ /bin/java -XX:-UsePerfData -Xms1024m -Xmx4096m -XX:ErrorFile=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/log/repserver_pid_%p.log -Djava.library.path=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/bin -Duser.timezone=UTC -Djava.awt.headless=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/disruptor-3.3.0.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-core-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-api-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/postgresql-42.2.19.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/commons-cli-1.4.jar:. com.edslab.TCRepSrv -n webx
		Listing running TCRepSrv program for all..
		postgres 6825 1 0 Mar16 ? 00:00:00 sh bin/runTCRepSrv.sh -n webp
		postgres 6871 6825 3 Mar16 ? 01:48:47 \_ /bin/java -XX:-UsePerfData -Xms1024m -Xmx4096m -XX:ErrorFile=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/log/repserver_pid_%p.log -Djava.library.path=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/bin -Duser.timezone=UTC -Djava.awt.headless=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/disruptor-3.3.0.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-core-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-api-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/postgresql-42.2.19.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/commons-cli-1.4.jar:. com.edslab.TCRepSrv -n webp
		postgres 17786 1 0 15:56 pts/0 00:00:00 sh bin/runTCRepSrv.sh -n webx
		postgres 17831 17786 9 15:56 pts/0 00:03:13 \_ /bin/java -XX:-UsePerfData -Xms1024m -Xmx4096m -XX:ErrorFile=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/log/repserver_pid_%p.log -Djava.library.path=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/bin -Duser.timezone=UTC -Djava.awt.headless=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/disruptor-3.3.0.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-core-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-api-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/postgresql-42.2.19.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/commons-cli-1.4.jar:. com.edslab.TCRepSrv -n webx
		postgres 3222 1 0 16:10 pts/0 00:00:00 sh bin/runTCRepSrv.sh -n weby
		postgres 3269 3222 1 16:10 pts/0 00:00:19 \_ /bin/java -XX:-UsePerfData -Xms1024m -Xmx4096m -XX:ErrorFile=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/log/repserver_pid_%p.log -Djava.library.path=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/bin -Duser.timezone=UTC -Djava.awt.headless=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/disruptor-3.3.0.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-core-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-api-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/postgresql-42.2.19.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/commons-cli-1.4.jar:. com.edslab.TCRepSrv -n weby

TWrapperSQL

--how execute sql script on a master node should not flow in queue

.. code-block:: sh

		sh bin/runTWrapperSQL.sh -n webx -s /tmp/script.sql
		Logging startup messages to : TWrapperSQL_webx_2021-03-18-16:38:32.log
		Launching..
		webx
		/tmp/script.sql
		walq _rdb_bdr.walq__webx
		main:Commmit curxid:12437855
		rdb_db__webx=# select * from _rdb_bdr.walq__webx_xid where xid_from_queue=-1;
		xid_from_queue | xid_current | lsn | dateop
		----------------+-------------+--------------+----------------------------
		-1 | 12437855 | 15B/A6AF1190 | 2021-03-18 15:38:32.82496

TCSrvCTL

.. code-block:: sh

		sh bin/TC_srvctl.sh --help
		Launching..
		-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
		usage: TCSrvCTL
		Configure a node. --config --node --type [producer/consumer] --host --port --user --passwd --db --rhost --ruser --rport --rpasswd
		Show node config. --showconf --node --type [producer/consumer/monitor]
		Setup a node . --setup --node --type [producer/consumer] [--producer] [--force]
		Unset a node . --unset --node --type [producer/consumer] [--producer] [--force]
		Enable a node . --enable --node --type [producer/consumer/moniotr] [--producer]
		Disable a node . --disable --node --type [producer/consumer/monitor] [--producer]
		Start a node . --start --node --type [producer/consumer/monitor] [--producer]
		Stop a node . --stop --node --type [producer/consumer/monitor] [--producer]
		Show status node. --status --node --type [producer/consumer/monitor]
		Move a marker . --marker --node --type consumer --producer [--next_xid/--set_xid=<xid number>])
		Show topology . --topology --node [--detail]
		Shutdown TCRSrv . --shutdown --node
		Print help messg. --help
		-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
		usage: Parameters explanation:
		--config Configure a node
		--db <arg> Database for replication
		--detail Show Mesh Topology more details
		--disable Disable a node at startup of TCRepSrv
		--enable Enable a node at startup of TCRepSrv
		--force Force setup a node
		--help Print this help message.
		--host <arg> Host server
		--marker Move marker to next/given xid for a consumer
		--next_xid Set marker to a next xid
		--node <arg> Node name
		--passwd <arg> User password
		--port <arg> Host port
		--producer <arg> Producer node name
		--rhost <arg> RHost server
		--rpasswd <arg> RUser password
		--rport <arg> RHost port
		--ruser <arg> RUser rdbbbdr_user
		--set_xid <arg> Set marker to a given xid
		--setup Setup a node
		--showconf Show node configuration
		--shutdown Shutdown TC Replication Server
		--start Start a thread on node running TCRepSrv
		--status Show status of TC Replication Server threads
		--stop Stop a thread on node running TCRepSrv
		--topology Show Mesh Topology
		--type <arg> Node type [producer/consumer]
		--unset Unset a node
		--user <arg> User rdbbbdr_user

Status

.. code-block:: sh

		(1076)$ sh bin/TC_srvctl.sh --status --node webx --type producer
		Launching..
		Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/webx_rdb_bdr.conf review
		Primary Database:
		node webx
		db webx
		host hostxx-adbdb.edslab.it
		port 5433
		user postgres
		pwd grespost
		RDB database:
		rnode null
		rdb rdb_db__webx
		rhost hostxx-bdbdb.edslab.it
		rport 5433
		ruser postgres
		rpwd grespost
		Reading Status from _rdb_bdr.tc_process
		----------------------------------------------------------------------------------------------
		Status node webx of type C
		id:0 should_be:up status:start pending_op:managed last_op:2021-03-1814:56:38
		----------------------------------------------------------------------------------------------
		Status node webp of type S
		id:0 should_be:up status:start pending_op:managed last_op:2021-03-1814:56:38
		----------------------------------------------------------------------------------------------
		Status node weby of type S
		id:1 should_be:up status:start pending_op:managed last_op:2021-03-1814:56:38
		----------------------------------------------------------------------------------------------
		Status node webx of type M
		id:0 should_be:up status:start pending_op:managed last_op:2021-03-1814:56:38
		----------------------------------------------------------------------------------------------
		Status node webx of type H
		id:0 should_be:up status:start pending_op:managed last_op:2021-03-1814:56:38

stop a thread

.. code-block:: sh

		sh bin/TC_srvctl.sh --stop --node webx --type producer
		Launching..
		stop node type 'M'

Move queue marker for a consumer

.. code-block:: sh

		sh bin/TC_srvctl.sh --marker --node webx --type consumer --producer webp --next_xid
		---------------------------------------------------------------------------------------
		You are going to move marker to next xid for consumer webx of producer webp
		---------------------------------------------------------------------------------------
		Data close to the marker :
		Data Substr | dateop | wid | xid
		UPDATE crm.user_accesses SET id = 15221532, user_id = 7, access_date = '2021-03-18 13:16:31.278', ti | 2021-03-18 03:50:48 388 | 148176212 | 2410159802
		<< CURRENT MARKER >> ******************************************************************************** | 2021-03-18 03:50:48 725 | 148176212 | 2410159802
		UPDATE crm.user_accesses SET id = 15221532, user_id = 7, access_date = '2021-03-18 13:16:31.278', ti | 2021-03-18 03:50:48 389 | 148176213 | 2410159803
		UPDATE crm.user_accesses SET id = 15221953, user_id = 2114, access_date = '2021-03-18 13:47:03.266', | 2021-03-18 03:50:48 390 | 148176214 | 2410159804
		UPDATE wms.wh_floating_positions SET wh_floating_position_id = 2079469, floating_position = 'R010201 | 2021-03-18 03:50:49 894 | 148176215 | 2410159806
		INSERT INTO wmsdwh.wh_floating_positions_dwh_modif (wh_floating_position_id, dwh_serial_id, dwh_oper | 2021-03-18 03:50:49 894 | 148176216 | 2410159806
		UPDATE wms.open_stock_details SET open_stock_detail_id = 61149202, master_carton_detail_id = 2393515 | 2021-03-18 03:50:49 897 | 148176217 | 2410159808
		UPDATE wms.pieces_by_location SET pieces_by_location_id = 102369866, warehouse_id = 359, catalogue_i | 2021-03-18 03:50:49 897 | 148176218 | 2410159808
		INSERT INTO wms.pieces_by_location (pieces_by_location_id, warehouse_id, catalogue_item_id, wh_posit | 2021-03-18 03:50:49 897 | 148176219 | 2410159808
		INSERT INTO wmsdwh.open_stock_details_dwh_modif (open_stock_detail_id, master_carton_detail_id, mast | 2021-03-18 03:50:49 897 | 148176220 | 2410159808
		View data next to marker (next record that will be processed) - the one you want to skip:
		Data Substr | dateop | wid | xid
		UPDATE crm.user_accesses SET id = 15221532, user_id = 7, access_date = '2021-03-18 13:16:31.278', ti | 2021-03-18 03:50:48 389 | 148176213 | 2410159803
		---------------------------------------------------------------------------------------
		You are going to set marker wid:148176213 - xid:2410159803 on consumer webx for primary webp
		Do you wish to proceed ? (Y/N):

Script to Monitor Lag between systems

.. code-block:: sh

-Sql utils
--Sequences

.. code-block:: sh

		One thing to note is that since logical replication in PostgreSQL does not provide any information about sequences, we do not replicate sequences. They will need to be handled by some other process.
		Set
		SELECT 'select '' SELECT setval(''''' || n.nspname || '.' || c.relname || ''''', ''||last_value - ( last_value % 4) + 4 || '' ); '' from '|| n.nspname|| '.'|| c.relname ||';'
		FROM pg_catalog.pg_class c
		JOIN pg_catalog.pg_roles r ON r.oid = c.relowner
		LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
		WHERE c.relkind IN ('S','') AND n.nspname <> 'pg_catalog' AND n.nspname <> 'information_schema' AND n.nspname !~ '^pg_toast'
		ORDER BY 1;
		Increment by
		SELECT 'alter sequence '||n.nspname|| '.'|| c.relname ||' increment by 4;'
		FROM pg_catalog.pg_class c
		JOIN pg_catalog.pg_roles r ON r.oid = c.relowner
		LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
		WHERE c.relkind IN ('S','') AND n.nspname <> 'pg_catalog' AND n.nspname <> 'information_schema' AND n.nspname !~ '^pg_toast'
		ORDER BY 1;
		drop FK constraints
		SELECT 'ALTER TABLE "'||nspname||'"."'||relname||'" DROP CONSTRAINT "'||conname||'";'
		FROM pg_constraint
		INNER JOIN pg_class ON conrelid=pg_class.oid
		INNER JOIN pg_namespace ON pg_namespace.oid=pg_class.relnamespace
		where contype = 'f' ORDER BY CASE WHEN contype='f' THEN 0 ELSE 1 END,contype,nspname,relname,conname;

unset a producer node

.. code-block:: sh

		sh bin/stopTCRepSrv.sh -n webp
		sh bin/TC_srvctl.sh --unset --node webp --type producer --Removing tc_process entry:Master webp.. Replication slot webp_publ_ exists !

		prima unset di eventuali slave

unset a consumer node

.. code-block:: sh

		sh bin/TC_srvctl.sh --unset --node webx --type consumer --producer webp
		You are going to unset consumer node webx from producer webp
		Do you wish to proceed ? (Y/N): Y
		>> Going to unset node webx as consumer for producer webp
		Check Schema _rdb_bdr exist on db rdb_db__webx :true
		--Removing tc_process entry:Slave webx for master webp..
		--Going to drop subscription..
		subscription webx_subs_webp for publication webp_publ: disabled
		subscription webx_subs_webp for publication webp_publ: set (slot_name=none)
		subscription webx_subs_webp for publication webp_publ: dropped
		Subscription webx_subs_webp for publication webp_publ DROPPED !
		--Going to drop rdb_bdr.walq__webp% tables on rdb_db__webx..
		Check Schema _rdb_bdr exist on db rdb_db__webx :true
		> Checking existance tables _rdb_bdr.walq__webp% in database rdb_db__webx exists!
		Dropping _rdb_bdr.walq__webp% tables on rdb_db__webx
		> Drop tables _rdb_bdr.walq__webp% in database rdb_db__webx !
		walq__webp_offset
		walq__webp_log
		walq__webp_filtro
		walq__webp_crules
		walq__webp_conflicts
		walq__webp_cmanaged
		walq__webp_4b69
		walq__webp_4b68
		walq__webp_4b67
		walq__webp_4b66
		walq__webp
		> Drop sequences _rdb_bdr.walq__webp% in database rdb_db__webx !
		--Going to drop rdb_bdr schema on rdb_db__webx and database rdb_db__webx for consumer webx ..
		Check Schema _rdb_bdr exist on db rdb_db__webx :true
		> Checking existance tables _rdb_bdr.walq__% in database rdb_db__webx exists!
		< Schema _rdb_bdr tables walq__% in database rdb_db__webx exists!
		Skipping drop of schema _rdb_bdr in database rdb_db__webx
		--Going to drop Replication Slot for removed slave subscription on master webp
		Replication slot _publ_webx
		Dropping replication slot :webp_publ_webx

		webx=# table dba.__events_ddl ;

		ddl_id | wal_lsn | wal_txid | ddl_user | ddl_object | ddl_type | ddl_command | creation_timestamp
		--------+---------------+------------+----------+---------------------+----------+-------------+-------------------------------
		47 | 4ADB/E5FAD780 | 2398623870 | statusr | webp | MASTER | CREATE NODE | 2021-03-08 11:10:48.330897+01
		48 | 150/C320BAF0 | 11211039 | postgres | webx<-weby | SLAVE | CREATE NODE | 2021-03-17 16:02:00.518704+01
		49 | 15A/F94C5088 | 12377485 | postgres | webx | MASTER | CREATE NODE | 2021-03-18 15:56:35.807368+01
		50 | 163/77E41D70 | 13570412 | postgres | webx<-webp | SLAVE | DROP NODE | 2021-03-19 15:04:08.827209+01
		-1 | 163/77E8F570 | 13571618 | statusr | NO_ACTIVITY_LSN_ACK | DML | UPSERT | 2021-03-19 15:22:24.24918+01

unset a producer node (cont..)

.. code-block:: sh

		sh bin/TC_srvctl.sh --unset --node webp --type producer --force
		You are going to unset producer node webp
		Do you wish to proceed ? (Y/N): Y
		>> Going to unset node webp as producer
		--Removing tc_process entry:Master webp..
		Replication slot webp_publ_ NOT exists OR Forced execution
		--Going to drop publication..
		Publication webp_publ for table _rdb_bdr.walq__webp exists
		Publication webp__publ: dropped
		--Going to drop rdb_bdr.walq__webp% tables on rdb_db__webp..
		Check Schema _rdb_bdr exist on db rdb_db__webp :true
		> Checking existance tables _rdb_bdr.walq__webp% in database rdb_db__webp exists!
		Dropping _rdb_bdr.walq__webp% tables on rdb_db__webp
		> Drop tables _rdb_bdr.walq__webp% in database rdb_db__webp !
		walq__webp_xid_4b69
		walq__webp_xid_4b68
		walq__webp_xid_4b67
		walq__webp_xid_4b66
		walq__webp_xid
		walq__webp_offset
		walq__webp_mon
		walq__webp_log
		walq__webp_filtro
		walq__webp_4b69
		walq__webp_4b68
		walq__webp_4b67
		walq__webp_4b66
		walq__webp
		> Drop sequences _rdb_bdr.walq__webp% in database rdb_db__webp !
		walq__webp_wid_seq
		--Going to drop rdb_bdr schema on rdb_db__webp and database rdb_db__webp for producer webp ..
		Check Schema _rdb_bdr exist on db rdb_db__webp :true
		> Checking existance tables _rdb_bdr.walq__% in database rdb_db__webp exists!
		Check Schema _rdb_bdr exist on db rdb_db__webp :true
		Schema _rdb_bdr dropped
		org.postgresql.util.PSQLException: ERROR: database "rdb_db__webp" is being accessed by other users
		Detail: There is 1 other session using the database.
		at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2553)
		at org.postgresql.core.v3.QueryExecutorImpl.processResults(QueryExecutorImpl.java:2285)
		at org.postgresql.core.v3.QueryExecutorImpl.execute(QueryExecutorImpl.java:323)
		at org.postgresql.jdbc.PgStatement.executeInternal(PgStatement.java:481)
		at org.postgresql.jdbc.PgStatement.execute(PgStatement.java:401)
		at org.postgresql.jdbc.PgStatement.executeWithFlags(PgStatement.java:322)
		at org.postgresql.jdbc.PgStatement.executeCachedSql(PgStatement.java:308)
		at org.postgresql.jdbc.PgStatement.executeWithFlags(PgStatement.java:284)
		at org.postgresql.jdbc.PgStatement.execute(PgStatement.java:279)
		at com.edslab.TCSrvCTL.dropRdbDatabase(TCSrvCTL.java:953)
		at com.edslab.TCSrvCTL.unsetNode(TCSrvCTL.java:1537)
		at com.edslab.TCSrvCTL.applyOptions(TCSrvCTL.java:2309)
		at com.edslab.TCSrvCTL.applyOptions(TCSrvCTL.java:2364)
		at com.edslab.TCSrvCTL.main(TCSrvCTL.java:2406)
		--Going to drop Replication Slot ..
		Replication slot _rdb_webp_bdr exists
		Dropping replication slot :rdb_webp_bdr

		To complete cleanup, manually execute : drop schema dba cascade; on primary database db01

		table dba.__events_ddl;

		797 | 4B67/80A38000 | 2411327431 | statusr | webp | MASTER | DROP NODE | 2021-03-19 15:06:34.203928+01

