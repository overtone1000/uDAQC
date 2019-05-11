package udaqc.network.passthrough;

import udaqc.io.log.IO_System_Logged;
import udaqc.network.Device;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.command.PT_Command;

public class Secondary_Device implements Device
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
		PT_Command ptc = new PT_Command(system.getSystemID(),update_command);
		center.Send_Command(ptc);
	}
}
