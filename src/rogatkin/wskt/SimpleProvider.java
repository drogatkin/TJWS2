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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import com.rslakra.logger.LogManager;

import Acme.IOHelper;
import Acme.Utils;
import Acme.Serve.Serve;
import Acme.Serve.Serve.ServeConnection;
import Acme.Serve.Serve.WebsocketProvider;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ClassAnnotationMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.InterfaceMatchProcessor;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.SubclassMatchProcessor;

/**
 * @author Rohtash Singh Lakra
 * @date 04/06/2018 12:46:53 PM
 */
public class SimpleProvider implements WebsocketProvider, Runnable {
	
	public static final String WSKT_KEY = "Sec-WebSocket-Key";
	public static final String WSKT_ORIGIN = "Origin";
	public static final String WSKT_PROTOCOL = "Sec-WebSocket-Protocol";
	public static final String WSKT_VERSION = "Sec-WebSocket-Version";
	public static final String WSKT_ACEPT = "Sec-WebSocket-Accept";
	// HandshakeRequest.SEC_WEBSOCKET_EXTENSIONS
	public static final String WSKT_EXTS = "Sec-WebSocket-Extensions";
	
	public static final String WSKT_RFC4122 = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	public static final String PROP_WSKT_MAIN_CONTAINER = "tjws.websocket.container";
	static final boolean debugEnabled = false;
	
	/** JAVAX_WEBSOCKET_SERVER_CONTAINER */
	public static final String JAVAX_WEBSOCKET_SERVER_CONTAINER = "javax.websocket.server.ServerContainer";
	
	// static final ForkJoinPool mainPool = new ForkJoinPool();
	
	protected Selector selector;
	protected Serve serve;
	protected ExecutorService messageFlowExec;
	protected ConcurrentLinkedQueue<SimpleSession> penndingSessions;
	protected boolean useRootContainer;
	
	/**
	 * Returns the <code>useRootContainer</code> value.
	 * 
	 * @return
	 */
	protected boolean isUseRootContainer() {
		return useRootContainer;
	}
	
	/**
	 * The <code>useRootContainer</code> to be set.
	 * 
	 * @param useRootContainer
	 */
	protected void setUseRootContainer(final boolean useRootContainer) {
		this.useRootContainer = useRootContainer;
	}
	
	/**
	 * @see Acme.Serve.Serve.WebsocketProvider#init(Acme.Serve.Serve)
	 */
	@Override
	public void init(final Serve serve) {
		this.serve = serve;
		try {
			penndingSessions = new ConcurrentLinkedQueue<SimpleSession>();
			selector = Selector.open();
			final Thread threadSelector = new Thread(this, "Websocket Provider Selector");
			threadSelector.setDaemon(true);
			threadSelector.start();
		} catch (IOException ex) {
			throw new RuntimeException("Can't initialize selector, websocket functionality is disabled", ex);
		}
		
		setUseRootContainer(Boolean.getBoolean(PROP_WSKT_MAIN_CONTAINER));
		messageFlowExec = Executors.newCachedThreadPool();
	}
	
