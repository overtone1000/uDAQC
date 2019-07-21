package network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.mina.core.buffer.IoBuffer;
import org.joda.time.DateTime;

import network.Constants;
import udaqc.io.IO_Constants;
import udaqc.network.Constants.Addresses;
import udaqc.network.center.command.Command;

public class UDP_TimeSync implements Runnable
{
	private DatagramSocket socket=null;
	protected Thread t;
	protected boolean continuerunning = false;
	
	public UDP_TimeSync()
	{
		try
		{
			socket = new DatagramSocket();
			socket.setSoTimeout(5000);
			start();
		} catch (SocketException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Integer port()
	{
		return socket.getLocalPort();
	}
	
	public void send(Command c, InetSocketAddress add)
	{
		//DatagramSocket socket = new DatagramSocket();
        byte[] buf = c.toBuffer().array();
        
        DatagramPacket packet = new DatagramPacket(buf, buf.length, add);
        try
		{
			socket.send(packet);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
                
        System.out.println("UDP of length " + buf.length + " sent.");
	}
    
    protected void start(){
		if(t==null || !t.isAlive() || t.isInterrupted()){
			t=new Thread(this,"UDP TimeSync");
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

	@Override
	public void run()
	{
		byte[] buf = new byte[Constants.UDP_MaxPacketSize];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while(continuerunning)
		{

			try
			{
				socket.receive(packet);
				ByteBuffer bb = ByteBuffer.wrap(packet.getData());
				Command c = Command.tryDecode(bb);
				
				switch(c.Header().command_id)
				{
					case IO_Constants.Command_IDs.timesync_request:
					{
						System.out.println("Timesync command received.");
						InetSocketAddress add = new InetSocketAddress(packet.getAddress(), packet.getPort());
						
						Long source_time = c.getmessage().getLong();
						Long current_time = java.time.Clock.systemDefaultZone().millis();
						
						Command response;
						ByteBuffer response_message = ByteBuffer.allocate(Long.BYTES*2);
						response_message.order(ByteOrder.LITTLE_ENDIAN);
						response_message.putLong(source_time);
						response_message.putLong(current_time);
						
						response = new Command(IO_Constants.Command_IDs.timesync_response,response_message.array());
						
						send(response,add);
					}
				}
			}
			
			catch(SocketTimeoutException timeout)
			{
				Thread.yield();
			}
			
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
