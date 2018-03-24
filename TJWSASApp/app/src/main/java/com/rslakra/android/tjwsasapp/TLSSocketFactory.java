package com.rslakra.android.tjwsasapp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * @Author: Rohtash Singh Lakra
 * @Created: 2018/03/23 1:05 PM
 */
public class TLSSocketFactory extends SSLSocketFactory {
    
    private SSLSocketFactory mSSLSocketFactory;
    
    public TLSSocketFactory(SSLSocketFactory sslSocketFactory) throws KeyManagementException, NoSuchAlgorithmException {
        mSSLSocketFactory = sslSocketFactory;
    }
    
    @Override
    public String[] getDefaultCipherSuites() {
        return mSSLSocketFactory.getDefaultCipherSuites();
    }
    
    @Override
    public String[] getSupportedCipherSuites() {
        return mSSLSocketFactory.getSupportedCipherSuites();
    }
    
    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(mSSLSocketFactory.createSocket(s, host, port, autoClose));
    }
    
    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLSOnSocket(mSSLSocketFactory.createSocket(host, port));
    }
    
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLSOnSocket(mSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }
    
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(mSSLSocketFactory.createSocket(host, port));
    }
    
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(mSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    /*
     * Utility methods
     */
    
    private static Socket enableTLSOnSocket(Socket socket) {
        if(socket != null && (socket instanceof SSLSocket) && isTLSServerEnabled((SSLSocket) socket)) {
            // skip the fix if server doesn't provide there TLS version
            ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1.1", "TLSv1.2"});
        }
        return socket;
    }
    
    private static boolean isTLSServerEnabled(SSLSocket sslSocket) {
        System.out.println("isTLSServerEnabled:" + sslSocket.getSupportedProtocols().toString());
        for(String protocol : sslSocket.getSupportedProtocols()) {
            if(protocol.equals("TLSv1.1") || protocol.equals("TLSv1.2")) {
                return true;
            }
        }
        return false;
    }
}