	/**
	 * @see Acme.Serve.Serve.WebsocketProvider#handshake(java.net.Socket,
	 *      java.lang.String, javax.servlet.Servlet,
	 *      javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void handshake(final Socket socket, String path, Servlet servlet, HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (socket.getChannel() == null) {
			response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Websockets implemented only with SelectorAcceptor");
			return;
		}
		
		final String webSocketVersion = request.getHeader(WSKT_VERSION);
		if (webSocketVersion == null || "13".equals(webSocketVersion.trim()) == false) {
			response.addHeader(WSKT_VERSION, "13");
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		String key = request.getHeader(WSKT_KEY);
		if (key == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sec Key is missed");
			return;
		}
		
		String contextPath;
		try {
			contextPath = (String) servlet.getClass().getMethod("getContextPath").invoke(servlet);
		} catch (Exception e) {
			if (isUseRootContainer()) {
				contextPath = "";
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "No endpoints associated with container allowed");
				return;
			}
		}
		
		SimpleServerContainer serverContainer;
		if (servlet != null) {
			serverContainer = (SimpleServerContainer) servlet.getServletConfig().getServletContext().getAttribute(JAVAX_WEBSOCKET_SERVER_CONTAINER);
		} else {
			serverContainer = (SimpleServerContainer) serve.getAttribute(JAVAX_WEBSOCKET_SERVER_CONTAINER);
		}
		
		if (serverContainer == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No end points associated with path " + path);
			return;
		}
		
		String found = null;
		int hops = -1;
		Map<String, String> foundVarMap = null;
		for (String keyContextPath : serverContainer.endpoints.keySet()) {
			Map<String, String> varMap = matchTemplate(path, contextPath + keyContextPath);
			if (varMap != null) {
				if (found == null || hops > varMap.size()) {
					found = keyContextPath;
					hops = varMap.size();
					foundVarMap = varMap;
				}
			}
		}
		
		if (found == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching endpoint found for path:" + path);
			return;
		}
		
		final ServerEndpointConfig endPointConfig = serverContainer.endpoints.get(found);
		// Objects.requireNonNull(epc.getConfigurator());
		final String webSocketOrigin = request.getHeader(WSKT_ORIGIN);
		if (endPointConfig.getConfigurator().checkOrigin(webSocketOrigin) == false) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origin check failed:" + webSocketOrigin);
			return;
		}
		endPointConfig.getConfigurator().modifyHandshake(endPointConfig, new SimpleHSRequest(request), new SimpleHSResponse(response));
		request.setAttribute("javax.websocket.server.ServerEndpointConfig", endPointConfig);
		request.setAttribute("javax.websocket.server.PathParametersMap", foundVarMap);
		
		response.setHeader(WSKT_ACEPT, getSHA1Base64(key.trim() + WSKT_RFC4122));
		response.setHeader(Serve.ServeConnection.UPGRADE, Serve.ServeConnection.WEBSOCKET);
		// resp.setHeader(Serve.ServeConnection.CONNECTION,
		// Serve.ServeConnection.KEEPALIVE + ", "
		// + Serve.ServeConnection.UPGRADE);
		// resp.addHeader(Serve.ServeConnection.CONNECTION,
		// Serve.ServeConnection.UPGRADE);
		if (serverContainer.getDefaultMaxSessionIdleTimeout() > 0) {
			response.setHeader(Serve.ServeConnection.KEEPALIVE, "timeout=" + serverContainer.getDefaultMaxSessionIdleTimeout() / 1000);
		}
		response.setStatus(HttpServletResponse.SC_SWITCHING_PROTOCOLS);
	}
	
	/**
	 * @see Acme.Serve.Serve.WebsocketProvider#upgrade(java.net.Socket,
	 *      java.lang.String, javax.servlet.Servlet,
	 *      javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void upgrade(final Socket socket, String path, Servlet servlet, HttpServletRequest request, HttpServletResponse response) throws IOException {
		SocketChannel sc = socket.getChannel();
		sc.configureBlocking(false);
		ByteChannel bc = sc;
		try {
			bc = (ByteChannel) socket.getClass().getMethod("getByteChannel").invoke(socket);
		} catch (Exception e) {
			if (debugEnabled) {
				serve.log(e, "No byte channel");
			}
		}
		
		SimpleServerContainer container = null;
		if (servlet != null) {
			container = (SimpleServerContainer) servlet.getServletConfig().getServletContext().getAttribute(JAVAX_WEBSOCKET_SERVER_CONTAINER);
		} else if (isUseRootContainer()) {
			container = (SimpleServerContainer) serve.getAttribute(JAVAX_WEBSOCKET_SERVER_CONTAINER);
		}
		
		ServerEndpointConfig epc = (ServerEndpointConfig) request.getAttribute("javax.websocket.server.ServerEndpointConfig");
		final SimpleSession simpleSession = new SimpleSession(bc, container);
		simpleSession.addMessageHandler(epc);
		simpleSession.pathParamsMap = (Map<String, String>) request.getAttribute("javax.websocket.server.PathParametersMap");
		if (request.getSession(false) != null) {
			simpleSession.id = request.getSession(false).getId();
			/*
			 * TODO this approach isn't robust and flexible, so consider as
			 * temporarily
			 */
			request.getSession(false).setAttribute("javax.websocket.server.session", new HttpSessionBindingListener() {
				
				@Override
				public void valueBound(HttpSessionBindingEvent event) {
					
				}
				
				@Override
				public void valueUnbound(HttpSessionBindingEvent event) {
					try {
						simpleSession.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Session invalidate"));
					} catch (IOException ex) {
						if (debugEnabled) {
							serve.log(ex, "At closing on session invalidation");
						}
					}
				}
			});
		} else {
			simpleSession.id = "wskt-" + serve.generateSessionId();
		}
		
		simpleSession.principal = request.getUserPrincipal();
		simpleSession.setMaxIdleTimeout(container.getDefaultMaxSessionIdleTimeout());
		simpleSession.paramsMap = new HashMap<String, List<String>>();
		for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
			simpleSession.paramsMap.put(e.getKey(), Arrays.asList(e.getValue()));
		}
		simpleSession.query = request.getQueryString();
		try {
			simpleSession.uri = new URI(request.getRequestURL().toString());
		} catch (URISyntaxException ex) {
			
		}
		String protocol = request.getHeader(WSKT_PROTOCOL);
		if (protocol != null) {
			simpleSession.subprotocol = epc.getConfigurator().getNegotiatedSubprotocol(epc.getSubprotocols(), Arrays.asList(protocol.split(",")));
		}
		if (epc.getExtensions().size() > 0) {
			// TODO maybe it should be going in handshake?
			simpleSession.extensions = epc.getConfigurator().getNegotiatedExtensions(epc.getExtensions(), parseToExtensions(request.getHeader(HandshakeRequest.SEC_WEBSOCKET_EXTENSIONS)));
			if (simpleSession.extensions.size() == 0) {
				simpleSession.close(new CloseReason(CloseReason.CloseCodes.NO_EXTENSION, ""));
				return;
			}
		}
		
		if (request instanceof ServeConnection) {
			simpleSession.serveConnection = (ServeConnection) request;
			((ServeConnection) request).spawnAsync(simpleSession);
		} else {
			serve.log("Request isn't of ServeConnection type:" + request.getClass());
		}
		
		penndingSessions.add(simpleSession);
		selector.wakeup();
		// sc.register(selector, SelectionKey.OP_READ, ss);
		// ss.open();
	}
	
	@Override
	public void destroy() {
		if (isUseRootContainer()) {
			try {
				((SimpleServerContainer) serve.getAttribute(JAVAX_WEBSOCKET_SERVER_CONTAINER)).contextDestroyed(null);
			} catch (Exception ex) {
				/* ignore me! */
				if (LogManager.isDebugEnabled()) {
					LogManager.error("Error destroying context!", ex);
				}
			}
		}
		
		messageFlowExec.shutdown();
		try {
			selector.close();
		} catch (IOException ex) {
			/* ignore me! */
			if (LogManager.isDebugEnabled()) {
				LogManager.error("Error closing selector!", ex);
			}
		}
	}
	
	/**
	 * @see Acme.Serve.Serve.WebsocketProvider#deploy(javax.servlet.ServletContext,
	 *      java.util.List)
	 */
	@Override
	public void deploy(final ServletContext servletContext, final List classPaths) {
		final SimpleServerContainer serverContainer = new SimpleServerContainer(this);
		final HashSet<ServerApplicationConfig> appConfigs = new HashSet<ServerApplicationConfig>();
		final HashSet<Class<?>> annSeps = new HashSet<Class<?>>();
		final HashSet<Class<? extends Endpoint>> endPoints = new HashSet<Class<? extends Endpoint>>();
		new FastClasspathScanner("") {
			/**
			 * @see io.github.lukehutch.fastclasspathscanner.FastClasspathScanner#getUniqueClasspathElements()
			 */
			@Override
			public List<File> getUniqueClasspathElements() {
				if (classPaths == null) {
					if (servletContext != null) {
						ClassLoader classLoader = servletContext.getClass().getClassLoader();
						if (classLoader instanceof URLClassLoader) {
							URL[] urls = ((URLClassLoader) classLoader).getURLs();
							if (urls != null && urls.length > 0) {
								ArrayList<File> result = new ArrayList<File>(urls.length);
								for (URL url : urls) {
									try {
										result.add(new File(URLDecoder.decode(url.getFile(), IOHelper.UTF_8)));
									} catch (UnsupportedEncodingException e) {
										serve.log("Can't add path component " + url + " :" + e);
									}
								}
								return result;
							}
						}
					}
					
					return super.getUniqueClasspathElements();
				}
				
				return classPaths;
			}
			
			/**
			 * @see io.github.lukehutch.fastclasspathscanner.FastClasspathScanner#getClassLoader()
			 */
			@Override
			public ClassLoader getClassLoader() {
				if (servletContext != null) {
					try {
						return (ClassLoader) servletContext.getClass().getMethod("getClassLoader").invoke(servletContext);
					} catch (Exception ex) {
						return servletContext.getClass().getClassLoader();
					}
				}
				
				return null;
			}
		}.matchClassesImplementing(ServerApplicationConfig.class, new InterfaceMatchProcessor<ServerApplicationConfig>() {
			
			@Override
			public void processMatch(Class<? extends ServerApplicationConfig> arg0) {
				try {
					appConfigs.add(arg0.newInstance());
				} catch (InstantiationException e) {
					serve.log(e, "Error at deployment");
				} catch (IllegalAccessException e) {
					serve.log(e, "Error at deployment");
				}
			}
			
		}).matchClassesWithAnnotation(ServerEndpoint.class, new ClassAnnotationMatchProcessor() {
			public void processMatch(Class<?> matchingClass) {
				annSeps.add(matchingClass);
			}
		}).matchSubclassesOf(Endpoint.class, new SubclassMatchProcessor<Endpoint>() {
			
			/**
			 * @see io.github.lukehutch.fastclasspathscanner.matchprocessor.SubclassMatchProcessor#processMatch(java.lang.Class)
			 */
			@Override
			public void processMatch(Class<? extends Endpoint> endPoint) {
				endPoints.add(endPoint);
			}
		}).scan();
		
		if (appConfigs.size() > 0) {
			for (ServerApplicationConfig serverAppConfig : appConfigs) {
				for (Class<?> endPointClass : serverAppConfig.getAnnotatedEndpointClasses(annSeps)) {
					try {
						serverContainer.addEndpoint(endPointClass);
						serve.log("Deployed ServerEndpoint:" + endPointClass);
					} catch (DeploymentException ex) {
						
					}
				}
				
				for (ServerEndpointConfig endPointConfig : serverAppConfig.getEndpointConfigs(endPoints)) {
					try {
						serverContainer.addEndpoint(endPointConfig);
						serve.log("Deployed ServerEndpointConfig:" + endPointConfig);
					} catch (DeploymentException ex) {
						
					}
				}
			}
		} else {
			for (Class<?> sEndPointClass : annSeps) {
				try {
					serverContainer.addEndpoint(sEndPointClass);
					serve.log("Deployed ServerEndpoint:" + sEndPointClass);
				} catch (DeploymentException ex) {
					
				}
			}
		}
		
		servletContext.setAttribute(JAVAX_WEBSOCKET_SERVER_CONTAINER, serverContainer);
		try {
			servletContext.addListener(serverContainer);
		} catch (Error ex) {
			// serve is still on old servlet spec
		}
	}
	
	String getSHA1Base64(String key) {
		try {
			MessageDigest cript = MessageDigest.getInstance("SHA-1");
			cript.reset();
			cript.update(key.getBytes());
			return Utils.base64Encode(cript.digest());
		} catch (NoSuchAlgorithmException nsa) {
			
		}
		return null;
	}
	
	Map<String, String> matchTemplate(String uri, String template) {
		// System.err.printf("Matching %s to %s%n", uri, template);
		Map<Integer, String> parsed = parseTemplate(template);
		Pattern p = Pattern.compile(parsed.get(0));
		Matcher m = p.matcher(uri);
		if (m.matches()) {
			HashMap<String, String> result = new HashMap<String, String>();
			for (int i = 0; i < m.groupCount(); i++)
				result.put(parsed.get(i + 1), m.group(i + 1));
			// System.err.printf("Success %s%n", result);
			return result;
		}
		// System.err.printf("unsucc %s%n", parsed);
		return null;
	}
	
	static final int s_invar = 1, s_inuri = 0;
	
	Map<Integer, String> parseTemplate(String template) {
		HashMap<Integer, String> result = new HashMap<Integer, String>();
		String regExp = "";
		int vi = 0;
		int st = s_inuri;
		String varName = null;
		for (int i = 0, n = template.length(); i < n; i++) {
			char c = template.charAt(i);
			switch (st) {
				case s_inuri:
					if (c == '/')
						regExp += c;
					else if (c == '{') {
						st = s_invar;
						varName = "";
					} else {
						// TODO check if reg exp escape needed
						regExp += c;
					}
					break;
				case s_invar:
					if (c == '}') {
						vi++;
						regExp += "((?:[a-zA-Z0-9-\\._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)";
						st = s_inuri;
						result.put(vi, varName);
					} else {
						// TODO check if valid character ALPHA, DIGIT, _, or %DD
						varName += c;
					}
					break;
			}
		}
		result.put(0, regExp);
		return result;
	}
	
	/**
	 * parses ext1;param=val param=val, ext2
	 * 
	 * @param parse
	 * @return
	 */
	List<Extension> parseToExtensions(String parse) {
		if (parse == null || parse.isEmpty())
			return Collections.emptyList();
		return Collections.emptyList();
	}
	
	@Override
	public void run() {
		while (selector.isOpen()) {
			try {
				for (SimpleSession ss = penndingSessions.poll(); ss != null; ss = penndingSessions.poll()) {
					SocketChannel sc = null;
					if (ss.channel instanceof SocketChannel)
						sc = (SocketChannel) ss.channel;
					else
						try {
							sc = (SocketChannel) ss.channel.getClass().getMethod("unwrapChannel").invoke(ss.channel);
						} catch (Exception e) {
							
						}
					if (sc != null) {
						sc.register(selector, SelectionKey.OP_READ, ss);
						ss.open();
					} else
						serve.log("Session with not proper channel will be closed");
				}
				
				int readyChannels = selector.select(1000);
				if (readyChannels == 0)
					continue;
				
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				
				Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
				
				while (keyIterator.hasNext()) {
					
					SelectionKey key = keyIterator.next();
					if (debugEnabled) {
						serve.log("key:" + key + " " + key.isValid() + " chan " + key.channel());
					}
					
					if (!key.isValid()) {
						continue;
					}
					
					if (key.isAcceptable()) {
						// a connection was accepted by a ServerSocketChannel.
					} else if (key.isConnectable()) {
						// a connection was established with a remote server.
					} else if (key.isReadable()) {
						// a channel is ready for reading
						if (key.channel().isOpen() && !messageFlowExec.isShutdown()) {
							messageFlowExec.submit(((SimpleSession) key.attachment()));
							// ((SimpleSession) key.attachment()).run();
						} else {
							if (debugEnabled) {
								serve.log("Cancel key :" + key + ", channel closed");
							}
							key.cancel();
						}
					} else if (key.isWritable()) {
						// a channel is ready for writing
						// TODO perhaps trigger flag in session too execute
						// writing bach
					}
					
					keyIterator.remove();
				}
			} catch (Exception ex) {
				serve.log("Websocket runtime problem", ex);
				if (!selector.isOpen()) {
					break;
				}
			}
		}
	}
}
