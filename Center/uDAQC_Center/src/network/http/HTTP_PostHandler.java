package network.http;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

public class HTTP_PostHandler implements Handler
{
	private static final String login = "login";
	private static final String password1 = "password1";
	private static final String password2 = "password2";
	private static final String realm = "realm";
	
	private static final String new_server_creds = "/new_server_credentials";
	private static final String new_device_creds = "/new_device_credentials";
	private static final String device_creds_html = "/device_credentials.html";
	private static final String device_cred_list = "/credentials/device_credential_list.txt";
	
	HTTPS_Server parent;
	public HTTP_PostHandler(HTTPS_Server parent)
	{
		this.parent=parent;
	}

	@Override
	public void start() throws Exception
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() throws Exception
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRunning()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStarted()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStarting()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStopping()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isStopped()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isFailed()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addLifeCycleListener(Listener listener)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeLifeCycleListener(Listener listener)
	{
		// TODO Auto-generated method stub
		
	}
	
	private void handleServerCredentialChange(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{		
		String new_login = request.getParameter(login);
		//System.out.println("New login:" + new_login);
		String pw1 = request.getParameter(password1);
		//System.out.println("Pw1:" + pw1);
		String pw2 = request.getParameter(password2);
		//System.out.println("Pw2:" + pw2);
		
		String message = "";
		
		if(new_login == null || pw1 == null || pw2 == null)
		{
			message = "Expected paramaters not included. This suggests a server side software error.";
		}
		else if(new_login.equals("") || pw1.equals("") || pw2.equals(""))
		{
			message = "A blank field was received. Credentials are unchanged.";
		}
		else if(!pw1.equals(pw2))
		{
			message = "Passwords don't match. Credentials are unchanged.";
		}
		else if(!parent.changeCredentials(new_login, pw1))
		{
			message = "Couldn't write credential file.";
		}
		else
		{
			message = "Credentials successfully changed.";
		}
		
		String htmlRespone = "<html>";
		htmlRespone += message;
		htmlRespone += "</html>";
		
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().print(htmlRespone);
	}
	
	private void handleDeviceCredentialChange(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{		
		String new_login = request.getParameter(login);
		System.out.println("New login:" + new_login);
		String pw1 = request.getParameter(password1);
		System.out.println("Pw1:" + pw1);
		String pw2 = request.getParameter(password2);
		System.out.println("Pw2:" + pw2);
		String rlm = request.getParameter(realm);
		System.out.println("Realm:" + pw2);
		
		String message = "";
		
		if(new_login == null || pw1 == null || pw2 == null || rlm == null)
		{
			message = "Expected paramaters not included. This suggests a server side software error.";
		}
		else if(new_login.equals("") || pw1.equals("") || pw2.equals("") || rlm.equals(""))
		{
			message = "A blank field was received. Credentials are unchanged.";
		}
		else if(!pw1.equals(pw2))
		{
			message = "Passwords don't match. Credentials are unchanged.";
		}
		else if(false)
		{
			message = "Couldn't write credential file.";
		}
		else
		{
			message = "Credentials successfully changed.";
		}
		
		String htmlRespone = "<html>";
		htmlRespone += message;
		htmlRespone += "</html>";
		
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().print(htmlRespone);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{		
		System.out.print("Target is " ); System.out.println(target);
		
		if(target.equals(new_server_creds))
		{
			baseRequest.setHandled(true);
			handleServerCredentialChange(request,response);
		}
		else if(target.contentEquals(new_device_creds))
		{
			baseRequest.setHandled(true);
			handleDeviceCredentialChange(request,response);
		}
		else if(target.contentEquals(device_creds_html))
		{
			//allow the request to remain unhandled so the resource handler sends the html file, but update the list of credentials first
			String filename = HTTPS_Server.home_dir + device_cred_list;
			Path path = Paths.get(filename);
			if(path.toFile().exists())
			{
				Files.delete(Paths.get(filename));
			}
			FileWriter w = new FileWriter(filename,false);
			w.write("Testing 123!\n");
			w.write("Testing 223!\n");
			w.close();
			
			baseRequest.setHandled(false);	
		}
		else
		{
			System.out.println("Request for target " + target + " unhandled by dedicated credential handler. Next handler will be called.");
			baseRequest.setHandled(false);
		}
	}

	@Override
	public void setServer(Server server)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Server getServer()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroy()
	{
		// TODO Auto-generated method stub
		
	}

}
