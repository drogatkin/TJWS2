package rogatkin.wskt;

import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		String ver =  req.getHeader(WSKT_VERSION);
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
		resp.setHeader(Serve.ServeConnection.CONNECTION, Serve.ServeConnection.UPGRADE);
		resp.setStatus(resp.SC_SWITCHING_PROTOCOLS);
	}

	@Override
	public void upgrade(Socket socket, String path, Servlet servlet, HttpServletRequest req, HttpServletResponse resp) throws ServletException {
		SocketChannel sc = socket.getChannel();
		try {
			//socket.setKeepAlive(true);
			sc.configureBlocking(false);
			SimpleSession ss = new SimpleSession(sc);
			if (servlet instanceof WebAppServlet) {
				List<Object> eps = ((WebAppServlet)servlet).endpoints;
				System.err.printf("Adding handlers %s%n", eps);
				if (eps != null) 
					for(Object ep:eps)
						ss.addMessageHandler(ep);
			}
			if (req.getSession(false) != null)
			ss.id = req.getSession(false).getId(); 
			sc.register(selector, SelectionKey.OP_READ, ss);
			//selector.wakeup();
		} catch (/*ClosedChannelException */ IOException cce) {
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

					serve.log("key:" + key);

					if (key.isAcceptable()) {
						// a connection was accepted by a ServerSocketChannel.

					} else if (key.isConnectable()) {
						// a connection was established with a remote server.

					} else if (key.isReadable()) {
						// a channel is ready for reading
						if (key.channel().isOpen())
							((SimpleSession) key.attachment()).run();
						else {
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
