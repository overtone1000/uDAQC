package udaqc.network.center.command;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class CommandDecoder extends CumulativeProtocolDecoder {
	
	@Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception 
	{
		Command result = Command.tryDecode(in.buf());
		
		if(result==null)
		{
			return false;
		}
		else
		{
			out.write(result);
			return true;
		}
    }
}
