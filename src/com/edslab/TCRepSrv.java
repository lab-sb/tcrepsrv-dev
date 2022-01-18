/**
 * # -- Copyright (c) 2018-2022  silvio.brandani <support@tcapture.net>. All rights reserved.
 *
 */

package com.edslab;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.Set;
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

/**
 * TCRepSrv 
*/

public class TCRepSrv implements Runnable {

 private static final Logger LOGGERS= LogManager.getLogger(TCRepSrv.class.getName());

    private TCapt tc ;
    private TMoni tm ;
    private TCoor to ;
    private Thread capthread ;
    private Thread monthread ;
    private Thread coothread ;
    private TAppl[] app  = new TAppl[5]; ;
    private Thread[] appthread = new Thread[5];
    private static int cardC;
    private static int cardA;
    private static int cardT;
    private static int cardM;
    private static int cardD;
    private String host = "";
    private String user = "";
    private String pwd = "";
    private int port = 5432;
    private String db = "";
    private String node = "";
    private String rhost = "";
    private String ruser = "";
    private String rpwd = "";
    int rport = 5432;
    private String rdb = "";
    private String nodemst = "";
   
   private Connection connection;


  private String createUrl() {
        return "jdbc:postgresql://" + rhost + ':' + rport + '/' + rdb;
    }


  public void createConnection()  throws SQLException
        {
     try {
            Properties properties = new Properties();
            properties.setProperty("user", ruser);
            properties.setProperty("password", rpwd);
            properties.setProperty("reWriteBatchedInserts", "true");
            connection = DriverManager.getConnection(createUrl(), properties);
        } catch (SQLException ex) {
         throw new SQLException("Invalid Connection " +ex.getMessage());
        }
    }

  private boolean isServerCompatible() {
        //return ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v9_5);
        return ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10);
    }

  public static boolean isNullOrEmpty(String myString)
    {
         return myString == null || "".equals(myString);
    }


  public void loadProps(String[] args) {
        String rdbbdr = System.getenv("RDBBDR");

        Options options = new Options();

        Option nodemstr = new Option("n", "nodemstr", true, "Please set desidered node master");
        nodemstr.setRequired(true);
        options.addOption(nodemstr);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOGGERS.error(e.getMessage());
            formatter.printHelp("TCapt", options);

            System.exit(1);
        }

        nodemst= cmd.getOptionValue("nodemstr");


        if (isNullOrEmpty(rdbbdr))
        {
                 LOGGERS.error("RDBBDR variable should be set ");
                 System.exit(-1);
        }

        if (isNullOrEmpty(nodemst))
        {
                 LOGGERS.error("NODEMST variable should be set ");
                 System.exit(-1);
        }

        LOGGERS.info("***********************************************************************");
        LOGGERS.info("Running TCapture Replication Server for node :" + nodemst);
        LOGGERS.info("***********************************************************************");

        String rdbbdr_conf = rdbbdr + "/conf/" + nodemst + "_rdb_bdr.conf";

        if(!Files.isRegularFile(Paths.get(rdbbdr_conf))) {
                 LOGGERS.error(rdbbdr_conf + " not exists! ");
                 System.exit(-1);
        }


        try (InputStream input = new FileInputStream(rdbbdr_conf)) {

            Properties prop = new Properties();

            prop.load(input);


            LOGGERS.trace("--------------------------------------------------------------------");
            LOGGERS.trace("Configuration file " +  rdbbdr_conf + " review");
            LOGGERS.trace("# Producer database: ");
            LOGGERS.trace("db     " + prop.getProperty("db"));
            LOGGERS.trace("user   " + prop.getProperty("user"));
            LOGGERS.trace("pwd    " + prop.getProperty("pwd"));
            LOGGERS.trace("node   " + prop.getProperty("node"));
            LOGGERS.trace("host   " + prop.getProperty("host"));
            LOGGERS.trace("# RDB database: ");
            LOGGERS.trace("rdb    " + prop.getProperty("rdb"));
            LOGGERS.trace("ruser  " + prop.getProperty("ruser"));
            LOGGERS.trace("rpwd   " + prop.getProperty("rpwd"));
            LOGGERS.trace("rnode  " + prop.getProperty("rnode"));
            LOGGERS.trace("rhost  " + prop.getProperty("rhost"));
            LOGGERS.trace("---------------------------------------------------------------------");

            host = prop.getProperty("host");
            user = prop.getProperty("user");
            port = Integer.valueOf(prop.getProperty("port"));
            pwd = prop.getProperty("pwd");
            db = prop.getProperty("db");
            node = prop.getProperty("node");

            rhost = prop.getProperty("rhost");
            ruser = prop.getProperty("ruser");
            rport = Integer.valueOf(prop.getProperty("rport"));
            rpwd = prop.getProperty("rpwd");
            rdb = prop.getProperty("rdb");


        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }




   public void run()  {  
 	LOGGERS.info("Running thread  "+ Thread.currentThread().getName());

		
  }

