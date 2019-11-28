package main;


import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;

import network.http.HTTPS_Server;
import security.PasswordManager;
import udaqc.io.IO_Constants;
import udaqc.io.IO_Device;
import udaqc.io.IO_System;
import udaqc.jdbc.Database_uDAQC;
import udaqc.jdbc.Database_uDAQC.HistoryResult;
import udaqc.jdbc.Database_uDAQC.Regime;
import udaqc.network.Constants.Addresses;
import udaqc.network.center.IO_Device_Connected;
import udaqc.network.center.command.Command;

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
		Database_uDAQC database = new Database_uDAQC();
		database.loadDevices();
		
		//Testing...
		IO_System sys=IO_Device_Connected.getDirectDevice((short)0).GetSystem((short)0);
		Instant start = Instant.parse("2019-10-20T06:22:05.00Z");
		Instant end = Instant.parse("2019-10-25T06:22:17.00Z");
		Timestamp start_ts = Timestamp.from(start);
		Timestamp end_ts = Timestamp.from(end);
		System.out.println("Counted " + database.count(sys, Regime.hour, start_ts, end_ts));
		HistoryResult c = database.getHistory(sys, Regime.hour, start_ts, end_ts);
		System.out.println("Retrieved history.");
		Command command = new Command(IO_Constants.Command_IDs.history,c.message.array());
		Database_uDAQC.PrintHistory(sys, command.getmessage());
		System.out.println("Database testing done.");
	}
	public static void TLS()
	{
		//InetSocketAddress host = new InetSocketAddress("127.0.0.1",udaqc.network.passthrough.Secondary_Constants.Ports.passthrough_server);
		//@SuppressWarnings("unused")
		//Secondary_Center cl = new Secondary_Center("Testing client",Paths.get("passthrough_history"),host,null);
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