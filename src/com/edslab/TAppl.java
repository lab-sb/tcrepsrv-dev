/**
 *   *  Copyright (c) 2022-2023
 *   *  Silvio Brandani <mktg.tcapture@gmail.com>. All rights reserved.
 *      */

package com.edslab;


import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.cli.*;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.ServerVersion;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


/**
 * Logical Decoding TAppl
*/

public class TAppl implements Runnable {

    private static final Logger LOGGERA= LogManager.getLogger(TAppl.class.getName());

    private AtomicBoolean running = new AtomicBoolean(false);
    private String mythread = "";
    private String host = "";
    private String user = "";
    private String pwd = "";
    private int port = 5432;
    private String db = "";
    private String node = "";
    private String nodemst = "";
    private String nodeslv = "";
    private int threadn = 0;
    private String walq = "";

    private String rhost = "";
    private String ruser = "";
    private String rpwd = "";
    int rport = 5432;
    private String rdb = "";
    int walqtrunc = 9;
    int batch_size = 1000;
    private String loglevel = "";
    private boolean filter = false;
    boolean log_hist = false;
    boolean isMaster = false;

    private Connection connection;
    private Connection connectionRdb;

    public boolean isInitialized = true;
    boolean isException=false;

    private static String toString(ByteBuffer buffer) {
        int offset = buffer.arrayOffset();
        byte[] source = buffer.array();
        int length = source.length - offset;

        return new String(source, offset, length);
    }


    private String createUrl() {
        return "jdbc:postgresql://" + rhost + ':' + rport + '/' + rdb;
    }


