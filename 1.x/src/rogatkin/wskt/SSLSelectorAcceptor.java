/* tjws - SSLSelectorAcceptor.java
 * Copyright (C) 1999-2015 Dmitriy Rogatkin.  All rights reserved.
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  
 *  Visit http://tjws.sourceforge.net to get the latest information
 *  about Rogatkin's products.                                                        
 *                  
 *  Created on Feb 18, 2015
 *  @author dmitriy
 */
package rogatkin.wskt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import Acme.Utils;
import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;

public class SSLSelectorAcceptor extends SSLAcceptor {
	private ServerSocketChannel channel;

	private Selector selector;

	private Iterator readyItor;
	
	private boolean clientAuth;

	//protected SSLEngine sslEngine;
	private SSLContext context;

	protected ExecutorService exec;

	public Socket accept() throws IOException {
		do {
			if (readyItor == null) {
				if (selector.select() > 0)
					readyItor = selector.selectedKeys().iterator();
				else
					throw new IOException();
			}

			if (readyItor.hasNext()) {

				// Get key from set
				SelectionKey key = (SelectionKey) readyItor.next();

				// Remove current entry
				readyItor.remove();
				// TODO add processing CancelledKeyException
				if (key.isValid() && key.isAcceptable()) {
					// Get channel
					ServerSocketChannel keyChannel = (ServerSocketChannel) key.channel();

					// Get server socket
					ServerSocket serverSocket = keyChannel.socket();

					// Accept request
					SSLEngine sslEngine = context.createSSLEngine();
					if (clientAuth)
						sslEngine.setNeedClientAuth(clientAuth);
					sslEngine.setUseClientMode(false);
					return new SSLChannelSocket(serverSocket.accept(), sslEngine, exec);
				}
			} else
				readyItor = null;
		} while (true);
	}

	public void destroy() throws IOException {
		String exceptions = "";
		try {
			channel.close();
		} catch (IOException e) {
			exceptions += e.toString();
		}
		try {
			selector.close();
		} catch (IOException e) {
			exceptions += e.toString();
		}
		if (exceptions.length() > 0)
			throw new IOException(exceptions);
		if (exec != null)
			exec.shutdownNow();
	}

	public void init(Map inProperties, Map outProperties) throws IOException {
		clientAuth = "true".equals(inProperties.get(ARG_CLIENTAUTH));
		
		context = initSSLContext(inProperties, outProperties);

		selector = Selector.open();

		channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		int port =  Utils.parseInt(inProperties.get(ARG_PORT),  Utils.parseInt(inProperties.get(Serve.ARG_PORT), PORT));
		sotimeout = Utils.parseInt(inProperties.get(ARG_SOTIMEOUT), SO_TIMEOIUT);
		InetSocketAddress isa = null;
		if (inProperties.get(Serve.ARG_BINDADDRESS) != null)
			try {
				isa = new InetSocketAddress((String) inProperties.get(Serve.ARG_BINDADDRESS), port);
			} catch (Exception e) {
			}
		if (isa == null)
			isa = new InetSocketAddress(port);
		// TODO add ARG_BACKLOG
		channel.socket().bind(isa);

		// Register interest in when connection
		channel.register(selector, SelectionKey.OP_ACCEPT);
		if (outProperties != null) {
			if (channel.socket().isBound())
				outProperties.put(Serve.ARG_BINDADDRESS, channel.socket().getInetAddress().getHostName());
			else
				outProperties.put(Serve.ARG_BINDADDRESS, InetAddress.getLocalHost().getHostName());
		}
		exec = Executors.newSingleThreadScheduledExecutor();
	}

	public String toString() {
		return "SSLSelectorAcceptor - " + (channel == null ? "unset" : "" + channel.socket());
	}

	protected static class SSLChannelSocket extends Socket {
		Socket socket;
		SSLSocketChannel channel;
		ByteBuffer readBuff, writeBuff;
		InputStream inp;
		OutputStream outp;

		protected SSLChannelSocket(Socket socket, SSLEngine sslEngine, ExecutorService exec) throws IOException {
			this.socket = socket;
			socket.setSoTimeout(sotimeout); 
			channel = new SSLSocketChannel(socket.getChannel(), sslEngine, exec, null);
			readBuff = ByteBuffer.allocate(1024 * 16);
			readBuff.flip();
			writeBuff = ByteBuffer.allocate(1024);
		}

