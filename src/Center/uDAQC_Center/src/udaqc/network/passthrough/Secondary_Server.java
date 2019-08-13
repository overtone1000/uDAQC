package udaqc.network.passthrough;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import network.tcp.server.TCP_Server;
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.log.IO_System_Logged;
import udaqc.network.center.Center;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.command.PT_Command;
import udaqc.network.passthrough.endpoints.TCP_Endpoint;

public class Secondary_Server extends TCP_Server
{
	protected Center parent;
	
	public Secondary_Server(String Parent_Threadname, Center parent)
	{
		super(Parent_Threadname + " Passthrough",Secondary_Constants.Ports.passthrough_server,true,false);
		this.parent=parent;
		this.start();
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
	}

	@Override
	public void messageReceived(IoSession session, Object message)
	{
		Command c = (Command) message;
		PT_Command ptc = new PT_Command(c);
				
		switch (c.Header().command_id)
		{
		default:
			parent.Passthrough_from_Secondary(ptc);
			break;
		}
	}
	

	@Override
	public void sessionIdle(IoSession session, IdleStatus status)
	{// throws Exception
		super.sessionIdle(session, status);
		Command c = new Command(Command_IDs.handshake);
		System.out.println("Sending manual handshake.");
		session.write(c);
	}

	@Override
	public void sessionClosed(IoSession session)
	{
		super.sessionClosed(session);
	}

	@Override
	public void sessionOpened(IoSession session)
	{
		IO_System_Logged.PassthroughInitialization(new TCP_Endpoint(session)); //need to do this first so new messages passed through don't interfere
		super.sessionOpened(session);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
	{
		parent.exceptionCaught(session, cause);
	}
	
	public void broadcast(PT_Command command)
	{
		for(IoSession ses:sessions)
		{
			ses.write(command);
		}
	}
}
