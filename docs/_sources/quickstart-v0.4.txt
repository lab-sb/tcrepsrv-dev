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

	milhost:sh tc_cli_add_primary_node.sh  -h localhost -u rdbbdr_user -p 5432 -pwd rdbbdr_pwd -db mil_db -n mila -rh localhost -ru rdbbdr_user -rport 5432  -rp rdbbdr_pwd -vrdb rdb_d

this will create the 
a)_rdb_bdr schema structure on master mila 
b) replication slot rdb_mila_bdr on mil_db 
c) parameter file for node mila under $RDBBDR/conf/mila_rdb_bdr.conf
d) mila_publ  

- do the same on newyork host

.. code-block:: sh 

	nychost:sh tc_cli_add_primary_node.sh  -h localhost -u rdbbdr_user -p 5432 -pwd rdbbdr_pwd -db nyc_db -n nyci -rh localhost -ru rdbbdr_user -rport 5432  -rp rdbbdr_pwd -vrdb rdb_db

-  add a remote primary node conf 

.. code-block:: sh 

	nychost: sh tc_cli_add_remote_node_conf.sh  -h vmildb-glf01  -u rdbbdr_user -p 5432 -pwd rdbbdr_pwd -db mil_db -n mila  -rh vmildb-glf01 -ru rdbbdr_user -rport 5432  -rp rdbbdr_pwd -vrdb rdb_db

this will populate the parameter file for remote node mila 

do the same in milan for remote primary nyci

.. code-block:: sh 

	milhost: sh tc_cli_add_remote_node_conf.sh  -h vnycdb-glf01  -u rdbbdr_user -p 5432 -pwd rdbbdr_pwd -db nyc_db -n nyci  -rh vnycdb-glf01 -ru rdbbdr_user -rport 5432  -rp rdbbdr_pwd -vrdb rdb_db

- we can now join the remote node (ie became a slave for the specified myci)

.. code-block:: sh 

	milhost:sh tc_cli_join_a_remote_node.sh  -h localhost -u rdbbdr_user -p 5432 -pwd rdbbdr_pwd -db rdb_db__mila -n mila -primary nyci 

- and can now join the remote node (ie became a slave for the specified mila)

.. code-block:: sh 

	nychost: sh tc_cli_join_a_remote_node.sh  -h localhost -u rdbbdr_user -p 5432 -pwd rdbbdr_pwd -db rdb_db__nyci -n nyci -primary mila

this will create the 
a) mila_subs_nyci and mila_publ_nyci slot for nyci subscription 
a) nyci_subs_mila and nyci_publ_mila slot for mila subscription 


we are ready to run T-Capture program on each master:

.. code-block:: sh 

	export NODEMST=nyci
	sh run-ReplicationServer.sh 

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

	 sh stop_ReplicationServer.sh
	root     38474 38472  4 Jun14 ?        06:00:30 /usr/java/jdk1.8.0_201-amd64/bin/java -cp ../lib/postgresql-42.2.19.jar:../pgjdbc/pgjdbc/src/:. com.edslab.TCapture


we are ready to run T-Apply program (RunWal will be renamed soon  ) on each master:

.. code-block:: sh 


	sh run-RunWal.sh

running for node: mila  node slave:  nyci

Logging startup messages to : RunWal_mila_nyci_2019-06-20-09:05:47.log
Launching..
root     40228 40226 32 09:05 pts/1    00:00:00 /usr/java/jdk1.8.0_201-amd64/bin/java -cp ../lib/postgresql-42.2.19.jar:../pgjdbc/pgjdbc/src/:. com.edslab.RunWal

.. code-block:: sh 

	sh stop_RunWal.sh
	postgres 62992 62990  0 Jun17 ?        00:07:01 /usr/java/jdk1.8.0_201-amd64/bin/java -cp ../lib/postgresql-42.2.19.jar:../pgjdbc/pgjdbc/src/:. com.edslab.RunWal
 

- test the replication

.. code-block:: sh 

	run on master 
  	sh prim_trans_gap_vcd.sh 10

2019-06-20-15:11:05:189496582  - Starting
>2019-06-20-15:11:05:355263046  - Checking
2019-06-20-15:11:05:882263273  - Find Gap on Txid   10104492 ..
2019-06-20-15:11:06:408876821  - Find Gap on Txid   10104492 ..
2019-06-20-15:11:06:936477024  - Find Gap on Txid   10104492 ..
<2019-06-20-15:11:07:463094627  - Gap filled on Txid  10104492 ..
2019-06-20-15:11:07:475373471  - Finished

check the master node mila log  log  tail -99f  log/mila_rdb_bdr.log
check the slave node nyci log  log  tail -99f  log/mila_rdb_bdr.log

check the master node mila log  log  tail -99f  log/mila_rdb_bdr.log

