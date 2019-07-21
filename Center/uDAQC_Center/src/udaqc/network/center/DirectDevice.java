package udaqc.network.center;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.apache.mina.core.session.IoSession;

import udaqc.io.log.IO_System_Logged;
import udaqc.network.Device;
import udaqc.network.center.command.Command;

public class DirectDevice implements Device 
{	
	private IO_System_Logged system = null;
	public IO_System_Logged System() {return system;}
	
	protected Center parent;
	protected IoSession session;
	
	public DirectDevice(Path path, ByteBuffer data, Center parent, IoSession session) 
	{
		this.parent=parent;
		this.session=session;
		system = IO_System_Logged.processSystemDescription(path, data, parent);
		system.setDevice(this);
	}

	@Override
	public void Send_Command(Command command)
	{
		if(session.isConnected())
		{
			System.out.println("Sending command " + command.Header().command_id + " to device " + this.system.FullName());
			session.write(command);
		}
		else
		{
			System.out.println("Session is diconnected.");
		}
	}
}