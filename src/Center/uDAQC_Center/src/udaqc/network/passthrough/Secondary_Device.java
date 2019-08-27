package udaqc.network.passthrough;

import java.net.InetSocketAddress;

import udaqc.io.log.IO_System_Logged;
import udaqc.network.Device;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.command.PT_Command;

public class Secondary_Device extends Device
{
	protected IO_System_Logged system;
	protected Secondary_Center center;
	public Secondary_Device(Secondary_Center center, IO_System_Logged system)
	{
		this.center=center;
		this.system=system; 
	}
	
	@Override
	public void Send_Command(Command update_command)
	{
		PT_Command ptc = new PT_Command(this.device_index,update_command);
		center.Send_Command(ptc);
	}

	@Override
	public void Synchronize(long epoch_micros_at_time_zero)
	{
	}

	@Override
	public InetSocketAddress TimeSyncAddress()
	{
		return null;
	}

	@Override
	public long timeZeroMicros()
	{
		return 0;
	}

	@Override
	public boolean KeepSynchronized()
	{
		return false;
	}
}