		public ByteChannel getByteChannel() {
			return channel;
		}

		public SSLSession getSession() {
			//channel.sslEngine.getSSLParameters();
			return channel.sslEngine.getSession();
		}

		@Override
		public SocketChannel getChannel() {
			return channel.unwrapChannel();
		}

		@Override
		public InetAddress getInetAddress() {
			return socket.getInetAddress();
		}

		@Override
		public int getLocalPort() {
			return socket.getLocalPort();
		}

		@Override
		public SocketAddress getRemoteSocketAddress() {
			return socket.getRemoteSocketAddress();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (inp == null)
				inp = Channels.newInputStream(channel);
			return inp;
		}

		int printFilter(int c) {
			//System.err.printf("%c", c);
			return c;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			if (outp == null)
				outp = Channels.newOutputStream(channel);
			return outp;
		}
	}

	protected static class SSLSocketChannel implements ByteChannel {
		/**
		 * This object is used to feed the {@link SSLEngine}'s wrap and unwrap
		 * methods during the handshake phase.
		 **/
		protected static ByteBuffer emptybuffer = ByteBuffer.allocate(0);

		protected ExecutorService exec;

		protected List<Future<?>> tasks;

		/** raw payload incoming */
		protected ByteBuffer inData;
		/** encrypted data outgoing */
		protected ByteBuffer outCrypt;
		/** encrypted data incoming */
		protected ByteBuffer inCrypt;

		/** the underlying channel */
		protected SocketChannel socketChannel;
		/**
		 * used to set interestOP SelectionKey.OP_WRITE for the underlying
		 * channel
		 */
		protected SelectionKey selectionKey;

		protected SSLEngine sslEngine;
		protected SSLEngineResult readEngineResult;
		protected SSLEngineResult writeEngineResult;

		/**
		 * Should be used to count the buffer allocations. But because of #190
		 * where HandshakeStatus.FINISHED is not properly returned by nio
		 * wrap/unwrap this variable is used to check whether
		 * {@link #createBuffers(SSLSession)} needs to be called.
		 **/
		protected int bufferallocations = 0;

		protected SSLSocketChannel(SocketChannel channel, SSLEngine sslEngine, ExecutorService exec, SelectionKey key)
				throws IOException {
			if (channel == null || sslEngine == null || exec == null)
				throw new IllegalArgumentException("parameter must not be null");

			this.socketChannel = channel;
			this.sslEngine = sslEngine;
			this.exec = exec;

			readEngineResult = writeEngineResult = new SSLEngineResult(Status.BUFFER_UNDERFLOW,
					sslEngine.getHandshakeStatus(), 0, 0); // init to prevent NPEs

			tasks = new ArrayList<Future<?>>(3);
			if (key != null) {
				key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
				this.selectionKey = key;
			}
			createBuffers(sslEngine.getSession());
			// kick off handshake
			socketChannel.write(wrap(emptybuffer));// initializes res
			processHandshake();
		}

