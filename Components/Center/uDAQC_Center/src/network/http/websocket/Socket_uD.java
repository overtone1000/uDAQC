package network.http.websocket;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import network.http.HTTPS_Server;
import udaqc.network.center.command.Command;

public class Socket_uD extends WebSocketAdapter
{
	private HTTPS_Server parent;
	
	public Socket_uD(HTTPS_Server parent)
	{
		this.parent=parent;
	}
	
	private Session session=null;
	
    @Override
    public void onWebSocketConnect(Session sess)
    {
    	this.session=sess;
        super.onWebSocketConnect(sess);
        System.out.println("Socket Connected: " + sess);
        parent.SessionOpened(sess);
    }
    
    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
    	this.session=null;
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
    	//System.out.println("Received binary data. " + ((Integer)payload.length).toString() + " bytes.");
    	//System.out.println(payload);
    	ByteBuffer bb = ByteBuffer.wrap(payload);
    	//System.out.println(bb);
    	Command c = Command.tryDecode(bb);
    	if(c==null)
    	{
    		return;
    	}
    	parent.HandleCommand(c,session);
    }
    
    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        
        if(cause instanceof TimeoutException)
        {
        	System.out.println("Websocket connection timed out. Closing.");
        }
        else
        {
        	cause.printStackTrace(System.err);
        }
    }
}
