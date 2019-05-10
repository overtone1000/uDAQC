package gndm.network.center;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.apache.mina.core.session.IoSession;
import gndm.io.log.IO_System_Logged;
import gndm.network.GNDM_Device;
import gndm.network.center.command.Command;

public class GNDM_DirectDevice implements GNDM_Device 
{	
	private IO_System_Logged system = null;
	public IO_System_Logged System() {return system;}
	
	protected GNDM_Center parent;
	protected IoSession session;
	
	public GNDM_DirectDevice(Path path, ByteBuffer data, GNDM_Center parent, IoSession session) 
	{
		this.parent=parent;
		this.session=session;
		system = IO_System_Logged.processSystemDescription(path, data);
		system.setDevice(this);
	}

	@Override
	public void Send_Command(Command command)
	{
		if(session.isConnected())
		{
			session.write(command);
		}
		else
		{
			System.out.println("Session is diconnected.");
		}
	}
}