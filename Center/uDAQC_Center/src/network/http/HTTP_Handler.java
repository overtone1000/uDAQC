package network.http;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HTTP_Handler extends HttpServlet
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final String baseDir;

	public HTTP_Handler(String baseDir) 
	{
	  this.baseDir = baseDir;
	}

	@Override
	protected void doGet( HttpServletRequest request,
            HttpServletResponse response ) throws ServletException,
                                          IOException
	{
		System.out.println("Handling HTTP request.");
				
		Path uri = Paths.get(request.getRequestURI());
		if(uri.toString().contains(".."))
		{
			return;
		}
		
		File path = new File(baseDir + uri);
		
		response.setContentType("text/html");
		
		if (path.exists())
		{
			response.setStatus(HttpServletResponse.SC_OK);
			byte[] page = Files.readAllBytes(path.toPath());
			response.getOutputStream().write(page);
		} else
		{
			System.err.println("Wrong URL '" + uri.toString() + "', redirecting.");
			
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", "/");
		}
       
	}

}