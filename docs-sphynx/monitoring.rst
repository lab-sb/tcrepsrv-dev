.. _monitoring:

TCapture Monitoring
===========================
This section describes how to monitor TCapture Replication Server status processes .

Check status Tcapture  processes  
-----------------------------------------

Internal processes are of type:

	- M	master process
	- S	slave process
	- H	monitor process


.. code-block:: sh


	  n_id  | n_name | n_shouldbe | n_state			 | n_operation 			| n_type  | n_mstr 	|          n_dateop          |         n_datecrea         | n_pid
   	   <0-5> <node>  <up/down>    <start/stop/shutdown> 	   <managed/start/stop/shutdown>  <M/S/H>   <masternode>	<date last operation>   <date creation>		    <internal pid>	 	



you can start/stop TCapture internal threads with TC_srvctl.sh  --start/stop command

.. code-block:: sh



	 sh TC_srvctl.sh  --stop  --node swap --type producer



in the log:

.. code-block:: sh

	INFO  | 2020-01-10 16:00:48.649 | [main] edslab.TCRepSrv (TCRepSrv.java:270) - TCapt Thread name:TC-swap_898 is going to be stopped!
	INFO  | 2020-01-10 16:00:48.649 | [main] edslab.TCRepSrv (TCRepSrv.java:270) - TCapt Thread name:TC-swap_898 is going to be stopped!

.. code-block:: sh



         sh TC_srvctl.sh  --start  --node swap --type producer



in the log:

.. code-block:: sh

	INFO  | 2020-01-10 16:02:42.799 | [main] edslab.TCapt (TCapt.java:735) - Running TCapt for node :swap
	INFO  | 2020-01-10 16:02:42.801 | [TC-swap] edslab.TCapt (TCapt.java:164) - TC-swap_278:  is in running state




you can check status of TCapture internal threads with TC_srvctl.sh  --status command

.. code-block:: sh


	sh TC_srvctl.sh  --status --node qas --type consumer
	Launching..
	Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/qas_bdr_rdb.conf review

	Reading Status from _rdb_bdr.tc_process
	----------------------------------------------------------------------------------------------
	Status node  swap of type S
	id:0 should_be:up status:start pending_op:managed last_op:2020-01-1015:08:00

	sh TC_srvctl.sh  --status --node swap --type producer
	Launching..
	Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/swap_rdb_bdr.conf review

	Reading Status from _rdb_bdr.tc_process
	----------------------------------------------------------------------------------------------
	Status node  swap of type M
	id:0 should_be:up status:start pending_op:managed last_op:2020-01-1015:08:22
	----------------------------------------------------------------------------------------------
	Status node  swap of type H
	id:0 should_be:up status:start pending_op:managed last_op:2020-01-1015:08:22

or you can investigate the _rdb_bdr.tc_process table under rdb_db__node database:

.. code-block:: sh


	rdb_db__swap=# select * from _rdb_bdr.tc_process ;
 	n_id | n_name | n_shouldbe | n_state | n_operation | n_type | n_mstr |          n_dateop          |         n_datecrea         | n_pid
	------+--------+------------+---------+-------------+--------+--------+----------------------------+----------------------------+-------
    	0 | swap   | up         | start   | managed     | M      | swap   | 2020-01-10 15:08:22.409342 | 2020-01-10 13:28:15.622296 |    96
    	0 | swap   | up         | start   | managed     | H      | swap   | 2020-01-10 15:08:22.423165 | 2020-01-10 13:28:15.623324 | 99304
	(2 rows)



Check the monitoring table 
--------------------------

If monitoring process is active  the monitoring table is updated with:
	- state 	 the state of the queue
	- q_xid		 the last xid in queue
	- q_dateop	 the last xid timestamp 
   	- n_mstr	 master node the queue belongs to
	- n_slv		 slave node the queue is replicated to
	- xid_offset   	 the last xid managed by the slave node		
	- db_xid_last_committed 	last xid committed in master system
 	- db_last_committed_dateop	last xid timestamp in master system
	- wal_lsn	 lsn in the master system
	- q_lsn		 lsn in the maser queue
	- flushed_lsn	 lsn flushed in the master/slave system

.. code-block:: sh


	rdb_db__swap=# select * from _rdb_bdr.tc_monit;
	 db_xid_last_committed |  db_last_committed_dateop  |    wal_lsn    |   q_xid    |          q_dateop          |    q_lsn     | state |        check_dateop        | n_mstr | n_slv |  flushed_lsn  | xid_offset
	-----------------------+----------------------------+---------------+------------+----------------------------+--------------+-------+----------------------------+--------+-------+---------------+------------
            2015417146 | 2020-01-10 16:08:13.529417 | 33B8/104A3188 | 2015408553 | 2020-01-10 15:18:44.216581 | 33B8/F0E0850 | f     | 2020-01-10 16:08:14.227641 | swap   | swap  | 33B8/104A2670 |
                       |                            |               | 2015408553 | 2020-01-10 15:18:44.216581 |              | t     | 2020-01-10 16:08:14.238013 | swap   | qas   | 33B8/104A3188 | 2015408553
	(2 rows)

