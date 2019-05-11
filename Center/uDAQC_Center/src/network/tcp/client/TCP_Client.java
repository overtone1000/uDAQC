package network.tcp.client;

import java.net.InetSocketAddress;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import network.TCP_Base;
import network.tcp.TCP_Commons;
import udaqc.network.center.command.Command;

public abstract class TCP_Client extends TCP_Base
{	
	private NioSocketConnector connector;
	private InetSocketAddress[] hosts;
	private int currenthostname=0;
	private IoSession session;

	public TCP_Client(String Threadname, InetSocketAddress host, boolean SSL_enabled, boolean start){
		this(Threadname, new InetSocketAddress[] {host}, SSL_enabled, start);
	}
	
	public TCP_Client(String Threadname, InetSocketAddress[] hosts, boolean SSL_enabled, boolean start){
		super();
		this.threadName=Threadname;
		this.hosts=hosts;
		this.SSL=SSL_enabled;
		if(start) {this.start();}
	}
	
	
	
	@Override
	public void run(){
		connector = new NioSocketConnector();
	    connector.setConnectTimeoutMillis(30000);
	    LoggingFilter logger=new LoggingFilter();
        logger.setMessageReceivedLogLevel(LogLevel.TRACE);
        logger.setMessageSentLogLevel(LogLevel.TRACE);
        
        connector.getFilterChain().addLast( "codec", new ProtocolCodecFilter(new udaqc.network.center.command.CommandCodecFactory(true)));
        connector.getFilterChain().addLast( "logger", logger);
        
        if(SSL)
        {
        	TCP_Commons.AddSSLFilter(connector);
        }
        
        
	    connector.setHandler(this);
	    
	    while (continuerunning) {
	    	if(disposing){break;}
	        try {
	        	InetSocketAddress host;
	        	host=hosts[currenthostname];
	        	currenthostname+=1;
	        	if(currenthostname>hosts.length-1){currenthostname=0;}
	            ConnectFuture future = connector.connect(host);
	            future.awaitUninterruptibly();
	            if(future.isConnected()){ 
	            	System.out.println("Connection to " + host.toString() + " has succeeded.");
	            	session = future.getSession();
		            break;
	            }
	            else{
	            	System.out.println("Connection to " + host.toString() + " has timed out.");
	            }
	        } catch (RuntimeIoException e) {
	        	e.printStackTrace();
	        }
	    }
	}
	
	public void Send_Command(Command c){
    	if(session!=null)
    	{
    		session.write(c);
    	}
    }	
	
	@Override
	public void dispose()
	{
		super.dispose();
		TCP_Commons.CloseSession(session);
	}

	@Override
	public void sessionOpened( IoSession session){
		System.out.println("Session opened with " + session.getRemoteAddress());
		this.session=session;
		session.getConfig().setIdleTime(IdleStatus.READER_IDLE, 10);
	}
	
	@Override
	public void sessionClosed(IoSession session){
		System.out.println("Session closed from " + session.getRemoteAddress());
		session=null;
		if(!disposing){this.restart();}
	}
		
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		System.out.println("Exception caught from " + session.getRemoteAddress());
		session.close(true);
	}
	
	@Override
    public void sessionIdle( IoSession session, IdleStatus status ){// throws Exception
        if(session.getIdleCount(IdleStatus.READER_IDLE)>3){
        	System.out.println("Idled out session " + session.getRemoteAddress() + ". Resetting client.");
        	session.close(true);
        }
        
    }	
}
