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
package com.rslakra.android.framework;

import android.net.http.SslCertificate;
import android.os.Bundle;
import android.util.Base64;

import com.rslakra.android.logger.LogHelper;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * @Author: Rohtash Singh Lakra
 * @Created: 2018/03/22 3:51 PM
 */
public final class SSLHelper {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "SSLHelper";
    
    /* TODO - MOVE THESE TO RESPECTIVE LOCATION, AFTER IT WORKS. */
    public static final String PASSWORD = "password";
    public static final String SERVER_KEY_STORE_FILE = "raw/client.bks";
    public static final String CLEINT_KEY_STORE_FILE = "client.pem";
    
    
    /**
     * SSL_SUPPORTED_PROTOCOLS
     */
    private static final String[] SSL_SUPPORTED_PROTOCOLS = new String[]{"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"};
    
    
    /**
     * Singleton object
     */
    private SSLHelper() {
        throw new UnsupportedOperationException("Object creation not allowed for this class.");
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
     * Converts the exception into IOException.
     *
     * @param exception
     * @return
     */
    public static IOException makeIOException(final Exception exception) {
        return new IOException(exception);
    }
    
    /**
     * Closes the given <code>mCloseable</code> object.
     *
     * @param mCloseable
     * @param nullify
     */
    public static final void closeSilently(Object mCloseable, final boolean nullify) {
        try {
            if(mCloseable != null) {
                if(mCloseable instanceof Closeable) {
                    ((Closeable) mCloseable).close();
                } else if(mCloseable instanceof Socket) {
                    ((Socket) mCloseable).close();
                } else if(mCloseable instanceof ServerSocket) {
                    ((ServerSocket) mCloseable).close();
                } else {
                    throw new IllegalArgumentException("mCloseable is not an instance of closeable object!");
                }
            }
        } catch(IOException ex) {
            LogHelper.d(LOG_TAG, ex);
        } finally {
            if(nullify) {
                mCloseable = null;
            }
        }
    }
    
    /**
     * Closes the given <code>mCloseable</code> object.
     *
     * @param mCloseable
     */
    public static final void closeSilently(Object mCloseable) {
        closeSilently(mCloseable, false);
    }
    
    /**
     * @param keyStoreStream
     * @param passChars
     * @param excludeParams
     * @return
     */
    public static KeyStore initKeyStore(final InputStream keyStoreStream, final char[] passChars, final boolean excludeParams) {
        LogHelper.d(LOG_TAG, "+initKeyStore(" + keyStoreStream + ", " + passChars + ")");
        
        KeyStore keyStore = null;
        if(!excludeParams) {
            if(LogHelper.isNull(keyStoreStream)) {
                throw new IllegalArgumentException("Invalid keyStoreStream:" + keyStoreStream);
            }
            
            if(LogHelper.isNull(passChars)) {
                throw new IllegalArgumentException("Invalid passChars:" + passChars);
            }
        }
        
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            LogHelper.d(LOG_TAG, "trustStoreType:" + keyStore.getType());
            if(excludeParams) {
                keyStore.load(null, null);
            } else {
                keyStore.load(keyStoreStream, passChars);
            }
        } catch(NoSuchAlgorithmException ex) {
            LogHelper.e(LOG_TAG, ex);
        } catch(CertificateException ex) {
            LogHelper.e(LOG_TAG, ex);
        } catch(KeyStoreException ex) {
            LogHelper.e(LOG_TAG, ex);
        } catch(IOException ex) {
            LogHelper.e(LOG_TAG, ex);
        }
        
        LogHelper.d(LOG_TAG, "-initKeyStore(), keyStore:" + keyStore);
        return keyStore;
    }
    
    /**
     * @param keyStoreStream
     * @param keyStorePassword
     * @return
     */
    public static KeyStore initKeyStore(final InputStream keyStoreStream, final String keyStorePassword) {
        return initKeyStore(keyStoreStream, keyStorePassword.toCharArray(), false);
    }
    
    
    /**
     * Reads and decodes a base-64 encoded DER certificate (a .pem certificate), typically the server's CA cert.
     *
     * @param certificateStream an InputStream from which to read the cert
     * @return a byte[] containing the decoded certificate
     * @throws IOException
     */
    public static byte[] loadPEMCertificate(final InputStream certificateStream) throws IOException {
        byte[] certBytes = null;
        BufferedReader bReader = null;
        try {
            final StringBuilder pemBuilder = new StringBuilder();
            bReader = new BufferedReader(new InputStreamReader(certificateStream));
            String line = bReader.readLine();
            while(line != null) {
                if(!line.startsWith("--")) {
                    pemBuilder.append(line);
                }
                line = bReader.readLine();
            }
            certBytes = Base64.decode(pemBuilder.toString(), Base64.DEFAULT);
        } finally {
            closeSilently(bReader);
        }
        
        return certBytes;
    }
    
