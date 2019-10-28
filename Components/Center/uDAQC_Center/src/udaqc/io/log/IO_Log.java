package udaqc.io.log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.Semaphore;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import logging.Point;
import udaqc.io.IO_Value;
import udaqc.io.log.IO_System_Logged.Regime;

public class IO_Log
{	
	private DateTime last_update = DateTime.now();
	private Duration datum_duration=Duration.ZERO;
	private Vector<IO_Value> values;
	
	private Semaphore deletion_mutex = new Semaphore(1);
	
	private RandomAccessFile file;
	
	private long max_file_size;
	private int max_entries;
	private int entry_length;
	
	private boolean start_new_epoch=true; //Initialize to true
	private boolean split_epoch_next=false;
	
	public long Size()
	{
		return max_file_size;
	}
	
	private static class ByteFlag
	{
		public static final int length = 1;
		public static enum EntryFlags
		{
			NewEpoch,
			SplitEpoch;
		}
		private byte flags;
		private static byte FlagByte(EntryFlags flag)
		{
			return (byte)(((byte)1)<<flag.ordinal());
		}
		public boolean Get(EntryFlags flag)
		{
			boolean retval = (flags&FlagByte(flag))!=0;
			return retval;
		}
		public void Set(EntryFlags flag, boolean value)
		{
			if(value)
			{
				flags |= FlagByte(flag);
			}
			else
			{
				flags &= (~FlagByte(flag));
			}
			
		}
		public void BBPut(ByteBuffer bb)
		{
			bb.put(flags);
		}
		public void BBGet(ByteBuffer bb)
		{
			flags=bb.get();
		}
	}
	
