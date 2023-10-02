# TCapture Replication Server 1.0 Beta

TCapture Replication Server  - Postgresql Multi-Master Database Replication Server

DESCRIPTION:
------------

TCapture is a bidirectional multi master replication server based on a 'capture and apply' asynchronous replica engine

The actual TCapture engine is a Java application which runs as a separate program outside Postgres, and which must be started explicitly.
When TCapture is running, it will scan the transaction log of all primary databases and pick up transactions which must be replicated.
Transactions which have been picked up are stored in the 'store database' , a Postgresql user database exclusively used by TCapture.
In the store database, transaction is 'copied' to all store databases which have a subscription for this transaction.
Transaction is then applied to the replicate databases 

One of the attractions of TCapture is that it’s quite easy to set up and configure: starting from scratch, you can deploy a working replication system in less than 30 minutes. 
The setup procedure is described in the TCapture Guide. As always with replication, make sure you have a clear idea of the replication logic you want to implement before you start.

KEY FEATURE:
------------

- Transactional. SQL are captured transactionally, can be coupled with surrounding business logic.
- Efficient. It capture transactions for replication from Write-Ahead Logs (WAL) instead of using triggers, eliminating overhead on master databases and significantly reduces latency
- Flexible. No limits on the number of producers or consumers, but complexity increases 
- Reliable. Transactions are stored in PostgreSQL database – this adds the benefit of write ahead logging and crash recovery.
- Transparent. No impact on the applications already running on that database, since both engine and 'store database' can run separate from production databases.
- Easy to use. Simple to set up and configure, is an  effective tool for data replication between different Postgres versions
- Open Source. No licensing fees, but occasionally you'll have to get your hands dirty


COPYRIGHT:
----------
	Copyright (c) 2022-2023  Silvio Brandani <mktg.tcapture@gmail.com>. All rights reserved.


REQUIREMENTS:
-------------
        Platforms: CentOS, 64-bit - Red Hat Enterprise Linux (RHEL), 64-bit
        Database: The database versions that can be managed by TCapture Replication Server as a producer or consumer databases are the following:  PostgreSQL versions 9.4 to latest
        Software: Java Runtime Environment (JRE) version 1.8. Any Java product such as Oracle® Java or OpenJDK may be used.



QUICK START:
-------------

To install this module type the following, :

        1 - git clone https://github.com/lab-sb/tcrepsrv-dev.git

        2 - root:
					
			2a - soruce environment file .rdbbdr_env.sh (set the variable RDBBDR HOME  to be TCapture software folder )
		    2b - install TCapture logical decoding library under /usr/pgsql-<ver>/lib (ie cp $RDBBDR/lib/10/rdblogdec.so /usr/pgsql-10/lib/

		3 - Preparation of master database, set  postgresql.conf :
			max_replication_slots = 10
			max_wal_senders = 10
			track_commit_timestamp = on # 9.5+ only
			wal_level= logical
        
		4 - Configure producer/consumer ( let's suppose we keep the database store and TCapture engine in the same installation as the database we want to replicate)
		 
			sh bin/TC_srvctl.sh --config --type producer --node e4s  --host e4s-srv --port 5432 --user prod_user --passwd prod_pwd -db e4s --rhost e4s-srv  --ruser prod_user --rport 5432 --rpasswd prod_pwd
		    sh bin/TC_srvctl.sh --config --type consumer --node e4p  --host e4p-srv --port 5432 --user prod_user --passwd prod_pwd -db e4p --rhost e4p-srv  --ruser prod_user --rport 5433 --rpasswd prod_pwd

		5 - Setup TCapture infrastructure
			
		    sh bin/TC_srvctl.sh --setup  --node e4s --type producer
			sh bin/TC_srvctl.sh --setup  --node e4p --type consumer --producer e4s

		6 - Run TCapture engines
		
  		    sh bin/runTCRepSrv.sh -n e4p
		    sh bin/runTCRepSrv.sh -n e4s
	
		7 - Try it !


DOCUMENTATION:
--------------

See docs, html documentation

GETTING HELP:
-------------

For general questions and troubleshooting, please use mktg.tcapture@gmail.com

