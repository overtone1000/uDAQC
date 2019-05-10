package gndm.io.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import fx.chart.Chart;
import fx.chart.ChartStyle;
import fx.chart.areas.ChartArea;
import fx.chart.children.CurveSet;
import gndm.io.IO_Constants;
import gndm.io.IO_Group;
import gndm.io.IO_Reporter;
import gndm.io.IO_Value;
import gndm.io.log.IO_System_Logged;
import gndm.io.log.IO_System_Logged.Regime;

import javafx.scene.paint.Color;
import trm.logging.Point;

public class IO_Chart extends Chart implements IO_Display
{
	protected IO_System_Logged system;
	protected Regime current_regime = Regime.Live;

	private static class CSVal_Pair
	{
		CurveSet curveset;
		IO_Value value;
	}

	Semaphore pair_mutex = new Semaphore(1);
	private java.util.LinkedList<CSVal_Pair> pairs = new java.util.LinkedList<CSVal_Pair>();

	@Override
	public void clearAreas()
	{
		try
		{
			pair_mutex.acquire();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		pairs.clear();
		super.clearAreas();
		pair_mutex.release();
	}

	@Override
	public void Set_IO(IO_Reporter selected_item, IO_System_Logged system)
	{
		this.clearAreas();
		this.system = system;

		try
		{
			pair_mutex.acquire();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		switch (selected_item.IO_Type())
		{
		case IO_Constants.Command_IDs.group_description:
			Build_Chart((IO_Group) selected_item);
			break;
		case IO_Constants.Command_IDs.value_description:
		case IO_Constants.Command_IDs.modifiablevalue_description:
			this.clearAreas();
			LinkedList<IO_Value> solo = new LinkedList<IO_Value>();
			solo.add((IO_Value) selected_item);
			Add_ChartArea(selected_item.Name(), solo);
			break;
		default:
			break;
		}

		pair_mutex.release();
	}

	public void Set_Regime(Regime reg)
	{
		this.current_regime = reg;
	}

	@Override
	public void Update_IO()
	{
		try
		{
			pair_mutex.acquire();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		for (CSVal_Pair pair : pairs)
		{
			LinkedList<ArrayList<Point>> point_queue = system.getPoints(current_regime, pair.value);

			if (point_queue == null)
			{
				System.out.println("Point queue was null for " + pair.value.FullName());
				return;
			}

			pair.curveset.setpoints(point_queue);
		}

		pair_mutex.release();

		this.draw();
	}

	private void Build_Chart(IO_Group parent)
	{
		LinkedList<IO_Group> sub_groups = new LinkedList<IO_Group>();

		// this will group IO_Values based on their units so they can be put into the
		// same ChartArea
		TreeMap<String, LinkedList<IO_Value>> sub_values = new TreeMap<String, LinkedList<IO_Value>>();

		// Sort members into the above groups
		Iterator<IO_Reporter> it = parent.GetMembers().iterator();
		while (it.hasNext())
		{
			IO_Reporter child = it.next();
			switch (child.IO_Type())
			{
			case IO_Constants.Command_IDs.group_description:
				sub_groups.add((IO_Group) child);
				break;
			case IO_Constants.Command_IDs.value_description:
			case IO_Constants.Command_IDs.modifiablevalue_description:
				IO_Value value = (IO_Value) child;
				if (!sub_values.containsKey(value.Units()))
				{
					sub_values.put(value.Units(), new LinkedList<IO_Value>());
				}
				sub_values.get(value.Units()).add(value);
				break;
			default:
				// Do nothing for other types.
				break;
			}
		}

		// Build chart areas from this group's direct IO_Values
		Iterator<LinkedList<IO_Value>> cag_it = sub_values.values().iterator();
		while (cag_it.hasNext())
		{
			LinkedList<IO_Value> cag = cag_it.next();
			Add_ChartArea(parent.Name(), cag);
		}

		// Then recursively build the rest of the chart from child IO_Groups
		Iterator<IO_Group> g_it = sub_groups.iterator();
		while (g_it.hasNext())
		{
			IO_Group g = g_it.next();
			Build_Chart(g);
		}
	}

	private void Add_ChartArea(String area_name, LinkedList<IO_Value> values)
	{
		String name = area_name;

		ChartStyle style = ChartStyle.LINE;
		ChartArea area = new ChartArea(name, this, style);

		Iterator<IO_Value> member_it = values.iterator();
		int col_int = 0;
		while (member_it.hasNext())
		{
			IO_Value member = member_it.next();
			Color c = colors[col_int];
			col_int++;
			if (col_int >= colors.length)
			{
				col_int = 0;
			}
			Add_CurveSet(member, area, c);
		}

		this.addArea(area);
	}

	private void Add_CurveSet(IO_Value value, ChartArea area, Color c)
	{
		CurveSet cs = new CurveSet(area);
		cs.setcolor(c);
		area.addCurveSet(value.Name(), value, cs);
		this.Add_IO_Value(cs, value);
	}

	private static final Color[] colors = { Color.RED, Color.GREEN, Color.ORANGE, Color.BLUE, Color.PURPLE, Color.BROWN, Color.DARKRED, Color.DARKGREEN, Color.DARKORANGE, Color.DARKBLUE };

	public void Add_IO_Value(CurveSet cs, IO_Value val)
	{
		if (cs == null || val == null)
		{
			return;
		}
		CSVal_Pair new_pair = new CSVal_Pair();
		new_pair.curveset = cs;
		new_pair.value = val;
		pairs.add(new_pair);
	}
}
