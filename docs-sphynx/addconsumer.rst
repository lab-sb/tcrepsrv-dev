.. _addconsumer:

Adding a Replicated node in TCapture
====================================

Once you have TCapture primary node configured (check out the :ref:`addproducer` section. ) , you need to add configuration for the consumer node .

Configuring a consumer database node
-----------------------------------
To set up a TCapture consumer, you must configure the consumer database server - the postgreSQL database on which the Distribution Database and the consumer databases will reside. This includes:
        - Setting up replication system user
        - Defining a node name  for the consumer TCapture database
        - Creating the Distribution Database

Setting up replication system user
----------------------------------

You must create a postgreSQL user for TCapture Replication Server. TC uses this login to access the primary database’s transaction log(wal) and the Distribution Database.
The TCapture system user  must have superuser role.

.. code-block:: sh

         create user rdbbdr_user  superuser inherit login password 'rdbbdr_pwd';

Defining a node name  for the consumer TCapture database
-------------------------------------------------------

The consumer database must have a node name defined for itself


Creating the Distribution Database
----------------------------------
To complete the consumer database configuration for TCapture you must create the Distribution Database. TC uses the
Distribution Database to maintain its stable queue and metadata objects.

.. code-block:: sh

                create database rdb_db__<node_name>;


Configuring the TC consumer database
-----------------------------------
If a database is to be used as a producer or consumer, it must be added to a replication server using the TCSrvCTL command.
The database server running the database must have been configured to support its access by a replication server

Source the environment file on TC installation directory:

.. code-block:: sh

        . ../.rdbbdr_env.sh


Move into the $RDBBDR directory:

.. code-block:: sh

        cd $RDBBDR


Run  TCSrvCTL --config as follow from the RDBBDR/bin directory:

.. code-block:: sh

        TCSrvCTL --config --type consumer --node --host --port --user --passwd --db --rhost --ruser  --rport --rpasswd

example:
         define a consumer node called qas on host edslab-qaspg01:5432 for consumer db webprdqas having rdb (replication database) on same host :

.. code-block:: sh
	
	$  sh TC_srvctl.sh --config --type consumer --node  qas --host edslab-qaspg01 --port 5432 --user webprd --passwd webqlty --db webprdqas --rhost edslab-qaspg01 --ruser webprd --rpasswd webqlty -rport 5432
	Launching..
	rdbbdrconf:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/qas_bdr_rdb.conf


where

* node          give a name to this consumer node
* host          host where consumer database run
* port          port where consumer database  is listening
* user          tcapture user (superuser)
* passwd
* db            consumer db name
* rhost         host containing replication database ( defined as rdb_db__<node>)
* rport         port where rdb is listening
* ruser         rdb user (superuser)
* rpasswd

This command generate a configuration file  under $RDBBDR/conf/ named <node>_bdr_rdb.conf

configuratin file as producer must be generated as well to be able to run TCapture Replication Server
""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

.. code-block:: sh

        $  sh TC_srvctl.sh --config --type producer --node  qas --host edslab-qaspg01 --port 5432 --user webprd --passwd webqlty --db webprdqas --rhost edslab-qaspg01 --ruser webprd --rpasswd webqlty -rport 5432
        Launching..
        rdbbdrconf:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/qas_rdb_bdr.conf
	

Run  TCSrvCTL --show to read the configuration file for node :

.. code-block:: sh

	$  sh TC_srvctl.sh --config --type producer --node  qas --host edslab-qaspg01 --port 5432 --user webprd --passwd webqlty --db webprdqas --rhost edslab-qaspg01 --ruser webprd --rpasswd webqlty -rport 5432
	Launching..
	rdbbdrconf:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/qas_rdb_bdr.conf

 	$  sh TC_srvctl.sh --show --node qas --type consumer
	Launching..
	Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/qas_bdr_rdb.conf review
	Primary Database:
	node qas
	db webprdqas
	host edslab-qaspg01
	port 5432
	user webprd
	pwd webqlty

	RDB database:
	rdb rdb_db__qas
	rhost edslab-qaspg01
	rport 5432
	ruser webprd
	rpwd webqlty



Run  TCSrvCTL --setup as follow from the RDBBDR/bin directory:

.. code-block:: sh

	 $  sh TC_srvctl.sh --setup  --node qas --type consumer --producer swap
	Launching..
	Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/qas_bdr_rdb.conf review
	Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/swap_rdb_bdr.conf review

	> Checking existance of database webprdqas
	< webprdqas exists!
	> Checking existance of database rdb_db__qas
	< rdb_db__qas exists!
	> Checking existance of schema _rdb_bdr in database webprdqas
	> Checking existance tables _rdb_bdr.walq__qas% in database webprdqas exists!
	> Checking existance tables _rdb_bdr.walq__swap% in database rdb_db__qas exists!
	> Checking existance tables _rdb_bdr.walq__qas% in database rdb_db__qas exists!
 	Creating rdb  node _rdb_bdr structure on webprdqas


