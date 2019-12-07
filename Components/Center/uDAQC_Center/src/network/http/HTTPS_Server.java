package network.http;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.PropertyUserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;

import network.SecurityBundle;
import network.http.websocket.Servlet_uD;
import threading.ThreadWorker;
import udaqc.io.IO_Constants;
import udaqc.io.IO_Constants.Command_IDs;
import udaqc.io.IO_System;
import udaqc.jdbc.Database_uDAQC.HistoryResult;
import udaqc.jdbc.Database_uDAQC.Regime;
import udaqc.network.center.Center;
import udaqc.network.center.IO_Device_Connected;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.command.PT_Command;
import udaqc.network.passthrough.endpoints.WS_Endpoint;

public class HTTPS_Server
{
	private class Subscription
    {
		public Subscription(Session session, IO_System system, Regime r, Instant d)
		{
			this.session=session;
			this.system=system;
			this.regime=r;
			this.setLast(d);
		}
		private Session session;
		private IO_System system;
    	private Regime regime;
    	private Instant last=null;
    	private Instant next=null;
    	private void updateNext()
    	{
    		System.out.println(this.regime);
    		Duration d = this.regime.getDuration();
    		this.next=this.last.plus(d); //this will work for raw too!
    	}
    	private void setLast(Instant l)
    	{
    		this.last=l;
    		this.updateNext();
    	}
    	private Instant getNext()
    	{
    		return this.next;
    	}
    	public void CheckUpdate(Instant update_timestamp)
    	{
    		System.out.println("Checking for updates for system " + system.FullName() + ", regime " + this.regime);
    		if(!(this.getNext().isAfter(update_timestamp)));
			{
				HistoryResult his = Center.database.getHistory(system, this.regime, Timestamp.from(this.last), end_of_time);
				
				if(his.data_count>0)
				{
					System.out.println("Sending update from " + update_timestamp.toString() + " containing " + his.data_count + " elements.");
					Command c = new Command(IO_Constants.Command_IDs.history_update,his.message.array());
					SendCommand(session,c);
				}
				
				this.setLast(update_timestamp);
			}
    	}
    }
	
	public String home_dir;
	public String HomeDirectory()
	{
		return home_dir;
	}
	
	private Path config_file;
	
	private static final String home_page = "index.html";
	public static final String credential_context = "/credentials";
		
	private HashSet<Session> sessions = new HashSet<Session>();
	private Semaphore session_mutex=new Semaphore(1);
    
    protected class SubscriptionMaintainer
    {
    	private HashMap<Session,HashMap<IO_System,Subscription>> subscriptions=new HashMap<Session,HashMap<IO_System,Subscription>>();
        private Semaphore subscription_mutex = new Semaphore(1);
        
		protected void handleSystemDataUpdated(IO_System system)
		{
			System.out.println("System " + system.FullName() + " data updated. Forwarding to subscribers.");
			
			//Just for testing right now...
			Instant ts=null;
			try
			{
				ts = system.getTimestamp();
			}
			catch(Exception e)
			{
				ts=Instant.now();
			}
			
			System.out.println("There are " + subscriptions.keySet().size() + " sessions subscribing.");
			
			checkSubscribers();
			for(Session sess:subscriptions.keySet())
			{
					HashMap<IO_System,Subscription> system_subs=subscriptions.get(sess);
					Subscription sub = system_subs.get(system);
					if(sub!=null)
					{
						sub.CheckUpdate(ts);
					}
			}
		}
		
