/**
 * # -- Copyright (c) 2018-2022  silvio.brandani <support@tcapture.net>. All rights reserved.
 *
 */

package com.edslab;

import org.apache.commons.cli.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.postgresql.PGConnection;
import org.postgresql.PGProperty;
import org.postgresql.core.BaseConnection;
import org.postgresql.core.ServerVersion;
import org.postgresql.replication.LogSequenceNumber;
import org.postgresql.replication.PGReplicationStream;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * Logical Decoding TWrapperSQL
 */
public class TWrapperSQL {

    private Connection connection;
    private Connection connectionRdb;
    private String nodemst = "";
    private String tsql = "";

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

        }

    }

    public int TWSQL()  throws IOException, SQLException {
	String scriptFilePath = tsql;
	BufferedReader reader = null;
	 boolean managed=false;
	 long curxid = 0;
        String curlsn ="";
	 int scanned = 0;
        String query;
 

	Statement st = connection.createStatement();
        Statement str = connectionRdb.createStatement();
	connection.setAutoCommit(false);
        connectionRdb.setAutoCommit(false);

	 try {
		

		reader = new BufferedReader(new FileReader(scriptFilePath));
		String line = null;
		while ((line = reader.readLine()) != null) {
                	st.execute(line);
		}

         } catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
            }

	finally {
		if (reader != null) {
			reader.close();
		}
	}

		
	if (!managed) {
         managed = true;
        // 19022021 PreparedStatement preparedStatement2 = connection.prepareStatement(" select case  WHEN isx is null then  txid_current() when isx> 0 then  isx end from  txid_current_if_assigned()  as isx;");
 //        PreparedStatement preparedStatement2 = connection.prepareStatement(" select case  WHEN isx is null then  txid_current() when isx> 0 then  isx end,"+ (((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ? "pg_current_wal_lsn()" : "pg_current_xlog_location()")+"::varchar from  txid_current_if_assigned()  as isx;");
//08042021  txid_current_if_assigned dalla versione 10
        PreparedStatement preparedStatement2 = connection.prepareStatement(
                (
                        ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v10) ?
                                " select case  WHEN isx is null then  txid_current() when isx> 0 then  isx end,pg_current_wal_lsn()::varchar from  txid_current_if_assigned()  as isx;  " :
                                " select txid_current(),pg_current_xlog_location()::varchar ;" )
        );


        ResultSet rs = preparedStatement2.executeQuery();
         if (rs.next()) {
             curxid = rs.getLong(1);
             curlsn = rs.getString(2);
         }
                rs.close();
       //  query = "INSERT INTO _rdb_bdr.walq__" + node + "_xid ( xid_from_queue, xid_current) values  ( -1 ," + curxid + ") On CONFLICT ON CONSTRAINT  walq__" + node + "_xid_pkey DO NOTHING ";
        query = "INSERT INTO walq__" + node + "_xid ( xid_from_queue, xid_current,lsn) values  (-1," + curxid + ",'" + curlsn + "')  ";

        str.addBatch(query);
 	}

	if (managed) {
		    System.out.println(Thread.currentThread().getName()+":" +"Commmit curxid:" + curxid);
                    try {
                        st.executeBatch();
                    } catch (BatchUpdateException ee) {
                        throw ee.getNextException();
                    }

        try {
            str.executeBatch();
        } catch (BatchUpdateException ee) {
            throw ee.getNextException();
         }

        connection.commit();
        connectionRdb.commit();



                }
                                return scanned;

	}




   public static boolean isNullOrEmpty(String myString)
    {
         return myString == null || "".equals(myString);
    }


 public synchronized void loadProps(String[] args) {

        String rdbbdr = System.getenv("RDBBDR");

        Options options = new Options();

        Option rnodemstr = new Option("n", "rnodemstr", true, "Please set desidered node ");
        rnodemstr.setRequired(true);
        options.addOption(rnodemstr);

        Option sql = new Option("s", "sql", true, "Please set desidered sql file");
        sql.setRequired(true);
        options.addOption(sql);

        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
                cmd = parser.parse(options, args);
        } catch (ParseException e) {
        System.out.println(e.getMessage());
        formatter.printHelp("TWrapperSQL", options);

       System.exit(1);
   }

        nodemst= cmd.getOptionValue("rnodemstr");
        tsql = cmd.getOptionValue("sql");
       
        System.out.println(nodemst);
        System.out.println(tsql);

   if (isNullOrEmpty(rdbbdr))
        {
                 System.out.println("RDBBDR variable should be set ");
                 System.exit(-1);
        }

        if (isNullOrEmpty(nodemst))
        {
                 System.out.println("NODEMST variable should be set ");
                 System.exit(-1);
        }

        if (isNullOrEmpty(tsql))
        {
                 System.out.println("SQL file should be set ");
                 System.exit(-1);
        }


         String rdbbdr_conf = rdbbdr + "/conf/" + nodemst + "_rdb_bdr.conf";

        if(!Files.isRegularFile(Paths.get(rdbbdr_conf))) {
                 System.out.println(rdbbdr_conf + " not exists! ");
                 System.exit(-1);
        }


  try (InputStream input = new FileInputStream(rdbbdr_conf)) {

            Properties prop = new Properties();

            prop.load(input);


            System.out.println("Configuration file: " +  rdbbdr_conf + " review");
            System.out.println("Primary Database: ");
            System.out.println("db " + prop.getProperty("db"));
            System.out.println("user " + prop.getProperty("user"));
            System.out.println("pwd " + prop.getProperty("pwd"));
            System.out.println("node " + prop.getProperty("node"));
            System.out.println("host " + prop.getProperty("host"));
            System.out.println("");
            System.out.println("RDB database: ");
            System.out.println("rdb " + prop.getProperty("rdb"));
            System.out.println("ruser " + prop.getProperty("ruser"));
            System.out.println("rpwd " + prop.getProperty("rpwd"));
            System.out.println("rnode " + prop.getProperty("rnode"));
            System.out.println("rhost " + prop.getProperty("rhost"));
            System.out.println("walqtrunc " + prop.getProperty("walqtrunc"));
            System.out.println("batch_size " + prop.getProperty("batch_size"));
            System.out.println("filter " + prop.getProperty("filter"));
            System.out.println("");

            host = prop.getProperty("host");
            user = prop.getProperty("user");
            port = Integer.valueOf(prop.getProperty("port"));
            pwd = prop.getProperty("pwd");
            db = prop.getProperty("db");
            node = prop.getProperty("node");
            walq = "_rdb_bdr.walq__" + node;
            System.out.println("walq " + walq);

            rhost = prop.getProperty("rhost");
            ruser = prop.getProperty("ruser");
            rport = Integer.valueOf(prop.getProperty("rport"));
            rpwd = prop.getProperty("rpwd");
            rdb = prop.getProperty("rdb");

            walqtrunc = Integer.valueOf(prop.getProperty("walqtrunc"));
            batch_size = Integer.valueOf(prop.getProperty("batch_size"));
            filter = Boolean.valueOf(prop.getProperty("filter"));


        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }


    private boolean isServerCompatible() {
        return ((BaseConnection) connection).haveMinimumServerVersion(ServerVersion.v9_5);
    }

    public static void main(String[] args) {

        TWrapperSQL app = new TWrapperSQL();

        app.loadProps(args);


        app.createConnection();
        app.createConnectionRdb();

        if (!app.isServerCompatible()) {
            System.err.println("must have server version greater than 9.4");
            System.exit(-1);
        }
        try {

            app.TWSQL();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}



