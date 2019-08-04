package network.tcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import network.TCP_Base;
import network.tcp.TCP_Commons;
//import trm.tcp.Command;
import utilities.Timer;

public class TCP_Server extends TCP_Base
{
	private boolean explicit_port = false;
	private int PORT;

	protected java.util.HashSet<IoSession> sessions = new java.util.HashSet<IoSession>();
		
	public void serverinitialized()
	{
		//Override to handle operations once the server is up and running	
	}
	
	public int Port()
	{
		return PORT;
	}
	
	public TCP_Server(String threadName, boolean SSL_enabled, boolean start)
	{
		this.explicit_port=false;
		this.threadName = threadName;
		this.SSL=SSL_enabled;
		if(start) {this.start();}
	}

	public TCP_Server(String threadName, int Port, boolean SSL_enabled, boolean start)
	{
		this.explicit_port = true;
		this.PORT = Port;
		this.threadName = threadName;
		this.SSL=SSL_enabled;
		if(start) {this.start();}
	}
	
	@Override
	public void run()
	{
		System.out.println("Starting server on thread " + threadName);
		IoAcceptor acceptor = null;
		Timer resettimer;
		int resetcount = 0;

		resettimer = new Timer(15 * 1000); // reset connection after 10 seconds of inactive time
		while (continuerunning)
		{
			if (acceptor != null)
			{
				if (acceptor.isActive())
				{
					resettimer.reset();
				}
			}
			if (resettimer.reached())
			{
				if (acceptor != null)
				{
					acceptor.dispose();
					acceptor = null;
				}
				TCP_Status = "resetting";
			}
			// isActive seems to become false when the connection is IDLE, based on the
			// initialization settings.
			if (acceptor == null || acceptor.isDisposed() == true)
			{
				resetcount += 1;
				acceptor = new NioSocketAcceptor();
				LoggingFilter logger = new LoggingFilter();
				logger.setMessageReceivedLogLevel(LogLevel.TRACE);
				logger.setMessageSentLogLevel(LogLevel.TRACE);
				
				acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new udaqc.network.center.command.CommandCodecFactory(false)));
				acceptor.getFilterChain().addLast("logger", logger);
				
				if(SSL)
		        {
		        	TCP_Commons.AddSSLFilter(acceptor);
		        }
				
				acceptor.setHandler(this);
				acceptor.getSessionConfig().setReadBufferSize(2048);
				acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 5);

				try
				{
					if(this.explicit_port)
					{
						acceptor.bind(new InetSocketAddress(PORT));
					}
					else
					{
						acceptor.bind(new InetSocketAddress(0));
						this.PORT = GetPort(acceptor.getLocalAddress());
					}
							
					TCP_Status = "Server successfully initialized";
					this.serverinitialized();
				} catch (IOException e)
				{
					TCP_Status = "IOException when starting server";
					System.out.println(TCP_Status + " " + threadName + ":" + e.getMessage() + ". Will reattempt after interval.");
				}
			} else
			{
				if (acceptor.isActive())
				{
					TCP_Status = "Online, active.";
				} else
				{
					TCP_Status = "Online, idle.";
				}
				TCP_Status = TCP_Status + " Resetting in " + resettimer.timeleft() + ". Total resets = " + resetcount + ".";
				try
				{
					Thread.sleep(1000);
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
		if (acceptor != null)
		{
			acceptor.dispose();
			acceptor = null;
		}
		
		for(IoSession ses:sessions) {
			TCP_Commons.CloseSession(ses);
		}
		
		System.out.println("Closing " + threadName + " server.");
	}
	
	@Override
	public void sessionOpened(IoSession session)
	{
		sessions.add(session);
		session.getConfig().setIdleTime(IdleStatus.READER_IDLE, 20);
	}

	@Override
	public void sessionClosed(IoSession session)
	{
		sessions.remove(session);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception
	{
		cause.printStackTrace();
		this.restart();
	}

	@Override
	public void messageReceived(IoSession session, Object message)
	{
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status)
	{// throws Exception
		System.out.println("IDLE " + session.getIdleCount(status));
	}

	/*
	public void Send_Command(Command c, IoSession session)
	{
		session.write(c);
	}

	public void BroadCast_Command(Command c)
	{
		if (sessions.isEmpty())
		{
			return;
		}
		for (IoSession s : sessions)
		{
			s.write(c);
		}
	}
	*/
}
