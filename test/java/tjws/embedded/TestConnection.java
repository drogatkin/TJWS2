// Copyright (C)2018 by Rohtash Singh Lakra <rohtash.singh@gmail.com>.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/
//

// All enhancements Copyright (C)2018 by Rohtash Singh Lakra
// This version is compatible with JSDK 2.5
// http://tjws.sourceforge.net
package tjws.embedded;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.rslakra.logger.LogManager;

import Acme.IOHelper;

/**
 * How to Turn Off Certificate Validation in Java HTTPS Connections?
 * 
 * Avoiding these exceptions is possible by switching off the certificate
 * validation and host verification for SSL for the current Java virtual
 * machine. This can be done by replacing the default SSL trust manager and the
 * default SSL hostname verifier using this class.
 * 
 * Voilla! Now the code runs as expected: it downloads the resource from an
 * https address with invalid certificate.
 * 
 * Note: -
 * Be careful when using this hack! Skipping certificate validation is dangerous
 * and should be done in testing environments only.
 * 
 * @author Rohtash Singh Lakra
 * @date 03/19/2018 10:19:17 AM
 */
public final class TestConnection {
	
	private final String keyStoreFile = "conf/tjws.jks";
	private final String PASSWORD = "password";
	
	private final boolean sslEnabled;
	
	/**
	 * 
	 * @param sslEnabled
	 */
	public TestConnection(final boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}
	
	/**
	 * @param keyStoreStream
	 * @param passChars
	 * @return
	 */
	public static KeyStore initKeyStore(final InputStream keyStoreStream, final char[] passChars) {
		final String trustStoreType = KeyStore.getDefaultType();
		KeyStore keyStore = null;
		if (keyStoreStream == null) {
			throw new IllegalArgumentException("Invalid keyStoreStream:" + keyStoreStream);
		}
		
		if (passChars == null) {
			throw new IllegalArgumentException("Invalid passChars:" + String.valueOf(passChars));
		}
		
		try {
			keyStore = KeyStore.getInstance(trustStoreType);
			keyStore.load(keyStoreStream, passChars);
		} catch (java.security.cert.CertificateException ex) {
			ex.printStackTrace();
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (KeyStoreException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		return keyStore;
	}
	
	/**
	 * Create and initialize the SSLContext
	 *
	 * @param mContext
	 * @param keyStoreFile
	 * @param password
	 * @return
	 */
	public SSLContext createSSLContext(final String keyStoreFile, final String password) {
		SSLContext sslContext = null;
		try {
			final String keyStoreFilePath = IOHelper.pathString(IOHelper.pathString(TestConnection.class), keyStoreFile);
			final InputStream keyStoreStream = new FileInputStream(keyStoreFilePath);
			final KeyStore keyStore = initKeyStore(keyStoreStream, password.toCharArray());
			
			// Create key manager
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, password.toCharArray());
			final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
			
			// Create trust manager
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(keyStore);
			TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
			
			// Initialize SSLContext
			sslContext = SSLContext.getInstance("TLSv1");
			sslContext.init(keyManagers, trustManagers, new SecureRandom());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return sslContext;
	}
	
	/**
	 * Create a socket factory with certificate.
	 *
	 * @return
	 */
	public final SSLSocketFactory newSSLSocketFactory() {
		SSLSocketFactory sslSocketFactory = null;
		try {
			sslSocketFactory = createSSLContext(keyStoreFile, PASSWORD).getSocketFactory();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return sslSocketFactory;
	}
	
	/**
	 * Create a socket factory that trusts all certificates.
	 *
	 * @return
	 */
	public final SSLSocketFactory newSSLSocketFactoryWithTrustAllCerts() {
		SSLSocketFactory sslSocketFactory = null;
		try {
			final String keyStoreFilePath = IOHelper.pathString(IOHelper.pathString(TestConnection.class), keyStoreFile);
			final InputStream keyStoreStream = new FileInputStream(keyStoreFilePath);
			final KeyStore keyStore = initKeyStore(keyStoreStream, PASSWORD.toCharArray());
			// Create key manager
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, PASSWORD.toCharArray());
			final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
			
			final SSLContext sslContext = SSLContext.getInstance("TLSv1");
			sslContext.init(keyManagers, new TrustManager[] { new AllCertsTrustManager() }, new SecureRandom());
			sslSocketFactory = sslContext.getSocketFactory();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return sslSocketFactory;
	}
	
	/**
	 * Test the server connection.
	 */
	public void testSSLConnection() {
		SSLSocketFactory sslSocketFactory = null;
		BufferedReader bReader = null;
		try {
			HttpURLConnection urlConnection = null;
			if (sslEnabled) {
				urlConnection = (HttpsURLConnection) new URL("https://localhost:9161/").openConnection();
			} else {
				urlConnection = (HttpURLConnection) new URL("http://localhost:5161/").openConnection();
			}
			
			if (sslEnabled && (urlConnection instanceof HttpsURLConnection)) {
				// Create socket factory
				if (sslSocketFactory == null) {
					sslSocketFactory = newSSLSocketFactory();
				}
				
				// Install the SSL socket factory on the connection.
				((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslSocketFactory);
			}
			
			bReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String line;
			while ((line = bReader.readLine()) != null) {
				LogManager.debug(line);
			}
		} catch (Exception ex) {
			LogManager.error(ex);
		} finally {
			IOHelper.closeSilently(bReader);
		}
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TestConnection testConnection = new TestConnection(true);
		testConnection.testSSLConnection();
	}
	
}
