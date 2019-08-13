package udaqc.network.interfaces;

import java.nio.ByteBuffer;

import udaqc.io.log.IO_System_Logged;
import udaqc.io.log.IO_System_Logged.Regime;

public interface HistoryUpdateHandler
{
	public void HistoryUpdated(IO_System_Logged system, Regime r, Long first_timestamp, ByteBuffer bb);
}
