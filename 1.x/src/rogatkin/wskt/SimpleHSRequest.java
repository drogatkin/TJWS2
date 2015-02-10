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

}
