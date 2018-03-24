/*
 * tjws - SSLAcceptor.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * Visit http://tjws.sourceforge.net to get the latest information
 * about Rogatkin's products.
 * $Id: SSLAcceptor.java,v 1.10 2013/03/02 09:11:56 cvs Exp $
 * Created on Feb 21, 2007
 * @author dmitriy
 */
package Acme.Serve;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import Acme.IOHelper;
import Acme.Utils;
import Acme.Serve.Serve.Acceptor;
import rslakra.logger.LogHelper;

public class SSLAcceptor implements Acceptor {
	// SUNX509
	public static final String ARG_ALGORITHM = "algorithm";
	// by default false
	public static final String ARG_CLIENTAUTH = "clientAuth";
	// ARG_KEYSTOREFILE
	public static final String ARG_KEYSTOREFILE = "keystoreFile";
	// ARG_USE_KEYSTORE_BYTES
	public static final String ARG_USE_KEYSTORE_BYTES = "useKeyStoreBytes";
	// ARG_KEYSTORE_BYTES
	public static final String ARG_KEYSTORE_BYTES = "keyStoreBytes";
	// PROP_KEYSTOREFILE
	public static final String PROP_KEYSTOREFILE = "javax.net.ssl.keyStore";
	// KEYSTOREPASS
	public static final String ARG_KEYSTOREPASS = "keystorePass";
	public static final String PROP_KEYSTOREPASS = "javax.net.ssl.keyStorePassword";
	// KEYSTORETYPE
	public static final String ARG_KEYSTORETYPE = "keystoreType";
	// PROP_KEYSTORETYPE
	public static final String PROP_KEYSTORETYPE = "javax.net.ssl.keyStoreType";
	// PROP_KEYSTORETYPE
	public static final String ARG_KEYPASS = "keyPass";
	// TLS ARG_PROTOCOL
	public static final String ARG_PROTOCOL = "protocol";
	public static final String ARG_BACKLOG = Serve.ARG_BACKLOG;
	public static final String ARG_SO_HS_TIMEOUT = "socket-handshake-timeout";
	public static final String ARG_IFADDRESS = "ifAddress";
	public static final String ARG_PORT = "ssl-port";
	public static final String PROTOCOL_HANDLER_JSSE10 = "com.sun.net.ssl.internal.www.protocol";
	public static final String PROTOCOL_HANDLER = "javax.net.ssl.internal.www.protocol";
	
	/**
	 * The name of the system property containing a "|" delimited list of
	 * protocol handler packages.
	 */
	public static final String PROTOCOL_PACKAGES = "java.protocol.handler.pkgs";
	
	/**
	 * Certificate encoding algorithm to be used.
	 */
	public final static String SUNX509 = "SunX509";
	
	/**
	 * default SSL port
	 */
	public final static int PORT = 8443;
	
	/**
	 * default backlog
	 */
	public final static int BACKLOG = 1000;
	
	/**
	 * Storage type of the key store file to be used.
	 */
	public final static String KEYSTORETYPE = "JKS";
	
	/**
	 * SSL protocol variant to use.
	 */
	public final static String TLS = "TLS";
	
	/**
	 * SSL protocol variant to use.
	 */
	public static final String protocol = TLS;
	
	/**
	 * Password for accessing the key store file.
	 */
	private static final String KEYSTOREPASS = "changeme";
	
	/**
	 * default socket SSL handshake timeout preventing DoS attacks
	 */
	protected static final int SO_HS_TIMEOIUT = 30 * 1000;
	
	/**
	 * Pathname to the key store file to be used.
	 */
	protected String keystoreFile = System.getProperty("user.home") + File.separator + ".keystore";
	protected ServerSocket socket;
	protected static int socketHandshakeTimeout;
	
	static {
		try {
			if (IOHelper.isAndroid()) {
				/* add bouncy castle provider. */
				Security.addProvider(new BouncyCastleProvider());
				LogHelper.log("Added BouncyCastleProvider!");
			}
		} catch (Exception ex) {
			LogHelper.log(ex);
		}
	}
	
	/**
	 * Returns the keystore file path.
	 * 
	 * @return
	 */
	private String getKeystoreFile() {
		return (this.keystoreFile);
	}
	
	/**
	 * 
	 * (non-Javadoc)
	 * 
	 * @see Acme.Serve.Serve.Acceptor#accept()
	 */
	public Socket accept() throws IOException {
		return socket.accept();
	}
	
	/**
	 * Disconnects the socket and set it null.
	 * 
	 * @see Acme.Serve.Serve.Acceptor#destroy()
	 */
	public void destroy() throws IOException {
		IOHelper.safeClose(socket, true);
	}
	
	/**
	 * Initialize the SSL server socket.
	 * 
	 * @see Acme.Serve.Serve.Acceptor#init(java.util.Map, java.util.Map)
	 */
	public void init(Map inProperties, Map outProperties) throws IOException {
		// Create the proxy and return
		SSLServerSocketFactory sslSocketFactory = initSSLContext(inProperties, outProperties).getServerSocketFactory();
		int port = Utils.parseInt(inProperties.get(ARG_PORT), Utils.parseInt(inProperties.get(Serve.ARG_PORT), PORT));
		
		if (inProperties.get(ARG_BACKLOG) == null) {
			if (inProperties.get(ARG_IFADDRESS) == null) {
				socket = sslSocketFactory.createServerSocket(port);
			} else {
				socket = sslSocketFactory.createServerSocket(port, BACKLOG, InetAddress.getByName((String) inProperties.get(ARG_IFADDRESS)));
			}
		} else if (inProperties.get(ARG_IFADDRESS) == null) {
			socket = sslSocketFactory.createServerSocket(port, Utils.parseInt(inProperties.get(ARG_BACKLOG), BACKLOG));
		} else {
			socket = sslSocketFactory.createServerSocket(port, Utils.parseInt(inProperties.get(ARG_BACKLOG), BACKLOG), InetAddress.getByName((String) inProperties.get(ARG_IFADDRESS)));
		}
		
		initServerSocket(socket, "true".equals(inProperties.get(ARG_CLIENTAUTH)));
		if (outProperties != null) {
			outProperties.put(Serve.ARG_BINDADDRESS, socket.getInetAddress().getHostName());
		}
		
		/*
		 * note it isn't use for the implementation since there is no control
		 * over handshake
		 */
		socketHandshakeTimeout = Utils.parseInt(inProperties.get(ARG_SO_HS_TIMEOUT), SO_HS_TIMEOIUT);
	}
	
