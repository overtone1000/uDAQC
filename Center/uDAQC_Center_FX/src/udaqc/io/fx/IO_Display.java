package udaqc.io.fx;

import gndm.io.IO_Node;
import gndm.io.log.IO_System_Logged;

public interface IO_Display
{
	public void Set_IO(IO_Node selected_item, IO_System_Logged system);
	public void Update_IO();
}