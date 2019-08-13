package udaqc.network.center;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.apache.mina.transport.socket.nio.NioSession;
import udaqc.io.log.IO_System_Logged;
import udaqc.network.Constants;
import udaqc.network.Device;
import udaqc.network.center.command.Command;

public class DirectDevice extends Device
{	
	private IO_System_Logged system = null;
	public IO_System_Logged System() {return system;}
	
	protected Center center;
	protected NioSession session;
	
	public DirectDevice(Path path, ByteBuffer data, Center center, NioSession session) 
	{
		this.center=center;
		this.session=session;
		system = IO_System_Logged.processSystemDescription(path, data, center);
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

	@Override
	public InetSocketAddress TimeSyncAddress()
	{
		InetAddress add = ((InetSocketAddress)session.getRemoteAddress()).getAddress();
		if(add==null) {return null;}
		InetSocketAddress retval = new InetSocketAddress(add,Constants.Addresses.udp_port);
		return retval;
	}

	@Override
	public boolean KeepSynchronized()
	{
		return session.isConnected();
	}
}