package rogatkin.wskt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

public class SimpleServerEndpointConfig implements ServerEndpointConfig {
    private String path;
    private Class<?> endpointClass;
    private List<String> subprotocols; 
    private List<Extension> extensions;
    private List<Class<? extends Encoder>> encoders;
    private List<Class<? extends Decoder>> decoders;
    private Map<String, Object> userProperties = new HashMap<String, Object>();
    private ServerEndpointConfig.Configurator serverEndpointConfigurator;


	SimpleServerEndpointConfig(Class<?> endPoint) throws DeploymentException {
		endpointClass = endPoint;
		ServerEndpoint sep = endpointClass.getAnnotation(ServerEndpoint.class);
		path = sep.value();
		if (path == null || path.startsWith("/") == false)
				throw new DeploymentException("Invalid path "+path);
		subprotocols = Arrays.asList(sep.subprotocols());
		encoders = Arrays.asList(sep.encoders());
		decoders =  Arrays.asList(sep.decoders());
		serverEndpointConfigurator = new SimpleConfigurator();
	}
	
	@Override
	public List<Class<? extends Decoder>> getDecoders() {
		return decoders;
	}

	@Override
	public List<Class<? extends Encoder>> getEncoders() {
		return encoders;
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return userProperties;
	}

	@Override
	public Configurator getConfigurator() {
		return serverEndpointConfigurator;
	}

	@Override
	public Class<?> getEndpointClass() {
		return endpointClass;
	}

	@Override
	public List<Extension> getExtensions() {
		return extensions;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public List<String> getSubprotocols() {
		return subprotocols;
	}

}
