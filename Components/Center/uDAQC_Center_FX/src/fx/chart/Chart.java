package fx.chart;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import fx.chart.areas.ChartArea;
import fx.chart.areas.XArea;
import trm.utilities.TRM_Math;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class Chart extends Pane
{
	public static final String newLine = System.getProperty("line.separator").toString();
	public static org.joda.time.format.DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy" + newLine + "HH:mm:ss");

	public double scene_left;
	public double scene_right;
	public double scene_top;
	public double scene_bottom;

	private double chart_left_margin;
	private double chart_right_margin;
	private double chart_top_margin;
	private double chart_bottom_margin;

	public DateTime x_min = DateTime.now();
	public DateTime x_max = DateTime.now().minusMinutes(60);

	public double chart_left()
	{
		return scene_left + chart_left_margin;
	}

	public double chart_right()
	{
		return scene_right - chart_right_margin;
	}

	public double chart_top()
	{
		return scene_top + chart_top_margin;
	}

	public double chart_bottom()
	{
		return scene_bottom - chart_bottom_margin;
	}

	private XArea x_area;
	protected java.util.ArrayList<ChartArea> chartareas = new java.util.ArrayList<ChartArea>();

	public Chart()
	{
		x_area = new XArea(this);
		chart_left_margin = 80;
		chart_right_margin = 200;
		chart_top_margin = 40;
		chart_bottom_margin = 80;

		this.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));

		this.widthProperty().addListener(new widthlistener());
		this.heightProperty().addListener(new heightlistener());

		mouse_handler mh = new mouse_handler();
		this.setOnMouseMoved(mh);
		this.setOnMouseDragged(mh);
		this.setOnMousePressed(mh);
		this.setOnMouseReleased(mh);
	}

	private boolean zooming_mode_is_X = true;
	private DateTime[] zoomX=new DateTime[2];
	private double[] zoomXpos=new double[2];
	private boolean zoomedX = false;
	private boolean zoomingX = false;
	private double[] zoomY=new double[2];
	private double[] zoomYpos=new double[2];
	private boolean zoomedY = false;
	private boolean zoomingY = false;

	private class mouse_handler implements EventHandler<MouseEvent>
	{
		@Override
		public void handle(MouseEvent event)
		{
			if (event.getButton().equals(MouseButton.SECONDARY) && event.isSecondaryButtonDown())
			{
				zooming_mode_is_X = !zooming_mode_is_X;
				if (zooming_mode_is_X)
				{
					for (ChartArea ca : chartareas)
					{
						ca.hideYmarker();
					}
				} else
				{
					x_area.hideXmarker();
				}
				System.out.println("Mode switch. Now " + zooming_mode_is_X);
			}
			ChartArea ca_y = chartareafromY(event.getY());
			for (ChartArea ca : chartareas)
			{
				if (ca != ca_y)
				{
					ca.hideYmarker();
				}
			}
			setmarkerbar(event, ca_y);
			if (event.getButton().equals(MouseButton.PRIMARY) && event.isPrimaryButtonDown())
			{
				if (zooming_mode_is_X)
				{
					if (!zoomingX)
					{
						zoomingX = true;
						System.out.println("Starting zoom.");
						zoomXpos[0] = event.getX();
						zoomX[0] = x_to_datetime(zoomXpos[0]);
						if (!zoomedX)
						{
							x_area.setzoom(zoomXpos[0], zoomX[0], true);
						}
					}
				} else
				{
					if (!zoomingY)
					{
						zoomingY = true;
						System.out.println("Starting zoom.");
						zoomYpos[0] = event.getY();
						zoomY[0] = ca_y.y_to_value(zoomYpos[0]);
						if (!zoomedY)
						{
							ca_y.setYzoom(zoomYpos[0], true);
						}
					}
				}
			}
			if (event.getButton().equals(MouseButton.PRIMARY) && !event.isPrimaryButtonDown())
			{
				if (zooming_mode_is_X)
				{
					zoomingX = false;
					System.out.println("Stopping zoom.");
					zoomXpos[1] = event.getX();
					zoomX[1] = x_to_datetime(zoomXpos[1]);
					zoomedX = !zoomedX;
					if (zoomedX)
					{
						if (zoomXpos[0] > zoomXpos[1])
						{
							x_min = zoomX[1];
							x_max = zoomX[0];
						} else
						{
							x_min = zoomX[0];
							x_max = zoomX[1];
						}
					}
					x_area.setzoom(zoomXpos[1], zoomX[1], false);
				} else
				{
					zoomingY = false;
					System.out.println("Stopping zoom.");
					zoomYpos[1] = event.getY();
					zoomY[1] = ca_y.y_to_value(zoomYpos[1]);
					zoomedY = !zoomedY;
					ca_y.y_override = zoomedY;
					if (zoomedY)
					{
						if (zoomY[0] > zoomY[1])
						{
							ca_y.set_y_overridevalues(zoomY[1], zoomY[0]);
						} else
						{
							ca_y.set_y_overridevalues(zoomY[0], zoomY[1]);
						}
					}
					ca_y.setYzoom(zoomYpos[1], false);
				}
				draw();
			}
		}

		private void setmarkerbar(MouseEvent event, ChartArea ca_y)
		{
			if (zooming_mode_is_X)
			{
				DateTime dt = x_to_datetime(event.getX());
				boolean inchartarea = (event.getX() > chart_left() && event.getX() < chart_right() && event.getY() > chart_top() && event.getY() < chart_bottom());

				x_area.setXmarker(event.getX(), dt, inchartarea);
				for (ChartArea ca : chartareas)
				{
					if (inchartarea)
					{
						ca.setupXmarkerlabel(dt);
					} else
					{
						ca.setupXmarkerlabel();
					}
				}
			} else
			{
				if (ca_y != null)
				{
					ca_y.setupYmarkerlabel(event.getY());
				}
			}
		}
	}

	public DateTime x_to_datetime(double x)
	{
		return new DateTime(x_min.plus((long) ((x_max.getMillis() - x_min.getMillis()) * ((x - chart_left()) / (chart_right() - chart_left())))));
	}

	private class widthlistener implements ChangeListener<Number>
	{
		@Override
		public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneWidth, Number newSceneWidth)
		{
			scene_left = 0;
			scene_right = (Double) newSceneWidth;
			draw();
		}
	}

	private class heightlistener implements ChangeListener<Number>
	{
		@Override
		public void changed(ObservableValue<? extends Number> observableValue, Number oldSceneHeight, Number newSceneHeight)
		{
			scene_top = 0;
			scene_bottom = (Double) newSceneHeight;
			draw();
		}
	}

	public void addArea(ChartArea a)
	{
		drawinglock.lock();
		chartareas.add(a);
		drawinglock.unlock();
	}

	public void clearAreas()
	{
		drawinglock.lock();
		for (ChartArea c : chartareas)
		{
			c.removearea();
		}
		chartareas.clear();
		drawinglock.unlock();
	}

	public ChartArea getArea(String name)
	{
		for (ChartArea c : chartareas)
		{
			if (c.name().equals(name))
			{
				return c;
			}
		}
		return null;
	}

	public Set<String> getKeys()
	{
		HashSet<String> retval = new HashSet<String>();
		for (ChartArea c : chartareas)
		{
			retval.add(c.name());
		}
		return retval;
	}

	public static String prettylabel(double val)
	{
		/*
		 * String str=Double.toString(val); int end=0; if(str.contains(".")){
		 * if(str.indexOf(".")==1){end=str.indexOf('.')+4;}
		 * else{end=str.indexOf('.')+3;} if(str.length()<end){end=str.length();}
		 * str=str.substring(0, end); }
		 */
		return Double.toString(TRM_Math.roundToSignificantFigures(val, 4));
	}

	private DateTime min_min_X = new DateTime(0);
	private DateTime max_max_X = DateTime.now().plusYears(100);

	private void autorange_X()
	{
		if (zoomedX)
		{
			return;
		}
		DateTime new_x_min = DateTime.now().plusYears(100);
		DateTime new_x_max = new DateTime(0);
		for (ChartArea c : chartareas)
		{
			DateTime x;
			x = c.min_X();
			if (x.isBefore(new_x_min))
			{
				new_x_min = x;
			}
			x = c.max_X();
			if (x.isAfter(new_x_max))
			{
				new_x_max = x;
			}
		}
		if (min_min_X.isAfter(new_x_min))
		{
			new_x_min = min_min_X;
		}
		if (max_max_X.isBefore(new_x_max))
		{
			new_x_max = max_max_X;
		}
		x_min = new_x_min;
		x_max = new_x_max;
	}

	private void assignareaheights()
	{
		double newtop = chart_top();
		double area_height = (newtop-chart_bottom()-chartareas.size())/chartareas.size();
		for(ChartArea c:chartareas)
		{
			double newbottom=newtop-area_height;
			c.settop(newtop);
			c.setbottom(newbottom);
			newtop = newbottom;
		}
	}

	private ChartArea chartareafromY(double Y)
	{
		for (ChartArea ca : chartareas)
		{
			if (Y <= ca.bottom() && Y > ca.top())
			{
				return ca;
			}
		}
		return null;
	}

	boolean predrawing = false;
	boolean drawing = false;

	protected ReentrantLock drawinglock = new ReentrantLock();

	public void draw()
	{
		pdt.run();
		Platform.runLater(dt);
	}

	private draw_thread dt = new draw_thread();

	private class draw_thread implements Runnable
	{
		@Override
		public void run()
		{
			System.out.println("Drawing");
			drawinglock.lock();
			x_area.draw_axis_labels();
			for (ChartArea c : chartareas)
			{
				c.draw_curves();
			}
			for (ChartArea c : chartareas)
			{
				c.draw_peripherals();
			}
			drawinglock.unlock();
		}
	}

	private predraw_thread pdt = new predraw_thread();

	private class predraw_thread implements Runnable
	{
		@Override
		public void run()
		{
			System.out.println("Predrawing");
			drawinglock.lock();
			autorange_X();
			assignareaheights();
			for (ChartArea c : chartareas)
			{
				c.predraw();
			}
			drawinglock.unlock();
		}
	}
}
