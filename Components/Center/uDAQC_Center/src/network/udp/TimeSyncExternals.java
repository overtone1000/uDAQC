package network.udp;

import java.net.InetSocketAddress;

public interface TimeSyncExternals
{
	public InetSocketAddress TimeSyncAddress();
	public boolean KeepSynchronized();
}
