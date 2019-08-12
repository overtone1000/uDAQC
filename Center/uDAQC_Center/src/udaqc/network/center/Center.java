package udaqc.network.center;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSession;

import network.http.HTTPS_Server;
import network.tcp.server.TCP_Server;
import network.udp.UDP_Funnel;
import network.udp.UDP_TimeSync;
import udaqc.io.IO_Constants;
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.log.IO_System_Logged;
import udaqc.io.log.IO_System_Logged.Regime;
import udaqc.network.Constants.Addresses;
import udaqc.network.center.command.Command;
import udaqc.network.interfaces.CenterHandler;
import udaqc.network.interfaces.HistoryUpdateHandler;
import udaqc.network.passthrough.Secondary_Server;
import udaqc.network.passthrough.command.PT_Command;

public class Center extends TCP_Server implements HistoryUpdateHandler
{
	protected Secondary_Server passthrough_server = null;

	public CenterHandler handler = null;

	private Logger log;
	//private Loghandler_File loghandler; // only record warning level output

	private TreeMap<Long,DirectDevice> devices = new TreeMap<Long,DirectDevice>();
	
	private HTTPS_Server webserver;

	private Path path;
	private String root;
	
	private UDP_Funnel udp;
	private UDP_TimeSync udp_ts = new UDP_TimeSync();

