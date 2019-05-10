package gndm.network.passthrough;

import gndm.io.log.IO_System_Logged;
import gndm.network.GNDM_Device;
import gndm.network.center.command.Command;
import gndm.network.passthrough.command.PT_Command;

public class Secondary_Device implements GNDM_Device
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
