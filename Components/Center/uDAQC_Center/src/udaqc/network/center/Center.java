package udaqc.network.center;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.IO_Device;
import udaqc.io.IO_System;
import udaqc.network.Constants.Addresses;
import udaqc.network.center.command.Command;
import udaqc.network.interfaces.CenterHandler;
import udaqc.network.passthrough.command.PT_Command;
import udaqc.jdbc.Database_uDAQC;

public class Center extends TCP_Server
{
	public static final Database_uDAQC database = new Database_uDAQC();
	public CenterHandler handler = null;

	private Logger log;
	//private Loghandler_File loghandler; // only record warning level output

	private TreeMap<Long,IO_Device_Connected> devices = new TreeMap<Long,IO_Device_Connected>();
	
	private HTTPS_Server webserver;

	private String master_device_cred_list;
	
	private UDP_Funnel udp;
	private UDP_TimeSync udp_ts = new UDP_TimeSync();

	public Center(String Threadname, String program_root, CenterHandler handler)
	{
		super(Threadname, true, false);
				
		this.handler = handler;
		this.master_device_cred_list = program_root + "/security/device_credential_list.txt";
		
		database.Initialize();
		
		handler.ClientListUpdate();
		
		log = Logger.getLogger(Threadname);
		//loghandler = new Loghandler_File(Level.WARNING);

		//loghandler.addlog(log);
		log.setLevel(Level.INFO); // Show info level logging on system.out

		this.start();
		
		webserver = new HTTPS_Server(this, program_root, Addresses.webserver_insecure_port, Addresses.webserver_secure_port);
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
			case Command_IDs.system_description:
			{
				System.err.println("Received direct system description to Center. Unanticipated.");
				break;
			}
			case Command_IDs.group_description:
			{
				//The only time a group description command ID is received in this setting is when an entire IO_Device description is being sent en bloc.
				//Initialize as a whole IO_Device, not an IO_Group or IO_System. IO_Groups within IO_Groups are all initialized within the function call to construct an IO_System
				
				System.out.println("Center received a group description. Interpreting as a device.");
				
				IO_Device_Connected new_device = IO_Device_Connected.getDirectDevice(data, (NioSession)session);
				//System.out.println("Putting device in list.");
				devices.put(session.getId(), new_device);
				//System.out.println("Adding synchronizer to list.");
				udp_ts.addSynchronizer(new_device.getTimeSynchronizer());
				if (handler != null)
				{
					handler.ClientListUpdate();
				}
				
				//System.out.println("Device interpretation completed.");
				break;
			}
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
			IO_Device_Connected device = devices.get(session.getId());
			if(device==null) 
			{
				System.out.println("IO_Device_Synchronized not recognized.");
				return;
			}
			else if(!device.getTimeSynchronizer().Synced())
			{
				System.out.println("IO_Device_Synchronized not yet time synced. Discarding data.");
				return;
			}
			else
			{
				handler.ReceivingDataUpdate();
				IO_System updated_system = device.ReceiveData(data);
				handler.ReceivedDataUpdate();
				webserver.handleSystemDataUpdated(updated_system);
			}
		  }
		  break;
		  default:
			System.err.println("Unhandled command " + c.Header().command_id + " of lenght " + c.Header().message_length);
			break;
		}
	}
	
	public void test_forceDataUpdate()
	{
		System.out.println("Testing: Forcing data update.");
		for(IO_Device d:IO_Device_Connected.getDevices())
		{
			for(IO_System s:d.Systems())
			{
				System.out.println("Testing: Forcing data update " + s.FullName());
				webserver.handleSystemDataUpdated(s);
			}
		}
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status)
	{// throws Exception
		super.sessionIdle(session, status);
		Command c = new Command(Command_IDs.handshake);
		System.out.println("Sending manual handshake.");
		session.write(c);
		
		if(session.getIdleCount(IdleStatus.READER_IDLE)>3)
		{
			session.closeNow();
		}
	}

	@Override
	public void sessionClosed(IoSession session)
	{
		super.sessionClosed(session);
		System.out.println("Session " + session.toString() + " closed.");
		IO_Device_Connected dev = devices.get(session.getId());
		if(dev!=null)
		{
			dev.ClientDisconnected();
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
			e.printStackTrace();
		}
		log.severe(ExceptionUtils.getStackTrace(cause));
	}
	
	public void Passthrough_to_Webserver(IO_Device_Connected device, Command c)
	{
		PT_Command ptc = new PT_Command(device.DeviceIndex(),c);
		if(webserver!=null)
		{
			webserver.Broadcast(ptc);
		}
	}
		
	public void Passthrough_from_Secondary(PT_Command ptc)
	{
		Command c = ptc.containedCommand();
		IO_Device_Connected.getDirectDevice(ptc.device_index).Send_Command(c);
	}
	
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
			//short c = (short)b;
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