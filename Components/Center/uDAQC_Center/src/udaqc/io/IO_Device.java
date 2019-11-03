package udaqc.io;

import java.nio.ByteBuffer;
import java.util.Vector;

import udaqc.network.center.Center;

public abstract class IO_Device extends IO_Group
{
	public IO_Device(ByteBuffer data)
	{
		super(data);
		System.out.println("IO_Device created, cloning description.");
		data.position(0);
		this.description=data.array().clone();
	}
	
	@Override
	protected void Construct(ByteBuffer data)
	{
		System.out.println("Constructing IO_Device from description.");
		short member_count = data.getShort();
		System.out.println(member_count + " members expected.");
		members.clear();
		for (short n = 0; n < member_count; n++)
		{
			IO_Node new_item = new IO_Node(this, data);
			switch (new_item.command_description)
			{
			case IO_Constants.Command_IDs.system_description:
				IO_System new_sys= new IO_System(new_item, data, this, n);
				members.add(new_sys);
				System.out.println("Added " + new_sys.Name());
				break;
			default:
				System.err.println("Unanticipated node command description in IO_Device. Description interpretation likely erronious.");
				break;
			}
		}
		System.out.println("Construction complete.");
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
