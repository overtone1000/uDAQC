package udaqc.io;

import java.nio.ByteBuffer;
import udaqc.network.center.Center;

public class IO_System extends IO_Group
{		
	protected IO_Device iodev;
	protected short system_index=-1;
	
	public Short Index()
	{
		return system_index;
	}
	
	public IO_System(ByteBuffer data, IO_Device iodev, Short index) {
		super(data);
		this.iodev=iodev;
		this.system_index=index;
	}
	
	public IO_System(IO_Node basis, ByteBuffer data, IO_Device iodev, Short index)
	{
		super(basis,data);
		this.iodev=iodev;
		this.system_index=index;
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
	
	public IO_Device Device()
	{
		return iodev;
	}
	
	public void ClientDisconnected()
	{
		Center.database.closeEpoch(this);
	}
	
}
