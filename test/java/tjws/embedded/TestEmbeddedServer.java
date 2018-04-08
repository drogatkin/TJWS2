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

import java.io.File;
import java.io.PrintStream;
import java.util.Properties;

import com.rslakra.logger.LogManager;

import Acme.IOHelper;
import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;
import Acme.Serve.Serve.Status;
import rogatkin.web.WebApp;
import rogatkin.web.WebAppServlet;

/**
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 03:35:59 PM
 */
public final class TestEmbeddedServer {
	
	/** sslEnabled */
	private boolean sslEnabled;
	
	/** port */
	private int port;
	
	/** webServer */
	private TJWSServer webServer;
	/** logStream */
	private PrintStream logStream;
	
	/**
	 * 
	 * @return
	 */
	public boolean isSSLEnabled() {
		return sslEnabled;
	}
	
	/**
	 * 
	 * @param sslEnabled
	 */
	public void setSSLEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Sets the default port.
	 */
	public void setDefaultPort() {
		if (isSSLEnabled()) {
			setPort(9161);
		} else {
			setPort(5161);
		}
	}
	
	/**
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * Initializes the logging stream.
	 */
	private void initLogging() {
		File logDir = new File(IOHelper.getLogsDir());
		if (!logDir.exists()) {
			LogManager.debug("Creating [" + logDir.getAbsolutePath() + "] folder ...");
			logDir.mkdirs();
		}
		
		if (logStream == null) {
			try {
				logStream = new PrintStream(new File(logDir, "tjws.logs"), "UTF-8");
			} catch (Exception ex) {
				LogManager.error(ex);
			}
			
			if (logStream == null) {
				logStream = System.out;
			} else {
				System.setErr(logStream);
			}
		}
	}
	
	/**
	 * Initializes the embedded web server.
	 *
	 * @param enableSSLWebServer
	 */
	public void initServer(final boolean sslEnabled) {
		setSSLEnabled(sslEnabled);
		if (webServer == null) {
			initLogging();
			setDefaultPort();
			
			// setting properties for the server, and exchangeable Acceptors
			Properties properties = new Properties();
			properties.setProperty(Serve.ARG_NOHUP, Serve.ARG_NOHUP);
			/* keepAlive time. */
//			properties.setProperty(Serve.ARG_MAX_CONN_USE, String.valueOf(100));
			
			// log properties
			properties.setProperty(Serve.ARG_ACCESS_LOG_FMT, "{0} {2} [{3,date,yyyy/MM/dd HH:mm:ss Z}] \"{4} {5} {6}\" {7,number,#}");
			properties.setProperty(Serve.ARG_LOG_DIR, IOHelper.getLogsDir());
			
			if (isSSLEnabled()) {
				// SSL configurations.
				properties.setProperty(Serve.ARG_PORT, String.valueOf(getPort()));
				// properties.setProperty(SSLAcceptor.ARG_PORT,
				// String.valueOf(getPort()));
				properties.setProperty(Serve.ARG_ACCEPTOR_CLASS, "Acme.Serve.SSLAcceptor");
				// properties.setProperty(Serve.ARG_ACCEPTOR_CLASS,
				// "rogatkin.wskt.SSLSelectorAcceptor");
				String parentFolderPath = IOHelper.pathString(TestEmbeddedServer.class);
				LogManager.debug("parentFolderPath:" + parentFolderPath);
				final String keyStoreFilePath = IOHelper.pathString(parentFolderPath, "conf/tjws.jks");
				LogManager.debug("keyStoreFilePath:" + keyStoreFilePath);
				properties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, keyStoreFilePath);
				// properties.setProperty(SSLAcceptor.ARG_USE_KEYSTORE_BYTES,
				// String.valueOf(true));
				// try {
				// final byte[] keyStoreBytes =
				// IOHelper.readBytes(keyStoreFilePath, true);
				// properties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE,
				// keyStoreBytes);
				// } catch (IOException ex) {
				// LogManager.debug(ex);
				// }
				// properties.setProperty(SSLAcceptor.ARG_PROTOCOL, "TLSv1.2");
				properties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, "JKS");
				properties.setProperty(SSLAcceptor.ARG_CLIENTAUTH, "false");
				properties.setProperty(SSLAcceptor.ARG_KEYSTOREPASS, "password");
				// properties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, "BKS");
			} else {
				properties.remove(Serve.ARG_ACCEPTOR_CLASS);
				// this acceptor is required for web-socket support
				// properties.setProperty("acceptorImpl",
				// "Acme.Serve.SelectorAcceptor");
			}
			
