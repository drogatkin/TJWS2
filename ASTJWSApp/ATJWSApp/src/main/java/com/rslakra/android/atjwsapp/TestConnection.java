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
// Visit the https://github.com/rslakra/TJWS2 page for up-to-date versions of
// this and other fine Java utilities.
//
// All enhancements Copyright (C)2018 by Rohtash Singh Lakra
// This version is compatible with JSDK 2.5
// https://github.com/rslakra/TJWS2
package com.rslakra.android.atjwsapp;

import android.content.Context;
import android.os.Build;

import com.rslakra.android.logger.LogHelper;
import com.rslakra.android.server.TJWSServer;
import com.rslakra.android.framework.SSLHelper;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import Acme.IOHelper;

/**
 * How to Turn Off Certificate Validation in Java HTTPS Connections?
 * <p>
 * Avoiding these exceptions is possible by switching off the certificate
 * validation and host verification for SSL for the current Java virtual
 * machine. This can be done by replacing the default SSL trust manager and the
 * default SSL hostname verifier using this class.
 * <p>
 * Voilla! Now the code runs as expected: it downloads the resource from an
 * https address with invalid certificate.
 * <p>
 * Note: -
 * Be careful when using this hack! Skipping certificate validation is dangerous
 * and should be done in testing environments only.
 *
 * @author Rohtash Singh Lakra
 * @date 03/19/2018 10:19:17 AM
 */
public final class TestConnection {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "TestConnection";
    
    /**
     * mContext
     */
    private final Context mContext;
    private final boolean sslEnabled;
    
    /**
     * @param context
     * @param sslEnabled
     */
    public TestConnection(final Context context, final boolean sslEnabled) {
        this.mContext = context;
        this.sslEnabled = sslEnabled;
        IOHelper.addBouncyCastleProvider();
    }
    
    /**
     * @return
     * @throws Exception
     */
    private SSLContext makeSSLContext() throws Exception {
        // Load CAs from an InputStream
        final InputStream certStream = LogHelper.readAssets(mContext, "client.pem");
        // Create a KeyStore containing our trusted CAs
        final KeyStore trustStore = SSLHelper.loadPEMTrustStore(certStream);
        
        // Create a TrustManager that trusts the CAs in our KeyStore
        TrustManagerFactory trustManagerFactory = SSLHelper.initTrustManager(trustStore);
        
        // Create an SSLContext that uses our TrustManager
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        LogHelper.d(LOG_TAG, "sslContext - Protocol:" + sslContext.getProtocol() + ", Provider:" + sslContext.getProvider());
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        
        return sslContext;
    }
    
    /**
     * Create and initialize the SSLContext
     *
     * @param mContext
     * @param keyStoreFile
     * @param password
     * @return
     */
    public SSLContext createSSLContext(final Context mContext, final String keyStoreFile, final String password) {
        SSLContext sslContext = null;
        try {
            final InputStream keyStoreStream = LogHelper.readAssets(mContext, keyStoreFile);
            final KeyStore keyStore = SSLHelper.initKeyStore(keyStoreStream, password);
            
            // Create key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password.toCharArray());
            final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
            
            // Create trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            
            // Initialize SSLContext
            sslContext = SSLContext.getInstance(TJWSServer.PROTOCOLS[3]);
            //            sslContext = SSLContext.getInstance(TJWSServer.PROTOCOLS[2]);
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
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
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                sslSocketFactory = new TLSSocketFactory();
            } else {
                sslSocketFactory = createSSLContext(mContext, SSLHelper.CLEINT_KEY_STORE_FILE, SSLHelper.PASSWORD).getSocketFactory();
            }
            //            sslSocketFactodry = createSSLContext(mContext, keyStoreFile, PASSWORD).getSocketFactory();
        } catch(Exception ex) {
            LogHelper.d(LOG_TAG, ex);
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
            //            final InputStream keyStoreStream = LogHelper.readAssets(mContext, keyStoreFile);
            final InputStream keyStoreStream = LogHelper.readRAWResources(mContext, "tjws");
            final KeyStore keyStore = SSLHelper.initKeyStore(keyStoreStream, SSLHelper.PASSWORD);
            // Create key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, SSLHelper.PASSWORD.toCharArray());
            final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
            
            final SSLContext sslContext = SSLContext.getInstance(TJWSServer.PROTOCOLS[0]);
            sslContext.init(keyManagers, new TrustManager[]{new AllCertsTrustManager()}, new SecureRandom());
            sslSocketFactory = sslContext.getSocketFactory();
        } catch(Exception ex) {
            LogHelper.d(LOG_TAG, ex);
        }
        
