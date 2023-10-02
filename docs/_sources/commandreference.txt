.. _commandreference:


TCapture SrvCtl Command reference
=================================
This section describes how to use TCapture SrvCtl command.


sh runTCRepSrv.sh -n prod1
sh statusTCRepSrv.sh -n prod1
sh stopTCRepSrv.sh -n prod1


TC_srvctl.sh --help
-----------------------------------------

.. code-block:: sh


	$ sh TC_srvctl.sh --help
			Launching..
			-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
			usage: TCSrvCT
			Configure a node.               --config        --node --type [producer/consumer] --host --port --user --passwd --db --rhost --ruser  --rport --rpasswd
			Show node config.               --showconf      --node --type [producer/consumer/monitor]
			Setup a node    .               --setup         --node --type [producer/consumer]
			Unset a node    .               --unset         --node --type [producer/consumer]
			Enable a node   .               --enable        --node --type [producer/consumer/moniotr]
			Disable a node  .               --disable       --node --type [producer/consumer/monitor]
			Start a node    .               --start         --node --type [producer/consumer/monitor]
			Stop  a node    .               --stop          --node --type [producer/consumer/monitor]
			Shutdown TC Rep Srv             --shutdown      --node --type [producer]
			Print help messg.               --help
			-----------------------------------------------------------------------------------------------------------------------------------------------------------------------
			usage: Parameters explanation:
			    --config           Configure a node
			    --db <arg>         Database for replication
			    --disable          Disable a node at startup of TCRepSrv
			    --enable           Enable a node at startup of TCRepSrv
			    --force            Force setup a node
			    --help             Print this help message.
			    --host <arg>       Host server
			    --node <arg>       Node name
			    --passwd <arg>     User password
			    --port <arg>       Host port
			    --producer <arg>   Producer node name
			    --rhost <arg>      RHost server
			    --rpasswd <arg>    RUser password
			    --rport <arg>      RHost port
			    --ruser <arg>      RUser rdbbbdr_user
			    --setup            Setup a node
			    --showconf         Show node configuration
			    --shutdown         Shutdown TC Replication Server
			    --start            Start a thread on  node running TCRepSrv
			    --status           Show status of TC Replication Server threads
			    --stop             Stop a thread on  node running TCRepSrv
			    --type <arg>       Node type [producer/consumer]
			    --unset            Unset a node
			    --user <arg>       User rdbbbdr_user


TCapture Replication Server wrapper scripts
-------------------------------------------
This section describes how to use TCapture Replication Server wrapper scripts to start/stop/status of main TCapture Replication Server program.

.. code-block:: sh


		sh runTCRepSrv.sh -n prod1
			Launching..
			1:postgres 29522  5373  0 14:43 pts/2    00:00:00  |                   \_ sh runTCRepSrv.sh -n prod1
			2:postgres 29536 29522  0 14:43 pts/2    00:00:00  |                       \_ sh runTCRepSrv.sh -n prod1
			3:postgres 29560 29536  0 14:43 pts/2    00:00:01  |                       |   \_ /bin/java -XX:-UsePerfData -Xms512m -Xmx1836m -XX:ErrorFile=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/log/repserver_pid_%p.log -Djava.library.path=/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/bin -Duser.timezone=UTC -Djava.awt.headless=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -cp /var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/disruptor-3.3.0.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-core-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/log4j-api-2.2.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/postgresql-42.2.19.jar:/var/lib/pgsql/scripts/mycode/tcrepsrv-dev/lib/commons-cli-1.4.jar:. com.edslab.TCRepSrv -n prod1

		sh statusTCRepSrv.sh -n prod1
		
		sh stopTCRepSrv.sh -n prod1



TCapture TWrapperSQL Command reference
--------------------------------------
This section describes how to use TCapture TWrapperSQL command.


.. code-block:: sh

	vi /tmp/sql-script.sql
		alter table public.prova add ad varchar(20);

	$ sh runTWrapperSQL.sh  -n prod1 -s /tmp/sql-script.sql
	Logging startup messages to : TWrapperSQL_prod1_2020-01-13-15:09:59.log
	Launching..
	prod1
	/tmp/sql-script.sql

	walq _rdb_bdr.walq__prod1
	main:Commmit curxid:9773862


a line for transaction 9773862 generated running the TWrapperSQL:
	- is added in table _rdb_bdr.walq__prod1_xid so will not be execute again avoiding recursive execution
	- is not added in table _rdb_bdr.walq__prod1 of replicate transactions, so the same sql must be execute on each node locally


.. code-block:: sh

	prd_db1=# select * from _rdb_bdr.walq__prod1_xid ;
	 xid_from_queue | xid_current |           dateop
	----------------+-------------+----------------------------
                     -1 |     9773862 | 2020-01-13 14:09:59.821165



	the TC Replication log shows:

.. code-block:: sh

	TRACE | 2020-01-13 14:31:30.489 | [TC-prod1] edslab.TCapt (TCapt.java:563) - TC-prod1_935:<Begin Txid> :9773862

	TRACE | 2020-01-13 14:31:30.489 | [TC-prod1] edslab.TCapt (TCapt.java:573) - TC-prod1_935:<< Debug >>  line:9773862#INSERT INTO _rdb_bdr.walq__prod1_ddl (ddl_id, wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp) VALUES (10, '6/A6056000', 9773862, 'prod_user', 'public.prova', 'DDL', 'ALTER TABLE', '2020-01-13 14:09:59.821165+00'); Txid: 9773862


	TRACE | 2020-01-13 14:31:30.489 | [TC-prod1] edslab.TCapt (TCapt.java:580) - TC-prod1_935: Imposto BoolTxid per lo **skip** alla prossima line con txid uguale :9773862 txid: 9773862


TCapture TC_SkipXid Command reference
-------------------------------------
This section describes how to use TCapture TC_SkipXid command.


.. code-block:: sh

	sh skip_xid_runwal.sh prod1 prod2 10739429

