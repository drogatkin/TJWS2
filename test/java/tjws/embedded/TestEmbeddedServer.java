/**
 * 
 */
package tjws.embedded;

import java.io.File;
import java.io.PrintStream;
import java.util.Properties;

import com.devamatre.logger.LogManager;
import com.devamatre.logger.Logger;

import Acme.IOHelper;
import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;
import rogatkin.web.WebApp;
import rogatkin.web.WebAppServlet;

/**
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 03:35:59 PM
 */
public final class TestEmbeddedServer {
	
	/** logger */
	private static Logger logger = LogManager.getLogger(TestEmbeddedServer.class);
	
	/** sslEnabled */
	private boolean sslEnabled;
	
	/** port */
	private int port;
	
	/** webServer */
	private TJWSServer webServer;
	/** logStream */
	private PrintStream logStream;
	/** status */
	private int status;
	
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
			logger.debug("Creating [" + logDir.getAbsolutePath() + "] folder ...");
			logDir.mkdirs();
		}
		
		if (logStream == null) {
			try {
				logStream = new PrintStream(new File(logDir, "tjws.logs"), "UTF-8");
			} catch (Exception ex) {
				logger.error(ex);
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
				logger.debug("parentFolderPath:" + parentFolderPath);
				final String keyStoreFilePath = IOHelper.pathString(parentFolderPath, "conf/tjws.jks");
				logger.debug("keyStoreFilePath:" + keyStoreFilePath);
				properties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, keyStoreFilePath);
				properties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, "JKS");
				properties.setProperty(SSLAcceptor.ARG_CLIENTAUTH, "no");
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
			logger.debug("webappdir:" + System.getProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR) + ", for app:" + IOHelper.getLogsDir());
			webServer.deployWebServer();
			
		} else {
			logger.debug("webServer is already initialized!");
		}
	}
	
	/**
	 * Returns true if the web server is running.
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return (webServer != null && status != 0);
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
		
		logger.debug("Web Server is stopped successfully!");
	}
	
	/**
	 *
	 */
	private void startServer() {
		if (!isRunning()) {
			new Thread() {
				/**
				 * Starts the web server here.
				 *
				 * @see Thread#run()
				 */
				@Override
				public void run() {
					int status = 0;
					try {
						status = webServer.serve();
					} finally {
						logger.info("Serve status:" + status);
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
	 * @param args
	 */
	public static void main(String... args) {
		LogManager.configure(LogManager.LOG4J_PROPERTY_FILE);
		// logger.debug("TempDir:" + IOHelper.getTempDir());
		TestEmbeddedServer server = new TestEmbeddedServer();
		server.initServer(true);
		server.startServer();
	}
}
