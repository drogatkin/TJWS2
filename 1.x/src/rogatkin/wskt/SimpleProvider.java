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
import java.io.FileFilter;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
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
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import Acme.Utils;
import Acme.Serve.Serve;
import Acme.Serve.Serve.ServeConnection;
import Acme.Serve.Serve.WebsocketProvider;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import io.github.lukehutch.fastclasspathscanner.*;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.*;

public class SimpleProvider implements WebsocketProvider, Runnable {
	public static final String WSKT_KEY = "Sec-WebSocket-Key";
	public static final String WSKT_ORIGIN = "Origin";
	public static final String WSKT_PROTOCOL = "Sec-WebSocket-Protocol";
	public static final String WSKT_VERSION = "Sec-WebSocket-Version";
	public static final String WSKT_ACEPT = "Sec-WebSocket-Accept";
	public static final String WSKT_EXTS = "Sec-WebSocket-Extensions"; // HandshakeRequest.SEC_WEBSOCKET_EXTENSIONS

	public static final String WSKT_RFC4122 = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	public static final String PROP_WSKT_MAIN_CONTAINER = "tjws.websocket.container";

	//static final ForkJoinPool mainPool = new ForkJoinPool();

	Selector selector;
	Serve serve;
	ExecutorService messageFlowExec;
	ConcurrentLinkedQueue<SimpleSession> penndingSessions;

	boolean rootContainerUse;

	static final boolean __debugOn = false;

	@Override
	public void init(Serve s) {
		serve = s;
		try {
			penndingSessions = new ConcurrentLinkedQueue<SimpleSession>();
			selector = Selector.open();
			Thread t = new Thread(this, "websocket provider selector");
			t.setDaemon(true);
			t.start();
		} catch (IOException ioe) {
			throw new RuntimeException("Can't initialize selector, websocket functionality is disabled", ioe);
		}
		rootContainerUse = Boolean.getBoolean(PROP_WSKT_MAIN_CONTAINER);

		messageFlowExec = Executors.newCachedThreadPool();
	}

	@Override
	public void handshake(Socket socket, String path, Servlet servlet, HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		if (socket.getChannel() == null) {
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Websockets implemented only with SelectorAcceptor");
			return;
		}
		String ver = req.getHeader(WSKT_VERSION);
		if (ver == null || "13".equals(ver.trim()) == false) {
			resp.addHeader(WSKT_VERSION, "13");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String key = req.getHeader(WSKT_KEY);
		if (key == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sec Key is missed");
			return;
		}
		String contextPath;
		try {
			contextPath = (String) servlet.getClass().getMethod("getContextPath").invoke(servlet);
		} catch (Exception e) {
			if (rootContainerUse)
				contextPath = "";
			else {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No endpoints associated with container allowed");
				return;
			}
		}
		SimpleServerContainer container;
		if (servlet != null)
			container = (SimpleServerContainer) servlet.getServletConfig().getServletContext()
					.getAttribute("javax.websocket.server.ServerContainer");
		else
			container = (SimpleServerContainer) serve.getAttribute("javax.websocket.server.ServerContainer");

		if (container == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No end points associated with path " + path);
			return;
		}

		String found = null;
		int hops = -1;
		Map<String, String> foundVarMap = null;
		for (String p : container.endpoints.keySet()) {
			Map<String, String> varMap = matchTemplate(path, contextPath + p);
			if (varMap != null) {
				if (found == null || hops > varMap.size()) {
					found = p;
					hops = varMap.size();
					foundVarMap = varMap;
				}
			}
		}
		if (found == null) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching endpoint found for " + path);
			return;
		}
		ServerEndpointConfig epc = container.endpoints.get(found);
		//Objects.requireNonNull(epc.getConfigurator());
		if (epc.getConfigurator().checkOrigin(req.getHeader(WSKT_ORIGIN)) == false) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Origin check failed : " + req.getHeader(WSKT_ORIGIN));
			return;
		}
		epc.getConfigurator().modifyHandshake(epc, new SimpleHSRequest(req), new SimpleHSResponse(resp));

