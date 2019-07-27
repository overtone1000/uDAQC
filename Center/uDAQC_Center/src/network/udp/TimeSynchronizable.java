package network.udp;

import java.net.InetSocketAddress;

public interface TimeSynchronizable
{
	public void Synchronize(long epoch_nanos_at_time_zero);
	public InetSocketAddress Address();
}
