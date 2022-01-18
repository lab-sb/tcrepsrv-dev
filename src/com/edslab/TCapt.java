/**
 * # -- Copyright (c) 2018-2022  silvio.brandani <support@tcapture.net>. All rights reserved.
 *
 */
package com.edslab;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.PGConnection;
import org.postgresql.PGProperty;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.ServerVersion;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.replication.PGReplicationStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Logical Decoding TCapt
 */
public class TCapt implements Runnable {




    private AtomicBoolean running = new AtomicBoolean(false);
    private String mythread = "";
    private static final Logger LOGGERT= LogManager.getLogger(TCapt.class.getName());

    private Connection connection;
    private Connection connectionRdb;
    private Connection replicationConnection;
    private String nodemst = "";
      private int threadn = 0;
    private String host = "";
    private String user = "";
    private String pwd = "";
    private int port = 5432;
    private String db = "";
    private String node = "";
    private String walq = "";

    private String rhost = "";
    private String ruser = "";
    private String rpwd = "";
    int rport = 5432;
    private String rdb = "";
    int walqtrunc = 9;
    int batch_size = 1000;

    private boolean filter = false;
    public boolean isInitialized = true;

    private String currentlsnfunc = "";

    private static String toString(ByteBuffer buffer) {
        int offset = buffer.arrayOffset();
        byte[] source = buffer.array();
        int length = source.length - offset;

        return new String(source, offset, length);
    }

    private String createUrl() {
        return "jdbc:postgresql://" + host + ':' + port + '/' + db;
    }

    private String createRdbUrl() {
        return "jdbc:postgresql://" + rhost + ':' + rport + '/' + rdb;
    }

    public void createConnectionRdb() {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", ruser);
            properties.setProperty("password", rpwd);
            properties.setProperty("reWriteBatchedInserts", "true");

            connectionRdb = DriverManager.getConnection(createRdbUrl(), properties);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }


    public void createConnection() {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", user);
            properties.setProperty("password", pwd);
            properties.setProperty("reWriteBatchedInserts", "true");
            connection = DriverManager.getConnection(createUrl(), properties);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }


    public TCapt(String[] args) {
	try {
		loadProps(args);
      	}  catch (Exception ex) {
               LOGGERT.error( ex.getMessage());
        }
     }

    public void stopRunning()
    {
	running.set(false);
	LOGGERT.info(  mythread+":  Stop Running");
    }
	
    public void interrupt() {
        running.set(false);
        Thread.currentThread().interrupt();
	LOGGERT.info(  mythread+":  Interrupting");

    }

   public void stop() {
        running.set(false);
    }

   boolean isRunning() {
        return running.get();
    }

   public String getMyThread() {
	return mythread;
  } 

    public void shutDown()
    {
        running.set(false);
        LOGGERT.info(  mythread+":  Shutting Down");
    }


 public void inCaseOfException() throws SQLException {
 String qerror;
  Connection connectionRdbE;
  qerror = "update _rdb_bdr.tc_process set n_operation ='shutdown' , n_dateop= now() where n_pid= " +threadn + " and n_mstr = '" +nodemst+ "' and  n_type = 'M';" ;
     try {
         Properties properties = new Properties();
         properties.setProperty("user", ruser);
         properties.setProperty("password", rpwd);
         properties.setProperty("reWriteBatchedInserts", "true");

         connectionRdbE = DriverManager.getConnection(createRdbUrl(), properties);
         Statement str = connectionRdbE.createStatement();
         str.execute(qerror);
     } catch (SQLException ex) {
         ex.printStackTrace();
     }

   }
  