This command create :
        - the replication database structure
        - a replication slot 'rdb_<nodename>_bdr'
        - a subscriton a publication of the queue table storing primary database transactions
	  ex:
.. code-block:: sh

	rdb_db__qas=# \dRs+
                                                                List of subscriptions
     	Name      | Owner  | Enabled | Publication | Synchronous commit |                                   Conninfo
	---------------+--------+---------+-------------+--------------------+-------------------------------------------------------------------------------
 	qas_subs_swap | webprd | t       | {swap_publ} | off                | host=edslab-qaspg01 port=5433 user=webprd password=webqlty dbname=rdb_db__swap
	(1 row)



After a database has been added, a consumer can be created to receive transactions from this primary database, or the database can join an existing publication.
If you'd prefer to get right into it, check out the :ref:`addconsumer` section.



Start TCapture Replication Server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Run TCapture Replication Server on defined primary node :

.. code-block:: sh

	sh runTCRepSrv.sh -n qas
	Logging exception  messages to : TCapture_qas_2020-01-10-15:59:47_err.log
	Launching..
	1:postgres 22336 17952  0 15:59 pts/0    00:00:00  |                       \_ sh runTCRepSrv.sh -n qas
	2:postgres 22350 22336  0 15:59 pts/0    00:00:00  |                           \_ sh runTCRepSrv.sh -n qas
	3:postgres 22378 22350 78 15:59 pts/0    00:00:01  |                           |   \_ /usr/bin/java -XX:-UsePerfData -Xms512m -Xmx1836m -XX:ErrorFile=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/log/repserver_pid_%p.log -Djava.library.path=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/bin -Duser.timezone=UTC -Djava.awt.headless=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/disruptor-3.3.0.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-core-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-api-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/postgresql-42.2.19.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/commons-cli-1.4.jar:. com.edslab.TCRepSrv -n qas
	press any button to tail the log :
	
	INFO  | 2020-01-10 14:59:48.238 | [main] edslab.TCRepSrv (TCRepSrv.java:126) - ***********************************************************************
	INFO  | 2020-01-10 14:59:48.242 | [main] edslab.TCRepSrv (TCRepSrv.java:127) - Running TCapture Replication Server for node :qas
	INFO  | 2020-01-10 14:59:48.242 | [main] edslab.TCRepSrv (TCRepSrv.java:127) - Running TCapture Replication Server for node :qas
	INFO  | 2020-01-10 14:59:48.243 | [main] edslab.TCRepSrv (TCRepSrv.java:128) - ***********************************************************************
	INFO  | 2020-01-10 14:59:48.243 | [main] edslab.TCRepSrv (TCRepSrv.java:128) - ***********************************************************************
	INFO  | 2020-01-10 14:59:48.424 | [main] edslab.TCRepSrv (TCRepSrv.java:557) - com.edslab.TCRepSrv is in running state
	INFO  | 2020-01-10 14:59:48.424 | [main] edslab.TCRepSrv (TCRepSrv.java:557) - com.edslab.TCRepSrv is in running state
	INFO  | 2020-01-10 14:59:48.460 | [main] edslab.TCRepSrv (TCRepSrv.java:373) - Running consumer thread for node slave: qas having  master: swap and  node id 0
	INFO  | 2020-01-10 14:59:48.460 | [main] edslab.TCRepSrv (TCRepSrv.java:373) - Running consumer thread for node slave: qas having  master: swap and  node id 0
	INFO  | 2020-01-10 14:59:48.468 | [main] edslab.TAppl (TAppl.java:483) - Running TAppl consumer qas for node swap
	INFO  | 2020-01-10 14:59:48.468 | [main] edslab.TAppl (TAppl.java:483) - Running TAppl consumer qas for node swap
	



Stop  TCapture Replication Server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Stop TCapture Replication Server on defined primary node :


.. code-block:: sh
	
	$   sh TC_srvctl.sh --shutdown -node qas
	Launching..

	Configuration file: /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/conf/qas_rdb_bdr.conf review

	Shutting down all threads in Replication Server
	Shutting down Replication Server for node qas Fri Jan 10 15:06:56 UTC 2020
	Shutting down  Fri Jan 10 15:06:56 UTC 2020
	Shutting down  Fri Jan 10 15:06:56 UTC 2020
	Shutting down  Fri Jan 10 15:06:56 UTC 2020
	Shutdown !!








