package main.console;


import java.nio.file.Path;
import java.nio.file.Paths;

import gndm.network.center.GNDM_Center;
import gndm.network.center.GNDM_Handler;
public class GNDM_Console_Server
{
	protected static class DummyHandler implements GNDM_Handler
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
	public static void main(String[] args) {
		Path path = Paths.get(args[0]);
				
		DummyHandler h = new DummyHandler();
		
		@SuppressWarnings("unused")
		GNDM_Center id_serv = new GNDM_Center("ID_Server", path, h);
		
		//String Threadname = "GNDM_Server";
		//InetSocketAddress[] hosts=new InetSocketAddress[1];
		//hosts[0]=new InetSocketAddress("192.168.1.13",49152);
		//hosts[0]=new InetSocketAddress("0.0.0.0",0);
				
		//@SuppressWarnings("unused")
		//GNDM_Server s=new GNDM_Server(Threadname,hosts);
		
	}

	
}