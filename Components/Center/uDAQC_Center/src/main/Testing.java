package main;


import java.net.InetSocketAddress;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;

import org.apache.commons.dbcp2.BasicDataSource;

import network.http.HTTPS_Server;
import security.PasswordManager;
import udaqc.network.Constants.Addresses;
import udaqc.network.passthrough.Secondary_Center;

public class Testing 
{
	public static void main(String[] args) 
	{
		//TLS();
		//WebServer();
		database();
	}
	public static void database()
	{
		System.out.println("Testing database.");
		
		String url = "jdbc:postgresql://localhost/udaqc_database";
		Properties props = new Properties();
		props.setProperty("user","udaqc");
		props.setProperty("password","udaqc");
		props.setProperty("ssl","false");
		
		Connection conn=null;
		Statement s=null;
		String command;
		String test_table_name = "test_table";
		try
		{
			conn = DriverManager.getConnection(url, props);
			DatabaseMetaData meta = conn.getMetaData();
			ResultSet meta_res = meta.getTables(null, null, test_table_name,new String[] {"TABLE"});
			System.out.println("Checking for tables with name " + test_table_name);
			if(meta_res.next()) 
			{
				System.out.println("Found table with name " + test_table_name);
			}
			else
			{
				System.out.println("Table " + test_table_name + " doesn't exist, creating.");
				
				s = conn.createStatement();
				command = "create table " + test_table_name + "( ";
				command += "time TIMESTAMPTZ NOT NULL,";
				command += "temperature DOUBLE PRECISION NOT NULL";
				command += ");";
				s.executeUpdate(command);
				s.close();
				
				s = conn.createStatement();
				command = "select create_hypertable('" + test_table_name + "', 'time');";
				s.execute(command);
				s.close();
			}
			
			s = conn.createStatement();
			Double temp = Math.random();
			command = "insert into " + test_table_name + "(time,temperature) values (NoW(), " + temp.toString() + ");";
			s.executeUpdate(command);
			s.close();
			
			s = conn.createStatement();
			command = "select * from " + test_table_name + " order by time";
			s.execute(command);
			ResultSet res = s.getResultSet();
			while(res.next())
			{
				Timestamp time = res.getTimestamp(1, java.util.Calendar.getInstance(java.util.TimeZone.getDefault()));
				Double temperature = res.getDouble(2);
				System.out.println(time.toString() + ", " + temperature);
			}
			
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(conn!=null)
				{
					conn.close();
				}
			} catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		System.out.println("Database testing done.");
	}
	public static void TLS()
	{
		InetSocketAddress host = new InetSocketAddress("127.0.0.1",udaqc.network.passthrough.Secondary_Constants.Ports.passthrough_server);
		@SuppressWarnings("unused")
		Secondary_Center cl = new Secondary_Center("Testing client",Paths.get("passthrough_history"),host,null);
	}
	public static void Password()
	{
		PasswordManager man = new PasswordManager(Paths.get("security/credentials.dat"));
		
		man.AddCredentials("decoy1","decoy");
		
		String login = "tyler";
		String pw = "test_pw";
		String pw2 = "test_pw2";
		
		man.AddCredentials(login, pw); 
		//man.AddCredentials(login, pw2);
		
		System.out.println("Checking credentials.");
		if(man.CheckCredentials(login, pw))
		{
			System.out.println(login + " found with pw1.");
		}
		else
		{
			System.out.println(login + " not found with pw1.");
		}
		
		if(man.CheckCredentials(login, pw2))
		{
			System.out.println(login + " found with pw2.");
		}
		else
		{
			System.out.println(login + " not found with pw2.");
		}
		
		if(man.CheckCredentials("wrong login", "wrong_pw"))
		{
			System.out.println("Error.");
		}
		else
		{
			System.out.println("Bogus login rejected.");
		}
		
		System.out.println();
		System.out.println("Finished!");
	}
	public static void WebServer()
	{
		HTTPS_Server webserver = new HTTPS_Server(null, "", Addresses.webserver_insecure_port, Addresses.webserver_secure_port);
	}
}