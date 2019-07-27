package udaqc.network.center;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSession;

import network.udp.TimeSynchronizable;
import udaqc.io.log.IO_System_Logged;
import udaqc.network.Constants;
import udaqc.network.Device;
import udaqc.network.center.command.Command;

public class DirectDevice implements Device, TimeSynchronizable
{	
	private IO_System_Logged system = null;
	public IO_System_Logged System() {return system;}
	
	protected Center center;
	protected NioSession session;
	
	protected boolean synced=false;
	protected long time_zero=0;
	
	public DirectDevice(Path path, ByteBuffer data, Center center, NioSession session) 
	{
		this.center=center;
		this.session=session;
		system = IO_System_Logged.processSystemDescription(path, data, center);
		system.setDevice(this);
	}

	public boolean Synced()
	{
		return synced;
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
	public void Synchronize(long epoch_nanos_at_time_zero)
	{
		synced=true;
		time_zero=epoch_nanos_at_time_zero;
	}

	@Override
	public InetSocketAddress Address()
	{
		InetAddress add = ((InetSocketAddress)session.getRemoteAddress()).getAddress();
		InetSocketAddress retval = new InetSocketAddress(add,Constants.Addresses.udp_port);
		return retval;
	}
}