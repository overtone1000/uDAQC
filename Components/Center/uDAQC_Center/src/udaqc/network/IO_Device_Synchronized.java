package udaqc.network;

import java.nio.ByteBuffer;

import org.joda.time.DateTime;

import network.udp.TimeSyncExternals;
import network.udp.TimeSynchronizer;
import udaqc.io.IO_Device;
import udaqc.io.IO_Value;
import udaqc.network.center.Center;

public abstract class IO_Device_Synchronized extends IO_Device implements TimeSyncExternals
{
	public IO_Device_Synchronized(ByteBuffer data)
	{
		super(data);
	}
	
	protected TimeSynchronizer syncer = new TimeSynchronizer(this);
	public TimeSynchronizer getTimeSynchronizer()
	{
		return syncer;
	}
	
	//public abstract void Send_Command(Command update_command);
	
	public DateTime GetTimestamp(short system_index)
	{
		DateTime retval=null;
		if(System(system_index)!=null)
		{
			IO_Value time = (IO_Value)(System(system_index).GetMembers().firstElement());
			long systemtimestamp = (long)(time.Value());
			long us = systemtimestamp+syncer.timeZeroMicros();
			retval = new DateTime(us/1000);
		}
		return retval;
	}
}