    public void createConnection() {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", ruser);
            properties.setProperty("password", rpwd);
            properties.setProperty("reWriteBatchedInserts", "true");
            connection = DriverManager.getConnection(createUrl(), properties);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    private String createRdbUrl() {
        // da redirezionare sul rdb rhost rport ruser rpwd quando i walq__nodemst saranno nel rdb__nodeslv
        return "jdbc:postgresql://" + host + ':' + port + '/' + db;
    }


    public void createConnectionRdb() {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", user);
            properties.setProperty("password", pwd);
            properties.setProperty("reWriteBatchedInserts", "true");

            connectionRdb = DriverManager.getConnection(createRdbUrl(), properties);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

   public TAppl (String[] args) {
	try {
	   loadProps(args);
 	}  catch (Exception ex) {
	        LOGGERA.error( ex.getMessage());
       	}
   }


    public void shutDown()
    {
        running.set(false);
        LOGGERA.info(  mythread+":  Shutting Down");
    }



     public void stopRunning()
    {
        running.set(false);
        LOGGERA.info(  mythread+":  Stop Running");
    }

    public void interrupt() {
        running.set(false);
        Thread.currentThread().interrupt();
        LOGGERA.info(  mythread+":  Interrupting");

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


    public void inCaseOfException() throws SQLException {
        String qerror;
        Connection connectionRdbE;
      //  qerror = "update _rdb_bdr.tc_process set n_state ='down' , n_dateop= now() where n_pid= " +threadn + " and n_mstr = '" +nodemst+ "' and  n_type = 'M';" ;
        qerror = "update _rdb_bdr.tc_process set n_operation ='shutdown' , n_dateop= now() where n_pid= " +threadn + " and n_mstr = '" +nodemst+ "' and  n_type = 'S';" ;

        try {
            Properties properties = new Properties();
            properties.setProperty("user", ruser);
            properties.setProperty("password", rpwd);
            properties.setProperty("reWriteBatchedInserts", "true");

            connectionRdbE = DriverManager.getConnection(createUrl(), properties);
            Statement str = connectionRdbE.createStatement();
            str.execute(qerror);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }


    public void inCaseOfExceptionWrong()   throws SQLException {
 String qerror;
       isException =true;
     if(connection != null) {
         connection.close();
         createConnection();
        // connection.setAutoCommit(false);
     }
  qerror = "update _rdb_bdr.tc_process set n_state ='down' , n_dateop= now() where n_pid= " +threadn + " and n_mstr = '" +nodemst+ "' and  n_type = 'S';" ;
        try {
 			Statement st = connection.createStatement();
         		st.execute(qerror);
           }
        catch (SQLException e) {
            e.printStackTrace();
        }
   }

    private boolean checkIsMaster(String primario) throws  Exception {
        //createConnection(rhost,rport,rdb, ruser, rpasswd);
        //System.out.println("check primary  exists:" + primary);
        String qq= " SELECT EXISTS(select 1 from _rdb_bdr.tc_process where n_type='M' ) ; ";
        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (null != rs && rs.next())

                    return  rs.getBoolean(1);
            }
        }

        return false;
    }

   public void run()  {  

   	mythread = "TA_"+ nodemst +"_"+ threadn; 
	Thread.currentThread().setName("TA-"+ nodemst );
	LOGGERA.info(mythread +" is in running state");
		
        running.set(true);

        try {

        createConnection();
        createConnectionRdb();
        isMaster = checkIsMaster(nodemst);
        runwal();

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
                        } catch(SQLException sqlee) {
                                sqlee.printStackTrace();
                        } finally {  // Just to make sure that both con and stat are "garbage collected"
                                connection = null;
                                connectionRdb = null;
                        }
                }
  }


    public synchronized int selectXid(Connection connection, int limit) 
            throws SQLException, Exception {
        LOGGERA.trace( mythread+":" +"<< Check >> :select xid from walq__" + nodemst);
	 int scanned = 0;
	 int ret_rows = 0;

         //BUG#101  try (PreparedStatement preparedStatement = connection.prepareStatement(" select distinct xid from walq__" + nodemst + " where xid >(select xid_offset from walq__" + nodemst + "_offset ) order by xid limit " + limit )) {
        //BUG#102  try (PreparedStatement preparedStatement = connection.prepareStatement(" select distinct xid from walq__" + nodemst + " where wid >(select last_offset from walq__" + nodemst + "_offset ) order by xid limit " + limit )) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(" select distinct xid,dateop from walq__" + nodemst + " where wid >(select last_offset from walq__" + nodemst + "_offset ) order by dateop limit " + limit )) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
		   LOGGERA.trace( mythread+":" +"<< Check >> :xid - get " + rs.getLong(1));
		        ret_rows = manageXid(rs.getLong(1) );
		        if (ret_rows == -1 ) {
                    scanned = -1;
		            break;
                } else
                    scanned = scanned + ret_rows;
                }
            }
         } /* catch(SQLException sqlee) {
                       sqlee.printStackTrace();
			inCaseOfException();
                        } 
	   */
		 return  scanned ;
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
        LOGGERA.trace( "checkFilt on  " + aquery);
        LOGGERA.trace( "v_opdml is  " + v_opdml);
        
        switch(v_opdml) {
            case "D":
                parz = aquery.substring(12,(aquery.indexOf("WHERE")-1 ));
                LOGGERA.trace( "parz is  #" + parz+ "#");
                qschema = parz.substring(0,parz.indexOf("."));
                LOGGERA.trace( "qschema is  #" + qschema+"#");
                qtable = parz.substring(parz.indexOf(".")+1);
                LOGGERA.trace( "qtable is  #" + qtable+"#");
                break;
            case "I":
                parz = aquery.substring(12,(aquery.indexOf("(")-1 ));
                LOGGERA.trace( "parz is  #" + parz+ "#");
                qschema = parz.substring(0,parz.indexOf("."));
                LOGGERA.trace( "qschema is  #" + qschema+"#");
                qtable = parz.substring(parz.indexOf(".")+1);
                LOGGERA.trace( "qtable is  #" + qtable+"#");
                break;
            case "U":
                parz = aquery.substring(7,(aquery.indexOf("SET")-1 ));
                LOGGERA.trace( "parz is  #" + parz+ "#");
                qschema = parz.substring(0,parz.indexOf("."));
                LOGGERA.trace( "qschema is  #" + qschema+"#");
                qtable = parz.substring(parz.indexOf(".")+1);
                LOGGERA.trace( "qtable is  #" + qtable+"#");
                break;
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("select 1 from walq__" + nodemst +
                "_filtro where schemaf = ? and(tablef = ? or tablef='all') and strpos(opdml, ? ) > 0 ")) {
            preparedStatement.setString(1, qschema);
            preparedStatement.setString(2, qtable);
            preparedStatement.setString(3, v_opdml);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }


  public int manageXid(long loc_xid_offset)  throws SQLException {
        int scanned = 0;
        Statement st = connection.createStatement();
        Statement str = connectionRdb.createStatement();
        long curxid = 0;
        String curlsn ="";
        String query;
        String qerror;
        long mywid = 0;
        long myxid = 0;
        boolean managed;
        boolean isException=false;
 
	LOGGERA.trace(  mythread+":" +"Call scanwalf_walq__" + nodemst + " - xid_offset: " + loc_xid_offset);
	LOGGERA.info(  mythread+":" +"Managing xid  " + loc_xid_offset);
        PreparedStatement preparedStatement =
        connection.prepareStatement("Select wid, lsn, xid,data, dateop from walq__" + nodemst + " where  xid=?  order by wid");
        preparedStatement.setLong(1, loc_xid_offset);
        ResultSet rs2 = preparedStatement.executeQuery();
        managed = false;
        connection.setAutoCommit(false);
        connectionRdb.setAutoCommit(false);

         while (rs2.next()) {
                    mywid =   rs2.getLong(1);
                    myxid =   rs2.getLong(3);
                    scanned = scanned + 1;
                    LOGGERA.trace( mythread+":" +"xid:" + rs2.getLong(3) + " wid:" + rs2.getLong(1) + " sql:" + rs2.getString(4));
                    query = rs2.getString(4);

                    if (!filter || checkFilt(connection,query)) {
                           LOGGERA.trace( "Filter is " + filter +" or if filter true then  checkFilt match ");

                        try {
                            str.execute(query);
                            //str.addBatch(query); va poi gestita l eccezione per inserire nel conflict
                        } catch (Exception e) {
                            isException = true;
                            scanned = -1;
                            qerror = "insert into _rdb_bdr.walq__" + nodemst + "_conflicts  (wid,xid,schemaf , tablef ,opdml ,state, message ,detail, hint, context)  values ("+mywid+", "+ myxid +",'missing' , 'missin' ,'?','error' ,'"+ e.toString().replaceAll("'", "''") + "', '"+ query.replaceAll("'", "''")+ "','','') ";
                            LOGGERA.info(mythread+":" +"into walq__nodemst_conflicts:" + qerror);
                            st.execute(qerror);
      qerror = "update _rdb_bdr.tc_process set n_operation ='shutdown' , n_dateop= now() where n_pid= " +threadn + " and n_mstr = '" +nodemst+ "' and  n_type = 'S';" ;

                            st.execute(qerror);
                            connection.commit();

                            e.printStackTrace();
                            break;
                        }

                     if ( log_hist && !isException ) {
	               query = "INSERT INTO walq__"+ nodemst +"_log (wid, lsn,xid, data, dateop) VALUES ("+rs2.getLong(1) +",'"+rs2.getString(2)+"',"+rs2.getLong(3)+",'" +rs2.getString(4).replaceAll("'", "''") + "','" +  rs2.getTimestamp (5) + "')";
                        LOGGERA.trace(mythread+":" +"query into walq_nodemst_log:" + query);
                        st.addBatch(query);
                     }

                    } else {
                        LOGGERA.trace(mythread+":" + "Filter NOT match");
                    }

                    if ( !managed  ) {
			
                        managed = true;
                     //   PreparedStatement preparedStatement2 = connectionRdb.prepareStatement(" select case  WHEN isx is null then  txid_current() when isx> 0 then  isx end from  txid_current_if_assigned()  as isx;");
// PreparedStatement preparedStatement2 = connectionRdb.prepareStatement(" select case  WHEN isx is null then  txid_current() when isx> 0 then  isx end,"+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +"::varchar from  txid_current_if_assigned()  as isx;");
//08042021  txid_current_if_assigned dalla versione 10
PreparedStatement preparedStatement2 = connectionRdb.prepareStatement(
        (
                ((BaseConnection) connectionRdb).haveMinimumServerVersion(ServerVersion.v10) ?
                                " select case  WHEN isx is null then  txid_current() when isx> 0 then  isx end,pg_current_wal_lsn()::varchar from  txid_current_if_assigned()  as isx;  " :
                                " select txid_current(),pg_current_xlog_location()::varchar ;" )
);


                        ResultSet rs = preparedStatement2.executeQuery();
                        if (rs.next()) {
                            curxid = rs.getLong(1);
                            curlsn = rs.getString(2);
                            LOGGERA.trace(mythread+":curxid:"+curxid );
                        }
                       // query = "INSERT INTO walq__" + node + "_xid ( xid_from_queue, xid_current) values  (" + rs2.getInt(3) + "," + curxid +  ") On CONFLICT ON CONSTRAINT  walq__" + node + "_xid_pkey DO NOTHING ";


                        if (isMaster) {
                            query = "INSERT INTO walq__" + node + "_xid ( xid_from_queue, xid_current,lsn) values  (" + rs2.getLong(3) + "," + curxid + ",'" + curlsn + "')  ";
                            //str.addBatch(query);
                            st.addBatch(query);
                        }
                        query = " update  walq__" + nodemst + "_offset set lsn_offset = '"+rs2.getString(2) +"' , last_offset = " + rs2.getLong(1) + ", xid_offset = " + rs2.getLong(3) + ", dateop=now() where src_topic_id ='"+node+"'";
                        st.addBatch(query);
			//notify();
                    }
                }
				
                if (managed && !isException) {
                    LOGGERA.trace( mythread+":" +"Commmit curxid:" + curxid);
                   try {
                       query = " update  walq__" + nodemst + "_offset set last_offset = " + mywid + ", xid_offset = " + myxid+ ", dateop=now() where src_topic_id ='"+node+"'";
                       st.addBatch(query);
                        st.executeBatch();
                    } catch (Exception e) {
					      e.printStackTrace();
                             try {
                                   inCaseOfException();
                              } catch(SQLException sqlee) {
                                sqlee.printStackTrace();
                             }
                    }
                    try {
                        str.executeBatch();
                    } catch (BatchUpdateException ee) {
                        //throw ee.getNextException();
                        try {
                            inCaseOfException();
                        } catch(SQLException sqlee) {
                            sqlee.printStackTrace();
                        }
                    }
		    //try { wait(); } catch(InterruptedException IntExp) {      }
                        connection.commit();
                        connectionRdb.commit();

                    connection.setAutoCommit(true);
                }
                rs2.close();
				return scanned;
  }



    public void runwal() throws Exception {

    int scanned = 0;
    int ret_rows = 0;
	int limit = batch_size;

        while (running.get()) {
	  try {


	   Object mutexLock = new Object();
	   synchronized(mutexLock) { //synchronized block  

           ret_rows = selectXid(connection,limit);
           if (ret_rows == -1 ) {
               // scanned = -1;
               TimeUnit.MILLISECONDS.sleep(3000L);
           } else {
               scanned = scanned + ret_rows;
       }

	    LOGGERA.info( mythread+":" +"Scanned: "+ scanned);
	    TimeUnit.MILLISECONDS.sleep(100L);
	   } //end sync

		
	  } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                 LOGGERA.error("  Thread was interrupted, Failed to complete operation:"+ mythread);
          } 
        }


    }


 public static boolean isNullOrEmpty(String myString)
    {
         return myString == null || "".equals(myString);
    }


    public synchronized void loadProps(String[] args)  throws Exception {

        String rdbbdr = System.getenv("RDBBDR");

	Options options = new Options();

   	Option rnodemstr = new Option("rn", "rnodemstr", true, "Please set desidered remote node master");
   	rnodemstr.setRequired(true);
   	options.addOption(rnodemstr);

	Option nodeslvs = new Option("s", "nodeslvs", true, "Please set desidered node slave");
        nodeslvs.setRequired(true);
        options.addOption(nodeslvs);

	Option threadnum =  new Option("tn", "threadnum", true, "Please set thread number");
        threadnum.setRequired(false);
	options.addOption(threadnum);

 	CommandLineParser parser = new DefaultParser();
   	HelpFormatter formatter = new HelpFormatter();
   	CommandLine cmd = null;

 	try {
       		cmd = parser.parse(options, args);
   	} catch (ParseException e) {
       		 LOGGERA.error(e.getMessage());
        	formatter.printHelp("TAppl", options);
		isInitialized = false ;
   	}

   	nodemst= cmd.getOptionValue("rnodemstr");
   	nodeslv= cmd.getOptionValue("nodeslvs");
   	threadn = Integer.parseInt(cmd.getOptionValue("threadnum"));

	LOGGERA.info("Running TAppl consumer " + nodeslv + " for node " + nodemst);

        if (isNullOrEmpty(rdbbdr))
        {
                 LOGGERA.error("RDBBDR variable should be set ");
		isInitialized = false ;
        }

        if (isNullOrEmpty(nodemst))
        {
                 LOGGERA.error("NODEMST variable should be set ");
		isInitialized = false ;
        }

        if (isNullOrEmpty(nodeslv))
        {
                LOGGERA.error("NODESLV variable should be set ");
		isInitialized = false ;
        }

        String nodemst_rdbbdr_conf = rdbbdr + "/conf/" + nodemst + "_rdb_bdr.conf";
        String nodeslv_rdbbdr_conf = rdbbdr + "/conf/" + nodeslv + "_bdr_rdb.conf";

        if(!Files.isRegularFile(Paths.get(nodemst_rdbbdr_conf))) {
                 LOGGERA.error(nodemst_rdbbdr_conf + " not exists! ");
		 isInitialized = false ;
		 throw new Exception(nodemst_rdbbdr_conf + " not exists!  Exit");
        }

        if(!Files.isRegularFile(Paths.get(nodeslv_rdbbdr_conf))) {
                 LOGGERA.error(nodeslv_rdbbdr_conf + " not exists! ");
		 isInitialized = false ;
		 throw new Exception(nodemst_rdbbdr_conf + " not exists!  Exit");

        }

        try (InputStream input = new FileInputStream(nodeslv_rdbbdr_conf)) {

            Properties prop = new Properties();

            prop.load(input);

            LOGGERA.trace("Configuration file: " +  nodeslv_rdbbdr_conf + " review");
		
            LOGGERA.trace("Master node: "+ nodemst);
            LOGGERA.trace("Slave Database: ");
            LOGGERA.trace("db " + prop.getProperty("db"));
            LOGGERA.trace("user " + prop.getProperty("user"));
            LOGGERA.trace("pwd " + prop.getProperty("pwd"));
            LOGGERA.trace("node " + prop.getProperty("node"));
            LOGGERA.trace("host " + prop.getProperty("host"));
            LOGGERA.trace("");
            LOGGERA.trace("RDB database: ");
            LOGGERA.trace("rdb " + prop.getProperty("rdb"));
            LOGGERA.trace("ruser " + prop.getProperty("ruser"));
            LOGGERA.trace("rpwd " + prop.getProperty("rpwd"));
            LOGGERA.trace("rnode " + prop.getProperty("rnode"));
            LOGGERA.trace("rhost " + prop.getProperty("rhost"));
            LOGGERA.trace("walqtrunc " + prop.getProperty("walqtrunc"));
            LOGGERA.trace("batch_size " + prop.getProperty("batch_size"));
            LOGGERA.trace("filter " + prop.getProperty("filter"));
            LOGGERA.trace("log_hist " + prop.getProperty("log_hist"));
            LOGGERA.trace("");

            host = prop.getProperty("host");
            user = prop.getProperty("user");
            port = Integer.valueOf(prop.getProperty("port"));
            pwd = prop.getProperty("pwd");
            db = prop.getProperty("db");
            node = prop.getProperty("node");
            walq = "_rdb_bdr.walq__" + node;

            rhost = prop.getProperty("rhost");
            ruser = prop.getProperty("ruser");
            rport = Integer.valueOf(prop.getProperty("rport"));
            rpwd = prop.getProperty("rpwd");
            rdb = prop.getProperty("rdb");

            walqtrunc = Integer.valueOf(prop.getProperty("walqtrunc"));
            batch_size = Integer.valueOf(prop.getProperty("batch_size"));
	    
            filter = Boolean.valueOf(prop.getProperty("filter"));
	    log_hist = Boolean.valueOf(prop.getProperty("log_hist"));



        } catch (IOException ex) {
		 isInitialized = false ;
            ex.printStackTrace();
        }

    }



    public static void main(String[] args) {

        TAppl app = new TAppl(args);

	try {
        	app.loadProps(args);
         }  catch (Exception ex) {
                LOGGERA.error( ex.getMessage());
         }

	Thread t1 = new Thread(app);
        t1.start();  

    }
}