		private void consumeFutureUninterruptible(Future<?> f) {
			try {
				boolean interrupted = false;
				while (true) {
					try {
						f.get();
						break;
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
				if (interrupted)
					Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				throw new RuntimeException(e);
			}
		}

		/**
		 * This method will do whatever necessary to process the sslengine
		 * handshake. Thats why it's called both from the
		 * {@link #read(ByteBuffer)} and {@link #write(ByteBuffer)}
		 **/
		private synchronized void processHandshake() throws IOException {
			if (sslEngine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING)
				return; // since this may be called either from a reading or a writing thread and because this method is synchronized it is necessary to double check if we are still handshaking.
			if (!tasks.isEmpty()) {
				Iterator<Future<?>> it = tasks.iterator();
				while (it.hasNext()) {
					Future<?> f = it.next();
					if (f.isDone()) {
						it.remove();
					} else {
						if (isBlocking())
							consumeFutureUninterruptible(f);
						return;
					}
				}
			}

			if (sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
				if (!isBlocking() || readEngineResult.getStatus() == Status.BUFFER_UNDERFLOW) {
					inCrypt.compact();
					int read = socketChannel.read(inCrypt);
					if (read == -1) {
						throw new IOException("connection closed unexpectedly by peer");
					}
					inCrypt.flip();
				}
				inData.compact();
				unwrap();
				if (readEngineResult.getHandshakeStatus() == HandshakeStatus.FINISHED) {
					createBuffers(sslEngine.getSession());
					return;
				}
			}
			consumeDelegatedTasks();
			if (tasks.isEmpty() || sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
				socketChannel.write(wrap(emptybuffer));
				if (writeEngineResult.getHandshakeStatus() == HandshakeStatus.FINISHED) {
					createBuffers(sslEngine.getSession());
					return;
				}
			}
			assert (sslEngine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING);// this function could only leave NOT_HANDSHAKING after createBuffers was called unless #190 occurs which means that nio wrap/unwrap never return HandshakeStatus.FINISHED

			bufferallocations = 1; // look at variable declaration why this line exists and #190. Without this line buffers would not be be recreated when #190 AND a rehandshake occur.
		}

		private synchronized ByteBuffer wrap(ByteBuffer b) throws SSLException {
			outCrypt.compact();
			writeEngineResult = sslEngine.wrap(b, outCrypt);
			//if (writeEngineResult.getStatus() != SSLEngineResult.Status.OK)
			//throw new SSLException("Can't wrap " + b + " in " + outCrypt + ", because "
			//	+ writeEngineResult.getStatus());
			outCrypt.flip();
			return outCrypt;
		}

		/**
		 * performs the unwrap operation by unwrapping from {@link #inCrypt} to
		 * {@link #inData}
		 **/
		private synchronized ByteBuffer unwrap() throws SSLException {
			int rem;
			do {
				rem = inData.remaining();
				readEngineResult = sslEngine.unwrap(inCrypt, inData);
			} while (readEngineResult.getStatus() == SSLEngineResult.Status.OK
					&& (rem != inData.remaining() || sslEngine.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP));
			inData.flip();
			return inData;
		}

		protected void consumeDelegatedTasks() {
			Runnable task;
			while ((task = sslEngine.getDelegatedTask()) != null) {
				tasks.add(exec.submit(task));
				// task.run();
			}
		}

		protected void createBuffers(SSLSession session) {
			int netBufferMax = session.getPacketBufferSize();
			int appBufferMax = Math.max(session.getApplicationBufferSize(), netBufferMax);

			if (inData == null) {
				inData = ByteBuffer.allocate(appBufferMax);
				outCrypt = ByteBuffer.allocate(netBufferMax);
				inCrypt = ByteBuffer.allocate(netBufferMax);
			} else {
				if (inData.capacity() != appBufferMax)
					inData = ByteBuffer.allocate(appBufferMax);
				if (outCrypt.capacity() != netBufferMax)
					outCrypt = ByteBuffer.allocate(netBufferMax);
				if (inCrypt.capacity() != netBufferMax)
					inCrypt = ByteBuffer.allocate(netBufferMax);
			}
			inData.rewind();
			inData.flip();
			inCrypt.rewind();
			inCrypt.flip();
			outCrypt.rewind();
			outCrypt.flip();
			bufferallocations++;
		}

		public int write(ByteBuffer src) throws IOException {
			if (!isHandShakeComplete()) {
				processHandshake();
				return 0;
			}
			return socketChannel.write(wrap(src));
		}

		/**
		 * Blocks when in blocking mode until at least one byte has been
		 * decoded.<br>
		 * When not in blocking mode 0 may be returned.
		 * 
		 * @return the number of bytes read.
		 **/
		public int read(ByteBuffer dst) throws IOException {
			if (!dst.hasRemaining())
				return 0;
			if (!isHandShakeComplete()) {
				if (isBlocking()) {
					while (!isHandShakeComplete()) {
						processHandshake();
					}
				} else {
					processHandshake();
					if (!isHandShakeComplete()) {
						return 0;
					}
				}
			}
			// assert ( bufferallocations > 1 ); //see #190
			//if( bufferallocations <= 1 ) {
			//	createBuffers( sslEngine.getSession() );
			//}
			/* 1. When "dst" is smaller than "inData" readRemaining will fill "dst" with data decoded in a previous read call.
			 * 2. When "inCrypt" contains more data than "inData" has remaining space, unwrap has to be called on more time(readRemaining)
			 */
			int purged = readRemaining(dst);
			if (purged != 0)
				return purged;

			/* We only continue when we really need more data from the network.
			 * Thats the case if inData is empty or inCrypt holds to less data than necessary for decryption
			 */
			assert (inData.position() == 0);
			inData.clear();

			if (!inCrypt.hasRemaining())
				inCrypt.clear();
			else
				inCrypt.compact();

			if (isBlocking() || readEngineResult.getStatus() == Status.BUFFER_UNDERFLOW)
				if (socketChannel.read(inCrypt) == -1) {
					return -1;
				}
			inCrypt.flip();
			unwrap();

			int transfered = transfereTo(inData, dst);
			if (transfered == 0 && isBlocking()) {
				return read(dst); // "transfered" may be 0 when not enough bytes were received or during rehandshaking
			}
			return transfered;
		}

		/**
		 * {@link #read(ByteBuffer)} may not be to leave all buffers(inData,
		 * inCrypt)
		 **/
		private int readRemaining(ByteBuffer dst) throws SSLException {
			if (inData.hasRemaining()) {
				return transfereTo(inData, dst);
			}
			if (!inData.hasRemaining())
				inData.clear();
			// test if some bytes left from last read (e.g. BUFFER_UNDERFLOW)
			if (inCrypt.hasRemaining()) {
				unwrap();
				int amount = transfereTo(inData, dst);
				if (amount > 0)
					return amount;
			}
			return 0;
		}

		public boolean isConnected() {
			return socketChannel.isConnected();
		}

		public void close() throws IOException {
			try {
				sslEngine.closeOutbound();
				sslEngine.getSession().invalidate();
				if (socketChannel.isOpen())
					socketChannel.write(wrap(emptybuffer));// FIXME what if not all bytes can be written
			} finally {
				socketChannel.close();
			}
			//System.err.printf("Socke %s is %b%n", socketChannel, socketChannel.isOpen());
			//exec.shutdownNow();
		}

		private boolean isHandShakeComplete() {
			HandshakeStatus status = sslEngine.getHandshakeStatus();
			return status == SSLEngineResult.HandshakeStatus.FINISHED
					|| status == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
		}

		public SelectableChannel configureBlocking(boolean b) throws IOException {
			return socketChannel.configureBlocking(b);
		}

		public boolean connect(SocketAddress remote) throws IOException {
			return socketChannel.connect(remote);
		}

		public boolean finishConnect() throws IOException {
			return socketChannel.finishConnect();
		}

		public Socket socket() {
			return socketChannel.socket();
		}

		public boolean isInboundDone() {
			return sslEngine.isInboundDone();
		}

		@Override
		public boolean isOpen() {
			return socketChannel.isOpen();
		}

		public boolean isNeedWrite() {
			return outCrypt.hasRemaining() || !isHandShakeComplete(); // FIXME this condition can cause high cpu load during handshaking when network is slow
		}

		public void writeMore() throws IOException {
			write(outCrypt);
		}

		public boolean isNeedRead() {
			return inData.hasRemaining()
					|| (inCrypt.hasRemaining() && readEngineResult.getStatus() != Status.BUFFER_UNDERFLOW && readEngineResult
							.getStatus() != Status.CLOSED);
		}

		public int readMore(ByteBuffer dst) throws SSLException {
			return readRemaining(dst);
		}

		private int transfereTo(ByteBuffer from, ByteBuffer to) {
			int fremain = from.remaining();
			int toremain = to.remaining();
			if (fremain > toremain) {
				// FIXME there should be a more efficient transfer method
				int limit = Math.min(fremain, toremain);
				for (int i = 0; i < limit; i++) {
					to.put(from.get());
				}
				return limit;
			} else {
				to.put(from);
				return fremain;
			}

		}

		public boolean isBlocking() {
			return socketChannel.isBlocking();
		}

		public SocketChannel unwrapChannel() {
			return socketChannel;
		}
	}
}