			webServer = new TJWSServer(properties, logStream, this);
			webServer.addServlet("/", new EmbeddedServlet());
			webServer.setProperty(Serve.ARG_PORT, getPort());
			
			// add shutdown hook.
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					webServer.notifyStop();
					webServer.destroyAllServlets();
				}
			}));
			
			/** Set System properties. */
			System.setProperty(WebAppServlet.WAR_NAME_AS_CONTEXTPATH, "yes");
			// set dex class loader 'AndroidClassLoader'
			// System.setProperty(WebApp.DEF_WEBAPP_CLASSLOADER,
			// AndroidClassLoader.class.getName());
			System.setProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR, IOHelper.getLogsDir());
			LogManager.debug("webappdir:" + System.getProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR) + ", for app:" + IOHelper.getLogsDir());
			webServer.deployWebServer();
			
		} else {
			LogManager.debug("webServer is already initialized!");
		}
	}
	
	/**
	 * Returns true if the web server is running.
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return (webServer != null && webServer.isRunning());
	}
	
	/**
	 * Stops the webServer.
	 */
	public void stopServer() {
		if (isRunning()) {
			webServer.notifyStop();
		}
		
		webServer.destroyAllServlets();
		webServer.setDeployed(false);
		webServer = null;
		
		if (logStream != System.out && logStream != null) {
			logStream.close();
			logStream = null;
		}
		
		LogManager.debug("Web Server is stopped successfully!");
	}
	
	/**
	 *
	 */
	private void startServer() {
		if (!isRunning()) {
			new Thread() {
				/**
				 * Check the staus of the server.
				 * 
				 * @see Thread#run()
				 */
				@Override
				public void run() {
					try {
						// give a chance to server to run
						Thread.sleep(1000);
					} catch (InterruptedException ex) {
						// ignore me!
					} finally {
						LogManager.debug("Serve Running:" + webServer.isRunning());
					}
				}
			}.start();
			new Thread() {
				/**
				 * Starts the web server here.
				 *
				 * @see Thread#run()
				 */
				@Override
				public void run() {
					try {
						Status result = webServer.serve();
						webServer.log("Error running server! Error code:" + result);
					} finally {
						LogManager.debug("Serve Running:" + webServer.isRunning());
					}
				}
			}.start();
		}
	}
	
	/**
	 * The starting point of the server.
	 * Once, the server has started, try to access the following url on the
	 * browser:
	 * 
	 * <pre>
	 * http://localhost:5161/
	 * OR
	 * http://localhost:5161/html
	 * </pre>
	 * 
	 * If you have enabled, the SSL, you have to use the following URL:
	 * 
	 * <pre>
	 * https://localhost:9161/
	 * OR
	 * https://localhost:9161/html
	 * </pre>
	 * 
	 * Even you can use the following command, to get the server information and
	 * it's certificate:
	 * openssl s_client -connect localhost:9161 | openssl x509 -noout -subject
	 * -issuer
	 * 
	 * @param args
	 */
	public static void main(String... args) {
		// LogManager.setLogLevel(Level.DEBUG);
		// LogManager.debug("TempDir:" + IOHelper.getTempDir());
		TestEmbeddedServer server = new TestEmbeddedServer();
		server.initServer(true);
		server.startServer();
	}
}