public void managed(int v_id, String v_op, String v_type ) throws SQLException, Exception {
	try {
	Statement st = connection.createStatement();
 
        st.execute("update _rdb_bdr.tc_process set n_state = '"+v_op+"',n_operation ='managed' , n_dateop= now() where n_id= "+v_id + " and n_type = '" + v_type +"'");
         st.close();
	 } catch (SQLException e) {
            e.printStackTrace();
        }
}


private boolean checkShutDown() throws SQLException, Exception {
         String qq= " SELECT EXISTS(select 1  from _rdb_bdr.tc_process where n_state != 'shutdown'); ";

         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (null != rs && rs.next())
                 return  rs.getBoolean(1);
            }
        }

        return false;
}



public void loppa()  throws SQLException, Exception {
	String	l_op = ""; 
	String	l_node = ""; 
	String	l_type = ""; 
	String	l_state = ""; 
	int 	l_pid = -1; 
	int	l_id = -1; 
 try {
    while (checkShutDown() ) {

                 try (PreparedStatement preparedStatement = connection.prepareStatement("select n_mstr,n_pid,n_operation,n_id,n_type,n_state from _rdb_bdr.tc_process where n_operation != 'managed'")) {
                         try (ResultSet rs = preparedStatement.executeQuery()) {
                                 while (rs.next()) {
					l_node = rs.getString(1);
					l_pid =  rs.getInt(2);
					l_op =  rs.getString(3);
					l_id =  rs.getInt(4);
					l_type =  rs.getString(5);
					l_state =  rs.getString(6);
                                         LOGGERS.info("To manage "+l_op+ " for node "+ l_node + " having internal id " + l_pid + " id " + l_id + " is of type " +l_type) ;


               if (l_type.equals("C")) {
                   if ((l_op.equals("stop")) || (l_op.equals("shutdown"))) {
                       if (l_state.equals("start")) {
                           if (to.getMyThread().equals("TO-" + l_node + "_" + l_pid)) {
                               LOGGERS.trace("Match");
                               if (l_op.equals("stop")) {
                                                         LOGGERS.info("TCoor Thread name:" + to.getMyThread() + " is going to be stopped!");
                                                         to.stopRunning();
                                                     } else {
                                                         LOGGERS.info("TCoor Thread name:" + to.getMyThread() + " is going to be  shutdown!");
                                                         to.shutDown();
                                                     }

                                                 }
                                             }
                                             managed(l_id, l_op, l_type);
                                         }
                                         if (l_op.equals("start")) {
                                             discoverCoordinator(true, l_pid);
                                         }
                                         //managed(l_id,l_op,l_type);
                                         TimeUnit.MILLISECONDS.sleep(1000L);


              }

				if (l_type.equals("S") ){
							if ( (l_op.equals("stop")) || (l_op.equals("shutdown")) ) {
							   if   (l_state.equals("start"))  {
			                                        if ( app[l_id].getMyThread().equals("TA_" + l_node + "_" + l_pid ) ) {
                                                         		LOGGERS.trace("Match");
									if  (l_op.equals("stop")) {
								 		LOGGERS.info("TAppl Thread name:" +app[l_id].getMyThread() + " is going to be stopped!" );
                                                                 		app[l_id].stopRunning();
									} else {	
								 		LOGGERS.info("TAppl Thread name:" +app[l_id].getMyThread() + " is going to be shutdown!" );
										app[l_id].shutDown();	
									}
								 }
							   } 
                                                           managed(l_id,l_op,l_type);
                                                         }
                                                         if (l_op.equals("start")) {
                                                                 discoverApply(true, l_id);
                                                         }

                                                          //managed(l_id,l_op,l_type);
                                                         TimeUnit.MILLISECONDS.sleep(1000L);

 				} 
				if (l_type.equals("M") ){
                                                        if ( (l_op.equals("stop")) || (l_op.equals("shutdown")) ) {
							   if   (l_state.equals("start"))  {
                                                		if ( tc.getMyThread().equals("TC-" + l_node + "_" + l_pid ) ) {
                                                         		LOGGERS.trace("Match");
									
									 if  (l_op.equals("stop")) {
										 LOGGERS.info("TCapt Thread name:" +tc.getMyThread() + " is going to be stopped!" );
										tc.stopRunning();
                                                                        } else {
										 LOGGERS.info("TCapt Thread name:" +tc.getMyThread() + " is going to be  shutdown!" );
                                                                                tc.shutDown();
                                                                        }
								  }
							    }
                                                            managed(l_id,l_op,l_type);
							 }
							 if (l_op.equals("start")) {
								discoverCapture(true,l_pid);
							 }
                                                          //managed(l_id,l_op,l_type);
                                                        TimeUnit.MILLISECONDS.sleep(1000L);


				}

				 if (l_type.equals("H") ){
                                                        if ( (l_op.equals("stop")) || (l_op.equals("shutdown")) ) {
							   if   (l_state.equals("start"))  {
                                                                if ( tm.getMyThread().equals("TM-" + l_node + "_" + l_pid ) ) {
                                                                        LOGGERS.trace("Match");
									 if  (l_op.equals("stop")) {
                                                                		LOGGERS.info("TMoni Thread name:" +tm.getMyThread() + " is going to be stopped!" );
                                                                                tm.stopRunning();
                                                                        } else {
                                                                                 LOGGERS.info("TMoni Thread name:" +tm.getMyThread() + " is going to be  shutdown!" );
                                                                                tm.shutDown();
                                                                        }

                                                                 }
							   }
                                                        	managed(l_id,l_op,l_type);
                                                         }
                                                         if (l_op.equals("start")) {
                                                                discoverMonitor(true,l_pid);
                                                         }
                                                         //managed(l_id,l_op,l_type);
                                                        TimeUnit.MILLISECONDS.sleep(1000L);


                                }

                for (int j = 0; j < cardC; j++) {
                                                LOGGERS.info("TCoor Thread name:" +to.getMyThread() );
                }
				 for (int j = 0; j < cardA; j++) {
					if (app[j] != null)
                                                LOGGERS.info("TAppl Thread name:" +app[j].getMyThread() );
				 }

				for (int j = 0; j < cardT; j++) {
                                                LOGGERS.info("TCapt Thread name:" +tc.getMyThread() );
				}

				for (int j = 0; j < cardM; j++) {
                                                LOGGERS.info("TMoni Thread name:" +tm.getMyThread() );
                                }	
					

					LOGGERS.trace("Threads list:");	
					 Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
       					 for ( Thread t : threadSet){
            					if ( t.getThreadGroup() == Thread.currentThread().getThreadGroup()){
                				LOGGERS.trace("Thread :"+t+" "+"state:"+t.getState());
            					}
        				}
	 
                                 }
				 //LOGGERS.info("Nothing to do , sleep ..");
                        }
                }
		 TimeUnit.MILLISECONDS.sleep(1000L);
         }
	} catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

}


