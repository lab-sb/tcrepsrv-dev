.. _introduction:


Introduction
=============

If you'd prefer to get right into it, check out the :ref:`quickstart` section.

Overview
-----------

TCapture is a bidirectional multi master replication server based on a “capture and apply” asynchronous replica engine. It is based on PostgreSQL open-source Object-Relational RDBMS.

“TCapture” is a new software for implementing data replication of Postgresql databases. T-Capture  was introduced for PG v>9.4 , released in beta version on March 2022 .
 TCapture  is available currently on Linux . T-Capture  supports PG-to-PG, log-based  replication of DML .
 The big advantage of T-Capture  is that  is based on PostgreSQL’s logical decoding. It capture transactions for replication from Write-Ahead Logs (WAL) instead of using triggers, eliminating overhead on master databases and significantly reduces latency..


Check out the :ref:`quickstart` sections for examples of TCapture in action.


What TCapture is
-------------------

**TCapture Replication Server** is a ‘data movement’ product, which, well, moves data from one place to another. More specifically, it captures database transactions in one database and then applies these to another database.

The actual TCapture  engine is a Java application which runs as a separate program outside PostgreSQL, and which must be started explicitly. This program is located in $RDBBDR/bin
(BTW: the letters "rdbbdr" or "rdb" appear in various places when you're working with TCapture ; these come from the name "Replication DataBase BiDiRectional", which is the name under which it was originally developed).

When TCapture  is running, it will scan the transaction log of the primary database and pick up transactions which must be replicated. Transactions which have been picked up are stored in the "Replication Database" (a user database exclusively used by TCapture ). 
In the Replication Database, a transaction is 'copied' to all replicate databases which have a subscription for this transaction.
A transaction is applied to the replicate table by inserting it into by a Java application which runs on the slave .


Architecture 
------------------
The replication “engine” itself is a Java application,picking up replicated transactions is done via replication slots logical decoding functionality.
T-Capture stores captured transactions in table queues.
Stores replication information in various tables, among others, a table called walq__node, for each primary node, in the ReplicationDataBase.
The T-Capture  store a set of tables in the Replication Database (RDB). Each T-Capture  instance requires its own RDB.
T-Capture  uses a table called walq__node_offset in each replicate database to keep track of replicated transactions.
Because these tables are different, a database can be a replicate database for multiple primary postgresql at the same time .
Moreover a database can be primary for multiple slaves  at same time.


the overall architecture:

    Replication software once installed is a set of scripts and java code under the TCapture HOME which is identify by the variable $RDBBDR.
    The replication “engine” itself is a Java application, picking up replicated transactions is done via replication slots logical decoding functionality. The logcal decoding plugin is a library "rdblogdec.so" in /usr/pgsql-10/lib/ .

    Picking up replicated transactions is done via replication slots which are scanned by TCapture process .
    RepServer stores its transactions in stable queues; T-capture stores this in various tables (among others, a table called walq__node, plus one table (the "history table") for each primary node, in the Replication Database).
    The T-Capture dataserver interface  is a publ/subs table in the Replication Database (there's one publ/subs table for each subscribed replicate node).

    The T-Capture store a set of tables in the ReplicationDatabase (RDB). Each T-Capture instance requires its own RDB.
    T-Capture uses a table called walq__node_offset in each replicate database to keep track of replicated transactions. Because these tables are different, a database can be a replicate database for multiple primary at the same time . a database can also be primary for multiple slave at same time.


How it works
------------

T-Capture  Replication is a replication solution that can replicate large volumes of data at low levels of latency. T-Capture  Replication captures changes to source tables and converts committed transactional data to messages(records).


 The data is staged in tables. As soon as the data is committed at the source and read by T-Capture, the data is written to a queue table. 
 
 The messages/records are sent to the target location through publication/subscription of tables queues. 
 At the target location, the messages are read from the queues and converted back into transactional data. 
 
 The transactions are then applied to your target tables with a method that preserves the integrity of your data.
 
 You can use T-Capture for a variety of purposes that require replicated data, including failover, capacity relief, geographically distributed applications, and data availability during rolling upgrades or other planned outages.
 
 Sources and targets must be on PostgreSQL relational database management systems
 
 You can replicate entire database or a subset of tables from the source tables, using filters features

 All subsetting occurs at the source location so that only the data that you want is transported across the network.

 You can replicate entire database and filter on target (this is the case of multiple target with different need of tables replications

 Still not developed the feature to perform data transformations ( is in program)

Infrastructure for a TCapture Replication environment
-----------------------------------------------------
With T-Capture Replication you replicate committed transactional data from source tables to target tables by using two programs: T-Capture and T Apply.

T-Capture Capture program
    TheCapture program reads the recovery logs for changed source data and writes the changes to table queues. 
T Apply program
    The T Apply program retrieves captured changes from queues and writes the changes to targets. 
	
	The  Capture and the  Apply programs use a set of control tables to track the information that they require to do their tasks and to store information that they generate themselves, such as information that you can use to find out how well
	they are performing and such as data about the Capture program's current position in the recovery log 
	You create these tables when you tell T-Capture Replication what your replication sources and targets are.
	
	You must create the control tables for a Capture program on the PG server where the Capture program runs. In most cases, this server is the same server/differen DB where the sources associated with the program are located.
	 
	You must create the control tables for a  Apply program on the server where the target tables associated with that program are located.
	
--  Sources and targets in T Replication	

You pair source queue tables with targets by defining publication / subscriptions.

Attractions
-------------------------


One of the attractions of TCapture  is that it’s quite easy to set up and configure: starting from scratch, you can deploy a working replication system (albeit with very simple primary and replicate tables configuration) in less than 30 minutes.
The setup procedure is described quite well in the TCapture  User’s Guide (see documentation page).
As always with replication, make sure you have a clear idea of the replication logic you want to implement before you start (see multimaster considerations page).