		protected void addSubscriber(Session session, IO_System system, Regime r, Instant last)
		{
			try {
				subscription_mutex.acquire();
				
				Subscription sub = new Subscription(session,system,r,last);
				
				HashMap<IO_System,Subscription> ses_subs=subscriptions.get(session);
				if(ses_subs==null)
				{
					ses_subs = new HashMap<IO_System,Subscription>();
					subscriptions.put(session, new HashMap<IO_System,Subscription>());
				}
				
				ses_subs.put(system, sub);
				
				subscription_mutex.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		protected void checkSubscribers()
		{
			try {
				subscription_mutex.acquire();
				Iterator<Session> i = subscriptions.keySet().iterator();
				while(i.hasNext())
				{
					Session sess=i.next();
					if(!sess.isOpen())
					{
						i.remove();
					}
				}
				subscription_mutex.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		protected void removeSubscriber(Session ses)
		{
			try {
				subscription_mutex.acquire();
				subscriptions.remove(ses);
				subscription_mutex.release();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
    }
    private SubscriptionMaintainer sub_main=new SubscriptionMaintainer();    
	
	private Center parent;
	private Server server;
	private Servlet_uD ws_servlet;
	private SecurityBundle bundle;
	
	PropertyUserStore store = new PropertyUserStore();
	
	private static final String[] roles = new String[] { "admin" };
	
	public boolean changeCredentials(String login, String password)
	{
		String md5 = Credential.MD5.digest(password);
		Credential cred = Credential.getCredential(md5);
		
		String rolestring="";
		for(int n=0;n<roles.length-1;n++)
		{
			rolestring+=roles[n]+",";
		}
		rolestring+=roles[roles.length-1];
		
		if(config_file.toFile().exists())
		{
			config_file.toFile().delete();
		}
		
		try
		{
			java.io.FileWriter fos = new java.io.FileWriter(config_file.toFile());
			fos.write(login + ":" + md5 + "," + rolestring);
			fos.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		
		Map<String,UserIdentity> users = store.getKnownUserIdentities();
		for(String s:users.keySet())
		{
			store.removeUser(s);
		}
		store.addUser(login, cred, roles);
		
		return true;		
	}
	
	private String root;
	public HTTPS_Server(Center parent, String root, int insecure_port, int secure_port)
	{
		this.parent = parent;
		this.root = root;
		
		this.home_dir = this.root + "/../../uDAQC_WebInterface";
		this.config_file = Paths.get(this.root + "/security/realm.properties");
		this.bundle = new SecurityBundle(this.root + "/security");
		
		if(!config_file.toFile().exists())
		{
			try
			{
				Files.createDirectories(config_file.getParent());
				config_file.toFile().createNewFile();
				
				FileWriter fw = new FileWriter(config_file.toFile(),false);
				
				fw.write("admin:admin");
				for(String s:roles)
				{
					fw.write("," + s);
				}
				
				fw.close();
			} catch (IOException e)
			{
	
				e.printStackTrace();
			}
		}
		
		store.setConfigPath(config_file);
		
		try
		{
			store.setHotReload(true);
			store.start();
		} catch (Exception e1)
		{
			System.out.println("Store couldn't start.");
			e1.printStackTrace();
			return;
		}
		
        server = new Server();
        //Server server2 = new Server(8080);
        
        ConfigTLS(insecure_port, secure_port);

        ResourceHandler resource_handler = new ResourceHandler();

        // Configure the ResourceHandler. Setting the resource base indicates where the files should be served out of.
        // In this example it is the current directory but it can be configured to anything that the jvm has access to.
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{home_page});
        resource_handler.setResourceBase(home_dir);
        
        ContextHandler context = new ContextHandler();
        context.setContextPath("/"); //This context will handle anything from the root directory up unless another context handles it first.
                       
        HandlerList hl = new HandlerList();
        hl.addHandler(new SecuredRedirectHandler());
        hl.addHandler(resource_handler);
        hl.addHandler(new DefaultHandler());
        context.setHandler(hl);
        
        ContextHandler cred_context = new ContextHandler();
        HTTP_PostHandler handler = new HTTP_PostHandler(this);
        cred_context.setHandler((Handler)handler);
        cred_context.setContextPath(credential_context);
        
        ServletContextHandler ws_context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ws_context.setContextPath("/socket"); //This context handles anything in the socket directory. Note, a call to "url/socket" won't work. It needs to be "url/socket/"
		ws_servlet = new Servlet_uD(this);
		ServletHolder ws_holder = new ServletHolder("socket",ws_servlet);
		ws_holder.setServlet(ws_servlet);
		ws_context.addServlet(ws_holder, "/*"); //This servlet will handle anything.
		
        //Now for security
		
		if(!config_file.toFile().exists())
		{
			try
			{
				Files.createDirectories(config_file.getParent());
				Files.createFile(config_file);
				//This file needs to be populated like so:
				/*
				 guest_login: guestpw,user
				 admin_login: adminpw,admin
				 encryptedpw_guest_login: MD5:d45e977bfef260da159d651b9de7035d,user
				 */
			} catch (IOException e)
			{
	
				e.printStackTrace();
			}
		}
		
		HashLoginService loginService = new HashLoginService("uDAQC");
        loginService.setUserStore(store);
        server.addBean(loginService);
                
        //Example of how to digest a password
        //String md5 = org.eclipse.jetty.util.security.Credential.MD5.digest("password");
        //System.out.println("Digested password is:" + md5);
        //Output for this is "MD5:5f4dcc3b5aa765d61d8327deb882cf99"
        
        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        server.setHandler(security);
        
        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(roles);
        
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);
        
        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);
        
		//ContextHandlerCollection server_handlers = new ContextHandlerCollection();
        HandlerList server_handlers=new HandlerList();	
        server_handlers.addHandler(ws_context);
        server_handlers.addHandler(cred_context);
        server_handlers.addHandler(context); //should go last? Yes. It's the default "/*" context, so let other contexts try first.
        
        security.setHandler(server_handlers); //chain security on top of contexts
        
        server.setHandler(security); //security is primary handler
        
        while(!server.isStarted())
        {
        	try
    		{
    			server.start();
    		} catch (Exception e)
    		{
    			int time = 5000;
    			System.out.println("Web server couldn't start. Exception: " + e.getMessage() + ". Restarting in " + time + " ms.");
    			e.printStackTrace();
    			
    			try
				{
					Thread.sleep(time);
				} catch (InterruptedException e1)
				{
					e1.printStackTrace();
				}
    		}
        }
	}
	
	public Center Parent()
	{
		return parent;
	}
	
	private void ConfigTLS(int insecure_port, int secure_port)
	{		
		HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(secure_port);
        http_config.setOutputBufferSize(32768);

        ServerConnector http = new ServerConnector(server,new HttpConnectionFactory(http_config));
        http.setPort(insecure_port);
        http.setIdleTimeout(30000);

        HttpConfiguration https_config = new HttpConfiguration(http_config);
        SecureRequestCustomizer src = new SecureRequestCustomizer();
        src.setStsMaxAge(2000);
        src.setStsIncludeSubDomains(true);
        https_config.addCustomizer(src);
        
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStore(bundle.keystore);
        //sslContextFactory.setKeyStorePath(bundle.keystore_filename);
        sslContextFactory.setKeyStorePassword(bundle.keystore_password);
        sslContextFactory.setKeyManagerPassword(bundle.key_password);

        ServerConnector https = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(https_config));
        https.setPort(secure_port);
        https.setIdleTimeout(500000);

        // Here you see the server having multiple connectors registered with
        // it, now requests can flow into the server from both http and https
        // urls to their respective ports and be processed accordingly by jetty.
        // A simple handler is also registered with the server so the example
        // has something to pass requests off to.

        // Set the connectors
        server.setConnectors(new Connector[] { https, http });       
	}

	public void stop()
	{
		try
		{
			server.join();
		} catch (InterruptedException e)
		{

			e.printStackTrace();
		}
	}
		
	public void SessionOpened(Session new_session)
	{
		IO_Device_Connected.PassthroughInitialization(new WS_Endpoint(new_session));
		try
		{
			session_mutex.acquire();
			sessions.add(new_session);
			session_mutex.release();
		} catch (InterruptedException e)
		{

			e.printStackTrace();
		}
	}
	
	public void SessionClosed(Session closed_session)
	{
		try
		{
			session_mutex.acquire();
			sessions.remove(closed_session);
			session_mutex.release();
			
			sub_main.removeSubscriber(closed_session);
			
		} catch (InterruptedException e)
		{

			e.printStackTrace();
		}
	}
	
	public void Broadcast(PT_Command command)
    {
		try
		{
			session_mutex.acquire();
			Iterator<Session> it=sessions.iterator();
			while(it.hasNext())
	    	{
				Session s = it.next();
				if(!s.isOpen())
				{
					System.out.println("Trying to close abandoned session.");
					it.remove();
				}
				else
				{
					SendCommand(s,command);
				}
	    	}
			session_mutex.release();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
    }
	
	
	public void handleSystemDataUpdated(IO_System system)
	{
		sub_main.handleSystemDataUpdated(system);
	}
	    
    public static boolean SendCommand(Session sess, Command command)
    {
    	if(!sess.isOpen())
    	{
    		System.out.println("Tried sending command, but the session closed.");
    		return false;
    	}
    	try
		{
			sess.getRemote().sendBytes(ByteBuffer.wrap(command.toBuffer().array()));
			return true;
		} catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
    }
    
    public static final Timestamp end_of_time = Timestamp.from(Instant.parse("9999-12-30T23:59:59.99Z")); //Long after human extinction, and also near the end of supported perior per SQL specs
    
    private static final int max_points = 1024;
    private void handleRecentHistoryRequest(Session session, IO_System system, ByteBuffer data)
    {
    	Duration d = Duration.ofMillis(data.getLong());
	
		HistoryResult his = Center.database.getRecentHistory(system, d, max_points);
		
		if(his.data_count>0)
		{
			Command c = new Command(IO_Constants.Command_IDs.history, his.message.array());
			SendCommand(session, c);
		}
		
		System.out.println("Adding subscriber for regime " + his.reg.toString() + " last datum at " + his.last.toString());
		sub_main.addSubscriber(session,system,his.reg,his.last);	
    }
    
    private void handleHistoryIntervalRequest(Session session, IO_System system, ByteBuffer data)
    {
		long start = data.getLong();
		long end = data.getLong();
		Timestamp start_ts = null;
		Timestamp end_ts = null;

		boolean live_subscription_requested = false;

		if (start >= 0) {
			start_ts = Timestamp.from(Instant.ofEpochMilli(start));
		} else {
			start_ts = Timestamp.from(Instant.ofEpochMilli(0));
			System.out.println("Returning earliest history available.");
		}
		if (end >= 0) {
			end_ts = Timestamp.from(Instant.ofEpochMilli(end));
		} else {
			end_ts = end_of_time;
			System.out.println("Returning latest history available.");
			live_subscription_requested = true;
		}

		HistoryResult his = Center.database.getConciseHistory(system, start_ts, end_ts, max_points);
		Command c = new Command(IO_Constants.Command_IDs.history, his.message.array());
		SendCommand(session, c);

		if (live_subscription_requested) {
			sub_main.addSubscriber(session,system,his.reg,his.last);
		} else {
			sub_main.removeSubscriber(session);
		}
	}
    
    public void HandleCommand(Command command, Session session)
    {
    	switch(command.Header().command_id)
    	{
    	case IO_Constants.Command_IDs.passthrough:
    		PT_Command ptc = new PT_Command(command);
    		parent.Passthrough_from_Secondary(ptc);
		break;
    	case Command_IDs.history_request:
		  {
			  ByteBuffer data = command.getmessage();
			  short dev_index = data.getShort();
			  short sys_index = data.getShort();
			  byte histype = data.get();
			  boolean type_is_recent = histype!=(byte)(0);
			  IO_System system = IO_Device_Connected.getDirectDevice(dev_index).GetSystem(sys_index);
			  if(type_is_recent)
			  {
				  System.out.println("Handling recent history request");
				  handleRecentHistoryRequest(session,system,data);
			  }
			  else
			  {
				  System.out.println("Handling history interval request");
				  handleHistoryIntervalRequest(session,system,data);
			  }
		  }
		  break;
    	default:
    		System.out.println("Unknown command received from web client.");
    	}
    }
}
