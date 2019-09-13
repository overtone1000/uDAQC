package main;

import gndm.network.center.GNDM_Center;
import javafx.stage.Stage;

public class GNDM_Center_UI extends GNDM_UI
{
	private GNDM_Center server;

	@Override
	public void start(Stage primaryStage)
	{
		try
		{
			primaryStage.setTitle("GNDM Primary Center");
			super.start(primaryStage);

			server = new GNDM_Center("ID_Server", systems_path, this);

			//Can use this function for situations where UDP broadcasting isn't working
			//server.AddStaticClient(new InetSocketAddress("192.168.1.23",IO_Constants.Constants.tcp_main_port));

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void stop()
	{
		this.server.dispose();
		this.server = null;
	}
	
	public static void main(String[] args)
	{
		launch(args);
	}
}
