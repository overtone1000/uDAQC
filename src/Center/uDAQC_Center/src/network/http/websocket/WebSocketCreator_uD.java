package network.http.websocket;

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import network.http.HTTPS_Server;

public class WebSocketCreator_uD implements WebSocketCreator
{
	Socket_uD socket;
    public WebSocketCreator_uD(HTTPS_Server parent)
    {
    	socket = new Socket_uD(parent);
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
    {
    	return socket;
    }
}
