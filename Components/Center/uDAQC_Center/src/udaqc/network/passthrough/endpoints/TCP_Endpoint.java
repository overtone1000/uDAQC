package udaqc.network.passthrough.endpoints;

import org.apache.mina.core.session.IoSession;

import udaqc.network.center.command.Command;

public class TCP_Endpoint implements Endpoint
{
	private IoSession session;
	public TCP_Endpoint(IoSession session)
	{
		this.session=session;
	}
	@Override
	public void SendCommand(Command c)
	{
		session.write(c);		
	}
}
