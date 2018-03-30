package com.rslakra.android.utils;

import android.content.Context;
import android.util.Log;

import com.rslakra.android.logger.LogHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import Acme.IOHelper;

/**
 * @Author: Rohtash Singh Lakra
 * @Created: 2018/03/22 3:51 PM
 */
public final class SSLHelper {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "SSLHelper";
    
    /** SSL_SUPPORTED_PROTOCOLS */
    private static final String[] SSL_SUPPORTED_PROTOCOLS = new String[]{"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"};
    
    
    /** Singleton object */
    private SSLHelper() {
        throw new UnsupportedOperationException("Object creation not allowed for this class.");
    }
    
    /**
     * @param strings
     * @return
     */
    public static String toString(final String[] strings, final boolean newLine) {
        final StringBuilder sBuilder = new StringBuilder();
        if(strings != null) {
            sBuilder.append("\n");
            for(String string : strings) {
                sBuilder.append(string).append("\n");
            }
        }
        
        return sBuilder.toString();
    }
    
    /**
     * @param strings
     * @return
     */
    public static String toString(final String[] strings) {
        return toString(strings, false);
    }
    
    /**
     * Returns the SSL supported protocols.
     *
     * @return
     */
    public static String[] getSSLSupportedProtocols() {
        return SSL_SUPPORTED_PROTOCOLS;
    }
    
    /**
     * @param context
     * @param fileName
     * @return
     */
    public static InputStream rawResource(final Context context, final String fileName) {
        return context.getResources().openRawResource(context.getResources().getIdentifier(fileName, "raw", context.getPackageName()));
    }
    
    /**
     * Converts the exception into IOException.
     *
     * @param exception
     * @return
     */
    public static IOException makeIOException(Exception exception) {
        return new IOException(exception);
    }
    
    /**
     * Creates and returns the <code>TrustManagerFactory</code> that trusts the CAs in the given
     * <code>KeyStore</code>.
     *
     * @param loadedKeyStore
     * @return
     * @throws IOException
     */
    public static TrustManagerFactory newTrustManagerFactory(KeyStore loadedKeyStore) throws Exception {
        TrustManagerFactory trustManagerFactory = null;
        if(loadedKeyStore != null) {
            // Create a TrustManager that trusts the CAs in our KeyStore
            String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            trustManagerFactory = TrustManagerFactory.getInstance(defaultAlgorithm);
            trustManagerFactory.init(loadedKeyStore);
        }
        
        return trustManagerFactory;
    }
    
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and an array of loaded
     * KeyManagers. These objects must properly loaded/initialized by the caller.
     *
     * @param loadedKeyStore
     * @param keyManagers
     * @return
     * @throws IOException
     */
    public static SSLServerSocketFactory makeSSLSocketFactory(KeyStore loadedKeyStore, KeyManager[] keyManagers) throws IOException {
        SSLServerSocketFactory serverSocketFactory;
        try {
            // Create a TrustManager that trusts the CAs in our KeyStore
            TrustManagerFactory trustManagerFactory = newTrustManagerFactory(loadedKeyStore);
            
            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagerFactory.getTrustManagers(), null);
            serverSocketFactory = sslContext.getServerSocketFactory();
        } catch(Exception ex) {
            throw makeIOException(ex);
        }
        