public int discoverApply(boolean restart, int restartid)  throws SQLException, Exception {
	String v_mstr="";	
	String qq= " select  n_mstr,n_id from _rdb_bdr.tc_process where n_shouldbe ='up' and n_type ='S' " ;
	int i = 0;
	int nodeid = 0;
	String[] myargs =  new String[6];

	if (restart){		
		qq =  " select  n_mstr,n_id from _rdb_bdr.tc_process where  n_type ='S' and n_state != 'start' and n_id = " + restartid + " and n_type = 'S'" ;
	} 

	   try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
		 while (rs.next()) {
                	 v_mstr = rs.getString(1);
			 nodeid = rs.getInt(2);
			 LOGGERS.info("Running consumer thread for node slave: " +nodemst+ " having  master: " + v_mstr + " and  node id "+ nodeid);
			  myargs[0] = "-rn";
			  myargs[1] = v_mstr;
			  myargs[2] = "-s";
			  myargs[3] = nodemst;
			  myargs[4] = "-tn";
		          myargs[5]=  Integer.toString((int)(Math.random()*1000 + (nodeid+10)*1000));
			  LOGGERS.trace( myargs[0]+ myargs[1] + myargs[2] + myargs[3] + myargs[4] +  myargs[5]);

		         app[nodeid] = new TAppl( myargs );
			
			if  ( app[nodeid].isInitialized ) {	
			  	appthread[nodeid] = new Thread(app[nodeid]);
			  	appthread[nodeid].start();
			 	i++;
			        Statement st = connection.createStatement();
        			st.execute("update _rdb_bdr.tc_process set n_state = 'start',n_operation ='managed' , n_dateop= now(), n_pid= "+ myargs[5] +" where n_id = "+nodeid+" and  n_type ='S'");
				st.close();
			} else {
					 Statement st = connection.createStatement();
					st.execute("update _rdb_bdr.tc_process set n_state ='down' , n_operation ='managed', n_dateop= now() where n_id= " + nodeid + " and  n_type = 'S';");	
					 st.close();
			}
		}
		rs.close();
            }
        } catch(SQLException sqlee) {
                                sqlee.printStackTrace();
				 Statement st = connection.createStatement();
				 String qerror;
  				qerror = "update _rdb_bdr.tc_process set n_state ='down' ,n_operation ='managed', n_dateop= now() where n_id= " + nodeid + " and  n_type = 'S';" ;
        				try {
         					st.execute(qerror);
           				}
        					catch (SQLException e) {
            					e.printStackTrace();
        				}

                        }	


         return i;

}


