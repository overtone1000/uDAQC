package gndm.network.center.command;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.mina.core.buffer.IoBuffer;

public class Command {
					
	protected CommandHeader header=new CommandHeader();
	protected byte[] message;
	
	public Command()
	{
	}
	
	public Command(short command_id){
		this.header.command_id=command_id;
		this.message=new byte[0];
		this.header.message_length=this.message.length;
	}
	
	public Command(short command_id, byte message){
		this.header.command_id=command_id;
		byte[] messagebytes=new byte[1];
		messagebytes[0]=message;
		this.message=messagebytes;
		this.header.message_length=this.message.length;
	}
	
	public Command(short command_id, byte[] message){
		this.header.command_id=command_id;
		this.message=message;
		this.header.message_length=this.message.length;
	}
	
	public Command(CommandHeader h, byte[] message)
	{
		this.header=h;
		this.message=message;
	}
	
	public CommandHeader Header() {return this.header;}

	public ByteBuffer getmessage()
	{
		ByteBuffer retval = ByteBuffer.wrap(message);
		retval.order(ByteOrder.LITTLE_ENDIAN);
		return retval;
	}
	
	public String getString(){return new String(message);}
	
	public IoBuffer toBuffer()
	{
		IoBuffer c=IoBuffer.allocate(CommandHeader.header_length + message.length);
		c.order(ByteOrder.LITTLE_ENDIAN);
		c.putInt(header.message_length);
		c.putShort(header.command_id);
		c.put(message);
		c.flip();
		return c;
	}
	
	public static Command tryDecode(IoBuffer in)
	{
		int start=in.position();
		
		in.order(ByteOrder.LITTLE_ENDIAN);
		
		if(in.remaining()<CommandHeader.header_length) 
		{
			//System.out.println("Waiting for header, only " + in.remaining() + " currently available.");
			in.position(start);
			return null;
		}
		
		CommandHeader header=new CommandHeader();
		header.message_length=in.getInt();
		header.command_id=in.getShort();
						
		if(in.remaining()>=header.message_length) //message length is now strictly the length of the message, not the message plus the command header
		{
            byte[] message=new byte[header.message_length];
            in.get(message);            
            Command result = new Command(header,message);
            
            return result;
		}
		else
		{
			//System.out.println("Waiting for command " + header.command_id + " of length " + (Integer)(header.message_length));
			in.position(start);
			return null;
		}
	}
}

