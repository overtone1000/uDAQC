package main;


import java.nio.file.Path;
import java.nio.file.Paths;

import udaqc.network.center.Center;
import udaqc.network.center.CenterHandler;
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
	public static void main(String[] args) {
		Path path = Paths.get(args[0]);
				
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