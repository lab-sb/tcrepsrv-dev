.. _namingconvention:

TCapture Naming Convention
==========================

*	**CDC** ``Change Data Capture``
*	**Source Node** called also ``primary/master/producer``
* 	**Target Node** called also ``slave/secondary/consumer``


*	**Replication Slots**  types: ``rdb_<node>_bdr``  ``<master>_publ_<slave>``
					
	*	name ``rdb_<node>_bdr``	   	logical slot with plugin ``rdblogdec`` on master db
	*	name ``<master>_publ_<slave>``  logical slot with plugin pgoutput  on ``rdb_db__<master>`` db  is the master node publication ``<master>_publ``  having a slave subscription ``<slave>_subs_<master>`` 
			

* 	**Publication**
	*	pubname: ``<master>_publ`` on `rdb_db_<master>``

* 	**Subscription**
		*	subname: ``<slave>_subs_<master>``
		*	subconninfo: primary node connection informations
		*	subpublications: <node master>_publ
		*	subdb: rdb_db__<node slave>

	

*	**MultiMaster**
	``MultiMaster 2 nodes``  
	``MultiMaster 3 nodes``
	``MultiMaster 4 nodes``

*	**Queue** table ``walq_<node>`` structure

	*	``wid``		incremental sequence
	*	``lsn`` 	Logical Sequence Numner
	*	``xid`` 	transaction as is read from replication slot
	*	``data``	text column containing DML
	*	``dateop``	timestamp	
	*	``current_xid`` transaction xid of insert into queue of dml 




*	**Incoming/Replicated**   queues 
*	**Outcoming/Replication** queues



*	**TCapt**  Module to capture change on master database
*	**TAppl**  Module to apply change on slave database
*	**TMoni**  Module to monitor replication flow

*	**TCRepSrv** 		TCapture Main Replication Server Program
*	**TC_srvctl.sh**	TCapture CLI 
*	**TWrapperSQL**		Wrapper program to execute SQL/DML locally not replicated 
*	**TSkipXid**		Skip transactions program to increase/advance replication marker on Slave 

*	**H/M/S** rispectively Monitor/Master/Slave, type of nodes 

*	**RDB** db ``rdb_db__<node>`` Replication Distribution Database 
*	schema:	_rdb_bdr 
*	conf files: <node>_rdb_bdr.conf  node master conf 
	    <node>_bdr_rdb.conf  node slave conf
*	tables: _rdb_bdr.
		walq_<node> queue storing replicate transactions
		walq_<node>_offset marker of last transaction consumed by slave 
		walq_<node>_xid	list of transactions ids incoming from replicate queues * to avoid recursive transactions (avoid cross CDC between 2 nodes  with infinitive loop problem)
				xid coming from replicated queues must not be trace and insert in replication queues 




*	software folder structure:
	*	path $RDBBDR
	*	conf
	*	sql
	*	src
	*	bin
	*	run
	*	tmp
	*	ecc..
		*


* features:

	*	CDC stored in queues 
	*	transaction level granularity and commit order 
	*	multimaster replication in architecture of type mesh
	*	automatic truncation of queues 
	*	table replication filtering 
	*	multimaster 2/3/4 nodes
	*	conflict detection (not prevention)
	*	history of replicated transaction stored in log tables
	*	java configuration script 
	*	monitoring : status of master/slaves/monitor threads 
	*	monitoring : status of replications,positions last updated queues 
	*	Wrapper program to execute sql locally * not captured transaction/not in replication flow 
	*	multithread TCapture Replication Server  
	*	thread TCapt TAppl TMoni able to be started/stopped/disabled/enabled with Repl Serv in execution
	*	tracing of  DDL execution   _rdb_bdr.walq__<node>_ddl    using event trigger
	* 	helatcheck with upserts  to move database/healthcheck  
	*	loglevel granularity
		 
	
