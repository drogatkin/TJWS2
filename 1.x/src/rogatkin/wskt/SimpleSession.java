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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

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
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpointConfig;

import Acme.Serve.Serve.AsyncCallback;
import Acme.Serve.Serve.ServeConnection;

public class SimpleSession implements Session, AsyncCallback, Runnable {

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
	ByteChannel channel;
	ServeConnection conn; // temporary

	HashSet<SimpleMessageHandler> handlers;

	String id;

	long idleTimeout;
	Map<String, List<String>> paramsMap;
	Map<String, String> pathParamsMap;
	String query;
	URI uri;
	Principal principal;
	SimpleServerContainer container;
	private SimpleBasic basicRemote;
	ServerEndpointConfig endpointConfig;
	String subprotocol;
	List<Extension> extensions;

	static final boolean __debugOn = false;
	static final boolean __parseDebugOn = __debugOn;

	SimpleSession(ByteChannel sc, SimpleServerContainer c) {
		channel = sc;
		container = c;
		container.addSession(this);
		buf = ByteBuffer.allocate(c.getDefaultMaxBinaryMessageBufferSize());
		buf.mark();
		state = FrameState.prepare;
		handlers = new HashSet<SimpleMessageHandler>();
	}

	public synchronized void run() {
		try {
			conn.extendAsyncTimeout(-1);
			int l = channel.read(buf);
			conn.extendAsyncTimeout(getMaxIdleTimeout());
			if (__parseDebugOn)
				container.log("Read len %d", l);
			if (l < 0)
				throw new IOException("Closed");
			else if (l > 0)
				parseFrame();
		} catch (IOException e) {
			container.log(e, "Frame reading");
			try {
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
				if (__parseDebugOn)
					container.log("hdr 0%x", hb);
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
				if (__parseDebugOn)
					container.log("len %d st %s avail %d", len, state, buf.limit() - buf.position());
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
				if (__parseDebugOn)
					container.log("data oper 0%x len %d", oper, len);
				boolean contin = false;
				// if (contin)
				// oper = frameText ? 1 : 2;

				avail = buf.remaining();
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
					int sl = (int) Math.min(avail, len - dataLen);
					data = Arrays.copyOf(data, dataLen + sl);
					buf.get(data, dataLen, sl);
					dataLen += sl;
				}
				if (dataLen == len) { // all data
					state = FrameState.header;
					if (masked) {
						int mp = 0;
						for (int p = 0; p < data.length; p++)
							data[p] = (byte) (data[p] ^ (mask >> (8 * (3 - mp++ % 4)) & 255));
					}
					switch (oper) {
					case 1:
						frameText = true;
						break;
					case 2:
						frameText = false;
						break;
					}
					switch (oper) {
					case 8: // close
						CloseReason cr = null;
						if (data != null && data.length >= 2) {
							short reason = (short) ((data[1] & 255) + (data[0] << 8));
							String msg;
							if (data.length > 2) {
								msg = bytesToString(Arrays.copyOfRange(data, 2, data.length));
							} else
								msg = "";
							cr = new CloseReason(CloseCodes.getCloseCode(reason), msg);
						}
						if (__parseDebugOn)
							container.log("close(%s)", cr);
						try {
							close(cr, data); // echo is handled by close
						} catch (IOException e1) {

						}
						break;
					case 0x9: // ping
						try {
							((SimpleBasic) getBasicRemote()).sendEcho((byte) 10, data);
						} catch (IOException e1) {
							container.log(e1, "Problem in returning pong");
						}
						break;
					case 0xa: // pong
						for (SimpleMessageHandler mh : handlers) {
							// System.err.printf("process pong %s%n", mh);
							mh.processPong(data);
						}
						break;
					case 0:
						contin = true;
					case 1:
					case 2:
						boolean partConsumed = false;
						if (frameText) {
							for (SimpleMessageHandler mh : handlers) {
								partConsumed |= mh.processText(bytesToString(data), frameFinal);
								if (__parseDebugOn)
									container.log("process text part %s - %b", mh, partConsumed);
							}
						} else {
							for (SimpleMessageHandler mh : handlers) {
								partConsumed |= mh.processBinary(data, frameFinal);
								if (__parseDebugOn)
									container.log("process binary part %s - %b", mh, partConsumed);
							}
						}
						if (partConsumed == false) {
							if (!contin)
								completeData = data;
							else {
								completeData = Arrays.copyOf(completeData, completeData.length + data.length);
								System.arraycopy(data, 0, completeData, completeData.length - data.length, data.length);
							}
							if (frameFinal) {
								if (frameText) {
									for (SimpleMessageHandler mh : handlers) {
										mh.processText(bytesToString(completeData));
										if (__parseDebugOn)
											container.log("process text %s", mh);
									}
								} else {
									for (SimpleMessageHandler mh : handlers) {
										mh.processBinary(completeData);
										if (__parseDebugOn)
											container.log("process binary %s", mh);
									}
								}
							}
						}
						break;
					default:
						container.log("Invalid frame op 0%x, len %d", oper, len);
						try {
							close(new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, "Unsupported frame operation:"
									+ oper));
						} catch (IOException e) {
							container.log(e, "Exception at closing");
						}
						break readmore;
					}
				}
				forceOp = false;
			}
		}
		if (__parseDebugOn)
			container.log("Exited %b", buf.hasRemaining());
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
		SimpleMessageHandler smh = new SimpleMessageHandler(arg0);
		for (SimpleMessageHandler h : handlers) {
			if (h.sameType(smh))
				throw new IllegalStateException("Only one handler of each type allowed");
		}
		handlers.add(new SimpleMessageHandler(arg0));

	}

	@Override
	public void close() throws IOException {
		close(null);

	}

	@Override
	public void close(CloseReason reason) throws IOException {
		close(reason, null);
	}

	public void close(CloseReason reason, byte[] b) throws IOException {
		//new Exception("attempt close already closed").printStackTrace();
		if (isOpen() == false) {
			return;
		}
		try {
			for (SimpleMessageHandler mh : handlers) {
				mh.processClose(reason);
				//
				mh.destroy();
			}
			if (b == null)
				if (reason == null)
					b = new byte[0];
				else {
					ByteBuffer bb = ByteBuffer.allocate(2 + reason.getReasonPhrase().length());
					bb.putShort((short) reason.getCloseCode().getCode());
					if (reason.getReasonPhrase().length() > 0)
						bb.put(reason.getReasonPhrase().getBytes());
					bb.flip();
					b = new byte[bb.remaining()];
					bb.put(b);
				}
			if (basicRemote != null) {
				try {
					basicRemote.sendEcho((byte) 8, b);
				} catch (Exception e) {
					// eat it, can be closed
				}
				basicRemote.destroy();
				basicRemote = null;
			}
		} finally {
			if (__debugOn)
				container.log("Channel closed");
			container.removeSession(this);
			channel.close();
			channel = null;
		}
	}

	@Override
	public Async getAsyncRemote() {
		// TODO maybe cache
		return new SimpleAsync();
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
		return buf.capacity();
	}

	@Override
	public long getMaxIdleTimeout() {
		return idleTimeout;
	}

	@Override
	public int getMaxTextMessageBufferSize() {
		return buf.capacity();
	}

	@Override
	public Set<MessageHandler> getMessageHandlers() {
		HashSet<MessageHandler> result = new HashSet<MessageHandler>();
		for (SimpleMessageHandler smh : handlers) {
			if (smh.endpoint instanceof MessageHandler)
				result.add((MessageHandler) smh.endpoint);
		}
		return result;
	}

	@Override
	public List<Extension> getNegotiatedExtensions() {
		return extensions;
	}

	@Override
	public String getNegotiatedSubprotocol() {
		return subprotocol;
	}

	@Override
	public Set<Session> getOpenSessions() {
		HashSet<Session> result = new HashSet<Session>();
		for (SimpleSession ss : container.sessions) {
			if (endpointConfig == ss.endpointConfig)
				result.add(ss);
		}
		return result;
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
		return endpointConfig.getUserProperties();
	}

	@Override
	public boolean isOpen() {
		return channel != null && channel.isOpen();
	}

	@Override
	public boolean isSecure() {
		return conn.isSecure();
	}

	@Override
	public void removeMessageHandler(MessageHandler arg0) {
		for (SimpleMessageHandler smh : handlers) {
			if (smh.endpoint == arg0) {
				handlers.remove(smh);
				break;
			}
		}
	}

	@Override
	public void setMaxBinaryMessageBufferSize(int arg0) {
		if (buf.capacity() < arg0 && buf.remaining() == 0)
			buf = ByteBuffer.allocate(arg0);
	}

	@Override
	public void setMaxIdleTimeout(long arg0) {
		idleTimeout = arg0;
	}

	@Override
	public void setMaxTextMessageBufferSize(int arg0) {
		if (buf.capacity() < arg0 && buf.remaining() == 0)
			buf = ByteBuffer.allocate(arg0);
	}

	static class ParameterEntry {
		ParameterEntry(int type) {
			sourceType = type;
		}

		ParameterEntry() {

		}

		int sourceType;
		String sourceName;
		Set<Decoder> decoder;
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
		private static final int READER = 11;
		private static final int BYTEBUF = 12;
		private static final int INPUT = 13;

		Method onText, onBin, onPong;
		Method onOpen;
		Method onClose;
		Method onError;
		boolean partText, partBin;

		ParameterEntry[] paramMapText, paramMapOpen, paramMapClose, paramMapError, paramMapPong, paramMapBin;

		Object endpoint;
		Object result;

		// ServerEndpointConfig endpointConfig;

		SimpleMessageHandler() {
			Class<?> epc = endpointConfig.getEndpointClass();
			try {
				endpoint = epc.newInstance();
				endpointConfig.getConfigurator().getEndpointInstance(epc);
			} catch (InstantiationException e) {
				container.log(e, "Can't instantiate end point for %s", epc);
			} catch (IllegalAccessException e) {
				container.log(e, "Can't instantiate end point for %s", epc);
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
							if (onBin != null)
								throw new IllegalArgumentException("Only one binary messages handler is allowed");
							pmap[pi].sourceType = BIN;
							onBin = m;
							paramMapBin = pmap;
							primeBin = true;
						} else if (t.isAssignableFrom(Reader.class)) {
							if (onText != null)
								throw new IllegalArgumentException("Only one text messages handler is allowed");
							onText = m;
							pmap[pi].sourceType = READER;
							paramMapText = pmap;
						} else if (t == ByteBuffer.class) {
							if (onBin != null)
								throw new IllegalArgumentException("Only one binary messages handler is allowed");
							onBin = m;
							pmap[pi].sourceType = BYTEBUF;
							paramMapBin = pmap;
						} else if (t == InputStream.class) {
							if (onBin != null)
								throw new IllegalArgumentException("Only one binary messages handler is allowed");
							onBin = m;
							pmap[pi].sourceType = INPUT;
							paramMapBin = pmap;
						} else if (t == PongMessage.class) {
							pmap[pi].sourceType = PONG;
							onPong = m;
							paramMapPong = pmap;
						} else {
							if (endpointConfig.getDecoders() != null) {
								Set<Decoder> decoders = matchDecoders(t, Decoder.Text.class, String.class);
								if (decoders.size() > 0) {
									if (onText != null)
										throw new IllegalStateException("Only one text messages handler is allowed");
									onText = m;
									pmap[pi].decoder = decoders;
									pmap[pi].sourceType = DECODER;
									paramMapText = pmap;
								} else {
									decoders = matchDecoders(t, Decoder.TextStream.class, Reader.class);
									if (decoders.size() > 0) {
										if (onText != null)
											throw new IllegalStateException("Only one text messages handler is allowed");
										onText = m;
										pmap[pi].decoder = decoders;
										pmap[pi].sourceType = DECODER;
										paramMapText = pmap;
									} else {
										decoders = matchDecoders(t, Decoder.Binary.class, ByteBuffer.class);
										if (decoders.size() > 0) {
											if (onBin != null)
												throw new IllegalArgumentException(
														"Only one binary messages handler is allowed");
											onBin = m;
											pmap[pi].decoder = decoders;
											pmap[pi].sourceType = DECODER;
											paramMapBin = pmap;
										} else {
											decoders = matchDecoders(t, Decoder.BinaryStream.class, InputStream.class);
											if (decoders.size() > 0) {
												if (onBin != null)
													throw new IllegalArgumentException(
															"Only one binary messages handler is allowed");
												onBin = m;
												pmap[pi].decoder = decoders;
												pmap[pi].sourceType = DECODER;
												paramMapBin = pmap;
											}
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

		boolean sameType(SimpleMessageHandler smh) {
			if (smh.onText != null)
				if (onText != null)
					return partText == smh.partText;
			if (smh.onPong != null)
				return onPong != null;
			if (smh.onBin != null)
				if (onBin != null)
					return partBin == smh.partBin;
			return false;
		}

		void destroy() {
			destroyDecoders(paramMapText);
			destroyDecoders(paramMapBin);
		}

		void destroyDecoders(ParameterEntry[] pmap) {
			if (pmap != null)
				for (ParameterEntry pe : pmap) {
					if (pe.decoder != null)
						for (Decoder decoder : pe.decoder)
							decoder.destroy();
				}
		}

		SimpleMessageHandler(MessageHandler mh) {
			initHandler(mh);
		}

		void initHandler(MessageHandler mh) {
			boolean partial = mh instanceof MessageHandler.Partial;
			Class<?> mhc = mh.getClass();
			for (Method m : mhc.getDeclaredMethods()) {
				if (!"onMessage".equals(m.getName()))
					continue;
				Class<?>[] pts = m.getParameterTypes();
				switch (pts.length) {
				case 2:
					if (!partial)
						continue;
					if (pts[1] != boolean.class)
						continue;
				case 1:
					if (pts[0] == String.class) {
						onText = m;
						endpoint = mh;
						paramMapText = new ParameterEntry[pts.length];
						paramMapText[0] = new ParameterEntry(TEXT);
						if (partial) {
							paramMapText[1] = new ParameterEntry(BOOLEAN);
							partText = true;
						}
					} else if (pts[0].isAssignableFrom(Reader.class)) {
						onText = m;
						endpoint = mh;
						paramMapText = new ParameterEntry[pts.length];
						paramMapText[0] = new ParameterEntry(READER);
						if (partial) {
							paramMapText[1] = new ParameterEntry(BOOLEAN);
							partText = true;
						}
					} else if (pts[0] == PongMessage.class) {
						if (partial)
							throw new IllegalArgumentException("Pong message handler has to be Whole");
						onPong = m;
						endpoint = mh;
						paramMapPong = new ParameterEntry[1];
						paramMapPong[0] = new ParameterEntry(PONG);
					} else if (pts[0] == byte[].class) {
						onBin = m;
						endpoint = mh;
						paramMapBin = new ParameterEntry[pts.length];
						paramMapBin[0] = new ParameterEntry(BIN);
						if (partial) {
							paramMapBin[1] = new ParameterEntry(BOOLEAN);
							partBin = true;
						}
					} else if (pts[0] == ByteBuffer.class) {
						onBin = m;
						endpoint = mh;
						paramMapBin = new ParameterEntry[pts.length];
						paramMapBin[0] = new ParameterEntry(BYTEBUF);
						if (partial) {
							paramMapBin[1] = new ParameterEntry(BOOLEAN);
							partBin = true;
						}
					} else {
						if (endpointConfig.getDecoders() == null)
							break;
						Set<Decoder> decs = matchDecoders(pts[0], Decoder.Text.class, String.class);
						if (decs.size() > 0) {
							onText = m;
							endpoint = mh;
							paramMapText = new ParameterEntry[pts.length];
							paramMapText[0] = new ParameterEntry(DECODER);
							paramMapText[0].decoder = decs;
							if (partial) {
								paramMapText[1] = new ParameterEntry(BOOLEAN);
								partText = true;
							}
							break;
						}
						decs = matchDecoders(pts[0], Decoder.TextStream.class, Reader.class);
						if (decs.size() > 0) {
							onText = m;
							endpoint = mh;
							paramMapText = new ParameterEntry[pts.length];
							paramMapText[0] = new ParameterEntry(DECODER);
							paramMapText[0].decoder = decs;
							if (partial) {
								paramMapText[1] = new ParameterEntry(BOOLEAN);
								partText = true;
							}
							break;
						}
						decs = matchDecoders(pts[0], Decoder.Binary.class, ByteBuffer.class);
						if (decs.size() > 0) {
							onBin = m;
							endpoint = mh;
							paramMapBin = new ParameterEntry[pts.length];
							paramMapBin[0] = new ParameterEntry(DECODER);
							paramMapBin[0].decoder = decs;
							if (partial) {
								paramMapBin[1] = new ParameterEntry(BOOLEAN);
								partBin = true;
							}
							break;
						}
						decs = matchDecoders(pts[0], Decoder.BinaryStream.class, InputStream.class);
						if (decs.size() > 0) {
							onBin = m;
							endpoint = mh;
							paramMapBin = new ParameterEntry[pts.length];
							paramMapBin[0] = new ParameterEntry(DECODER);
							paramMapBin[0].decoder = decs;
							if (partial) {
								paramMapBin[1] = new ParameterEntry(BOOLEAN);
								partBin = true;
							}
							break;
						}
					}
					break;
				}
			}
		}

		Set<Decoder> matchDecoders(Class<?> type, Class<? extends Decoder> dt, Class<?> param) {
			HashSet<Decoder> result = new HashSet<Decoder>();
			for (Class<? extends Decoder> dc : endpointConfig.getDecoders()) {
				if (dt.isAssignableFrom(dc)) {
					Method dm;
					try {
						dm = dc.getDeclaredMethod("decode", param);
						if (dm.getReturnType() == type) {
							Decoder decoder = dc.newInstance();
							decoder.init(endpointConfig);
							result.add(decoder);
						}
					} catch (Exception e) {
						if (__debugOn)
							container.log(e, "Problem of adding decoder %s", dc);
						else
							container.log("Problem %s at adding decoder %s", e, dc);
					}
				}
			}
			return result;
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
					container.log(e, "Error in sending pong");
					processError(e);
				}
			}
		}

		boolean processBinary(byte[] b, boolean f) {
			if (onBin != null && partBin) {
				processBinary(b, f);
				return true;
			}
			return false;
		}

		void processBinary(byte[] b) {
			processBinary(b, null);
		}

		void processBinary(byte[] b, Boolean part) {
			if (onBin != null) {
				Class<?>[] paramts = onBin.getParameterTypes();
				Object[] params = new Object[paramts.length];
				for (int pi = 0; pi < params.length; pi++)
					switch (paramMapBin[pi].sourceType) {
					case BIN:
						params[pi] = b;
						break;
					case SESSION_PARAM:
						params[pi] = SimpleSession.this;
						break;
					case PATH_PARAM:
						params[pi] = pathParamsMap.get(paramMapBin[pi].sourceName);
						break;
					case BOOLEAN:
						params[pi] = part;
						break;
					case DECODER:
						ByteBuffer bb = ByteBuffer.wrap(b);
						for (Decoder decoder : paramMapBin[pi].decoder)
							if (((Decoder.Binary) decoder).willDecode(bb))
								try {
									params[pi] = ((Decoder.Binary) decoder).decode(bb);
									break;
								} catch (DecodeException e) {
									if (__debugOn)
										container.log(e, "in decoding...");
									processError(e);
								}
						break;
					case BYTEBUF:
						params[pi] = ByteBuffer.wrap(b);
						break;
					case INPUT:
						params[pi] = new ByteArrayInputStream(b);
						break;
					default:
						container.log("Unmapped binary parameter %d at calling %s", pi, onBin);
						params[pi] = null;
					}
				try {
					if (__debugOn)
						container.log("Called %s", b);
					result = onBin.invoke(endpoint, params);
					if (result != null) {
						if (result instanceof String)
							getBasicRemote().sendText(result.toString());
					}
				} catch (Exception e) {
					container.log(e, "Error in sending binary data");
					processError(e);
				}
			} else
				container.log("No handler for binary message %s", b);
		}

		boolean processText(String t, boolean f) {
			if (onText != null && partText) {
				processText(t, f);
				return true;
			}
			return false;
		}

		void processText(String t) {
			processText(t, null);
		}

		void processText(String t, Boolean part) {
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
						params[pi] = part;
						break;
					case DECODER:
						for (Decoder decoder : paramMapText[pi].decoder)
							if (((Decoder.Text) decoder).willDecode(t))
								try {
									params[pi] = ((Decoder.Text) decoder).decode(t);
									break;
								} catch (DecodeException e) {
									if (__debugOn)
										container.log(e, "in decoding...");
									processError(e);
								}
						break;
					case READER:
						params[pi] = new StringReader(t);
						break;
					default:
						container.log("Unmapped text parameter %d at call %s", pi, onText);
						params[pi] = null;
					}
				try {
					if (__debugOn)
						container.log("Called %s", t);
					result = onText.invoke(endpoint, params);
					if (result != null) {
						if (result instanceof String)
							getBasicRemote().sendText(result.toString());
					}
				} catch (Exception e) {
					container.log(e, "Exception in text message processing");
				}
			} else
				container.log("No handler for text message %s", t);
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
							container.log("Unmapped open parameter %d at call %s", pi, onOpen);
							params[pi] = null;
						}

				try {
					if (__debugOn)
						container.log("Called %s", "on open");
					result = onOpen.invoke(endpoint, params);
				} catch (Exception e) {
					container.log(e, "Exception in onOpen");
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
						case CLOSEREASON_PARAM:
							params[pi] = reason;
							break;
						default:
							container.log("Unmapped close parameter %d at call %s", pi, onClose);
							params[pi] = null;
						}
				try {
					if (__debugOn)
						container.log("Called %s", "on close");
					result = onClose.invoke(endpoint, params);
				} catch (Exception e) {
					container.log(e, "Exception in onClose");
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
							container.log("Unmapped error parameter %d at call %s", pi, onError);
							params[pi] = null;
						}
				try {
					if (__debugOn)
						container.log("Called %s", "on error");
					result = onError.invoke(endpoint, params);
				} catch (Exception e) {
					container.log(e, "Exception in onError");
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
		ByteBuffer[] batchBuffer;

		@Override
		public void flushBatch() throws IOException {
			if (batchBuffer != null) {
				for (ByteBuffer cb : batchBuffer) {
					sendBuffer(cb, true);
				}
				batchBuffer = new ByteBuffer[0];
			}
		}

		@Override
		public boolean getBatchingAllowed() {
			return batchBuffer != null;
		}

		@Override
		public void sendPing(ByteBuffer arg0) throws IOException, IllegalArgumentException {
			if (arg0 == null || arg0.remaining() > 125)
				throw new IllegalArgumentException("Control frame data length can't exceed 125");
			sendBuffer(createFrame(true, arg0), true);
		}

		@Override
		public void sendPong(ByteBuffer arg0) throws IOException, IllegalArgumentException {
			if (arg0 == null || arg0.remaining() > 125)
				throw new IllegalArgumentException("Control frame data length can't exceed 125");
			sendBuffer(createFrame(false, arg0), true);
		}

		@Override
		public void setBatchingAllowed(boolean arg0) throws IOException {
			batchBuffer = new ByteBuffer[0];
		}

		@Override
		public OutputStream getSendStream() throws IOException {
			return new ByteArrayOutputStream() {
				boolean closed;

				@Override
				public void close() throws IOException {
					if (closed)
						throw new IOException("Stream is already closed");
					flush();
					sendBinary(ByteBuffer.wrap(toByteArray()));
					super.close();
					closed = true;
				}
			};
		}

		@Override
		public Writer getSendWriter() throws IOException {
			return new StringWriter() {
				boolean closed;

				@Override
				public void close() throws IOException {
					if (closed)
						throw new IOException("Writer is already closed");
					flush();
					sendText(toString());
					super.close();
					closed = true;
				}

			};
		}

		@Override
		public void sendBinary(ByteBuffer arg0) throws IOException {
			sendBuffer(createFrame(arg0, true, true));
		}

		void sendBuffer(ByteBuffer bb) throws IOException {
			sendBuffer(bb, false);
		}

		void sendBuffer(ByteBuffer bb, boolean nobatch) throws IOException {
			if (batchBuffer != null && !nobatch) {
				batchBuffer = Arrays.copyOf(batchBuffer, batchBuffer.length + 1);
				batchBuffer[batchBuffer.length - 1] = bb;
			} else {
				conn.extendAsyncTimeout(-1);
				try {
					for (int len = bb.remaining(); len > 0; len = bb.remaining()) {
						int lc = channel.write(bb);
						if (lc <= 0)
							throw new IOException("Can't sent complete buffer, remmaining " + len);
					}
				} finally {
					conn.extendAsyncTimeout(getMaxIdleTimeout());
				}
			}
		}

		@Override
		public void sendBinary(ByteBuffer arg0, boolean arg1) throws IOException {
			sendBuffer(createFrame(arg0, arg1, !cont));
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
				sendText(((Encoder.Text) ec).encode(arg0));
			} else if (ec instanceof Encoder.TextStream) {
				((Encoder.TextStream) ec).encode(arg0, getSendWriter()); // TODO
																			// perhaps
																			// close
																			// writer
			} else if (ec instanceof Encoder.Binary) {
				sendBinary(((Encoder.Binary) ec).encode(arg0));
			} else if (ec instanceof Encoder.BinaryStream) {
				((Encoder.BinaryStream) ec).encode(arg0, getSendStream());
			} else
				throw new EncodeException(ec, "The encoder doesn't provide proper encding method");

		}

		private void initEncoders() {
			encoders = new HashMap<Class<?>, Encoder>();
			for (Class<? extends Encoder> ec : endpointConfig.getEncoders()) {
				for (Method em : ec.getDeclaredMethods()) {
					if (__debugOn)
						container.log("method %s returns %s", em.getName(), em.getReturnType());
					if (!"encode".equals(em.getName()))
						continue;
					Class<?> rt = em.getReturnType();
					// Type[] pts = em.getGenericParameterTypes();
					Class<?>[] pts = em.getParameterTypes();
					if (rt == String.class) {
						if (pts.length == 1) {
							try {
								Encoder e = ec.newInstance();
								e.init(endpointConfig);
								encoders.put(pts[0], e);
							} catch (Exception e) {
								if (__debugOn)
									container.log(e, "at encoder creation");
							}
						}
					} // else
						// throw new IllegalArgumentException
						// ("Only text encoders are implemented - "+rt+" for method "+em.getName());
				}
			}

		}

		@Override
		public void sendText(String arg0) throws IOException {
			sendBuffer(createFrame(arg0, true, true));
		}

		@Override
		public void sendText(String arg0, boolean arg1) throws IOException {
			sendBuffer(createFrame(arg0, arg1, !cont));
			cont = !arg1;
		}

		void sendEcho(byte op, byte[] data) throws IOException {
			if (data.length > 125)
				throw new IllegalArgumentException("Control frame data length can't exceed 125");
			ByteBuffer bb = prepreFrameHeader(op, data.length, true, true);
			bb.put(data).flip();
			sendBuffer(bb, true);
		}

		ByteBuffer createFrame(ByteBuffer bbp, boolean fin, boolean first) {
			//System.err.printf("Sending %d bytes as final %b as first %b%n",
			//	bbp.remaining(), fin, first);
			ByteBuffer bb = prepreFrameHeader((byte) 2, bbp.remaining(), fin, first);
			bb.put(bbp).flip();
			//System.err.printf("Send frame %s of %d %s 0%x %x %x %x %n", bbp,
			//	bb.remaining(), bb, bb.get(0), bb.get(1), bb.get(2),
			//bb.get(3));
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
			if (__debugOn)
				container.log("Send frame %s of %d %s hdr: 0%x%x", text, bb.remaining(), bb, bb.get(0), bb.get(1));
			return bb;
		}

		void destroy() {
			if (encoders != null)
				for (Encoder ec : encoders.values())
					ec.destroy();
		}

	}

	void open() {
		for (SimpleMessageHandler mh : handlers) {
			mh.processOpen();
		}
	}

	class SimpleAsync implements Async {
		long sendTimeout;

		SimpleAsync() {
			getBasicRemote();
		}

		@Override
		public void flushBatch() throws IOException {
			basicRemote.flushBatch();
		}

		@Override
		public boolean getBatchingAllowed() {
			return basicRemote.getBatchingAllowed();
		}

		@Override
		public void sendPing(ByteBuffer arg0) throws IOException, IllegalArgumentException {
			basicRemote.sendPing(arg0);

		}

		@Override
		public void sendPong(ByteBuffer arg0) throws IOException, IllegalArgumentException {
			basicRemote.sendPong(arg0);
		}

		@Override
		public void setBatchingAllowed(boolean arg0) throws IOException {
			basicRemote.setBatchingAllowed(arg0);
		}

		@Override
		public long getSendTimeout() {
			return sendTimeout;
		}

		@Override
		public Future<Void> sendBinary(final ByteBuffer arg0) {
			return container.asyncService.submit(new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					synchronized (basicRemote) { // TODO it can be not required
													// since socket will
													// synchronize
						basicRemote.sendBinary(arg0);
					}
					return null;
				}
			});
		}

		@Override
		public void sendBinary(final ByteBuffer arg0, final SendHandler arg1) {
			container.asyncService.execute(new Runnable() {

				@Override
				public void run() {
					try {
						basicRemote.sendBinary(arg0);
						arg1.onResult(new SendResult());
					} catch (IOException e) {
						arg1.onResult(new SendResult(e));
					}

				}
			});
		}

		@Override
		public Future<Void> sendObject(final Object arg0) {
			return container.asyncService.submit(new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					basicRemote.sendObject(arg0);
					return null;
				}
			});
		}

		@Override
		public void sendObject(final Object arg0, final SendHandler arg1) {
			container.asyncService.execute(new Runnable() {

				@Override
				public void run() {
					try {
						basicRemote.sendObject(arg0);
						arg1.onResult(new SendResult());
					} catch (Exception e) {
						arg1.onResult(new SendResult(e));
					}

				}
			});
		}

		@Override
		public Future<Void> sendText(final String arg0) {
			return container.asyncService.submit(new Callable<Void>() {

				@Override
				public Void call() throws Exception {
					basicRemote.sendText(arg0);
					return null;
				}
			});
		}

		@Override
		public void sendText(final String arg0, final SendHandler arg1) {
			container.asyncService.execute(new Runnable() {

				@Override
				public void run() {
					try {
						basicRemote.sendText(arg0);
						arg1.onResult(new SendResult());
					} catch (IOException e) {
						arg1.onResult(new SendResult(e));
					}

				}
			});
		}

		@Override
		public void setSendTimeout(long arg0) {
			if (arg0 < 0)
				return;
			sendTimeout = arg0;
		}

	}

	@Override
	public void notifyTimeout() {
		if (isOpen())
			try {
				close();
			} catch (IOException e) {
				if (__debugOn)
					container.log(e, "close() at timeout");
			}
	}

	@Override
	public long getTimeout() {
		return idleTimeout;
	}

	public String getRemoteAddr() {
		return conn.getRemoteAddr();
	}
}
