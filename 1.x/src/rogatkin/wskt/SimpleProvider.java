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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import Acme.Utils;
import Acme.Serve.Serve;
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
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import rogatkin.web.WebAppServlet;
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

	//static final ForkJoinPool mainPool = new ForkJoinPool();
	
	Selector selector;
	Serve serve;

	@Override
	public void init(Serve s) {
		serve = s;
		try {
			selector = Selector.open();
			Thread t = new Thread(this, "websockets provider selector");
			t.setDaemon(true);
			t.start();
		} catch (IOException ioe) {
			throw new RuntimeException("Can't initialize selector, websocket functionality is disabled", ioe);
		}
	}

	@Override
	public void handshake(Socket socket, String path, Servlet servlet, HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {
		String ver = req.getHeader(WSKT_VERSION);
		if (ver == null || "13".equals(ver.trim()) == false) {
			resp.addHeader(WSKT_VERSION, "13");
			try {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			} catch (Exception e) {

				e.printStackTrace();
			}
			return;
		}

		String key = req.getHeader(WSKT_KEY);
		if (key == null)
			throw new ServletException("Sec Key is missed");
		resp.setHeader(WSKT_ACEPT, getSHA1Base64(key.trim() + WSKT_RFC4122));
		resp.setHeader(Serve.ServeConnection.UPGRADE, Serve.ServeConnection.WEBSOCKET);
		resp.setHeader(Serve.ServeConnection.CONNECTION, Serve.ServeConnection.KEEPALIVE + ", "
				+ Serve.ServeConnection.UPGRADE);
		//resp.addHeader(Serve.ServeConnection.CONNECTION, Serve.ServeConnection.UPGRADE);
		resp.setHeader(Serve.ServeConnection.KEEPALIVE, "timeout=3000");
		resp.setStatus(resp.SC_SWITCHING_PROTOCOLS);
		
		// TODO copy code from upgrade to find endpoint, and then preserve endpoint in request attribute
		// also take configurator and checkorigin, negotiate extension and protocols and finally apply modifyHandshake
	}

	@Override
	public void upgrade(Socket socket, String path, Servlet servlet, HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {
		SocketChannel sc = socket.getChannel();
		try {

			if (servlet != null) {
				SimpleServerContainer container = (SimpleServerContainer) servlet.getServletConfig()
						.getServletContext().getAttribute("javax.websocket.server.ServerContainer");
				if (container == null)
					throw new ServletException("No end points associated with path " + path);
				String found = null;
				int hops = -1;
				Map<String, String> foundVarMap = null;
				for (String p : container.endpoints.keySet()) {
					Map<String, String> varMap = matchTemplate(path, ((WebAppServlet) servlet).getContextPath() + p);
					if (varMap != null) {
						if (found == null || hops > varMap.size()) {
							found = p;
							hops = varMap.size();
							foundVarMap = varMap;
						}
					}
				}
				if (found == null)
					throw new ServletException("No matching endpoint found for " + path);
				sc.configureBlocking(false);
				ServerEndpointConfig epc = container.endpoints.get(found);
				if (epc.getConfigurator() != null
						&& epc.getConfigurator().checkOrigin(req.getHeader(WSKT_ORIGIN)) == false)
					throw new ServletException("Origin check failed : " + req.getHeader(WSKT_ORIGIN));
				final SimpleSession ss = new SimpleSession(sc, container);
				ss.addMessageHandler(epc);
				ss.pathParamsMap = foundVarMap;
				if (req.getSession(false) != null) {
					ss.id = req.getSession(false).getId();
					// TDO this approach isn't robust and flexible, so consider as temporarly 
					req.getSession(false).setAttribute("javax.websocket.server.session", new HttpSessionBindingListener() {

						@Override
						public void valueBound(HttpSessionBindingEvent arg0) {
							
						}

						@Override
						public void valueUnbound(HttpSessionBindingEvent arg0) {
							try {
								ss.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Session invalidate"));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						}});
				} else
					ss.id = "wskt-" + serve.generateSessionId();
				ss.soTimeout = socket.getSoTimeout();
				ss.paramsMap = new HashMap<String, List<String>>();
				for (Map.Entry<String, String[]> e : req.getParameterMap().entrySet()) {
					ss.paramsMap.put(e.getKey(), Arrays.asList(e.getValue()));
				}
				ss.query = req.getQueryString();
				try {
					ss.uri = new URI(req.getRequestURL().toString());
				} catch (URISyntaxException e) {

				}
				
				selector.wakeup();
				sc.register(selector, SelectionKey.OP_READ, ss);
				ss.open();
			} else
				// TODO looks also in default location
				throw new ServletException("No web application associated with " + path);
		} catch (/*ClosedChannelException */IOException cce) {
			// TODO call onError
			throw new ServletException("Can't register channel", cce);
		}
	}

	@Override
	public void destroy() {
		try {
			selector.close();
		} catch (IOException e) {

		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deploy(final HttpServlet servlet, final List cp) {
		final SimpleServerContainer ssc = new SimpleServerContainer(this);
		final HashSet<ServerApplicationConfig> appCfgs = new HashSet<ServerApplicationConfig>();
		final HashSet<Class<?>> annSeps = new HashSet<Class<?>>();
		final HashSet<Class<? extends Endpoint>> endps = new HashSet<Class<? extends Endpoint>>();
		new FastClasspathScanner("") {
			@Override
			public List<File> getUniqueClasspathElements() {
				return cp;
			}

			@Override
			public ClassLoader getClassLoader() {
				if (servlet instanceof WebAppServlet)
					return ((WebAppServlet) servlet).getClassLoader();
				else if (servlet != null)
					return servlet.getClass().getClassLoader();
				return null;
			}
		}.matchClassesImplementing(ServerApplicationConfig.class,
				new InterfaceMatchProcessor<ServerApplicationConfig>() {

					@Override
					public void processMatch(Class<? extends ServerApplicationConfig> arg0) {
						try {
							appCfgs.add(arg0.newInstance());
						} catch (InstantiationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
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

		// TODO build list of all scanned classes
		// if ServerApplicationConfig exist then use them to filter all found and produce maybe more
		// if not then just use ServerEndpoint
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
		ServletContext servCtx = servlet.getServletConfig().getServletContext(); 
		servCtx.setAttribute("javax.websocket.server.ServerContainer", ssc);
		servCtx.addListener(ssc);
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
		System.err.printf("Matching %s to %s%n", uri, template);
		Map<Integer, String> parsed = parseTemplate(template);
		Pattern p = Pattern.compile(parsed.get(0));
		Matcher m = p.matcher(uri);
		if (m.matches()) {
			HashMap<String, String> result = new HashMap<String, String>();
			for (int i = 0; i < m.groupCount(); i++)
				result.put(parsed.get(i + 1), m.group(i + 1));
			System.err.printf("Success %s%n", result);
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

	@Override
	public void run() {
		while (true) {
			try {
				int readyChannels = selector.select(1000);
				if (readyChannels == 0)
					continue;

				Set<SelectionKey> selectedKeys = selector.selectedKeys();

				Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

				while (keyIterator.hasNext()) {

					SelectionKey key = keyIterator.next();

					serve.log("key:" + key + " " + key.isValid());

					if (!key.isValid())
						continue;
					if (key.isAcceptable()) {
						// a connection was accepted by a ServerSocketChannel.

					} else if (key.isConnectable()) {
						// a connection was established with a remote server.

					} else if (key.isReadable()) {
						// a channel is ready for reading
						if (key.channel().isOpen())
							((SimpleSession) key.attachment()).run();
						else {
							serve.log("Cancel key :" + key + ", cnannel closed");
							key.cancel();
						}
					} else if (key.isWritable()) {
						// a channel is ready for writing
						// TODO perhaps trigger flag in session
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