        return serverSocketFactory;
    }
    
    
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and a loaded KeyManagerFactory.
     * These objects must properly loaded/initialized by the caller.
     *
     * @param loadedKeyStore
     * @param loadedKeyFactory
     * @return
     * @throws IOException
     */
    public static SSLServerSocketFactory makeSSLSocketFactory(KeyStore loadedKeyStore, KeyManagerFactory loadedKeyFactory) throws IOException {
        try {
            return makeSSLSocketFactory(loadedKeyStore, loadedKeyFactory.getKeyManagers());
        } catch(Exception ex) {
            throw makeIOException(ex);
        }
    }
    
    /**
     * @param keyStoreStream
     * @param passChars
     * @return
     */
    public static KeyStore initKeyStore(final InputStream keyStoreStream, final char[] passChars) {
        KeyStore keyStore = null;
        if(keyStoreStream == null) {
            throw new IllegalArgumentException("Invalid keyStoreStream:" + keyStoreStream);
        }
        
        if(passChars == null) {
            throw new IllegalArgumentException("Invalid passChars:" + passChars);
        }
        
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            LogHelper.d(LOG_TAG, "trustStoreType:" + keyStore.getType());
            keyStore.load(keyStoreStream, passChars);
        } catch(NoSuchAlgorithmException ex) {
            LogHelper.e(LOG_TAG, ex);
        } catch(CertificateException ex) {
            LogHelper.e(LOG_TAG, ex);
        } catch(KeyStoreException ex) {
            LogHelper.e(LOG_TAG, ex);
        } catch(IOException ex) {
            LogHelper.e(LOG_TAG, ex);
        }
        
        return keyStore;
    }
    
    /**
     * @param keyStoreStream
     * @param keyStorePassword
     * @return
     */
    public static KeyStore initKeyStore(final InputStream keyStoreStream, final String keyStorePassword) {
        return initKeyStore(keyStoreStream, keyStorePassword.toCharArray());
    }
    
    
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a KeyStore resource with your certificate and
     * passphrase
     *
     * @param keyAndTrustStoreFilePath
     * @param passphrase
     * @return
     * @throws IOException
     */
    public static SSLServerSocketFactory makeSSLSocketFactory(String keyAndTrustStoreFilePath, char[] passphrase) throws IOException {
        try {
            KeyStore keystore = initKeyStore(new FileInputStream(keyAndTrustStoreFilePath), passphrase);
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, passphrase);
            return makeSSLSocketFactory(keystore, keyManagerFactory);
        } catch(Exception ex) {
            throw makeIOException(ex);
        }
    }
    
    /**
     * Creates a SSLSocketFactory for HTTPS connection with the given '.crt' (certificate) file.
     *
     * @param certInputStream
     * @return
     * @throws IOException
     */
    public static SSLServerSocketFactory makeSSLSocketFactory(final InputStream certInputStream) throws IOException {
        try {
            // Load CAs from an InputStream
            CertificateFactory mCertFactory = CertificateFactory.getInstance("X.509");
            InputStream caInputStream = new BufferedInputStream(certInputStream);
            Certificate mCertificate;
            try {
                mCertificate = mCertFactory.generateCertificate(caInputStream);
                Log.d(LOG_TAG, "CA=" + ((X509Certificate) mCertificate).getSubjectDN());
            } finally {
                IOHelper.safeClose(caInputStream);
            }
            
            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", mCertificate);
            
            // Create a TrustManager that trusts the CAs in our KeyStore
            TrustManagerFactory trustManagerFactory = newTrustManagerFactory(keyStore);
            
            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext.getServerSocketFactory();
        } catch(Exception ex) {
            throw makeIOException(ex);
        }
    }
    
    /**
     * The 'javax.net.ssl.trustStore' to be set with the 'JKS' file path.
     *
     * @param mFile
     */
    public static void setSSLTrustStore(final File mFile) {
        System.setProperty("javax.net.ssl.trustStore", mFile.getAbsolutePath());
    }
    
    /**
     * The 'javax.net.ssl.trustStore' to be set with the 'JKS' file path.
     *
     * @param jksFilePath
     */
    public static void setSSLTrustStore(final String jksFilePath) {
        setSSLTrustStore(new File(jksFilePath));
    }
    
    /**
     * Returns true if the <cod>hostName</cod> contains any of the <code>allowedhostNames</code>
     * otherwise false.
     *
     * @param hostName
     * @param allowedHostNames
     * @return
     */
    public static boolean isAllowedHostname(final String hostName, final String... allowedHostNames) {
        if(hostName != null && allowedHostNames != null) {
            for(String host : allowedHostNames) {
                if(hostName.contains(host)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * It must be called before setting the SSL factory. The <code>hostName</code> is real host
     * name, which handles the SSL requests. If the <code>hostName</code> is null, by default
     * 'localhost' is used.
     *
     * @param hostName
     * @param allowedHostNames
     */
    public static void setDefaultHostnameVerifier(final String hostName, final String... allowedHostNames) {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            
            /**
             * Verify the SSL host name.
             * @param hostname
             * @param sslSession
             * @return
             */
            public boolean verify(String hostname, SSLSession sslSession) {
                if(hostname == null || hostname.trim().length() == 0) {
                    hostname = hostName;
                }
                
                //                HostnameVerifier hostNameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
                //                if(hostNameVerifier != null) {
                //                    return hostNameVerifier.verify(hostname, sslSession);
                //                }
                
                if(hostname.equals("localhost")) {
                    return true;
                } else if(hostname.equals(hostName)) {
                    return true;
                } else if(isAllowedHostname(hostname, allowedHostNames)) {
                    return true;
                }
                
                return false;
            }
        });
    }
    
    
    /**
     * Returns true if the key store is supported otherwise false.
     *
     * @param keyStoreStream
     * @param keyStorePassword
     * @return
     */
    public static boolean isKeyStoreSupported(final InputStream keyStoreStream, final String keyStorePassword) {
        /*
        int keyStoreVersion;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            keyStoreVersion = -1; //The BKS file
        } else {
            keyStoreVersion = -1; //The BKS (v-1) file
        }
        */
        
        return (initKeyStore(keyStoreStream, keyStorePassword) != null);
    }
}
