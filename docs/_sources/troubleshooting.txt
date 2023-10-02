.. _troubleshooting:

TCapture Troubleshooting 
===========================
This section describes how to toubleshoot TCapture Replication Server  .

Check TCapture log files 
-----------------------------------------



	- log files 
	- exception files
	- excpetion table





.. code-block:: sh

        INFO  | 2020-01-10 16:33:56.883 | [TA-swap] edslab.TAppl (TAppl.java:335) - TA_swap_10544:into walq__nodemst_conflicts:insert into _rdb_bdr.walq__swap_conflicts  (xid,schemaf , tablef ,opdml ,state, message ,detail, hint, context)  values ( 2015417330,'missing' , 'missin' ,'?','error' ,'org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "prova_pkey"
  Detail: Key (aa)=(2) already exists.', 'INSERT INTO public.prova (aa, ab) VALUES (2, null)--swap','','')




.. code-block:: sh


$ ls -atalr $RDBBDR/log/
-rw-r--r--  1 postgres postgres       0 Jan 10 16:07 TCapture_qas_2020-01-10-16:07:59_err.log
-rw-r--r--  1 postgres postgres       0 Jan 10 17:07 TCapture_swap_2020-01-10-17:07:54_err.log
-rw-r--r--  1 postgres postgres 2908756 Jan 10 17:08 TCRepSrv_swap.log
-rw-r--r--  1 postgres postgres 2156140 Jan 10 17:27 TCRepSrv_qas.log


Increase log  verbosity to level="all" in log4j2.xml file 

.. code-block:: sh


	$ vi ../out/log4j2.xml

	 <Root level="info" includeLocation="true">
                        <AppenderRef ref="rollingFile" level="info"/>
                </Root>

Check TCapture process status
-----------------------------------------




.. code-block:: sh

	$ sh TC_srvctl.sh  --status --node qas --type producer

	Reading Status from _rdb_bdr.tc_process
	----------------------------------------------------------------------------------------------
	Status node  swap of type S
	id:0 should_be:up *status:down* pending_op:managed last_op:2020-01-1016:33:56



Check TCapture excpetion table
-----------------------------------------




.. code-block:: sh

	rdb_db__qas=# select * from _rdb_bdr.walq__swap_conflicts ;
    	xid     | schemaf | tablef | opdml | state |                                                message                                                |                          detail                          | hint | context |
  	dateop
	------------+---------+--------+-------+-------+-------------------------------------------------------------------------------------------------------+----------------------------------------------------------+------+---------+--------
 	2015417330 | missing | missin | ?     | error | org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "prova_pkey"+| INSERT INTO public.prova (aa, ab) VALUES (2, null)--swap |      |         | 2020-01
	-10 16:33:56.88427
       	     |         |        |       |       |   Detail: Key (aa)=(2) already exists.                                                                |                                                          |      |         |
(1 row)





.. code-block:: sh

	INFO  | 2020-01-10 16:33:56.883 | [TA-swap] edslab.TAppl (TAppl.java:335) - TA_swap_10544:into walq__nodemst_conflicts:insert into _rdb_bdr.walq__swap_conflicts  (xid,schemaf , tablef ,opdml ,state, message ,detail, hint, context)  values ( 2015417330,'missing' , 'missin' ,'?','error' ,'org.postgresql.util.PSQLException: ERROR: duplicate key value violates unique constraint "prova_pkey"
  Detail: Key (aa)=(2) already exists.', 'INSERT INTO public.prova (aa, ab) VALUES (2, null)--swap','','')

