package rogatkin.wskt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.OnMessage;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleSession implements Session {

	enum FrameState {
		prepare, header, length, length32, length64, mask, data
	}

	boolean frameFinal;
	FrameState state;
	boolean masked;
	int mask;
	long len;
	int oper;
	byte[] data;
	int dataLen;

	SocketChannel channel;

	ByteBuffer buf;

	ArrayList<SimpleMessageHandler> handlers;

	String id;

	SimpleSession(SocketChannel sc) {
		channel = sc;
		buf = ByteBuffer.allocate(1024 * 2);
		buf.mark();
		state = FrameState.prepare;
		handlers = new ArrayList<SimpleMessageHandler>();
	}

	public void run() {
		try {
			int l = channel.read(buf);
			System.err.printf("Read len %d%n", l);
			if (l < 0)
				throw new IOException("Closed");
			parseFrame();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				channel.close();
			} catch (IOException e1) {

			}
		}
	}

	void parseFrame() {
		int lim = buf.position();
		buf.reset();
		buf.limit(lim);
		// buf.flip();
		int avail;
		boolean forceOp = false;
		readmore: while (buf.hasRemaining() || forceOp) {
			switch (state) {
			case header:
			case prepare:
				byte hb = buf.get();
				System.err.printf("hdr 0%x%n", hb);
				frameFinal = (hb & 0x80) != 0;
				oper = hb & 0x0f;
				state = FrameState.length;
				dataLen = 0;
				break;
			case length:
				byte lb = buf.get();
				masked = (lb & 0x80) != 0;
				len = lb & 0x7f;
				if (len == 126)
					state = FrameState.length32;
				else if (len == 127)
					state = FrameState.length64;
				state = masked ? FrameState.mask : FrameState.data;
				forceOp = !masked;
				System.err.printf("len %d st %s avail %d%n", len, state,
						buf.limit() - buf.position());
				break;
			case length32:
				avail = buf.limit() - buf.position();
				if (avail >= 4) {
					len = buf.getInt();
					state = masked ? FrameState.mask : FrameState.data;
					break;
				} else {
					break readmore;
				}
			case length64:
				avail = buf.limit() - buf.position();
				if (avail >= 8) {
					len = buf.getLong();
					if (len > Integer.MAX_VALUE)
						throw new IllegalArgumentException(
								"Frame length is too long");
					state = masked ? FrameState.mask : FrameState.data;
					break;
				} else
					break readmore;
			case mask:
				avail = buf.limit() - buf.position();
				if (avail >= 4) {
					mask = buf.getInt();
					state = FrameState.data;
					// break;
				} else
					break readmore;
			case data:
				System.err.printf("data oper 0%x len %d%n", oper, len);
				switch (oper) {
				case 0:
					// TODO provide accum content flag
					// break;

				case 1:
					avail = buf.limit() - buf.position();
					if (dataLen == 0) {
						if (avail >= len) {
							data = new byte[(int) len];
							buf.get(data);
							dataLen = (int) len;
						} else {
							data = new byte[(int) avail];
							buf.get(data);
							dataLen = avail;
						}
					} else {
						// if (dataLen+avail >= len) {
						int sl = (int) Math.min(avail, len - dataLen);
						data = Arrays.copyOf(data, dataLen + sl);
						buf.get(data, dataLen, sl);
						dataLen += sl;
						// } else {
						// data = Arrays.copyOf(data, avail);
						// }
					}
					if (dataLen == len) { // all data
						state = FrameState.header;
						if (masked) {
							int mp = 0;
							for (int p = 0; p < data.length; p++)
								data[p] = (byte) (data[p] ^ (mask >> (8 * (3 - mp++ % 4)) & 255));
						}
						// TODO send onMessage
						String message = null;
						try {
							message = new String(data, "UTF-8");
							System.err.printf("text (%s) %d fl: %b%n", message,
									buf.limit() - buf.position(), frameFinal);
						} catch (UnsupportedEncodingException e) {
							message = new String(data);
						}
						if (frameFinal) {
							for (SimpleMessageHandler mh : handlers) {
								System.err.printf("process text %s%n", mh);
								mh.processText(message);
								if (mh.getResult() != null) {
									// TODO send it
								}
							}
						}
					} else
						break readmore;
					break;
				case 2:
					System.err.printf("bin%n");
					state = FrameState.header;
					break;
				case 8: // close
					System.err.printf("close() %n");
					try {
						channel.close();
						// TODO notify onClose()
					} catch (IOException e1) {

					}
					state = FrameState.header;
					break;
				case 0x9: // ping
					state = FrameState.header;
					break;
				case 0xa: // pong
					state = FrameState.header;
				}
				forceOp = false;
			}
		}
		System.err.printf("Exited %b%n", buf.hasRemaining());
		if (buf.hasRemaining()) {
			buf.mark();
			buf.position(lim);
			buf.limit(buf.capacity());
		} else {
			buf.clear();
			buf.mark();
		}
	}

	void addMessageHandler(Object arg0) throws IllegalStateException {
		handlers.add(new SimpleMessageHandler(arg0));
	}

	@Override
	public void addMessageHandler(MessageHandler arg0)
			throws IllegalStateException {
		addMessageHandler((Object) arg0);
	}

	@Override
	public void close() throws IOException {
		close(null);

	}

	@Override
	public void close(CloseReason reason) throws IOException {
		for (SimpleMessageHandler mh : handlers) {
			mh.processClose(reason);
		}

	}

	@Override
	public Async getAsyncRemote() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Basic getBasicRemote() {
		return new SimpleBasic();
	}

	@Override
	public WebSocketContainer getContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int getMaxBinaryMessageBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getMaxIdleTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxTextMessageBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<MessageHandler> getMessageHandlers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Extension> getNegotiatedExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNegotiatedSubprotocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Session> getOpenSessions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getPathParameters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProtocolVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getQueryString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<String>> getRequestParameterMap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getRequestURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getUserProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isOpen() {
		return channel != null && channel.isOpen();
	}

	@Override
	public boolean isSecure() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeMessageHandler(MessageHandler arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMaxBinaryMessageBufferSize(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMaxIdleTimeout(long arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMaxTextMessageBufferSize(int arg0) {
		// TODO Auto-generated method stub

	}

	class SimpleMessageHandler implements MessageHandler {
		Method onText;
		int[] mapOnText;
		ServerEndpoint sepa;
		Object endpoint;
		Object result;

		SimpleMessageHandler(Object ep) {
			Class<?> epc = ep.getClass();
			sepa = epc.getAnnotation(ServerEndpoint.class);
			if (sepa == null)
				throw new IllegalArgumentException(
						"No annotation ServerEndpoint");
			endpoint = ep;
			Method[] ms = epc.getDeclaredMethods();
			for (Method m : ms) {
				if (m.getAnnotation(OnMessage.class) != null) {
					int pi = 0;
					mapOnText = new int[3];
					for (Class<?> t : m.getParameterTypes()) {
						if (t == String.class) {
							mapOnText[0] = pi + 1;
							onText = m;
						} else if (t.isAssignableFrom(Session.class))
							mapOnText[1] = pi + 1;
						else if (t == boolean.class)
							mapOnText[2] = pi + 1;
						pi++;
					}
				}
			}

		}

		void processBinary(byte[] b) {

		}

		void processText(String t) {
			if (onText != null) {
				Class<?>[] paramts = onText.getParameterTypes();
				Object[] params = new Object[paramts.length];
				for (int pi = 0; pi < params.length; pi++)
					switch (mapOnText[pi] - 1) {
					case 0:
						params[pi] = t;
						break;
					case 1:
						params[pi] = SimpleSession.this;
						break;
					default:
						params[pi] = null;
					}
				try {
					System.err.printf("Called %s%n", t);
					result = onText.invoke(endpoint, params);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		void processClose(CloseReason reason) {

		}

		Object getResult() {
			return result;
		}
	}

	class SimpleBasic implements Basic {
		Random rn = new Random();

		@Override
		public void flushBatch() throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean getBatchingAllowed() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void sendPing(ByteBuffer arg0) throws IOException,
				IllegalArgumentException {
			// TODO Auto-generated method stub

		}

		@Override
		public void sendPong(ByteBuffer arg0) throws IOException,
				IllegalArgumentException {
			// TODO Auto-generated method stub

		}

		@Override
		public void setBatchingAllowed(boolean arg0) throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public OutputStream getSendStream() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Writer getSendWriter() throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void sendBinary(ByteBuffer arg0) throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public void sendBinary(ByteBuffer arg0, boolean arg1)
				throws IOException {
			// TODO Auto-generated method stub

		}

		@Override
		public void sendObject(Object arg0) throws IOException, EncodeException {
			// TODO Auto-generated method stub

		}

		@Override
		public void sendText(String arg0) throws IOException {
			int lc = channel.write(createFrame(arg0));
			System.err.printf("%d%n", lc);
		}

		@Override
		public void sendText(String arg0, boolean arg1) throws IOException {
			// TODO Auto-generated method stub

		}

		ByteBuffer createFrame(String text) {
			byte[] mb = null;
			try {
				mb = text == null || text.length() == 0 ? new byte[0] : text
						.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				mb = text.getBytes();
			}
			mask = rn.nextInt();
			int bl = 6;
			boolean masked = false;
			byte lm = (byte) (masked ? 0x80 : 0x00);
			if (mb.length > 125) {
				bl += 4;
				lm |= 126;
				if (mb.length > Integer.MAX_VALUE) { // Never case
					bl += 4;
					lm |= 127;
				}
			} else
				lm |= mb.length;
			ByteBuffer bb = ByteBuffer.allocate(bl + mb.length);
			bb.put((byte) 0x81).put(lm);
			if (mb.length > 125)
				bb.putInt(bl);
			if (masked) {
				bb.putInt(mask);
				int mp = 0;
				for (int p = 0; p < mb.length; p++)
					mb[p] = (byte) (mb[p] ^ (mask >> (8 * (3 - mp++ % 4)) & 255));
			}
			bb.put(mb);
			bb.flip();
			System.err.printf("Send frame %s of %d %s 0%x%xn", text,
					bb.remaining(), bb, bb.get(0), bb.get(1));
			return bb;
		}

	}

}
