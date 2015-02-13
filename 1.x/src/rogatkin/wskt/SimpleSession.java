package rogatkin.wskt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpointConfig;

public class SimpleSession implements Session {

	enum FrameState {
		prepare, header, length, length32, length64, mask, data
	}

	// ///// TODO encapsulate in a parser class
	ByteBuffer buf;
	boolean frameFinal;
	FrameState state;
	boolean masked;
	int mask;
	long len;
	int oper;
	byte[] data;
	int dataLen;
	boolean frameText;
	byte[] completeData;
	// /////////////////////////////////
	SocketChannel channel;

	HashSet<SimpleMessageHandler> handlers;

	String id;

	int binBufSize = 1024 * 2;

	int soTimeout;
	Map<String, List<String>> paramsMap;
	Map<String, String> pathParamsMap;
	String query;
	URI uri;
	Principal principal;
	SimpleServerContainer container;
	private SimpleBasic basicRemote;
	private ServerEndpointConfig endpointConfig;

	SimpleSession(SocketChannel sc, SimpleServerContainer c) {
		channel = sc;
		container = c;
		buf = ByteBuffer.allocate(binBufSize);
		buf.mark();
		state = FrameState.prepare;
		handlers = new HashSet<SimpleMessageHandler>();
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
				close();
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
				else
					state = masked ? FrameState.mask : FrameState.data;
				forceOp = !masked;
				System.err.printf("len %d st %s avail %d%n", len, state, buf.limit() - buf.position());
				break;
			case length32:
				avail = buf.remaining();
				if (avail >= 2) {
					// buf.order(ByteOrder.BIG_ENDIAN);
					len = buf.getShort();
					state = masked ? FrameState.mask : FrameState.data;
					break;
				} else {
					break readmore;
				}
			case length64:
				avail = buf.remaining();
				if (avail >= 8) {
					len = buf.getLong();
					if (len > Integer.MAX_VALUE)
						throw new IllegalArgumentException("Frame length is too long");
					state = masked ? FrameState.mask : FrameState.data;
					break;
				} else
					break readmore;
			case mask:
				avail = buf.remaining();
				if (avail >= 4) {
					mask = buf.getInt();
					state = FrameState.data;
					// break;
				} else
					break readmore;
			case data:
				System.err.printf("data oper 0%x len %d%n", oper, len);
				boolean contin = oper == 0;
				if (contin)
					oper = frameText ? 1 : 2;
				switch (oper) {
				case 0:
					// TODO provide accum content flag
					// break;
					throw new IllegalStateException();
				case 1:
					avail = buf.remaining();// buf.limit() - buf.position();
					frameText = true;
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
						if (!contin)
							completeData = data;
						else {
							completeData = Arrays.copyOf(completeData, completeData.length + data.length);
							System.arraycopy(data, 0, completeData, completeData.length - data.length, data.length);
						}
						for (SimpleMessageHandler mh : handlers) {
							System.err.printf("process text part %s%n", mh);
							mh.processText(bytesToString(data), frameFinal);
						}
						if (frameFinal) {
							for (SimpleMessageHandler mh : handlers) {
								System.err.printf("process text all %s%n", mh);
								mh.processText(bytesToString(completeData));
							}
						}
					} else
						break readmore;
					break;
				case 2:
					frameText = false;
					System.err.printf("bin%n");
					avail = buf.remaining();// buf.limit() - buf.position();

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
						boolean partBinConsumed = false;
						for (SimpleMessageHandler mh : handlers) {
							System.err.printf("process text %s%n", mh);
							partBinConsumed |= mh.processBinary(data, frameFinal);
						}
						if (!contin)
							completeData = data;
						else {
							completeData = Arrays.copyOf(completeData, completeData.length + data.length);
							System.arraycopy(data, 0, completeData, completeData.length - data.length, data.length);
						}
						if (frameFinal) {
							for (SimpleMessageHandler mh : handlers) {
								System.err.printf("process text %s%n", mh);
								mh.processBinary(completeData);
							}
						}
					} else
						break readmore;
					break;
				case 8: // close
					CloseReason cr = null;
					avail = buf.remaining();
					state = FrameState.header;
					if (len >= 0)
						if (avail >= 2) {
							data = new byte[(int) len];
							buf.get(data);
							if (masked) {
								int mp = 0;
								for (int p = 0; p < data.length; p++)
									data[p] = (byte) (data[p] ^ (mask >> (8 * (3 - mp++ % 4)) & 255));
							}
							short reason = (short) ((data[1]&255)+(data[0]<<8));
							//System.err.printf("close code %x %x %d%n", data[0], data[1], reason);
							cr = new CloseReason(CloseCodes.getCloseCode(reason), "");
							// TODO clean all len
						}
					System.err.printf("close(%s) %n", cr);
					try {
						// TODO find out reason
						// TODO send close frame as well
						close(cr);
						channel.close();
					} catch (IOException e1) {

					}
					state = FrameState.header;
					break;
				case 0x9: // ping
					avail = buf.remaining();
					if (avail >= len) {
						state = FrameState.header;
						data = new byte[(int) len];
						buf.get(data);
						dataLen = data.length;
						// TODO send pong
					}
					break;
				case 0xa: // pong
					avail = buf.remaining();
					if (avail >= len) {
						state = FrameState.header;
						data = new byte[(int) len];
						buf.get(data);
						dataLen = data.length;
						for (SimpleMessageHandler mh : handlers) {
							System.err.printf("process text %s%n", mh);
							mh.processPong(data);
						}
					}
					break;
				default:
					System.err.printf(" Invalid frame op 0%x, len %d%n", oper, len);
					try {
						close(new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, "Unsupported frame operation:"
								+ oper));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break readmore;

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

