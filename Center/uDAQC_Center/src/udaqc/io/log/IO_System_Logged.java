package udaqc.io.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.mina.core.session.IoSession;
import org.eclipse.jetty.websocket.api.Session;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import logging.Point;
import udaqc.io.IO_System;
import udaqc.io.IO_Value;
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.network.center.Center;
import udaqc.network.center.command.Command;
import udaqc.network.interfaces.HistoryUpdateHandler;
import udaqc.network.passthrough.command.PT_Command;
import udaqc.network.passthrough.endpoints.Endpoint;


public class IO_System_Logged extends IO_System
{
	private static TreeMap<Short, IO_System_Logged> systems=new TreeMap<Short, IO_System_Logged>();
	private static Semaphore system_mutex=new Semaphore(1);
	
	//default is 10 Mb for now, no way to change it programmatically without bigger modifications
	//probably would be better to make it part of the actualy IO_System description so it can be different for different devices
	private int file_size=1024*1024*10;
	
	public static IO_System_Logged getSystem(short id)
	{
		return systems.get(id);
	}
	
	private short id=-1;
	public short getSystemID()
	{
		return id;
	}
		
	public static void PassthroughInitialization(Endpoint ep)
	{
		for(Short id:systems.keySet())
		{
			IO_System_Logged system = systems.get(id);
			
			System.out.println("Sending descriptions to passthrough.");
			Command c = new Command(Command_IDs.group_description,system.description);
			PT_Command ptc = new PT_Command(id,c);
			ep.SendCommand(ptc);
			
			for(Regime r:Regime.values())
			{
				System.out.println("Sending regime " + r.toString() + " to passthrough.");
				File file = system.regPath(r).toFile();
				FileInputStream f;
				try
				{
					f = new FileInputStream(file);
				} catch (FileNotFoundException e)
				{
					e.printStackTrace();
					break;
				}
				
				ByteBuffer message = ByteBuffer.allocate((int) (Integer.BYTES + Long.BYTES + file.length()));
				message.order(ByteOrder.LITTLE_ENDIAN);
				message.putInt(r.ordinal());
				
				message.putLong(system.logs.get(r).Size());
				//message.putLong(500L);
				//message.putLong(-500L);
				//message.putLong(254305453037L);
				//message.putLong(-254305453037L);
				//message.putLong(9007199254740992L); //unsafe for javascript
				//System.err.println("Message length is still in a non-functional testing mode.");
								
				try
				{
					f.read(message.array(),message.position(),message.remaining());
					f.close();
				} catch (IOException e)
				{
					e.printStackTrace();
					break;
				}
				
				//System.out.println("Message is: ");
				//for(int n=0;n<message.array().length;n++)
				//{
				//	System.out.println(Integer.toBinaryString((char)(message.array()[n])));
				//}
				
				Command hc = new Command(Command_IDs.history,message.array());
				PT_Command pthc = new PT_Command(id,hc);
				ep.SendCommand(pthc);
			}
		}
	}
	
