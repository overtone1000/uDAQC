package udaqc.network.passthrough;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import network.tcp.client.TCP_Client;
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.log.IO_System_Logged;
import udaqc.io.log.IO_System_Logged.Regime;
import udaqc.network.center.command.Command;
import udaqc.network.interfaces.CenterHandler;
import udaqc.network.interfaces.HistoryUpdateHandler;
import udaqc.network.passthrough.command.PT_Command;

public class Secondary_Center extends TCP_Client implements HistoryUpdateHandler
{
	//private HashMap<Short,IO_System_Logged> systems = new HashMap<Short,IO_System_Logged>();
	private Path path=null;
	private CenterHandler parent;
	public Secondary_Center(String Threadname, Path path, InetSocketAddress hosts, CenterHandler parent)
	{
		super(Threadname, hosts, true, false);
		this.path=path;
		this.parent=parent;
		this.start();
	}

	@Override
	public void messageReceived(IoSession session, Object message)
	{
		Command c = (Command) message;
		
		switch (c.Header().command_id)
		{
		case Command_IDs.passthrough:
		{
			PT_Command ptc = new PT_Command(c);
			
			System.out.print("Passthrough command: ");
			System.out.print("ID " + ptc.Header().command_id);
			System.out.print(", length " + ptc.Header().message_length);
			System.out.print(", source " + ptc.source_id);
			System.out.println(", messsage = " + ptc.getmessage().array());
			
			handlePassthroughCommand(ptc);
		}
			break;
		default:
			System.out.print("Unhandled command: ");
			System.out.print("ID " + c.Header().command_id);
			System.out.println(", length " + c.Header().message_length);
			break;
		}
	}
	
	protected void handlePassthroughCommand(PT_Command c)
	{
		if(c.Header().command_id==Command_IDs.group_description)
		{
			IO_System_Logged.processSystemDescription(path, c.getmessage(), this);
			if (parent != null)
			{
				parent.ClientListUpdate();
			}
			return;
		}
		
		IO_System_Logged system = IO_System_Logged.getSystem(c.source_id);
		if(system==null)
		{
			return;
		}
		
		system.setDevice(new Secondary_Device(this,system));
		
		switch(c.Header().command_id)
		{
		case Command_IDs.data:
		{
			parent.ReceivingDataUpdate();
			system.ReceiveData(c.getmessage());
			parent.ReceivedDataUpdate();
			break;
		}
		case Command_IDs.history:
		{
			parent.ReceivingDataUpdate();
			system.ReceiveHistory(c.getmessage());
			parent.ReceivedDataUpdate();
			break;
		}
		default:
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
		System.out.println("Session closed!");
	}

	@Override
	public void sessionOpened(IoSession session)
	{
		super.sessionOpened(session);
		System.out.println("Session opened!");
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
	{
		super.exceptionCaught(session, cause);
	}

	@Override
	public void HistoryUpdated(IO_System_Logged system, Regime r, Long first_timestamp, ByteBuffer bb)
	{
		//Does nothing...
	}
}
