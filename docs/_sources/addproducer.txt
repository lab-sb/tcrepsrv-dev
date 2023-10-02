.. _addproducer:

Adding a Primary node in TCapture 
=================================


Once you have TCapture installed on your system (check out the :ref:`installation` section. ), you just need to add configuration for the primary node .



Configuring a primary database node 
-----------------------------------
To set up TCapture, you must configure the primary database server - the postgreSQL database on which the Distribution Database and the primary databases will reside. This includes:
	- Enabling and configure logical decoding
	- Setting up replication system user
	- Defining a node name  for the primary TCapture database 
	- Creating the Distribution Database

Enabling and configure logical decoding: Editing the postgreSQL configuration file
----------------------------------------------------------------------------------

In order for TCapture to run, the :code:`wal_level` configuration parameter must be set in :code:`postgresql.conf`, which can be found underneath your data directory. It's also a good idea to set :code:`max_wal_senders and max_replication_slots` to something reasonably high to give TCapture worker processes plenty of capacity:

	# At the bottom of <data directory>/postgresql.conf

.. code-block:: sh


	##### logical decoding
	wal_level = logical
	max_wal_senders = 10            # max number of walsender processes
	max_replication_slots = 18
	track_commit_timestamp = on
	

Setting up replication system user
----------------------------------

You must create a postgreSQL user for TCapture Replication Server. TC uses this login to access the primary database’s transaction log(wal) and the Distribution Database.
The TCapture system user  must have superuser role. 

.. code-block:: sh

	 create user rdbbdr_user  superuser inherit login password 'rdbbdr_pwd';

Defining a node name  for the primary TCapture database
-------------------------------------------------------

The primary database must have a node name defined for itself


Creating the Distribution Database
----------------------------------
To complete the primary database configuration for TCapture you must create the Distribution Database. TC uses the
Distribution Database to maintain its stable queue and metadata objects.

.. code-block:: sh
	
		create database rdb_db__<node_name>;


Configuring the TC primary database
-----------------------------------
If a database is to be used as a producer or consumer, it must be added to a replication server using the tc_cli_xxx.sh command.
The database server running the database must have been configured to support its access by a replication server

Source the environment file on TC installation directory:

.. code-block:: sh

	. ../.rdbbdr_env.sh


Move into the $RDBBDR directory:

.. code-block:: sh

	cd $RDBBDR


Run  TCSrvCTL --config as follow from the RDBBDR/bin directory:

.. code-block:: sh
	
	TCSrvCTL --config --type producer --node --host --port --user --passwd --db --rhost --ruser  --rport --rpasswd

example:
	 define a primary node called swap on host edslab-qaspg01:5433 for primary db db01qas having rdb (replication database) on same host :

.. code-block:: sh

	$  sh TC_srvctl.sh --config  --type producer --node swap --host edslab-qaspg01 --port 5433 --user db01 --passwd webqlty --db db01qas --rhost edslab-qaspg01 --ruser db01 --rpasswd webqlty -rport 5433
	Launching..
	rdbbdrconf:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/swap_rdb_bdr.conf

where 

* node		give a name to this primary node 
* host		host where primary database run
* port		port where primary database  is listening
* user 		tcapture user (superuser)
* passwd		
* db		primary db name
* rhost		host containing replication database ( defined as rdb_db__<node>) 
* rport		port where rdb is listening
* ruser		rdb user (superuser)
* rpasswd

This command generate a configuration file  under $RDBBDR/conf/ named <node>_rdb_bdr.conf

Run  TCSrvCTL --show to read the configuration file for node :

.. code-block:: sh

       $ sh TC_srvctl.sh --show --node swap --type producer
	Launching..
	Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/swap_rdb_bdr.conf review
	Primary Database:
	node swap
	db db01qas
	host edslab-qaspg01
	port 5433
	user db01
	pwd webqlty

	RDB database:
	rnode null
	rdb rdb_db__swap
	rhost edslab-qaspg01
	rport 5433
	ruser db01
	rpwd webqlty



Run  TCSrvCTL --setup as follow from the RDBBDR/bin directory:

.. code-block:: sh

	$  sh TC_srvctl.sh --setup  --node swap --type producer
	Launching..
	Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/swap_rdb_bdr.conf review
	Primary Database:
	node swap
	db db01qas
	host edslab-qaspg01
	port 5433
	user db01
	pwd webqlty

	RDB database:
	rdb rdb_db__swap
	rhost edslab-qaspg01
	rport 5433
	ruser db01
	rpwd webqlty

	> Checking existance of database db01qas
	< db01qas exists!
	> Checking existance of database rdb_db__swap
	< rdb_db__swap exists!
	> Checking existance of schema _rdb_bdr in database db01qas
	> Checking existance tables _rdb_bdr.walq__swap% in database db01qas exists!
	> Checking existance tables _rdb_bdr.walq__swap% in database rdb_db__swap exists!
 	Creating rdb  node _rdb_bdr structure on rdb_db__swap
 	Creating rdb  node _rdb_bdr structure on db01qas


This command create :
	- the replication database structure
	- a replication slot 'rdb_<nodename>_bdr'  
	- a publication of the queue table storing primary database transactions
	

After a database has been added, a consumer can be created to receive transactions from this primary database, or the database can join an existing publication.
If you'd prefer to get right into it, check out the :ref:`addconsumer` section.




Start TCapture Replication Server 
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Run TCapture Replication Server on defined primary node :

