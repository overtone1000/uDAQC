package udaqc.io;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Iterator;

import udaqc.jdbc.Database_uDAQC.Regime;
import udaqc.network.center.Center;
import udaqc.network.center.IO_Device_Connected;

public class IO_System extends IO_Group
{		
	protected IO_Device_Connected iodev;
	protected short system_index=-1;
	
	private int raw_history_entry_size=-1;
	private int aggregate_history_entry_size=-1;
	public static final int aggregates_per_value = 3;
	public int HistoryEntrySize(Regime reg)
	{
		if(reg==Regime.raw)
		{
			return raw_history_entry_size;
		}
		else
		{
			return aggregate_history_entry_size;
		}
	}
	private void CalcEntrySizes()
	{
		//long timestamp, either way
		raw_history_entry_size=Long.BYTES; 
		aggregate_history_entry_size=Long.BYTES;
		
		//flag
		raw_history_entry_size+=Byte.BYTES;
		aggregate_history_entry_size+=Byte.BYTES;

		Iterator<IO_Value> i = GetNestedValues().iterator();
		i.next(); //ditch timestamp
		while(i.hasNext())
		{
			IO_Value v = i.next();
			raw_history_entry_size+=v.Size();
			if(v.getDataType()==udaqc.io.IO_Constants.DataTypes.bool)
			{
				aggregate_history_entry_size+=Float.BYTES*aggregates_per_value; //Gets converted to a float
			}
			else
			{
				aggregate_history_entry_size+=v.Size()*aggregates_per_value;
			}
		}
	}
	
	public Short Index()
	{
		return system_index;
	}
	
	public IO_System(ByteBuffer data, IO_Device_Connected iodev, Short index) {
		super(data);
		this.iodev=iodev;
		this.system_index=index;
		this.CalcEntrySizes();
	}
	
	public IO_System(IO_Node basis, ByteBuffer data, IO_Device_Connected iodev, Short index)
	{
		super(basis,data);
		this.iodev=iodev;
		this.system_index=index;
		this.CalcEntrySizes();
	}
	
	public void ReceiveData(ByteBuffer data)
	{
		//If this function is not overridden, just interpret the data.
		super.InterpretData(data);
	}
		
	//Use this function to create an IO_System from file (gets its description)
	/*
	public static IO_System fromFile(Path p, IO_Device_Connected device)
	{
		IO_System retval=null;
		
		try
		{
			ByteBuffer bb = ByteBuffer.allocate((int) new File(p.toString()).length());

			File f = new File(p.toString());
			if (!(f.isFile() && f.canRead()))
			{
				System.out.println("File doesn't exist or can't be read: " + p.toString());
				return null;
			}

			FileInputStream filein = new FileInputStream(p.toString());
			filein.read(bb.array());
			filein.close();
			retval = new IO_System(bb, device);
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
		return retval;
	}
	*/
	
	/*
	public void ModifyValue(short index, byte[] value)
	{
		if(iodev.DirectDev()==null) {return;}
		
		IoBuffer message = IoBuffer.allocate(value.length + Short.BYTES);
		message.order(ByteOrder.LITTLE_ENDIAN);
		message.putShort(index);
		message.put(value);
		Command c=new Command(IO_Constants.Command_IDs.modifiablevalue_modification,message.array());
		
		iodev.DirectDev().Send_Command(c);
	}
	*/
	
	public IO_Device_Connected Device()
	{
		return iodev;
	}
	
	public void ClientDisconnected()
	{
		Center.database.closeEpoch(this);
	}
	
	public Instant getTimestamp()
	{
		return iodev.GetTimestamp(this.system_index);
	}
}