	String bytesToString(byte[] b) {
		try {
			return new String(b, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return new String(data);
		}
	}

	void addMessageHandler(ServerEndpointConfig arg0) throws IllegalStateException {
		if (endpointConfig != null)
			throw new IllegalStateException("Only one endpoint can be associated with session/connection");
		endpointConfig = arg0;
		handlers.add(new SimpleMessageHandler());
	}

	@Override
	public void addMessageHandler(MessageHandler arg0) throws IllegalStateException {
		handlers.add(new SimpleMessageHandler(arg0));
	}

	@Override
	public void close() throws IOException {
		close(null);

	}

	@Override
	public void close(CloseReason reason) throws IOException {
		for (SimpleMessageHandler mh : handlers) {
			mh.processClose(reason);
			// 
			mh.destroy();
		}
		if (basicRemote != null) {
			basicRemote.destroy();
			basicRemote = null;
		}
	}

	@Override
	public Async getAsyncRemote() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Basic getBasicRemote() {
		// TODO investigate if possible to use from different threads
		if (basicRemote == null)
			basicRemote = new SimpleBasic(); 
		return basicRemote;
	}

	@Override
	public WebSocketContainer getContainer() {
		return container;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public int getMaxBinaryMessageBufferSize() {
		return binBufSize;
	}

	@Override
	public long getMaxIdleTimeout() {
		return soTimeout;
	}

	@Override
	public int getMaxTextMessageBufferSize() {
		return binBufSize;
	}

	@Override
	public Set<MessageHandler> getMessageHandlers() {
		HashSet<MessageHandler> result = new HashSet<MessageHandler>();
		
		return result;
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
		return pathParamsMap;
	}

	@Override
	public String getProtocolVersion() {
		return "13";
	}

	@Override
	public String getQueryString() {
		return query;
	}

	@Override
	public Map<String, List<String>> getRequestParameterMap() {
		return paramsMap;
	}

	@Override
	public URI getRequestURI() {
		return uri;
	}

	@Override
	public Principal getUserPrincipal() {
		return principal;
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
		binBufSize = arg0;
		// TODO apply to the buffer
	}

	@Override
	public void setMaxIdleTimeout(long arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMaxTextMessageBufferSize(int arg0) {
		binBufSize = arg0;
		// TODO apply to the buffer
	}

	static class ParameterEntry {
		ParameterEntry(int type) {
			sourceType = type;
		}

		ParameterEntry() {

		}

		int sourceType;
		String sourceName;
		Decoder decoder;
	}

	class SimpleMessageHandler implements MessageHandler {
		private static final int TEXT = 1;
		private static final int BIN = 2;
		private static final int BOOLEAN = 3;
		private static final int SESSION_PARAM = 4;
		private static final int PATH_PARAM = 5;
		private static final int ENDPOINTCONFIG_PARAM = 6;
		private static final int CLOSEREASON_PARAM = 7;
		private static final int THROWABLE_PARAM = 8;
		private static final int PONG = 9;
		private static final int DECODER = 10;

		Method onText, onBin, onPong;
		Method onOpen;
		Method onClose;
		Method onError;
		boolean partText, partBin;

		ParameterEntry[] paramMapText, paramMapOpen, paramMapClose, paramMapError, paramMapPong, paramMapBin;

		Object endpoint;
		Object result;
		//ServerEndpointConfig endpointConfig;

		SimpleMessageHandler() {
			Class<?> epc = endpointConfig.getEndpointClass();
			try {
				endpoint = epc.newInstance();
				endpointConfig.getConfigurator().getEndpointInstance(epc);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Method[] ms = epc.getDeclaredMethods();
			for (Method m : ms) {
				if (m.getAnnotation(OnMessage.class) != null) {
					int pi = 0;
					Annotation[][] annots = m.getParameterAnnotations();
					Class<?>[] params = m.getParameterTypes();
					ParameterEntry[] pmap = new ParameterEntry[params.length];
					boolean partReq = false, primeText = false, primeBin = false;
					for (Class<?> t : params) {
						pmap[pi] = new ParameterEntry();
						if (t == String.class) {
							PathParam pp = (PathParam) getFromList(annots[pi], PathParam.class);
							if (pp == null) {
								pmap[pi].sourceType = TEXT;
								if (onText != null)
									throw new IllegalArgumentException("Only one text messages handler is allowed");
								onText = m;
								paramMapText = pmap;
								primeText = true;
							} else {
								// if (pathParamsMap.containsKey(pp.value()) ==
								// false)
								// throw new
								// IllegalArgumentException("Not supported variable "
								// + pp.value());
								pmap[pi].sourceName = pp.value();
								pmap[pi].sourceType = PATH_PARAM;
							}
						} else if (t.isAssignableFrom(Session.class)) {
							pmap[pi].sourceType = SESSION_PARAM;
						} else if (t == boolean.class) {
							pmap[pi].sourceType = BOOLEAN;
							partReq = true;
						} else if (t == byte[].class) {
							pmap[pi].sourceType = BIN;
							if (onBin != null)
								throw new IllegalArgumentException("Only one binary messages handler is allowed");
							onBin = m;
							paramMapBin = pmap;
							primeBin = true;
						} else if (t == Reader.class) {
						} else if (t == ByteBuffer.class) {
						} else if (t == InputStream.class) {
						} else if (t == PongMessage.class) {
							pmap[pi].sourceType = PONG;
							onPong = m;
							paramMapPong = pmap;
						} else {
							if (endpointConfig.getDecoders() != null) {
								for (Class<? extends Decoder> dc : endpointConfig.getDecoders()) {
									Method dm = null;
									try {
										// TODO consider if more robust to use dc.getInterfaces();
										// or simply instantiate and then check?
										dm = dc.getDeclaredMethod("decode", String.class);
									} catch (NoSuchMethodException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									} catch (SecurityException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
									if (dm != null && t == dm.getReturnType()) {
										// TODO since several decoders can match, then add all and do willDecode(String) at runtime
										try {
											pmap[pi].decoder = dc.newInstance();
											pmap[pi].decoder.init(endpointConfig);
											pmap[pi].sourceType = DECODER;
											if (onText != null)
												throw new IllegalArgumentException(
														"Only one text messages handler is allowed");
											onText = m;
											paramMapText = pmap;
										} catch (InstantiationException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										} catch (IllegalAccessException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								}
							}
						}
						pi++;
					}
					partText = primeText && partReq;
					partBin = primeBin && partReq;
				} else if (m.getAnnotation(OnOpen.class) != null) {
					onOpen = m;
					paramMapOpen = creatParamMap(onOpen);
				} else if (m.getAnnotation(OnError.class) != null) {
					onError = m;
					paramMapError = creatParamMap(onError);
				} else if (m.getAnnotation(OnClose.class) != null) {
					onClose = m;
					paramMapClose = creatParamMap(onClose);
				}
			}

		}

		void destroy() {
			destroyDecoders(paramMapText);
			destroyDecoders(paramMapBin);
		}

		void destroyDecoders(ParameterEntry[] pmap) {
			if (pmap != null)
				for (ParameterEntry pe : pmap) {
					if (pe.decoder != null)
						pe.decoder.destroy();
				}
		}

		SimpleMessageHandler(MessageHandler mh) {
			if (mh instanceof MessageHandler.Partial) {
				Class<?> mhc = mh.getClass();
				if (!initPatialText(mhc, mh))
					if (!initPatialBin2(mhc, mh))
						initPatialBin1(mhc, mh);
			} else if (mh instanceof MessageHandler.Whole) {

			}

		}

		boolean initPatialText(Class<?> hc, MessageHandler mh) {
			try {
				Method m = hc.getDeclaredMethod("onMessage", String.class, boolean.class);
				onText = m;
				endpoint = mh;
				paramMapText = new ParameterEntry[2];
				paramMapText[0] = new ParameterEntry(TEXT);
				paramMapText[1] = new ParameterEntry(BOOLEAN);
				return true;
			} catch (NoSuchMethodException e) {
			} catch (SecurityException e) {
			}
			return false;
		}

		boolean initPatialBin1(Class<?> hc, MessageHandler mh) {
			try {
				Method m = mh.getClass().getDeclaredMethod("onMessage", byte[].class, boolean.class);
				onBin = m;
				endpoint = mh;
				paramMapText = new ParameterEntry[2];
				paramMapText[0] = new ParameterEntry(BIN);
				paramMapText[1] = new ParameterEntry(BOOLEAN);
				return true;
			} catch (NoSuchMethodException e) {
			} catch (SecurityException e) {
			}
			return false;
		}

		boolean initPatialBin2(Class<?> hc, MessageHandler mh) {
			try {
				Method m = mh.getClass().getDeclaredMethod("onMessage", ByteBuffer.class, boolean.class);
				onText = m;
				endpoint = mh;
				paramMapText = new ParameterEntry[2];
				paramMapText[0] = new ParameterEntry(BIN);
				paramMapText[1] = new ParameterEntry(BOOLEAN);
				return true;
			} catch (NoSuchMethodException e) {
			} catch (SecurityException e) {
			}
			return false;
		}

		Annotation getFromList(Annotation[] annots, Class<?> targAnnot) {
			if (annots != null)
				for (Annotation a : annots)
					if (a.annotationType() == targAnnot)
						return a;
			return null;
		}

		ParameterEntry[] creatParamMap(Method m) {
			Annotation[][] annots = m.getParameterAnnotations();
			Class<?>[] params = m.getParameterTypes();
			ParameterEntry[] pmap = new ParameterEntry[params.length];
			int pi = 0;
			for (Class<?> t : params) {
				pmap[pi] = new ParameterEntry();
				if (t.isAssignableFrom(Session.class)) {
					pmap[pi].sourceType = SESSION_PARAM;
				} else if (t.isAssignableFrom(Session.class)) {
					pmap[pi].sourceType = ENDPOINTCONFIG_PARAM;
				} else if (t == CloseReason.class) { // TODO exclude from onOpen
					pmap[pi].sourceType = CLOSEREASON_PARAM;
				} else if (t == String.class) {
					PathParam pp = (PathParam) getFromList(annots[pi], PathParam.class);
					if (pp == null)
						throw new IllegalArgumentException("String parameter isn't supported");
					// if (pathParamsMap.containsKey(pp.value()) == false)
					// throw new
					// IllegalArgumentException("Not supported variable " +
					// pp.value());
					pmap[pi].sourceName = pp.value();
					pmap[pi].sourceType = PATH_PARAM;
				} else if (t == Throwable.class) {
					pmap[pi].sourceType = THROWABLE_PARAM;
				} else
					throw new IllegalArgumentException("Argument of " + t + " isn't allowed for a parameter");
				pi++;
			}
			return pmap;
		}

		void processPong(final byte[] b) {
			if (onPong != null) {
				Class<?>[] paramts = onText.getParameterTypes();
				Object[] params = new Object[paramts.length];
				for (int pi = 0; pi < params.length; pi++)
					switch (paramMapPong[pi].sourceType) {
					case PONG:
						params[pi] = new PongMessage() {

							@Override
							public ByteBuffer getApplicationData() {
								// TODO Auto-generated method stub
								return ByteBuffer.wrap(b);
							}

						};
						break;
					case PATH_PARAM:
						params[pi] = pathParamsMap.get(paramMapPong[pi].sourceName);
						break;
					}
				try {
					result = onPong.invoke(endpoint, params);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		boolean processBinary(byte[] b, boolean f) {
			if (onBin != null && partBin) {
				return true;
			}
			return false;
		}

		void processBinary(byte[] b) {

		}

		boolean processText(String t, boolean f) {
			if (onText != null && partText) {
				return true;
			}
			return false;
		}

		void processText(String t) {
			if (onText != null) {
				Class<?>[] paramts = onText.getParameterTypes();
				Object[] params = new Object[paramts.length];
				for (int pi = 0; pi < params.length; pi++)
					switch (paramMapText[pi].sourceType) {
					case TEXT:
						params[pi] = t;
						break;
					case SESSION_PARAM:
						params[pi] = SimpleSession.this;
						break;
					case PATH_PARAM:
						params[pi] = pathParamsMap.get(paramMapText[pi].sourceName);
						break;
					case BOOLEAN:
						params[pi] = null;
						break;
					case DECODER:
						if (((Decoder.Text) paramMapText[pi].decoder).willDecode(t))
							try {
								params[pi] = ((Decoder.Text) paramMapText[pi].decoder).decode(t);
							} catch (DecodeException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								processError(e);
							}
						break;
					default:
						System.err.printf("Unmapped text  parameter %d%n", pi);
						params[pi] = null;
					}
				try {
					System.err.printf("Called %s%n", t);
					result = onText.invoke(endpoint, params);
					if (result != null) {
						if (result instanceof String)
							getBasicRemote().sendText(result.toString());
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else 
				System.err.printf("No handler for text mess %s%n", t);
		}

		void processOpen() {
			if (onOpen != null) {
				Class<?>[] paramts = onOpen.getParameterTypes();
				Object[] params = new Object[paramts.length];
				if (paramMapOpen != null)
					for (int pi = 0; pi < params.length; pi++)
						switch (paramMapOpen[pi].sourceType) {
						case SESSION_PARAM:
							params[pi] = SimpleSession.this;
							break;
						case ENDPOINTCONFIG_PARAM:
							params[pi] = endpointConfig;
							break;
						case PATH_PARAM:
							params[pi] = pathParamsMap.get(paramMapOpen[pi].sourceName);
							break;
						default:
							System.err.printf("Unmapped open parameter %d%n", pi);
							params[pi] = null;
						}

				try {
					System.err.printf("Called %s%n", "on open");
					result = onOpen.invoke(endpoint, params);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		void processClose(CloseReason reason) {
			if (onClose != null) {
				Class<?>[] paramts = onClose.getParameterTypes();
				Object[] params = new Object[paramts.length];
				if (paramMapClose != null)
					for (int pi = 0; pi < params.length; pi++)
						switch (paramMapClose[pi].sourceType) {
						case SESSION_PARAM:
							params[pi] = SimpleSession.this;
							break;
						case ENDPOINTCONFIG_PARAM:
							params[pi] = endpointConfig;
							break;
						case PATH_PARAM:
							params[pi] = pathParamsMap.get(paramMapClose[pi].sourceName);
							break;
						default:
							System.err.printf("Unmapped open parameter %d%n", pi);
							params[pi] = null;
						}
				try {
					System.err.printf("Called %s%n", "on close");
					result = onClose.invoke(endpoint, params);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		void processError(Throwable error) {
			if (onError != null) {
				Class<?>[] paramts = onError.getParameterTypes();
				Object[] params = new Object[paramts.length];
				if (paramMapError != null)
					for (int pi = 0; pi < params.length; pi++)
						switch (paramMapError[pi].sourceType) {
						case SESSION_PARAM:
							params[pi] = SimpleSession.this;
							break;
						case THROWABLE_PARAM:
							params[pi] = error;
							break;
						case PATH_PARAM:
							params[pi] = pathParamsMap.get(paramMapError[pi].sourceName);
							break;
						default:
							System.err.printf("Unmapped open parameter %d%n", pi);
							params[pi] = null;
						}
				try {
					System.err.printf("Called %s%n", "on close");
					result = onError.invoke(endpoint, params);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		Object getResult() {
			return result;
		}
	}

	class SimpleBasic implements Basic {
		Random rn = new Random();
		boolean masked;
		boolean cont;
		HashMap<Class<?>, Encoder> encoders;
		boolean batchMode;

		@Override
		public void flushBatch() throws IOException {
			if (batchMode ) {
				
			}
		}

		@Override
		public boolean getBatchingAllowed() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void sendPing(ByteBuffer arg0) throws IOException, IllegalArgumentException {
			if (arg0 == null || arg0.remaining() > 125)
				throw new IllegalArgumentException("Control fame data length can't exceed 125");
			int lc = channel.write(createFrame(true, arg0));
		}

		@Override
		public void sendPong(ByteBuffer arg0) throws IOException, IllegalArgumentException {
			if (arg0 == null || arg0.remaining() > 125)
				throw new IllegalArgumentException("Control fame data length can't exceed 125");
			int lc = channel.write(createFrame(false, arg0));
		}

		@Override
		public void setBatchingAllowed(boolean arg0) throws IOException {
			batchMode = arg0;
		}

		@Override
		public OutputStream getSendStream() throws IOException {
			return new ByteArrayOutputStream() {

				@Override
				public void close() throws IOException {
					sendBinary(ByteBuffer.wrap(toByteArray()));
					super.close();
				}

			};
		}

		@Override
		public Writer getSendWriter() throws IOException {
			return new OutputStreamWriter(getSendStream());
		}

		@Override
		public void sendBinary(ByteBuffer arg0) throws IOException {
			int lc = channel.write(createFrame(arg0, true, true));
			System.err.printf("Sent %d bytes binarry%n", lc);
		}

		@Override
		public void sendBinary(ByteBuffer arg0, boolean arg1) throws IOException {
			int lc = channel.write(createFrame(arg0, arg1, !cont));
			cont = !arg1;
		}

		/**
		 * Sends a custom developer object, blocking until it has been
		 * transmitted. Containers will by default be able to encode java
		 * primitive types and their object equivalents, otherwise the developer
		 * must have provided an encoder for the object type in the endpoint
		 * configuration. A developer-provided encoder for a Java primitive type
		 * overrides the container default encoder.
		 *
		 * @param data
		 *            the object to be sent.
		 * @throws IOException
		 *             if there is a communication error sending the message
		 *             object.
		 * @throws EncodeException
		 *             if there was a problem encoding the message object into
		 *             the form of a native websocket message.
		 * @throws IllegalArgumentException
		 *             if the data parameter is {@code null}
		 */
		@Override
		public void sendObject(Object arg0) throws IOException, EncodeException {
			if (encoders == null)
				initEncoders();
			Encoder ec = encoders.get(arg0.getClass());
			if (ec == null)
				throw new EncodeException(arg0, "There is no encoder");
			if (ec instanceof Encoder.Text) {
				sendText(((Encoder.Text)ec).encode(arg0));
			} else if (ec instanceof Encoder.TextStream) {
			} else if (ec instanceof Encoder.Binary) {
			} else if (ec instanceof Encoder.BinaryStream) {
				
			} else
				throw new EncodeException(ec, "The encoder doesn't rpvide proper encding method");

		}

		private void initEncoders() {
			encoders = new HashMap<Class<?>, Encoder>(); 
			for (Class<? extends Encoder> ec :endpointConfig.getEncoders()) {
				for (Method em :ec.getDeclaredMethods()) {
					System.err.printf("method %s returns %s%n", em.getName(), em.getReturnType());
					if (!"encode".equals(em.getName()))
						continue;
					Class<?> rt = em.getReturnType();
					//Type[] pts = em.getGenericParameterTypes();
					Class<?>[] pts = em.getParameterTypes();
					if (rt == String.class) {
						if (pts.length == 1) {
							try {
								Encoder e = ec.newInstance();
								e.init(endpointConfig);
								encoders.put(pts[0], e);
							} catch (InstantiationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} //else 
						//throw new IllegalArgumentException ("Only text encoders are implemented - "+rt+" for method "+em.getName());
				}
			}
			
		}

		@Override
		public void sendText(String arg0) throws IOException {
			int lc = channel.write(createFrame(arg0, true, true));
			System.err.printf("%d%n", lc);
		}

		@Override
		public void sendText(String arg0, boolean arg1) throws IOException {
			int lc = channel.write(createFrame(arg0, arg1, !cont));
			cont = !arg1;
		}

		ByteBuffer createFrame(ByteBuffer bbp, boolean fin, boolean first) {
			System.err.printf("Sending %d bytes as final %b as first %b%n", bbp.remaining(), fin, first);
			ByteBuffer bb = prepreFrameHeader((byte) 2, bbp.remaining(), fin, first);
			bb.put(bbp).flip();
			System.err.printf("Send frame %s of %d %s 0%x %x %x %x %n", bbp, bb.remaining(), bb, bb.get(0), bb.get(1), bb.get(2), bb.get(3));
			return bb;
		}

		ByteBuffer createFrame(boolean ping, ByteBuffer bbp) {
			ByteBuffer bb = prepreFrameHeader((byte) (ping ? 0x9 : 0xa), bbp.remaining(), true, true);
			bb.put(bbp).flip();
			return bb;
		}

		ByteBuffer prepreFrameHeader(byte cmd, long len, boolean fin, boolean first) {
			if (cmd != 1 && cmd != 2) {
				fin = first = true;
			}
			int bl = 2;
			byte lm = 0;
			if (masked) {
				mask = rn.nextInt();
				bl += 4;
				lm = (byte) 0x80;
			}
			if (len > 125) {
				bl += 2;
				lm |= 126;
				if (len > Short.MAX_VALUE) { // Never case
					bl += 6;
					lm |= 127;
				}
			} else
				lm |= len;
			ByteBuffer bb = ByteBuffer.allocate(bl + (int) len);
			byte hb = (byte) (fin ? 0x80 : 0);
			if (first)
				hb |= cmd;
			bb.put(hb).put(lm);
			if (len > 125)
				if (len <= Short.MAX_VALUE)
					bb.putShort((short) len);
				else
					bb.putLong(len);
			return bb;
		}

		ByteBuffer createFrame(String text, boolean fin, boolean first) {
			cont = !fin && !first;
			byte[] mb = null;
			try {
				mb = text == null || text.length() == 0 ? new byte[0] : text.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				mb = text.getBytes();
			}

			ByteBuffer bb = prepreFrameHeader((byte) 1, (long) mb.length, fin, first);
			if (masked) {
				bb.putInt(mask);
				int mp = 0;
				for (int p = 0; p < mb.length; p++)
					mb[p] = (byte) (mb[p] ^ (mask >> (8 * (3 - mp++ % 4)) & 255));
			}
			bb.put(mb);
			bb.flip();
			System.err.printf("Send frame %s of %d %s 0%x%xn", text, bb.remaining(), bb, bb.get(0), bb.get(1));
			return bb;
		}
		
		void destroy() {
			if (encoders != null)
				for (Encoder ec:encoders.values())
					ec.destroy();
		}

	}

	void open() {
		for (SimpleMessageHandler mh : handlers) {
			mh.processOpen();
		}

	}

}
