package gndm.io;

import java.net.InetSocketAddress;

public class IO_Constants {
		
	public static class Constants
	{
		//ports have to be integers because java doesn't have unsigned shorts, which is what defines the range for ports
		
		//These are not necessary. Just grab any available port by creating server or client with port 0.
		//public static final Integer tcp_main_port = 49152;
		//public static final Integer tcp_id_port = 49153;
		
		public static final InetSocketAddress udp_broadcast = new InetSocketAddress("255.255.255.255",49154); //This should just broadcast it.
	}

	public static class Command_IDs
	{
		public static final short group_description=1;
		public static final short emptyreporter_description=2;		
		public static final short value_description=3;		
		public static final short modifiablevalue_description=4;		
		public static final short data=5;
		public static final short handshake=6;
		//public static final short request_identity=7;
		//public static final short declare_identity=8;
		public static final short modifiablevalue_modification=9;
		public static final short request_subscription=10;
		public static final short history=11;
		public static final short passthrough=12;
	}
	
	public static class DataTypes
	{
		public static final short undefined=-1;
		public static final short signed_integer=1;		
		public static final short unsigned_integer=2;
		public static final short floating_point=3;	
		public static final short bool=4;
	}	
}
