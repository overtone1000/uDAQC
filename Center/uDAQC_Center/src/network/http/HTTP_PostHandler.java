package network.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.PasswordConverter;
import org.bouncycastle.jcajce.provider.keystore.BC;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

public class HTTP_PostHandler implements Handler
{
	private static final String login = "login";
	private static final String password1 = "password1";
	private static final String password2 = "password2";
	private static final String realm = "realm";
	
	private static final String new_server_creds = "/new_server_credentials";
	private static final String new_device_creds = "/new_device_credentials";
	private static final String remove_device_creds = "/remove_device_credentials";
	private static final String device_creds_html = "/device_credentials.html";
	private static final String device_cred_list = "/credentials/device_credential_list.txt";
	private static final String master_device_cred_list = "security/device_credential_list.txt";
	
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
	
	private boolean changeDeviceCredentials(String new_login, String realm, String pw)
	{
		String conglomerate = new_login + ":" + realm + ":" + pw;
		
		Charset encoding = java.nio.charset.StandardCharsets.UTF_8;
		
		byte[] conglomerate_bytes = encoding.encode(conglomerate).array();
		
		System.out.println("Input bytes:");
		for(byte b:conglomerate_bytes)
		{
			System.out.println(b);
		}
		System.out.println();
		
		MessageDigest digester=null;
		try
		{
			digester=java.security.MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			return false;
		}
		
		byte[] md5 = digester.digest(conglomerate_bytes);
		
		String md5_hex_string="";
		ByteBuffer bb = ByteBuffer.wrap(md5);
		while(bb.hasRemaining())
		{
			md5_hex_string+=Integer.toHexString(bb.getInt());
		}
		
		System.out.print("Conglomerate is: ");
		System.out.println(encoding.decode(ByteBuffer.wrap(conglomerate_bytes)));
		System.out.println("Digest is " + md5_hex_string);
						
		LinkedList<String> creds = readDeviceCredentials();
		creds.add(new_login + ":" + realm + ":" + md5_hex_string);
		return deviceCredsToFile(creds);	
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
	
	private boolean handleDeviceCredentialChange(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
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
		else if(!this.changeDeviceCredentials(new_login, rlm, pw1))
		{
			message = "Couldn't write credential file.";
		}
		else
		{
			return true;
		}
		
		String htmlRespone = "<html>";
		htmlRespone += message;
		htmlRespone += "</html>";
		
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.getOutputStream().print(htmlRespone);
		
		return false;
	}
	
	private LinkedList<String> readDeviceCredentials()
	{
		LinkedList<String> retval = new LinkedList<String>();
		
		File f = new File(master_device_cred_list);
		if(f.exists())
		{
			int size;
			try
			{
				size = (int) Files.size(f.toPath());
			} catch (IOException e)
			{
				e.printStackTrace();
				return retval;
			}
						
			FileReader fr=null;
			try
			{
				fr = new FileReader(f);
				CharBuffer buf = CharBuffer.allocate((int) Files.size(f.toPath()));
				fr.read(buf);
				buf.flip();
				String this_cred = "";
				
				char next;
				while(buf.remaining()>0)
				{
					next=buf.get();
				
					if(next=='\n')
					{
						retval.add(this_cred);
						this_cred = new String();
					}
					else
					{
						this_cred+=(char)next;
					}
				}
				
				fr.close();
			} catch (IOException e)
			{
				e.printStackTrace();
				return retval;
			}
			
			System.out.println("Current creds are:");
			for(String s:retval)
			{
				System.out.println(s);
			}
			System.out.println("File size = " + size);
		}
		else
		{
			System.out.println("No credential file found.");
		}
		
		return retval;
	}
	private boolean removeDeviceCredentials(int index)
	{
		LinkedList<String> creds = readDeviceCredentials();
		creds.remove(index);
		return deviceCredsToFile(creds);		
	}
	private boolean deviceCredsToFile(LinkedList<String> creds)
	{
		File f = new File(master_device_cred_list);

		if(!f.exists()) {try
		{
			System.out.println("Creating directories for file " + f.getAbsolutePath().toString());
			Files.createDirectories(f.toPath().getParent());
			System.out.println("Creating file " + f.getAbsolutePath().toString());
			if(!f.createNewFile())
			{
				System.out.println("Couldn't create file.");
				return false;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}}
		
		try
		{
			FileWriter fos = new FileWriter(f,false);
			for(String s:creds)
			{
				fos.write(s + '\n');
			}
			fos.close();
		} catch (IOException e)
		{
			e.printStackTrace();
			
			return false;
		}
		
		return true;
	}
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
	{		
		System.out.print("Target is " ); System.out.println(target);
		
		//boolean remove_cred = target.substring(0, target.lastIndexOf('/')-1).equals(remove_device_creds);
		
		if(target.length()>=remove_device_creds.length())
		{
			if(target.substring(0,remove_device_creds.length()).equals(remove_device_creds))
			{
				System.out.println("Handling credential removal.");
				Integer index = Integer.parseInt(target.substring(remove_device_creds.length()+1,target.length()));
				if(removeDeviceCredentials(index))
				{
					baseRequest.setHandled(true);
					response.sendRedirect(HTTPS_Server.credential_context+device_creds_html);
				}
				else
				{
					response.setContentType("text/html");
					response.setStatus(HttpServletResponse.SC_OK);
					response.setCharacterEncoding("UTF-8");
					response.getOutputStream().print("Couldn't delete credentials.");
				}
				return;
			}
		}
		
		switch(target)
		{
		case new_server_creds:
		{
			System.out.println("Handling new server credentials.");
			handleServerCredentialChange(request,response);
			baseRequest.setHandled(true);
			break;
		}
		case new_device_creds:
		{
			System.out.println("Handling new device credentials.");
			if(handleDeviceCredentialChange(request,response))
			{
				response.sendRedirect(HTTPS_Server.credential_context+device_creds_html);
			}
			baseRequest.setHandled(true);
			break;
		}
		case device_creds_html:
		{
			System.out.println("Handling basic device credential list form.");
			//get the existing credentials for the list
			LinkedList<String> current_creds = readDeviceCredentials();
			
			//allow the request to remain unhandled so the resource handler sends the html file, but update the list of credentials first
			String filename = HTTPS_Server.home_dir + device_cred_list;
			Path path = Paths.get(filename);
			if(path.toFile().exists())
			{
				Files.delete(Paths.get(filename));
			}
			FileWriter w = new FileWriter(filename,false);
			for(String s:current_creds)
			{
				w.write(s + '\n');
			}
			w.close();
			
			baseRequest.setHandled(false);	
			break;
		}
		default:
		{
			System.out.println("Request for target " + target + " unhandled by dedicated credential handler. Next handler will be called.");
			baseRequest.setHandled(false);
			break;
		}
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
