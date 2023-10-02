/**
    *  Copyright (c) 2022-2023
    *  Silvio Brandani <mktg.tcapture@gmail.com>. All rights reserved.
 **/


package com.edslab;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.ServerVersion;

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
 * Logical Decoding TCoor
 */

public class TCoor implements Runnable {


    private AtomicBoolean running = new AtomicBoolean(false);
    private String mythread = "";
    private static final Logger LOGGERM= LogManager.getLogger(TCoor.class.getName());

    private Connection connection;
    private Connection connectionRdb;
    private Connection connectionSRdb;
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
    int walqtrunc_min = 0;
    int walqtrunc_max = 0;
    int batch_size = 1000;
    private String loglevel = "";
    private boolean filter = false;
    private String currentlsnfunc = "";


    private String  shost		 = "";
 private String  suser       = "";
 private int  	 sport       = 0;
 private String  spwd        = "";
 private String  sdb 		 = "";
 private String  snode       = "";
 private String  swalq       = "";
 private String  srhost       = "";
 private String  sruser       = "";
 private int  	 srport       = 0;
 private String  srpwd       = "";
 private String  srdb        = "";
 private int  swalqtrunc  = 0;
 private int  sbatch_size = 0;
 private boolean  sfilter 	 = false;
 private boolean slog_hist 	 = false;

  public boolean isInitialized = true;


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


private String createSRdbUrl() {
        return "jdbc:postgresql://" + srhost + ':' + srport + '/' + srdb;
    }


