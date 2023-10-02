
/**
    *  Copyright (c) 2022-2023
    *  Silvio Brandani <mktg.tcapture@gmail.com>. All rights reserved.
 **/

package com.edslab;


import org.apache.commons.cli.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.ServerVersion;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.util.concurrent.TimeoutException;

/**
 * TCSrvCTL
*/

public final class TCSrvCTL {


 private Options options;
 private CommandLineParser parser;
    private String rdbbdr = null;
    private String nodetype = null;
    private String node = null;
    private String host = null;
    private  String user = null;
    private  String passwd = null;
    private  String db = null;
    private  String rhost = null;
    private  String ruser = null;
    private  String rpasswd = null;
    private int port = 0;
    private int rport = 0;
    private String rdb = null;

    private String primary = null;
    private  String pnode = null;
    private  String phost = null;
    private  String puser = null;
    private  String ppasswd = null;
    private  String pdb = null;
    private  String prdb = null;
    private  String prhost = null;
    private  String pruser = null;
    private  String prpasswd = null;
    private  int pport = 0;
    private  int prport = 0;


    private boolean isServer = false;
 private Connection connection;
 private int WT = 9;
 private int BS = 500;
 private String currentlsnfunc = "";
 private String msg= null;

    private String setCurrentLSNFunction() throws SQLException {
        try (Statement st = connection.createStatement()) {
            if (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10)) {
                return "pg_current_wal_lsn()";
            } else {
                return "pg_current_xlog_location()";
            }
        }
    }
  private boolean isServerCompatible() {
        //return ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10);
      return ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v9_5);
    }


    private  void createConnection(String host, int port, String db, String user , String pwd)  throws SQLException
	{

    String url =  "jdbc:postgresql://" + host + ':' + port + '/' + db;	

	
     try {
            Properties properties = new Properties();
            properties.setProperty("user", user);
            properties.setProperty("password", pwd);
            properties.setProperty("reWriteBatchedInserts", "true");
            connection = DriverManager.getConnection(url, properties);
        } catch (SQLException ex) {
	 throw new SQLException("Invalid Connection " +ex.getMessage());
        }
	if (!isServerCompatible()) {
            System.err.println("must have server version greater than 9.4");
            System.exit(-1);
        }
	
    }

    private static boolean isNullOrEmpty(String myString)
    {
         return myString == null || "".equals(myString);
    }

    private static boolean askYesNo(String question) {
        return askYesNo(question, "[Y]", "[N]");
    }

    private static boolean askYesNo(String question, String positive, String negative) {
        Scanner input = new Scanner(System.in);
        positive = positive.toUpperCase();
        negative = negative.toUpperCase();
        String answer;
        do {
            System.out.print(question);
            answer = input.next().trim().toUpperCase();
        } while (!answer.matches(positive) && !answer.matches(negative));
        return answer.matches(positive);
    }


 private static Options createOptions() { 
	Options options = new Options(); 

	Option help = new Option(null, "help", false,    "Print this help message."); 
	  options.addOption(help);

     Option topology = new Option(null, "topology", false,  "Show Mesh Topology");
     options.addOption( topology);
     Option detail = new Option(null, "detail", false,  "Show Mesh Topology more details");
     options.addOption( detail);


     Option shutdown = new Option(null, "shutdown", false,  "Shutdown TC Replication Server  ");
        options.addOption( shutdown);

	Option config = new Option(null,"config", false ,  "Configure a node ");
	options.addOption( config);

     Option marker = new Option(null,"marker", false ,  "Move marker to next/given xid for a consumer  ");
     options.addOption( marker);

     Option setup = new Option(null, "setup", false,  "Setup a node ");
        options.addOption( setup);

     Option unset = new Option(null, "unset", false,  "Unset a node ");
     options.addOption( unset);

        Option force = new Option(null, "force", false,  "Force setup a node ");
        options.addOption( force);

        Option enable  = new Option(null, "enable", false,  "Enable a node at startup of TCRepSrv");
	options.addOption( enable);

        Option disable = new Option(null, "disable", false,  "Disable a node at startup of TCRepSrv");
        options.addOption( disable);

	 Option start = new Option(null, "start", false,  "Start a thread on  node running TCRepSrv");
        options.addOption( start );

        Option stop = new Option(null, "stop", false,  "Stop a thread on  node running TCRepSrv");
        options.addOption( stop);

	 Option showconf = new Option(null, "showconf", false,  "Show node configuration ");
        options.addOption( showconf);
	
	  Option status = new Option(null, "status", false,  "Show status of TC Replication Server threads ");
        options.addOption( status);

         Option type = new Option(null,"type", true,  "Node type [producer/consumer]");
        options.addOption( type);

         Option producer = new Option(null,"producer", true,  "Producer node name ");
        options.addOption( producer);

	Option node = new Option(null,"node", true,  "Node name ");
        options.addOption( node);

     Option set_xid = new Option(null,"set_xid", true,  "Set marker to a given xid ");
     options.addOption( set_xid);

     Option next_xid = new Option(null,"next_xid", false,  "Set marker to a next xid ");
     options.addOption( next_xid);

	Option host= new Option(null,"host", true,  "Host server");
        host.setRequired( false);
        options.addOption( host);

	Option user = new Option(null,"user", true,  "User rdbbbdr_user");
        options.addOption( user);
	
	Option port  = new Option(null,"port", true,  "Host port");
	options.addOption(port);

	Option passwd  = new Option(null,"passwd", true,  "User password");
        options.addOption(passwd);
	
        Option db = new Option(null,"db", true,  "Database for replication");
        options.addOption(db);

        Option rhost= new Option(null,"rhost", true,  "RHost server ");
        options.addOption( rhost);

        Option ruser = new Option(null,"ruser", true,  "RUser rdbbbdr_user");
        options.addOption( ruser);

        Option rport  = new Option(null,"rport", true,  "RHost port");
        options.addOption(rport);

        Option rpasswd  = new Option(null,"rpasswd", true,  "RUser password");
        options.addOption(rpasswd);


	return options;
 }

boolean isProcessIdRunning(String pid, String [] command) {
    try {
	//System.out.println("Executing command:" +command[2] + " matching node "+pid);
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(command);

        InputStreamReader isReader = new InputStreamReader(pr.getInputStream());
        BufferedReader bReader = new BufferedReader(isReader);
        String strLine = null;
        while ((strLine= bReader.readLine()) != null) {
            if (strLine.contains( pid )) {
                return true;
            }
        }

        return false;
    } catch (Exception ex) {
        System.out.println("Got exception using system command "+command+" "+ ex);
        return true;
    }
}


    private boolean topologyMesh(boolean detail )  throws SQLException, Exception {
        Date date = new Date();
        System.out.println( date);
        String sint_msg=node;
        String qq =null;
        if (detail) {
            //System.out.println("----------------------------------------------------------------------------------------------");
            System.out.println(" ");
            System.out.println("---------- Show  ReplSrvr for node " + node + " ----------" );

        } else {
            //System.out.println("----------------------------------------------------------------------------------------------");
            System.out.println(" ");
            System.out.println("---------- Show Mesh Topology for Node " + node + " ----------" );

        }


        createConnection(rhost,rport,rdb, ruser, rpasswd);
       //String qq= " select active  from pg_replication_slots where slot_name = 'rdb_"+ node +"_bdr'";
       //10.02.2021 String qq = "select active , creation_timestamp as upsert_date, tx_src_dateop as master_creation  from _rdb_bdr.tc_monit,  _rdb_bdr.walq__"+ node +"_ddl , pg_replication_slots where slot_name = 'rdb_"+ node +"_bdr' AND ddl_command='UPSERT' and mstr_id='"+ node +"'";
        if  (checkSchemaRdbExist(rdb) && checkIsPrimary(node) ) {
            createConnection(host,port,db, user, passwd);
             qq = "select active , ddl.creation_timestamp as upsert_date, tc.creation_timestamp as master_creation  from  dba.__events_ddl tc,  dba.__events_ddl ddl, pg_replication_slots where slot_name = 'rdb_" + node + "_bdr' AND ddl.ddl_command='UPSERT' and tc.ddl_object='" + node + "'";
            try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (rs.next()) {
                        if (detail) {
                            System.out.println("Node " + node + " is a MASTER NODE                                        since has a replication slot 'rdb_" + node + "_bdr' on its primary database ");


                        } else {
                            sint_msg = sint_msg + " MASTER ";


                        }


                        if (rs.getBoolean(1)) {
                            if (detail) {
                                System.out.println("Node " + node + " have TCapture Replication Server running                  since active status is " + rs.getBoolean(1) + " for  replication slot 'rdb_" + node + "_bdr'");
                            } else {
                                sint_msg = sint_msg + " up ";
                            }
                        } else {
                            if (detail) {
                                System.out.println("Node " + node + " have TCapture Replication Server NOT running                since active status is " + rs.getBoolean(1) + " for  replication slot 'rdb_" + node + "_bdr'");
                            } else {
                                sint_msg = sint_msg + " down ";
                            }

                        }

                    } else {
                        if (detail) {
                            System.out.println("Node " + node + " has not a replication slot on its primary database, is NOT a MASTER NODE");
                        } else {
                            sint_msg = sint_msg + " not a MASTER ";
                        }


                    }

                }
                if (!detail) System.out.println(sint_msg);
            }
        }
        if (detail) {
            System.out.println(" ");
            System.out.println("---------- Show Consumers for node " + node + " ----------");
        }
        sint_msg = node;
        createConnection(rhost,rport,rdb, ruser, rpasswd);

         qq = " select substring(slot_name,length('"+node+"_publ_')+1, length(slot_name)),  active , database from pg_replication_slots where slot_name like '"+ node +"_publ_%'";
        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while  ( rs.next()) {

                    if (rs.getBoolean(2)) {
                        if (detail) {
                            System.out.println("Node " + rs.getString(1) + " is an active consumer of " + node + "                    since active status is " + rs.getBoolean(2) + " for  replication slot '" + node + "_publ_" + rs.getString(1) + "'");
                        } else {
                            sint_msg = sint_msg + " --> " + rs.getString(1) + " active ";
                        }
                    } else {
                        if (detail) {
                            System.out.println("Node " + rs.getString(1) + " is NOT active consumer of " + node + "                    since active status is " + rs.getBoolean(2) + " for  replication slot '" + node + "_publ_" + rs.getString(1) + "'");
                        } else {
                            sint_msg = sint_msg + " --> " + rs.getString(1) + " not_active ";
                        }


                    }


                }
            }
            if (!detail) System.out.println( sint_msg );
        }

        if (detail) {
            System.out.println(" ");
            System.out.println("---------- Show Producers for node " + node + " ----------");
        }
        createConnection(rhost,rport,rdb, ruser, rpasswd);
        sint_msg = node;

        qq = " select substring(subname,length('"+node+"_subs_')+1, length(subname)),  subenabled from pg_subscription where subname  like '"+ node +"_subs_%'";
        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while  ( rs.next()) {

                    if (rs.getBoolean(2)) {
                        if (detail) {
                            System.out.println("Node "+ rs.getString(1)+" is an enabled producer of " + node +"                    since enable status is " +rs.getBoolean(2) + " for  subscription '"+node+"_subs_"+rs.getString(1)+"'" );

                        } else {
                            sint_msg = sint_msg + " <-- " + rs.getString(1) + " enabled ";
                        }


                    } else {
                        if (detail) {
                            System.out.println("Node " + rs.getString(1) + " is NOT enabled producer of " + node + "                    since enable status is " + rs.getBoolean(2) + " for  subscription '" + node + "_subs_" + rs.getString(1) + "'");
                        } else  {
                            sint_msg = sint_msg + " <-- " + rs.getString(1) + " disabled ";
                        }

                    }


                }
            }
            if (!detail) System.out.println( sint_msg );
        }

        return false;
    }