	public static LinkedList<IO_System_Logged> Systems()
	{
		return new LinkedList<IO_System_Logged>(systems.values());
	}
	public static void LoadSavedSystems(Path path, HistoryUpdateHandler his_update_handler)
	{
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
			for(File system:path.toFile().listFiles())
			{
				if(system.isDirectory())
				{
					File descfile = new File(system+filesep+descname);
					if(descfile.exists())
					{
						ByteBuffer data = ByteBuffer.wrap(Files.readAllBytes(descfile.toPath()));
						data.position(0);
						data.order(ByteOrder.LITTLE_ENDIAN);
						processSystemDescription(path,data,his_update_handler); //use the getSystem function for thread safety because it uses the mutex and makes sure this isn't a duplicate
					}
				}
			}
		} catch (Exception e)
		{
			System.out.println(ExceptionUtils.getStackTrace(e));
		}
	}
	
	public static IO_System_Logged processSystemDescription(Path path, ByteBuffer data, HistoryUpdateHandler his_update_handler)
	{
		try
		{
			system_mutex.acquire();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
			return null;
		}
		IO_System_Logged new_system = new IO_System_Logged(path,data,his_update_handler);
		if(new_system.system_IsKnown())
		{
			for(IO_System_Logged sys:systems.values())
			{
				if(new_system.isEqual(sys))
				{
					system_mutex.release();
					return sys;
				}
			}
		}

		new_system.description_toFile();
		new_system.InitLogs();
		
		new_system.id=0;
		while(systems.keySet().contains(new_system.id))
		{
			new_system.id++;
		}
		systems.put(new_system.id,new_system);
		
		system_mutex.release();
		
		return new_system;
	}
	
	private Logger log;
	private static String suffix = ".dat";
	private Path storage_path;
	public final static String filesep=System.getProperty("file.separator");
	public final static String descname ="descripton";

	public enum Regime
	{
		Live, Minute, Hour, Day;
	}

	private TreeMap<Regime,IO_Log> logs = new TreeMap<Regime,IO_Log>();
	
	//private static TreeMap<Regime,Duration> to_keep=new TreeMap<Regime,Duration>();
	private static TreeMap<Regime,Regime> aggregate_from=new TreeMap<Regime,Regime>();
	private static TreeMap<Regime,Duration> regime_duration=new TreeMap<Regime,Duration>();
	private static boolean debug=true;
	static
	{
		regime_duration.put(Regime.Live, Duration.ZERO);
		if(debug)
		{
			regime_duration.put(Regime.Minute, Duration.standardSeconds(5));
			regime_duration.put(Regime.Hour, Duration.standardSeconds(15));
			regime_duration.put(Regime.Day, Duration.standardSeconds(30));
		}
		else
		{
			regime_duration.put(Regime.Minute, Duration.standardMinutes(1));
			regime_duration.put(Regime.Hour, Duration.standardHours(1));
			regime_duration.put(Regime.Day, Duration.standardDays(1));
		}
	
		//to_keep.put(Regime.Live, Duration.standardMinutes(1));
		//to_keep.put(Regime.Minute, Duration.standardHours(1));
		//to_keep.put(Regime.Hour, Duration.standardDays(1));
		//to_keep.put(Regime.Day, Duration.standardDays(30));
		
		aggregate_from.put(Regime.Minute, Regime.Live);
		aggregate_from.put(Regime.Hour, Regime.Minute);
		aggregate_from.put(Regime.Day, Regime.Hour);
	}
	
	private HistoryUpdateHandler his_update_handler;
	private IO_System_Logged(Path path, ByteBuffer data, HistoryUpdateHandler his_update_handler)
	{
		super(data);
				
		this.storage_path = Paths.get(path.toString() + filesep + this.FullName());
		this.his_update_handler = his_update_handler;
		// need to check whether basis is already stored in storage path and that it's
		// the same. If it isn't, wipe out the history.
	}
	public void HistoryUpdate(Regime r, Long first_timestamp, ByteBuffer bb)
	{
		his_update_handler.HistoryUpdated(this, r, first_timestamp, bb);
	}
	
	private Path regPath(Regime reg)
	{
		return Paths.get(storage_path.toString() + filesep + reg.toString());
	}
	
	private void InitLogs()
	{
		for(Regime r:Regime.values())
		{
			System.out.println("Loading regime " + r.toString());
			IO_Log l = new IO_Log(regPath(r),r,this,file_size,regime_duration.get(r));
			logs.put(r, l);
		}
	}
	
	public LinkedList<ArrayList<Point>> getPoints(Regime r, IO_Value value)
	{
		return logs.get(r).getPoints(value);
	}

	@Override
	public void ReceiveData(ByteBuffer data)
	{	
		InterpretData(data);
		
		DateTime now = this.GetTimestamp();
		
		logs.get(Regime.Live).UpdateFromSystem(now);
				
		//need to create minute, hour, and day data and save to files
		for(Regime dest_regime:aggregate_from.keySet())
		{
			Regime source_regime=aggregate_from.get(dest_regime);
			
			logs.get(dest_regime).UpdateFromAggregation(now, logs.get(source_regime));
		}
	}
	
	public void ReceiveHistory(ByteBuffer data)
	{
		// This CANNOT work like ReceiveData because the logs don't contain the data, they contain floats
		// Should probably just transmit the file wholesale and reload the system when it's all done
		
		int regint = data.getInt();
		long filesize = data.getLong();
		Regime r = Regime.values()[regint];
		
		System.out.println("Receiving history for " + r.toString());
		
		File file = regPath(r).toFile();
		FileOutputStream f;
		
		try
		{
			System.out.println("Opening fileoutputstream");
			f = new FileOutputStream(file);
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
		
		try
		{
			System.out.println("Writing file to byte array.");
			f.write(data.array(),data.position(),data.remaining());
			f.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
		
		System.out.println("Replacing IO_Log.");
		
		
		logs.put(r,new IO_Log(regPath(r),r,this,filesize));
		
		System.out.println("Finished processing history.");
	}

	public boolean isEqual(Object o)
	{
		return o instanceof IO_System_Logged && //this works
				java.util.Arrays.equals(description,((IO_System_Logged) o).description) && //this works
				storage_path.equals(((IO_System_Logged) o).storage_path); //this works
	}
	
	protected boolean system_IsKnown() //returns true if had to be written. If d
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
	
	public void ClientDisconnected()
	{
		for(IO_Log log:logs.values())
		{
			log.CloseCurrentEpoch();
		}
	}
	}
