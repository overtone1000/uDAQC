package udaqc.network.center.command;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class CommandCodecFactory implements ProtocolCodecFactory{
	private ProtocolEncoder encoder;
    private ProtocolDecoder decoder;

    public CommandCodecFactory(boolean client) {
    	encoder = new CommandEncoder();
        decoder = new CommandDecoder();
    }

    public ProtocolEncoder getEncoder(IoSession ioSession) throws Exception {
        return encoder;
    }

    public ProtocolDecoder getDecoder(IoSession ioSession) throws Exception {
        return decoder;
    }
}