public int discoverCapture(boolean restart, int restartid)  throws SQLException, Exception {
        String v_mstr="";
        int i = 0;
        int nodeid = 0;
        String[] myargs =  new String[4];
		String qq= " select  n_mstr,n_id from _rdb_bdr.tc_process where n_shouldbe ='up' and n_type ='M' ";

	if (restart){
                qq =  " select  n_mstr,n_id from _rdb_bdr.tc_process where  n_type ='M' and n_state != 'start' and n_pid = " + restartid;
        }

         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                 while (rs.next()) {
                                 v_mstr = rs.getString(1);
                                 nodeid = rs.getInt(2);
			         LOGGERS.info("Running producer thread for node master: " + v_mstr + " having  node id "+ nodeid);
                                 myargs[0] = "-n";
                                 myargs[1] = v_mstr;
                                 myargs[2] = "-tn";
                                 myargs[3] = Integer.toString((int)(Math.random()*1000));

                                 tc = new TCapt(myargs);

				if  ( tc.isInitialized ) {
                                 	capthread = new Thread(tc);
                                 	capthread.start();
                                  	i++;
                                	Statement st = connection.createStatement();
                                st.execute("update _rdb_bdr.tc_process set n_state = 'start',n_operation ='managed'  , n_dateop= now(), n_pid= "+ myargs[3] +" where n_id = "+nodeid+ " and  n_type ='M'");
                                	st.close();
				} else {
                                         Statement st = connection.createStatement();
                                        st.execute("update _rdb_bdr.tc_process set n_state ='down' , n_operation ='managed', n_dateop= now() where n_id= " + nodeid + " and  n_type = 'M';");
                                         st.close();
                        	}


                }
                rs.close();
            }
        } catch(SQLException sqlee) {
                         sqlee.printStackTrace();
                         Statement st = connection.createStatement();
                         String qerror;
                         qerror = "update _rdb_bdr.tc_process set n_state ='down' ,n_operation ='managed', n_dateop= now() where n_id= " + nodeid + " and  n_type = 'M';" ;
                         try {
                                st.execute(qerror);
                          }
                               catch (SQLException e) {
                               e.printStackTrace();
                          }
                        }
        return i ;

}