private boolean shutDownRepSrv()  throws SQLException, Exception {
	 String[] cmd_ps = { "/bin/sh", "-c", "ps -ef |grep -v grep| grep java|grep 'com.edslab.TCRepSrv -n '"  +node };
	if  (!isProcessIdRunning(node,cmd_ps))  {
		System.out.println("TC Replication Server is not running for node "+node);	
		System.exit(0);
	}

	
	System.out.println("Shutting down all threads in Replication Server ");
	 createConnection(rhost,rport,rdb, ruser, rpasswd);

	 String qq= " update _rdb_bdr.tc_process set n_operation='shutdown' ;";
        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();
            currentStatement.execute(qq);

        } catch (SQLException e) {
                    e.printStackTrace();
           }
    	Date date = new Date();

	System.out.println("Shutting down Replication Server for node " + node + " " + date);
	while (isProcessIdRunning(node,cmd_ps))  {
		System.out.println("Shutting down" + "  " + date);
		 TimeUnit.MILLISECONDS.sleep(1000L);
	}	
	System.out.println("Shutdown !!");

 return false;
}


private boolean statusNode() throws SQLException, Exception {
	System.out.println("Reading Status from _rdb_bdr.tc_process");
	createConnection(rhost,rport,rdb, ruser, rpasswd);


         String qq= " select n_id , n_name , n_shouldbe , n_state , n_operation , n_type ,n_mstr, n_dateop, n_datecrea, n_pid from _rdb_bdr.tc_process ;";

         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
		while ( rs.next()) {
			System.out.println("----------------------------------------------------------------------------------------------");
			System.out.println("Status node  "+  rs.getString(7)+ " of type " +  rs.getString(6) );
			System.out.println("id:"+ rs.getInt(1)+" should_be:" + rs.getString(3) + " status:" +  rs.getString(4)+ " pending_op:" +  rs.getString(5)+ " last_op:" + rs.getDate(8) + rs.getTime(8) );
		}
                 //System.out.println("rs:"+ rs.next() );
                if (null != rs && rs.next())
                 return  rs.getBoolean(1);
            }
        }

        return false;
}


private boolean checkDbExist(String db) throws  Exception {
         String qq= " SELECT EXISTS(select 1 from pg_database where datname='"+db +"') ; ";

         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
		 //System.out.println("rs:"+ rs.next() );
		if (null != rs && rs.next()) 
		 return  rs.getBoolean(1);
            }
        }
	
	return false;
}

    private boolean checkIsMasterActive(String primario) throws  Exception {
        //createConnection(rhost,rport,rdb, ruser, rpasswd);
        //System.out.println("check primary  exists:" + primary);
        String qq= " SELECT EXISTS(select 1 from _rdb_bdr.tc_process where n_type='M' and n_state = 'start') ; ";
        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (null != rs && rs.next())

                    return  rs.getBoolean(1);
            }
        }

        return false;
    }


    private boolean checkIsSlaveActive(String primario) throws  Exception {
        //createConnection(rhost,rport,rdb, ruser, rpasswd);
        //System.out.println("check primary  exists:" + primary);
        String qq= " SELECT EXISTS(select 1 from _rdb_bdr.tc_process where n_type='S' and n_state = 'start') ; ";
        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (null != rs && rs.next())

                    return  rs.getBoolean(1);
            }
        }

        return false;
    }

    private boolean checkIsPrimary(String primario) throws  Exception {
        //createConnection(rhost,rport,rdb, ruser, rpasswd);
        //System.out.println("check primary  exists:" + primary);
        String qq= " SELECT EXISTS(select 1 from _rdb_bdr.tc_process where n_type='M' and n_mstr = '"+primario +"') ; ";
        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (null != rs && rs.next())

                    return  rs.getBoolean(1);
            }
        }

        return false;
    }

private boolean checkExistsPrimary(String primario) throws  Exception {
	//createConnection(rhost,rport,rdb, ruser, rpasswd);
    //System.out.println("check primary  exists:" + primary);
	String qq= " SELECT EXISTS(select 1 from _rdb_bdr.tc_process where n_type='S' and n_mstr = '"+primario +"') ; ";
	 try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (null != rs && rs.next())

                 return  rs.getBoolean(1);
            }
        }

        return false;
}



private boolean checkSchemaRdbExist(String db) throws  Exception {
         String qq= "SELECT EXISTS(select 1 from information_schema.schemata where  schema_name = '_rdb_bdr')";

         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                 //System.out.println("rs:"+ rs.next() );
                if (null != rs && rs.next()) {
                    System.out.println("Check Schema _rdb_bdr exist on db "+db +" :" + rs.getBoolean(1));
                         return rs.getBoolean(1);
                }
            }
        }

        return false;
}


    private boolean dropSeqRdb(String db, String node) throws  Exception {
        System.out.println( "> Drop sequences _rdb_bdr.walq__"+node+"% in database " + db + " !");

        // String qq = " SELECT EXISTS(select 1 from pg_class c,pg_namespace n where  c.relnamespace= n.oid and n.nspname='_rdb_bdr' and relname like 'walq__"+node+"%')";
        String qq = "SELECT n.nspname as \"Schema\", c.relname as \"Name\",\n" +
                "          CASE c.relkind WHEN 'r' THEN 'table' WHEN 'v' THEN 'view' WHEN 'm' THEN 'materialized view' WHEN 'i' THEN 'index' WHEN 'S' THEN 'sequence' WHEN 's' THEN 'special' WHEN 'f' THEN 'foreign table' WHEN 'p' THEN 'table' END as \"Type\",\n" +
                "          pg_catalog.pg_get_userbyid(c.relowner) as \"Owner\"\n" +
                "        FROM pg_catalog.pg_class c\n" +
                "             LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace\n" +
                "        WHERE c.relkind IN ('S')\n" +
                "              AND n.nspname !~ '^pg_toast'\n" +
                "          AND c.relname OPERATOR(pg_catalog.~) '^(walq__"+node+".*)$'\n" +
                "          AND n.nspname OPERATOR(pg_catalog.~) '^(_rdb_bdr)$'\n" +
                "        ORDER BY 1,2";
        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                Statement  currentStatement = null;
                currentStatement = connection.createStatement();
                while (rs.next()) {
                    System.out.println( rs.getString(2));
                    String q2= " drop sequence _rdb_bdr."+rs.getString(2);
                    try {

                        currentStatement.execute(q2);

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                //System.out.println("rs:"+ rs.next() );
                if (null != rs && rs.next())
                    return  rs.getBoolean(1);
            }
        }

        return false;
    }

private boolean dropTableRdb(String db, String node) throws  Exception {
        System.out.println( "> Drop tables _rdb_bdr.walq__"+node+"% in database " + db + " !");

       // String qq = " SELECT EXISTS(select 1 from pg_class c,pg_namespace n where  c.relnamespace= n.oid and n.nspname='_rdb_bdr' and relname like 'walq__"+node+"%')";
        String qq = "SELECT n.nspname as \"Schema\", c.relname as \"Name\",\n" +
                "          CASE c.relkind WHEN 'r' THEN 'table' WHEN 'v' THEN 'view' WHEN 'm' THEN 'materialized view' WHEN 'i' THEN 'index' WHEN 'S' THEN 'sequence' WHEN 's' THEN 'special' WHEN 'f' THEN 'foreign table' WHEN 'p' THEN 'table' END as \"Type\",\n" +
                "          pg_catalog.pg_get_userbyid(c.relowner) as \"Owner\"\n" +
                "        FROM pg_catalog.pg_class c\n" +
                "             LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace\n" +
                "        WHERE c.relkind IN ('r','p','s','')\n" +
                "              AND n.nspname !~ '^pg_toast'\n" +
                "          AND c.relname OPERATOR(pg_catalog.~) '^(walq__"+node+".*)$'\n" +
                "          AND n.nspname OPERATOR(pg_catalog.~) '^(_rdb_bdr)$'\n" +
                "        ORDER BY 1,2 desc";
        // 17022021 aggiungo desc nella query per droppare prima le partitioned che la main table altrimenti da errore
        // perchè quando togli la main togli anche tutte le partitioned (ci vorrebbe una drop puntuale
        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                Statement  currentStatement = null;
                currentStatement = connection.createStatement();
                while (rs.next()) {
                    System.out.println( rs.getString(2));
                    String q2= " drop table _rdb_bdr."+rs.getString(2);
                    try {

                        currentStatement.execute(q2);

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                //System.out.println("rs:"+ rs.next() );
                if (null != rs && rs.next())
                    return  rs.getBoolean(1);
            }
        }

        return false;
    }


private boolean checkTableRdbExist(String db, String node, String schema ) throws  Exception {
	 System.out.println( "> Checking existance tables _rdb_bdr.walq__"+node+"% in database " + db + " exists!");

     //   String qq = " SELECT EXISTS(select 1 from pg_class c,pg_namespace n where  c.relnamespace= n.oid and n.nspname='_rdb_bdr' and relname like 'walq__"+node+"%')";
    String qq = " SELECT EXISTS(select 1 from pg_class c,pg_namespace n where  c.relnamespace= n.oid and n.nspname='"+ schema +"' or relname like 'walq__"+node+"%')";
         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                 //System.out.println("rs:"+ rs.next() );
                if (null != rs && rs.next())
                 return  rs.getBoolean(1);
            }
        }

        return false;
}



