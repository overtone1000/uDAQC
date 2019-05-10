package main.fx;

import java.net.InetSocketAddress;
import java.nio.file.Paths;
import gndm.network.passthrough.Secondary_Center;
import gndm.network.passthrough.Secondary_Constants;
import javafx.stage.Stage;

public class GNDM_Passthrough_Client_UI extends GNDM_UI
{
	private Secondary_Center client;

	@Override
	public void start(Stage primaryStage)
	{
		try
		{
			primaryStage.setTitle("GNDM Secondary Client");
			super.start(primaryStage);

			InetSocketAddress host = new InetSocketAddress("127.0.0.1", Secondary_Constants.Ports.passthrough_server);
			client = new Secondary_Center("ID_Server", Paths.get("passthrough_history"), host, this);

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
		this.client.dispose();
		this.client = null;
	}
	
	public static void main(String[] args)
	{
		launch(args);
	}
}
