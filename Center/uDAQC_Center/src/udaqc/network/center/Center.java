package udaqc.network.center;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import network.http.HTTPS_Server;
import network.tcp.server.TCP_Server;
import network.udp.UDP_Sender;
import udaqc.io.IO_Constants;
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.log.IO_System_Logged;
import udaqc.network.Constants.Addresses;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.Secondary_Server;
import udaqc.network.passthrough.command.PT_Command;
import logging.Loghandler_File;

public class Center extends TCP_Server
{
	protected Secondary_Server passthrough_server = null;

	public CenterHandler handler = null;

	private Logger log;
	private Loghandler_File loghandler; // only record warning level output

	private TreeMap<Long,DirectDevice> devices = new TreeMap<Long,DirectDevice>();
	
	private HTTPS_Server webserver;

	private Path path;

	public Center(String Threadname, Path path, CenterHandler handler)
	{
		super(Threadname, true, false);
		
		this.handler = handler;
		this.path = path;
				
		passthrough_server = new Secondary_Server(Threadname, this);

		// super(Threadname, IO_Constants.Constants.tcp_id_port);
		IO_System_Logged.LoadSavedSystems(path);
		
		handler.ClientListUpdate();

		log = Logger.getLogger(Threadname);
		loghandler = new Loghandler_File(Level.WARNING);

		loghandler.addlog(log);
		log.setLevel(Level.INFO); // Show info level logging on system.out

		this.start();
		
		webserver = new HTTPS_Server(Addresses.webserver_insecure_port, Addresses.webserver_secure_port);
		try
		{
			webserver.start();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void serverinitialized()
	{
		try
		{
			System.out.println("Sending multicast.");

			IoBuffer message = IoBuffer.allocate(Integer.BYTES);
			message.order(ByteOrder.LITTLE_ENDIAN);
			message.putInt(this.Port());
			Command c = new Command(IO_Constants.Command_IDs.request_subscription, message.array());

			UDP_Sender.send(c, Addresses.udp_broadcast);
		} catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void messageReceived(IoSession session, Object message)
	{
		Command c = (Command) message;
		
		Passthrough_to_Secondaries(session, c);
		
		ByteBuffer data = c.getmessage();

		switch (c.Header().command_id)
		{
			case Command_IDs.group_description:
			{
				//The only time a group description command ID is received in this setting is when an entire IO_System is being sent.
				//Initialize as an IO_System, not an IO_Group. IO_Groups within IO_Groups are all initialized within the function call to construct an IO_System
				
				DirectDevice new_device = new DirectDevice(path, data, this, session);
				devices.put(session.getId(), new_device);
				
				if (handler != null)
				{
					handler.ClientListUpdate();
				}
				
				log.info("Group description message for " + new_device.System().Name() + " received.");
			}
			break;
		  case Command_IDs.emptyreporter_description:
			log.info("Empty reporter description message from server: " + c.getString());
			break;
		  case Command_IDs.value_description:
			log.info("Value description message from server: " + c.getString());
			break;
		  case Command_IDs.modifiablevalue_description:
			log.info("Modifiable value description message from server: " + c.getString());
			break;
		  case Command_IDs.data:
		  {
			IO_System_Logged system = devices.get(session.getId()).System();
			if(system != null)
			{				
				handler.ReceivingDataUpdate();
				system.ReceiveData(data);
				handler.ReceivedDataUpdate();
				
				log.info("Received data from " + system.Name());
			}
		  }
		  break;
		  default:
			System.out.println("Received command " + c.Header().command_id);
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
		devices.get(session.getId()).System().ClientDisconnected();
		devices.remove(session.getId());
	}

	@Override
	public void sessionOpened(IoSession session)
	{
		super.sessionOpened(session);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
	{// throws Exception{
		if (cause.getClass().equals(java.io.IOException.class) && cause.getMessage().equals("Connection reset by peer"))
		{
			return;
		}
		if (cause.getClass().equals(org.apache.mina.core.write.WriteToClosedSessionException.class))
		{
			return;
		}
		try
		{
			super.exceptionCaught(session, cause);
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.severe(ExceptionUtils.getStackTrace(cause));
	}

	public void Passthrough_to_Secondaries(IoSession session, Command c)
	{
		DirectDevice dd = devices.get(session.getId());
		if(dd!=null)
		{
			IO_System_Logged system = dd.System();
			PT_Command ptc = new PT_Command(system.getSystemID(),c);
			if(passthrough_server!=null)
			{
				passthrough_server.broadcast(ptc);
			}
			if(webserver!=null)
			{
				webserver.Broadcast(ptc);
			}
		}
	}
	
	public void Passthrough_from_Secondary(PT_Command ptc)
	{
		Command c = ptc.containedCommand();
		IO_System_Logged.getSystem(ptc.source_id).Device().Send_Command(c);
	}
}