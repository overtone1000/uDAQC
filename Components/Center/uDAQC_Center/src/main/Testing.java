package main;


import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.time.Instant;

import network.http.HTTPS_Server;
import security.PasswordManager;
import udaqc.io.IO_Device;
import udaqc.io.IO_System;
import udaqc.jdbc.Database_uDAQC;
import udaqc.jdbc.Database_uDAQC.Regime;
import udaqc.network.Constants.Addresses;
import udaqc.network.center.IO_Device_Connected;

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
		Instant start = Instant.ofEpochMilli(0);
		Instant end = Instant.now();
		database.count(sys, Regime.raw, start, end);
		System.out.println("Counted.");
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