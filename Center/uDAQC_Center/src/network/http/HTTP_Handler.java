package network.http;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HTTP_Handler implements HttpHandler
{

	private final String baseDir;

	public HTTP_Handler(String baseDir) 
	{
	  this.baseDir = baseDir;
	}

	@Override
	public void handle(HttpExchange ex) throws IOException
	{
		System.out.println("Handling HTTP request.");
		
		URI uri = ex.getRequestURI();
		String name = new File(uri.getPath()).getName();
		if(name.contains(".."))
		{
			return;
		}
		
		File path = new File(baseDir, name);

		Headers h = ex.getResponseHeaders();
		// Could be more clever about the content type based on the filename here.
		h.add("Content-Type", "text/html");

		OutputStream out = ex.getResponseBody();

		if (path.exists())
		{
			ex.sendResponseHeaders(200, path.length());
			out.write(Files.readAllBytes(path.toPath()));
		} else
		{
			System.err.println("Wrong URL '" + name + "', redirecting.");
			ex.getResponseHeaders().add("Location", "/"); //always send right back to root
			ex.sendResponseHeaders(301, -1);
			ex.close();
		}

		out.close();
	}

}