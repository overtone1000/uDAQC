package udaqc.io.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Vector;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import logging.Point;
import udaqc.io.IO_System;
import udaqc.io.IO_Value;
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.log.IO_System_Logged.Regime;
import udaqc.io.IO_Device;
import udaqc.io.IO_Node;
import udaqc.network.center.DirectDevice;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.command.PT_Command;
import udaqc.network.passthrough.endpoints.Endpoint;


public class IO_System_Logged extends IO_System
{	
	//default is 10 Mb for now, no way to change it programmatically without bigger modifications
	//probably would be better to make it part of the actualy IO_System description so it can be different for different devices
	private int file_size=1024*1024*10;	
	
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
	
	private Path StoragePath()
	{
		return Paths.get(iodev.DevicePath().toString() + DirectDevice.filesep + this.Name());
	}
	public IO_System_Logged(IO_Node basis, ByteBuffer data, IO_Device iodev, Short index)
	{
		super(basis,data,iodev,index);
		this.iodev=iodev;
	}
	public IO_System_Logged(ByteBuffer data, IO_Device iodev, Short index)
	{
		super(data, iodev ,index);
		this.iodev=iodev;
		// need to check whether basis is already stored in storage path and that it's
		// the same. If it isn't, wipe out the history.
	}
	public void HistoryUpdate(Regime r, Long first_timestamp, ByteBuffer bb)
	{
		iodev.HistoryHandler().HistoryUpdated(iodev.DirectDev(), this.system_index, r, first_timestamp, bb);
	}
	
	private Path regPath(Regime reg)
	{
		return Paths.get(StoragePath().toString() + DirectDevice.filesep + reg.toString());
	}
	
	public void InitLogs()
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

	
	
	public void ClientDisconnected()
	{
		for(IO_Log log:logs.values())
		{
			log.CloseCurrentEpoch();
		}
	}
	
	public void HandleLossyDataRequest(Endpoint ep, Regime regime, DateTime start, DateTime end)
	{
		/*
		boolean current_requested = end==null;
		//need to flag for future updates if this is true
		
		IO_Log log = logs.get(regime);
		
		Vector<Integer> epoch_counts = log.CountInRange(start, end);
		Integer total_raw_points = 0;
		for(Integer i:epoch_counts)
		{
			total_raw_points+=i;
		}
		boolean binning = total_raw_points>x_bins;
		Vector<IO_Value> values = this.GetNestedValues();
		Integer datum_size = Long.BYTES+Float.BYTES*values.size();
		Integer dataset_size;
		Integer set_count=0;
		Integer data_size;
		byte flags=0;
		
		if(binning)
		{
			dataset_size = datum_size*x_bins;
			flags = (byte) (LossyDataTypeFlags.Max.toFlag() | LossyDataTypeFlags.Min.toFlag());
		}
		else
		{
			dataset_size = datum_size*total_raw_points;
			flags = LossyDataTypeFlags.Raw.toFlag();
		}
		
		for(LossyDataTypeFlags thisflag:LossyDataTypeFlags.values())
		{
			if(thisflag.checkForFlag(flags))
			{
				set_count++;
			}
		}
		
		data_size=dataset_size*set_count;
		
		int message_length = 0;
		message_length += Short.BYTES;
		message_length += Byte.BYTES;
		message_length += Integer.BYTES*(1+epoch_counts.size());
		message_length += data_size;
		ByteBuffer message = ByteBuffer.allocate(message_length);
		message.order(ByteOrder.LITTLE_ENDIAN);
		message.putShort(system_index);
		message.put(flags);
		message.putInt(epoch_counts.size());
		for(int n=0;n<epoch_counts.size();n++)
		{
			message.putInt(epoch_counts.get(n));
		}
		for(LossyDataTypeFlags thisflag:LossyDataTypeFlags.values())
		{
			if(thisflag.checkForFlag(flags))
			{
				//write this into buffer
			}
		}
		*/
	}
	
	public void PassthroughInitialization(Endpoint ep, Integer regime, Long last_time)
	{
		Regime r = Regime.values()[regime];
		System.out.println("Sending regime " + r.toString() + " to passthrough.");
		File file = regPath(r).toFile();
		FileInputStream f;
		try
		{
			f = new FileInputStream(file);
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return;
		}
		
		ByteBuffer message = ByteBuffer.allocate((int) (Short.BYTES + Integer.BYTES + Long.BYTES + file.length()));
		message.order(ByteOrder.LITTLE_ENDIAN);
		message.putShort(system_index);
		message.putInt(r.ordinal());
		message.putLong(logs.get(r).Size());
		
		try
		{
			f.read(message.array(),message.position(),message.remaining());
			f.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
				
		Command hc = new Command(Command_IDs.history,message.array());
		PT_Command pthc = new PT_Command(iodev.DirectDev().DeviceIndex(),hc);
		ep.SendCommand(pthc);
	}
}
