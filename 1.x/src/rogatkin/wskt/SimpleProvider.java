package rogatkin.wskt;

import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
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

public class SimpleProvider implements WebsocketProvider {
	public static final String WSKT_KEY = "Sec-WebSocket-Key";
	public static final String WSKT_ORIGIN = "Origin";
	public static final String WSKT_PROTOCOL = "Sec-WebSocket-Protocol";
	public static final String WSKT_VERSION = "Sec-WebSocket-Version";
	public static final String WSKT_ACEPT = "Sec-WebSocket-Accept";
	public static final String WSKT_EXTS = "Sec-WebSocket-Extensions";

	public static final String WSKT_RFC4122 = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	@Override
	public void init(Map properties) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handshake(Socket socket, Servlet servlet, HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {
		String key = req.getHeader(WSKT_KEY);
		if (key == null)
			throw new ServletException("Sec Key is missed");
		resp.setHeader(WSKT_ACEPT, getSHA1Base64(key.trim() + WSKT_RFC4122));
		resp.setHeader(Serve.ServeConnection.UPGRADE, Serve.ServeConnection.WEBSOCKET);
		resp.setHeader(Serve.ServeConnection.CONNECTION, Serve.ServeConnection.UPGRADE);
		resp.setStatus(resp.SC_SWITCHING_PROTOCOLS);

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

}
