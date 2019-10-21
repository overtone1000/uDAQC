package network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;
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
		File external_certificates_filename=new File(directory+"/external_certificate.txt");
		
		final String cert_preface="PublicCertificateDirectory=";
		final String key_preface="PrivateKeyDirectory=";
		
		if(external_certificates_filename.exists())
		{
			BufferedReader fis;
			try
			{
				fis = new BufferedReader(new FileReader(external_certificates_filename));
				String public_cert = fis.readLine();
				String private_key = fis.readLine();
				fis.close();
				
				public_cert=public_cert.substring(cert_preface.length(),public_cert.length());
				private_key=private_key.substring(key_preface.length(),private_key.length());
				
				System.out.println("Public certificate at " + Paths.get(public_cert).toAbsolutePath().toString());
				System.out.println("Private key at " + Paths.get(private_key).toAbsolutePath().toString());
				
				if(!Files.exists(Paths.get(public_cert)))
				{
					System.out.println("public certificate file not found");
				}
				else if(!Files.exists(Paths.get(private_key)))
				{
					System.out.println("private certificate file not found");
				}
				else
				{
					final String priv_key_head = "-----BEGIN PRIVATE KEY-----";
					final String priv_key_foot = "-----END PRIVATE KEY-----";
					final String pub_key_head = "-----BEGIN CERTIFICATE-----";
					final String pub_key_foot = "-----END CERTIFICATE-----";
							
					System.out.println("Loading externally maintained certificate.");
					//Ripped from https://gist.github.com/destan/b708d11bd4f403506d6d5bb5fe6a82c5
					//Ripped from https://gist.github.com/wsargent/26c0d478d0901d4b0a11eeb10dcbe0d1
					
					System.out.println("Reading files.");
					String privateKeyContent = new String(Files.readAllBytes(Paths.get(private_key)));
			        String publicKeyContent = new String(Files.readAllBytes(Paths.get(public_cert)));
			        
			        System.out.println("Removing headers and footers.");
			        privateKeyContent = privateKeyContent.replaceAll("\\n", "").replace(priv_key_head, "").replace(priv_key_foot, "");
			        
			        Vector<String> public_keys = new Vector<String>();
			        //System.out.println(publicKeyContent);
			        while(publicKeyContent.indexOf(pub_key_foot)>=0)
			        {
			        	String next = publicKeyContent.substring(0,publicKeyContent.indexOf(pub_key_foot));
			        	next = next.replaceAll("\\n", "").replace(pub_key_head, "").replace(pub_key_foot, "");
			        	System.out.println("Next cert:");
			        	System.out.println(next);
			        	public_keys.add(next);
			        	publicKeyContent=publicKeyContent.substring(publicKeyContent.indexOf(pub_key_foot)+pub_key_foot.length(),publicKeyContent.length());
			        }
			        
			        System.out.println("Creating RSA keystore.");
			        KeyFactory kf = KeyFactory.getInstance("RSA");
			        
			        System.out.println("Getting private key.");
			        PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
			        PrivateKey privKey = kf.generatePrivate(keySpecPKCS8);
			        
			        System.out.println("Private key:");
			        System.out.println(privKey.toString());
			        
			        System.out.println("Getting public certificates.");
			        CertificateFactory certfac = CertificateFactory.getInstance("X.509");
			        Vector<X509Certificate> certs = new Vector<X509Certificate>();
			        FileInputStream public_cert_is = new FileInputStream(public_cert);
			        List<X509Certificate> certificates = new ArrayList<>();
			        certificates = (List<X509Certificate>)certfac.generateCertificates(public_cert_is);
			        
			        for(X509Certificate cert:certificates)
			        {
			        	System.out.println("Public cert:");
				        System.out.println(cert.toString());
			        }
				}
		        
			} catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | CertificateException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			 
		}
		
		else
		{
			try
			{
				Files.createDirectories(Paths.get(directory));
				external_certificates_filename.createNewFile();
				PrintWriter fos = new PrintWriter(external_certificates_filename);
				fos.println(cert_preface);
				fos.println(key_preface);
				fos.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
