package network.http;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

import network.tcp.TCP_Commons;
import network.tcp.TCP_Commons.SecurityBundle;
import udaqc.network.Constants.Addresses;

/*
 *  public static void main(String[] args) throws Exception {
	    SimpleHttpServer server = new SimpleHttpServer();
	    server.start();
	  }
 */

public class HTTPS_Server
{
	private static final String HOMEPAGE = "uDAQC_WebInterface/home.html";

	private HttpServer server;
	private SecurityBundle bundle = new SecurityBundle("security");
	
	private int port;
	public HTTPS_Server(int port)
	{
		this.port=port;
	}
	
	public void start() throws Exception
	{
		HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);

		SSLContext sslContext = getSslContext();
		httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));

		httpsServer.createContext("/", new HTTP_Handler(HOMEPAGE));
		httpsServer.start();
	}

	private SSLContext getSslContext() throws Exception
	{
		KeyManager[] kms = TCP_Commons.CreateKeypair(bundle);
		TrustManager[] tms = TCP_Commons.trustAllCerts();
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kms, tms, null);

		return sslContext;
	}

	public void stop()
	{
		server.stop(0);
	}
}