public int discoverMonitor(boolean restart, int restartid)  throws SQLException, Exception {
        String v_mstr="";
        int i = 0;
        int nodeid = 0;
        String[] myargs =  new String[4];
                String qq= " select  n_mstr,n_id from _rdb_bdr.tc_process where n_shouldbe ='up' and n_type ='H' ";

         if (restart){
                qq =  " select  n_mstr,n_id from _rdb_bdr.tc_process where  n_type ='H' and n_state != 'start' and n_pid = " + restartid;
        }

         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                 while (rs.next()) {
                               v_mstr = rs.getString(1);
                               nodeid = rs.getInt(2);
			       LOGGERS.info("Running monitor thread for node master: " + v_mstr + " having  node id "+ nodeid);
                               myargs[0] = "-n";
                               myargs[1] = v_mstr;
                               myargs[2] = "-tn";
                               myargs[3] = Integer.toString((int)(Math.random()*1000 + 99000));

                               tm = new TMoni(myargs);
				if  ( tm.isInitialized ) {
                               		monthread = new Thread(tm);
                               		monthread.start();
                               		i++;
                               		Statement st = connection.createStatement();
                               	st.execute("update _rdb_bdr.tc_process set n_state = 'start',n_operation ='managed'  , n_dateop= now(), n_pid= "+ myargs[3] +" where n_id = "+nodeid+ " and  n_type ='H'");
                               		st.close();
				 } else {
                                         Statement st = connection.createStatement();
                                st.execute("update _rdb_bdr.tc_process set n_state ='down' , n_operation ='managed', n_dateop= now() where n_id= " + nodeid + " and  n_type = 'H';");
                                         st.close();
                       		 }


                }
                rs.close();
            }
        } catch(SQLException sqlee) {
                         sqlee.printStackTrace();
                         Statement st = connection.createStatement();
                         String qerror;
                         qerror = "update _rdb_bdr.tc_process set n_state ='down' ,n_operation ='managed', n_dateop= now() where n_id= " + nodeid + " and  n_type = 'H';" ;
                         try {
                                    st.execute(qerror);
                          }
                           catch (SQLException e) {
                                    e.printStackTrace();
                          }
                   }
        return i ;

}

    public int discoverCoordinator(boolean restart, int restartid)  throws SQLException, Exception {
        String v_mstr="";
        int i = 0;
        int nodeid = 0;
        String[] myargs =  new String[4];
        String qq= " select  n_mstr,n_id from _rdb_bdr.tc_process where n_shouldbe ='up' and n_type ='C' ";

        if (restart){
            qq =  " select  n_mstr,n_id from _rdb_bdr.tc_process where  n_type ='C' and n_state != 'start' and n_pid = " + restartid;
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    v_mstr = rs.getString(1);
                    nodeid = rs.getInt(2);
                    LOGGERS.info("Running coordinator thread for node master: " + v_mstr + " having  node id "+ nodeid);
                    myargs[0] = "-n";
                    myargs[1] = v_mstr;
                    myargs[2] = "-tn";
                    myargs[3] = Integer.toString((int)(Math.random()*1000 + 99000));

                    to = new TCoor(myargs);
                    if  ( to.isInitialized ) {
                        coothread = new Thread(to);
                        coothread.start();
                        i++;
                        Statement st = connection.createStatement();
                        st.execute("update _rdb_bdr.tc_process set n_state = 'start',n_operation ='managed'  , n_dateop= now(), n_pid= "+ myargs[3] +" where n_id = "+nodeid+ " and  n_type ='C'");
                        st.close();
                    } else {
                        Statement st = connection.createStatement();
                        st.execute("update _rdb_bdr.tc_process set n_state ='down' , n_operation ='managed', n_dateop= now() where n_id= " + nodeid + " and  n_type = 'C';");
                        st.close();
                    }


                }
                rs.close();
            }
        } catch(SQLException sqlee) {
            sqlee.printStackTrace();
            Statement st = connection.createStatement();
            String qerror;
            qerror = "update _rdb_bdr.tc_process set n_state ='down' ,n_operation ='managed', n_dateop= now() where n_id= " + nodeid + " and  n_type = 'C';" ;
            try {
                st.execute(qerror);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return i ;

    }


public int discoverDown()  throws SQLException, Exception {
      int i = 0; 
      try (PreparedStatement preparedStatement = connection.prepareStatement(" update _rdb_bdr.tc_process set n_state = 'stop',n_operation ='managed'  , n_dateop= now(), n_pid= -1 where n_shouldbe !='up' ")) {
    	   i = preparedStatement.executeUpdate(); 
				LOGGERS.trace("state set to stop for " + i + " threads");
       }
        return i ;

}



    public static void main(String[] args) {
	int ApplyThread;	
        TCRepSrv reps = new TCRepSrv();
       try {
		reps.loadProps(args);
		reps.createConnection();

	if (!reps.isServerCompatible()) {
            System.err.println("must have server version greater than 9.6.15");
            System.exit(-1);
        }

	 	LOGGERS.info(TCRepSrv.class.getName() +" is in running state");

	    cardC =	 reps.discoverCoordinator(false,0);
		cardA =	 reps.discoverApply(false,0);
		cardT =  reps.discoverCapture(false,0);
		cardM =  reps.discoverMonitor(false,0);
		cardD =  reps.discoverDown();
		
	 	TimeUnit.MILLISECONDS.sleep(1000L);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

	 LOGGERS.info(Thread.currentThread().getName());	
	 LOGGERS.info("Cardinality Apply:"+ cardA);	
	 LOGGERS.info("Cardinality Capture:"+ cardT);	
	 LOGGERS.info("Cardinality Monitor:"+ cardM);	
      


	Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for ( Thread t : threadSet){
            if ( t.getThreadGroup() == Thread.currentThread().getThreadGroup()){
                LOGGERS.trace("Thread :"+t+":"+"state:"+t.getState());
            }
        }

	try {
 	 	reps.loppa();	
		LOGGERS.info(TCRepSrv.class.getName() +"  shut down !");
		System.exit(0);

	 } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }



    }
}