	/**
	 * 
	 * @param inProperties
	 * @param outProperties
	 * @return
	 * @throws IOException
	 */
	protected SSLContext initSSLContext(Map inProperties, Map outProperties) throws IOException {
		// init keystore
		KeyStore keyStore = null;
		InputStream keyStoreStream = null;
		String keystorePass = null;
		LogHelper.log("isAndroid:" + IOHelper.isAndroid());
		try {
			String keystoreType = getWithDefault(inProperties, ARG_KEYSTORETYPE, KEYSTORETYPE);
			LogHelper.log("keystoreType:" + keystoreType);
			keyStore = KeyStore.getInstance(keystoreType);
			String keystoreFile = (String) inProperties.get(ARG_KEYSTOREFILE);
			if (keystoreFile == null) {
				keystoreFile = getKeystoreFile();
			}
			LogHelper.log("keystoreFile:" + keystoreFile);
			// if (IOHelper.toBoolean(inProperties.get(ARG_USE_KEYSTORE_BYTES)))
			// {
			// LogHelper.log("using keystore bytes.");
			// final byte[] keyStoreBytes = (byte[])
			// inProperties.get(ARG_KEYSTORE_BYTES);
			// keyStoreStream = IOHelper.toInputStream(keyStoreBytes);
			// } else {
			// keyStoreStream = new FileInputStream(keystoreFile);
			// }
			keyStoreStream = new FileInputStream(keystoreFile);
			keystorePass = getWithDefault(inProperties, ARG_KEYSTOREPASS, KEYSTOREPASS);
			keyStore.load(keyStoreStream, keystorePass.toCharArray());
		} catch (Exception ex) {
			LogHelper.log("Error initializing SSLAcceptor!", ex);
			throw (IOException) new IOException(ex.toString()).initCause(ex);
		} finally {
			IOHelper.safeClose(keyStoreStream);
		}
		
		try {
			// Register the JSSE security Provider (if it is not already there)
			if (!IOHelper.isAndroid()) {
				try {
					Security.addProvider((Provider) Class.forName("com.sun.net.ssl.internal.ssl.Provider").newInstance());
				} catch (Throwable th) {
					LogHelper.log("Error adding SSL provider!", th);
					if (th instanceof ThreadDeath) {
						throw (ThreadDeath) th;
					}
					// TODO think to do not propagate absence of the provider,
					// since other can still work
					throw (IOException) new IOException(th.toString()).initCause(th);
				}
			}
			
			// Create an SSL context used to create an SSL socket factory
			String protocol = getWithDefault(inProperties, ARG_PROTOCOL, TLS);
			LogHelper.log("protocol:" + protocol);
			SSLContext context = SSLContext.getInstance(protocol);
			
			// Create the key manager factory used to extract the server key
			String algorithm = getWithDefault(inProperties, ARG_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
			LogHelper.log("algorithm:" + algorithm);
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);
			
			String keyPass = getWithDefault(inProperties, ARG_KEYPASS, keystorePass);
			keyManagerFactory.init(keyStore, keyPass.toCharArray());
			
			// Initialize the context with the key managers
			context.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
			return context;
		} catch (Exception ex) {
			LogHelper.log("Error while creating SSLSocket!", ex);
			throw (IOException) new IOException(ex.toString()).initCause(ex);
		}
	}
	
	/**
	 * Register our URLStreamHandler for the "https:" protocol.
	 */
	protected static void initHandler() {
		String packages = System.getProperty(PROTOCOL_PACKAGES);
		if (packages == null) {
			packages = PROTOCOL_HANDLER;
		} else if (packages.indexOf(PROTOCOL_HANDLER) < 0) {
			packages += "|" + PROTOCOL_HANDLER;
		}
		System.setProperty(PROTOCOL_PACKAGES, packages);
	}
	
	/**
	 * Returns the string representation of this object.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return (socket != null ? socket.toString() : "SSLAcceptor Uninitialized!");
	}
	
	static {
		initHandler();
	}
	
	/**
	 * Set the requested properties for this server socket.
	 * 
	 * @param serverSocket
	 * @param needClientAuth
	 */
	protected void initServerSocket(final ServerSocket serverSocket, final boolean needClientAuth) {
		SSLServerSocket sslSocket = (SSLServerSocket) serverSocket;
		// Enable all available cipher suites when the socket is connected
		sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
		// Set client authentication if necessary
		sslSocket.setNeedClientAuth(needClientAuth);
	}
	
	/**
	 * 
	 * @param args
	 * @param name
	 * @param defValue
	 * @return
	 */
	private String getWithDefault(Map args, String name, String defValue) {
		String result = (String) args.get(name);
		return (result == null ? defValue : result);
	}
}
