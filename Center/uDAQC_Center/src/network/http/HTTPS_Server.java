package network.http;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import network.http.websocket.Servlet_uD;
import network.tcp.TCP_Commons;
import network.SecurityBundle;
import udaqc.io.log.IO_System_Logged;
import udaqc.network.Constants.Addresses;
import udaqc.network.center.command.Command;
import udaqc.network.passthrough.command.PT_Command;
import udaqc.network.passthrough.endpoints.WS_Endpoint;

import org.apache.felix.framework.util.Mutex;
import org.apache.mina.core.session.IoSession;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;


public class HTTPS_Server
{
	private ArrayList<Session> sessions = new ArrayList<Session>();
	
	private static final String home_dir = "../uDAQC_WebInterface";
	private static final String home_page = "index.html";
	
	private Semaphore session_mutex=new Semaphore(1);

	Server server;
	Servlet_uD ws_servlet;
	private SecurityBundle bundle = new SecurityBundle("security");
	
	public HTTPS_Server(int insecure_port, int secure_port)
	{
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
        
        ServletContextHandler ws_context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ws_context.setContextPath("/socket"); //This context handles anything in the socket directory. Note, a call to "url/socket" won't work. It needs to be "url/socket/"
		ws_servlet = new Servlet_uD(this);
		ServletHolder ws_holder = new ServletHolder("socket",ws_servlet);
		ws_holder.setServlet(ws_servlet);
		ws_context.addServlet(ws_holder, "/*"); //This servlet will handle anything.
		
        //Now for security
		Path config_file = Paths.get("./security/realm.properties");
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
        LoginService loginService = new HashLoginService("MyRealm",
                config_file.toString());
        server.addBean(loginService);
        
        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        server.setHandler(security);
        
        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[] { "admin_role", "guest_role" });
        
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);
        
        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(loginService);
        
		//ContextHandlerCollection server_handlers = new ContextHandlerCollection();
        HandlerList server_handlers=new HandlerList();	
        server_handlers.addHandler(ws_context);
        server_handlers.addHandler(context); //should go last? Yes. It's the default "/*" context, so let other contexts try first.
        
        security.setHandler(server_handlers); //chain security on top of contexts
        
        server.setHandler(security); //security is primary handler
        
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
	
	public void start() throws Exception
	{
		server.start();
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
		IO_System_Logged.PassthroughInitialization(new WS_Endpoint(new_session));
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
}
