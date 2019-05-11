package udaqc.network.center.command;

public class CommandHeader 
{
	public static final int header_length=4+2;
	
	public int message_length=-1;
	public short command_id=-1;
	
	//public DateTime getDateTime()
	//{
	//	int millis_from_ts = 0;
	//	DateTime dt = new DateTime(millis_from_ts);
	//	return dt;
	//}
}
