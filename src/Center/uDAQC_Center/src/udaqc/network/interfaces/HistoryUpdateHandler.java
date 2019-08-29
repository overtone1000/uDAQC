package udaqc.network.interfaces;

import java.nio.ByteBuffer;

import udaqc.io.log.IO_System_Logged.Regime;
import udaqc.network.center.DirectDevice;

public interface HistoryUpdateHandler
{
	public void HistoryUpdated(DirectDevice device, short system_index, Regime r, Long first_timestamp, ByteBuffer bb);
}