    /**
     * Converts the <code>certBytes</code> into <code>Certificate</code> object.
     *
     * @param certBytes
     * @return
     */
    public static Certificate toCertificate(final byte[] certBytes) {
        Certificate mCertificate = null;
        if(LogHelper.isNotNull(certBytes)) {
            try {
                final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                mCertificate = certificateFactory.generateCertificate(new ByteArrayInputStream(certBytes));
            } catch(CertificateException ex) {
                LogHelper.e(LOG_TAG, ex);
            }
        }
        
        return mCertificate;
    }
    
    /**
     * Produces a KeyStore from a String containing a PEM certificate (typically, the server's CA certificate)
     *
     * @param keyStoreStream A String containing the PEM-encoded certificate
     * @return a KeyStore (to be used as a trust store) that contains the certificate
     * @throws Exception
     */
    public static KeyStore loadPEMTrustStore(final InputStream keyStoreStream) throws Exception {
        byte[] certBytes = loadPEMCertificate(keyStoreStream);
        X509Certificate x509Certificate = (X509Certificate) toCertificate(certBytes);
        LogHelper.d(LOG_TAG, "ca=" + x509Certificate.getSubjectDN());
        final String alias = x509Certificate.getSubjectX500Principal().getName();
        
        final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        trustStore.setCertificateEntry(alias, x509Certificate);
        
        return trustStore;
    }
    
    /**
     * Converts the <code>SslCertificate</code> to <code>Certificate</code> object.
     *
     * @param sslCertificate
     * @return
     */
    public static Certificate toCertificate(final SslCertificate sslCertificate) {
        final Bundle bundle = sslCertificate.saveState(sslCertificate);
        byte[] certBytes = bundle.getByteArray("x509-certificate");
        return toCertificate(certBytes);
    }
    
    /**
     * Creates and returns the <code>KeyManagerFactory</code> that trusts the CAs in the given
     * <code>loadedKeyStore</code>.
     *
     * @param loadedKeyStore
     * @param password
     * @return
     * @throws Exception
     */
    public static KeyManagerFactory initKeyManager(final KeyStore loadedKeyStore, final char[] password) throws Exception {
        LogHelper.d(LOG_TAG, "+initKeyManager(" + loadedKeyStore + ", " + loadedKeyStore + ", " + password + ")");
        // Create a KeyStoreManager that trusts the CAs in our KeyStore
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        LogHelper.d(LOG_TAG, "keyManagerType:" + keyManagerFactory.getAlgorithm());
        if(LogHelper.isNull(loadedKeyStore)) {
            keyManagerFactory.init(null, null);
        } else {
            keyManagerFactory.init(loadedKeyStore, password);
        }
        
        LogHelper.d(LOG_TAG, "-initKeyManager(), keyManagerFactory:" + keyManagerFactory);
        return keyManagerFactory;
    }
    