private boolean checkPublExist() throws  Exception {
         String qq= " SELECT EXISTS(select 1 from  pg_publication where pubname = '"+ node + "_publ')";

         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                 //System.out.println("rs:"+ rs.next() );
                if (null != rs && rs.next())
                 return  rs.getBoolean(1);
            }
        }

        return false;
}


    private boolean skipXid(String lsntoskip ,long xidtoskip) throws  Exception {

        String qq= "insert into _rdb_bdr.walq__"+node+"_xid  (xid_from_queue, xid_current, lsn, dateop )  values  (-1 , "+xidtoskip+", now() ) " ;
        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();
            currentStatement.execute(qq);


        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private boolean goToNext(long wid_offset,long xid_offset) throws  Exception {

        String qq= "update _rdb_bdr.walq__"+primary+"_offset set last_offset ="+wid_offset+" , xid_offset="+xid_offset+" , dateop=now(); ";
        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();
            currentStatement.execute(qq);


        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }


        return true;
    }

    private long viewXidtoSet( long xidToSet) throws  Exception {
        long next_wid=-1;
        long next_xid;
        //String qq =" select substring(data,1,100) as data_substr, substring(to_char(dateop,'YYYY-MM-DD HH:MI:SS MS'),1,23) as dateop,wid,xid from _rdb_bdr.walq__"+primary+" where xid ="+xidToSet+" order by wid  desc limit 10  ";
        String qq =" select substring(data,1,100) as data_substr, substring(to_char(dateop,'YYYY-MM-DD HH:MI:SS MS'),1,23) as dateop,wid,xid from _rdb_bdr.walq__"+primary+" where xid ="+xidToSet+" order by wid  asc limit 1  ";
        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {

                while ( rs.next()) {
                    System.out.println(" ");
                     next_wid =rs.getLong(3);
                     next_xid =rs.getLong(4);
                    System.out.println(" ");
                    System.out.println("                                         Data Substr                                                  |         dateop           |      wid         |    xid       ");
                    System.out.println( rs.getString(1) +" |  " + rs.getString     (2)+" |      " + next_wid+"          |     " + next_xid   );
                    System.out.println("---------------------------------------------------------------------------------------");

                }
            }
        }
         return next_wid;
    }

    private boolean viewMarkerNext(boolean isNext , long xidToSet) throws  Exception {
    String qq =" select substring(data,1,100) as data_substr, substring(to_char(dateop,'YYYY-MM-DD HH:MI:SS MS'),1,23) as dateop,wid,xid from _rdb_bdr.walq__"+primary+" where wid > (select last_offset from _rdb_bdr.walq__"+primary+"_offset ) order by wid limit 1  ";

    try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
        try (ResultSet rs = preparedStatement.executeQuery()) {

            while ( rs.next()) {
                System.out.println(" ");
                if (!isNext) {
                    System.out.println("View data next to marker (next record that will be processed) -  the one you want to skip:");
                } else {
                    System.out.println("View data next to marker (next record that will be processed) -  the one you want to skip is:"+ xidToSet);
                }
                long next_wid =rs.getLong(3);
                long next_xid =rs.getLong(4);
                System.out.println(" ");
                System.out.println("                                         Data Substr                                                  |         dateop           |      wid         |    xid       ");
                System.out.println( rs.getString(1) +" |  " + rs.getString     (2)+" |      " + next_wid+"          |     " + next_xid   );
                System.out.println("---------------------------------------------------------------------------------------");

                if (isNext) {
                       next_wid=viewXidtoSet(xidToSet);
                       next_xid=xidToSet;

                }

                System.out.println("You are going to set marker wid:"+next_wid+" - xid:"+next_xid +" on  consumer " + node + "  for primary  " + primary);
                if (askYesNo("Do you wish to proceed  ? (Y/N): ")) {

                } else {
                    throw new Exception(" Exit");
                }
                if (                goToNext(next_wid, next_xid) ) {
                    System.out.println("Marker set  wid:"+next_wid+" - xid:"+next_xid + " on  consumer " + node + " for primary  " + primary);
                } else {
                    throw new Exception(" ERROR setting   marker wid:"+next_wid+" - xid:"+next_xid +" on  consumer " + node + " for primary  " + primary);

                }
                return true;
            }



        }
    }

    return false ;
}



    private boolean viewMarkerAround() throws  Exception {

        String qq = "select '<< CURRENT MARKER >> ********************************************************************************' ,  substring(to_char(dateop,'YYYY-MM-DD HH:MI:SS MS'),1,23) as dateop,last_offset as wid, xid_offset as xid  " +
                " from _rdb_bdr.walq__"+primary+"_offset " +
                " union " +
                " select substring(data,1,100) as data_substr, substring(to_char(dateop,'YYYY-MM-DD HH:MI:SS MS'),1,23) as dateop,wid,xid from _rdb_bdr.walq__"+primary+" where wid >= (select last_offset from _rdb_bdr.walq__"+primary+"_offset ) order by wid limit 10  ";

        try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                //System.out.println("rs:"+ rs.next() );
                System.out.println(" ");
                System.out.println("                                         Data Substr                                                  |         dateop           |      wid         |    xid       ");

                while ( rs.next()) {
                    System.out.println( rs.getString(1) +" |  " + rs.getString     (2)+" |      " + rs.getLong(3)+"          |     " + rs.getLong(4)   );
                }


                if (null != rs && rs.next())
                    return  rs.getBoolean(1);
            }
        }

        return true ;
    }


private boolean checkSubExist() throws  Exception {
         String qq= " SELECT EXISTS(select 1 from  pg_subscription where subname = '"+ node + "_subs_" + primary +"')";

         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                 //System.out.println("rs:"+ rs.next() );
                if (null != rs && rs.next())
                 return  rs.getBoolean(1);
            }
        }

        return false;
}



private boolean checkReplSlotExist(String slot_name ) throws  Exception {
        // String qq= " SELECT EXISTS(select 1 from  pg_replication_slots  where slot_name = 'rdb_"+ node + "_bdr')";
    // 16022021 String qq= " SELECT EXISTS(select 1 from  pg_replication_slots  where slot_name = '"+ slot_name + "')";
    String qq= " SELECT EXISTS(select 1 from  pg_replication_slots  where slot_name like '"+ slot_name + "')";
         try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
            try (ResultSet rs = preparedStatement.executeQuery()) {
                 //System.out.println("rs:"+ rs.next() );
                if (null != rs && rs.next())
                 return  rs.getBoolean(1);
            }
        }

        return false;
}



private boolean alterRole(String db,String user)  {
         String qq= " ALTER ROLE " + user +" IN DATABASE " + db +"   SET search_path TO _rdb_bdr,'public';";
	try {
            Statement  currentStatement = null;
	    currentStatement = connection.createStatement();
            currentStatement.execute(qq);

       	} catch (SQLException e) {
                    e.printStackTrace();
                }


        return false;
}

private boolean createPublication()  {
         String qq= " create publication "+node+"_publ  WITH (publish = 'insert');";
        /*String qq= " create publication "+node+"_publ for table _rdb_bdr.walq__"+node+" WITH (publish = 'insert');";
                 */
        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();
            currentStatement.execute(qq);

        } catch (SQLException e) {
                    e.printStackTrace();
                }


        return false;
}

