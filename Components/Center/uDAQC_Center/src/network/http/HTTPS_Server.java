package network.http;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;

import network.http.websocket.Servlet_uD;
import network.SecurityBundle;
import udaqc.io.IO_Constants;
import udaqc.network.center.Center;
import udaqc.network.center.DirectDevice;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.command.PT_Command;
import udaqc.network.passthrough.endpoints.WS_Endpoint;

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
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.util.security.Credential;

public class HTTPS_Server
{
	private ArrayList<Session> sessions = new ArrayList<Session>();
	
	public String home_dir;
	public String HomeDirectory()
	{
		return home_dir;
	}
	
	private Path config_file;
	
	private static final String home_page = "index.html";
	public static final String credential_context = "/credentials";
		
	private Semaphore session_mutex=new Semaphore(1);

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
				// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
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
        sslContextFactory.setKeyStorePath(bundle.keystore_filename);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	public void SessionOpened(Session new_session)
	{
		DirectDevice.PassthroughInitialization(new WS_Endpoint(new_session));
		try
		{
			session_mutex.acquire();
			sessions.add(new_session);
			session_mutex.release();
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
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
		} catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
    
    public void HandleCommand(Command command)
    {
    	switch(command.Header().command_id)
    	{
    	case IO_Constants.Command_IDs.passthrough:
    		PT_Command ptc = new PT_Command(command);
    		parent.Passthrough_from_Secondary(ptc);
    		break;
    	default:
    		System.out.println("Unknown command received from web client.");
    	}
    }
}
