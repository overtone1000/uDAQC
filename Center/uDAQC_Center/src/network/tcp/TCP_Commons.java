package network.tcp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.Certificate;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

public class TCP_Commons
{

	private static TrustManager[] trustAllCerts()
	{
		TrustManager[] retval = new TrustManager[] 
		{
			new X509TrustManager() 
			{
	            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException
				{
					// TODO Auto-generated method stub
					
				}
				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException
				{
					// TODO Auto-generated method stub
					
				}
			}
        };
		return retval;
    }
	private static Certificate generateCertificate(KeyPair keyPair, String algorithm)
	{
		X500NameBuilder builder = new X500NameBuilder();
		builder.addRDN(BCStyle.CN, "Tyler Moore");
		builder.addRDN(BCStyle.O, "Medical Holographics");
		builder.addRDN(BCStyle.ST, "Oregon");
		builder.addRDN(BCStyle.C, "US");
		X500Name issuer = builder.build();
		
		java.math.BigInteger serial = new java.math.BigInteger(64, new SecureRandom());
		java.util.Date notBeforeDate=new java.util.Date();
		java.util.Date notAfter=new java.util.Date(1000*60*24*365*100);
		SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
		
		X509v3CertificateBuilder cert = new X509v3CertificateBuilder(issuer,serial,notBeforeDate,notAfter,issuer,publicKeyInfo);
		
		AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1WithRSAEncryption");
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        
        AsymmetricKeyParameter privateKeyAsymKeyParam;
        try
		{
			privateKeyAsymKeyParam = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		ContentSigner sigGen;
		try
		{
			sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(privateKeyAsymKeyParam);
		} catch (OperatorCreationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		X509CertificateHolder certificateHolder = cert.build(sigGen);
		
		CertificateFactory certFactory;
		try
		{
			certFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		Certificate retval;
		try
		{
			retval = certFactory.generateCertificate(new ByteArrayInputStream(certificateHolder.getEncoded()));
		} catch (CertificateException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		return retval;
	}
	private static void AddSSLFilter(DefaultIoFilterChainBuilder filter_chain, boolean is_client)
	{
		try
		{
			/*
            Possible values for protocol
			    SSL
			    SSLv2
			    SSLv3
			    TLS
			    TLSv1
			    TLSv1.1
			    TLSv1.2 (not supported in Java 6)
            */
			
			/* 
			 * keytool commands:
			 * 
			 * navigate to directory where files are wanted (see file names below)
			 * 
			 * generate keypair and save in a keystore
			 * keytool -genkeypair -alias "trmoore" -keyalg RSA -validity 99999 -keystore keystore.jks
			 * 
			 * export a certificate signed using that keypair
			 * keytool -export -alias "trmoore" -keystore keystore.jks -rfc -file 0.cer
			 * 
			 * import that certificate into a new truststore
			 * keytool -import -alias "trmoore" -file 0.cer -keystore truststore.ts
			 * 
			 * in the end, just did away with the use of certificates by:
			 * 1. making a trustmanager that ignores the certificate
			 * 2. programmatically generated the keystore
			 * 
			 * comments left for posterity
			 */
			
			//SslContextFactory sslContextFactory = new SslContextFactory();
            //sslContextFactory.setProtocol("TLS");
                                    
            //File keyStoreFile = new File("keystore/keystore.jks");
        	//File trustStoreFile = new File("keystore/truststore.ts");
        	String pw = "trmtrm";
        	String algorithm = "RSA";
        	//String ks_type = "RSA";
        	
        	/*
            final KeyStoreFactory trustStoreFactory = new KeyStoreFactory();
            trustStoreFactory.setDataFile(trustStoreFile);
            trustStoreFactory.setPassword(pw);
            
            final KeyStore trustStore = trustStoreFactory.newInstance();
                        
            sslContextFactory.setTrustManagerFactoryKeyStore(trustStore);
            sslContextFactory.setKeyManagerFactoryKeyStorePassword(pw);
            */
        	
        	KeyManager[] kms = null;
        	
            if(!is_client)
            {
            	KeyStore ks  = KeyStore.getInstance(KeyStore.getDefaultType());
                
            	ks.load(null,null);
            	//ks.load(new FileInputStream(keyStoreFile), pw.toCharArray());
                
            	KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
            	kpg.initialize(2048);
            	KeyPair kp = kpg.generateKeyPair();
            	
            	Certificate certificate = generateCertificate(kp,algorithm);
            	Certificate[] certChain = new Certificate[1];
            	certChain[0] = certificate;
            	ks.setKeyEntry("key1",kp.getPrivate(), pw.toCharArray(), certChain);
            	
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, pw.toCharArray());
                kms = kmf.getKeyManagers();
            }
                                                
            //SSLContext sslContext = sslContextFactory.newInstance();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kms, trustAllCerts(), null);
                        
            SslFilter sslFilter = new SslFilter(sslContext);
            sslFilter.setUseClientMode(is_client);
            
            sslFilter.setEnabledCipherSuites(sslContext.getDefaultSSLParameters().getCipherSuites());
            sslFilter.setEnabledProtocols(sslContext.getDefaultSSLParameters().getProtocols());
            
            sslFilter.setNeedClientAuth(false);
            sslFilter.setWantClientAuth(false);
            
        	filter_chain.addFirst("sslFilter", sslFilter);
		}
		catch(Exception e)
		{
			System.out.println("Error during SSL Filter addition.");
			System.out.println(e.getMessage());
		}
	}
	public static void AddSSLFilter(IoAcceptor acceptor)
	{
		AddSSLFilter(acceptor.getFilterChain(),false);
	}
	public static void AddSSLFilter(NioSocketConnector connector)
	{
		AddSSLFilter(connector.getFilterChain(),true);
	}
	public static void CloseSession(IoSession session)
	{
		try{
			if(session!=null){
				session.close(false);
				if(session.isClosing()){
					session.getCloseFuture().awaitUninterruptibly();
					System.out.println("Session closed.");
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