		req.setAttribute("javax.websocket.server.ServerEndpointConfig", epc);
		req.setAttribute("javax.websocket.server.PathParametersMap", foundVarMap);

		resp.setHeader(WSKT_ACEPT, getSHA1Base64(key.trim() + WSKT_RFC4122));
		resp.setHeader(Serve.ServeConnection.UPGRADE, Serve.ServeConnection.WEBSOCKET);
		resp.setHeader(Serve.ServeConnection.CONNECTION, Serve.ServeConnection.KEEPALIVE + ", "
				+ Serve.ServeConnection.UPGRADE);
		//resp.addHeader(Serve.ServeConnection.CONNECTION, Serve.ServeConnection.UPGRADE);
		if (container.getDefaultMaxSessionIdleTimeout() > 0)
			resp.setHeader(Serve.ServeConnection.KEEPALIVE, "timeout=" + container.getDefaultMaxSessionIdleTimeout()
					/ 1000);
		resp.setStatus(resp.SC_SWITCHING_PROTOCOLS);
	}

	@Override
	public void upgrade(Socket socket, String path, Servlet servlet, HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		SocketChannel sc = socket.getChannel();
		sc.configureBlocking(false);
		ByteChannel bc = sc;
		try {
			bc = (ByteChannel) socket.getClass().getMethod("getByteChannel").invoke(socket);
		} catch (Exception e) {
			if (__debugOn)
				serve.log(e, "No byte channel");
		}
		SimpleServerContainer container = null;
		if (servlet != null)
			container = (SimpleServerContainer) servlet.getServletConfig().getServletContext()
					.getAttribute("javax.websocket.server.ServerContainer");
		else if (rootContainerUse)
			container = (SimpleServerContainer) serve.getAttribute("javax.websocket.server.ServerContainer");
		ServerEndpointConfig epc = (ServerEndpointConfig) req
				.getAttribute("javax.websocket.server.ServerEndpointConfig");

		final SimpleSession ss = new SimpleSession(bc, container);
		ss.addMessageHandler(epc);
		ss.pathParamsMap = (Map<String, String>) req.getAttribute("javax.websocket.server.PathParametersMap");
		if (req.getSession(false) != null) {
			ss.id = req.getSession(false).getId();
			// TODO this approach isn't robust and flexible, so consider as temporarly 
			req.getSession(false).setAttribute("javax.websocket.server.session", new HttpSessionBindingListener() {

				@Override
				public void valueBound(HttpSessionBindingEvent arg0) {

				}

				@Override
				public void valueUnbound(HttpSessionBindingEvent arg0) {
					try {
						ss.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Session invalidate"));
					} catch (IOException e) {
						if (__debugOn)
							serve.log(e, "At closing on session invalidation");
					}

				}
			});
		} else
			ss.id = "wskt-" + serve.generateSessionId();
		ss.principal = req.getUserPrincipal();
		ss.setMaxIdleTimeout(container.getDefaultMaxSessionIdleTimeout());
		ss.paramsMap = new HashMap<String, List<String>>();
		for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
			ss.paramsMap.put(e.getKey(), Arrays.asList(e.getValue()));
		}
		ss.query = req.getQueryString();
		try {
			ss.uri = new URI(req.getRequestURL().toString());
		} catch (URISyntaxException e) {

		}
		String protocol = req.getHeader(WSKT_PROTOCOL);
		if (protocol != null) {
			ss.subprotocol = epc.getConfigurator().getNegotiatedSubprotocol(epc.getSubprotocols(),
					Arrays.asList(protocol.split(",")));
		}
		if (epc.getExtensions().size() > 0) {
			// TODO maybe  it should be going in handshake?
			ss.extensions = epc.getConfigurator().getNegotiatedExtensions(epc.getExtensions(),
					parseToExtensions(req.getHeader(HandshakeRequest.SEC_WEBSOCKET_EXTENSIONS)));
			if (ss.extensions.size() == 0) {
				ss.close(new CloseReason(CloseReason.CloseCodes.NO_EXTENSION, ""));
				return;
			}
		}
		if (req instanceof ServeConnection) {
			ss.conn = (ServeConnection) req;
			((ServeConnection) req).spawnAsync(ss);
		} else
			serve.log("Request isn't of ServeConnection type " + req.getClass());
		penndingSessions.add(ss);
		selector.wakeup();
		//sc.register(selector, SelectionKey.OP_READ, ss);
		//ss.open();
	}

	@Override
	public void destroy() {
		messageFlowExec.shutdown();
		try {
			selector.close();
		} catch (IOException e) {

		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deploy(final ServletContext servCtx, final List cp) {
		final SimpleServerContainer ssc = new SimpleServerContainer(this);
		final HashSet<ServerApplicationConfig> appCfgs = new HashSet<ServerApplicationConfig>();
		final HashSet<Class<?>> annSeps = new HashSet<Class<?>>();
		final HashSet<Class<? extends Endpoint>> endps = new HashSet<Class<? extends Endpoint>>();
		new FastClasspathScanner("") {
			@Override
			public List<File> getUniqueClasspathElements() {
				if (cp != null)
					return cp;
				List<File> self = super.getUniqueClasspathElements();
				self.add(new File(".")); // TODO evaluate potential security risk
				return self;
			}

			@Override
			public ClassLoader getClassLoader() {
				if (servCtx != null)
					try {
						return (ClassLoader) servCtx.getClass().getMethod("getClassLoader").invoke(servCtx);
					} catch (Exception e) {
						return servCtx.getClass().getClassLoader();
					}
				return null;
			}
		}.matchClassesImplementing(ServerApplicationConfig.class,
				new InterfaceMatchProcessor<ServerApplicationConfig>() {

					@Override
					public void processMatch(Class<? extends ServerApplicationConfig> arg0) {
						try {
							appCfgs.add(arg0.newInstance());
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

			@Override
			public void processMatch(Class<? extends Endpoint> arg0) {
				endps.add(arg0);
			}
		}).scan();

		if (appCfgs.size() > 0) {
			for (ServerApplicationConfig sac : appCfgs) {
				for (Class<?> se : sac.getAnnotatedEndpointClasses(annSeps))
					try {
						ssc.addEndpoint(se);
						serve.log("Deployed ServerEndpoint " + se);
					} catch (DeploymentException de) {

					}
				for (ServerEndpointConfig epc : sac.getEndpointConfigs(endps))
					try {
						ssc.addEndpoint(epc);
						serve.log("Deployed ServerEndpointConfig " + epc);
					} catch (DeploymentException de) {

					}
			}
		} else {
			for (Class<?> se : annSeps)
				try {
					ssc.addEndpoint(se);
					serve.log("Deployed ServerEndpoint " + se);
				} catch (DeploymentException de) {

				}
		}
		servCtx.setAttribute("javax.websocket.server.ServerContainer", ssc);
		try {
			servCtx.addListener(ssc);
		} catch (Error e) {
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
		//System.err.printf("Matching %s to %s%n", uri, template);
		Map<Integer, String> parsed = parseTemplate(template);
		Pattern p = Pattern.compile(parsed.get(0));
		Matcher m = p.matcher(uri);
		if (m.matches()) {
			HashMap<String, String> result = new HashMap<String, String>();
			for (int i = 0; i < m.groupCount(); i++)
				result.put(parsed.get(i + 1), m.group(i + 1));
			//System.err.printf("Success %s%n", result);
			return result;
		}
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
					regExp += "((?:[a-z0-9-\\._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)";
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
					if (__debugOn)
						serve.log("key:" + key + " " + key.isValid() + " chan " + key.channel());

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
							//((SimpleSession) key.attachment()).run();
						} else {
							if (__debugOn)
								serve.log("Cancel key :" + key + ", channel closed");
							key.cancel();
						}
					} else if (key.isWritable()) {
						// a channel is ready for writing
						// TODO perhaps trigger flag in session too execute writing bach
					}

					keyIterator.remove();
				}
			} catch (Exception e) {
				serve.log("Websocket runtime problem", e);
				if (!selector.isOpen())
					break;
			}
		}
	}
}
