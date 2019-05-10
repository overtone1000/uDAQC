package gndm.network.passthrough.command;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.mina.core.buffer.IoBuffer;

import gndm.io.IO_Constants;
import gndm.network.center.command.Command;
import gndm.network.center.command.CommandHeader;

public class PT_Command extends Command
{
	/*
	 * This class piggybacks on the existing command functions but adds a
	 * command id and source id to the front.
	 * This allows a server to send commands almost directly through
	 * to additional clients.
	 */
	
	public static final int extra_length = Short.BYTES*2; //just one short for source_id and another for the contained command id
			
	public short source_id=-1; //If <0, error!
	
	public PT_Command(short source, Command contained_command)
	{
		super(contained_command.Header(),contained_command.getmessage().array());
		source_id=source;
	}
	
	public Command containedCommand()
	{
		return new Command(this.Header().command_id,this.getmessage().array());
	}
		
	public PT_Command(Command passthrough_command)
	{
		//Use this to convert a raw passthrough command into this inherited object
		super();
		
		//The passthrough command first contains a normal header.
		//The command id for this header is passthrough, so ignore it.
		
		ByteBuffer c = passthrough_command.getmessage();
		source_id = c.getShort(); //first in the message is the source id
		header.command_id = c.getShort(); //next is the command id
				
		//Message length is whatever remains in the buffer
		header.message_length = c.remaining();
		
		//And get the rest of the message
		message = new byte[header.message_length];
		c.get(message);
	}
	
	@Override
	public IoBuffer toBuffer()
	{
		IoBuffer c=IoBuffer.allocate(extra_length + CommandHeader.header_length + message.length);
		c.order(ByteOrder.LITTLE_ENDIAN);
		
		c.putInt(header.message_length+extra_length); //modified length
		c.putShort(IO_Constants.Command_IDs.passthrough);
		
		c.putShort(source_id);
		c.putShort(header.command_id);
		
		c.put(message);
		c.flip();
		return c;
	}
}
