package network.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import network.http.HTTPS_Server;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.command.PT_Command;

public class Socket_uD extends WebSocketAdapter
{
	private HTTPS_Server parent;
	
	public Socket_uD(HTTPS_Server parent)
	{
		this.parent=parent;
	}
	
    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        System.out.println("Socket Connected: " + sess);
        parent.SessionOpened(sess);
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
        parent.SessionClosed(this.getSession());
    }
    
    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        System.out.println("Received TEXT message: " + message);
    }
    
    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len)
    {
    	super.onWebSocketBinary(payload, offset, len);
    	System.out.println("Received binary data. " + ((Integer)payload.length).toString() + " bytes.");
    }
    
    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }
}
