
/**
 * # -- Copyright (c) 2018-2022  silvio.brandani <support@tcapture.net>. All rights reserved.
 *
 */

package com.edslab;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.cli.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Logical Decoding TMoni
 */

public class TMoni implements Runnable {


    private AtomicBoolean running = new AtomicBoolean(false);
    private String mythread = "";
    private static final Logger LOGGERM= LogManager.getLogger(TMoni.class.getName());

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
    int batch_size = 1000;
    private String loglevel = "";
    private boolean filter = false;

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


    public TMoni(String[] args) {
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
	mythread = "TM-"+ nodemst  +"_"+ threadn;;
	Thread.currentThread().setName("TM-"+ nodemst );
   
        LOGGERM.info(  mythread+"  is in running state");

	 running.set(true);
        createConnection();
        createConnectionRdb();
        if (!isServerCompatible()) {
             LOGGERM.error("must have server version greater than 9.4");
            System.exit(-1);
        }
        try {

  	    iterateMon();

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

public void refreshASlave(String aSlave) throws SQLException, Exception  {
	String qs;
	Statement str = connectionRdb.createStatement();
	qs= "INSERT INTO _rdb_bdr.tc_monit  (n_mstr,n_slv,state,flushed_lsn)  select '"+nodemst+"',substr(slot_name, length('"+nodemst+"_publ_')+1),active,"+(((BaseConnection) connectionRdb).haveMinimumServerVersion(ServerVersion.v10) ? "confirmed_flush_lsn" : "restart_lsn") +"  from pg_replication_slots where slot_name = '"+nodemst+"_publ_"+aSlave+"' ON CONFLICT (n_mstr,n_slv)  DO UPDATE SET  (state,flushed_lsn, check_dateop ) = (select  active,"+ (((BaseConnection) connectionRdb).haveMinimumServerVersion(ServerVersion.v10) ? "confirmed_flush_lsn" : "restart_lsn") +",now() from pg_replication_slots where slot_name like '"+nodemst+"_publ_"+aSlave+"' )  ";
         try {
                 str.execute(qs);
          } catch (SQLException e) {
		e.printStackTrace();
         }
}


public  int refreshRemoteSlave(String aSlave)  throws SQLException, Exception {
	int scanned = 0;
	String qs = null;
	refreshASlave(aSlave);

        loadSlaveProps(aSlave);
        createConnectionSRdb();    
        PreparedStatement sps = connectionSRdb.prepareStatement("select qqq.xid, qqq.dateop,qqq.lsn::varchar , xid_offset , qt.lsn::varchar " +
                " from _rdb_bdr.walq__"+nodemst+" qqq ,  _rdb_bdr.walq__"+nodemst+"_offset o, _rdb_bdr.walq__"+nodemst+" qt" +
                " where qqq.wid = (select max(wid) from _rdb_bdr.walq__"+nodemst+" ) and qt.wid=  last_offset  " +
                " limit 1");

        ResultSet srs = sps.executeQuery();
         if (srs.next()) {
		Statement str = connectionRdb.createStatement();
                LOGGERM.trace( mythread+":" +"Slave:"+aSlave+ " - xid:" + srs.getLong(1) );
                qs = " UPDATE _rdb_bdr.tc_monit set q_xid = " + srs.getLong(1)+  ", q_dateop ='" + srs.getString(2)+ "'," +
                        " q_lsn ='" + srs.getString(3)+  "', xid_offset = "+ srs.getLong(4)+ ", wal_lsn = '"+ srs.getString(5) +"'" +
                        " where n_mstr='"+nodemst+"' and n_slv = '" + aSlave +"'";
                        try {
                            str.execute(qs);

                          }  catch (SQLException e) {
                                 e.printStackTrace();
                          } finally {
                            if (connectionSRdb != null) {
                                srs.close();
                                sps.close();
                                connectionSRdb.close();
                                connectionSRdb = null;
                            }
                           }

         } // srs result 1 row
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
	return 1;
}


public void refreshLocalSlave(String lslave) throws SQLException, Exception  {
	String qs;
	Statement str = connectionRdb.createStatement();
	qs= " INSERT INTO _rdb_bdr.tc_monit  (n_mstr,n_slv, q_xid, q_dateop,q_lsn, state,xid_offset )  select substr(subname, length('"+nodemst+"_subs_')+1), '"+nodemst+"',  qqq.xid, qqq.dateop,qqq.lsn ,subenabled,xid_offset  from pg_subscription,_rdb_bdr.walq__"+lslave+" qqq, _rdb_bdr.walq__"+lslave+"_offset where subname = '"+nodemst+"_subs_"+ lslave+"' and qqq.wid = (select max(wid) from _rdb_bdr.walq__"+lslave+" ) UNION select  substr(subname, length('"+nodemst+"_subs_')+1), '"+nodemst+"',  q_xid,q_dateop,q_lsn ,subenabled,oo.xid_offset  from pg_subscription,_rdb_bdr.tc_monit , _rdb_bdr.walq__"+lslave+"_offset oo  where subname = '"+nodemst+"_subs_"+ lslave+"' and n_mstr= substr(subname, length('"+nodemst+"_subs_')+1) and n_slv = '"+nodemst+"'  order by 1 desc limit 1  ON CONFLICT (n_mstr,n_slv)  DO UPDATE SET (q_xid, q_dateop,q_lsn, state,xid_offset,check_dateop) =  (select qqq.xid, qqq.dateop,qqq.lsn , subenabled ,xid_offset, now()  from pg_subscription,_rdb_bdr.walq__"+lslave+" qqq, _rdb_bdr.walq__"+lslave+"_offset  where subname = '"+nodemst+"_subs_"+ lslave+"' and qqq.wid = (select max(wid) from _rdb_bdr.walq__"+lslave+" ) UNION select q_xid,q_dateop,q_lsn , subenabled ,oo.xid_offset, now()  from pg_subscription,_rdb_bdr.tc_monit , _rdb_bdr.walq__"+lslave+"_offset oo where subname = '"+nodemst+"_subs_"+ lslave+"' and n_mstr= substr(subname, length('"+nodemst+"_subs_')+1) and n_slv = '"+nodemst+"'  order by 1 desc limit 1 ) ";
          try {
                     str.execute(qs);
            }
          catch (SQLException e) {
			e.printStackTrace();
          }
}

public  int refreshMon() throws SQLException, Exception {
        int scanned = 0;

        LOGGERM.info( mythread+":" +"Monitor producer " + nodemst);
	refreshMaster();

	 try (PreparedStatement preparedStatementL =  connectionRdb.prepareStatement("select substr(subname, length('"+nodemst+"_subs_')+1),subname from pg_subscription where subname like  '"+nodemst+"%' order by 1 ")) {
            try (ResultSet rsl = preparedStatementL.executeQuery()) {
                while (rsl.next()) {
                   LOGGERM.info( mythread+":" +"Monitor local consumer " + nodemst + " for  producer " + rsl.getString(1) );
                      refreshLocalSlave(rsl.getString(1) );

                }
            }
         }



        try (PreparedStatement preparedStatement =  connectionRdb.prepareStatement("select substr(slot_name, length('"+nodemst+"_publ_')+1) from pg_replication_slots where slot_name like '"+nodemst+"%' and  slot_name not like '"+nodemst+"%sync%' ")) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                   LOGGERM.info( mythread+":" +"Monitor remote consumer " + rs.getString(1)  + " for  producer " +  nodemst );
                     scanned = scanned + refreshRemoteSlave(rs.getString(1) );

                }
            }
         } 
                 return  scanned ;
    }


