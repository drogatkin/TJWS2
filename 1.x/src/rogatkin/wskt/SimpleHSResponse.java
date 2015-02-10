package rogatkin.wskt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.websocket.HandshakeResponse;

public class SimpleHSResponse implements HandshakeResponse {

	HttpServletResponse response;
	
	SimpleHSResponse(HttpServletResponse r) {
		response = r;
	}
	
	@Override
	public Map<String, List<String>> getHeaders() {
		HashMap<String, List<String>> headersMap = new HashMap<String, List<String>>();
		for(String name: response.getHeaderNames()) {
			headersMap.put(name, new ArrayList<String>(response.getHeaders(name)));
		}
		return headersMap;
	}

}