private boolean partitionedTables()  {

  /* 15022021
    String qq= " select upper(to_hex(generate_series(_rdb_bdr.hex2dec( substring(a.lsn::text,1,position('/' in a.lsn::text)-1 ) )," +
           " _rdb_bdr.hex2dec( substring(b.lsn::text,1,position('/' in b.lsn::text)-1 ) ) "+
             "    ) ) )     from _rdb_bdr.walq__"+primary +" a , _rdb_bdr.walq__"+primary +" b " +
    "where a.wid =(select min(wid) from _rdb_bdr.walq__"+primary +") and " +
    "b.wid = (select max(wid) from _rdb_bdr.walq__"+primary +"		) ";


    SELECT substring(c.oid::pg_catalog.regclass,1, position('stg_' in  c.oid::pg_catalog.regclass) +length('stg_') )
    FROM pg_catalog.pg_class c, pg_catalog.pg_inherits i      WHERE c.oid=i.inhrelid AND i.inhparent =
            (SELECT c.oid             FROM pg_catalog.pg_class c                  LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
    WHERE c.relname OPERATOR(pg_catalog.~) '^(walq__stg)$'              AND n.nspname OPERATOR(pg_catalog.~) '^(_rdb_bdr)$')
    ORDER BY c.oid::pg_catalog.regclass::pg_catalog.text
    ;
*/

    String qq = " SELECT upper(substring(c.relname,position('"+primary+"_' in  c.relname) +length('"+primary+"_') ) )" +
            "     FROM pg_catalog.pg_class c, pg_catalog.pg_inherits i " +
            "     WHERE c.oid=i.inhrelid AND i.inhparent = " +
            "    (SELECT c.oid " +
            "            FROM pg_catalog.pg_class c " +
            "                 LEFT JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace" +
            "            WHERE c.relname OPERATOR(pg_catalog.~) '^(walq__" + primary + ")$'" +
            "              AND n.nspname OPERATOR(pg_catalog.~) '^(_rdb_bdr)$') " +
            "     ORDER BY c.oid::pg_catalog.regclass::pg_catalog.text";

    try {

            PreparedStatement sps = connection.prepareStatement(qq);
            ResultSet srs = sps.executeQuery();

        createConnection(rhost,rport,rdb, ruser, rpasswd);
        while  (srs.next()) {

            srs.getString(1);

           /* 15022021 String qq2 = " SELECT EXISTS(select 1  from pg_class where relname=lower('walq__" + primary + "_" + srs.getString(1) + "')) ";
            */
            String qq2 = " SELECT EXISTS(select 1  from pg_class where relname=lower('"+ srs.getString(1) + "')) ";

            try (PreparedStatement preparedStatement = connection.prepareStatement(qq2)) {
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (null != rs && rs.next())
                        if (rs.getBoolean(1)) {
                            System.out.println("Coordinate consumer " + node + " slave of " + primary + " - Current WAL LSN:" + srs.getString(1) + "- EXIST");
                        } else {
                            System.out.println("Coordinate consumer " + node + " slave of " + primary + " - Current WAL LSN:" + srs.getString(1) + "- NOT EXIST");

                            try {
                                Statement st = connection.createStatement();

                                st.execute("CREATE TABLE iF NOT EXISTS  _rdb_bdr.walq__" + primary + "_" + srs.getString(1) + " partition of _rdb_bdr.walq__" + primary + " for values in ('" + srs.getString(1) + "')");
                                st.execute("ALTER TABLE _rdb_bdr.walq__" + primary + "_" + srs.getString(1) + "  ADD CONSTRAINT  walq__" + primary + "_" + srs.getString(1) + "_pkey PRIMARY KEY (wid)");
                                st.execute("CREATE INDEX walq__" + primary + "_" + srs.getString(1) + "_idx ON _rdb_bdr.walq__" + primary + "_" + srs.getString(1) + " USING btree (xid, lsn)");
                                st.close();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                }
            }


        }

        } catch (SQLException e) {
            e.printStackTrace();
        }


        return false;
    }


private boolean createSubscription()  {

         String qq= " create subscription "+node+"_subs_"+primary+" CONNECTION 'host="+prhost+" port="+prport+" user="+pruser+" password="+prpasswd+" dbname="+prdb+"' PUBLICATION "+primary+"_publ WITH (connect = true, slot_name = '"+primary+"_publ_"+node+"')";
        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();
            currentStatement.execute(qq);

        } catch (SQLException e) {
                    e.printStackTrace();
                }


        return false;
}

    private boolean dropSchema() throws  Exception {
        String q1=" drop schema _rdb_bdr  cascade";

        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();
            currentStatement.execute(q1);
            System.out.println("Schema _rdb_bdr dropped ");


        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }   finally {
        if (connection != null) {
            connection.close();
        }
    }


        return true;
    }


    private boolean dropRdbDatabase() {
        String q1=" drop database  rdb_db__"+node;

        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();
            currentStatement.execute(q1);
            System.out.println("DB rdb_db__"+node+" dropped ");


        } catch (SQLException e) {
            e.printStackTrace();
        }


        return false;
    }

    private boolean dropPublication()  {
        String q1=" drop publication "+node+"_publ";
        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();

            currentStatement.execute(q1);
            System.out.println("Publication  " + node + "__publ:  dropped ");

    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }


        return true;
}

    private boolean dropSubscription() throws Exception {
        String q1=" alter subscription "+node+"_subs_"+primary+" disable";
        String q2=" alter subscription "+node+"_subs_"+primary+"  set (slot_name=none)";
        String q3=" drop subscription "+node+"_subs_"+primary+" ";
        String qq = "select subconninfo from pg_subscription where subname = '"+node+"_subs_"+primary+"' and subenabled=false ";
        String conninfo = null;
        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();

            currentStatement.execute(q1);
            System.out.println("subscription  " + node + "_subs_" + primary + "  for publication " + primary + "_publ:  disabled ");
            currentStatement.execute(q2);
            System.out.println("subscription  " + node + "_subs_" + primary + "  for publication " + primary + "_publ:  set (slot_name=none) ");

            try (PreparedStatement preparedStatement = connection.prepareStatement(qq)) {
                try (ResultSet rs = preparedStatement.executeQuery()) {

                    if (null != rs && rs.next())
                         conninfo =  rs.getString(1);
                }
            }

            if (conninfo != null) {
                // createConnection(rhost,rport,rdb, ruser, rpasswd);
                currentStatement.execute(q3);
                System.out.println("subscription  " + node + "_subs_" + primary + "  for publication " + primary + "_publ:  dropped ");
            }  else {
                System.out.println( "Error encounter in  subscription drop ");
                System.out.println( "Manually drop subscription "+node+"_subs_"+primary+" and corresponding replication_slot ");

                throw new Exception("Unset need DBA manual work on database to clean from TCapture structures! Exit ");

            }




        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }


        return true;
    }

    public void createLogicalReplicationSlot(String slotName, String outputPlugin) throws InterruptedException, SQLException, TimeoutException {
        //drop previous slot
        dropReplicationSlot(connection, slotName);

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement("SELECT * FROM pg_create_logical_replication_slot(?, ?)")) {
            preparedStatement.setString(1, slotName);
            preparedStatement.setString(2, outputPlugin);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    //LOGGERT.trace("Slot Name: " + rs.getString(1));

                    //LOGGERT.trace(  mythread+":" + "Slot Name: " + rs.getString(1));
                    //LOGGERT.trace(  mythread+":" + "Xlog Position: " + rs.getString(2));
                    //LOGGERT.trace("Xlog Position: " + rs.getString(2));
                }
            }

        }
    }

    public void dropReplicationSlot(Connection connection, String slotName)
            throws SQLException, InterruptedException, TimeoutException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "select pg_terminate_backend(active_pid) from pg_replication_slots "
                        + "where active = true and slot_name = ?")) {
            preparedStatement.setString(1, slotName);
            preparedStatement.execute();
        }

        waitStopReplicationSlot(connection, slotName);

        try (PreparedStatement preparedStatement = connection.prepareStatement("select pg_drop_replication_slot(slot_name) "
                + "from pg_replication_slots where slot_name = ?")) {
            preparedStatement.setString(1, slotName);
            preparedStatement.execute();
        }
    }

    public boolean isReplicationSlotActive(Connection connection, String slotName)
            throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("select active from pg_replication_slots where slot_name = ?")) {
            preparedStatement.setString(1, slotName);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    private void waitStopReplicationSlot(Connection connection, String slotName)
            throws InterruptedException, TimeoutException, SQLException {
        long startWaitTime = System.currentTimeMillis();
        boolean stillActive;
        long timeInWait = 0;

        do {
            stillActive = isReplicationSlotActive(connection, slotName);
            if (stillActive) {
                TimeUnit.MILLISECONDS.sleep(100L);
                timeInWait = System.currentTimeMillis() - startWaitTime;
            }
        } while (stillActive && timeInWait <= 30000);

        if (stillActive) {
            throw new TimeoutException("Wait stop replication slot " + timeInWait + " timeout occurs");
        }
    }

private boolean   dropReplicationSlot(String slot_name ) throws  Exception {
   //String q1= " SELECT EXISTS(select 1 from  pg_replication_slots  where slot_name =  '"+primary+"_publ_"+node+"')";
    String q1= " SELECT EXISTS(select 1 from  pg_replication_slots  where slot_name =  '"+slot_name+"')";
    System.out.println("Dropping replication slot :" +slot_name );
    String qq=null;
    try (PreparedStatement preparedStatement = connection.prepareStatement(q1)) {
        try (ResultSet rs = preparedStatement.executeQuery()) {
            //System.out.println("rs:"+ rs.next() );
            if (null != rs && rs.next()) {
                qq = " SELECT pg_drop_replication_slot( '" + slot_name + "') ";

                try {
                    Statement currentStatement;
                    currentStatement = connection.createStatement();
                    currentStatement.execute(qq);

                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return rs.getBoolean(1);
            }
        }
    }

            return false;
    }

    private void finalSetup_events_ddl()  {
        String qq2= "INSERT INTO dba.__events_ddl ( wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp )"+
                " VALUES "+
                " ( "+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()")+", txid_current(), CURRENT_USER, '"+ node +"','MASTER', 'DROP NODE',  NOW()) "+
                " ON CONFLICT (ddl_id,ddl_origin) DO UPDATE SET wal_lsn="+(((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +", creation_timestamp=NOW();";

        try {
            Statement  currentStatement ;
            currentStatement = connection.createStatement();

            currentStatement.execute(qq2);

        } catch (SQLException e) {
            e.printStackTrace();
        }


        // return false;
    }


    private void finalSetupCons()  {
        //String qq2=  " update _rdb_bdr.tc_process set   n_shouldbe='disable'  where n_type= 'S' and n_mstr ='"+ primary +"'"  ;
        String qq2=  " delete from  _rdb_bdr.tc_process   where n_type= 'S' and n_mstr ='"+ primary +"'"  ;

        try {
            Statement  currentStatement ;
            currentStatement = connection.createStatement();

            currentStatement.execute(qq2);

        } catch (SQLException e) {
            e.printStackTrace();
        }


        // return false;
    }

    private void finalSetup()  {
        // String qq2=  " update _rdb_bdr.tc_process set   n_shouldbe='disable'  where n_type= 'M'"  ;
        String qq2=  " delete from  _rdb_bdr.tc_process   where n_type= 'M' and n_mstr ='"+ node +"'"  ;

        try {
            Statement  currentStatement ;
            currentStatement = connection.createStatement();

            currentStatement.execute(qq2);

        } catch (SQLException e) {
            e.printStackTrace();
        }


        // return false;
    }


    private void finalSetupCons_events_ddl()  {
        String qq2= "INSERT INTO dba.__events_ddl ( wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp )"+
                " VALUES "+
                " ("+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +", txid_current(), CURRENT_USER, '"+ node +"<-"+primary+"','SLAVE', 'DROP NODE',  NOW()) "+
                " ON CONFLICT (ddl_id,ddl_origin) DO UPDATE SET wal_lsn="+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +", creation_timestamp=NOW();";

        try {
            Statement  currentStatement ;
            currentStatement = connection.createStatement();

            currentStatement.execute(qq2);

        } catch (SQLException e) {
            e.printStackTrace();
        }


        // return false;
    }



private void initialSetupCons_events_ddl()  {
    String qq2= "INSERT INTO dba.__events_ddl ( wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp )"+
            " VALUES "+
            " ("+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()")+", txid_current(), CURRENT_USER, '"+node+"<-"+ primary +"','SLAVE', 'CREATE NODE',  NOW()) "+
            " ON CONFLICT (ddl_id,ddl_origin) DO UPDATE SET wal_lsn="+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()")+", creation_timestamp=NOW();";

        try {
            Statement  currentStatement ;
            currentStatement = connection.createStatement();

            currentStatement.execute(qq2);

        } catch (SQLException e) {
            e.printStackTrace();
        }


        // return false;
    }


private void createReplicationSlot()  {
         String qq= " SELECT * FROM pg_create_logical_replication_slot('rdb_"+node+"_bdr', 'rdblogdec')";
   //10.02.2021      String qq2= " INSERT INTO _rdb_bdr.tc_monit   (\"mstr_id\", \"tx_id\",\"tx_src_dateop\",\"tx_lsn\")
    // values ('master',1,current_timestamp, pg_current_wal_lsn() )\n" +
    //             "ON CONFLICT (mstr_id) DO UPDATE SET tx_id=1, tx_src_dateop=current_timestamp ,tx_lsn=pg_current_wal_lsn() ";

    String qq2= "INSERT INTO dba.__events_ddl ( wal_lsn, wal_txid, ddl_user, ddl_object, ddl_type, ddl_command, creation_timestamp )"+
    " VALUES "+
    " ("+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +", txid_current(), CURRENT_USER, '"+ node +"','MASTER', 'CREATE NODE',  NOW()) "+
    " ON CONFLICT (ddl_id,ddl_origin) DO UPDATE SET wal_lsn="+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()")+", creation_timestamp=NOW()";
        try {
            Statement  currentStatement ;
            currentStatement = connection.createStatement();
            connection.setAutoCommit(false);
           // currentStatement.execute(qq);
            // currentStatement.execute(qq2);
            currentStatement.addBatch(qq);
            currentStatement.addBatch(qq2);
            currentStatement.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
                    e.printStackTrace();
                }


       // return false;
}



private boolean initialSetup()  {
    String qqC= " insert into _rdb_bdr.tc_process (n_id,n_name,n_shouldbe,n_state,n_operation,n_type,n_mstr,n_dateop,n_datecrea,n_pid) values (0,'" +node + "', 'up','stop','managed','C','"+node+"', now(),now(),-1)  ON CONFLICT ON CONSTRAINT tc_process_pkey DO NOTHING; ";
    String qq= " insert into _rdb_bdr.tc_process (n_id,n_name,n_shouldbe,n_state,n_operation,n_type,n_mstr,n_dateop,n_datecrea,n_pid) values (0,'" +node + "', 'up','stop','managed','M','"+node+"', now(),now(),-1)  ON CONFLICT ON CONSTRAINT tc_process_pkey DO NOTHING;";
         String qqH= " insert into _rdb_bdr.tc_process (n_id,n_name,n_shouldbe,n_state,n_operation,n_type,n_mstr,n_dateop,n_datecrea,n_pid) values (0,'" +node + "', 'up','stop','managed','H','"+node+"', now(),now(),-1)  ON CONFLICT ON CONSTRAINT tc_process_pkey DO NOTHING; ";

        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();
            currentStatement.execute(qqC);
            currentStatement.execute(qq);
            currentStatement.execute(qqH);

        } catch (SQLException e) {
                    e.printStackTrace();
                }
        return false;
}


private boolean initialSetupCons()  {
   // String qqC= " insert into _rdb_bdr.tc_process (n_id,n_name,n_shouldbe,n_state,n_operation,n_type,n_mstr,n_dateop,n_datecrea,n_pid) values (0,'" +node + "', 'up','stop','managed','C','"+node+"', now(),now(),-1)  ON CONFLICT ON CONSTRAINT tc_process_pkey DO NOTHING; ";
  //  String qq= " insert into _rdb_bdr.tc_process (n_id,n_name,n_shouldbe,n_state,n_operation,n_type,n_mstr,n_dateop,n_datecrea,n_pid) values (0,'" +node + "', 'up','stop','managed','S','"+primary+"', now(),now(),-1)  ON CONFLICT ON CONSTRAINT tc_process_pkey DO NOTHING;";
    String qq= " insert into _rdb_bdr.tc_process (n_name,n_shouldbe,n_state,n_operation,n_type,n_mstr,n_dateop,n_datecrea,n_pid) values ('" +node + "', 'up','stop','managed','S','"+primary+"', now(),now(),-1)  ON CONFLICT ON CONSTRAINT tc_process_pkey DO NOTHING;";
	String qqO = "insert into _rdb_bdr.walq__"+primary+"_offset (src_topic_id,last_offset,xid_offset,lsn_offset,dateop) values ('"+node+"',0,0, "+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()") +",current_timestamp)  ON CONFLICT ON CONSTRAINT walq__"+primary+"_offset_pkey DO NOTHING;";

        try {
            Statement  currentStatement = null;
            currentStatement = connection.createStatement();
           // currentStatement.execute(qqC);
            currentStatement.execute(qq);
            currentStatement.execute(qqO);

        } catch (SQLException e) {
                    e.printStackTrace();
                }
        return false;
}




    private void executeSqlScript(Connection conn, File inputFile, boolean replaceSlave) {

    String delimiter = "#";
    String rawStatement;
    Scanner scanner;
    try {
        scanner = new Scanner(inputFile).useDelimiter(delimiter);

    Statement currentStatement = null;
    while(scanner.hasNext()) {
        //String rawStatement = scanner.next() + delimiter;
	if ( replaceSlave ) {
        	rawStatement = scanner.next().replaceAll("master",primary) ;
	} else {
        	rawStatement = scanner.next().replaceAll("master",node) ;
        }	
        try {
            currentStatement = conn.createStatement();
            currentStatement.execute(rawStatement);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (currentStatement != null) {
                try {
                    currentStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            currentStatement = null;
        }
    }
	scanner.close();
	  } catch (IOException ex) {
            ex.printStackTrace();

        }

}

private boolean enableNodeSlave( String msg ) throws Exception {
        try {
		//createConnection(rhost,rport,rdb, ruser, rpasswd);
                 String qq= null;
                 String shouldBe= "NA";
                 String operation= "NA";
                if (msg.equals("disable")) shouldBe = "down";
                if (msg.equals("enable")) shouldBe = "up";
                if (msg.equals("start")) operation= "start";
                if (msg.equals("stop")) operation= "stop";

		if ( operation.equals("NA")) {
                	System.out.println(    " Set node 'should be' status for consumer of node "+primary  + "on "+ rdb + ".tc_process to " + shouldBe);
                        qq= " update _rdb_bdr.tc_process set   n_shouldbe='"+shouldBe +"'  where n_type= 'S' and n_mstr = '"+ primary +"'" ;
                   } else {
                        System.out.println(   operation + " node consumer for producer "  + primary);
                        qq= " update _rdb_bdr.tc_process set  n_operation = '" + operation + "'  where n_type=  'S' and n_mstr = '"+ primary +"'" ;

                   }
	
                  //qq= " update _rdb_bdr.tc_process set n_shouldbe='"+shouldBe +"' where n_type='S' and n_mstr = '"+ primary +"'";
                Statement  currentStatement = null;
                currentStatement = connection.createStatement();
                currentStatement.execute(qq);


        } catch (Exception ex) {
                 System.out.println( ex.getMessage());
                 ex.printStackTrace();
        } finally {
                if (connection != null) {
                        connection.close();
                }
        }
         return true;
}



private boolean enableNode(boolean isMonitor, String msg) throws Exception {
	try {
		createConnection(rhost,rport,rdb, ruser, rpasswd);
		 String qq= null;
		 String qq_type= "'M' ";
		 String shouldBe= "NA";
		 String operation= "NA";
		if (msg.equals("disable") ) shouldBe = "down";
		if (msg.equals("enable")) shouldBe = "up";
		if (msg.equals("start")) operation= "start";
		if (msg.equals("stop")) operation= "stop";
		if (isMonitor) qq_type = "'H' ";  
		
		    if ( operation.equals("NA")) {
                	System.out.println(    " Set node 'should be' status for node type "+qq_type+" on "+ rdb + ".tc_process to "+ shouldBe);
		  	qq= " update _rdb_bdr.tc_process set   n_shouldbe='"+shouldBe +"'  where n_type= " + qq_type ;
		   } else {
			System.out.println(   operation + " node type " +qq_type );
			qq= " update _rdb_bdr.tc_process set  n_operation = '" + operation + "'  where n_type= " + qq_type; 
		   }
		   
            	Statement  currentStatement = null;
            	currentStatement = connection.createStatement();
            	currentStatement.execute(qq);

				
	} catch (Exception ex) {
                 System.out.println( ex.getMessage());
                 ex.printStackTrace();
        } finally {
                if (connection != null) {
                        connection.close();
                }
        }
         return true;
}
	

private boolean  setupNode(boolean isForce ) throws Exception {
    System.out.println("---------------------------------------------------------------------------------------");
    System.out.println("You are going to set producer node " + node + "    ");
    if (askYesNo("Do you wish to proceed  ? (Y/N): ")) {
        System.out.println(">> Going to set  node " + node + " as   producer ");
    } else {
        throw new Exception(" Exit");
    }
	String master_rdbbdr = rdbbdr + "/sql/producer_structure_master_rdbbdr.sql";
	String rdb_rdbbdr = rdbbdr + "/sql/producer_structure_rdb_rdbbdr.sql";

	BufferedReader reader = null;
	Statement statement = null;


	try {
        System.out.println(" ");
        System.out.println("--Going to create _rdb_bdr structure on database "+ rdb+ " ..");

		createConnection(rhost,rport,rdb, ruser, rpasswd);

        try {

            if (checkSchemaRdbExist(rdb) && checkIsSlaveActive(node) ) {
                System.out.println("STOP all Slaves before setup node as new primary :" + node);
                return false;
                //throw new Exception("Invalid choice, primary not exists:" + primary);


                //throw new ParseException("Invalid arguments, not a slave for primary :"+primary);
            }
        } catch (Exception ex) {
            System.out.println( ex.getMessage());
        }


		executeSqlScript(connection , new File(rdb_rdbbdr), false);
		alterRole(rdb,ruser);
        System.out.println(" ");
        System.out.println("--Going to create publication "+node+"_publ ..");

        createPublication();
		initialSetup();

   			createConnection(host,port,db, user, passwd);
		if ((!checkSchemaRdbExist(db)) || isForce ) {
            System.out.println(" ");
            System.out.println("--Going to create dba structure on database "+ db+ " ..");

            if (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10)) {
                master_rdbbdr = rdbbdr + "/sql/producer_structure_master_rdbbdr.sql";
            } else {
                master_rdbbdr = rdbbdr + "/sql/producer_structure_master_rdbbdr_v95.sql";
            }

			executeSqlScript(connection , new File(master_rdbbdr), false);
		} else {
                        System.out.println(    "Skipped creation of _rdb_bdr structure on "+ db + " since already exits!");
                }


		alterRole(db,user);

        System.out.println(" ");
        System.out.println("--Going to create replication slot database rdb_"+node+"_bdr ..");
		createReplicationSlot();

	

	 } catch (Exception ex) {
       		 System.out.println( ex.getMessage());
       		 ex.printStackTrace();
   	} finally {
		if (reader != null) {
			reader.close();
		}
		if (connection != null) {
			connection.close();
		}
	}
       	 return true;
}

    private boolean  unsetNode(boolean isForce ) throws Exception {
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("You are going to unset producer node " + node + "    ");
        if (askYesNo("Do you wish to proceed  ? (Y/N): ")) {
            System.out.println(">> Going to unset  node " + node + " as   producer ");
        } else {
            throw new Exception(" Exit");
        }

        createConnection(rhost,rport,rdb, ruser, rpasswd);

        try {
            if ( !isForce && (!checkSchemaRdbExist(rdb) || !checkIsPrimary(node) ) ) {
                System.out.println("Invalid choice, not a master :" + node);
                return false;
                //throw new Exception("Invalid choice, primary not exists:" + primary);


                //throw new ParseException("Invalid arguments, not a slave for primary :"+primary);
            }
        } catch (Exception ex) {
            System.out.println( ex.getMessage());
        }

        try {
            if ( checkIsMasterActive(node)) {
                System.out.println("STOP the master,  is an active Master :" + node);
                return false;
                //throw new Exception("Invalid choice, primary not exists:" + primary);


                //throw new ParseException("Invalid arguments, not a slave for primary :"+primary);
            }
        } catch (Exception ex) {
            System.out.println( ex.getMessage());
        }


        System.out.println(" ");
        System.out.println("--Removing tc_process entry:Master "+node+"..");
        finalSetup();


        if (!checkReplSlotExist(node+"_publ_%") || isForce ) {
            System.out.println( "Replication slot "+node+"_publ_  NOT exists OR Forced execution");

        } else {
            System.out.println( "Replication slot "+node+ "_publ_  exists !" );
            System.exit(-1);
        }


        System.out.println(" ");
        System.out.println("--Going to drop publication..");
        Statement statement = null;

        if ( checkPublExist() || isForce ) {
            System.out.println( "Publication  " +node+"_publ for table _rdb_bdr.walq__"+node+"  exists ");

            if (dropPublication() || isForce) {


            } else {
                System.exit(-1);
            }
        } else {
            System.out.println("Publication  " +node+"_publ for table _rdb_bdr.walq__"+node+" NOT exists ");
        }


        /*
        if ((!checkSchemaRdbExist(rdb)) || isForce ) {
            System.out.println(    " Dropping  _rdb_bdr structure on "+ rdb);
            if (dropSchema() || isForce ) {
                createConnection(rhost,rport,"postgres", ruser, rpasswd);
                dropRdbDatabase();
            }
        } else {
            System.out.println(    "Skipped drop of _rdb_bdr structure on "+ rdb + " , please contact DBA !");
        }
        */
        System.out.println(" ");
        System.out.println("--Going to drop rdb_bdr.walq__"+node+"% tables on "+ rdb + "..");
        if ((checkSchemaRdbExist(rdb) && checkTableRdbExist(rdb,node,"_rdb_bdr")) || isForce ) {
            System.out.println(    "Dropping  _rdb_bdr.walq__"+node+"% tables on "+ rdb);
            dropTableRdb(rdb,node) ;
            dropSeqRdb(rdb,node);


        } else {

            System.out.println(    "Tables rdb_bdr.walq__"+node+"% structure on "+ rdb +" NOT EXISTS! ");
        }

        System.out.println(" ");
        System.out.println("--Going to drop rdb_bdr schema  on "+ rdb  + " and database " + rdb +" for producer "+node+" ..");
        if ( (checkSchemaRdbExist(rdb) && checkTableRdbExist(rdb,"","_rdb_bdr")) ) {
            System.out.println( "< Schema _rdb_bdr tables walq__% in database " + rdb + " exists!");
            System.out.println(" Skipping drop of schema  _rdb_bdr in database "+ rdb);
        } else {

            if  (checkSchemaRdbExist(rdb)) {
                if (dropSchema() || isForce) {
                    if (connection != null) {
                        connection.close();
                    }
                    createConnection(rhost, rport, "postgres", ruser, rpasswd);
                    dropRdbDatabase();
                }
            }  else{
                if (connection != null) {
                    connection.close();
                }
                createConnection(rhost, rport, "postgres", ruser, rpasswd);
                dropRdbDatabase();
            }
        }



        if (connection != null) {
            connection.close();
        }

        System.out.println(" ");
        System.out.println("--Going to drop Replication Slot ..");

        createConnection(host, port, db, user, passwd);

        if (checkReplSlotExist("rdb_" +node+"_bdr")) {
            System.out.println( "Replication slot _rdb_" +node+"_bdr   exists ");
            dropReplicationSlot("rdb_"+ node + "_bdr");
        }

        finalSetup_events_ddl();
        System.out.println( "To complete cleanup, manually execute : drop schema dba cascade;  on primary database "+db);

        if (connection != null) {
            connection.close();
        }
        /*createConnection(rhost,rport,"postgres", ruser, rpasswd);
            dropDatabase();
        */
        return true;
    }

    private boolean  setXid(boolean isNext , long xidToSet) throws Exception {
        System.out.println("---------------------------------------------------------------------------------------");
       // System.out.println("You are going  to move marker to next xid for consumer node " + node + "   from producer "+primary +" ");

        /*if (askYesNo("Do you wish to proceed  ? (Y/N): ")) {
            System.out.println(">> Going to move marker to next xid for consumer node " + node + "   from producer "+primary +" ");
        } else {
            throw new Exception(" Exit");
        }*/

        Statement statement = null;
        createConnection(rhost,rport,rdb, ruser, rpasswd);
        System.out.println(" ");
        System.out.println("Data close to the marker :");

        if (viewMarkerAround() && viewMarkerNext( isNext ,  xidToSet)) {
            System.out.println(" ");
           // System.out.println(" Transactions following the Marker FOUND for consumer  " + node + "   of producer "+ primary +" ");

        } else {
            System.out.println(" ");
            System.out.println(" NO transactions following the marker FOUND for consumer  " + node + "   of producer "+ primary +" ");
        }

        if (connection != null) {
            connection.close();
        }

        return true;
    }

    private boolean  unsetNodeCons(boolean isForce ) throws Exception {

      System.out.println("---------------------------------------------------------------------------------------");
    System.out.println("You are going to unset consumer node " + node + "   from producer "+primary +" ");
    if (askYesNo("Do you wish to proceed  ? (Y/N): ")) {
        System.out.println(">> Going to unset  node " + node + " as  consumer  for producer "+primary);
    } else {
        throw new Exception(" Exit");
    }



    Statement statement = null;
    createConnection(rhost,rport,rdb, ruser, rpasswd);
        try {

          // ???  if (!checkSchemaRdbExist(rdb) || !checkIsPrimary(node)) {
            if (!checkSchemaRdbExist(rdb) ) {
                 System.out.println("Invalid choice, not a slave for primary :" + primary);
                return false;
                //throw new Exception("Invalid choice, primary not exists:" + primary);


                //throw new ParseException("Invalid arguments, not a slave for primary :"+primary);
            }
        } catch (Exception ex) {
            System.out.println( ex.getMessage());
        }

    System.out.println(" ");
    System.out.println("--Removing tc_process entry:Slave "+node+" for master "+primary+"..");
    finalSetupCons();

    System.out.println(" ");
    System.out.println("--Going to drop subscription..");

    if (checkSubExist() || isForce) {
        if (dropSubscription() || isForce) {
            System.out.println("Subscription  " + node + "_subs_" + primary + "  for publication " + primary + "_publ  DROPPED ! ");
        } else {
            System.out.println("Error dropping subscription , exit! ");
            System.exit(-1);
        }
    } else {
        System.out.println("Subscription  " + node + "_subs_" + primary + "  for publication " + primary + "_publ  NOT exists ");
    }

    System.out.println(" ");
    System.out.println("--Going to drop rdb_bdr.walq__"+primary+"% tables on "+ rdb + "..");
    if ((checkSchemaRdbExist(rdb) && checkTableRdbExist(rdb,primary,"_rdb_bdr")) || isForce ) {
         System.out.println(    "Dropping  _rdb_bdr.walq__"+primary+"% tables on "+ rdb);

        dropTableRdb(rdb,primary) ;
        dropSeqRdb(rdb,primary);
     } else {

           System.out.println(    "Tables rdb_bdr.walq__\"+node+\"% structure on \"+ rdb NOT EXISTS! ");
    }

    System.out.println(" ");
    System.out.println("--Going to drop rdb_bdr schema  on "+ rdb  + " and database " + rdb +" for consumer "+node+" ..");
    if ( (checkSchemaRdbExist(rdb))  &&   (checkTableRdbExist(rdb,"","_rdb_bdr"))) {
        System.out.println( "< Schema _rdb_bdr tables walq__% in database " + rdb + " exists!");
            System.out.println(" Skipping drop of schema  _rdb_bdr in database "+ rdb);
    } else {
        if (connection != null) {
            connection.close();
        }
        createConnection(rhost, rport, rdb, ruser, rpasswd);
        if ((checkSchemaRdbExist(rdb))  && dropSchema() || isForce) {
            if (connection != null) {
                connection.close();
            }
            createConnection(rhost, rport, "postgres", ruser, rpasswd);
            dropRdbDatabase();

        } else {
            if (connection != null) {
                connection.close();
            }
            createConnection(rhost, rport, "postgres", ruser, rpasswd);
            dropRdbDatabase();

        }
    }

    if (connection != null) {
        connection.close();
    }

    System.out.println(" ");
    System.out.println("--Going to drop Replication Slot for removed slave subscription on master " + primary );
    createConnection(prhost, prport, prdb, pruser, prpasswd);
    if (checkReplSlotExist(primary+"_publ_"+node)) {
        System.out.println( "Replication slot _publ_"+node);
        dropReplicationSlot(primary+"_publ_"+node);
    } else {
        System.out.println( "Replication slot _publ_"+node+ " not exists !" );
    }

        createConnection(host, port, db, user, passwd);
        finalSetupCons_events_ddl();

    if (connection != null) {
        connection.close();
    }





    return true;
}
private boolean  setupNodeCons(boolean isForce ) throws Exception {
    System.out.println("---------------------------------------------------------------------------------------");
    System.out.println("You are going to set consumer node " + node + "   from producer "+primary +" ");
    if (askYesNo("Do you wish to proceed  ? (Y/N): ")) {
        System.out.println(">> Going to set  node " + node + " as  consumer  for producer "+primary);
    } else {
        throw new Exception(" Exit");
    }

    String master_rdbbdr = rdbbdr + "/sql/producer_structure_master_rdbbdr.sql";
  //  String master_rdbbdr_v95 = rdbbdr + "/sql/producer_structure_master_rdbbdr_v95.sql";
	String rdb_rdbbdr = rdbbdr + "/sql/consumer_structure_rdb_rdbbdr.sql";

        BufferedReader reader = null;
        Statement statement = null;

        try {
                	createConnection(host,port,db, user, passwd);
            if (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10)) {
                master_rdbbdr = rdbbdr + "/sql/producer_structure_master_rdbbdr.sql";
            } else {
                master_rdbbdr = rdbbdr + "/sql/producer_structure_master_rdbbdr_v95.sql";
            }


		if ((!checkSchemaRdbExist(db)) || isForce ) {
            System.out.println(" ");
            System.out.println("--Going to create dba structure on database "+ db+ " ..");

                	executeSqlScript(connection , new File(master_rdbbdr), false);
		} else {
			System.out.println(    "Skipped creation of _rdb_bdr structure on "+ db + " since already exits!");	
		}
		// set __event_ddl for slave node creation
            initialSetupCons_events_ddl();
                alterRole(db,user);
            System.out.println(" ");
            System.out.println("--Going to create _rdb_bdr structure on database "+ rdb+ " ..");

            createConnection(rhost,rport,rdb, ruser, rpasswd);
                executeSqlScript(connection , new File(rdb_rdbbdr), true);
                alterRole(rdb,ruser);

            System.out.println(" ");
            System.out.println("--Going to create partitioned _rdb_bdr.walq__"  +node+"_XX tables on database "+ rdb+ " ..");
            createConnection(prhost, prport, prdb, pruser, prpasswd);

            partitionedTables();

            System.out.println(" ");
            System.out.println("--Going to create subscription"  +node+"_subs_"+primary+" on database  "+ rdb+ " ..");

            createSubscription();
                initialSetupCons();


         } catch (Exception ex) {
                 System.out.println( ex.getMessage());
                 ex.printStackTrace();
        } finally {
                if (reader != null) {
                        reader.close();
                }
                if (connection != null) {
                        connection.close();
                }
        }
         return true;
}



private boolean  checkupNodeProd( ) throws Exception {
boolean exit_ = false;
try {
     createConnection(rhost,rport,rdb, ruser, rpasswd);
     if (checkPublExist()) {
                System.out.println( "Publication  " +node+"_publ for table _rdb_bdr.walq__"+node+" already exists ");
               // System.out.println( "Manually drop publication " +node+"_publ for table _rdb_bdr.walq__"+node+" on node "+node+" db "+rdb);
                 exit_ =true;
                throw new Exception("Setup need to run on database clean from TCapture structures! Exit ");
         }
         if (checkReplSlotExist("_rdb_" +node+"_bdr")) {
                System.out.println( "Replication slot _rdb_" +node+"_bdr  already exists ");
              //  System.out.println( "Manually drop replication slot:  select  pg_drop_replication_slot('rdb_" +node+"_bdr');  on node "+node+" db "+db);
                 exit_ =true;
                 throw new Exception("Setup need to run on database clean from TCapture structures! Exit ");
         }


   }  catch (Exception ex) {
        System.out.println( ex.getMessage());
   }  finally {
                if (connection != null) {
                        connection.close();
                }
        }

   //  System.out.println( "checkupNodeProd:" +exit_);
        return exit_;

}


private boolean  checkupNodeCons( ) throws Exception {
boolean exit_ = false;
try {
     createConnection(rhost,rport,rdb, ruser, rpasswd);
     if (checkSubExist()) {
                System.out.println( "subscription  " +node+"_subs_"+primary+"  for publication "+primary+"_publ already exists ");
                System.out.println( "Manually drop subscription" +node+"_subs_"+primary+" on node "+node+" db "+rdb);
                 exit_ =true;
                throw new Exception("Setup need to run on database clean from TCapture structures! Exit ");
         }

   }  catch (Exception ex) {
        System.out.println( ex.getMessage());
   }  finally {
                if (connection != null) {
                        connection.close();
                }
        }
    //  System.out.println( "checkupNodeCons:" +exit_);

        return exit_;

}




private boolean  checkupNode( boolean consumer) throws Exception {
boolean exit_ = false;	
    try {
	 System.out.println(	"> Checking existance of database "+ db +" on host "+host );
		createConnection(host,port,"postgres", user, passwd);
	if (checkDbExist(db)) {
		System.out.println( "< " + db + " exists!");
	} else {
	    exit_ =true;
	  throw new Exception("Invalid datatabase! Create db : create database " +db  );
	 
	}
	 System.out.println(	"> Checking existance of database "+ rdb+" on host "+rhost);
        createConnection(rhost,rport,"postgres", ruser, rpasswd);
        if (checkDbExist(rdb)) {
              System.out.println("< " +rdb + " exists!");
        } else {
	    exit_ =true;
          throw new Exception("Invalid datatabase! Create db : create database " +rdb);
        }
	connection.close();
	createConnection(host,port,db, user, passwd);
	System.out.println(    "> Checking existance of schema _rdb_bdr in database "+ db);
        //if (checkSchemaRdbExist(db)) {
        if (checkTableRdbExist(db,node,"dba")) {
	  // System.out.println( "< Schema dba tables walq__"+node+"% in database " + db + " exists!");
            System.out.println( "< Schema dba  in database " + db + " exists!");
		if (askYesNo("Do you wish to proceed anyway ? (Y/N): ")) {
			System.out.println(" skipping  creation (already exists) on schema _rdb_bdr in database "+ db);
		} else {
	       		exit_ =true;
                	throw new Exception("Setup need to run on database clean from TCapture structures! Exit");
		}
        }
	createConnection(rhost,rport,rdb, ruser, rpasswd);
        //if (checkSchemaRdbExist(rdb)) {
        if (consumer && checkTableRdbExist(rdb,primary,"_rdb_bdr")) {
	   System.out.println( "< Schema _rdb_bdr tables walq__"+primary+"% in database " + rdb + " exists!");
              if (askYesNo("Do you wish to proceed anyway ? (Y/N): ")) {
                       System.out.println(" skipping creation (already exists) on schema  _rdb_bdr in database "+ rdb);
                } else {
                        exit_ =true;
                        throw new Exception("Setup need to run on database clean from TCapture structures! Exit");
                }

        } else if ( checkTableRdbExist(rdb,node,"_rdb_bdr")) {
	   System.out.println( "< Schema _rdb_bdr tables walq__"+node+"% in database " + rdb + " exists!");
              if (askYesNo("Do you wish to proceed anyway ? (Y/N): ")) {
                       System.out.println(" skipping creation (already exists) on schema  _rdb_bdr in database "+ rdb);
                } else {
                        exit_ =true;
                        throw new Exception("Setup need to run on database clean from TCapture structures! Exit");
                }
 
	}	


   }  catch (Exception ex) { 
      	System.out.println( ex.getMessage()); 
   }  finally {
                if (connection != null) {
                        connection.close();
                }
        }

	// System.out.println( "checkupNode :" +exit_);
	return exit_;
	
}

private boolean showconfPrimary( String primary )  {
	String rdbbdrconf = null;
	rdbbdrconf = rdbbdr + "/conf/" + primary + "_rdb_bdr.conf";

	if(!Files.isRegularFile(Paths.get(rdbbdrconf))) {
                        System.out.println(rdbbdrconf + " not exists! ");
                        System.exit(-1);
                 }

	try (InputStream input = new FileInputStream(rdbbdrconf)) {

            Properties prop = new Properties();

            prop.load(input);

            phost = prop.getProperty("host");
            puser = prop.getProperty("user");
            pport = Integer.valueOf(prop.getProperty("port"));
            ppasswd = prop.getProperty("pwd");
            pdb = prop.getProperty("db");
            pnode = prop.getProperty("node");

            prhost = prop.getProperty("rhost");
            pruser = prop.getProperty("ruser");
            prport = Integer.valueOf(prop.getProperty("rport"));
            prpasswd = prop.getProperty("rpwd");
            prdb = "rdb_db__"+ primary;


            System.out.println("Configuration file: " +  rdbbdrconf + " review");
            System.out.println("Primary Database: ");
            System.out.println("node " + pnode);
            System.out.println("db " +   pdb);
            System.out.println("host " + phost);
            System.out.println("port " + pport);
            System.out.println("user " + puser);
            System.out.println("pwd " + ppasswd);
            System.out.println("");
            System.out.println("RDB database: ");
            System.out.println("rnode " + pnode);
            System.out.println("rdb " + prdb);
            System.out.println("rhost " + prhost);
            System.out.println("rport " + prport);
            System.out.println("ruser " + pruser);
            System.out.println("rpwd " + prpasswd);
            System.out.println("");


        } catch (Exception ex) {
            ex.printStackTrace();
        }


                 return true;
}



private boolean showconfNode( CommandLine cmd, String rdbbdr, boolean isServer )  throws ParseException {
	
	if ( (!isServer) && cmd.hasOption("node") && cmd.hasOption("type")) {
		nodetype = cmd.getOptionValue("type");

                         if (!(( nodetype.equals("producer")) || (nodetype.equals("consumer")) || (nodetype.equals("monitor")) )) {
                                throw new ParseException("Invalid type specified: " + nodetype );
                        }
	 } else if ( isServer) {	
		nodetype = "producer";		
		 } else { 
		  throw new ParseException("Invalid arguments, usage : TCSrvCTL "+msg+" --node --type [producer/consumer/monitor]");
           }

	    if (cmd.hasOption("node")) {
            node = cmd.getOptionValue("node");
        } else {
            throw new ParseException("Invalid arguments, usage : TCSrvCTL "+msg+" --node <node>");
        }
		
		 String rdbbdrconf = null; 
		 if (( nodetype.equals("producer"))  || (nodetype.equals("monitor"))) {
                                 rdbbdrconf = rdbbdr + "/conf/" + node + "_rdb_bdr.conf";
                        } else {
                                 rdbbdrconf = rdbbdr + "/conf/" + node + "_bdr_rdb.conf";
                        }
		if(!Files.isRegularFile(Paths.get(rdbbdrconf))) {
			System.out.println(rdbbdrconf + " not exists! ");
                 	System.exit(-1);
       		 }
		
		 try (InputStream input = new FileInputStream(rdbbdrconf)) {

            Properties prop = new Properties();

            prop.load(input);


            System.out.println("Configuration file: " +  rdbbdrconf + " review");
            System.out.println("Primary Database: ");
            System.out.println("node " + prop.getProperty("node"));
            System.out.println("db " + prop.getProperty("db"));
            System.out.println("host " + prop.getProperty("host"));
            System.out.println("port " + prop.getProperty("port"));
            System.out.println("user " + prop.getProperty("user"));
            System.out.println("pwd " + prop.getProperty("pwd"));
            System.out.println("");
            System.out.println("RDB database: ");
            System.out.println("rnode " + prop.getProperty("rnode"));
            System.out.println("rdb " + prop.getProperty("rdb"));
            System.out.println("rhost " + prop.getProperty("rhost"));
            System.out.println("rport " + prop.getProperty("rport"));
            System.out.println("ruser " + prop.getProperty("ruser"));
            System.out.println("rpwd " + prop.getProperty("rpwd"));
            System.out.println("");


            host = prop.getProperty("host");
            user = prop.getProperty("user");
            port = Integer.valueOf(prop.getProperty("port"));
            passwd = prop.getProperty("pwd");
            db = prop.getProperty("db");
            node = prop.getProperty("node");

            rhost = prop.getProperty("rhost");
            ruser = prop.getProperty("ruser");
            rport = Integer.valueOf(prop.getProperty("rport"));
            rpasswd = prop.getProperty("rpwd");
            rdb = "rdb_db__"+ node;


	

        } catch (IOException ex) {
            ex.printStackTrace();
        }

	         return true;


}

private boolean configNode( CommandLine cmd, String rdbbdr )  throws ParseException {
	if (cmd.hasOption("type") && cmd.hasOption("node") && cmd.hasOption("host")  && cmd.hasOption("user")  && cmd.hasOption("port")  && cmd.hasOption("passwd")  && cmd.hasOption("db")  && cmd.hasOption("rhost")  && cmd.hasOption("ruser")  && cmd.hasOption("rport")  && cmd.hasOption("rpasswd")  )  {
                         nodetype = cmd.getOptionValue("type");

                         if (!(( nodetype.equals("producer")) || (nodetype.equals("consumer") ))) {
                                throw new ParseException("Invalid type specified: " + nodetype );
                        }

			String rdbbdrconf = null;

                        node = cmd.getOptionValue("node");
                        host = cmd.getOptionValue("host");
                        port = Integer.parseInt(cmd.getOptionValue("port"));
                        user = cmd.getOptionValue("user");
                        passwd = cmd.getOptionValue("passwd");
                        db = cmd.getOptionValue("db");
                        rhost = cmd.getOptionValue("rhost");
                        ruser = cmd.getOptionValue("ruser");
                        rpasswd = cmd.getOptionValue("rpasswd");
                        rport =  Integer.parseInt(cmd.getOptionValue("rport"));
                        rdb = "rdb_db__"+ node;

                        if ( nodetype.equals("producer")) {
                                 rdbbdrconf = rdbbdr + "/conf/" + node + "_rdb_bdr.conf";
                        } else {
                                 rdbbdrconf = rdbbdr + "/conf/" + node + "_bdr_rdb.conf";
                        }
                        System.out.println("rdbbdrconf:"+rdbbdrconf);

                         try (OutputStream ouput = new FileOutputStream(new File(rdbbdrconf))) {

                            Properties prop = new Properties();

                             prop.setProperty("host", host);
                             prop.setProperty("user", user);
                             prop.setProperty("port", Integer.toString(port));
                             prop.setProperty("pwd", passwd);
                             prop.setProperty("db", db);
                             prop.setProperty("node", node);
                             prop.setProperty("rhost", rhost);
                             prop.setProperty("rport", Integer.toString(rport));
                             prop.setProperty("ruser", ruser);
                             prop.setProperty("rpwd", rpasswd);
                             prop.setProperty("rdb", rdb);
                             prop.setProperty("walqtrunc", Integer.toString(WT));
                             prop.setProperty("batch_size", Integer.toString(BS));
                             prop.setProperty("filter", "false");
                             prop.setProperty("loglevel", "OFF");
			prop.setProperty("log_hist", "false");


                         prop.store(ouput,"TC node properties");


                          } catch (IOException ex) {
                                    ex.printStackTrace();
                          }

			return true;



                 } else {
                     throw new ParseException("Invalid arguments, usage : TCSrvCTL --config [--type producer/consumer] --node <node_name> --host <host name> --port <listening port> --user <tcapture user> --passwd  <tcapture password> --db <primary db> --rhost <rdb host name> --ruser <rdb tcapture user> --rport <rdb istening port> --rpasswd  <tcapture rdb user password>");
                 }
	
}





private boolean applyOptions( CommandLineParser parser, Options options, String[] args) 
   throws ParseException 
{ 

	rdbbdr = System.getenv("RDBBDR");
	String rdbbdrconf = null;

	if (isNullOrEmpty(rdbbdr))
        {
                 System.out.println("RDBBDR home variable should be set! ");
                 System.exit(-1);
        }

	 if(!Files.isRegularFile(Paths.get(rdbbdr+"/.rdbbdr_env.sh"))) {
                        System.out.println("File "+rdbbdr+"/.rdbbdr_env.sh  not found!");
			System.out.println(" Please execute install.sh script located in the rdbbdr software home !");
                        System.exit(-1);
                 }



	 if (args.length < 1) {
                throw new IllegalArgumentException("Invalid number of arguments! ");
	}
	
	 CommandLine cmd = parser.parse(options, args); 

	 if (cmd.hasOption("help")) {
            printHelp();
	    return true;
        }

    if (cmd.hasOption("topology")) {
        isServer = true ;
        msg = "--topology";
        showconfNode( cmd, rdbbdr,isServer );
        try  {
            if (cmd.hasOption("detail")) {
                topologyMesh(true);
            } else {
                topologyMesh(false);
            }
        }  catch (Exception ex) {
            System.out.println( ex.getMessage());
        }
    } else


     if (cmd.hasOption("shutdown")) {
		isServer = true ;
         msg = "--shutdown";
		 showconfNode( cmd, rdbbdr,isServer );
		 try  {
                	shutDownRepSrv();
                 }  catch (Exception ex) {
                        System.out.println( ex.getMessage());
                }
     } else

     if (cmd.hasOption("marker")) {
         isServer = false ;
         msg = "--marker";

         showconfNode( cmd, rdbbdr,isServer );

         if ( nodetype.equals("consumer")) {
             try {
                 if (cmd.hasOption("producer"))  {
                     primary = cmd.getOptionValue("producer");
                     showconfPrimary(primary);
                     if ( (cmd.hasOption("next_xid")) || (cmd.hasOption("set_xid"))  ) {
                         if  (cmd.hasOption("next_xid")) {
                             System.out.println("---------------------------------------------------------------------------------------");
                             System.out.println("You are going  to move marker to next xid for consumer  " + node + "   of producer "+primary +" ");
                             setXid(false,0);
                         } else if (cmd.hasOption("set_xid"))   {
                             // xidToSet = cmd.getOptionValue("set_xid").;
                             long xidToSet = Long.parseLong(cmd.getOptionValue("set_xid"));
                             setXid(true,xidToSet);
                             System.out.println();
                             System.out.println("---------------------------------------------------------------------------------------");
                             System.out.println("You are going  to move marker to given xid "+xidToSet +" for consumer  " + node + "   of producer "+primary +" ");
                         }

                     } else {
                         throw new ParseException("Invalid arguments, usage : TCSrvCTL "+msg+" --node [consumer node name] --type consumer --producer [producer node name]  [--next_xid/--set_xid=<xid number>] ");
                     }
                 } else {

                     throw new ParseException("Invalid arguments, usage : TCSrvCTL "+msg+" --node [consumer node name] --type consumer --producer [producer node name]  [--next_xid/--set_xid=<xid number>] ");
                 }
             } catch (Exception ex) {
                 System.out.println( ex.getMessage());
             }

         } else {

             throw new ParseException("Invalid arguments, usage : TCSrvCTL "+msg+" --node [consumer node name] --type consumer --producer [producer node name]  [--next_xid/--set_xid=<xid number>] ");
         }

         }  else

	if (cmd.hasOption("config")) {
		configNode( cmd, rdbbdr );
	} else if (cmd.hasOption("showconf")) {
			msg = "--showconf";
			showconfNode( cmd, rdbbdr ,isServer);
	} else if (cmd.hasOption("status")) {
                        msg = "--status";
			showconfNode( cmd, rdbbdr ,isServer);
		try  {
                        statusNode( );
		 }  catch (Exception ex) {
		        System.out.println( ex.getMessage());
        	}



	} else if ( (cmd.hasOption("enable")) || (cmd.hasOption("disable")) || (cmd.hasOption("start")) || (cmd.hasOption("stop")) ) {
		  msg = "enable";
		  showconfNode( cmd, rdbbdr ,isServer);
		 if (cmd.hasOption("disable"))  msg = "disable"; 
		 if (cmd.hasOption("enable"))  msg = "enable"; 
		 if (cmd.hasOption("stop"))  msg = "stop"; 
		 if (cmd.hasOption("start"))  msg = "start"; 
		  boolean isConsumer=false;
		  boolean isMonitor=false;

		 if (( nodetype.equals("producer")) || ( nodetype.equals("monitor")) ) {
                                 try {
                                        isConsumer=false;
					if ( nodetype.equals("monitor")) isMonitor = true; 
						enableNode(isMonitor,msg);
				 } catch (Exception ex) {
                                        System.out.println( ex.getMessage());
                                }
		 } else {
		     isConsumer=true;
		     if (cmd.hasOption("producer"))  {
                        primary = cmd.getOptionValue("producer");
			 try {
			 	createConnection(rhost,rport,rdb, ruser, rpasswd);

			if (checkExistsPrimary(primary)) {
				try {
                                              enableNodeSlave(msg);
                                  } catch (Exception ex) {
                                              System.out.println( ex.getMessage());
                                  }

			} else {
                                        throw new ParseException("Invalid arguments, primary not exists:"+primary);
                        }
				} catch (Exception ex) {
                                        System.out.println( ex.getMessage());
                                }

                       } else {
                                  throw new ParseException("Invalid arguments, usage : TCSrvCTL --"+msg+" --node [consumer node name]--type consumer --producer [producer node name] ");
                        }
	}


	 } else if ( (cmd.hasOption("setup")) ||  (cmd.hasOption("unset") ) ) {
			msg = "--setup";
			 showconfNode( cmd, rdbbdr,isServer );
			 boolean isConsumer=false;
			 boolean isForce =false;
			 if (cmd.hasOption("force")) {
				 isForce = true; 
				 System.out.println("Setup node forced!");
			 }
			 if ( nodetype.equals("producer")) {
				 try {
					isConsumer=false;
                     if (cmd.hasOption("unset")) {
                         msg = "--unset";
                         unsetNode(isForce);
                     } else {
                         if ((!isForce) && (checkupNode(isConsumer) || checkupNodeProd()) ) {
                             System.exit(-1);
                         } else {
                             setupNode(isForce);
                         }
                     }
				 } catch (Exception ex) {
                                	System.out.println( ex.getMessage());
                      	}

              } else {
				isConsumer=true;
				if (cmd.hasOption("producer"))  { 
					primary = cmd.getOptionValue("producer");
					showconfPrimary(primary);
					try {
                        if (cmd.hasOption("unset")) {
                            msg = "--unset";
                            unsetNodeCons(isForce);
                        } else {

                            if ((!isForce) && (checkupNode(isConsumer) || checkupNodeCons())) {
                                System.exit(-1);
                            } else {

                                setupNodeCons(isForce);

                            }
                        }
                        } catch(Exception ex){
                            System.out.println(ex.getMessage());
                        }


					
				} else {
                 			    throw new ParseException("Invalid arguments, usage : TCSrvCTL "+msg+" --node [consumer node name]--type consumer --producer [producer node name] ");
                 			}
					
                        }


	} 

	 return true;

}



public boolean applyOptions(String[] args) 
   throws Exception 
 { 
  return applyOptions( parser, options, args); 
 } 

  public void printHelp() { 
	System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------");       
	System.out.println("usage: TCSrvCTL");
	System.out.println("Configure a node. --config  	--node --type [producer/consumer] --host --port --user --passwd --db --rhost --ruser  --rport --rpasswd");
	System.out.println("Show node config. --showconf 	--node --type [producer/consumer/monitor]");
	System.out.println("Setup a node    . --setup 	--node --type [producer/consumer]  [--producer] [--force]" );
	System.out.println("Unset a node    . --unset 	--node --type [producer/consumer]  [--producer] [--force]");
	System.out.println("Enable a node   . --enable 	--node --type [producer/consumer/moniotr] [--producer]");
	System.out.println("Disable a node  . --disable 	--node --type [producer/consumer/monitor] [--producer]");
	System.out.println("Start a node    . --start 	--node --type [producer/consumer/monitor] [--producer]");
	System.out.println("Stop  a node    . --stop		--node --type [producer/consumer/monitor] [--producer]");
	System.out.println("Show status node. --status	--node --type [producer/consumer/monitor] ");
	System.out.println("Move a  marker  . --marker    --node --type consumer --producer  [--next_xid/--set_xid=<xid number>])");
	System.out.println("Show topology   . --topology  --node [--detail] ");
	System.out.println("Shutdown TCRSrv	. --shutdown  --node ");
	System.out.println("Print help messg. --help");
	System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------------");       
  	printHelp(options); 
 } 

 private void printHelp(Options options) {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("Parameters explanation:", options);
        System.exit(0);
    }

 public TCSrvCTL() {
    options = createOptions(); 
     parser = new DefaultParser(); 
 }


    public static void main(String[] args) {

        TCSrvCTL srvctl = new TCSrvCTL();

	try {
       
	 //	srvctl.parseCLI(args);
		srvctl.applyOptions( args);	

	 } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

    }
}


