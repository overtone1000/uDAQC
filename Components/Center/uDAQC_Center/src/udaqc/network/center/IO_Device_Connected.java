package udaqc.network.center;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import org.apache.mina.transport.socket.nio.NioSession;
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.IO_System;
import udaqc.network.Constants;
import udaqc.network.IO_Device_Synchronized;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.command.PT_Command;
import udaqc.network.passthrough.endpoints.Endpoint;

public class IO_Device_Connected extends IO_Device_Synchronized
{	
	protected short device_index=-1;
	public short DeviceIndex() {return device_index;}
	
	private static Vector<IO_Device_Connected> devices = new Vector<IO_Device_Connected>();
	
	private NioSession session;
	
	private static Semaphore device_list_mutex=new Semaphore(1);
	
	public static IO_Device_Connected getDirectDevice(short index)
	{
		return devices.get(index);
	}
	public static IO_Device_Connected getDirectDevice(ByteBuffer data, NioSession session)
	{
		IO_Device_Connected retval = getDirectDevice(data);
		retval.session=session;
		return retval;
	}
	public static IO_Device_Connected getDirectDevice(ByteBuffer data)
	{
		IO_Device_Connected retval = new IO_Device_Connected(data);
		
		try
		{
			device_list_mutex.acquire();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
			return null;
		}
		

		for(IO_Device_Connected d:devices)
		{
			if(d.isEqual(retval))
			{
				return d;
			}
		}
		
		System.out.println("Unknown device. Saving.");
		Center.database.insertDevice(retval);
		
		System.out.println("Adding device to device list.");
		retval.device_index=(short) devices.size();
		devices.add(retval);
		device_list_mutex.release();
		
		return retval;
	}
	
	
	private IO_Device_Connected(ByteBuffer data)
	{
		super(data);
	}
	
	public void AssociateSession(NioSession session)
	{
		this.session=session;
	}
	
	/*
	public static void LoadSavedDevices(Path path, Center center)
	{
		System.out.println("Loading saved devices.");
		try
		{
			Files.createDirectories(path);
			//need to load history from files
		} catch (IOException e1)
		{
			System.out.println(ExceptionUtils.getStackTrace(e1));
		}

		try
		{			
			for(File device:path.toFile().listFiles())
			{
				if(device.isDirectory())
				{
					File descfile = new File(device+filesep+descname);
					if(descfile.exists())
					{
						ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(descfile.toPath()));
						data.position(0);
						data.order(ByteOrder.LITTLE_ENDIAN);
						
						System.out.println("Calling getDirectDevice function.");
						getDirectDevice(data,center); //use the getSystem function for thread safety because it uses the mutex and makes sure this isn't a duplicate
					}
				}
			}
		} catch (Exception e)
		{
			System.out.println(ExceptionUtils.getStackTrace(e));
		}
	}	
	*/
		
	public static void PassthroughInitialization(Endpoint ep)
	{
		System.out.println("Sending descriptions to passthrough.");
		for(IO_Device_Connected d:devices)
		{
			System.out.println("Sending " + d.Name());
			Command c = new Command(Command_IDs.group_description,d.Description());
			PT_Command ptc = new PT_Command(d.DeviceIndex(),c);
			System.out.println("Sending initialization for device of index " + d.DeviceIndex());
			ep.SendCommand(ptc);
			
			//This now only happens upon request from the client
			//Vector<IO_System> i = d.Systems();
			//for(IO_System s:i)
			//{
			//	s.PassthroughInitialization(ep);				
			//}
		}
	}
	
	public IO_System GetSystem(short index)
	{
		return this.System(index);
	}
	
	public void ClientDisconnected()
	{
		for(IO_System sys:Systems())
		{
			sys.ClientDisconnected();
		}
	}
		
	//@Override
	public void Send_Command(Command command)
	{
		if(session.isConnected())
		{
			System.out.println("Sending command " + command.Header().command_id + " to device " + Name());
			session.write(command);
		}
		else
		{
			System.out.println("Session is diconnected.");
		}
	}

	@Override
	public InetSocketAddress TimeSyncAddress()
	{
		InetAddress add = ((InetSocketAddress)session.getRemoteAddress()).getAddress();
		if(add==null) {return null;}
		InetSocketAddress retval = new InetSocketAddress(add,Constants.Addresses.udp_port);
		return retval;
	}

	@Override
	public boolean KeepSynchronized()
	{
		return session.isConnected();
	}

	public void ReceiveData(ByteBuffer data)
	{
		Short system_index = data.getShort();
		IO_System system = System(system_index);
		if(system != null)
		{				
			system.ReceiveData(data);
			System.out.println("Processed data from " + Name() + ":" + system.Name());
			Center.database.insertSystemTable(this, system_index);
		}
		else
		{
			System.out.println("System " + system_index + " on device " + Name() + " not found, discarding data.");
		}
	}
}