    public void run()  {
	mythread = "TC-"+ nodemst  +"_"+ threadn;;
	Thread.currentThread().setName("TC-"+ nodemst );
   
        LOGGERT.info(  mythread+":  is in running state");

	running.set(true);
        createConnection();
        createConnectionRdb();
        try {
            openReplicationConnection();
            currentlsnfunc= setCurrentLSNFunction();

            LOGGERT.trace(  mythread+":  set currentlsn:"+currentlsnfunc);

  	    receiveChangesOccursBeforTCapt();
	 } catch (InterruptedException e) {
            e.printStackTrace();
                 try {
                        inCaseOfException();
                } catch(SQLException sqlee) {
                        sqlee.printStackTrace();
                }

        } catch (SQLException e) {
            e.printStackTrace();
                 try {
                        inCaseOfException();
                } catch(SQLException sqlee) {
                        sqlee.printStackTrace();
                }

        } catch (TimeoutException e) {
            e.printStackTrace();
                 try {
                        inCaseOfException();
                } catch(SQLException sqlee) {
                                sqlee.printStackTrace();
                        }

        } catch (Exception e) {
            e.printStackTrace();
	                 try {
                        inCaseOfException();
                } catch(SQLException sqlee) {
                                sqlee.printStackTrace();
                        }

        }
		finally {
    	       	try {
        		if(connection != null)
            			connection.close();
			if(connectionRdb != null)
                                connectionRdb.close();
			if(replicationConnection!= null)
                                replicationConnection.close();
    			} catch(SQLException sqlee) {
        			sqlee.printStackTrace();
    			} finally {  // Just to make sure that both con and stat are "garbage collected"
        			connection = null;
        			connectionRdb = null;
        			replicationConnection = null;
    			}
		}

  }