 public void createConnectionSRdb() {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", sruser);
            properties.setProperty("password", srpwd);
            properties.setProperty("reWriteBatchedInserts", "true");

            connectionSRdb = DriverManager.getConnection(createSRdbUrl(), properties);
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


    public TCoor(String[] args) {
	try {
		loadProps(args);
	}  catch (Exception ex) {
                LOGGERM.error( ex.getMessage());
        }
     }

    public void stopRunning()
    {
	running.set(false);
	LOGGERM.info(  mythread+":  Stop Running");
    }
	
    public void interrupt() {
        running.set(false);
        Thread.currentThread().interrupt();
	LOGGERM.info(  mythread+":  Interrupting");

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
        LOGGERM.info(  mythread+":  Shutting Down");
    }


 public void inCaseOfException()  throws SQLException {
 Statement str = connectionRdb.createStatement();
 String qerror;
  qerror = "update _rdb_bdr.tc_process set n_state ='down' , n_dateop= now() where n_pid= " +threadn + " and n_mstr = '" +nodemst+ "' and  n_type = 'H';" ;
        try {
         str.execute(qerror);
           }
	catch (SQLException e) {
            e.printStackTrace();
        }
   }
  
 public boolean isSlotActive(Connection connection, int itxid)
            throws SQLException {
       
        LOGGERM.trace(  mythread+":" + "<< Check slot :>> " + nodemst);
        try (PreparedStatement preparedStatement = connection.prepareStatement("select active from pg_replication_slots where slot_name ='rdb_" + nodemst+ "_bdr'")) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                
                return rs.next();
            }
        }
    }

    public void run()  {
	mythread = "TO-"+ nodemst  +"_"+ threadn;;
	Thread.currentThread().setName("TO-"+ nodemst );
   
        LOGGERM.info(  mythread+"  is in running state");

	 running.set(true);
        createConnection();
        createConnectionRdb();
        if (!isServerCompatible()) {
             LOGGERM.error("must have server version greater than 9.4");
            System.exit(-1);
        }
        try {
            currentlsnfunc= setCurrentLSNFunction();
            LOGGERM.trace(  mythread+":  set currentlsn:"+currentlsnfunc);


            iterateCoor();

        } catch (InterruptedException e) {
		try {
                        inCaseOfException();
                 } catch(SQLException sqlee) {
                        sqlee.printStackTrace();
                  }

            e.printStackTrace();
        } catch (SQLException e) {
		try {
		 	inCaseOfException();
		 } catch(SQLException sqlee) {
		        sqlee.printStackTrace();
                  } 
            e.printStackTrace();
        } catch (TimeoutException e) {
	    try {
                        inCaseOfException();
                 } catch(SQLException sqlee) {
                        sqlee.printStackTrace();
                  }
            e.printStackTrace();
        } catch (Exception e) {
		try {
                        inCaseOfException();
                 } catch(SQLException sqlee) {
                        sqlee.printStackTrace();
                  }
            e.printStackTrace();
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



    public  int refreshCoorPast() throws SQLException, Exception {
        int scanned = 0;

        LOGGERM.info( mythread+":" +"Coordinate producer " + nodemst + " past");
        if (checkMasterLsnPast(true,nodemst)) {


        } else {
            LOGGERM.info( mythread+":" +"Coordinate producer " + nodemst+ " not need to manage LSN table partitions");
        }


        try (PreparedStatement preparedStatement =  connectionRdb.prepareStatement("select substr(slot_name, length('"+nodemst+"_publ_')+1) from pg_replication_slots where slot_name like '"+nodemst+"%' and  slot_name not like '"+nodemst+"%sync%'")) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    LOGGERM.info( mythread+":" +"Coordinate remote consumer " + rs.getString(1)  + " for  producer " +  nodemst );
                    scanned = scanned + 1;
                    if  (checkMasterLsnPast(false,(rs.getString(1) )) ) {

                    } else {
                        LOGGERM.info( mythread+":" +"Coordinate producer remote consumer " + rs.getString(1)  + " for  producer " +  nodemst+ " not need to manage LSN table partitions");
                    }

                }
            }
        }


        return  scanned ;
    }


    public boolean checkMasterLsnPast(boolean isMaster,String aSlave) throws SQLException, Exception  {
        Statement str = connection.createStatement();
        String qq,qe;
        boolean missingLSN=false;


        if (!isMaster) {
            qq = "\n" +
                    "select \n" +
                    " upper(to_hex(\n" +
                    "  generate_series(\n" +
                    "_rdb_bdr.hex2dec(\n" +
                    "min(substring(slave_lsn::text,1,position('/' in slave_lsn::text)-1 ) )  )::int -"+ walqtrunc_max +"," +
                    "_rdb_bdr.hex2dec(\n" +
                    " min(substring(slave_lsn::text,1,position('/' in slave_lsn::text)-1 ) )  )::int -"+ walqtrunc_min +") ) )" +
                    " from ( select q_lsn  as slave_lsn from _rdb_bdr.tc_monit where n_mstr='" + nodemst + "' and n_slv='" + aSlave + "' union select wal_lsn as slave_lsn from _rdb_bdr.tc_monit \n" +
                    " where n_mstr='" + nodemst + "' and n_slv='" + aSlave + "') q  ";
        } else {
            qq = "select \n" +
                    " upper(to_hex(\n" +
                    "  generate_series(\n" +
                    "_rdb_bdr.hex2dec(\n" +
                    " min(substring(q_lsn::text,1,position('/' in q_lsn::text)-1 ) )  )::int -"+ walqtrunc_max +"," +
                    "_rdb_bdr.hex2dec(\n" +
                    " min(substring(q_lsn::text,1,position('/' in q_lsn::text)-1 ) )  )::int -"+ walqtrunc_min +") ) )" +
                    "  from _rdb_bdr.tc_monit where n_mstr='"+nodemst +"'  ";
        }

        try {

            PreparedStatement sps = connectionRdb.prepareStatement(qq);
            ResultSet srs = sps.executeQuery();
            if (!isMaster) {
                loadSlaveProps(aSlave);
                createConnectionSRdb();
            }
            while  (srs.next()) {
                srs.getString(1);
                String qq2= " SELECT EXISTS(select 1  from pg_class where relname=lower('walq__"+nodemst +"_"+srs.getString(1)+"') )";

                if (!isMaster) {

                    try (PreparedStatement preparedStatement = connectionSRdb.prepareStatement(qq2)) {
                        try (ResultSet rs = preparedStatement.executeQuery()) {
                            if (null != rs && rs.next())
                                if (  !rs.getBoolean(1)){
                                    LOGGERM.info( mythread+":" +"Coordinate consumer " +aSlave+ " slave of "+ nodemst +" - Past WAL LSN:" + srs.getString(1) +"- NOT EXIST" );
                                } else {
                                    LOGGERM.info( mythread+":" +"Coordinate consumer " +aSlave+ " slave of "+ nodemst +" - Past WAL LSN:" + srs.getString(1) +"-  EXIST" );

                                    try {
                                        Statement st = connectionSRdb.createStatement();
                                        missingLSN=true;
                                        st.execute("DROP TABLE iF EXISTS  _rdb_bdr.walq__"+nodemst +"_"+srs.getString(1)+" ");

                                        st.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                }
                        }
                    }



                } else {

                    try (PreparedStatement preparedStatement = connectionRdb.prepareStatement(qq2)) {
                        try (ResultSet rs = preparedStatement.executeQuery()) {
                            if (null != rs && rs.next())
                                if (!rs.getBoolean(1)) {
                                    LOGGERM.info(mythread + ":" + "Coordinate producer " + nodemst + " - Past WAL LSN:" + srs.getString(1) + "- NOT EXIST");
                                } else {
                                    LOGGERM.info(mythread + ":" + "Coordinate producer " + nodemst + " - Past WAL LSN:" + srs.getString(1) + "-  EXIST");


                                    try {
                                        Statement st = connectionRdb.createStatement();
                                        missingLSN = true;
                                        st.execute("DROP TABLE iF  EXISTS _rdb_bdr.walq__" + nodemst + "_" + srs.getString(1)  );
                                        st.execute("DROP TABLE iF EXISTS  _rdb_bdr.walq__"+nodemst +"_xid_"+srs.getString(1)+" ");
                                        st.close();
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                }
                        }
                    }


                } /* END else */
            }
            if (missingLSN && !isMaster) {
                try {
                    Statement st = connectionSRdb.createStatement();
                    LOGGERM.info( mythread+":" +"Coordinate consumer " +aSlave+ " slave of "+ nodemst +" - Refresh publication " );

                    st.execute("alter subscription "+aSlave+ "_subs_"+ nodemst  + " refresh publication ");
                    st.close();
                    TimeUnit.MILLISECONDS.sleep(5000L);
                } catch (SQLException e) {
                    e.printStackTrace();
                }   finally {
                    if (connectionSRdb != null) {
                        srs.close();
                        sps.close();
                        connectionSRdb.close();
                        connectionSRdb = null;
                    }
                }
            }

            try {

                if (connectionSRdb != null) {
                    LOGGERM.debug(mythread + ":" + "Slave:" + aSlave + " - connectionSRdb still open, closing ...");
                    connectionSRdb.close();
                } else {
                    LOGGERM.debug( mythread+":" +"Slave:"+aSlave+ " - connectionSRdb closed!"  );
                }
            }  catch (SQLException e) {
                e.printStackTrace();
            }

        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return missingLSN;
    }



    public  int refreshCoor() throws SQLException, Exception {
        int scanned = 0;

        LOGGERM.info( mythread+":" +"Coordinate producer " + nodemst);
	    if (checkMasterLsn(true,nodemst)) {


        } else {
            LOGGERM.info( mythread+":" +"Coordinate producer " + nodemst+ " not need to manage LSN table partitions");
        }


    try (PreparedStatement preparedStatement =  connectionRdb.prepareStatement("select substr(slot_name, length('"+nodemst+"_publ_')+1) from pg_replication_slots where slot_name like '"+nodemst+"%' and  slot_name not like '"+nodemst+"%sync%'")) {
        try (ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                LOGGERM.info( mythread+":" +"Coordinate remote consumer " + rs.getString(1)  + " for  producer " +  nodemst );
                scanned = scanned + 1;
                if  (checkMasterLsn(false,(rs.getString(1) )) ) {

                } else {
                    LOGGERM.info( mythread+":" +"Coordinate producer remote consumer " + rs.getString(1)  + " for  producer " +  nodemst+ " not need to manage LSN table partitions");
                }

            }
        }
    }


    return  scanned ;
    }


public boolean checkMasterLsn(boolean isMaster,String aSlave) throws SQLException, Exception  {
	Statement str = connection.createStatement();
    String qq,qe;
    boolean missingLSN=false;
	/* qq =  "select substring(pg_current_wal_lsn()::text,1,position('/' in pg_current_wal_lsn()::text)-1 )";

	 */
	qq = "select upper(to_hex(\n" +
            "generate_series(dba.hex2dec(\n" +
            " substring("+currentlsnfunc+"::text,1,position('/' in "+currentlsnfunc+"::text)-1 ) ),\n" +
            "dba.hex2dec(\n" +
            " substring("+currentlsnfunc+"::text,1,position('/' in "+currentlsnfunc+"::text)-1 ) ) +2 )\n" +
            "));";
    try {
        PreparedStatement sps = connection.prepareStatement(qq);
        ResultSet srs = sps.executeQuery();
        if (!isMaster) {
            loadSlaveProps(aSlave);
            createConnectionSRdb();
        }
        while  (srs.next()) {
           /* LOGGERM.info( mythread+":" +"Coordinate producer " + nodemst +" - Current WAL LSN:" + srs.getString(1) );
            */
            srs.getString(1);

            String qq2= " SELECT EXISTS(select 1  from pg_class where relname=lower('walq__"+nodemst +"_"+srs.getString(1)+"')) ";

            if (!isMaster) {

                try (PreparedStatement preparedStatement = connectionSRdb.prepareStatement(qq2)) {
                    try (ResultSet rs = preparedStatement.executeQuery()) {
                        if (null != rs && rs.next())
                            if (  rs.getBoolean(1)){
                                LOGGERM.info( mythread+":" +"Coordinate consumer " +aSlave+ " slave of "+ nodemst +" - Current WAL LSN:" + srs.getString(1) +"- EXIST" );
                            } else {
                                LOGGERM.info( mythread+":" +"Coordinate consumer " +aSlave+ " slave of "+ nodemst +" - Current WAL LSN:" + srs.getString(1) +"- NOT EXIST" );

                                try {
                                    Statement st = connectionSRdb.createStatement();
                                    missingLSN=true;
                                    st.execute("CREATE TABLE iF NOT EXISTS  _rdb_bdr.walq__"+nodemst +"_"+srs.getString(1)+" partition of _rdb_bdr.walq__"+nodemst +" for values in ('"+srs.getString(1)+"')");
                                    st.execute("ALTER TABLE _rdb_bdr.walq__"+nodemst +"_"+srs.getString(1)+"  ADD CONSTRAINT  walq__"+nodemst +"_"+srs.getString(1)+"_pkey PRIMARY KEY (wid)");
                                    st.execute("CREATE INDEX walq__"+nodemst +"_"+srs.getString(1)+"_idx ON _rdb_bdr.walq__"+nodemst +"_"+srs.getString(1)+" USING btree (xid, lsn)");
                                    st.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                }



            } else {

                try (PreparedStatement preparedStatement = connectionRdb.prepareStatement(qq2)) {
                    try (ResultSet rs = preparedStatement.executeQuery()) {
                        if (null != rs && rs.next())
                            if (rs.getBoolean(1)) {
                                LOGGERM.info(mythread + ":" + "Coordinate producer " + nodemst + " - Current WAL LSN:" + srs.getString(1) + "- EXIST");
                            } else {
                                LOGGERM.info(mythread + ":" + "Coordinate producer " + nodemst + " - Current WAL LSN:" + srs.getString(1) + "- NOT EXIST");

                           /*String qq3 = "CREATE TABLE _rdb_bdr.walq__stg_"+nodemst +"_"+srs.getString(1)+" partition of _rdb_bdr.walq__stg for values in ('"+srs.getString(1)+"')";
                                    ALTER TABLE _rdb_bdr.walq__stg_287  ADD CONSTRAINT walq__stg_287_pkey PRIMARY KEY (wid);
                                    CREATE INDEX walq__stg_287_idx ON _rdb_bdr.walq__stg_287 USING btree (xid, lsn);
                                    alter publication stg_publ  add  table  _rdb_bdr.walq__stg_XXX
                            */
                                try {
                                    Statement st = connectionRdb.createStatement();
                                    missingLSN = true;
                                    st.execute("CREATE TABLE iF NOT EXISTS _rdb_bdr.walq__" + nodemst + "_" + srs.getString(1) + " partition of _rdb_bdr.walq__" + nodemst + " for values in ('" + srs.getString(1) + "')");
                                    st.execute("ALTER TABLE _rdb_bdr.walq__" + nodemst + "_" + srs.getString(1) + "  ADD CONSTRAINT  walq__" + nodemst + "_" + srs.getString(1) + "_pkey PRIMARY KEY (wid)");
                                    st.execute("CREATE INDEX walq__" + nodemst + "_" + srs.getString(1) + "_idx ON _rdb_bdr.walq__" + nodemst + "_" + srs.getString(1) + " USING btree (xid, lsn)");
                                    st.execute("ALTER PUBLICATION "+nodemst + "_publ add table _rdb_bdr.walq__" + nodemst + "_" + srs.getString(1) + " ");

                                    st.execute("CREATE TABLE iF NOT EXISTS _rdb_bdr.walq__" + nodemst + "_xid_" + srs.getString(1) + " partition of _rdb_bdr.walq__" + nodemst + "_xid for values in ('" + srs.getString(1) + "')");
                                    st.execute("ALTER TABLE _rdb_bdr.walq__" + nodemst + "_xid_" + srs.getString(1) + "  ADD CONSTRAINT  walq__" + nodemst + "_xid_" + srs.getString(1) + "_pkey PRIMARY KEY (xid_current)");


                                    st.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                }


            } /* END else */
        }
        if (missingLSN && !isMaster) {
            try {
                Statement st = connectionSRdb.createStatement();
                LOGGERM.info( mythread+":" +"Coordinate consumer " +aSlave+ " slave of "+ nodemst +" - Refresh publication " );

                st.execute("alter subscription "+aSlave+ "_subs_"+ nodemst  + " refresh publication ");
                st.close();
                TimeUnit.MILLISECONDS.sleep(5000L);
            } catch (SQLException e) {
                e.printStackTrace();
            }   finally {
                if (connectionSRdb != null) {
                    srs.close();
                    sps.close();
                    connectionSRdb.close();
                    connectionSRdb = null;
                }
            }
        }

        try {

            if (connectionSRdb != null) {
                LOGGERM.debug(mythread + ":" + "Slave:" + aSlave + " - connectionSRdb still open, closing ...");
                connectionSRdb.close();
            } else {
                LOGGERM.debug( mythread+":" +"Slave:"+aSlave+ " - connectionSRdb closed!"  );
            }
        }  catch (SQLException e) {
            e.printStackTrace();
        }

    }
    catch (SQLException e) {
        e.printStackTrace();
    }
    return missingLSN;
}


public void iterateCoor() throws Exception {

        int batchsize;
        int scanned = 0;
         int scanned_del = 0;
        batchsize = 0;

         while (running.get()) {
	try {

            	batchsize = batchsize + 1;
		scanned = scanned + refreshCoor();
        scanned_del = scanned_del + refreshCoorPast();
		LOGGERM.trace( mythread+":" +"<scanned: "+ scanned + " scanned deleted: "+scanned_del);
                TimeUnit.MILLISECONDS.sleep(10000L);
			
	 } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                LOGGERM.info("  Thread was interrupted, Failed to complete operation:"+ mythread);
          }
               
	 }
}



  public static boolean isNullOrEmpty(String myString)
   {
         return myString == null || "".equals(myString);
   }

 
 public synchronized void loadSlaveProps(String nodeslv) {

    	String rdbbdr = System.getenv("RDBBDR");
	String nodeslv_rdbbdr_conf = rdbbdr + "/conf/" + nodeslv + "_bdr_rdb.conf";				
  
   if(!Files.isRegularFile(Paths.get(nodeslv_rdbbdr_conf))) {
                 LOGGERM.error(nodeslv_rdbbdr_conf + " not exists! ");
                 System.exit(-1);
        }

    try (InputStream input = new FileInputStream(nodeslv_rdbbdr_conf)) {

            Properties prop = new Properties();

            prop.load(input);
            LOGGERM.info(mythread+ ": reading config file for consumer node "+nodeslv);

            
            shost = prop.getProperty("host");
            suser = prop.getProperty("user");
            sport = Integer.valueOf(prop.getProperty("port"));
            spwd = prop.getProperty("pwd");
            sdb = prop.getProperty("db");
            snode = prop.getProperty("node");
            swalq = "_rdb_bdr.walq__" + snode;

            srhost = prop.getProperty("rhost");
            sruser = prop.getProperty("ruser");
            srport = Integer.valueOf(prop.getProperty("rport"));
            srpwd = prop.getProperty("rpwd");
            srdb = prop.getProperty("rdb");

            swalqtrunc = Integer.valueOf(prop.getProperty("walqtrunc"));
            sbatch_size = Integer.valueOf(prop.getProperty("batch_size"));

            sfilter = Boolean.valueOf(prop.getProperty("filter"));
            slog_hist = Boolean.valueOf(prop.getProperty("log_hist"));
			
	LOGGERM.trace( mythread+":" +"Slave:" + shost+":"+ suser +":"+sport +":"+spwd +":"+sdb +":"+ snode+":"+swalq +":"+srhost +":"+ sruser+":"+srport +":"+ srpwd+":"+srdb+":"+swalqtrunc +":"+ sbatch_size+":"+sfilter +":"+slog_hist +":" );

	 } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
		


    public void loadProps(String[] args) throws Exception {
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
            LOGGERM.error(e.getMessage());
            formatter.printHelp("TCoor", options);
	    isInitialized = false ;
        }

        nodemst= cmd.getOptionValue("nodemstr");
	threadn = Integer.parseInt(cmd.getOptionValue("threadnum"));

	 LOGGERM.info("Running TCoor for node :" + nodemst);


        if (isNullOrEmpty(rdbbdr))
        {
                 LOGGERM.error("RDBBDR variable should be set ");
		isInitialized = false ;
        }

	if (isNullOrEmpty(nodemst))
	{
                 LOGGERM.error("NODEMST variable should be set ");
		isInitialized = false ;
	}

        String rdbbdr_conf = rdbbdr + "/conf/" + nodemst + "_rdb_bdr.conf";

	if(!Files.isRegularFile(Paths.get(rdbbdr_conf))) {
		 LOGGERM.error(rdbbdr_conf + " not exists! ");
		isInitialized = false ;
	}


        try (InputStream input = new FileInputStream(rdbbdr_conf)) {

            Properties prop = new Properties();

            prop.load(input);


            LOGGERM.trace("Configuration file: " +  rdbbdr_conf + " review");
            LOGGERM.trace("Primary Database: ");
            LOGGERM.trace("db " + prop.getProperty("db"));
            LOGGERM.trace("user " + prop.getProperty("user"));
            LOGGERM.trace("pwd " + prop.getProperty("pwd"));
            LOGGERM.trace("node " + prop.getProperty("node"));
            LOGGERM.trace("host " + prop.getProperty("host"));
	    LOGGERM.trace("");
            LOGGERM.trace("RDB database: ");
            LOGGERM.trace("rdb " + prop.getProperty("rdb"));
            LOGGERM.trace("ruser " + prop.getProperty("ruser"));
            LOGGERM.trace("rpwd " + prop.getProperty("rpwd"));
            LOGGERM.trace("rnode " + prop.getProperty("rnode"));
            LOGGERM.trace("rhost " + prop.getProperty("rhost"));
            LOGGERM.trace("walqtrunc " + prop.getProperty("walqtrunc"));
            LOGGERM.trace("batch_size " + prop.getProperty("batch_size"));
            LOGGERM.trace("filter " + prop.getProperty("filter"));
	    LOGGERM.trace("");

            host = prop.getProperty("host");
            user = prop.getProperty("user");
            port = Integer.valueOf(prop.getProperty("port"));
            pwd = prop.getProperty("pwd");
            db = prop.getProperty("db");
            node = prop.getProperty("node");
            walq = "_rdb_bdr.walq__" + node;
            LOGGERM.trace("walq " + walq);

            rhost = prop.getProperty("rhost");
            ruser = prop.getProperty("ruser");
            rport = Integer.valueOf(prop.getProperty("rport"));
            rpwd = prop.getProperty("rpwd");
            rdb = prop.getProperty("rdb");
    //Math.abs(-5)
            walqtrunc = Math.abs(Integer.valueOf(prop.getProperty("walqtrunc")));
            batch_size = Math.abs(Integer.valueOf(prop.getProperty("batch_size")));
            filter = Boolean.valueOf(prop.getProperty("filter"));
            if ( walqtrunc<3 ) {
                walqtrunc = 3;
            }
            walqtrunc_min = walqtrunc ;
            walqtrunc_max = walqtrunc + 2;

        } catch (IOException ex) {
		isInitialized = false ;
            ex.printStackTrace();
        }

    }


    private boolean isServerCompatible() {
        return ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v9_5);
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

    public static void main(String[] args) {

        TCoor app = new TCoor(args);

       try {
        	app.loadProps(args);
        }  catch (Exception ex) {
                LOGGERM.error( ex.getMessage());
        }



	 Thread t1 = new Thread(app);
	 t1.start();

    }
}