        return sslSocketFactory;
    }
    
    /**
     * Test the server connection.
     */
    public void testSSLConnection() {
        SSLSocketFactory sslSocketFactory = null;
//        BufferedReader bReader = null;
        try {
            HttpURLConnection urlConnection = null;
            if(sslEnabled) {
                urlConnection = (HttpsURLConnection) new URL("https://localhost:9161/").openConnection();
            } else {
                urlConnection = (HttpURLConnection) new URL("http://localhost:5161/").openConnection();
            }
            
            if(sslEnabled && (urlConnection instanceof HttpsURLConnection)) {
                // Create socket factory
                if(sslSocketFactory == null) {
                    //                    sslSocketFactory = newSSLSocketFactory();
                    /*
                    sslSocketFactory = newSSLSocketFactoryWithTrustAllCerts();
                    */
                    /*
                    SSLContext sslContext = SSLContext.getInstance(TJWSServer.PROTOCOLS[0]);
                    sslContext.init(null, null, null);
                    sslSocketFactory = new NoSSLv3SocketFactory(sslContext.getSocketFactory());
                    sslSocketFactory = sslContext.getSocketFactory();
                    */
                    
                    /*
                    SSLContext sslContext = SSLContext.getInstance("SSLv3");
                    sslContext.init(null, null, null);
                    sslSocketFactory = new TLSSocketFactory();
                    */
                    
                    //                    SSLContext sslContext = null;
                    //                    final InputStream keyStoreStream = LogHelper.readAssets(mContext, SSLHelper.CLEINT_KEY_STORE_FILE);
                    //                    final KeyStore keyStore = SSLHelper.initKeyStore(keyStoreStream, SSLHelper.PASSWORD);
                    /* Only 1 will be out of comments from these 4.
                    
                    sslContext = SSLHelper.makeSSLContext(TJWSServer.PROTOCOLS[3], null, null, null);
                    sslContext = SSLHelper.makeSSLContext(TJWSServer.PROTOCOLS[3], keyStore, SSLHelper.PASSWORD.toCharArray(), true);
                    sslContext = SSLHelper.makeSSLContext(TJWSServer.PROTOCOLS[3], keyStore, null, false);
                    sslContext = SSLHelper.makeSSLContext(TJWSServer.PROTOCOLS[3], keyStore, SSLHelper.PASSWORD.toCharArray(), false);
                    */
                    //                    sslContext = SSLHelper.makeSSLContext(TJWSServer.PROTOCOLS[3], null, null, null);
                    //                    sslSocketFactory = new NoSSLv3SocketFactory(sslContext.getSocketFactory());
                    
                    //                    sslSocketFactory = sslContext.getSocketFactory();
                    sslSocketFactory = makeSSLContext().getSocketFactory();
                }
                
                //                final byte[] authBytes  = "admin:admin".getBytes("UTF-8");
                //                final byte[] authBytes  = PASSWORD.getBytes("UTF-8");
                //                final String authString  = Base64.encodeToString(authBytes, Base64.DEFAULT);
                //                urlConnection.setRequestProperty("Authorization", "Basic " + authString);
                
                // Install the SSL socket factory on the connection.
                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslSocketFactory);
            }
            
            urlConnection.connect();
            LogHelper.i(LOG_TAG, "ResponseCode:" + urlConnection.getResponseCode());
            LogHelper.d(LOG_TAG, "Response:" + SSLHelper.readStream(urlConnection.getInputStream(), true));
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        } finally {
//            IOHelper.safeClose(bReader);
        }
    }
    
    /**
     * Test the SSL server connection.
     */
    public void testSSLSocketConnection() {
        SSLSocketFactory sslSocketFactory = null;
        try {
            if(sslEnabled) {
                // Create socket factory
                //                sslSocketFactory = newSSLSocketFactory();
                //                sslSocketFactory = newSSLSocketFactoryWithTrustAllCerts();
                //                HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
                /*
                // Install the all-trusting host verifier
                HttpsURLConnection.setDefaultHostnameVerifier(new AllHostNameVerifier());
                */
                sslSocketFactory = makeSSLContext().getSocketFactory();
            }
            
            // Open SSLSocket
            final SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", 9161);
            sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
            
            // Start handshake
            sslSocket.startHandshake();
            
            /**
             * Verify that the certificate hostname is for [localhost],
             * This is due to lack of SNI support in the current SSLSocket.
             */
            final HostnameVerifier hostNameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
            final SSLSession sslSession = sslSocket.getSession();
            if(!hostNameVerifier.verify("localhost", sslSession)) {
                throw new SSLHandshakeException("Expected [localhost], found:" + sslSession.getPeerPrincipal());
            }
            
            SSLHelper.logServerCertificate(sslSocket);
            SSLHelper.logSocketInfo(sslSocket);
            
            final PrintWriter outWriter = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream()));
            
            // Send request to server.
            outWriter.println("html");
            outWriter.println();
            outWriter.flush();
            
            LogHelper.d(LOG_TAG, "Response:" + SSLHelper.readStream(sslSocket.getInputStream(), true));
            sslSocket.close();
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        }
    }
}



