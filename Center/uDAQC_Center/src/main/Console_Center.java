package main;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void ReceivingDataUpdate()
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public void ReceivedDataUpdate()
		{
			// TODO Auto-generated method stub
			
		}
	}
	
	private static final String props_file = "properties";
	private static final String history_key = "history";
	public static void main(String[] args) {
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
		
		FileOutputStream os=null; 
		if(!props.containsKey(history_key))
		{
			props.put(history_key, "history");
			try
			{
				os = new FileOutputStream(props_file);
				props.store(os, null);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		if(os!=null)
		{
			try
			{
				os.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
		String path_str = props.getProperty(history_key);
		Path path = Paths.get(path_str);
		
		DummyHandler h = new DummyHandler();
		
		@SuppressWarnings("unused")
		Center id_serv = new Center("IO_Center", path, h);
		
		//String Threadname = "GNDM_Server";
		//InetSocketAddress[] hosts=new InetSocketAddress[1];
		//hosts[0]=new InetSocketAddress("192.168.1.13",49152);
		//hosts[0]=new InetSocketAddress("0.0.0.0",0);
				
		//@SuppressWarnings("unused")
		//GNDM_Server s=new GNDM_Server(Threadname,hosts);
		
	}

	
}