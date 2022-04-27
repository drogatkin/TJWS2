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
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

/**
 * The container is created one per web application and one for default
 * application
 * 
 * @author Dmitriy
 *
 */
public class SimpleServerContainer implements ServerContainer, ServletContextListener {

	HashMap<String, ServerEndpointConfig> endpoints;
	SimpleProvider provider;

	HashSet<SimpleSession> sessions;
	
	ServletContext context;

	ExecutorService asyncService;
	
	int defBufSize = 1024 * 8;
	
	long idleTimeout;
	long asyncTimeout;
	
	static final int threshHold = 1024;

	public SimpleServerContainer(SimpleProvider simpleProvider) {
		provider = simpleProvider;
		endpoints = new HashMap<String, ServerEndpointConfig>();
		sessions = new HashSet<SimpleSession>();
		asyncService = Executors.newSingleThreadExecutor();
	}

	@Override
	public void addEndpoint(Class<?> arg0) throws DeploymentException {
		if (arg0.isAssignableFrom(ServerEndpointConfig.class)) {
			try {
				addEndpoint((ServerEndpointConfig) arg0.getConstructor().newInstance());
			} catch (InstantiationException e) {
				throw new DeploymentException("Error in deployment of an endpoint", e);
			} catch (IllegalAccessException|NoSuchMethodException e) {
				throw new DeploymentException("Error in deployment of an endpoint", e);
			} catch(InvocationTargetException e){
				throw new DeploymentException("Error in calling constructor at deployment of an endpoint", e);
			}
		} else {
			ServerEndpoint sep = arg0.getAnnotation(ServerEndpoint.class);
			Configurator configurator = null;
			if (ServerEndpointConfig.Configurator.class !=sep.configurator())
				try {
					configurator = sep.configurator().getConstructor().newInstance();
				} catch(Exception e) {
					throw new DeploymentException("Can't instantiate custom Configurator for "+sep.configurator(), e);
				}
			addEndpoint(ServerEndpointConfig.Builder.create(arg0, sep.value())
					.subprotocols(Arrays.asList(sep.subprotocols())).encoders(Arrays.asList(sep.encoders()))
					.decoders(Arrays.asList(sep.decoders())).configurator(configurator).build());
		}

	}

	@Override
	public void addEndpoint(ServerEndpointConfig arg0) throws DeploymentException {
		//if (endpoints.containsKey(arg0.getPath()))
			//throw new DeploymentException("More than one endpoint uses the same path " + arg0.getPath());
		if (containsKeyAdv(arg0.getPath())) {
			//System.err.printf("match to %s fount%n", arg0.getPath());
			throw new DeploymentException("More than one endpoint uses the same path " + arg0.getPath());
		}
		
		endpoints.put(arg0.getPath(), arg0);
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
	public Session connectToServer(Endpoint arg0, ClientEndpointConfig arg1, URI arg2) throws DeploymentException,
			IOException {
		throw new UnsupportedOperationException("No client websocket support");
	}

	@Override
	public Session connectToServer(Class<? extends Endpoint> arg0, ClientEndpointConfig arg1, URI arg2)
			throws DeploymentException, IOException {
		throw new UnsupportedOperationException("No client websocket support");
	}

	@Override
	public long getDefaultAsyncSendTimeout() {
		return asyncTimeout;
	}

	@Override
	public int getDefaultMaxBinaryMessageBufferSize() {
		return defBufSize;
	}

	@Override
	public long getDefaultMaxSessionIdleTimeout() {
		return idleTimeout;
	}

	@Override
	public int getDefaultMaxTextMessageBufferSize() {
		return defBufSize;
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

	@Override
	public void setDefaultMaxBinaryMessageBufferSize(int arg0) {
		if (arg0 > 1024)
			defBufSize = arg0;
	}

	@Override
	public void setDefaultMaxSessionIdleTimeout(long arg0) {
		if (arg0 < 0)
			return;
		idleTimeout = arg0;
	}

	@Override
	public void setDefaultMaxTextMessageBufferSize(int arg0) {
		if (arg0 > threshHold)
			defBufSize = arg0;
	}
	
	enum RFC_6570 {FirstPart, Transn}
	
	/**
	 * 
	 * @param key path in RFC 6570
	 * @return true if already such provided
	 * 
	 * @see SimpleProvider.parseTemplate
	 */
	boolean containsKeyAdv(String key) {
		allset: for (String entry : endpoints.keySet()) {
			//System.err.printf("looking for %s in %s%n", key, entry);
			RFC_6570 sta = RFC_6570.FirstPart;
			for (int k = 0, n=entry.length(), m=key.length(); k < n; k++) {
				if (k >= m)
					continue allset;
				switch(sta) {
				case FirstPart:
					if (entry.charAt(k) != key.charAt(k)) {
						continue allset;
					}
					if (entry.charAt(k) == '/')
						sta = RFC_6570.Transn;
					break;
				case Transn:
					if (entry.charAt(k) != key.charAt(k)) {
						continue allset;
					}
					if (entry.charAt(k) == '{')
						return true;
					if (entry.charAt(k) != '/')
						sta = RFC_6570.FirstPart;
					break;
				}
				
			}
		}
		return false;
	}

	void log(String msg, Object... params) {
		log(null, msg, params);
	}

	void log(Throwable e, String msg, Object... params) {
		msg = "websocket : " + msg;
		if (e == null)
			if (params == null || params.length == 0)
				provider.serve.log(msg);
			else
				provider.serve.log(String.format(msg, params));
		else if (params == null || params.length == 0)
			provider.serve.log(msg, e);
		else
			provider.serve.log(String.format(msg, params), e);
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		CloseReason cr = new CloseReason(CloseCodes.SERVICE_RESTART, "");
		//ArrayList <SimpleSession> copyss = new ArrayList<SimpleSession>(sessions);
		for (SimpleSession ss : new ArrayList<SimpleSession>(sessions))
			try {
				//System.err.printf("Closing session %s%n", ss);
				ss.close(cr);
			} catch (IOException e) {

			}
		context = null;
		sessions.clear();
		endpoints.clear();
		asyncService.shutdown();
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		context = event.getServletContext();
	}

	void addSession(SimpleSession ss) {
		sessions.add(ss);
	}

	void removeSession(SimpleSession ss) {
		sessions.remove(ss);
	}

}