    /**
     * Creates and returns the <code>TrustManagerFactory</code> that trusts the CAs in the given
     * <code>keyStore</code>.
     *
     * @param keyStore
     * @return
     * @throws Exception
     */
    public static TrustManagerFactory initTrustManager(final KeyStore keyStore) throws Exception {
        LogHelper.d(LOG_TAG, "+initTrustManager(" + keyStore + ")");
        // Create a TrustManager that trusts the CAs in our KeyStore
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        LogHelper.d(LOG_TAG, "trustManagerType:" + trustManagerFactory.getAlgorithm());
        trustManagerFactory.init(keyStore);
        
        LogHelper.d(LOG_TAG, "-initTrustManager(), trustManagerFactory:" + trustManagerFactory);
        return trustManagerFactory;
    }
    
    
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and an array of loaded
     * KeyManagers. These objects must properly loaded/initialized by the caller.
     *
     * @param protocol
     * @param keyManagers
     * @param trustManagers
     * @param secureRandom
     * @return
     * @throws Exception
     */
    public static SSLContext makeSSLContext(final String protocol, final KeyManager[] keyManagers, final TrustManager[] trustManagers, final SecureRandom secureRandom) throws Exception {
        LogHelper.d(LOG_TAG, "+makeSSLContext(" + protocol + ", " + keyManagers + ", " + trustManagers + ", " + secureRandom + ")");
        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext = null;
        final boolean useDefaultContext = false;
        if(useDefaultContext) {
            sslContext = SSLContext.getInstance("Default");
        } else {
            if(LogHelper.isNullOrEmpty(protocol)) {
                sslContext = SSLContext.getDefault();
            } else {
                sslContext = SSLContext.getInstance(protocol);
            }
            
            if(LogHelper.isNotNull(keyManagers) && LogHelper.isNotNull(trustManagers)) {
                sslContext.init(keyManagers, trustManagers, secureRandom);
            } else if(LogHelper.isNotNull(keyManagers) && LogHelper.isNull(trustManagers)) {
                sslContext.init(keyManagers, null, secureRandom);
            } else if(LogHelper.isNull(keyManagers) && LogHelper.isNotNull(trustManagers)) {
                sslContext.init(null, trustManagers, secureRandom);
            } else {
                sslContext.init(null, null, secureRandom);
            }
        }
        
        LogHelper.d(LOG_TAG, "-makeSSLContext(), sslContext:" + sslContext);
        return sslContext;
    }
    
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and an array of loaded
     * KeyManagers. These objects must properly loaded/initialized by the caller.
     *
     * @param protocol
     * @param keyManagers
     * @param trustManagers
     * @return
     * @throws Exception
     */
    public static SSLContext makeSSLContext(final String protocol, final KeyManager[] keyManagers, final TrustManager[] trustManagers) throws Exception {
        return makeSSLContext(protocol, keyManagers, trustManagers, null);
    }
    
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and an array of loaded
     * KeyManagers. These objects must properly loaded/initialized by the caller.
     *
     * @param keyManagers
     * @param trustManagers
     * @param secureRandom
     * @return
     * @throws Exception
     */
    public static SSLContext makeSSLContext(final KeyManager[] keyManagers, final TrustManager[] trustManagers, final SecureRandom secureRandom) throws Exception {
        return makeSSLContext("TLS", keyManagers, trustManagers, secureRandom);
    }
    
    
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and an array of loaded
     * KeyManagers. These objects must properly loaded/initialized by the caller.
     *
     * @param keyManagers
     * @param trustManagers
     * @return
     * @throws Exception
     */
    public static SSLContext makeSSLContext(final KeyManager[] keyManagers, final TrustManager[] trustManagers) throws Exception {
        return makeSSLContext(keyManagers, trustManagers, null);
    }
    
    
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a loaded KeyStore and a loaded KeyManagerFactory.
     * These objects must properly loaded/initialized by the caller.
     *
     * @param protocol
     * @param loadedKeyStore
     * @param passphrase
     * @param excludeTrustManager
     * @return
     * @throws Exception
     */
    public static SSLContext makeSSLContext(final String protocol, final KeyStore loadedKeyStore, final char[] passphrase, final boolean excludeTrustManager) throws Exception {
        if(LogHelper.isNotNull(loadedKeyStore) && LogHelper.isNotNull(passphrase) && !excludeTrustManager) {
            final KeyManager[] keyManagers = initKeyManager(loadedKeyStore, passphrase).getKeyManagers();
            final TrustManager[] trustManagers = initTrustManager(loadedKeyStore).getTrustManagers();
            return makeSSLContext(protocol, keyManagers, trustManagers);
        } else if(LogHelper.isNotNull(loadedKeyStore) && LogHelper.isNull(passphrase) && !excludeTrustManager) {
            final TrustManager[] trustManagers = initTrustManager(loadedKeyStore).getTrustManagers();
            return makeSSLContext(protocol, null, trustManagers);
        } else if(LogHelper.isNotNull(loadedKeyStore) && LogHelper.isNotNull(passphrase) && excludeTrustManager) {
            final KeyManager[] keyManagers = initKeyManager(loadedKeyStore, passphrase).getKeyManagers();
            return makeSSLContext(protocol, keyManagers, null);
        } else if(LogHelper.isNotNull(loadedKeyStore) && LogHelper.isNull(passphrase) && !excludeTrustManager) {
            final TrustManager[] trustManagers = initTrustManager(loadedKeyStore).getTrustManagers();
            return makeSSLContext(protocol, null, trustManagers);
        } else {
            return makeSSLContext(protocol, null, null, null);
        }
    }
    
    
    /**
     * Creates an SSLSocketFactory for HTTPS. Pass a KeyStore resource with your certificate and
     * passphrase
     *
     * @param protocol
     * @param keyAndTrustStoreFilePath
     * @param passphrase
     * @return
     * @throws Exception
     */
    public static SSLContext makeSSLContext(final String protocol, final String keyAndTrustStoreFilePath, final char[] passphrase) throws Exception {
        final KeyStore keystore = initKeyStore(new FileInputStream(keyAndTrustStoreFilePath), passphrase, false);
        return makeSSLContext(protocol, keystore, passphrase, false);
    }
    