	private IO_System_Logged parent;
	private Regime regime;
	public IO_Log(Path path, Regime r, IO_System_Logged parent, long file_size)
	{
		if(path==null || parent==null)
		{
			return;
		}
		
		this.parent = parent;
		this.regime=r;
		
		this.values = parent.GetNestedValues();
		current_epoch = new Epoch(values);
		
		entry_length = ByteFlag.length + Long.BYTES + Float.BYTES*values.size();
		
		max_entries = (int)(file_size/entry_length);
		max_file_size = max_entries*entry_length;
		
		//max_entries=20;
		
		try
		{
			Files.createDirectories(path.getParent());
			if(Files.exists(path)) 
			{
				file = new RandomAccessFile(path.toFile(),"rw");
				Load();
			}
			else
			{
				if(path.getParent()!=null)
				{
					Files.createDirectories(path.getParent());
				}
				file = new RandomAccessFile(Files.createFile(path).toFile(),"rw");
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		/*
		System.out.println("Points loaded from file:");
		for(IO_Value val : values)
		{
			LinkedList<ArrayList<Point>> list = this.getPoints(val);
			for(ArrayList<Point> al : list)
			{
				for(Point p : al)
				{
					System.out.println(p.toString());
				}
			}
		}
		*/
		
	}
	public IO_Log(Path path, Regime r, IO_System_Logged parent, long file_size, Duration duration)
	{
		this(path,r,parent,file_size);
		
		if(duration!=null)
		{
			this.datum_duration=duration;
		}
	}
	
	private void Load() throws IOException
	{
		System.out.println("Loading from file.");
		
		file.seek(0);
		if(file.length()%entry_length!=0)
		{
			System.out.println("Poorly formed file. Erasing.");
			file.setLength(0);
			return;
		}
		ByteBuffer bb = ByteBuffer.allocate((int)file.length());
		bb.order(ByteOrder.LITTLE_ENDIAN);
		file.readFully(bb.array());
		
		if(bb.remaining()<entry_length)
		{
			return;
		}
		
		Epoch new_epoch = new Epoch(values);
		Epoch split_epoch=null;
		DateTime oldest_time=new DateTime();
		DateTime last_time=new DateTime(0);
		DateTime this_time=null;
		int oldest_index=0;
		int this_index=0;
		ByteFlag flag=new ByteFlag();
		
		ByteBuffer entry = ByteBuffer.allocate(entry_length);
		entry.order(ByteOrder.LITTLE_ENDIAN);
		
		while(bb.remaining()>=entry_length)
		{
			bb.get(entry.array());
			entry.position(0);
			
			flag.BBGet(entry);
			
			if(flag.Get(ByteFlag.EntryFlags.NewEpoch))
			{
				//System.out.println("New epoch at " + this_index);
				AddEpoch(new_epoch);
				new_epoch = new Epoch(values);
			}
			if(flag.Get(ByteFlag.EntryFlags.SplitEpoch))
			{
				//System.out.println("Epoch split at " + this_index);
				split_epoch=new_epoch;
			}
			
			if(new_epoch!=null)
			{
				this_time = new DateTime(entry.getLong());
				
				if(last_time.isAfter(this_time))
				{
					System.out.println("Older time at " + this_index);
					AddEpoch(new_epoch);
					new_epoch = new Epoch(values);
					
					if(oldest_time.isAfter(this_time))
					{
						oldest_index=this_index;
						oldest_time=this_time;
					}
				}
				
				new_epoch.times.add(this_time);
				for(IO_Value value:values)
				{
					new_epoch.points.get(value).add(new Point(this_time,entry.getFloat()));
				}
			}
						
			last_time=this_time;
			this_index++;
		}
		
		if(split_epoch!=null)
		{
			if(split_epoch==new_epoch)
			{
				System.out.println("Wait, this shouldn't happen!");
			}
			else
			{
				epochs.remove(split_epoch);
				for(int n=0;n<split_epoch.size();n++) 
				{
					new_epoch.times.add(split_epoch.times.get(n));
					for(IO_Value value:values)
					{
						new_epoch.points.get(value).add(split_epoch.points.get(value).get(n));
					}
				}
			}
		}
		
		AddEpoch(new_epoch);
		
		int file_position=this_index*entry_length; //this_index get left on the spot for the next entry, so seek there for the next write
		//if pointer is too high, start at oldest index instead
		if(file_position>=max_file_size-1)
		{
			file_position=oldest_index*entry_length;
			
		}
		System.out.println("Seeking file to position " + file_position + ", max size is " + max_file_size);
		file.seek(file_position);
		
	}
	private void AddEpoch(Epoch new_epoch)
	{
		if(new_epoch!=null)
		{
			if(!new_epoch.isEmpty())
			{
				int insert_index = 0;
				for(insert_index = 0;insert_index<epochs.size();insert_index++)
				{
					if(epochs.get(insert_index).StartTime().isAfter(new_epoch.StartTime()))
					{
						break;
					}
				}
				epochs.add(insert_index,new_epoch);
			}
		}
	}
	public void UpdateFromSystem(DateTime now)
	{
		//should update log here too
		ByteBuffer bb = GetEntryByteBuffer(now);
		bb.order(ByteOrder.LITTLE_ENDIAN);
						
		current_epoch.times.add(now);
		
		for(IO_Value key:values)
		{
			Float val = key.ValueAsFloat();
			current_epoch.points.get(key).add(new Point(now,val));
			bb.putFloat(val);
		}
		
		WriteEntry(bb);
	}
	
	public void Trim()
	{
		try
		{
			deletion_mutex.acquire();
			//This is called by WriteEntry function
			int count=0;
			for(Epoch e:epochs)
			{
				count += e.size();
			}
			count+=current_epoch.size();
			if(count>max_entries)
			{
				RemoveOldest(count-max_entries);
			}
			deletion_mutex.release();
		} catch (InterruptedException e1)
		{
			e1.printStackTrace();
		}
	}
	
	private int RemoveOldest(int number_to_remove)
	{
		int removed=0;
		while(removed<number_to_remove)
		{
			Epoch oldest_epoch = OldestEpoch();
			if(oldest_epoch.isEmpty())
			{
				return removed;
			}
			removed+=oldest_epoch.TryRemoveFirst(number_to_remove-removed);
		}
		return removed;
	}
	
	private Epoch OldestEpoch()
	{
		for(int n=0;n<epochs.size();n++)
		{
			if(epochs.get(n).isEmpty())
			{
				epochs.remove(n);
			}
		}
		if(epochs.isEmpty())
		{
			return current_epoch;
		}
		Iterator<Epoch> it = epochs.iterator();
		Epoch retval = it.next();
		while(it.hasNext())
		{
			Epoch test_epoch=it.next();
			if(test_epoch.times.get(0).isBefore(retval.times.get(0)))
			{
				retval=test_epoch;
			}
		}
		
		return retval;
	}
	
	public void UpdateFromAggregation(DateTime now, IO_Log source_log)
	{
		DateTime next_update = last_update.plus(datum_duration);
			
		if(now.isAfter(next_update))
		{
			//should update log here too
			ByteBuffer bb = GetEntryByteBuffer(now);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			
			Interval interval = new Interval(last_update,next_update);
			last_update=new DateTime(next_update);
			
			//System.out.println("Aggregating " + dest_regime.toString());
			
			if(source_log.current_epoch.times.isEmpty())
			{
				return;
			}
			
			current_epoch.times.add(now);
			//dest_log.datalog.add(null);//The aggregates currently don't contain data logs, but the null placeholder is needed so collection counts are correct for trimming
			for(IO_Value value:values)
			{
				Iterator<Point> iterator = source_log.current_epoch.points.get(value).iterator();
				
				Point new_point=new Point(now,0.0f);
				int size=0;
				while(iterator.hasNext())
				{
					Point p = iterator.next();
					if(interval.contains(p.x))
					{
						new_point.y+=p.y;
						size++;
					}
				}
				
				if(size==0)
				{
					System.out.println("Current epoch is too small. Cancelling aggregation.");
					break;
				}
				
				new_point.y/=size;
				
				//System.out.println("New historical point:");
				//System.out.println(size);
				//System.out.println(new_point.toString());
				
				current_epoch.points.get(value).add(new_point);
				bb.putFloat(new_point.y);
			}
			
			WriteEntry(bb);
		}
	}
	
	private ByteBuffer GetEntryByteBuffer(DateTime time)
	{
		ByteBuffer bb = ByteBuffer.allocate(entry_length);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.position(0);
		
		ByteFlag flag = new ByteFlag();
		flag.Set(ByteFlag.EntryFlags.NewEpoch, start_new_epoch);
		flag.Set(ByteFlag.EntryFlags.SplitEpoch, split_epoch_next);
		start_new_epoch=false;
		split_epoch_next=false;
		
		flag.BBPut(bb);
		bb.putLong(time.getMillis());
		return bb;
	}
	
	private void WriteEntry(ByteBuffer bb)
	{
		Epoch oldest = OldestEpoch();
		Long first_timestamp;
		if(oldest!=null)
		{
			first_timestamp=oldest.StartTime().getMillis();
		}
		else
		{
			first_timestamp=0L;
		}
		parent.HistoryUpdate(regime, first_timestamp, bb);
		bb.rewind();
		
		try
		{
			file.write(bb.array());
			if(file.getFilePointer()>=max_file_size-1)
			{
				file.seek(0);
				split_epoch_next=true;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		Trim();
	}
	
	public LinkedList<ArrayList<Point>> getPoints(IO_Value value)
	{
		LinkedList<ArrayList<Point>> retval = new LinkedList<ArrayList<Point>>();
		for(Epoch e:epochs)
		{
			retval.add(e.points.get(value));
		}
		retval.add(current_epoch.points.get(value));
		return retval;
	}
	
	private static class Epoch
	{
		public Epoch(Vector<IO_Value> values)
		{
			for(IO_Value value:values)
			{
				points.put(value, new ArrayList<Point>());
			}
		}
		public ArrayList<DateTime> times = new ArrayList<DateTime>();
		public HashMap<IO_Value,ArrayList<Point>> points = new HashMap<IO_Value,ArrayList<Point>>();
		public int TryRemoveFirst(int to_remove)
		{
			int size = times.size();
			if(size<=to_remove)
			{
				times.clear();
				for(IO_Value value:points.keySet())
				{
					points.get(value).clear();
				}
				return to_remove-size;
			}
			else
			{
				times=new ArrayList<DateTime>(times.subList(to_remove, size));
				for(IO_Value value:points.keySet())
				{
					ArrayList<Point> new_points = new ArrayList<Point>(points.get(value).subList(to_remove, size));
					points.put(value, new_points);
				}
				return to_remove;
			}
		}
		public boolean isEmpty()
		{
			return times.isEmpty();
		}
		public int size()
		{
			return times.size();
		}
		public DateTime StartTime()
		{
			if(isEmpty())
			{
				return new DateTime(0);
			}
			else
			{
				return times.get(0);
			}
		}
		public DateTime EndTime()
		{
			if(isEmpty())
			{
				return new DateTime(0);
			}
			else
			{
				return times.get(times.size()-1);
			}
		}
	}
	
	private LinkedList<Epoch> epochs=new LinkedList<Epoch>();
	private Epoch current_epoch;
	
	private static final int x_bins = 1024;
	public static class LogSubset
	{
		public enum LossyDataTypeFlags
		{
			Raw,
			Max,
			Min;
			public byte toFlag()
			{
				return (byte)(1<<this.ordinal());
			}
			public boolean checkForFlag(byte flags)
			{
				return (this.toFlag()&flags)!=0;
			}
		}
		
		private byte flags;
		public byte Flags() {return flags;}
		
		private Vector<DateTime> times = new Vector<DateTime>();
		private HashMap<LossyDataTypeFlags,Vector<Float[]>> values = new HashMap<LossyDataTypeFlags,Vector<Float[]>>();
		
		public LogSubset()
		{
			for(LossyDataTypeFlags f:LossyDataTypeFlags.values()) 
			{
				values.put(f,new Vector<Float[]>());
			}
		}
		
		private boolean binning;
		private int datum_count;
		private int bin_count;
		private int pointer;
		public void SetBinning(int count, DateTime start, DateTime stop, int bins)
		{
			this.binning=count>x_bins;
			this.datum_count=count;
			this.bin_count=bins;
			
			if(binning)
			{
				this.flags=LossyDataTypeFlags.Raw.toFlag();
				times.setSize(bin_count);
				values.get(LossyDataTypeFlags.Raw).setSize(bin_count);
				
				double one = 1;
				double denom = bin_count-1;
				for(int n=0;n<bin_count;n++)
				{
					double along = n/one;
					DateTime this_time = start.getMillis()*(one-along)+stop.getMillis()*along
					times.set(n, element)
				}
			}
			else
			{
				this.flags=(byte)(LossyDataTypeFlags.Max.toFlag() | LossyDataTypeFlags.Min.toFlag());
				times.setSize(datum_count);
				values.get(LossyDataTypeFlags.Max).setSize(datum_count);
				values.get(LossyDataTypeFlags.Min).setSize(datum_count);
			}
						
			pointer=0;
		}
		public void AddDatum(DateTime new_time, Float[] new_values)
		{
			if(binning)
			{
				
			}
			else
			{
				times.set(pointer, new_time);
				values.get(LossyDataTypeFlags.Raw).set(pointer, new_values.clone());
			}
			
			pointer++;
		}
	}
	
	public LogSubset GetSubset(DateTime start, DateTime end)
	{
		LogSubset retval = new LogSubset();
		
		try
		{
			deletion_mutex.acquire();
			
			Vector<Vector<Integer>> includes = new Vector<Vector<Integer>>();
			for(Epoch e:epochs)
			{
				Vector<Integer> next_epoch = new Vector<Integer>();
				if(!(e.EndTime().isBefore(start) || e.StartTime().isAfter(end)))
				{
					for(Integer n = 0; n<e.times.size(); n++)
					{
						DateTime dt = e.times.get(n);
						if(dt.isAfter(start) && dt.isBefore(end))
						{
							next_epoch.add(n);
						}
					}
				}
				includes.add(next_epoch);
			}
			
			int count=0;
			for(Vector<Integer> vec:includes)
			{
				count+=vec.size();
			}
			
			retval.SetBinning(count,start,end,x_bins);
			
			Float[] vals = new Float[values.size()];
			for(int ep_ind=0;ep_ind<epochs.size();ep_ind++)
			{
				Epoch e = epochs.get(ep_ind);
				for(Integer n:includes.get(ep_ind))
				{
					for(int v_ind=0;v_ind<vals.length;v_ind++)
					{
						vals[v_ind]=e.points.get(values.get(v_ind)).get(n).y;
					}
					retval.AddDatum(e.times.get(n),vals);
				}
			}
			
			deletion_mutex.release();
			
		} catch (InterruptedException e1)
		{
			e1.printStackTrace();
		}
		
		return retval;
	}
	
	public void CloseCurrentEpoch()
	{
		epochs.add(current_epoch);
		current_epoch=new Epoch(values);
		start_new_epoch=true;
	}
}
