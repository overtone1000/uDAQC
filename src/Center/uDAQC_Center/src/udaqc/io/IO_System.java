package udaqc.io;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

import org.apache.mina.core.buffer.IoBuffer;
import org.joda.time.DateTime;

import udaqc.network.Device;
import udaqc.network.center.DirectDevice;
import udaqc.network.center.command.Command;

public class IO_System extends IO_Group
{		
	protected DirectDevice device = null;
	
	public IO_System(ByteBuffer data, DirectDevice device) {
		super(data);
		this.device=device;
	}
	
	public DateTime GetTimestamp()
	{
		DateTime retval=null;
		if(members!=null)
		{
			long us = (long)(((IO_Value)members.firstElement()).value)+device.timeZeroMicros();
			retval = new DateTime(us/1000);
		}
		return retval;
	}
	
	public void ReceiveData(ByteBuffer data)
	{
		//Rather than interpreting the data, pass it to the logger.
		//The logger and any other future functions and classes should call InterpretData function (in parent IO_Group object)
		//As needed
		
		//If this function is not overridden, just interpret the data.
		super.InterpretData(data);
	}
		
	//Use this function to create an IO_System from file (gets its description)
	public static IO_System fromFile(Path p, DirectDevice device)
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
	
	public void ModifyValue(short index, byte[] value)
	{
		if(device==null) {return;}
		
		IoBuffer message = IoBuffer.allocate(value.length + Short.BYTES);
		message.order(ByteOrder.LITTLE_ENDIAN);
		message.putShort(index);
		message.put(value);
		Command c=new Command(IO_Constants.Command_IDs.modifiablevalue_modification,message.array());
		
		device.Send_Command(c);
	}
	
	public Device Device()
	{
		return device;
	}
}
