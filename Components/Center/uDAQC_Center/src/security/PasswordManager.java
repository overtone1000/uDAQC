package security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Vector;

public class PasswordManager
{
	private RandomAccessFile file;
	public Vector<Credentials> creds = new Vector<Credentials>();
	
	public static class Credentials
	{
		byte[] login;
		byte[] password_hash;
		byte[] salt;
		public void Write(ByteArrayOutputStream bbs)
		{
			ByteBuffer bb = ByteBuffer.allocate(login.length + password_hash.length + salt.length + Integer.BYTES*3);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			
			Integer item_length;
			
			item_length = login.length;
			bb.putInt(item_length);
			bb.put(login);
			
			item_length = password_hash.length;
			bb.putInt(item_length);
			bb.put(password_hash);
			
			item_length = salt.length;
			bb.putInt(item_length);
			bb.put(salt);
			
			try
			{
				bbs.write(bb.array());
			} catch (IOException e)
			{
	
				e.printStackTrace();
			}
		}
		public Credentials(ByteBuffer bb)
		{
			int item_length;
			
			item_length = bb.getInt();
			login = new byte[item_length];
			bb.get(login);
			
			item_length = bb.getInt();
			password_hash = new byte[item_length];
			bb.get(password_hash);
			
			item_length = bb.getInt();
			salt = new byte[item_length];
			bb.get(salt);
		}
		public Credentials(String login, String password)
		{
			this.login = login.getBytes();
			this.salt = PasswordManager.getSalt();
			this.password_hash = PasswordManager.getHash(password, this.salt);
		}
	}
	
	public PasswordManager(Path path)
	{
		if(path==null)
		{
			return;
		}
		
		try
		{
			if(Files.exists(path)) 
			{
				file = new RandomAccessFile(path.toFile(),"rw");
			}
			else
			{
				if(path.getParent()!=null)
				{
					Files.createDirectories(path.getParent());
				}
				file = new RandomAccessFile(Files.createFile(path).toFile(),"rw");
			}
			Load();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean CheckCredentials(String login, String password)
	{
		byte[] login_bytes = login.getBytes();
		for(Credentials c:creds)
		{
			if(java.util.Arrays.equals(login_bytes, c.login))
			{
				byte[] hash = getHash(password, c.salt);
				if(java.util.Arrays.equals(hash, c.password_hash))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public void AddCredentials(String login, String password)
	{
		removelogin(login);
		creds.add(new Credentials(login,password));
		Save();
	}
	
	private void removelogin(String login)
	{
		byte[] login_bytes = login.getBytes();
		for(Credentials c:creds)
		{
			if(java.util.Arrays.equals(login_bytes, c.login)) 
			{
				creds.remove(c);
			}
		}
	}
	
	public void RemoveCredentials(String login)
	{
		removelogin(login);
		Save();
	}
	
	public void Save()
	{
		ByteArrayOutputStream bbs=new ByteArrayOutputStream();
		for(Credentials c:creds)
		{
			c.Write(bbs);
		}
		
		try
		{
			file.setLength(0);
			file.seek(0);
			file.write(bbs.toByteArray());
		} catch (IOException e)
		{

			e.printStackTrace();
		}
		
	}
	
	public void Load()
	{
		creds.clear();
		try
		{
			file.seek(0);
			ByteBuffer bb = ByteBuffer.allocate((int) file.length());
			file.readFully(bb.array());
			bb.position(0);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			while(bb.hasRemaining())
			{
				creds.add(new Credentials(bb));
			}
		} catch (IOException e)
		{

			e.printStackTrace();
		}
	}
	
	private static final int salt_length=64;
	private static byte[] getSalt()
    {
		byte[] salt = new byte[salt_length];
		
		try
		{
	        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
	        sr.nextBytes(salt);
	        return salt;
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
		
		Random i = new Random();
		i.nextBytes(salt);
		return salt;
    }
	
	private static Vector<String> HashAlgorithms;
	static
	{
		HashAlgorithms = new Vector<String>();
		HashAlgorithms.add("SHA-512");
		HashAlgorithms.add("SHA-384");
		HashAlgorithms.add("SHA-256");
		HashAlgorithms.add("SHA-1");
	}
	
	private static byte[] getHash(String password, byte[] salt)
	{
		MessageDigest md=null;
		
		for(String s:HashAlgorithms)
		{
		    try 
		    {
		    	md = MessageDigest.getInstance(s);
		    }
		    catch (NoSuchAlgorithmException e)
		    {
		    	HashAlgorithms.remove(s);
		        e.printStackTrace();
		    }
		}
		
	    if(md==null)
	    {
	    	return new byte[] {0};
	    }
	    
        md.update(salt);
        byte[] bytes = md.digest(password.getBytes());
        return bytes;
	}
}
