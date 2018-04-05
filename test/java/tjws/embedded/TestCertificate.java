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

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;

import com.rslakra.logger.LogManager;

import Acme.IOHelper;

/**
 * @author Rohtash Singh Lakra (Rohtash.Lakra@nasdaq.com)
 * @date 03/22/2018 01:26:54 PM
 */
public class TestCertificate {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			// add security provider
			IOHelper.addBouncyCastleProvider();
			
			String parentFolderPath = IOHelper.pathString(TestCertificate.class);
			LogManager.debug("parentFolderPath:" + parentFolderPath);
			final String keStoreFile = "newConf/tjws.bks";
			final String keyStoreFilePath = IOHelper.pathString(parentFolderPath, keStoreFile);
			LogManager.debug("keyStoreFilePath:" + keyStoreFilePath);
			final String KEY_TRUST_STORE = "javax.net.ssl.trustStore";
			System.setProperty(KEY_TRUST_STORE, keyStoreFilePath);
			LogManager.debug("truststore" + System.getProperty(KEY_TRUST_STORE));
			final Provider provide = Security.getProvider("BC");
			LogManager.debug(provide.getInfo() + " version:" + provide.getVersion());
			final String trustStoreType = KeyStore.getDefaultType();
			LogManager.debug("trustStoreType:" + trustStoreType);
			final KeyStore keyStore = KeyStore.getInstance("BKS");
			LogManager.debug("keyStore:" + keyStore);
			final InputStream keyStoreStream = new FileInputStream(keyStoreFilePath);
			keyStore.load(keyStoreStream, "password".toCharArray());
			LogManager.debug("Loaded!");
			
			// final String algorithm =
			// TrustManagerFactory.getDefaultAlgorithm();
			// TrustManagerFactory trustManagerFactory =
			// TrustManagerFactory.getInstance(algorithm, provide);
			// trustManagerFactory.init((KeyStore) null);
			// X509TrustManager xTrustManager = (X509TrustManager)
			// trustManagerFactory.getTrustManagers()[0];
			// for (X509Certificate xCertificate :
			// xTrustManager.getAcceptedIssuers()) {
			// String certStr = "S:" + xCertificate.getSubjectDN().getName() +
			// "\nI:" + xCertificate.getIssuerDN().getName();
			// System.out.println(certStr);
			// }
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
