.. _tablefiltering:

TCapture table level filtering 
==============================

Learn how to use table level filtering in TCapture Replication

TCapture RepSrv filtering
-------------------------


Activate the filtering 

.. code-block:: sh

	filter=true
	 # file ../conf/prod1_rdb_bdr.conf

Configure filter options inserting directly in table , 
	example all table in public schema Delete/Insert/Update;



.. code-block:: sh


	insert into _rdb_bdr.walq__prod1_filtro values ('public','all','DIU', now());
	INSERT 0 1

	rdb_db__prod1=# select * from _rdb_bdr.walq__prod1_filtro ;
	
 	schemaf | tablef | opdml |           dateop
	---------+--------+-------+----------------------------
 	public  | all    | DIU   | 2020-01-13 15:49:21.344249




Recycle Replication Server:

.. code-block:: sh

	sh stopTCRepSrv.sh -n prod1
	sh runTCRepSrv.sh -n prod1


Test it:

.. code-block:: sh

	delete from prova where aa=66;


Check log trace:

.. code-block:: sh

	TRACE | 2020-01-13 15:52:02.334 | [TC-prod1] edslab.TCapt (TCapt.java:563) - TC-prod1_651:<Begin Txid> :9777247
	TRACE | 2020-01-13 15:52:02.334 | [TC-prod1] edslab.TCapt (TCapt.java:573) - TC-prod1_651:<< Debug >>  line:9777247#DELETE FROM public.prova WHERE aa = 66 AND ac = 'prod1'; Txid: 9777247
	TRACE |	 2020-01-13 15:52:02.334 | [TC-prod1] edslab.TCapt (TCapt.java:315) - TC-prod1_651:<< Check >> : select 1 from _rdb_bdr.walq__prod1_xid where xid_current =9777247
	TRACE | 2020-01-13 15:52:02.338 | [TC-prod1] edslab.TCapt (TCapt.java:362) - TC-prod1_651:checkFilt on  DELETE FROM public.prova WHERE aa = 66 AND ac = 'prod1'
	TRACE | 2020-01-13 15:52:02.338 | [TC-prod1] edslab.TCapt (TCapt.java:363) - TC-prod1_651:v_opdml is  D
	TRACE | 2020-01-13 15:52:02.338 | [TC-prod1] edslab.TCapt (TCapt.java:368) - TC-prod1_651:parz is  #public.prova#
	TRACE | 2020-01-13 15:52:02.338 | [TC-prod1] edslab.TCapt (TCapt.java:370) - TC-prod1_651:qschema is  #public#
	TRACE | 2020-01-13 15:52:02.338 | [TC-prod1] edslab.TCapt (TCapt.java:372) - TC-prod1_651:qtable is  #prova#
	TRACE | 2020-01-13 15:52:02.344 | [TC-prod1] edslab.TCapt (TCapt.java:610) - TC-prod1_651:Filter is true
	TRACE | 2020-01-13 15:52:02.354 | [TC-prod1] edslab.TCapt (TCapt.java:513) - TC-prod1_651:.......................................>>  Commit#1 >> in Buffer null - Txid:0 - Txidbef:9777247
	INFO  | 2020-01-13 15:52:02.355 | [TC-prod1] edslab.TCapt (TCapt.java:514) - TC-prod1_651:Managing xid 9777247
	INFO  | 2020-01-13 15:52:02.356 | [TC-prod1] edslab.TCapt (TCapt.java:521) - TC-prod1_651:Scanned:1



