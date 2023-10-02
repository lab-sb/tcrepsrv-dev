.. _bugissues:

TCapture  Issues Bugs
==========================


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
			2- #BUG eccezione da gestire se lib non esiste allo startup o wal_level=logical not set	
			   #SOLVED mette in status down il thread master 
			3-  #BUG il monitor genera troppe connessioni 
				#SOLVED inseriti stmt ,result set , connection , close
			4-  #BUG perde prima transazione se il sistema è nuovo !!
				#SOLVED (da testare ancora) alla definizione TC_srvctl setup , fa un upsert su walq_node_ddl
			5- #ISSUE pg_temp se alter table da gestire
			
			riprendere la TCVer rdbbdr-dev-0.9.8.006
				
			
			6-  #BUG#101 
				#SOLVED#101: messo su wid invece che xid: select distinct xid from walq__qas where wid > (select last_offset from  walq__qas_offset )
							 messo fuori dal loop update walq_node_offset l'ulitmo wid preso nel loop (prima ci andava il primo wid)
							 opzione aggiuntiva : select distinct xid from walq__qas where wid > (select last_offset from  walq__qas_offset ) and xid <> (select xid_offset from  walq__qas_offset ) order by xid limit 500;

			
			7- #BUG#102 conseguente al #101  ovvero  order by xid riordina in modo errato se una transaz è terminata prima diuna precedente, va ordinato per dateop 
				 select distinct xid,dateop from _rdb_bdr.walq__prod2 where wid > (select last_offset from _rdb_bdr.walq__prod2_offset )order by dateop limit 500;

