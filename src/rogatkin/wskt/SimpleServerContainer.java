/* tjws - JSR356
 * Copyright (C) 2004-2015 Dmitriy Rogatkin.  All rights reserved.
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  
 * Created on Jan 11, 2015
*/
package rogatkin.wskt;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

/**
 * The container is created one per web application and one for default
 * application
 * 
 * @author Dmitriy
 * @author Rohtash Singh Lakra
 */
public class SimpleServerContainer implements ServerContainer, ServletContextListener {
	
	HashMap<String, ServerEndpointConfig> endpoints;
	SimpleProvider provider;
	
	HashSet<SimpleSession> sessions;
	ExecutorService asyncService;
	int maxBufferSize = 1024 * 8;
	long idleTimeout;
	long asyncTimeout;
	
	/**
	 * 
	 * @param simpleProvider
	 */
	public SimpleServerContainer(SimpleProvider simpleProvider) {
		provider = simpleProvider;
		endpoints = new HashMap<String, ServerEndpointConfig>();
		sessions = new HashSet<SimpleSession>();
		asyncService = Executors.newSingleThreadExecutor();
	}
	
	/**
	 * @see javax.websocket.server.ServerContainer#addEndpoint(java.lang.Class)
	 */
	@Override
	public void addEndpoint(Class<?> endPointClass) throws DeploymentException {
		if (endPointClass.isAssignableFrom(ServerEndpointConfig.class)) {
			try {
				addEndpoint((ServerEndpointConfig) endPointClass.newInstance());
			} catch (InstantiationException ex) {
				throw new DeploymentException("Error in deployment end point", ex);
			} catch (IllegalAccessException ex) {
				throw new DeploymentException("Error in deployment end point", ex);
			}
		} else {
			ServerEndpoint serverEndPoint = endPointClass.getAnnotation(ServerEndpoint.class);
			Configurator configurator = null;
			if (ServerEndpointConfig.Configurator.class != serverEndPoint.configurator()) {
				try {
					configurator = serverEndPoint.configurator().newInstance();
				} catch (Exception ex) {
					throw new DeploymentException("Can't instantiate custom Configurator for:" + serverEndPoint.configurator(), ex);
				}
			}
			addEndpoint(ServerEndpointConfig.Builder.create(endPointClass, serverEndPoint.value()).subprotocols(Arrays.asList(serverEndPoint.subprotocols())).encoders(Arrays.asList(serverEndPoint.encoders())).decoders(Arrays.asList(serverEndPoint.decoders())).configurator(configurator).build());
		}
	}
	
	/**
	 * @see javax.websocket.server.ServerContainer#addEndpoint(javax.websocket.server.ServerEndpointConfig)
	 */
	@Override
	public void addEndpoint(ServerEndpointConfig serverEndPointConfig) throws DeploymentException {
		if (endpoints.containsKey(serverEndPointConfig.getPath())) {
			throw new DeploymentException("More than one end points use same path:" + serverEndPointConfig.getPath());
		}
		endpoints.put(serverEndPointConfig.getPath(), serverEndPointConfig);
	}
	
	@Override
	public Session connectToServer(Object arg0, URI arg1) throws DeploymentException, IOException {
		throw new UnsupportedOperationException("No client websocket support");
	}
	
	@Override
	public Session connectToServer(Class<?> arg0, URI arg1) throws DeploymentException, IOException {
		throw new UnsupportedOperationException("No client websocket support");
	}
	
	@Override
	public Session connectToServer(Endpoint arg0, ClientEndpointConfig arg1, URI arg2) throws DeploymentException, IOException {
		throw new UnsupportedOperationException("No client websocket support");
	}
	
	@Override
	public Session connectToServer(Class<? extends Endpoint> arg0, ClientEndpointConfig arg1, URI arg2) throws DeploymentException, IOException {
		throw new UnsupportedOperationException("No client websocket support");
	}
	
	@Override
	public long getDefaultAsyncSendTimeout() {
		return asyncTimeout;
	}
	
	@Override
	public int getDefaultMaxBinaryMessageBufferSize() {
		return maxBufferSize;
	}
	
	@Override
	public long getDefaultMaxSessionIdleTimeout() {
		return idleTimeout;
	}
	
	@Override
	public int getDefaultMaxTextMessageBufferSize() {
		return maxBufferSize;
	}
	
	@Override
	public Set<Extension> getInstalledExtensions() {
		return null;
	}
	
	@Override
	public void setAsyncSendTimeout(long arg0) {
		if (arg0 >= 0)
			asyncTimeout = arg0;
	}
	
	/**
	 * @see javax.websocket.WebSocketContainer#setDefaultMaxBinaryMessageBufferSize(int)
	 */
	@Override
	public void setDefaultMaxBinaryMessageBufferSize(int maxBufferSize) {
		if (maxBufferSize > 1024) {
			this.maxBufferSize = maxBufferSize;
		}
	}
	
	/**
	 * @see javax.websocket.WebSocketContainer#setDefaultMaxSessionIdleTimeout(long)
	 */
	@Override
	public void setDefaultMaxSessionIdleTimeout(long maxIdleTimeout) {
		if (maxIdleTimeout < 0) {
			return;
		}
		idleTimeout = maxIdleTimeout;
	}
	
	/**
	 * @see javax.websocket.WebSocketContainer#setDefaultMaxTextMessageBufferSize(int)
	 */
	@Override
	public void setDefaultMaxTextMessageBufferSize(int maxBufferSize) {
		if (maxBufferSize > 1024) {
			this.maxBufferSize = maxBufferSize;
		}
	}
	
	/**
	 * 
	 * @param message
	 * @param params
	 */
	void log(String message, Object... params) {
		log(null, message, params);
	}
	
	/**
	 * 
	 * @param throwable
	 * @param message
	 * @param params
	 */
	void log(Throwable throwable, String message, Object... params) {
		message = "websocket : " + message;
		if (throwable == null) {
			if (params == null || params.length == 0) {
				provider.serve.log(message);
			} else {
				provider.serve.log(String.format(message, params));
			}
		} else if (params == null || params.length == 0) {
			provider.serve.log(message, throwable);
		} else {
			provider.serve.log(String.format(message, params), throwable);
		}
	}
	
	/**
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		CloseReason closeReason = new CloseReason(CloseCodes.SERVICE_RESTART, "");
		// ArrayList <SimpleSession> copyss = new
		// ArrayList<SimpleSession>(sessions);
		for (SimpleSession ss : new ArrayList<SimpleSession>(sessions)) {
			try {
				// System.err.printf("Closing session %s%n", ss);
				ss.close(closeReason);
			} catch (IOException e) {
				
			}
		}
		
		sessions.clear();
		asyncService.shutdown();
	}
	
	/**
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent event) {
		
	}
	
	/**
	 * 
	 * @param simpleSession
	 */
	void addSession(SimpleSession simpleSession) {
		sessions.add(simpleSession);
	}
	
	/**
	 * 
	 * @param simpleSession
	 */
	void removeSession(SimpleSession simpleSession) {
		sessions.remove(simpleSession);
	}
	
}
