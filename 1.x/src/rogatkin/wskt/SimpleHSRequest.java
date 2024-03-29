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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.HandshakeRequest;

public class SimpleHSRequest implements HandshakeRequest {

	HttpServletRequest request;
	SimpleHSRequest(HttpServletRequest req) {
		request = req;
	}
	
	@Override
	public Map<String, List<String>> getHeaders() {
		HashMap<String, List<String>> headersMap = new HashMap<String, List<String>>();
		for(Enumeration<String> hn = request.getHeaderNames(); hn.hasMoreElements();) {
			String name = hn.nextElement();
			headersMap.put(name, Collections.list(request.getHeaders(name)));
		}
		return headersMap;
	}

	@Override
	public Object getHttpSession() {
		return request.getSession(false);
	}

	@Override
	public Map<String, List<String>> getParameterMap() {
		 HashMap<String, List<String>> paramsMap = new HashMap<String, List<String>>();
		for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
			paramsMap.put(e.getKey(), Arrays.asList(e.getValue()));
		}
		return paramsMap;
	}

	@Override
	public String getQueryString() {
		return request.getQueryString();
	}

	@Override
	public URI getRequestURI() {
		try {
			return new URI(request.getRequestURI());
		} catch (URISyntaxException e) {
			
		}
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return request.getUserPrincipal();
	}

	@Override
	public boolean isUserInRole(String arg0) {
		return request.isUserInRole(arg0);
	}
	
	// not portable, just internal hack
	public HttpServletRequest getHttpRequest() {
		return request;
	}

}