public void refreshMaster() throws SQLException, Exception  {
    Boolean  active_slot =null;
    String confirmed_flush_lsn=null;
    String pg_current_wal_lsn=null;
    long xxx_xid =-1;
     String xxx_timestamp=null;


    try (PreparedStatement preparedStatementL =  connection.prepareStatement("select active::char, "+(((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "confirmed_flush_lsn" : "restart_lsn") + "::varchar,"+
            (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +"::varchar,cast(cast(xxx.xid AS text) AS bigint)  ,xxx.timestamp   from pg_replication_slots, pg_last_committed_xact() xxx where slot_name ='rdb_"+nodemst+"_bdr'" )) {
        try (ResultSet rsl = preparedStatementL.executeQuery()) {
            if (rsl.next()) {
                LOGGERM.info( mythread+":" +"Monitor producer " + nodemst  );
                active_slot=rsl.getBoolean(1) ;
                confirmed_flush_lsn=rsl.getString(2) ;
                pg_current_wal_lsn=rsl.getString(3) ;
                xxx_xid =rsl.getLong(4) ;
                xxx_timestamp =rsl.getString(5) ;
            }
        }
    }

		Statement str = connectionRdb.createStatement();
         String qq;
		 //qq = "INSERT INTO _rdb_bdr.tc_monit  (n_mstr,n_slv,db_xid_last_committed, db_last_committed_dateop, wal_lsn, q_xid, q_dateop,q_lsn, state,flushed_lsn) select '"+nodemst+"','"+nodemst+"',cast(cast(xxx.xid AS text) AS int)  ,xxx.timestamp, pg_current_wal_lsn , qqq.xid, qqq.dateop,qqq.lsn , active,confirmed_flush_lsn   from pg_last_committed_xact() xxx,pg_current_wal_lsn(), _rdb_bdr.walq__"+nodemst+" qqq ,pg_replication_slots where  slot_name='rdb_"+nodemst+"_bdr' and  qqq.xid = (select max(xid) from _rdb_bdr.walq__"+nodemst+" ) limit 1 ON CONFLICT (n_mstr,n_slv)  DO UPDATE SET (db_xid_last_committed,db_last_committed_dateop, wal_lsn, q_xid, q_dateop,q_lsn, state,check_dateop,flushed_lsn) = (select cast(cast(xxx.xid AS text) AS int),xxx.timestamp, pg_current_wal_lsn ,qqq.xid, qqq.dateop,qqq.lsn , active , now(),confirmed_flush_lsn  from pg_last_committed_xact() xxx,pg_current_wal_lsn(), _rdb_bdr.walq__"+nodemst+" qqq ,pg_replication_slots where  slot_name='rdb_"+nodemst+"_bdr' and  qqq.xid = (select max(xid) from _rdb_bdr.walq__"+nodemst+" ) limit 1 )";
        qq =                 "INSERT INTO _rdb_bdr.tc_monit  (n_mstr,n_slv,db_xid_last_committed, db_last_committed_dateop, wal_lsn, q_xid, q_dateop,q_lsn, state,flushed_lsn) " +
                "select '"+nodemst+"','"+nodemst+"',"+xxx_xid+" ,'"+xxx_timestamp+"'::timestamp, '"+pg_current_wal_lsn+"'::pg_lsn  , qqq.xid, qqq.dateop,qqq.lsn , "+active_slot+",'"+confirmed_flush_lsn+"'::pg_lsn    " +
                "from "+ (((BaseConnection) connectionRdb).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()")  +", _rdb_bdr.walq__"+nodemst+" qqq ,pg_replication_slots " +
                "where  slot_name='rdb_"+nodemst+"_bdr' and  qqq.wid = (select max(wid) from _rdb_bdr.walq__"+nodemst+" ) " +
                "UNION " +
                "select '"+nodemst+"','"+nodemst+"',"+xxx_xid+" ,'"+xxx_timestamp+"'::timestamp, '"+pg_current_wal_lsn+ "'::pg_lsn , qqq.xid, qqq.dateop,qqq.lsn , "+active_slot+",'"+confirmed_flush_lsn+"'::pg_lsn    " +
                " from" +
                "   "+  (((BaseConnection) connectionRdb).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") + ", " +
                "   _rdb_bdr.walq__"+nodemst+" qqq  " +
                "   where  not exists (select 1 from pg_replication_slots where     slot_name='rdb_"+nodemst+"_bdr'  ) " +
                "   and  qqq.wid = (select max(wid) from _rdb_bdr.walq__"+nodemst+"  ) " +
                "limit 1 " +
                "ON CONFLICT (n_mstr,n_slv)  " +
                "DO UPDATE SET (db_xid_last_committed,db_last_committed_dateop, wal_lsn, q_xid, q_dateop,q_lsn, state,check_dateop,flushed_lsn) = " +
                "(select "+xxx_xid+" ,'"+xxx_timestamp+"'::timestamp, '"+pg_current_wal_lsn+ "'::pg_lsn  ,qqq.xid, qqq.dateop,qqq.lsn , "+active_slot+", now(),'"+confirmed_flush_lsn+"'::pg_lsn   from "+  (((BaseConnection) connectionRdb).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +", " +
                "_rdb_bdr.walq__"+nodemst+" qqq ,pg_replication_slots where  slot_name='rdb_"+nodemst+"_bdr' and  qqq.wid = (select max(wid) from _rdb_bdr.walq__"+nodemst+" ) " +
                "UNION " +
                "Select "+xxx_xid+" ,'"+xxx_timestamp+"'::timestamp, '"+pg_current_wal_lsn+ "'::pg_lsn  ,qqq.xid, qqq.dateop,qqq.lsn , "+active_slot+" , now(),'"+confirmed_flush_lsn+"'::pg_lsn   from "+  (((BaseConnection) connectionRdb).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +", " +
                "_rdb_bdr.walq__"+nodemst+" qqq  " +
                "where  not exists (select 1 from pg_replication_slots where    slot_name='rdb_"+nodemst+"_bdr'   ) and  qqq.wid = (select max(wid) from _rdb_bdr.walq__"+nodemst+" ) " +
                "limit 1 ) ";
		 try {
                str.execute(qq);
          }     catch (SQLException e) {
                e.printStackTrace();
          }
}


public void iterateMon() throws Exception {

        int batchsize;
        int scanned = 0;
        batchsize = 0;

         while (running.get()) {
	try {

            	batchsize = batchsize + 1;
		scanned = scanned + refreshMon();

		LOGGERM.trace( mythread+":" +"<scanned: "+ scanned);
                TimeUnit.MILLISECONDS.sleep(1500L);
			
	 } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                LOGGERM.info("  Thread was interrupted, Failed to complete operation:"+ mythread);
          }
               
	 }
}


/*
     public void iterateM() throws Exception {

        int batchsize;
        int scanned = 0;
        batchsize = 0;

         while (running.get()) {

            batchsize = batchsize + 1;
        	 //isSlotActive(connection);
		Statement str = connectionRdb.createStatement();
 		String qq;
 		String qs;
 		String qd;
		 	qq = "INSERT INTO _rdb_bdr.tc_monit  (n_mstr,n_slv,db_xid_last_committed, db_last_committed_dateop, wal_lsn, q_xid, q_dateop,q_lsn, state,flushed_lsn) select '"+nodemst+"','"+nodemst+"',cast(cast(xxx.xid AS text) AS int)  ,xxx.timestamp, pg_current_wal_lsn , qqq.xid, qqq.dateop,qqq.lsn , active,confirmed_flush_lsn   from pg_last_committed_xact() xxx,pg_current_wal_lsn(), _rdb_bdr.walq__"+nodemst+" qqq ,pg_replication_slots where  slot_name='rdb_"+nodemst+"_bdr' and  qqq.xid = (select max(xid) from _rdb_bdr.walq__"+nodemst+" ) limit 1 ON CONFLICT (n_mstr,n_slv)  DO UPDATE SET (db_xid_last_committed,db_last_committed_dateop, wal_lsn, q_xid, q_dateop,q_lsn, state,check_dateop,flushed_lsn) = (select cast(cast(xxx.xid AS text) AS int),xxx.timestamp, pg_current_wal_lsn ,qqq.xid, qqq.dateop,qqq.lsn , active , now(),confirmed_flush_lsn  from pg_last_committed_xact() xxx,pg_current_wal_lsn(), _rdb_bdr.walq__"+nodemst+" qqq ,pg_replication_slots where  slot_name='rdb_"+nodemst+"_bdr' and  qqq.xid = (select max(xid) from _rdb_bdr.walq__"+nodemst+" ) limit 1 )";

        	try {
			//str.execute(qd);
         		str.execute(qq);
           	}
        		catch (SQLException e) {
            		e.printStackTrace();
        	}

		PreparedStatement preparedStatement =  connectionRdb.prepareStatement("select substr(slot_name, length('"+nodemst+"_publ_')+1) from pg_replication_slots where slot_name like '"+nodemst+"%' ");
				ResultSet rs = preparedStatement.executeQuery();
				while (rs.next()) {
						LOGGERM.info( mythread+":" +"Slave:" + rs.getString(1) );
						qs= "INSERT INTO _rdb_bdr.tc_monit  (n_mstr,n_slv,state,flushed_lsn)  select '"+nodemst+"',substr(slot_name, length('"+nodemst+"_publ_')+1),active,confirmed_flush_lsn  from pg_replication_slots where slot_name = '"+nodemst+"_publ_"+rs.getString(1)+"' ON CONFLICT (n_mstr,n_slv)  DO UPDATE SET  (state,flushed_lsn, check_dateop ) = (select  active,confirmed_flush_lsn,now() from pg_replication_slots where slot_name like '"+nodemst+"_publ_"+rs.getString(1)+"' )  ";
						try {
                        				str.execute(qs);
                				}
                        				catch (SQLException e) {
                        				e.printStackTrace();
                				}

						loadSlaveProps(rs.getString(1));
						createConnectionSRdb();
						Statement ssta = connectionSRdb.createStatement();
						PreparedStatement sps = connectionSRdb.prepareStatement("select qqq.xid, qqq.dateop,qqq.lsn , xid_offset  from _rdb_bdr.walq__"+nodemst+" qqq ,  _rdb_bdr.walq__"+nodemst+"_offset where qqq.xid = (select max(xid) from _rdb_bdr.walq__"+nodemst+" ) limit 1");
					ResultSet srs = sps.executeQuery();
					 while (srs.next()) {
					 	LOGGERM.info( mythread+":" +"Slave xid:" + srs.getLong(1) );
						qs = " UPDATE _rdb_bdr.tc_monit set q_xid = " + srs.getLong(1)+  ", q_dateop ='" + srs.getString(2)+  "', xid_offset = "+ srs.getLong(4)+ " where n_mstr='"+nodemst+"' and n_slv = '" + rs.getString(1) +"'";
						
					   try {
                                                        str.execute(qs);
						if( connectionSRdb != null)
                               				  connectionSRdb.close();
                                                }
                                            catch (SQLException e) {
                                                        e.printStackTrace();
						 } finally {  // Just to make sure that both con and stat are "garbage collected"
                                 		connectionSRdb = null;
                        			}
	
						
					 }

				}	
	
	



                LOGGERM.info( mythread+":" +"<scanned: "+ scanned);
                TimeUnit.MILLISECONDS.sleep(1500L);
        }
    }

*/
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
            formatter.printHelp("TMoni", options);
	    isInitialized = false ;
        }

        nodemst= cmd.getOptionValue("nodemstr");
	threadn = Integer.parseInt(cmd.getOptionValue("threadnum"));

	 LOGGERM.info("Running TMoni for node :" + nodemst);


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
		
            walqtrunc = Integer.valueOf(prop.getProperty("walqtrunc"));
            batch_size = Integer.valueOf(prop.getProperty("batch_size"));
            filter = Boolean.valueOf(prop.getProperty("filter"));


        } catch (IOException ex) {
		isInitialized = false ;
            ex.printStackTrace();
        }

    }


    private boolean isServerCompatible() {
        return ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v9_5);
    }


    public static void main(String[] args) {

        TMoni app = new TMoni(args);

       try {
        	app.loadProps(args);
        }  catch (Exception ex) {
                LOGGERM.error( ex.getMessage());
        }



	 Thread t1 = new Thread(app);
	 t1.start();

    }
}



