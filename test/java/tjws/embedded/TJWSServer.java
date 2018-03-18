/**
 * 
 */
package tjws.embedded;

import java.io.PrintStream;
import java.util.Properties;

import com.devamatre.logger.LogManager;
import com.devamatre.logger.Logger;

import rogatkin.web.WarRoller;
import rogatkin.web.WebAppServlet;

/**
 * @author Rohtash Singh Lakra (Rohtash.Lakra@nasdaq.com)
 * @date 03/15/2018 04:00:49 PM
 */

/**
 * @author Rohtash Singh (rsingh@boardvantage.com)
 * @version 1.0.0
 * @since Apr 28, 2015 5:26:56 PM
 */
public final class TJWSServer extends Acme.Serve.Serve {
	
	/** serialVersionUID */
	private static final long serialVersionUID = 2121247574146350235L;
	
	/** logger */
	private static Logger logger = LogManager.getLogger(TJWSServer.class);
	
	/** warDeployer */
	private WarRoller warDeployer;
	
	/** deployed */
	private boolean deployed;
	
	/**
	 * The default constructor which is the only one that works in android.
	 *
	 * @param arguments
	 * @param logStream
	 * @param runtime
	 * @param preferenceManager
	 */
	public TJWSServer(final Properties arguments, PrintStream logStream, Object runtime) {
		super(arguments, logStream);
		/* provide SERVLET context Android environment access */
		try {
			WebAppServlet.setRuntimeEnv(runtime);
		} catch (Exception ex) {
			logger.error(ex);
		}
		
		logger.info("TJWSServer(" + arguments + ", " + logStream + ", " + runtime + ")");
	}
	
	/**
	 * @return
	 */
	public boolean isDeployed() {
		return deployed;
	}
	
	/**
	 * The deployed to be set.
	 *
	 * @param deployed
	 */
	public void setDeployed(boolean deployed) {
		logger.debug("setDeployed(" + deployed + ")");
		this.deployed = deployed;
	}
	
	/**
	 * Overriding method for public access
	 *
	 * @param mappingTable
	 * @see Acme.Serve.Serve#setMappingTable(Acme.Serve.Serve.PathTreeDictionary)
	 */
	@Override
	public void setMappingTable(PathTreeDictionary mappingTable) {
		logger.debug("setMappingTable(" + mappingTable + ")");
		super.setMappingTable(mappingTable);
	}
	
	/**
	 * (non-Javadoc)
	 *
	 * @see Acme.Serve.Serve#setRealms(Acme.Serve.Serve.PathTreeDictionary)
	 */
	@Override
	protected void setRealms(PathTreeDictionary realms) {
		logger.debug("setRealms(" + realms + ")");
		super.setRealms(realms);
	}
	
	/**
	 * @param key
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public synchronized void setProperty(Object key, Object value) {
		logger.debug("setProperty(" + key + ", " + value + ")");
		if (super.arguments == null) {
			super.arguments = new Properties();
		}
		
		super.arguments.put(key, value);
	}
	
	/**
	 * Removes the specified property.
	 *
	 * @param key
	 */
	public synchronized void removeProperty(Object key) {
		logger.debug("removeProperty(" + key + ")");
		if (super.arguments != null) {
			super.arguments.remove(key);
		}
	}
	
	/**
	 * Deploy the local web server into the warDeployer.
	 */
	public synchronized void deployWebServer() {
		logger.debug("+deployWebServer(), deployed:" + isDeployed());
		try {
			if (!isDeployed()) {
				if (warDeployer == null) {
					warDeployer = new WarRoller();
				}
				
				warDeployer.deploy(this);
				setDeployed(true);
			}
		} catch (Throwable ex) {
			logger.error("Unexpected problem in deployment, ex:", ex);
			if (ex instanceof ThreadDeath) {
				throw (ThreadDeath) ex;
			}
		}
		
		logger.debug("-deployWebServer(), deployed:" + isDeployed());
	}
	
	/**
	 * @param logEnabled
	 */
	protected void setLogEnabled(boolean logEnabled) {
		if (logEnabled) {
			setProperty(ARG_LOG_OPTIONS, "L");
		} else {
			removeProperty(ARG_LOG_OPTIONS);
		}
		
		setAccessLogged();
	}
	
}