package udaqc.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;

import network.udp.TimeSynchronizer;
import udaqc.io.log.IO_System_Logged;
import udaqc.network.center.DirectDevice;
import udaqc.network.center.command.Command;
import udaqc.network.interfaces.HistoryUpdateHandler;

public abstract class Device extends TimeSynchronizer
{
	protected short device_index=-1;
	public short DeviceIndex() {return device_index;}
	
	protected byte[] description;
	
	protected TreeMap<Short, IO_System_Logged> systems = new TreeMap<Short, IO_System_Logged>();
	public Iterator<IO_System_Logged> Systems() {return systems.values().iterator();}
	public void AddSystem(IO_System_Logged new_system) 
	{
		systems.put(new_system.Index(),new_system);
		System.out.println("New system " + new_system.Index() + " added. Systems indices known:");
		for(Short s:systems.keySet())
		{
			System.out.println(s);
		}
	}
	public IO_System_Logged System(Short n) {return systems.get(n);}
	
	public final static String filesep=System.getProperty("file.separator");
	public final static String descname ="descripton";
	
	protected Path storage_path;
	
	private String name = "Unnamed";
	public String Name()
	{
		return name;
	}
	
	protected HistoryUpdateHandler handler;
	
	protected Logger log;
	public boolean isEqual(Object o)
	{
		return o instanceof DirectDevice && //this works
				java.util.Arrays.equals(description,((DirectDevice) o).description) && //this works
				storage_path.equals(((DirectDevice) o).storage_path); //this works
	}
	
	protected boolean device_IsKnown() //returns true if had to be written. If d
	{
		try
		{
			Files.createDirectories(storage_path);
			if(Files.exists(description()))
			{
				byte[] fileContent = Files.readAllBytes(description());
				
				return java.util.Arrays.equals(description,fileContent);
			}
			
		} catch (IOException e1)
		{
			log.severe(ExceptionUtils.getStackTrace(e1));
		}
		return false;
	}
	
	protected void description_toFile()
	{
		try
		{
			Files.createDirectories(storage_path);
			Files.write(description(), description);
		}
		catch (IOException e1)
		{
			log.severe(ExceptionUtils.getStackTrace(e1));
		}
	}
	
	protected Path description()
	{
		return Paths.get(storage_path + filesep + descname);
	}
	

	public void ReceiveData(ByteBuffer data)
	{
		Short system_index = data.getShort();
		IO_System_Logged system = System(system_index);
		if(system != null)
		{				
			system.ReceiveData(data);
			System.out.println("Processed data from " + Name() + ":" + system.Name());
		}
		else
		{
			System.out.println("System " + system_index + " on device " + Name() + " not found, discarding data.");
			System.out.println("System indices are: ");
			for(Short s:systems.keySet())
			{
				System.out.println(s);
			}
		}
	}
	
	public void ReceiveHistory(ByteBuffer data)
	{
		short system_index = data.getShort();
		IO_System_Logged system = System(system_index);
		if(system != null)
		{				
			system.ReceiveHistory(data);
			log.info("Received history from " + system.Name());
		}
	}

	public void ClientDisconnected()
	{
		for(IO_System_Logged sys:systems.values())
		{
			sys.ClientDisconnected();
		}
	}
	
	public abstract void Send_Command(Command update_command);
}
