package udaqc.network;

import network.udp.TimeSynchronizer;
import udaqc.network.center.command.Command;

public abstract class Device extends TimeSynchronizer
{
	public abstract void Send_Command(Command update_command);
}
