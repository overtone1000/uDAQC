package main;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import udaqc.network.center.Center;
import udaqc.network.interfaces.CenterHandler;
public class Console_Center
{
	protected static class DummyHandler implements CenterHandler
	{
		@Override
		public void ClientListUpdate()
		{

			
		}
		
		@Override
		public void ReceivingDataUpdate()
		{

			
		}

		@Override
		public void ReceivedDataUpdate()
		{

			
		}
	}
	
	public static void main(String[] args) {
		
				
		String program_root = "";
		try
		{
			Path root_path = Paths.get(Console_Center.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			program_root = root_path.getParent().toString(); 
		} catch (URISyntaxException e2)
		{
			e2.printStackTrace();
		}
		
		String props_file = program_root + "/properties";
				
		Properties props = new Properties();
		FileInputStream is=null;
		try
		{
			is = new FileInputStream(props_file);
			props.load(is);
		} catch (FileNotFoundException e1)
		{
			System.out.println("No property file exists.");
		} catch (IOException e)
		{
			System.out.println("Couldn't read property file.");	
		}
		
		if(is!=null)
		{
			try
			{
				is.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		DummyHandler h = new DummyHandler();
		
		@SuppressWarnings("unused")
		Center id_serv = new Center("IO_Center", program_root, h);
		
		//String Threadname = "GNDM_Server";
		//InetSocketAddress[] hosts=new InetSocketAddress[1];
		//hosts[0]=new InetSocketAddress("192.168.1.13",49152);
		//hosts[0]=new InetSocketAddress("0.0.0.0",0);
				
		//@SuppressWarnings("unused")
		//GNDM_Server s=new GNDM_Server(Threadname,hosts);
		
		
	}

	
}