package main;

import java.util.Iterator;

import gndm.io.IO_Constants;
import gndm.io.IO_Group;
import gndm.io.IO_Reporter;
import javafx.scene.control.TreeItem;

public class IO_System_Manipulations
{
	public static TreeItem<IO_Reporter> Build_Tree(IO_Group parent)
	{
		TreeItem<IO_Reporter> retval = new TreeItem<IO_Reporter>(parent);
		retval.setExpanded(true);
		Iterator<IO_Reporter> it = parent.GetMembers().iterator();
		while (it.hasNext())
		{
			IO_Reporter next_reporter = it.next();
			TreeItem<IO_Reporter> next_branch;

			switch (next_reporter.IO_Type())
			{
			case IO_Constants.Command_IDs.group_description:
				next_branch = Build_Tree((IO_Group) next_reporter);
				break;
			default:
				next_branch = new TreeItem<IO_Reporter>(next_reporter);
				break;
			}

			retval.getChildren().add(next_branch);
		}
		return retval;
	}
}
