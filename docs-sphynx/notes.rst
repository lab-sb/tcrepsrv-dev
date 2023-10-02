.. _notes:

TCapture Notes Issues Bugs 
==========================

-bash-4.2$ more note/note-pc.txt
ReplicaBdr

RdbBdr is a Replication Server Multi master solution for PostgreSql databases

the Engine is a combination of java , custom decoder lib , custom _rdb_bdr schema moving the data replication
and  slots/publication/subsctiption configuration on posgres side

The data decoded from wals is moved in primary nodes tables (walq_node tables) which are published

the node joininig a primary node flow replication subscribe the walq primary node table and scan the walq to apply
the replicate data locally.

replication slots are created on primary nodes for :
main decoderer slot
each subscriptor node of its walq publication

The java engine (StartReplication(could be renamed to RepSrvr )) duty is to  move data from decoder wal to walq table in a robust, secure, fast, reliable manner.
The java engine/or/set of functions postgres (Consumer) duty is to scan the walq locally tables and apply changes to local databases managing filters and conflicts.




-- 04 apr

pull wal CDC to walq table from primary
pushwal function

push from walq tables to slave
runwal function
scanwal function
--

-- 07 apr
custom mydecoder.c

-- 09 apr

cross CDC between 2 nodes  with infinitive loop problem

-- 12 apr
mnaged inifnite loop with  introduc of walq_node_xid

-- 16 apr
java AppLocalDb sostituisce pushwal functions

-- 18 apr

problema di unica xid con tante DML gestito replicandola come unica xid
e non con n xid a destino

-- 24 apr

funziona MultiMaster 2 nodes

-- 27 aprile

funzionea MMR 3 nodes

-- 01 mag

test carico  150 Tps su 2 nodi
-- 03 mag
scirpt di configuraz
add_primry_node
join_a primary
..

configurazione folders
path RDBBDR
conf
sql
src
bin
run
tmp
ecc..

-- 04 mag

Java StartReplication

- 05 mag
Java StartReplicationRDB ( con db di apooggio per  la replica)

-- 06 maggio

skip xid se è un blocco contenente walq_ operazioni

-- 08 mag
introduzione truncate table walq su primary in java e su slave in runwalt
introduzionie filtri scanwalf rdb_bdr.walq__cina_filtro

-- 09 mag
- conflitti manca il filtor su tipo op v_opdml

-- rdb_help
   rdb_resumerep
   rdb_suspendrep
   rdb_replicateddl

_rdb_bdr.rdb_walq_urss_lastcommit   rinomina di walq__urss_offset

 _rdb_bdr.walq__cina_xid       rdb_walq_cina_xid
 _rdb_bdr.walq__urss           rdb_walq_urss
 _rdb_bdr.walq__urss_conflicts rdb_walq_urss_conflicts
 _rdb_bdr.walq__urss_filtro    rdb_walq_urss_filtro
 _rdb_bdr.walq__urss_log       rdb_walq_urss_log
 _rdb_bdr.walq__urss_offset    rdb_walq_urss_lastcommit



-- FDW+db_link

----------------------------------------------------------------------------------------
-- portare tutto su java gli script di creazione e monitoraggio..
-- funzionalità multithread dei proessi Capture e Apply 
-- da gestire la truncate dei walq_flor sia sul master che sugli slave tenenedo conto del gap per non peredere dati

-- TWrapperSQL.class  , ex:  sh runTWrapperSQL.sh /tmp/1.sql 
   Wrapper per eseguire sql che non entri nel flusso di replica usando la _rdb_bdr.walq__flor_xid

-- src/com/edslab/TCRepSrv.class
-- TAppl.java TCapt.java  TCRepSrv.java
   multithread  RepServer con tabella tc_process , con funzionalità stop/sart Master e Slaves con RS in esecuzione


-- trace di ddl eseguite su  _rdb_bdr.walq__flor_ddl     con uso di event trigger
-- UPSERT ogni secondo su walq__flor_ddl 
   
-- tc_cli_validator  
	valida il flusso di replica master e i vari slaves .. 
	scrive su  master  _rdb_bdr.walq__syba_mon ;
    -- da creare un thread  separato di monitoraggio continuo che popoli la tabella  


