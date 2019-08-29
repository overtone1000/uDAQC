package udaqc.io;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Vector;

import udaqc.io.log.IO_System_Logged;
import udaqc.network.center.DirectDevice;
import udaqc.network.interfaces.HistoryUpdateHandler;

public class IO_Device extends IO_Group
{

	private Path path;
	private HistoryUpdateHandler his_update_handler;
	private DirectDevice device;
	
	public Path DevicePath() {return path;}
	public HistoryUpdateHandler HistoryHandler() {return his_update_handler;}
	public DirectDevice DirectDev() {return device;}
	public IO_Device(Path path, ByteBuffer data, HistoryUpdateHandler his_update_handler, DirectDevice device)
	{
		super(data);
		this.path=path;
		this.his_update_handler=his_update_handler;
		this.device=device;
		for(IO_System_Logged sys:Systems())
		{
			sys.InitLogs();
		}
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
				IO_System_Logged new_sys= new IO_System_Logged(new_item, data, this, n);
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
	
	public IO_System_Logged System(Short s) {return (IO_System_Logged)GetMembers().get(s);}
	public Vector<IO_System_Logged> Systems() 
	{
		Vector<IO_System_Logged> retval = new Vector<IO_System_Logged>();
		for(IO_Node n:GetMembers())
		{
			retval.add((IO_System_Logged)n);
		}
		return retval;
	}
}
