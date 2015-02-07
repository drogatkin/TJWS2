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
			return arg0.newInstance();
		} catch (IllegalAccessException e) {
			throw new InstantiationException();
		}
	}

	@Override
	public List<Extension> getNegotiatedExtensions(List<Extension> arg0, List<Extension> arg1) {
		return Arrays.asList(firstMatch(arg0, arg1));
	}

	@Override
	public String getNegotiatedSubprotocol(List<String> arg0, List<String> arg1) {
		return firstMatch(arg0, arg1);
	}

	<T> T firstMatch(List<T> l1, List<T> l2) {
		for (T t:l2) {
			for(T t2:l2) {
				if (t.equals(l2))
					return t;
			}
		}
		return null;
	}
}
