package network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import network.Constants;
import udaqc.io.IO_Constants;
import udaqc.network.center.command.Command;

public class UDP_TimeSync implements Runnable
{
	private DatagramSocket socket=null;
	protected Thread t;
	protected boolean continuerunning = false;
	
	private static final int points_to_full = 20;
	private static final long sync_timeout_ms = 10000; //10 second timeout
	
	private ConcurrentLinkedDeque<TimeSynchronizer> syncs_to_perform = new ConcurrentLinkedDeque<TimeSynchronizer>();
		
	TimeSynchronizer current_sync = null;
	private long last_sync_message=0;
	
	private class sync_point
	{
		public long interval;
		public long micros;
	};
	
	private ArrayList<sync_point> points = new ArrayList<sync_point>();
	
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
	
	public void addSynchronizer(TimeSynchronizer target)
	{
		if(!syncs_to_perform.contains(target))
		{
			syncs_to_perform.add(target);
		}
	}
	
	public void finishCurrent()
	{
		if(!points.isEmpty())
		{
			double mean=0;
			double std_dev=0;
			
			for(sync_point p:points)
			{
				mean+=p.interval;
			}
			
			mean=mean/points.size();
			
			for(sync_point p:points)
			{
				std_dev+=Math.pow(p.interval-mean,2);
			}
			std_dev=std_dev/(points.size()-1);
			std_dev=Math.sqrt(std_dev);
			
			System.out.println("Sync point mean interval = " + mean + ", std_dev = " + std_dev);
			
			Iterator<sync_point> i = points.iterator();
			int removed=0;
			while(i.hasNext())
			{
				sync_point p = i.next();
				if(p.interval>(mean+std_dev))
				{
					i.remove();
					removed++;
				}
			}
			
			System.out.println(removed + " sync points removed for excessive interval.");
			
			long mean_micros=0;
			
			for(sync_point p:points)
			{
				mean_micros+=p.micros;
			}
			mean_micros=mean_micros/points.size();
			
			System.out.println("Mean micros is " + mean_micros);
			
			current_sync.Synchronize(mean_micros);
		}
		else
		{
			System.out.println("Insufficient sync data for " + current_sync);
		}
		
		points.clear();

		syncs_to_perform.remove(current_sync);
		if(current_sync.getExternal().KeepSynchronized())
		{
			System.out.println(current_sync + " requests continued synchronization. Moving to end of list.");
			syncs_to_perform.add(current_sync);
		};
		current_sync=null;
	}
	
	private void pollCurrent()
	{
		//System.out.println("Trying synchronization.");
		Long current_time = java.time.Clock.systemDefaultZone().millis()*1000;
		
		Command request;
		ByteBuffer response_message = ByteBuffer.allocate(Long.BYTES*1);
		response_message.order(ByteOrder.LITTLE_ENDIAN);
		response_message.putLong(current_time);
		
		request = new Command(IO_Constants.Command_IDs.timesync_request,response_message.array());
		
		InetSocketAddress add = current_sync.getExternal().TimeSyncAddress();
		if(add!=null)
		{
			//System.out.println("Sending timesync request to " + add.toString());
			send(request,add);
		}
		else
		{
			System.out.println("Null address for TimeSync. Removing.");
			syncs_to_perform.remove(current_sync);
			current_sync = null;
		}
	}
	
	private void handleResponse(Command c)
	{
		if(current_sync==null)
		{
			return;
		}
		
		last_sync_message=java.time.Clock.systemDefaultZone().millis();
		long receipt_time=last_sync_message*1000;
				
		//System.out.println("Timesync response received.");
		
		ByteBuffer message = c.getmessage();
		
		long request_time = message.getLong();
		long device_time = message.getLong();
		
		//System.out.println(request_time + ", request time");
		//System.out.println(device_time + ", device time");
		//System.out.println(receipt_time + ", response time");
		
		sync_point newpoint = new sync_point();
		newpoint.interval=receipt_time-request_time;
		newpoint.micros=request_time+newpoint.interval/2-device_time;
		points.add(newpoint);
		
		if(points.size()>points_to_full)
		{
			finishCurrent();
		}
	}
	
	@Override
	public void run()
	{
		byte[] buf = new byte[Constants.UDP_MaxPacketSize];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		while(continuerunning)
		{
			if(current_sync==null)
			{
				//System.out.println("There are " + syncs_to_perform.size() + " synchronizations being managed by " + this);
				Iterator<TimeSynchronizer> i = syncs_to_perform.iterator();
				TimeSynchronizer next = null;
				while(i.hasNext())
				{
					next=i.next();
					if(next.SynchronizeNow())
					{
						current_sync = next;
						last_sync_message=java.time.Clock.systemDefaultZone().millis();
						break;
					}
				}
			}
			
			if(current_sync!=null)
			{
				long current = java.time.Clock.systemDefaultZone().millis();
				if((current-last_sync_message)>sync_timeout_ms || points.size()>=points_to_full)
				{
					finishCurrent();
				}
				else
				{
					pollCurrent();
				}
			}
			
			try
			{
				socket.receive(packet);
				
				ByteBuffer bb = ByteBuffer.wrap(packet.getData());
				Command c = Command.tryDecode(bb);
				
				switch(c.Header().command_id)
				{
					case IO_Constants.Command_IDs.timesync_response:
					{
						InetAddress expected = current_sync.getExternal().TimeSyncAddress().getAddress();
						if(packet.getAddress().equals(expected)) //only handle the response if this packet is from the expected source. Don't want to mix up time syncs. This is also a security issue.
						{
							handleResponse(c);
						}
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
