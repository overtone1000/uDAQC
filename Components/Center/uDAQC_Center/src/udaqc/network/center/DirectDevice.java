package udaqc.network.center;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.mina.transport.socket.nio.NioSession;

import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.IO_Device;
import udaqc.io.log.IO_System_Logged;
import udaqc.network.Constants;
import udaqc.network.Device;
import udaqc.network.center.command.Command;
import udaqc.network.interfaces.HistoryUpdateHandler;
import udaqc.network.passthrough.command.PT_Command;
import udaqc.network.passthrough.endpoints.Endpoint;

public class DirectDevice extends Device
{	
	private static Vector<DirectDevice> devices = new Vector<DirectDevice>();
	
	private NioSession session;
	
	private static Semaphore device_list_mutex=new Semaphore(1);
	
	public static DirectDevice getDirectDevice(short index)
	{
		return devices.get(index);
	}
	public static DirectDevice getDirectDevice(Path path, ByteBuffer data, HistoryUpdateHandler handler, NioSession session)
	{
		DirectDevice retval = getDirectDevice(path, data, handler);
		retval.session=session;
		return retval;
	}
	public static DirectDevice getDirectDevice(Path path, ByteBuffer data, HistoryUpdateHandler handler)
	{
		DirectDevice retval = new DirectDevice(path,data,handler);
		
		try
		{
			device_list_mutex.acquire();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
			return null;
		}
		
		boolean known = retval.device_IsKnown();
		if(known)
		{
			System.out.println("Device already known.");
			for(DirectDevice d:devices) {
				if(d.isEqual(retval))
				{
					System.out.println("Found device in device list. Returning.");
					return d;
				}
			}
		}
		else
		{
			System.out.println("Unknown device. Saving.");
			retval.description_toFile();
		}
		
		System.out.println("Adding device to device list.");
		retval.device_index=(short) devices.size();
		devices.add(retval);
		device_list_mutex.release();
		
		return retval;
	}
	
	
	private DirectDevice(Path path, ByteBuffer data, HistoryUpdateHandler handler)
	{
		this.storage_path = Paths.get(path.toString());
		this.iodev = new IO_Device(data, handler, this);
		this.handler=handler;
		description=data.array().clone();
	}
	
	public void AssociateSession(NioSession session)
	{
		this.session=session;
	}
	
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
						getDirectDevice(path,data,center); //use the getSystem function for thread safety because it uses the mutex and makes sure this isn't a duplicate
					}
				}
			}
		} catch (Exception e)
		{
			System.out.println(ExceptionUtils.getStackTrace(e));
		}
	}	
	
	public static void PassthroughInitialization(Endpoint ep)
	{
		System.out.println("Sending descriptions to passthrough.");
		for(DirectDevice d:devices)
		{
			System.out.println("Sending " + d.iodev.Name());
			Command c = new Command(Command_IDs.group_description,d.description);
			PT_Command ptc = new PT_Command(d.DeviceIndex(),c);
			System.out.println("Sending initialization for device of index " + d.DeviceIndex());
			ep.SendCommand(ptc);
			
			//This now only happens upon request from the client
			//Vector<IO_System_Logged> i = d.iodev.Systems();
			//for(IO_System_Logged s:i)
			//{
			//	s.PassthroughInitialization(ep);				
			//}
		}
	}
	
	public IO_System_Logged GetSystem(short index)
	{
		return this.iodev.System(index);
	}
	
	
	@Override
	public void Send_Command(Command command)
	{
		if(session.isConnected())
		{
			System.out.println("Sending command " + command.Header().command_id + " to device " + iodev.Name());
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

}