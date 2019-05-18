package udaqc.network;

import java.net.InetSocketAddress;

public class Constants
{
	public static class Addresses
	{
		//ports have to be integers because java doesn't have unsigned shorts, which is what defines the range for ports
		
		//These are not necessary. Just grab any available port by creating server or client with port 0.
		//public static final Integer tcp_main_port = 49152;
		//public static final Integer tcp_id_port = 49153;
		public static final Integer webserver_insecure_port = 49153;
		public static final Integer webserver_secure_port = 49154;
		
		public static final InetSocketAddress udp_broadcast = new InetSocketAddress("255.255.255.255",49154); //This should just broadcast it.
	}
}
