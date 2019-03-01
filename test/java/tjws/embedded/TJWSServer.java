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

import java.io.PrintStream;
import java.util.Properties;

import com.rslakra.logger.LogManager;

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
			LogManager.error(ex);
		}
		
		LogManager.debug("TJWSServer(" + arguments + ", " + logStream + ", " + runtime + ")");
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
		LogManager.debug("setDeployed(" + deployed + ")");
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
		LogManager.debug("setMappingTable(" + mappingTable + ")");
		super.setMappingTable(mappingTable);
	}
	
	/**
	 * (non-Javadoc)
	 *
	 * @see Acme.Serve.Serve#setRealms(Acme.Serve.Serve.PathTreeDictionary)
	 */
	@Override
	protected void setRealms(PathTreeDictionary realms) {
		LogManager.debug("setRealms(" + realms + ")");
		super.setRealms(realms);
	}
	
	/**
	 * @param key
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public synchronized void setProperty(Object key, Object value) {
		LogManager.debug("setProperty(" + key + ", " + value + ")");
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
		LogManager.debug("removeProperty(" + key + ")");
		if (super.arguments != null) {
			super.arguments.remove(key);
		}
	}
	
	/**
	 * Deploy the local web server into the warDeployer.
	 */
	public synchronized void deployWebServer() {
		LogManager.debug("+deployWebServer(), deployed:" + isDeployed());
		try {
			if (!isDeployed()) {
				if (warDeployer == null) {
					warDeployer = new WarRoller();
				}
				
				warDeployer.deploy(this);
				setDeployed(true);
			}
		} catch (Throwable ex) {
			LogManager.error("Unexpected problem in deployment, ex:", ex);
			if (ex instanceof ThreadDeath) {
				throw (ThreadDeath) ex;
			}
		}
		
		LogManager.debug("-deployWebServer(), deployed:" + isDeployed());
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