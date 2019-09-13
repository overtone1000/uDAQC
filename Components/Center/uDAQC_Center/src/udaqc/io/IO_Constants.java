package udaqc.io;

public class IO_Constants {
		
	public static class Command_IDs
	{
		public static final short system_description=17;
		public static final short group_description=1;
		public static final short emptynode_description=2;		
		public static final short value_description=3;		
		public static final short modifiablevalue_description=4;		
		public static final short data=5;
		public static final short handshake=6;
		public static final short auth_request=7;
		public static final short auth_provision=8;
		public static final short modifiablevalue_modification=9;
		public static final short request_subscription=10;
		public static final short history=11;
		public static final short passthrough=12;
		public static final short new_device_available=13;
		public static final short history_addendum=14;
		public static final short timesync_request=15;
		public static final short timesync_response=16;
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