    /**
     * Creates a SSLSocketFactory for HTTPS connection with the given '.crt' (certificate) file.
     *
     * @param certStream
     * @return
     * @throws Exception
     */
    public static SSLContext makeSSLContext(final InputStream certStream) throws Exception {
        // Load CAs from an InputStream
        CertificateFactory mCertFactory = CertificateFactory.getInstance("X.509");
        InputStream caInputStream = new BufferedInputStream(certStream);
        Certificate mCertificate;
        try {
            mCertificate = mCertFactory.generateCertificate(caInputStream);
            LogHelper.d(LOG_TAG, "CA=" + ((X509Certificate) mCertificate).getSubjectDN());
        } finally {
            closeSilently(caInputStream);
        }
        
        // Create a KeyStore containing our trusted CAs
        final KeyStore keyStore = initKeyStore(null, null, true);
        keyStore.setCertificateEntry("ca", mCertificate);
        return makeSSLContext(null, initTrustManager(keyStore).getTrustManagers());
    }
    
    /**
     * The 'javax.net.ssl.trustStore' to be set with the 'JKS'/'BKS' file path.
     *
     * @param trustStoreFile
     */
    public static void setSSLTrustStore(final File trustStoreFile) {
        System.setProperty("javax.net.ssl.trustStore", trustStoreFile.getAbsolutePath());
    }
    
    /**
     * The 'javax.net.ssl.trustStore' to be set with the 'JKS' or 'BKS' file path.
     *
     * @param trustStoreFilePath
     */
    public static void setSSLTrustStore(final String trustStoreFilePath) {
        setSSLTrustStore(new File(trustStoreFilePath));
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
    
    /**
     * Logs the server certificates.
     *
     * @param sslSocket
     */
    public static void logServerCertificate(final SSLSocket sslSocket) {
        try {
            Certificate[] serverCerts = sslSocket.getSession().getPeerCertificates();
            for(int i = 0; i < serverCerts.length; i++) {
                final Certificate mCertificate = serverCerts[i];
                LogHelper.i(LOG_TAG, "==== Certificate:" + (i + 1) + " ====");
                LogHelper.i(LOG_TAG, "Public Key:" + mCertificate.getPublicKey());
                LogHelper.i(LOG_TAG, "Certificate Type:" + mCertificate.getType());
                LogHelper.i(LOG_TAG, "\n");
            }
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        }
    }
    
    /**
     * Logs the socket information.
     *
     * @param sslSocket
     */
    public static void logSocketInfo(final SSLSocket sslSocket) {
        LogHelper.i(LOG_TAG, "Socket Class: " + sslSocket.getClass());
        LogHelper.i(LOG_TAG, "Remote Address:" + sslSocket.getInetAddress().toString());
        LogHelper.i(LOG_TAG, "Remote Port:" + sslSocket.getPort());
        LogHelper.i(LOG_TAG, "Local Socket Address:" + sslSocket.getLocalSocketAddress().toString());
        LogHelper.i(LOG_TAG, "Local Address:" + sslSocket.getLocalAddress().toString());
        LogHelper.i(LOG_TAG, "Local Port:" + sslSocket.getLocalPort());
        LogHelper.i(LOG_TAG, "Need Client Authentication:" + sslSocket.getNeedClientAuth());
        LogHelper.i(LOG_TAG, "Bound:" + sslSocket.isBound());
        LogHelper.i(LOG_TAG, "Closed:" + sslSocket.isClosed());
        LogHelper.i(LOG_TAG, "Connected:" + sslSocket.isConnected());
        LogHelper.i(LOG_TAG, "SupportedCipherSuites:" + LogHelper.toString(sslSocket.getSupportedCipherSuites()));
        LogHelper.i(LOG_TAG, "SupportedProtocols:" + LogHelper.toString(sslSocket.getSupportedProtocols()));
        LogHelper.i(LOG_TAG, "\n");
        
        /* session information. */
        // Get session after the connection is established
        final SSLSession sslSession = sslSocket.getSession();
        LogHelper.i(LOG_TAG, "SSLSession:");
        LogHelper.i(LOG_TAG, "Cipher Suite:" + sslSession.getCipherSuite());
        LogHelper.i(LOG_TAG, "Protocol:" + sslSession.getProtocol());
        LogHelper.i(LOG_TAG, "\n");
    }
    
    /**
     * Returns the response of the given <code>inputStream</code>. if <code>closeStream</code> is
     * set to be true, the <code>inputStream</code> stream is also closed.
     *
     * @param inputStream
     * @param closeStream
     * @return
     */
    public static String readStream(final InputStream inputStream, final boolean closeStream) {
        final StringBuilder resBuilder = new StringBuilder();
        BufferedReader resReader = null;
        try {
            resReader = new BufferedReader(new InputStreamReader(inputStream));
            String line = resReader.readLine();
            boolean firstLine = true;
            while(!LogHelper.isNullOrEmpty(line)) {
                if(firstLine) {
                    firstLine = false;
                } else {
                    resBuilder.append("\n");
                }
                resBuilder.append(line);
                line = resReader.readLine();
            }
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        } finally {
            if(closeStream) {
                closeSilently(resReader);
            }
        }
        
        return resBuilder.toString();
    }
    
}