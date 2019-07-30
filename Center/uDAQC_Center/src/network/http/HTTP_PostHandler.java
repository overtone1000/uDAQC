package network.http;

import java.io.IOException;
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

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{
		baseRequest.setHandled(true);
		
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
