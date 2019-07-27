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
import java.util.concurrent.ConcurrentLinkedDeque;

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
	
	private ConcurrentLinkedDeque<TimeSynchronizable> syncs_to_perform = new ConcurrentLinkedDeque<TimeSynchronizable>();
	TimeSynchronizable current_sync = null;
	
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
	
	public void synchronize(TimeSynchronizable target)
	{
		syncs_to_perform.add(target);
	}
	
	@Override
	public void run()
	{
		byte[] buf = new byte[Constants.UDP_MaxPacketSize];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while(continuerunning)
		{
			System.out.println("UDP Loop.");
			try
			{
				socket.receive(packet);
				long receipt_time=java.time.Clock.systemDefaultZone().millis()*1000;
				
				ByteBuffer bb = ByteBuffer.wrap(packet.getData());
				Command c = Command.tryDecode(bb);
				
				switch(c.Header().command_id)
				{
					case IO_Constants.Command_IDs.timesync_response:
					{
						System.out.println("Timesync response received.");
						InetSocketAddress add = new InetSocketAddress(packet.getAddress(), packet.getPort());
						
						ByteBuffer message = c.getmessage();
						
						long request_time = message.getLong();
						long device_time = message.getLong();
						
						System.out.println(request_time + ", request time");
						System.out.println(device_time + ", device time");
						System.out.println(receipt_time + ", response time");
						
					}
				}
			}
			
			catch(SocketTimeoutException timeout)
			{
				System.out.println("Timeout.");
				Thread.yield();
			}
			
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			System.out.println(current_sync);
			if(current_sync==null)
			{
				System.out.println("Trying dequeue.");
				current_sync=syncs_to_perform.pollFirst();
			}
			else
			{
				System.out.println("Trying synchronization.");
				Long current_time = java.time.Clock.systemDefaultZone().millis()*1000;
				
				Command request;
				ByteBuffer response_message = ByteBuffer.allocate(Long.BYTES*1);
				response_message.order(ByteOrder.LITTLE_ENDIAN);
				response_message.putLong(current_time);
				
				request = new Command(IO_Constants.Command_IDs.timesync_request,response_message.array());
				
				InetSocketAddress add = current_sync.Address();
				if(add!=null)
				{
					System.out.println("Sending timesync request to " + add.toString());
					send(request,add);
				}
				else
				{
					System.out.println("Null address for TimeSync.");
					current_sync = null;
					syncs_to_perform.removeFirst();
				}
			}
		}
	}
}
