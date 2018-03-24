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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import Acme.IOHelper;
import rslakra.logger.LogHelper;

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
	
	public TestConnection() {
		
	}
	
	/**
	 * Create a socket factory that trusts all certificates.
	 * 
	 * @param trustAllCerts
	 * @return
	 */
	public final SSLSocketFactory createSSLSocketFactory(final boolean trustAllCerts) {
		SSLSocketFactory sslSocketFactory = null;
		try {
			SSLContext sslContext = null;
			if (trustAllCerts) {
				X509TrustManager trustManager = new AllCertsTrustManager();
				sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, new TrustManager[] { trustManager }, new SecureRandom());
				sslSocketFactory = sslContext.getSocketFactory();
			}
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (KeyManagementException ex) {
			ex.printStackTrace();
		}
		
		return sslSocketFactory;
	}
	
	/**
	 * 
	 * @param trustAllCerts
	 * @param trustAllHosts
	 */
	public void installSSLSocketFactory(final boolean trustAllCerts, final boolean trustAllHosts) {
		if (trustAllCerts) {
			// Install the all-trusting trust manager
			SSLSocketFactory sslSocketFactory = createSSLSocketFactory(trustAllCerts);
			if (sslSocketFactory != null) {
				HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
			}
		}
		
		if (trustAllHosts) {
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(new AllHostNameVerifier());
		}
	}
	
	/**
	 * 
	 */
	public void testConnection() throws IOException {
		URL url = new URL("https://localhost:9161/");
		URLConnection urlConnection = url.openConnection();
		
		BufferedReader bReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
		String line;
		while ((line = bReader.readLine()) != null) {
			LogHelper.log(line);
		}
		IOHelper.safeClose(bReader);
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		TestConnection testConnection = new TestConnection();
		testConnection.installSSLSocketFactory(true, false);
		try {
			testConnection.testConnection();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
}
