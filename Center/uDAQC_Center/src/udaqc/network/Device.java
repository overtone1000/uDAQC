package udaqc.network;

import udaqc.network.center.command.Command;

public interface Device
{
	public void Send_Command(Command update_command);
}
