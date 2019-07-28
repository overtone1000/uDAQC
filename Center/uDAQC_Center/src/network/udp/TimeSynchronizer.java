package network.udp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import udaqc.network.Constants;

public abstract class TimeSynchronizer
{
	protected boolean synced=false;
	protected long time_zero=0;
	protected long last_sync_millis=0;
	protected static final long resync_interval=1000*60*60*6; //Every 6 hours
	
	public TimeSynchronizer()
	{
		
	}
	
	public boolean Synced()
	{
		return synced;
	}
	
	public void Synchronize(long epoch_micros_at_time_zero)
	{
		if(synced)
		{
			System.out.println("Drift since last sync was " + (time_zero-epoch_micros_at_time_zero) + " us.");
		}
		synced=true;
		time_zero=epoch_micros_at_time_zero;
		last_sync_millis=java.time.Clock.systemDefaultZone().millis();
	}
	
	public abstract InetSocketAddress TimeSyncAddress();

	public long timeZeroMicros()
	{
		return time_zero;
	}

	public boolean SynchronizeNow()
	{
		return !synced || (java.time.Clock.systemDefaultZone().millis()-last_sync_millis)>=resync_interval;
	}
	
	public abstract boolean KeepSynchronized();
}
