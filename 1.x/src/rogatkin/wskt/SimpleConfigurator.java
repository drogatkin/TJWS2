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

import java.util.Arrays;
import java.util.List;

import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig.Configurator;

public class SimpleConfigurator extends Configurator {

	@Override
	public boolean checkOrigin(String arg0) {
		return true;
	}

	@Override
	public <T> T getEndpointInstance(Class<T> arg0) throws InstantiationException {
		try {
			return arg0.getConstructor().newInstance();
		} catch (Exception e) {
			throw (InstantiationException)new InstantiationException("Can't instantiate the endpoint "+arg0).initCause( e);
		}
	}

	@Override
	public List<Extension> getNegotiatedExtensions(List<Extension> installed, List<Extension> requested) {
		// or use retainAll of a mutable copy
		return Arrays.asList(firstMatch(installed, requested));
	}

	@Override
	public String getNegotiatedSubprotocol(List<String> supported, List<String> requested) {
		return firstMatch(supported, requested);
	}

	<T> T firstMatch(List<T> l1, List<T> l2) {
		for (T t:l2) {
			for(T t2:l1) {
				if (t.equals(t2))
					return t;
			}
		}
		return null;
	}
}
