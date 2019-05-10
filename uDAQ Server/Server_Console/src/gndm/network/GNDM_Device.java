package gndm.network;

import gndm.network.center.command.Command;

public interface GNDM_Device
{
	public void Send_Command(Command update_command);
}
