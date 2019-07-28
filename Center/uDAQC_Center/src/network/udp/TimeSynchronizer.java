package network.udp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import udaqc.network.Constants;

public abstract class TimeSynchronizer
{
	protected boolean synced=false;
	protected long time_zero=0;
	
	public TimeSynchronizer()
	{
		
	}
	
	public boolean Synced()
	{
		return synced;
	}
	
	public void Synchronize(long epoch_micros_at_time_zero)
	{
		synced=true;
		time_zero=epoch_micros_at_time_zero;
	}
	
	public abstract InetSocketAddress TimeSyncAddress();

	public long timeZeroMicros()
	{
		return time_zero;
	}

	public abstract boolean SynchronizationNeeded();
}
