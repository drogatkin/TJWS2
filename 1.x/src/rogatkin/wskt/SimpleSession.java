package rogatkin.wskt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleSession implements Session {
	SocketChannel channel;

	ByteBuffer buf;

	SimpleSession(SocketChannel sc) {
		channel = sc;
		buf = ByteBuffer.allocate(1024 * 2);
		buf.mark();
	}

	public void run() {
		try {
			int l = channel.read(buf);
			System.err.printf("Read len %d%n", l);
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
		
		byte hb = buf.get();
		System.err.printf("hdr 0%x%n", hb);
		if ((hb & 0x80) != 0) {
		}
		byte lb = buf.get();
		boolean masked = (lb  & 0x80) != 0;
		long len = lb & 0x7f;
		if (len == 126)
			len = buf.getInt();
		else if (len == 127)
			len = buf.getLong();
		int mask = 0;
		if (masked)
			mask = buf.getInt();
		switch(hb & 0x0f) {
		case 0:
			System.err.printf("continue%n");
			break;
		case 1:
			byte tb[] = new byte[(int) len];
			buf.get(tb);
if (masked) {
	int mp = 0;
	for(int p=0; p<tb.length; p++)
		tb[p] = (byte)(tb[p] ^ (mask >> (8*(3-mp++%4)) & 255));
}
			try {
				System.err.printf("text %s%n", new String(tb, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				
			}
			break;
		case 2:
			System.err.printf("bin%n");
			break;
		case 8: // close
			try {
				channel.close();
			} catch (IOException e1) {
		
			}
			break;
		case 0x9: // ping
			break;
		case 0xa: // pong
		}
		buf.mark();
		buf.position(lim);
	}

	@Override
	public void addMessageHandler(MessageHandler arg0) throws IllegalStateException {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void close(CloseReason arg0) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Async getAsyncRemote() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Basic getBasicRemote() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public WebSocketContainer getContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return false;
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
	
	class SimpleBasic implements Basic {

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
		public void sendPing(ByteBuffer arg0) throws IOException, IllegalArgumentException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendPong(ByteBuffer arg0) throws IOException, IllegalArgumentException {
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
		public void sendBinary(ByteBuffer arg0, boolean arg1) throws IOException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendObject(Object arg0) throws IOException, EncodeException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void sendText(String arg0) throws IOException {
			channel.write(createFrame(arg0));
			
		}

		@Override
		public void sendText(String arg0, boolean arg1) throws IOException {
			// TODO Auto-generated method stub
			
		}
		
		ByteBuffer createFrame(String text) {
			return null;
		}
		
	}

}
