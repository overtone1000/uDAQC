package udaqc.io;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class IO_Node 
{
	protected short command_description;
	protected String name = "Unnamed Node";
	protected IO_Group parent;
	protected int data_bytecount;
	
	public IO_Node(IO_Node basis)
	{
		//This is for creating children from an already populated parent IO_Node.
		//The bytebuffer has already been read partially by the basis argument.
		//Call this to copy members into the child instance and then further read through the bytebuffer with the child's constructor.
		this.command_description=basis.command_description;
		this.name=basis.name;
		this.parent=basis.parent;
		this.data_bytecount=basis.data_bytecount;
	}
	
	public IO_Node(ByteBuffer data)
	{
		command_description=data.getShort();
		data_bytecount = data.getInt();
		short name_length = data.getShort();
		byte[] name_bytes = new byte[name_length];
		if(name_length>data.remaining())
		{
			System.err.println("Malformed data.");
			return;
		}
		data.get(name_bytes);
		ByteBuffer name_bb = ByteBuffer.wrap(name_bytes);
		name = StandardCharsets.UTF_8.decode(name_bb).toString();
		System.out.println(name + ", size = " + data_bytecount);
	}
	
	public IO_Node(IO_Group parent, ByteBuffer data)
	{		
		this(data);
		this.parent = parent;
	}
	
	public void InterpretData(ByteBuffer data)
	{
		System.out.println("No overload of data interpretation.");
		byte[] data_bytes = new byte[data_bytecount];
		data.get(data_bytes);
		//System.out.println(name + " " + data_bytes);
	}
	
	@Override
	public String toString()
	{
	    return this.Name(); //For TreeItem use, this needs to return name only
	}
	
	public String Name()
	{
		return this.name;
	}
	
	public String FullName()
	{
		String retval="";
		if(parent!=null)
		{retval+=parent.FullName() + System.getProperty("file.separator");}
		retval += this.Name();
		return retval;
	}
	
	public short IO_Type()
	{
		return command_description;
	}
}
