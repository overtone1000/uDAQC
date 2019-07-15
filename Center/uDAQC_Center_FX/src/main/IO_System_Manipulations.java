package main;

import java.util.Iterator;

import gndm.io.IO_Constants;
import gndm.io.IO_Group;
import gndm.io.IO_Node;
import javafx.scene.control.TreeItem;

public class IO_System_Manipulations
{
	public static TreeItem<IO_Node> Build_Tree(IO_Group parent)
	{
		TreeItem<IO_Node> retval = new TreeItem<IO_Node>(parent);
		retval.setExpanded(true);
		Iterator<IO_Node> it = parent.GetMembers().iterator();
		while (it.hasNext())
		{
			IO_Node next_node = it.next();
			TreeItem<IO_Node> next_branch;

			switch (next_node.IO_Type())
			{
			case IO_Constants.Command_IDs.group_description:
				next_branch = Build_Tree((IO_Group) next_node);
				break;
			default:
				next_branch = new TreeItem<IO_Node>(next_node);
				break;
			}

			retval.getChildren().add(next_branch);
		}
		return retval;
	}
}
