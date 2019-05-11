package udaqc.io;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Vector;


public class IO_Group extends IO_Reporter
{
	protected Vector<IO_Reporter> members = new Vector<IO_Reporter>();

	public final Vector<IO_Reporter> GetMembers()
	{
		return members;
	}

	public IO_Group(IO_Reporter basis, ByteBuffer data)
	{
		super(basis);
		Construct(data);
	}

	public IO_Group(ByteBuffer data)
	{
		super(data);
		Construct(data);
	}
	
	public Vector<IO_Value> GetNestedValues()
	{
		Vector<IO_Value> retval=new Vector<IO_Value>();
		for(IO_Reporter member:members)
		{
			switch(member.command_description)
			{
			case IO_Constants.Command_IDs.value_description:
			case IO_Constants.Command_IDs.modifiablevalue_description:
				retval.add((IO_Value)member);
				break;
			case IO_Constants.Command_IDs.group_description:
				retval.addAll(((IO_Group)member).GetNestedValues());
				break;
			default:
				break;
			}
		}
		return retval;
	}
	
	private void Construct(ByteBuffer data)
	{
		short member_count = data.getShort();
		members.clear();
		for (short n = 0; n < member_count; n++)
		{
			IO_Reporter new_item = new IO_Reporter(this, data);

			switch (new_item.command_description)
			{
			case IO_Constants.Command_IDs.emptyreporter_description:
				// Nothing else, so do nothing.
				break;
			case IO_Constants.Command_IDs.group_description:
				// Groups then have a short showing number of members.
				new_item = new IO_Group(new_item, data);
				break;
			case IO_Constants.Command_IDs.modifiablevalue_description:
				new_item = new IO_ModifiableValue(new_item,data);
				break;
			case IO_Constants.Command_IDs.value_description:
				// Values then have a string containing their unit, another command telling
				// their type, and a short saying how many bytes their type is
				new_item = new IO_Value(new_item, data);
				break;
			default:
				System.out.println("Unanticipated reporter command description. Description interpretation likely erronious.");
				break;
			}
			members.add(new_item);
		}
	}

	@Override
	public void InterpretData(ByteBuffer data)
	{
		// System.out.println("Iterating through group " + name + " to interpret
		// data.");
		Iterator<IO_Reporter> it = members.iterator();
		while (it.hasNext())
		{
			it.next().InterpretData(data);
		}
	}

	// Use this to populate the data from a file
	public boolean dataFromFile(Path p)
	{
		try
		{
			ByteBuffer bb = ByteBuffer.allocate((int) new File(p.toString()).length());

			File f = new File(p.toString());
			if (!(f.isFile() && f.canRead()))
			{
				System.out.println("File doesn't exist or can't be read: " + p.toString());
				return false;
			}

			FileInputStream filein = new FileInputStream(p.toString());
			filein.read(bb.array());
			filein.close();
			InterpretData(bb);
			return true;
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
}