.. code-block:: sh

 	sh runTCRepSrv.sh -n swap 

	Launching..
	1:postgres 20754 17952  0 14:43 pts/0    00:00:00  |                       \_ sh runTCRepSrv.sh -n swap
	2:postgres 20768 20754  0 14:43 pts/0    00:00:00  |                           \_ sh runTCRepSrv.sh -n swap
	3:postgres 20795 20768 40 14:43 pts/0    00:00:00  |                           |   \_ /usr/bin/java -XX:-UsePerfData -Xms512m -Xmx1836m -XX:ErrorFile=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/log/repserver_pid_%p.log -Djava.library.path=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/bin -Duser.timezone=UTC -Djava.awt.headless=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/disruptor-3.3.0.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-core-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-api-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/postgresql-42.2.19.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/commons-cli-1.4.jar:. com.edslab.TCRepSrv -n swap

	
	INFO  | 2020-01-10 13:46:28.155 | [main] edslab.TCRepSrv (TCRepSrv.java:126) - ***********************************************************************
	INFO  | 2020-01-10 13:46:28.155 | [main] edslab.TCRepSrv (TCRepSrv.java:126) - ***********************************************************************
	INFO  | 2020-01-10 13:46:28.159 | [main] edslab.TCRepSrv (TCRepSrv.java:127) - Running TCapture Replication Server for node :swap
	INFO  | 2020-01-10 13:46:28.159 | [main] edslab.TCRepSrv (TCRepSrv.java:127) - Running TCapture Replication Server for node :swap
	INFO  | 2020-01-10 13:46:28.160 | [main] edslab.TCRepSrv (TCRepSrv.java:128) - ***********************************************************************
	INFO  | 2020-01-10 13:46:28.160 | [main] edslab.TCRepSrv (TCRepSrv.java:128) - ***********************************************************************
	INFO  | 2020-01-10 13:46:28.348 | [main] edslab.TCRepSrv (TCRepSrv.java:557) - com.edslab.TCRepSrv is in running state
	INFO  | 2020-01-10 13:46:28.348 | [main] edslab.TCRepSrv (TCRepSrv.java:557) - com.edslab.TCRepSrv is in running state
	INFO  | 2020-01-10 13:46:28.385 | [main] edslab.TCRepSrv (TCRepSrv.java:435) - Running producer thread for node master: swap having  node id 0
	INFO  | 2020-01-10 13:46:28.385 | [main] edslab.TCRepSrv (TCRepSrv.java:435) - Running producer thread for node master: swap having  node id 0
	INFO  | 2020-01-10 13:46:28.398 | [main] edslab.TCapt (TCapt.java:735) - Running TCapt for node :swap
	INFO  | 2020-01-10 13:46:28.398 | [main] edslab.TCapt (TCapt.java:735) - Running TCapt for node :swap
	INFO  | 2020-01-10 13:46:28.403 | [TC-swap] edslab.TCapt (TCapt.java:164) - TC-swap_466:  is in running state
	INFO  | 2020-01-10 13:46:28.403 | [TC-swap] edslab.TCapt (TCapt.java:164) - TC-swap_466:  is in running state
	INFO  | 2020-01-10 13:46:28.406 | [main] edslab.TCRepSrv (TCRepSrv.java:493) - Running monitor thread for node master: swap having  node id 0
	INFO  | 2020-01-10 13:46:28.406 | [main] edslab.TCRepSrv (TCRepSrv.java:493) - Running monitor thread for node master: swap having  node id 0
	INFO  | 2020-01-10 13:46:28.414 | [main] edslab.TMoni (TMoni.java:547) - Running TMoni for node :swap
	INFO  | 2020-01-10 13:46:28.414 | [main] edslab.TMoni (TMoni.java:547) - Running TMoni for node :swap
	INFO  | 2020-01-10 13:46:28.419 | [TM-swap] edslab.TMoni (TMoni.java:210) - TM-swap_99646  is in running state
	INFO  | 2020-01-10 13:46:28.419 | [TM-swap] edslab.TMoni (TMoni.java:210) - TM-swap_99646  is in running state
	INFO  | 2020-01-10 13:46:28.444 | [TM-swap] edslab.TMoni (TMoni.java:328) - TM-swap_99646:Monitor producer swap
	INFO  | 2020-01-10 13:46:28.444 | [TM-swap] edslab.TMoni (TMoni.java:328) - TM-swap_99646:Monitor producer swap
	INFO  | 2020-01-10 13:46:28.564 | [TC-swap] edslab.TCapt (TCapt.java:521) - TC-swap_466:Scanned:0
	INFO  | 2020-01-10 13:46:28.564 | [TC-swap] edslab.TCapt (TCapt.java:521) - TC-swap_466:Scanned:0
	INFO  | 2020-01-10 13:46:29.422 | [main] edslab.TCRepSrv (TCRepSrv.java:576) - main


Stop  TCapture Replication Server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Stop TCapture Replication Server on defined primary node :


.. code-block:: sh

	sh TC_srvctl.sh --shutdown -node swap
	1$  sh TC_srvctl.sh --shutdown -node swap
	Launching..
	Shutting down all threads in Replication Server
	Shutting down Replication Server for node swap Fri Jan 10 13:46:06 UTC 2020
	Shutting down  Fri Jan 10 13:46:06 UTC 2020
	Shutting down  Fri Jan 10 13:46:06 UTC 2020
	Shutting down  Fri Jan 10 13:46:06 UTC 2020
	Shutting down  Fri Jan 10 13:46:06 UTC 2020
	Shutdown !!

or using wrapper scripts, which kill TC Replication Server process if graceful shutdown exceed time limit


.. code-block:: sh


	 sh stopTCRepSrv.sh -n prod1
	Still Shutting down..
	Still Shutting down..
	Graceful shutdown exceed time limit, going to kill TC Replication Server process
	TC Replication Server process killed !!

