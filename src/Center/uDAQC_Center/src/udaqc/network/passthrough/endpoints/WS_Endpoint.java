package udaqc.network.passthrough.endpoints;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Session;

import udaqc.network.center.command.Command;

public class WS_Endpoint implements Endpoint
{
	Session session;
	public WS_Endpoint(Session session)
	{
		this.session=session;
	}
	
	@Override
	public void SendCommand(Command c)
	{
		try
		{
			session.getRemote().sendBytes(ByteBuffer.wrap(c.toBuffer().array()));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
