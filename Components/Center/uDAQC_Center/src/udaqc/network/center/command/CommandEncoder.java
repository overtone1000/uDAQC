package udaqc.network.center.command;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class CommandEncoder implements ProtocolEncoder{	
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {		
		Command request = (Command) message;
        out.write(request.toBuffer());
    }

    public void dispose(IoSession session) throws Exception {
        // nothing to dispose
    }
}
