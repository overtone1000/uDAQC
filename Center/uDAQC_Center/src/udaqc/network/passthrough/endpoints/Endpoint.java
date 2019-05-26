package udaqc.network.passthrough.endpoints;

import udaqc.network.center.command.Command;

public interface Endpoint
{
	public void SendCommand(Command c);
}
