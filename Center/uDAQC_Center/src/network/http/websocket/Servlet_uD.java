package network.http.websocket;

import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class Servlet_uD extends WebSocketServlet
{
	private static final long serialVersionUID = 1L;

	@Override
	public void configure(WebSocketServletFactory factory)
	{
		System.out.println("Configuring WebSocketHandler.");
		factory.register(Socket_uD.class);
	}
}