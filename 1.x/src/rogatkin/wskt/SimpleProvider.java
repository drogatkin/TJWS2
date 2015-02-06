package rogatkin.wskt;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.websocket.server.ServerEndpoint;

import rogatkin.web.WebAppServlet;

public class SimpleProvider implements WebsocketProvider, Runnable {
	public static final String WSKT_KEY = "Sec-WebSocket-Key";
	public static final String WSKT_ORIGIN = "Origin";
	public static final String WSKT_PROTOCOL = "Sec-WebSocket-Protocol";
	public static final String WSKT_VERSION = "Sec-WebSocket-Version";
	public static final String WSKT_ACEPT = "Sec-WebSocket-Accept";
	public static final String WSKT_EXTS = "Sec-WebSocket-Extensions"; // HandshakeRequest.SEC_WEBSOCKET_EXTENSIONS

	public static final String WSKT_RFC4122 = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

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
	}

	@Override
	public void upgrade(Socket socket, String path, Servlet servlet, HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {
		SocketChannel sc = socket.getChannel();
		try {
			//socket.setKeepAlive(true);
			sc.configureBlocking(false);
			SimpleSession ss = new SimpleSession(sc);
			if (servlet instanceof WebAppServlet) {
				List<Object> eps = ((WebAppServlet) servlet).endpoints;
				System.err.printf("Adding handlers %s%n", eps);
				if (eps != null)
					for (Object ep : eps) {
						// https://tools.ietf.org/html/rfc6570
						
						ServerEndpoint sepa = ep.getClass().getAnnotation(ServerEndpoint.class);
						Map<String, String> varMap = matchTemplate(path, ((WebAppServlet) servlet).getContextPath()+sepa.value());
						if (varMap != null) {
							ss.pathParamsMap = varMap;
							ss.addMessageHandler(ep);
						}
					}
			}
			if (req.getSession(false) != null)
				ss.id = req.getSession(false).getId();
			else
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
			sc.register(selector, SelectionKey.OP_READ, ss);
			//selector.wakeup();
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
				result.put(parsed.get(i+1), m.group(i+1));
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
					regExp += "((?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)";
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
