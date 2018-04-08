/* tjws - DualSocketAcceptor.java
 * Copyright (C) 1999-2010 Dmitriy Rogatkin.  All rights reserved.
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
 *  $Id: DualSocketAcceptor.java,v 1.5 2009/12/31 05:02:13 dmitriy Exp $                
 *  Created on Feb 21, 2007
 *  @author Dmitriy
 */
package rogatkin.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Map;

import com.rslakra.logger.LogManager;

import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;

public class DualSocketAcceptor extends SSLAcceptor {
	
	/**
	 * @see Acme.Serve.SSLAcceptor#init(java.util.Map, java.util.Map)
	 */
	public void init(Map<Object, Object> inProperties, Map<Object, Object> outProperties) throws IOException {
		super.init(inProperties, outProperties);
		int port = inProperties.get(Serve.ARG_PORT) != null ? ((Integer) inProperties.get(Serve.ARG_PORT)).intValue() : Serve.DEF_PORT;
		int bl = 50;
		try {
			// TODO: consider conversion at getting the argument
			bl = Integer.parseInt((String) inProperties.get(ARG_BACKLOG));
			if (bl < 2) {
				bl = 2;
			}
		} catch (Exception ex) {
			LogManager.error(ex);
		}
		
		InetAddress ia = null;
		if (inProperties.get(Serve.ARG_BINDADDRESS) != null) {
			try {
				ia = InetAddress.getByName((String) inProperties.get(Serve.ARG_BINDADDRESS));
			} catch (Exception ex) {
				LogManager.error(ex);
			}
		}
		
		socket = new TwoHeadServerSocket(new ServerSocket(port, bl, ia), socket);
	}
}