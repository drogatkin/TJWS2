/* tjws - SSLAcceptor.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  
 *  Visit http://tjws.sourceforge.net to get the latest information
 *  about Rogatkin's products.                                                        
 *  $Id: SSLAcceptor.java,v 1.10 2013/03/02 09:11:56 cvs Exp $                
 *  Created on Feb 21, 2007
 *  @author dmitriy
 */
package Acme.Serve;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Security;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

import Acme.Utils;
import Acme.Serve.Serve.Acceptor;

public class SSLAcceptor implements Acceptor {
	public static final String ARG_ALGORITHM = "algorithm"; // SUNX509

	public static final String ARG_CLIENTAUTH = "clientAuth"; // false

	public static final String ARG_KEYSTOREFILE = "keystoreFile"; //

	public static final String PROP_KEYSTOREFILE = "javax.net.ssl.keyStore"; //

	public static final String ARG_KEYSTOREPASS = "keystorePass"; // KEYSTOREPASS

	public static final String PROP_KEYSTOREPASS = "javax.net.ssl.keyStorePassword";

	public static final String ARG_KEYSTORETYPE = "keystoreType"; // KEYSTORETYPE

	public static final String PROP_KEYSTORETYPE = "javax.net.ssl.keyStoreType"; // 

	public static final String ARG_KEYPASS = "keyPass"; //

	public static final String ARG_PROTOCOL = "protocol"; // TLS

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
	 *  default socket SSL handshake timeout preventing DoS attacks
	 */
	protected static final int SO_HS_TIMEOIUT = 30 * 1000;

	/**
	 * Pathname to the key store file to be used.
	 */
	protected String keystoreFile = System.getProperty("user.home") + File.separator + ".keystore";

	protected ServerSocket socket;
	
	protected static int so_hs_timeout;

	protected boolean android, jsse10;

	private String getKeystoreFile() {
		return (this.keystoreFile);
	}

	public Socket accept() throws IOException {
		return socket.accept();
	}

	public void destroy() throws IOException {
		try {
			socket.close();
		} finally {
			socket = null;
		}
	}

	public void init(Map inProperties, Map outProperties) throws IOException {
		// Create the proxy and return
		javax.net.ssl.SSLServerSocketFactory sslSoc = initSSLContext(inProperties, outProperties)
				.getServerSocketFactory();
		int port = Utils.parseInt(inProperties.get(ARG_PORT),  Utils.parseInt(inProperties.get(Serve.ARG_PORT), PORT));
		
		if (inProperties.get(ARG_BACKLOG) == null)
			if (inProperties.get(ARG_IFADDRESS) == null)
				socket = sslSoc.createServerSocket(port);
			else
				socket = sslSoc.createServerSocket(port, BACKLOG,
						InetAddress.getByName((String) inProperties.get(ARG_IFADDRESS)));
		else if (inProperties.get(ARG_IFADDRESS) == null)
			socket = sslSoc.createServerSocket(port, Utils.parseInt(inProperties.get(ARG_BACKLOG), BACKLOG));
		else
			socket = sslSoc.createServerSocket(port, Utils.parseInt(inProperties.get(ARG_BACKLOG), BACKLOG),
					InetAddress.getByName((String) inProperties.get(ARG_IFADDRESS)));

		initServerSocket(socket, "true".equals(inProperties.get(ARG_CLIENTAUTH)));
		if (outProperties != null)
			outProperties.put(Serve.ARG_BINDADDRESS, socket.getInetAddress().getHostName());
		// note it isn't use for the implementation since there is no control over handshake
		so_hs_timeout = Utils.parseInt(inProperties.get(ARG_SO_HS_TIMEOUT), SO_HS_TIMEOIUT);
	}

	protected SSLContext initSSLContext(Map inProperties, Map outProperties) throws IOException {
		// init keystore
		KeyStore keyStore = null;
		FileInputStream istream = null;
		String keystorePass = null;
		android = System.getProperty("java.vm.name") != null && System.getProperty("java.vm.name").startsWith("Dalvik");
		try {
			String keystoreType = getWithDefault(inProperties, ARG_KEYSTORETYPE, KEYSTORETYPE);
			keyStore = KeyStore.getInstance(keystoreType);
			String keystoreFile = (String) inProperties.get(ARG_KEYSTOREFILE);
			if (keystoreFile == null)
				keystoreFile = getKeystoreFile();
			istream = new FileInputStream(keystoreFile);
			keystorePass = getWithDefault(inProperties, ARG_KEYSTOREPASS, KEYSTOREPASS);
			keyStore.load(istream, keystorePass.toCharArray());
		} catch (Exception e) {
			throw (IOException) new IOException(e.toString()).initCause(e);
		} finally {
			if (istream != null)
				istream.close();
		}

		try {
			// Register the JSSE security Provider (if it is not already there)
			if (android == false)
				try {
					Security.addProvider((java.security.Provider) Class
							.forName("com.sun.net.ssl.internal.ssl.Provider").newInstance());
				} catch (Throwable t) {
					if (t instanceof ThreadDeath)
						throw (ThreadDeath) t;
					// TODO think to do not propagate absence of the provider, since other can still work
					throw (IOException) new IOException(t.toString()).initCause(t);
				}

			// Create an SSL context used to create an SSL socket factory
			String protocol = getWithDefault(inProperties, ARG_PROTOCOL, TLS);
			SSLContext context = SSLContext.getInstance(protocol);

			// Create the key manager factory used to extract the server key
			String algorithm = getWithDefault(inProperties, ARG_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);

			String keyPass = getWithDefault(inProperties, ARG_KEYPASS, keystorePass);

			keyManagerFactory.init(keyStore, keyPass.toCharArray());

			// Initialize the context with the key managers
			context.init(keyManagerFactory.getKeyManagers(), null, new java.security.SecureRandom());
			return context;
		} catch (Exception e) {
			System.err.println("SSLsocket creation:  " + e);
			e.printStackTrace();
			throw (IOException) new IOException(e.toString()).initCause(e);
		}
	}

	/**
	 * Register our URLStreamHandler for the "https:" protocol.
	 */
	protected static void initHandler() {

		String packages = System.getProperty(PROTOCOL_PACKAGES);
		if (packages == null)
			packages = PROTOCOL_HANDLER;
		else if (packages.indexOf(PROTOCOL_HANDLER) < 0)
			packages += "|" + PROTOCOL_HANDLER;
		System.setProperty(PROTOCOL_PACKAGES, packages);
	}

	public String toString() {
		return socket != null ? socket.toString() : "SSLAcceptor uninitialized";
	}

	static {
		initHandler();
	}

	/**
	 * Set the requested properties for this server socket.
	 * 
	 * @param ssocket
	 *            The server socket to be configured
	 */
	protected void initServerSocket(ServerSocket ssocket, boolean clientAuth) {

		SSLServerSocket socket = (SSLServerSocket) ssocket;

		// Enable all available cipher suites when the socket is connected
		String cipherSuites[] = socket.getSupportedCipherSuites();
		socket.setEnabledCipherSuites(cipherSuites);
		// Set client authentication if necessary
		socket.setNeedClientAuth(clientAuth);
	}

	private String getWithDefault(Map args, String name, String defValue) {
		String result = (String) args.get(name);
		if (result == null)
			return defValue;
		return result;
	}
}