-- gestitre le truvcate walq  e walq_xid 
	walqtrunc=30
	batch_size=1000
	loglevel=FINE
	log_hist=true

   
-- modifca logger per usare log4j2cat.xml  su   log/TCRepSrv.log 
  
  
-- -Duser.timezone=UTC in bin/runTCRepSrv.sh
   
   
   
 -- varie  --------
caching query altro thread
docker
truncate table wal_q con tabella parallela di switch
healtcheck table in _rdb_bdr.walq__flor_mon 
 -----------

architettura di tipo mesh


tc_process 				configuro master e  di quale nodi sono slave , es :  flor M, flor slave di syba S e dl380 S
pg_replication_slots 	vedo di quale nodi sono master				 , es :  flor master per syba S e dl380 S

deve finire tutto in tc_process o tc_monitor in modo da monitorare gli slaves rempoti (stato, position, dateop,ecc)


-- 30 08 2019
 
 TMoni.java per il monitoraggio di master, slave remoti , slave locali

-- 01 09 2019
 following features are not available:

    Management User Interface
    Column level filtering
    Update/Update and Insert/Insert conflict management
    Enhanced cluster-wide monitoring
    Windows Server support


-- 02 09 2019
	rotate RollingFile in ../src/log4j2.xml
	 <level value="OFF" /> in log4j2
	 INSERT INTO _rdb_bdr.tc_monit   ON CONFLICT  se q  è ststa truncate

-- 03 09 2019 
	rivedere le truncate di TCapt e TAppl 
	occorre un thread Coordinator ?? 	
	viste varie per query su tc_monit tc_proces walq__ddl xid ecc
	spostare walq__flor_ddl e walq__flor_xid su rdb_db__flor ??
	
	refresh slave di TMON se db slave down da eccessione e esce
	
