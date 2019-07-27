package udaqc.network.center;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSession;

import network.http.HTTPS_Server;
import network.tcp.server.TCP_Server;
import network.udp.UDP_Funnel;
import network.udp.UDP_TimeSync;
import udaqc.io.IO_Constants;
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.IO_System;
import udaqc.io.log.IO_System_Logged;
import udaqc.io.log.IO_System_Logged.Regime;
import udaqc.network.Constants.Addresses;
import udaqc.network.center.command.Command;
import udaqc.network.interfaces.CenterHandler;
import udaqc.network.interfaces.HistoryUpdateHandler;
import udaqc.network.passthrough.Secondary_Server;
import udaqc.network.passthrough.command.PT_Command;
import logging.Loghandler_File;

public class Center extends TCP_Server implements HistoryUpdateHandler
{
	protected Secondary_Server passthrough_server = null;

	public CenterHandler handler = null;

	private Logger log;
	private Loghandler_File loghandler; // only record warning level output

	private TreeMap<Long,DirectDevice> devices = new TreeMap<Long,DirectDevice>();
	
	private HTTPS_Server webserver;

	private Path path;
	
	private UDP_Funnel udp;
	private UDP_TimeSync udp_ts = new UDP_TimeSync();

	public Center(String Threadname, Path path, CenterHandler handler)
	{
		super(Threadname, true, false);
		
		this.handler = handler;
		this.path = path;
				
		passthrough_server = new Secondary_Server(Threadname, this);

		// super(Threadname, IO_Constants.Constants.tcp_id_port);
		IO_System_Logged.LoadSavedSystems(path,this);
		
		handler.ClientListUpdate();

		log = Logger.getLogger(Threadname);
		loghandler = new Loghandler_File(Level.WARNING);

		loghandler.addlog(log);
		log.setLevel(Level.INFO); // Show info level logging on system.out

		this.start();
		
		webserver = new HTTPS_Server(this, Addresses.webserver_insecure_port, Addresses.webserver_secure_port);
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
		System.out.println("Sending multicast.");
		udp = new UDP_Funnel(this.Port());
		udp.Announce();
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
				
				DirectDevice new_device = new DirectDevice(path, data, this, (NioSession)session);
				devices.put(session.getId(), new_device);
				udp_ts.synchronize(new_device);
				if (handler != null)
				{
					handler.ClientListUpdate();
				}
				
				log.info("Group description message for " + new_device.System().Name() + " received.");
			}
			break;
		  case Command_IDs.emptynode_description:
			log.info("Empty node description message from server: " + c.getString());
			break;
		  case Command_IDs.value_description:
			log.info("Value description message from server: " + c.getString());
			break;
		  case Command_IDs.modifiablevalue_description:
			log.info("Modifiable value description message from server: " + c.getString());
			break;
		  case Command_IDs.data:
		  {
			DirectDevice device = devices.get(session.getId());
			if(!device.Synced())
			{
				System.out.println("Device not yet time synced. Discarding data.");
			}
			IO_System_Logged system = device.System();
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
	
	public void Passthrough_to_Webserver(IO_System_Logged system, Command c)
	{
		PT_Command ptc = new PT_Command(system.getSystemID(),c);
		if(webserver!=null)
		{
			webserver.Broadcast(ptc);
		}
	}
	
	public void Passthrough_to_Secondaries(IO_System_Logged system, Command c)
	{
		PT_Command ptc = new PT_Command(system.getSystemID(),c);
		if(passthrough_server!=null)
		{
			passthrough_server.broadcast(ptc);
		}
	}

	public void Passthrough_to_Secondaries(IoSession session, Command c)
	{
		DirectDevice dd = devices.get(session.getId());
		if(dd!=null)
		{
			IO_System_Logged system = dd.System();
			Passthrough_to_Secondaries(system,c);
		}
	}
	
	public void Passthrough_from_Secondary(PT_Command ptc)
	{
		Command c = ptc.containedCommand();
		IO_System_Logged.getSystem(ptc.source_id).Device().Send_Command(c);
	}

	@Override
	public void HistoryUpdated(IO_System_Logged system, Regime r, Long first_timestamp, ByteBuffer bb)
	{
		
		ByteBuffer message = ByteBuffer.allocate(Integer.BYTES + Long.BYTES + bb.capacity());
		message.order(ByteOrder.LITTLE_ENDIAN);
		message.putInt(r.ordinal());
		message.putLong(first_timestamp);
		message.put(bb.array());
		
		Command c=new Command(IO_Constants.Command_IDs.history_addendum,message.array());
		Passthrough_to_Webserver(system,c);
	}
}