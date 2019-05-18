package network.http;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import network.http.websocket.Servlet_uD;
import network.tcp.TCP_Commons;
import network.tcp.TCP_Commons.SecurityBundle;
import udaqc.network.Constants.Addresses;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;


public class HTTPS_Server
{
	private static final String home_dir = "./uDAQC_WebInterface";
	private static final String home_page = "index.html";

	Server server;
	private SecurityBundle bundle = new SecurityBundle("security");
	
	public HTTPS_Server(int insecure_port, int secure_port)
	{
        server = new Server();
        
        ConfigTLS(insecure_port, secure_port);

        ResourceHandler resource_handler = new ResourceHandler();

        // Configure the ResourceHandler. Setting the resource base indicates where the files should be served out of.
        // In this example it is the current directory but it can be configured to anything that the jvm has access to.
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{home_page});
        resource_handler.setResourceBase(home_dir);
        
        ContextHandler context = new ContextHandler();
        context.setContextPath("/*");
                       
        HandlerList hl = new HandlerList();
        hl.addHandler(new SecuredRedirectHandler());
        hl.addHandler(resource_handler);
        hl.addHandler(new DefaultHandler());
        context.setHandler(hl);
        
        ServletContextHandler ws_context = new ServletContextHandler();
		ws_context.setContextPath("/socket");
		Servlet_uD ws_servlet = new Servlet_uD();
		//ServletHolder ws_holder = new ServletHolder();
		//ws_holder.setServlet(ws_servlet);
		//ws_context.addServlet(ws_holder, "/socket");
		ws_context.setHandler(ws_servlet);
        
        HandlerList server_handler=new HandlerList();
        server_handler.addHandler(ws_context);
        server_handler.addHandler(context); //goes last
        server.setHandler(context);
        
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
        
        SslContextFactory sslContextFactory = new SslContextFactory();
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
	
	private void ConfigServlet()
	{
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        HTTP_Handler servlet = new HTTP_Handler(home_dir + "/" + home_page);
        ServletHolder holder = new ServletHolder(servlet);
        
        context.addServlet(holder,"/*");
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
}
