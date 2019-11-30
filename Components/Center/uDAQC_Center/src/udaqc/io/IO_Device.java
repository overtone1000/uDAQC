package udaqc.io;

import java.nio.ByteBuffer;
import java.util.Vector;

public abstract class IO_Device extends IO_Group
{
	public IO_Device(ByteBuffer data)
	{
		super(data);
		System.out.println("IO_Device created, cloning description.");
		data.position(0);
		this.description=data.array().clone();
	}
	
	public IO_System System(Short s) {return (IO_System)GetMembers().get(s);}
	public Vector<IO_System> Systems() 
	{
		Vector<IO_System> retval = new Vector<IO_System>();
		for(IO_Node n:GetMembers())
		{
			retval.add((IO_System)n);
		}
		return retval;
	}


	public byte[] Description() {return description.clone();}
	
	protected byte[] description;
	
	public boolean isEqual(Object o)
	{
		return o instanceof IO_Device && //this works
				java.util.Arrays.equals(description,((IO_Device) o).description);
				//storage_path.equals(((IO_Device_Connected) o).storage_path); //this works
	}	
}
