package network.http.websocket;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import network.http.HTTPS_Server;

public class Servlet_uD extends WebSocketServlet
{
	private static final long serialVersionUID = 1L;
	
	private HTTPS_Server parent;
	public Servlet_uD(HTTPS_Server parent)
	{
		this.parent=parent;
	}

	@Override
	public void configure(WebSocketServletFactory factory)
	{
		System.out.println("Configuring WebSocketHandler.");
		factory.setCreator(new WebSocketCreator_uD(parent));
	}
}