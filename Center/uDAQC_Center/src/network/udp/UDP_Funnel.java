package network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.mina.core.buffer.IoBuffer;

import network.Constants;
import udaqc.io.IO_Constants;
import udaqc.network.Constants.Addresses;
import udaqc.network.center.command.Command;

public class UDP_Funnel implements Runnable
{
	private DatagramSocket socket=null;
	protected Thread t;
	protected boolean continuerunning = false;
	protected int destination_port;
	private StopWatch broadcast_sw = new StopWatch();
	private static final int repeat = 1000*60*10; //broadcast every 10 minutes

	public UDP_Funnel(int destination_port)
	{
		this.destination_port = destination_port;
		broadcast_sw.start();
		try
		{
			socket = new DatagramSocket(Addresses.udp_broadcast.getPort());
			socket.setSoTimeout(1000);
			start();
		} catch (SocketException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	public Command formCommand()
	{
		IoBuffer message = IoBuffer.allocate(Integer.BYTES);
		message.order(ByteOrder.LITTLE_ENDIAN);
		message.putInt(destination_port);
		return new Command(IO_Constants.Command_IDs.request_subscription, message.array());
	}
	
	public void Announce()
	{
		broadcast(formCommand());
	}
	
    public void broadcast(Command c)
    {
        send(c,Addresses.udp_broadcast);
    }
    
    protected void start(){
		if(t==null || !t.isAlive() || t.isInterrupted()){
			t=new Thread(this,"UDP Manager");
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
			if(broadcast_sw.getTime()>=repeat)
			{
				Announce();
				broadcast_sw.reset();
				broadcast_sw.start();
			}
			try
			{
				socket.receive(packet);
				ByteBuffer bb = ByteBuffer.wrap(packet.getData());
				Command c = Command.tryDecode(bb);
				
				switch(c.Header().command_id)
				{
					case IO_Constants.Command_IDs.new_device_available:
					{
						System.out.println("New device available command received.");
						InetSocketAddress add = new InetSocketAddress(packet.getAddress(), packet.getPort());
						send(formCommand(),add);
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
