/**
 *
 */
package com.rslakra.android.tjwsasapp;

import android.content.Context;

import com.rslakra.android.logger.LogHelper;
import com.rslakra.android.utils.SSLHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

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
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "TestConnection";
    
    private final String keyStoreFile = "newConf/tjws.bks";
    private final String PASSWORD = "password";
    
    /** mContext */
    private final Context mContext;
    private final boolean sslEnabled;
    
    /**
     * @param context
     * @param sslEnabled
     */
    public TestConnection(final Context context, final boolean sslEnabled) {
        this.mContext = context;
        this.sslEnabled = sslEnabled;
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
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        }
        
        return sslContext;
    }
    
    /**
     * Create a socket factory that trusts all certificates.
     *
     * @param trustAllCerts
     * @return
     */
    public final SSLSocketFactory newSSLSocketFactory(final boolean trustAllCerts) {
        SSLSocketFactory sslSocketFactory = null;
        try {
            final SSLContext sslContext;
            if(trustAllCerts) {
                final InputStream keyStoreStream = LogHelper.readAssets(mContext, keyStoreFile);
                final KeyStore keyStore = SSLHelper.initKeyStore(keyStoreStream, PASSWORD);
                // Create key manager
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, PASSWORD.toCharArray());
                final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
                
                sslContext = SSLContext.getInstance("TLSv1");
                sslContext.init(keyManagers, new TrustManager[]{new AllCertsTrustManager()}, new SecureRandom());
                sslSocketFactory = sslContext.getSocketFactory();
            } else {
                sslContext = createSSLContext(mContext, keyStoreFile, PASSWORD);
                sslSocketFactory = sslContext.getSocketFactory();
            }
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
        BufferedReader bReader = null;
        try {
            URL url = null;
            if(sslEnabled) {
                url = new URL("https://localhost:9161/");
            } else {
                url = new URL("http://localhost:5161/");
            }
            
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            if(sslEnabled && (urlConnection instanceof HttpsURLConnection)) {
                // Create socket factory
                if(sslSocketFactory == null) {
                    sslSocketFactory = newSSLSocketFactory(false);
                    
                    /*
                    SSLContext sslContext = SSLContext.getInstance("SSLv3");
                    sslContext.init(null, null, null);
                    sslSocketFactory = new TLSSocketFactory(sslContext.getSocketFactory());
                    */
                }
                
                if(urlConnection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslSocketFactory);
                }
            }
            
            bReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while((line = bReader.readLine()) != null) {
                LogHelper.i(LOG_TAG, line);
            }
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        } finally {
            IOHelper.safeClose(bReader);
        }
    }
    
    /**
     * Test the server connection.
     */
    public void testSSLSocketConnection() {
        SSLSocketFactory sslSocketFactory = null;
        try {
            if(sslEnabled) {
                // Create socket factory
                sslSocketFactory = newSSLSocketFactory(true);
                HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
                /*
                // Install the all-trusting host verifier
                HttpsURLConnection.setDefaultHostnameVerifier(new AllHostNameVerifier());
                */
            }
            
            // Create socket
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", 9161);
            LogHelper.d(LOG_TAG, "SSL client started");
            new ClientThread(sslSocket).start();
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        }
    }
    
    // Thread handling the socket to server
    private class ClientThread extends Thread {
        
        private SSLSocket sslSocket = null;
        
        ClientThread(SSLSocket sslSocket) {
            this.sslSocket = sslSocket;
        }
        
        public void run() {
            sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
            
            try {
                // Start handshake
                sslSocket.startHandshake();
                
                // Get session after the connection is established
                SSLSession sslSession = sslSocket.getSession();
                LogHelper.d(LOG_TAG, "SSLSession :");
                LogHelper.d(LOG_TAG, "Protocol : " + sslSession.getProtocol());
                LogHelper.d(LOG_TAG, "Cipher suite : " + sslSession.getCipherSuite());
                
                // Start handling application content
                InputStream inputStream = sslSocket.getInputStream();
                OutputStream outputStream = sslSocket.getOutputStream();
                
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream));
                
                // Write data
                printWriter.println("Hello Server");
                printWriter.println();
                printWriter.flush();
                
                String line = null;
                while((line = bufferedReader.readLine()) != null) {
                    LogHelper.d(LOG_TAG, line);
                    if(line.trim().equals("HTTP/1.1 200\r\n")) {
                        break;
                    }
                }
                
                IOHelper.safeClose(sslSocket);
            } catch(Exception ex) {
                LogHelper.e(LOG_TAG, ex);
            }
        }
    }
    
}



