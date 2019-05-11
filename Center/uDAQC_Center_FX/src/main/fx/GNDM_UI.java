package main.fx;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import gndm.io.IO_Reporter;
import gndm.io.log.IO_System_Logged;
import gndm.io.log.IO_System_Logged.Regime;
import gndm.network.center.GNDM_Handler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import udaqc.io.fx.IO_Chart;
import udaqc.io.fx.IO_Live;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public abstract class GNDM_UI extends Application implements GNDM_Handler
{
	protected Stage stage;
	protected BorderPane root;
	protected MenuBar mb;
	
	protected IO_Chart trm_chart;
	protected IO_Live trm_live;
	
	protected Text statustext = new Text();
	
	protected IO_System_Logged active_device=null;

	protected double height = 800;
	protected double width = 1200;
	
	protected Path systems_path = Paths.get("history");

	@Override
	public void start(Stage primaryStage) throws Exception
	{
		root = new BorderPane();

		Scene scene = new Scene(root, width, height, Color.WHITE);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

		this.stage = primaryStage;
		primaryStage.setScene(scene);
		
		root.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));

		trm_chart = new IO_Chart();
		trm_live = new IO_Live();

		setupmenubar();

		root.setTop(mb);
		root.setLeft(treeview);
		root.setCenter(trm_chart);
		root.setBottom(statustext);

		// resize();
		treeview.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<IO_Reporter>>()
		{
			@Override
			public void changed(ObservableValue<? extends TreeItem<IO_Reporter>> observable, TreeItem<IO_Reporter> old_value, TreeItem<IO_Reporter> new_value)
			{
				setchartareas();
			}
		});

		primaryStage.show();
	}

	private void setchartareas()
	{
		try
		{
			IO_Reporter selected = treeview.getSelectionModel().getSelectedItem().getValue();
			trm_chart.Set_IO(selected,active_device);
			trm_live.Set_IO(selected,active_device);
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		trm_chart.Update_IO();
		trm_live.Update_IO();
	}

	public Stage getStage()
	{
		return stage;
	}

	private TreeView<IO_Reporter> treeview = new TreeView<IO_Reporter>();

	public void DeviceSwitched(IO_System_Logged device)
	{
		active_device = device;
		TreeItem<IO_Reporter> root = IO_System_Manipulations.Build_Tree(active_device);
		treeview.setRoot(root);
		treeview.getSelectionModel().select(root);
		
	}

	protected Menu devicemenu;
	protected ToggleGroup devicetogglegroup = new ToggleGroup();

	@Override
	public void ClientListUpdate()
	{
		// Device
		LinkedList<IO_System_Logged> devices = IO_System_Logged.Systems();
		Iterator<IO_System_Logged> it = devices.iterator();

		devicemenu.getItems().clear();
		
		RadioMenuItem selected_device = (RadioMenuItem) devicetogglegroup.getSelectedToggle();

		while (it.hasNext())
		{
			final IO_System_Logged device = it.next();

			RadioMenuItem ni = new RadioMenuItem(device.Name());

			ni.setOnAction(new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(ActionEvent event)
				{
					// Select device n
					System.out.println("Device " + device.FullName() + " selected.");
					
					DeviceSwitched(device); 
					//need to search through clients and look for a match
					//if no match, need to set to null and handle this
				}
			});

			ni.setToggleGroup(devicetogglegroup);
			devicemenu.getItems().add(ni);
		}

		// By default, this selects the first toggle if one isn't already selected
		if (selected_device == null && devicemenu.getItems().size() > 0)
		{
			Platform.runLater(new Runnable()
			{
				@Override
				public void run()
				{
					devicemenu.getItems().get(0).fire();
				}
			});
		}
	}
	
	private Semaphore data_mutex = new Semaphore(1);
	
	@Override 
	public void ReceivingDataUpdate()
	{
		try
		{
			data_mutex.acquire();
			got_mutex=true;
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean got_mutex=false;

	@Override
	public void ReceivedDataUpdate()
	{
		if(got_mutex)
		{
			trm_chart.Update_IO();
			trm_live.Update_IO();
			data_mutex.release();
			got_mutex=false;
		}
	}

	private void setupmenubar()
	{

		mb = new MenuBar();

		devicemenu = new Menu("Device");
		mb.getMenus().add(devicemenu); // Only do this once on setup

		// Time
		Menu m1 = new Menu("Interface");

		ToggleGroup interfacetogglegroup = new ToggleGroup();
		
		RadioMenuItem m1_0 = new RadioMenuItem("Live");
		m1_0.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				root.setCenter(trm_live);
			}
		});

		Menu m1_1 = new Menu("Chart");
		
		for(Regime r:Regime.values())
		{
			RadioMenuItem i = new RadioMenuItem(r.toString());
			i.setToggleGroup(interfacetogglegroup);
			m1_1.getItems().add(i);
			i.setOnAction(new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(ActionEvent event)
				{
					i.setSelected(true);
					trm_chart.Set_Regime(r);
					trm_chart.Update_IO();
					root.setCenter(trm_chart);
				}
			});
		}

		m1_0.setToggleGroup(interfacetogglegroup);
		
		m1.getItems().addAll(m1_0,m1_1);
		mb.getMenus().addAll(m1);
		
		//Select the chart interface to start.
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				m1_1.getItems().get(0).fire(); //Set default to live chart
			}
		});
	}
}