    //public boolean isXidCurrentinWal(Connection connection, int itxid)
    public boolean isXidCurrentinWal(Connection connectionRdb, long itxid)
            throws SQLException {
        // try (PreparedStatement preparedStatement = connection.prepareStatement("select active from pg_replication_slots where slot_name = ?")){
        LOGGERT.trace(  mythread+":" + "<< Check >> : select 1 from " + walq + "_xid where xid_current =" + itxid);
        try (PreparedStatement preparedStatement = connectionRdb.prepareStatement("select 1 from " + walq + "_xid where xid_current = ?")) {
            preparedStatement.setLong(1, itxid);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                // return rs.next() && rs.getBoolean(1);
                return rs.next();
            }
        }
    }



    public boolean checkFilt(Connection connection, String aquery)
            throws SQLException {
        String v_opdml="";
        String v_schemaf="";
        String v_tablef="";
        String qschma = "";
        String qschema = "";
        String qtable = "";
        String parz = "";

        v_opdml = aquery.substring(0, 1);
        //SELECT  substring(rlcMena.data,1,1) into v_opdml;
        LOGGERT.trace( mythread+":" +  "checkFilt on  " + aquery);
        LOGGERT.trace(  mythread+":" + "v_opdml is  " + v_opdml);
        //  schema
        switch(v_opdml) {
            case "D":
                parz = aquery.substring(12,(aquery.indexOf("WHERE")-1 ));
                LOGGERT.trace(  mythread+":" + "parz is  #" + parz+ "#");
                qschema = parz.substring(0,parz.indexOf("."));
                LOGGERT.trace(  mythread+":" + "qschema is  #" + qschema+"#");
                qtable = parz.substring(parz.indexOf(".")+1);
                LOGGERT.trace(  mythread+":" + "qtable is  #" + qtable+"#");
                break;
            case "I":
                parz = aquery.substring(12,(aquery.indexOf("(")-1 ));
                LOGGERT.trace(  mythread+":" + "parz is  #" + parz+ "#");
                qschema = parz.substring(0,parz.indexOf("."));
                LOGGERT.trace(  mythread+":" + "qschema is  #" + qschema+"#");
                qtable = parz.substring(parz.indexOf(".")+1);
                LOGGERT.trace(  mythread+":" + "qtable is  #" + qtable+"#");
                break;
            case "U":
                parz = aquery.substring(7,(aquery.indexOf("SET")-1 ));
                LOGGERT.trace(  mythread+":" + "parz is  #" + parz+ "#");
                qschema = parz.substring(0,parz.indexOf("."));
                LOGGERT.trace(  mythread+":" + "qschema is  #" + qschema+"#");
                qtable = parz.substring(parz.indexOf(".")+1);
                LOGGERT.trace(  mythread+":" + "qtable is  #" + qtable+"#");
                break;
        }

/*
select 1 from _rdb_bdr.walq__enodt_filtro where schemaf = 'public' and (tablef = 'tstlastdoc'  or tablef='all') and strpos(opdml, 'I' ) > 0
union
select 1 from _rdb_bdr.walq__enodt_filtro where '__events_ddl' = '__events_ddl' and 'dba' ='dba';
 */
        try (PreparedStatement preparedStatement = connection.prepareStatement("select 1 from " + walq +
                "_filtro where schemaf = ? and(tablef = ? or tablef='all') and strpos(opdml, ? ) > 0   UNION " +
                "Select 1 from " + walq +"_filtro where '__events_ddl' =  ? and 'dba' =? ")) {
            preparedStatement.setString(1, qschema);
            preparedStatement.setString(2, qtable);
            preparedStatement.setString(3, v_opdml);
            preparedStatement.setString(4, qtable);
            preparedStatement.setString(5, qschema);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                 return rs.next() && rs.getBoolean(1);
            }
        }
    }


    public void receiveChangesOccursBeforTCapt() throws Exception {
        PGConnection pgConnection = (PGConnection) replicationConnection;

        LogSequenceNumber lsn_cur = getCurrentLSN();
        String query;
        String myquery;
        String myquerycut;
        String querychk;
        long Txid;
        //String BoolTxid;
        long BoolTxid;
        long Txidbef;
        long txidstart = 0;
        String mylsn;
        String qlsn;
        int mydmlpos;
        int scanned;
        int scannedX;
        StringBuilder sb = new StringBuilder();
        String RdbSlotBdr = "rdb_" + node + "_bdr";
        LogSequenceNumber lsn = null;
        Statement stp = connection.createStatement();
        //PreparedStatement ps = conn.prepareStatement(query);
        Statement st = connectionRdb.createStatement();
        PreparedStatement preparedStatementInsert = null;


	try {

        /* check restart lsn dalla coda e skip  */
        querychk = " select lsn::varchar,xid from " + walq + " where wid = (select max(wid) from " + walq + " )";
        // LOGGERT.trace(  mythread+":" + "<< Check lsn restart >> :" + querychk);
        ResultSet rs = st.executeQuery(querychk);
        if (rs.next()) {
            //lsn =  rs.getObject(1).asString(); LogSequenceNumber.valueOf(
            String slsn = rs.getString(1);
            lsn = LogSequenceNumber.valueOf(slsn);
            txidstart = rs.getLong(2);


            LOGGERT.trace(  mythread+":" + "Queue XID at restart: " + txidstart);

        } else {
            lsn = getSlotLSN(RdbSlotBdr);
            LOGGERT.trace(  mythread+":" + "Slot LSN Position : " + lsn);
        }

        rs.close();


        PGReplicationStream stream =
                pgConnection
                        .getReplicationAPI()
                        .replicationStream()
                        .logical()
                        .withSlotName(RdbSlotBdr)
                        .withStartPosition(lsn)
                        //    .withSlotOption("proto_version",1)
                        //    .withSlotOption("publication_names", "pub1")
                        //  .withSlotOption("include-xids", true)
                        // .withSlotOption("include_transaction", true)
                        //    .withSlotOption("skip-empty-xacts", true)
                        .withStatusInterval(1, TimeUnit.SECONDS)
                        .start();
        ByteBuffer buffer;
        Txidbef = 0;
        scanned = 0;
        scannedX = 1;
        boolean TxisOpen = false;
        boolean bufferIsNull = false;
        BoolTxid = -1;


        LOGGERT.trace(  mythread+":" + "Xlog Position: " + lsn );
        LOGGERT.trace(  mythread+":" + "Xlog Current : " + lsn_cur);
        LOGGERT.trace(  mythread+":" + "Slot Name: " + RdbSlotBdr);


        /* check  lsn Start Position is gt lsn riletto dalla coda  */

        while (true) {
	    mylsn = stream.getLastReceiveLSN().asString();
            qlsn = lsn.asString();
            LOGGERT.trace(  mythread+":" + " Check LSn from stream > LSn from queue  " + mylsn + " vs " + qlsn);
            if (mylsn.equals(qlsn)) {
                LOGGERT.trace(  mythread+":" + "LESS - stay in the loop" + mylsn + "-" + qlsn);

                stream.readPending();
                //stream.setAppliedLSN(stream.getLastReceiveLSN());
                //stream.setFlushedLSN(stream.getLastReceiveLSN());
                TimeUnit.MILLISECONDS.sleep(100L);
                continue;
            } else {
                LOGGERT.trace(  mythread+":" + " ok go on :" + mylsn + "-" + qlsn);
                break;
            }
        }


     //   while (true) {
	 while (running.get()) {
         try {

            Txid = 0;
            //BoolTxid = "false0";
            buffer = stream.readPending();

             if (buffer == null) {
                 LOGGERT.trace(  mythread+":" + "buffer is null ");

  stp.execute("INSERT INTO dba.__events_ddl (ddl_id, wal_lsn, wal_txid, ddl_user, ddl_object,ddl_type,ddl_command,creation_timestamp)VALUES(-1,"+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +",txid_current(),CURRENT_USER,'NO_ACTIVITY_LSN_ACK','DML','UPSERT', NOW()) ON CONFLICT (ddl_id,ddl_origin) DO UPDATE SET  wal_lsn="+ currentlsnfunc +", wal_txid = txid_current(), creation_timestamp=NOW()");


                 if (!TxisOpen || bufferIsNull ) {
                     LOGGERT.trace(  mythread+":" + "buffer is null:"+bufferIsNull +" - sleep 1000L - TxisOpen:"+TxisOpen);
                      TimeUnit.MILLISECONDS.sleep(1000L);


                 }
                 LOGGERT.trace(  mythread+":" + "buffer is null:"+bufferIsNull +" - sleep 100L - TxisOpen:"+TxisOpen);
                 TimeUnit.MILLISECONDS.sleep(100L);
                 bufferIsNull = true;
                 continue;
             } else {
                 bufferIsNull = false;
             }


            myquerycut = "";
            myquery = toString(buffer);
            mydmlpos = myquery.indexOf("#");
            Txid = Long.parseLong(myquery.substring(0, mydmlpos));
	   		
               // myquerycut = myquery.substring(mydmlpos + 1);
                myquerycut =  myquery.substring(mydmlpos + 1, myquery.length() -1 );

             LOGGERT.trace(  mythread+":" + "----------------------------------------------------------------------------------------------------------------------------------------------");
                LOGGERT.trace(  mythread+":" + "<Begin Txid> :" + Txid );


            if (!TxisOpen) {
                Txidbef = Txid;
            }


            if (Txid != BoolTxid && Txid != txidstart) {

                LOGGERT.trace(  mythread+":" + "<< Debug >>  line:" + myquery + " Txid: " + Txid );

                if (TxisOpen && Txid != Txidbef) {
                    int[] inserted = st.executeBatch();

                    //07-05 if (scanned % batch_size == 0) {
                    LOGGERT.info ( mythread+":" + "Scanned#3:" + scanned );
                    LOGGERT.trace(  mythread+":" + ".......................................>> Commit#3 >> - Txid:" + Txid + " - Txidbef:" + Txidbef + "scanned:" + scanned );
                    LOGGERT.info(  mythread+":" + "Managing xid " + Txidbef );
                    connectionRdb.commit();


                    scanned = scanned + 1;
                    scannedX = scannedX + 1;
                    Txidbef = Txid;
                    TxisOpen = false;
                }

		        //if (myquerycut.contains("dba.__events_ddl") || myquerycut.contains(".pg_temp_")){
                if ( myquerycut.contains(".pg_temp_") ){

                    LOGGERT.trace(  mythread+":" + "<< Skip >> walq DML :" + myquery);
                
		    /* Imposto BoolTxid per lo skip alla prossima line con txid uguale */
                    BoolTxid = Txid;
                    LOGGERT.trace(  mythread+":" + " Imposto BoolTxid per lo skip alla prossima line con txid uguale :" + BoolTxid + " txid: " + Txid);
                } else {


                     Txidbef = Txid;

                    if (!TxisOpen && isXidCurrentinWal(connectionRdb, Txid)) {
                        LOGGERT.trace(  mythread+":" + "<< Checked >> : walqueue_xid table contain xid_current " + Txid + " and xid_from_queue is not null");
                        /* Imposto BoolTxid per lo skip alla prossima line con txid uguale */
                        BoolTxid = Txid;
                        LOGGERT.trace(  mythread+":" + " Imposto BoolTxid per lo skip alla prossima line con txid uguale :" + BoolTxid + " txid: " + Txid);

                    } else {
                        if (!filter || checkFilt(connectionRdb,myquerycut)) {
                            //LOGGERT.trace(  mythread+":" + "Filter is " + filter +" or if filter true then  checkFilt match ");
                            LOGGERT.trace(  mythread+":" + "Filter is " + filter);

                        connectionRdb.setAutoCommit(false);
                        TxisOpen = true;
			 mylsn = stream.getLastReceiveLSN().asString();

                        query = "insert into " + walq + " (wid,lsn,xid,current_xid,data) values  ( nextval('" + walq + "_wid_seq'::regclass),'" + mylsn + "', " + Txid + ", txid_current(),'" + myquerycut.replaceAll("'", "''") + "--" + node + "')";
                        st.addBatch(query);

                        } else {
                        	LOGGERT.trace(  mythread+":" + "Filter NOT match");
                        }
                    }
                    rs.close();

                    // LOGGERT.trace("Waiting some DML ..");
                }
            }  // endif BoolTxid check
            stream.setAppliedLSN(stream.getLastReceiveLSN());
            stream.setFlushedLSN(stream.getLastReceiveLSN());
             //23022021
             stream.forceUpdateStatus();

	    } catch (InterruptedException e){
                Thread.currentThread().interrupt();
		 LOGGERT.trace("  Thread was interrupted, Failed to complete operation:"+ mythread);
                     try {
                         inCaseOfException();
                     } catch(SQLException sqlee) {
                         sqlee.printStackTrace();
                     }
            }
        }
        //  st.close();
		} catch (SQLException e) {
			   e.printStackTrace();
                try {
                    inCaseOfException();
                } catch(SQLException sqlee) {
                    sqlee.printStackTrace();
                }

		}
		finally {
                try {
			if(stp != null)
                                stp.close();

                        if(st != null)
                                st.close();
                        } catch(SQLException sqlee) {
                                sqlee.printStackTrace();
                        } finally {  // Just to make sure that both con and stat are "garbage collected"
                                st = null;
                                stp = null;
                        }
                }

    }


    //select restart_lsn from pg_replication_slots where slot_name ='rdb_cina_bdr';
    private LogSequenceNumber getSlotLSN(String slotName) throws SQLException {
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("select restart_lsn from pg_replication_slots where slot_name ='" + slotName + "'")) {
                if (rs.next()) {
                    String lsn = rs.getString(1);
                    return LogSequenceNumber.valueOf(lsn);
                } else {
                    return LogSequenceNumber.INVALID_LSN;
                }
            }
        }
    }



    private String setCurrentLSNFunction() throws SQLException {
        try (Statement st = connection.createStatement()) {
                    if (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10)) {
                        return "pg_current_wal_lsn()";
                    } else {
                        return "pg_current_xlog_location()";
                    }
        }
    }


    private LogSequenceNumber getCurrentLSN() throws SQLException {
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("select "
                    + (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10)
                    ? "pg_current_wal_lsn()" : "pg_current_xlog_location()"))) {

                if (rs.next()) {
                    String lsn = rs.getString(1);
                    return LogSequenceNumber.valueOf(lsn);
                } else {
                    return LogSequenceNumber.INVALID_LSN;
                }
            }
        }
    }

    private void openReplicationConnection() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", pwd);
        properties.setProperty("reWriteBatchedInserts", "true");
        PGProperty.ASSUME_MIN_SERVER_VERSION.set(properties, "9.4");
        PGProperty.REPLICATION.set(properties, "database");
        PGProperty.PREFER_QUERY_MODE.set(properties, "simple");
        replicationConnection = DriverManager.getConnection(createUrl(), properties);
    }


   public static boolean isNullOrEmpty(String myString)
    {
         return myString == null || "".equals(myString);
    }

    public void loadProps(String[] args)   throws Exception  {
	String rdbbdr = System.getenv("RDBBDR");

	Options options = new Options();

        Option nodemstr = new Option("n", "nodemstr", true, "Please set desidered node master");
        nodemstr.setRequired(true);
        options.addOption(nodemstr);

	Option threadnum =  new Option("tn", "threadnum", true, "Please set thread number");
        threadnum.setRequired(false);
        options.addOption(threadnum);


	CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

	try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
	    LOGGERT.error(e.getMessage());
            formatter.printHelp("TCapt", options);
	     isInitialized = false ;
        }

        nodemst= cmd.getOptionValue("nodemstr");
	threadn = Integer.parseInt(cmd.getOptionValue("threadnum"));


	LOGGERT.info("Running TCapt for node :" + nodemst);


        if (isNullOrEmpty(rdbbdr))
        {
                  LOGGERT.error("RDBBDR variable should be set ");
		  isInitialized = false ;
        }

	if (isNullOrEmpty(nodemst))
	{
                  LOGGERT.error("NODEMST variable should be set ");
 		isInitialized = false ;
	}

        String rdbbdr_conf = rdbbdr + "/conf/" + nodemst + "_rdb_bdr.conf";

	if(!Files.isRegularFile(Paths.get(rdbbdr_conf))) {
		  LOGGERT.error(rdbbdr_conf + " not exists! ");
 		isInitialized = false ;
	}


        try (InputStream input = new FileInputStream(rdbbdr_conf)) {

            Properties prop = new Properties();

            prop.load(input);


            LOGGERT.trace("Configuration file: " +  rdbbdr_conf + " review");
            LOGGERT.trace("Primary Database: ");
            LOGGERT.trace("db " + prop.getProperty("db"));
            LOGGERT.trace("user " + prop.getProperty("user"));
            LOGGERT.trace("pwd " + prop.getProperty("pwd"));
            LOGGERT.trace("node " + prop.getProperty("node"));
            LOGGERT.trace("host " + prop.getProperty("host"));
	    LOGGERT.trace("");
            LOGGERT.trace("RDB database: ");
            LOGGERT.trace("rdb " + prop.getProperty("rdb"));
            LOGGERT.trace("ruser " + prop.getProperty("ruser"));
            LOGGERT.trace("rpwd " + prop.getProperty("rpwd"));
            LOGGERT.trace("rnode " + prop.getProperty("rnode"));
            LOGGERT.trace("rhost " + prop.getProperty("rhost"));
            LOGGERT.trace("walqtrunc " + prop.getProperty("walqtrunc"));
            LOGGERT.trace("batch_size " + prop.getProperty("batch_size"));
            LOGGERT.trace("filter " + prop.getProperty("filter"));
	    LOGGERT.trace("");

            host = prop.getProperty("host");
            user = prop.getProperty("user");
            port = Integer.valueOf(prop.getProperty("port"));
            pwd = prop.getProperty("pwd");
            db = prop.getProperty("db");
            node = prop.getProperty("node");
            walq = "_rdb_bdr.walq__" + node;
            LOGGERT.trace("walq " + walq);

            rhost = prop.getProperty("rhost");
            ruser = prop.getProperty("ruser");
            rport = Integer.valueOf(prop.getProperty("rport"));
            rpwd = prop.getProperty("rpwd");
            rdb = prop.getProperty("rdb");
		
            walqtrunc = Integer.valueOf(prop.getProperty("walqtrunc"));
            batch_size = Integer.valueOf(prop.getProperty("batch_size"));
            filter = Boolean.valueOf(prop.getProperty("filter"));


        } catch (IOException ex) {
	 	isInitialized = false ;
            ex.printStackTrace();
        }

    }



    public static void main(String[] args) {
        String pluginName = "rdblogdec";

        TCapt app = new TCapt(args);
	try {
        	app.loadProps(args);
      	}  catch (Exception ex) {
                LOGGERT.error( ex.getMessage());
        }

	 Thread t1 = new Thread(app);
	 t1.start();

    }
}