-- 05092019
	modificata isXidCurrentNotManage in TCapt select exists( select 1 from " + walq + "_xid where xid_current > ?  (dove ? è preso da select max(xid) from " + walq )
	in questo modo pare funzionare , 
	da verfificare i blob sul campo data (quanti byte di blob c'entrano??)
	
-- 09092019
	bug # java heap out of memory transazione commita parziale 
	BEGIN > bug # 
	10:47:46.999 [TM-flor] INFO  com.edslab.TAppl - TM-flor_99350:<scanned: 55
	Exception in thread "TC-flor" java.lang.OutOfMemoryError: Java heap space
        at java.util.Arrays.copyOf(Arrays.java:3332)
        at java.lang.AbstractStringBuilder.ensureCapacityInternal(AbstractStringBuilder.java:124)
        at java.lang.AbstractStringBuilder.append(AbstractStringBuilder.java:448)
        at java.lang.StringBuilder.append(StringBuilder.java:136)
        at org.apache.logging.log4j.core.pattern.LineSeparatorPatternConverter.format(LineSeparatorPatternConverter.java:64)
        at org.apache.logging.log4j.core.pattern.PatternFormatter.format(PatternFormatter.java:36)
        at org.apache.logging.log4j.core.layout.PatternLayout.toSerializable(PatternLayout.java:196)
        at org.apache.logging.log4j.core.layout.PatternLayout.toSerializable(PatternLayout.java:55)
        at org.apache.logging.log4j.core.layout.AbstractStringLayout.toByteArray(AbstractStringLayout.java:71)
        at org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender.append(AbstractOutputStreamAppender.java:108)
        at org.apache.logging.log4j.core.config.AppenderControl.callAppender(AppenderControl.java:99)
        at org.apache.logging.log4j.core.config.LoggerConfig.callAppenders(LoggerConfig.java:430)
        at org.apache.logging.log4j.core.config.LoggerConfig.log(LoggerConfig.java:409)
        at org.apache.logging.log4j.core.config.LoggerConfig.log(LoggerConfig.java:412)
        at org.apache.logging.log4j.core.config.LoggerConfig.log(LoggerConfig.java:367)
        at org.apache.logging.log4j.core.Logger.logMessage(Logger.java:112)
        at org.apache.logging.log4j.spi.AbstractLogger.logMessage(AbstractLogger.java:727)
        at org.apache.logging.log4j.spi.AbstractLogger.logIfEnabled(AbstractLogger.java:716)
        at org.apache.logging.log4j.spi.AbstractLogger.info(AbstractLogger.java:526)
        at com.edslab.TCapt.checkFilt(TCapt.java:348)
        at com.edslab.TCapt.receiveChangesOccursBeforTCapt(TCapt.java:596)
        at com.edslab.TCapt.run(TCapt.java:179)
        at java.lang.Thread.run(Thread.java:748)
	bug # < END
	
-- 09092019 interfaccia grafica	
			postgres 11 e 12 e 9.X 
			funzioni 
			 tc_cli.sh -n flor -status down 
			 tc_cli.sh -n flor -status up
			 ...
			 
-- 12092019 
		sqlworkbench , symmericds 
		docker e aws 
		sicurezza rdbbdr_user 
		parallelizzazione TCapt TAppl per performance
		
-- 15092019
	http://www.tcapture.org/		
	in stile PostgreSQL Replicator
	LA https://postgresrocks.enterprisedb.com/t5/EDB-Technical-Updates-and-Alerts/EDB-Technical-Update-for-Postgres-Replication-Server-7-0-Limited/ta-p/4050
	
-- 18092019	
	add VERSION file under rdbbdr
	aggiungere controlli su pg version pg up acc

	add installation steps:
	rpm , dockers, ecc
	vedi installation-rdbbdr_0.9.6
	Data Validator source and target database to compare the tables in schema
	BUG # In caso di definizione di master senza slave e startup del processo di replica la coda si riempe fino a raggiunger il walqtrunc point e quindi troncata con conseguente perdita di dati!!
		# quini alla truncate verificare che ci siano slots di sottoscrizione e che il max(xid) in walq_ replicato sia appaiato al max(xid) del walq locale che vado a troncare
		# ad esempio funzione canBeTrunc() che verifica quanto sopra 
	
-- 19092019	
	BUG # In caso di definizione di master senza slave e startup del processo di replica il log su TCRepsrv.log è ok ma su src/Tcapture---.log logga TAppl come mmirror di TCapt 
	BUG#  In caso di start del processo di replica  e non ci sono transazioni _rdb_bdr.tc_monit è vuoto
	###  ci sono tabelle da togliere su schemi _rdb_bdr del master e del rdb_db_master
	
	# nel configurare lo slave losa di un primary nyci già configurato :
	 - cerca il file /var/lib/pgsql/scripts/mycode/rdbbdr-0.9.6/conf/losa_rdb_bdr.conf
	 - cerca il file /var/lib/pgsql/scripts/mycode/rdbbdr-0.9.6/conf/losa_log.conf
	 - errore relation "_rdb_bdr.tc_process" non esiste sullo slave losa
	 - errore  relation "walq__losa_xid" il los_db perche viene creata con l add_primry_node mentre qua è solo slave .. ma il Tappl se ne frega e non controlla...
	 - erroe /var/lib/pgsql/scripts/mycode/rdbbdr-0.9.6/conf/losa_bdr_rdb.conf not exists! sul primario quando TMon va a fare il check delgi slave

- 20092019
	#rivedere i messaggi di log  che sono poco chiari
	

- 21092019 
	# audit delle truncate della walq_node e delle operazioni sulle _rdb_bdr.tc_process
- 22092019
	# gestire le strutture temporanee pg_temp.. quando viene fatto un alter table
	# invece di tc_cli_add_primary_node... conf.. remote.. ecc..
	
	# tcctl add node  -- Adds a database configuration to your TC Replication server  configuration.
		
		tcctl add node -n node -h host -u -p -pwd -db -rh -ru -rp -rpwd -rdb 
		
	 tcctl setenv nodeapps -n node_name {-t name=val[,name=value,...] 
		tcctl setenv -n nyci -t waltrunc=1000000, filter=on 
	
	  tcctl config	-- Displays the configuration for a node
		tccl config	-n nycy 

- 25092019	
	tc_process tc_monit n_master diventa n_mstr n_slave diventa n_slv
	
- 27092019
	 --	Creating a subscription that connects to the same database cluster (for example, to replicate between databases in the same cluster or to replicate within the same database) will only succeed if the replication slot is not created as part 
		of the same command.
		Otherwise, the CREATE SUBSCRIPTION call will hang. To make this work, create the replication slot separately (using the function pg_create_logical_replication_slot with the plugin name pgoutput)
		and create the subscription using the parameter create_slot = false. 
		This is an implementation restriction that might be lifted in a future release.
		
	-- setup consumer node : inserisce con id = 0 ma se ce ne sono più di 1 ?! da gestire	
	
- 28092019	
	-- check versione pg comaptibile con tc 
	-- check version TC , all'installazione/setup inserisce la versione , poi il controllo viene fatto con il file TCVer
	
-29092019
	- runTCRepSrv -n node check che non sia già attivo e deviazione error log su TCapt..._err.log
	- com/edslab/TCSrvCTL.java unset -node -producer/consumer 

-30092019	
   -  gestito lo stato di messa in down nel caso di eccezione su loadProps per TCapt e TAppl manca TMoni

-01102019 
	- da gestire i logs( togliere i System.out.println  e sostituire con LOGGERS.info / LOGGERS.error  
	- togliere i system.exit (tranne che sulla classe principale TCRepSrv) 
    -ripulire i log messages
    - gestire tc_process start stop da sh TC_srvctl.sh -start -node nyci -type producer/monitor/consumer 
 -01102019 
	# BUG  mentre lo slave razzola le transazioni  non controlla se è stato messo a stop nel tc_process quindi occorre aspettare il termine della razzolata che in caso sia molto indietro potrebbe essere lunghissimo
	# messa toppa con limit nel selectXid ( questo però da problemi nel caso di truncate che richiama la selectXid per svuotare la coda .... meglio allora l opzione senza limit
    usare modo di versionare il codice 
    approfondire log4j threadcontext (in particolare diffenti livelli di log per i vari threads)	

	TCRepSrv trasformare in thread e da  TC_srvctl.sh -stop -repsrv -node <node_name> che chiama una funzione nel thread TCRepSrv che fa lo stop dei vari thread (TCapt TAppl TMoni) ed esce pulito
	
-------
	isServerCompatible spostato in TCRepSrv e > 9.6.15 ovvero dalla 10 in poi
	
	review pg-jdbc per spunti 
	revire gitlab github per versioning (private use)
	
- 02102019
		Reload log4j.xml at runtime  in modo da poter cambiare il livello di log senza restart
		# messa toppa al BUG 01102019 con il limt a 500 nella selectXid() in TAppl
		
-03102109
		da implementare:
			- cryptog password
			- check versione Community Edition e disattivazione delle funzionalità : ad esempio start di TMoni  && checkVersioneCE , 
			- propagazione SQL DML dal nodo master a tutti i consumer  con un TCWrapper_DML (diverso da CWrapper_SQL) insernedo direttamnte su walq_node e allo slave arriva come campo data da eseguire)
			V - stopTCepSrv  graceful  - mette a stop tutti ithread e loppa per check che siano stopped e a questo punto system.exit se supera il timeout di graceful shutdown lo killa
				- in loppa di TCRepSrv controlla operations not managed , in questo caso trova n_operations='shutdwon' messo da un processo esterno che mette tutti i thread in shutdown 
					mette tutti i thread in stop e state in shutdown ed esce
			- fare uno stroico del tc_monit in modo da avere una tendenza (converge,diverge,stabile) sulle ultime delta o gap 
			- check conflitti da Moni in base a data check e data  conflict ( e status false dello slave ) ovvero se è down (stato false) vado a pescare la data ultimo conflitto e se è vicina al check lo visualizzo in tc_monit
		    - sonda 
					metto in una tabella (id, ready_to_send,now()) 
					TCapt quando passa nel giro vuoto(ovvero quando fa l upsert su wal_ddl) lo spara su walq_nodo con stato send
					TAppl lo trova su  walq_node e lo esegue aggiungendo applyed e la sua data di esec : now() che sarà di un delta rispetto al now() del ready_to_send , questo delta ci dice il gap tra una transaz al master e la esec allo slave
		
		- CommunityEdition BusinessEdition ->  StandardEdition  / EnterprisEdition
		
-04102019 
			gestito stopTCepSrv che richiama TCSrvCTL shutdown e gestisce il gracefull shutdown time limit e poi lo stronca
			# in caso di start di TCapt e deve frullar moltissime transazioni applicate come slave , fino a quando non le ha smazzate tuttte non logga perche busy 
		
		
-- deployare una versione con struttura definitiva 	 tcrepsrv-0.9.7.rhel7.x86_64.tar.gz	
   come ramo distinto da quello di dev  ovvero un sotto insieme 
		la struttura dev dev-0.9.7.001 ha formato dev-MainVersion(0)-SubVersion(9)-X-YYY
		la struttura non dev 0.9.7 ha formato MainVersion(0)-SubVersion(9)-X 
		

-- possibile parallelizzazione 
			partition della tabella walq_node 
			 abbiamo walq_node_11 
					walq_node_2
					walq_node_3	
					walq_node_4
				
			che vengono alimentati con un modulto sul txid (modulo 4)
			
			il TAppl viene diviso in 4 thread ognuno segue walq_node_x e in modo syncronyzed sull oggetto esegue la transazione e committa in ordine 
			il commit in ordine può essere fatto mantenendo la lista dei txid in gestione in quell istante ai vari threads e guindi uno può committare se non esiste nessuno con txid < in lista
			
-- in version 11 la funzionalità di truncate viene replicata : Replicate TRUNCATE activity when using logical replication				
	questo ha impatto sulla trucate delle walq_node  che potrebbero essere troncate a valle (lo slave prima che le abbia consumate) 
	occorre coordinare la truncate in modo che siano sincronizzati e consumati ...
	un modulo coordinator che quando la walq_node supera il limite e gli slave sono allineati o quasi spegne il TCapt finisce di scodare i TAppl  diabilita le subscr tronca la tabella negli slave e nel master e riabilita tutto
					
			
-08102019
	- txid wraparound
	- initial load  , attivazine replica - pg_dpump con xid e poi restore e set dell xid_offset a xid di restore						
	- modulo maintenance (thread) che check in tc_monit che il lag tra 	xid_offset e q_xid sia vicino e stoppa slave local master local e aspetta che gli slave remoti smazzino le xid rimanenti e tronca le walq_node e poi riattiva tutto.
	 e salavare la wid di truncate da qualche parte e poi il check lo fa in base al superamento di un threshold sulle wid ( pwerchè le xid possono avere dei salti anche importanti)
		
	- aggiungere a walq_node_xid lo slave di provenienza , field n_slv
	
	-- la funzionalità di truncate della vers 11 di postgres viene bypassata implementando la create publication con : 
			CREATE PUBLICATION insert_only FOR TABLE mydata      WITH (publish = 'insert');
			 modify  com/edslab/TCSrvCTL.java   String qq= " create publication "+node+"_publ for table _rdb_bdr.walq__"+node+"  WITH (publish = 'insert');";

	
	
	-- da aggiungere la 	big quey check_maintenance e la walq_master_trunc nella TMoni in modo da avere anche un alert di lags tra le varie code e xid e truncate threshold
	
	
-09102019
		TWrapperSQl di un update di milioni di record occorrono minuti prima che lo smazzi
		ByteBuffer usare in TCapt al posto delle operazioni su String
-10102019
		aggiungere campo sorgente in _rdb_bdr.walq__urss_xid 
		
-11102019 
		fdw della walq_ in modo che il contenuto "data" possa essere caricato su un sistema eterogeneo 
		hash ?!! del data /transazione in walq 
		database chiave valore  ( txid, data) come pre  walq_node da cui poi selezionare i record per walq_node 
		
		
-16102019 		
		test con queue ConcurrentLinkedQueue e multithreading Worker process  
			ispirandosi a https://stackoverflow.com/questions/49811474/fastest-way-to-process-a-file-db-insert-java-multi-threading
		io faccio in com/edslab/TCaptureTest.java :
						myquery = toString(buffer);
                         mylsn = stream.getLastReceiveLSN().asString();
                         queue.add(mylsn+"#"+myquery );
	ma vedendo https://github.com/tolitius/mongodb-write-performance-playground/blob/master/java/driver.mods/com.mongodb/DBApiLayer.java:
		capisco che  invece di:
			 Queue<String> queue = new ConcurrentLinkedQueue<>();
	posso
			Queue<WalTrans> queue = new ConcurrentLinkedQueue<WalTrans>();
	
			dove WalTrans  è:
			
		static class WalTrans {
        WalTrans( LogSequenceNumber a , ByteBuffer b ){
            lsn = a;
            bb = b;
        }

        final LogSequenceNumber lsn;
        final ByteBuffer bb;
    }
	
	
	e posso accedere;
	
		 while (( c = queue.poll()) != null ){
             x = c.bb ;
			 
			 
-17102019
			la Queue<WalTrans>  viene alimentata da TCapt (TcaptureTest) che ne è il produttore
			viene consumata dai thread TWork_XX che ne sono i consumatori
			
			consumate le coppie ByteBuffer ( che diventa Stringa txid#data ) e LSN 
			queste vanno aggiunte all stmt.addBath in modo che siano ordinate e che vengano committate alla fine di ogni txid.
			per questo metterei tutto sotto  una mappa 
			Map<TXID,List<LSN>> m = new HashMap<TXID,List<LSN>>();
			
			ovvero ogni txid ha una serie di LSN
			
			in più vanno agganciati 
			ad ogni lsn i data String con un altra mappa
			Map<LSN, String> = new HashMap<LSN,String>();
			
			
			a questo punto i threads TWork_XX sono diventati produttori per queste mappe 
			occorre creare dei consumatori che scansionano le mappe riordinate per txid e per ogni lsn vanno sull'altra mappa e prendono data String per poi costruire  stmt.addBath e commit con la logica originaria di TCapture
			
			
vedere debezium  per spunti molto interessanti:
https://github.com/debezium/debezium/blob/master/debezium-connector-postgres/src/main/java/io/debezium/connector/postgresql/connection/pgoutput/PgOutputMessageDecoder.java

			vedere nuove versioni jdbc postgres driver
			
			
- 04112019
	TCapt	
		stream -> lsn,buffer  - queue add <WalTrans>  
	  		-> TWorkP_X   queue poll - check vari - map <xid , <lsn>>  e <lsn,data>  - <xid,<wrkrs>>
			-> TWorkC     manage  min xid di <xid,<wkrs>>   - for each xid, lsn - lsn data e commit
		
   TAppl   
			xid  from walq__nodemst  con xid > xid_offset limit 500	-  for each xid get data order by wid - check vari - execute (data) - update offset e walq_node_xid
			
			select xid,lsn,data   - queue add xid oppure select lista xid soltanto da passare in queue lasciando la select di data e lsn ai workers
			 
			 -> TAWorkP_X   	queue poll -  select data,lsn from walq_nodemst where xid = xid (polled)  
								check vari (filtri)
									map <xid , <lsn>>  e <lsn,data>  - <xid,<wrkrs>>
				
				

       altra opzione:
		
			prendo la granularità delgi lsn e non xid . ovvero faccio il lavoro simile al TCapt - TWorkP che lavora su una queue di lsn, buffer
			diventa:
			
			lsn from walq__nodemst  con xid > xid_offset limit 500	- 
		

-- 05112019
	TCapt -> passare stream in TStructures per aggiornare puntatore : 
				stream.setAppliedLSN(stream.getLastReceiveLSN());  ovvero stream.setAppliedLSN(lsn)  dove lsn è quello gestito da TWorkC
				stream.setFlushedLSN(stream.getLastReceiveLSN());  idem
				
	 isXidCurrentinWal in TWorkP dee impostare uno skipp (BoolTxid) sennò viene ripetuto inutilmente.
	 
	 le map di xid,<lsn> e <lsn,data> vanno rimosse le entry elaborate
	 ancora da gestire la truncate dei walq_node

-- 07112019
	da rivedere il catch delle eccezzioni e i logs trace/info
	far girare il rep server su un nodo a se ?!
	togliere schema _rdb_bdr da db master , implica spostare walq__node_xid
	
-- 20112019
	 rimessa la truncate nel giro a vuoto del TCapt . la connessione è condivisa con TWorkP/C e quindi eredita il set autocommit fals messo in TWorkP 
	 da vedere il truncate del walq_xid che  e il Tappl che se ristrutturato con i thread come TCapt ne avrà un comportamento simile.
	 
-- 22112019
	 gestire eccezzioni tirando giu il thread di appartenenza , testare mettendo errori nei sotto thread e vedere che il thread principale va in stato down
	 gestire conflitti  
	  
	aggiungere  :  .. associata alla _rdb_bdr.walq__master_log del ../sql/consumer_structure_rdb_rdbbdr.sql
	CREATE SEQUENCE IF NOT EXISTS _rdb_bdr.walq__master_wid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
	
	in questo caso:
	CREATE SEQUENCE IF NOT EXISTS _rdb_bdr.walq__urss_log_wid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
	
	e alter table walq__urss_log alter wid set default nextval('walq__urss_log_wid_seq'::regclass);
	
-- 25112019
  cpu alte con TAppl attivo
  non elabora fino all ultima transazione il TAwrkC ma il wid lo setta bente Tappl
  seguire jdbc tips best practice: no connessini condivise tra i thread , no string concatenation m a usare bind variabile ? , usare prepared statemnet e batch, no execute diretto. ( questo lo facevo già nella vecchia version)
  migliora anche la old version??
  
 
  il campo data termina con ; -- nodemstr 
  questo va  bene se il TAppl esegue st.execute(query) . ma se esegue st.addBatch(quesry)  i ; danno noia : org.postgresql.util.psqlexception too many update results were returned
  quindi va tolto il ; alla fine del campo data   , sostituito in TWorkP con scut = s.substring(mydmlpos + 1, s.length() -1 );

  
-- 26112019
	- valutare come togliere connection condivise tra i thread , impliczioni su connection.setAutocommit(false); add.batch(query), executeBatch...
	- String concatenation sositure con i bind variables (?,?,?) ...
	- gestire eccezioni e state down dei sottothread 
	- truncate walq e walq_xid ( thread indipendente che gestisce?)
	- multithread TWorkP può andare n errore e causare bug inconsistenza di una xid inserita + volte. da rivedere come capire che un thread ha fatto la xid ed è andato oltre
	- code versioning
	- intellij pulire codice
 	
	
-- 03122019
	#DONE# 	github https://github.com/lab-sb/tcrepsrv-dev/
	#NOTA#	due filoni di sviluppo - devedition - standardedition
	#DONE# 	http://sbrandani.alwaysdata.net/tcapture/ mysphinx
	
	
-- 16122019
	#CHECK#099	run del TC Rep Server da un nodo (sganciato dai nodi db replicati)
				sh runTCRepSrv.sh -n prod4
				sh runTCRepSrv.sh -n prod3
	
	
-- 18122019
	#CHECK#099	TCSrvCTL.java setup con user diverso da rdbbdr_user e setup con rdb_db__<node> su db diverso dal db di <node> 
    #NOTA#118	TAppl.java	rimesso execute diretto per catch dell'eccezione e insert nella tabella conflict : 
							str.execute(query);
                          //str.addBatch(query); va poi gestita l eccezione per inserire nel conflict
					con il addBatch e il commit a fine transazione  non riesco a gestire il conflitto ( da rivedere)
	
-- 19122019 
	#ISSUE da fare create subscription da gestire  copy_data (boolean) - The default is true. ( ma si vuole dare l'opzione false)
     da fare 
	 
-- 13012020
	in mysphinx a complete example con :
		drop_all_fk_constraint.sql
		incremnet_by_4_per_all_mmr_4nodes.sql
		set_value_sequnece_0_mil.sql
		fix_primary_key_missing.sql

-- 14012020
	 #BUG#100 eccezione too long not fit in columns:  
	 #solved#100  modificata _rdb_bdr.walq__master_conflicts seguenti colonne in text: 
				state text,
				message text,
				detail text,
				hint text,
				context text,
		
-- 15012020
			1- utente rdbbdr_usr e permessi grant in sql script 
			   #attenzione: usando db01 user da problemi appliativi perchè imposta la search_path a _rdb_bdr	
			2- #BUG 		eccezione da gestire se lib non esiste allo startup o wal_level=logical not set	
			   #SOLVED 		mette in status down il thread master 
			3-  #BUG 		il monitor genera troppe connessioni 
				#SOLVED 	inseriti stmt ,result set , connection , close
			4-  #BUG 		perde prima transazione se il sistema è nuovo !!
				#SOLVED 	(da testare ancora) alla definizione TC_srvctl setup , fa un upsert su walq_node_ddl
			5- #ISSUE 		pg_temp se alter table da gestire
			
			riprendere la TCVer rdbbdr-dev-0.9.8.006
				
			
			6-  #BUG#101 
				#SOLVED#101: messo su wid invece che xid: select distinct xid from walq__qas where wid > (select last_offset from  walq__qas_offset )
							 messo fuori dal loop update walq_node_offset l'ulitmo wid preso nel loop (prima ci andava il primo wid)
							 opzione aggiuntiva : select distinct xid from walq__qas where wid > (select last_offset from  walq__qas_offset ) and xid <> (select xid_offset from  walq__qas_offset ) order by xid limit 500;

			
			7- #BUG#102 conseguente al #101  ovvero  order by xid riordina in modo errato se una transaz è terminata prima diuna precedente, va ordinato per dateop 
				 select distinct xid,dateop from _rdb_bdr.walq__prod2 where wid > (select last_offset from _rdb_bdr.walq__prod2_offset )order by dateop limit 500;

	
-- 17012020	
		#BUG#103	search_path='_rdb_bdr' su user rdbbdr_user genera errore se viene richiamata una funzione public (ex. ll_to_earth è in default in un campo tabella)
		#SOLVED#103 	search_path='_rdb_bdr','public'
	
-- 20012020
		v- check user rdbbdr_user su db prod - e user differente su rdb 
			
		#TODO#108	walq_node_xid aggiungere campo di node_name di provenienza
		#CHECK#100	prova 3 nodi mmr script di configur
		#NOTA#119 	prova pg_basebackup e sincr con tcapture tutto in in un comando
		#TODO#109	tabella storico conflitti e pulizia tabella conflitti automatica quando offset è maggiore
		#TODO#110	monitor script che looppa e incrocia tc_proces tc_monit conflict filter e last sql nei vari processi - in stile tc_cli_validator e che faccia anche la topology
		#TODO#111 	TC_srvctl confirmation messages on each choice (at least unset)
	
-- 21012020
		#CHECK#101 versione postgres 9.6 ?? funziona??
		#NOTA#120 
					prd_db=# create table prova (aa serial, ab varchar(20) default 'prd' , ac timestamp default now(), ad bigint default txid_current());
					prd_db=# alter table prova add primary key (aa,ab);

		#BUG#104 NON BLOCCANTE - MISSING INFORMATION - se rdb è su altra istanza TMoni non popola la tc_monit per il record relativo al replication slot rdb_prd_bdr
		#SOLVED#104 aggiunta union per il ramo slot rdb_prd_bdr not exits per questo caso che mancherà delle informazioni su questo slot e il suo stato
		
-- 22012020
		#TODO#112 legge conf file e mette info in una tabella in modo per esempio da rileggere la variabile filter al volo 
			TC_srvctl --filter=on volatile 
			TC_srvctl --reloadconf --node 
		#TC_srvctl --setfilter --schema --table --operations [D/I/U]
		#FEATURES# slave heterogeneous datatbase type 
			
		#DONE# sh TC_srvctl.sh --marker --node swap --type consumer --producer qas [--next_xid/--set_xid=<xid number>]
		
-- 24012020		
		#TODO] 
			_rdb_bdr.tc_monit manca wid offset e dataop dell offset
		
-- 27012020		
			#DONE# TC_srvctl.sh --topology --node qas --detail
				qas MASTER up/down	
				qas <-- prd active
				qas --> prd enable 
				
				prd MASTER up/down	
				prd <-- qas not_active
				qas --> prd enable
--28012020
			#ISSUE#	quando fa la creazione della subscription con copy_data = true - partono dei temporary replication slots che si chiamano : <sub_name>_45800913_sync_2720590 
				
			#FEATURES# automatizzare: 
				setup replica e sync directory : (nam3ab) pg_basebackup -h ahost.edslab.it -U barman -p 5432 -D/ backup/pg01 -P -Xs -R -v -l backup_pg01ng
			#DONE#	select _rdb_bdr.monitor_qas_to_('prd');

--04022020
			# 
			#		procedura per sincronizzare master con slave:
			# https://www.percona.com/blog/2018/11/30/postgresql-streaming-physical-replication-with-slots/
			# poi commit 
			# stop master
			# stop slave
			# setup TC masters
			# setup TC slaves
			# start masters
			# start slaves 
				


