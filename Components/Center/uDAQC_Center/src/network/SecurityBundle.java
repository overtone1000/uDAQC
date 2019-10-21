package network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Locale;
import java.util.Random;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.IntStream;

import network.tcp.TCP_Commons;

public class SecurityBundle
{
	public String keystore_filename=null;
	public String keystore_password;
	public String key_password;
	public KeyStore keystore;
	
	private static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String lower = upper.toLowerCase(Locale.ROOT);
	private static final String digits = "0123456789";
	private static final String special = "_";
	private static final String alphanum = upper + lower + digits + special;
			
	private static String RandomString(int length)
	{
		String retval="";
		Random rand = new Random();
		
		IntStream ints = rand.ints(0, alphanum.length());
		OfInt ints_it = ints.iterator();
		for(int n = 0; n<length; n++)
		{
			retval+=alphanum.charAt(ints_it.next());
		}
		return retval;
	}
	
	private void GeneratePasswords()
	{
		keystore_password = RandomString(15) + "0";	
		key_password = RandomString(15) + "0";
	}
	
	public SecurityBundle()
	{
		//Creates a security bundle with keystore in memory only.
		GeneratePasswords();
		CreateKeystore();
	}
	
	public SecurityBundle(String directory)
	{
		
		//Creates a security bundle that's saved to disk.
		keystore_filename=directory+"/self.ks";
		String bundle_filename=directory+"/random.txt";
		Path p = Paths.get(bundle_filename);
		File f = new File(bundle_filename);
		if(!Files.exists(p))
		{
			GeneratePasswords();
			try
			{
				Files.createDirectories(Paths.get(directory));
				f.getParentFile().mkdirs();
				f.createNewFile();
				PrintWriter fos = new PrintWriter(bundle_filename);
				fos.println(keystore_password);
				fos.println(key_password);
				fos.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				BufferedReader fis = new BufferedReader(new FileReader(bundle_filename));
				keystore_password = fis.readLine();
				key_password = fis.readLine();
				fis.close();
			} catch (IOException e)
			{
				GeneratePasswords();
				e.printStackTrace();
			}
		}
		CreateKeystore();			
	}
	
	protected void CreateKeystore()
	{
		try
		{
			String algorithm = "RSA";
			keystore  = KeyStore.getInstance(KeyStore.getDefaultType());
			
			boolean generate = true;
	        			
			if(keystore_filename!=null && Files.exists(Paths.get(keystore_filename)))
			{
				System.out.println("Keystore " + keystore_filename + " loaded.");
				keystore.load(new FileInputStream(new File(keystore_filename)), keystore_password.toCharArray());
				generate = false;
			}
			else
			{
				System.out.println("Temporary keystore created.");
				keystore.load(null, keystore_password.toCharArray());
			}
	        
			if(generate)
			{				
		    	KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
		    	kpg.initialize(2048);
		    	KeyPair kp = kpg.generateKeyPair();
		    	
		    	Certificate certificate = TCP_Commons.generateCertificate(kp);
		    	Certificate[] certChain = new Certificate[1];
		    	certChain[0] = certificate;
		    	keystore.setKeyEntry("key1",kp.getPrivate(), key_password.toCharArray(), certChain);
		    	System.out.println("Keystore generated.");
		    	
		    	if(keystore_filename!=null)
		    	{
		    		FileOutputStream fos = new FileOutputStream(keystore_filename);
		    		keystore.store(fos,keystore_password.toCharArray());
		    		System.out.println("Key store " + keystore_filename + " saved.");
		    	}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
