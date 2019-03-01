/*
 * tjws - SimpleAcceptor.java
 * Copyright (C) 1999-2007 Dmitriy Rogatkin. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * Visit http://tjws.sourceforge.net to get the latest information
 * about Rogatkin's products.
 * $Id: SimpleAcceptor.java,v 1.9 2012/08/16 02:50:15 dmitriy Exp $
 * Created on Jun 12, 2007
 * @author dmitriy
 * @author Rohtash Singh Lakra
 */
package Acme.Serve;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import com.rslakra.logger.LogManager;

import Acme.IOHelper;

/**
 * SimpleAcceptor.java
 */
public class SimpleAcceptor implements Serve.Acceptor {
	
	/** socket */
	private ServerSocket socket;
	
	/**
	 * @see Acme.Serve.Serve.Acceptor#accept()
	 */
	public Socket accept() throws IOException {
		return socket.accept();
	}
	
	/**
	 * @see Acme.Serve.Serve.Acceptor#destroy()
	 */
	public void destroy() throws IOException {
		if (socket == null) {
			throw new IOException("Socket already destroyed!");
		}
		
		IOHelper.closeSilently(socket);
		socket = null;
	}
	
	/**
	 * @see Acme.Serve.Serve.Acceptor#init(java.util.Map, java.util.Map)
	 */
	public void init(Map<Object, Object> inProperties, Map<Object, Object> outProperties) throws IOException {
		int port = inProperties.get(Serve.ARG_PORT) != null ? ((Integer) inProperties.get(Serve.ARG_PORT)).intValue() : Serve.DEF_PORT;
		String bindAddrStr = (String) inProperties.get(Serve.ARG_BINDADDRESS);
		LogManager.debug("bindAddrStr:" + bindAddrStr);
		InetSocketAddress bindAddr = bindAddrStr != null ? new InetSocketAddress(InetAddress.getByName(bindAddrStr), port) : null;
		String backlogStr = (String) inProperties.get(Serve.ARG_BACKLOG);
		int backlog = backlogStr != null ? Integer.parseInt(backlogStr) : -1;
		if (bindAddr != null) {
			socket = new ServerSocket();
			if (backlog < 0) {
				socket.bind(bindAddr);
			} else {
				socket.bind(bindAddr, backlog);
			}
		} else {
			if (backlog < 0) {
				socket = new ServerSocket(port);
			} else {
				socket = new ServerSocket(port, backlog);
			}
		}
		
		if (outProperties != null) {
			if (socket.isBound()) {
				outProperties.put(Serve.ARG_BINDADDRESS, socket.getInetAddress().getHostName());
			} else {
				outProperties.put(Serve.ARG_BINDADDRESS, InetAddress.getLocalHost().getHostName());
			}
		}
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "SimpleAcceptor - " + (socket != null ? socket.toString() : "Uninitialized!");
	}
}