	public Center(String Threadname, String root, Path path, CenterHandler handler)
	{
		super(Threadname, true, false);
		
		this.handler = handler;
		this.path = path;
		this.root = root;
		
		passthrough_server = new Secondary_Server(Threadname, this);

		// super(Threadname, IO_Constants.Constants.tcp_id_port);
		IO_System_Logged.LoadSavedSystems(path,this);
		
		handler.ClientListUpdate();

		log = Logger.getLogger(Threadname);
		//loghandler = new Loghandler_File(Level.WARNING);

		//loghandler.addlog(log);
		log.setLevel(Level.INFO); // Show info level logging on system.out

		this.start();
		
		webserver = new HTTPS_Server(this, root, Addresses.webserver_insecure_port, Addresses.webserver_secure_port);
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
			case Command_IDs.auth_request:
			{
				short length = data.getShort();
				byte[] bytes = new byte[length];
				data.get(bytes);
				ByteBuffer bb = ByteBuffer.wrap(bytes);
				String login = StandardCharsets.UTF_8.decode(bb).toString();
				
				length = data.getShort();
				bytes = new byte[length];
				data.get(bytes);
				bb = ByteBuffer.wrap(bytes);
				String realm = StandardCharsets.UTF_8.decode(bb).toString();
				
				System.out.println("Auth request received for creds " + login + ":" + realm);
				
				String auth = getAuth(login,realm);
				System.out.println("Sending auth " + auth);
				byte[] auth_bytes = StandardCharsets.UTF_8.encode(auth).array();
				System.out.print("Bytes are: ");
				for(byte b:auth_bytes)
				{
					System.out.print(b);
					System.out.print(',');
				}
				System.out.println();
				Command auth_prov_com = new Command(Command_IDs.auth_provision,auth_bytes);				
				session.write(auth_prov_com);
				
				break;
			}
			case Command_IDs.group_description:
			{
				//The only time a group description command ID is received in this setting is when an entire IO_System is being sent.
				//Initialize as an IO_System, not an IO_Group. IO_Groups within IO_Groups are all initialized within the function call to construct an IO_System
				
				DirectDevice new_device = new DirectDevice(path, data, this, (NioSession)session);
				devices.put(session.getId(), new_device);
				udp_ts.addSynchronizer(new_device);
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
			if(device==null) 
			{
				System.out.println("Device not recognized.");
				return;
			}
			else if(!device.Synced())
			{
				System.out.println("Device not yet time synced. Discarding data.");
				return;
			}
			else
			{
				IO_System_Logged system = device.System();
				if(system != null)
				{				
					handler.ReceivingDataUpdate();
					system.ReceiveData(data);
					handler.ReceivedDataUpdate();
					
					log.info("Received data from " + system.Name());
				}
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
		System.out.println("Session " + session.toString() + " closed.");
		DirectDevice dev = devices.get(session.getId());
		if(dev!=null)
		{
			dev.System().ClientDisconnected();
		}
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
	
	private static final String master_device_cred_list = "security/device_credential_list.txt";
	public boolean changeDeviceCredentials(String new_login, String realm, String pw)
	{
		String conglomerate = new_login + ":" + realm + ":" + pw;
		
		System.out.println(conglomerate);
		
		Charset encoding = java.nio.charset.StandardCharsets.UTF_8;
		
		byte[] conglomerate_bytes = new byte[conglomerate.length()];
		encoding.encode(conglomerate).get(conglomerate_bytes);
		
		System.out.println("Input bytes:");
		for(byte b:conglomerate_bytes)
		{
			short c = (short)b;
			//if(c<0)
			//{
			//	c+= 256;
			//}
			System.out.println(b);
		}
		System.out.println();
		
		MessageDigest digester=null;
		try
		{
			digester=java.security.MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			return false;
		}
		
		byte[] md5 = digester.digest(conglomerate_bytes);
		
		String md5_hex_string="";
		ByteBuffer bb = ByteBuffer.wrap(md5);
		while(bb.hasRemaining())
		{
			md5_hex_string+=Integer.toHexString(bb.getInt());
		}
		
		System.out.print("Conglomerate is: ");
		System.out.println(encoding.decode(ByteBuffer.wrap(conglomerate_bytes)));
		System.out.println("Digest is " + md5_hex_string);
						
		LinkedList<String> creds = readDeviceCredentials();
		creds.add(new_login + ":" + realm + ":" + md5_hex_string);
		return deviceCredsToFile(creds);	
	}
	public LinkedList<String> readDeviceCredentials()
	{
		LinkedList<String> retval = new LinkedList<String>();
		
		File f = new File(master_device_cred_list);
		if(f.exists())
		{
			int size;
			try
			{
				size = (int) Files.size(f.toPath());
			} catch (IOException e)
			{
				e.printStackTrace();
				return retval;
			}
						
			FileReader fr=null;
			try
			{
				fr = new FileReader(f);
				CharBuffer buf = CharBuffer.allocate((int) Files.size(f.toPath()));
				fr.read(buf);
				buf.flip();
				String this_cred = "";
				
				char next;
				while(buf.remaining()>0)
				{
					next=buf.get();
				
					if(next=='\n')
					{
						retval.add(this_cred);
						this_cred = new String();
					}
					else
					{
						this_cred+=(char)next;
					}
				}
				
				fr.close();
			} catch (IOException e)
			{
				e.printStackTrace();
				return retval;
			}
			
			System.out.println("Current creds are:");
			for(String s:retval)
			{
				System.out.println(s);
			}
			System.out.println("File size = " + size);
		}
		else
		{
			System.out.println("No credential file found.");
		}
		
		return retval;
	}
	public boolean removeDeviceCredentials(int index)
	{
		LinkedList<String> creds = readDeviceCredentials();
		creds.remove(index);
		return deviceCredsToFile(creds);		
	}
	public boolean deviceCredsToFile(LinkedList<String> creds)
	{
		File f = new File(master_device_cred_list);

		if(!f.exists()) {try
		{
			System.out.println("Creating directories for file " + f.getAbsolutePath().toString());
			Files.createDirectories(f.toPath().getParent());
			System.out.println("Creating file " + f.getAbsolutePath().toString());
			if(!f.createNewFile())
			{
				System.out.println("Couldn't create file.");
				return false;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}}
		
		try
		{
			FileWriter fos = new FileWriter(f,false);
			for(String s:creds)
			{
				fos.write(s + '\n');
			}
			fos.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			
			return false;
		}
		
		return true;
	}
	private String getAuth(String login, String realm)
	{
		String comp = login + ":" + realm;
		LinkedList<String> creds = readDeviceCredentials();
		for(String s:creds)
		{
			if(s.length()>=comp.length())
			{
				String this_login_realm = s.substring(0,comp.length());
				System.out.println("This login realm: " + this_login_realm);
				if(this_login_realm.equals(comp)) 
				{
					String auth = s.substring(comp.length()+1,s.length());
					System.out.println("Auth is: " + auth);
					return auth;
				}
			}
		}
		return "";
	}
}