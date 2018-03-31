/**
 *
 */
package com.rslakra.android.tjwsasapp;

import android.content.Context;
import android.os.Build;

import com.rslakra.android.logger.LogHelper;
import com.rslakra.android.server.TJWSServer;
import com.rslakra.android.utils.SSLHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
    
    //    private final String keyStoreFile = "conf/tjws.bks";
    //    private final String keyStoreFile = "tjws.keystore";
    private final String keyStoreFile = "clienttruststore.bks";
    private final String PASSWORD = "password";
    
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
            sslContext = SSLContext.getInstance(TJWSServer.PROTOCOLS[0]);
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
                sslSocketFactory = createSSLContext(mContext, keyStoreFile, PASSWORD).getSocketFactory();
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
            final KeyStore keyStore = SSLHelper.initKeyStore(keyStoreStream, PASSWORD);
            // Create key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, PASSWORD.toCharArray());
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
        BufferedReader bReader = null;
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
                    sslSocketFactory = newSSLSocketFactory();
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
                }
                
                //                final byte[] authBytes  = "admin:admin".getBytes("UTF-8");
                //                final byte[] authBytes  = PASSWORD.getBytes("UTF-8");
                //                final String authString  = Base64.encodeToString(authBytes, Base64.DEFAULT);
                //                urlConnection.setRequestProperty("Authorization", "Basic " + authString);
                
                // Install the SSL socket factory on the connection.
                ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslSocketFactory);
                //                ((HttpsURLConnection) urlConnection).setHostnameVerifier(new AllHostNameVerifier());
                
                //                HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
                // Install the all-trusting host verifier
                //                HttpsURLConnection.setDefaultHostnameVerifier(new AllHostNameVerifier());
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
     * Test the SSL server connection.
     */
    public void testSSLSocketConnection() {
        SSLSocketFactory sslSocketFactory = null;
        try {
            if(sslEnabled) {
                // Create socket factory
                sslSocketFactory = newSSLSocketFactory();
                //                sslSocketFactory = newSSLSocketFactoryWithTrustAllCerts();
                //                HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);
                /*
                // Install the all-trusting host verifier
                HttpsURLConnection.setDefaultHostnameVerifier(new AllHostNameVerifier());
                */
            }
            
            // Create socket
            final SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", 9161);
            LogHelper.d(LOG_TAG, "SSL client started");
            new SSLSocketThread(sslSocket).start();
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        }
    }
    
    /**
     * Thread handling the socket to server.
     */
    private final class SSLSocketThread extends Thread {
        
        /**
         * mSSLSocket
         */
        private SSLSocket mSSLSocket = null;
        
        SSLSocketThread(final SSLSocket sslSocket) {
            this.mSSLSocket = sslSocket;
        }
        
        public void run() {
            try {
                LogHelper.d(LOG_TAG, "isBound:" + mSSLSocket.isBound());
                LogHelper.d(LOG_TAG, "isClosed:" + mSSLSocket.isClosed());
                LogHelper.d(LOG_TAG, "isConnected:" + mSSLSocket.isConnected());
                LogHelper.d(LOG_TAG, "supportedCipherSuites:" + LogHelper.toString(mSSLSocket.getSupportedCipherSuites(), true));
                LogHelper.d(LOG_TAG, "supportedProtocols:" + LogHelper.toString(mSSLSocket.getSupportedProtocols(), true));
                mSSLSocket.setEnabledCipherSuites(mSSLSocket.getSupportedCipherSuites());
                
                // Start handshake
                mSSLSocket.startHandshake();
                
                // Get session after the connection is established
                SSLSession sslSession = mSSLSocket.getSession();
                LogHelper.d(LOG_TAG, "SSLSession :");
                LogHelper.d(LOG_TAG, "Protocol : " + sslSession.getProtocol());
                LogHelper.d(LOG_TAG, "Cipher suite : " + sslSession.getCipherSuite());
                
                // Start handling application content
                InputStream inputStream = mSSLSocket.getInputStream();
                
                // Write data (Send Request to server).
                OutputStream outputStream = mSSLSocket.getOutputStream();
                outputStream.write("GET / HTTP/1.1\r\nHost: localhost/\r\n\r\n".getBytes());
                outputStream.flush();
                
                final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                while((line = bufferedReader.readLine()) != null) {
                    LogHelper.d(LOG_TAG, line);
                    if(line.trim().equals("HTTP/1.1 200\r\n")) {
                        break;
                    }
                }
                
                IOHelper.safeClose(mSSLSocket);
            } catch(Exception ex) {
                LogHelper.e(LOG_TAG, ex);
            }
        }
    }
    
}



