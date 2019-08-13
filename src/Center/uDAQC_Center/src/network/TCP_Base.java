package network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

public abstract class TCP_Base extends IoHandlerAdapter implements Runnable
{
	protected Thread t;
	protected String threadName;
	protected boolean SSL=false;
	protected boolean continuerunning = false;
	protected boolean disposing=false;
	protected String TCP_Status = "not initialized";
	
	public void SetName(String Threadname)
	{
		this.threadName = Threadname;
		if(t != null)
		{
			t.setName(this.threadName);
		}
	}
	
	protected void start(){
		if(t==null || !t.isAlive() || t.isInterrupted()){
			t=new Thread(this,threadName);
			continuerunning=true;
			t.start();
		}
	}
	
	protected void restart(){
		if(t!=null){
			continuerunning=false;
			try {
				t.join(10000);
			} catch (InterruptedException e) {
				t.interrupt();
			}
			t=null;
		}
		this.start();
	}
	
	protected void stoprunning(){
		System.out.println("Stop running called successfully.");
		continuerunning=false;
	}
	
	public void dispose()
	{
		disposing=true;
		this.stoprunning();
	}
	
	protected static int GetPort(SocketAddress address)
	{
		try
		{
			return ((InetSocketAddress)address).getPort();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return 0;
	}
	
	protected static InetAddress GetAddress(IoSession session)
	{
		InetSocketAddress isa = (InetSocketAddress) session.getRemoteAddress();
		InetAddress ia = (InetAddress) isa.getAddress();
		
		return ia;
	}
}
