package rogatkin.wskt;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

public class SimpleServerContainer implements ServerContainer {

	@Override
	public void addEndpoint(Class<?> arg0) throws DeploymentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addEndpoint(ServerEndpointConfig arg0) throws DeploymentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Session connectToServer(Object arg0, URI arg1) throws DeploymentException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session connectToServer(Class<?> arg0, URI arg1) throws DeploymentException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session connectToServer(Endpoint arg0, ClientEndpointConfig arg1, URI arg2) throws DeploymentException,
			IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Session connectToServer(Class<? extends Endpoint> arg0, ClientEndpointConfig arg1, URI arg2)
			throws DeploymentException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getDefaultAsyncSendTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDefaultMaxBinaryMessageBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDefaultMaxSessionIdleTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDefaultMaxTextMessageBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Set<Extension> getInstalledExtensions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAsyncSendTimeout(long arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDefaultMaxBinaryMessageBufferSize(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDefaultMaxSessionIdleTimeout(long arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDefaultMaxTextMessageBufferSize(int arg0) {
		// TODO Auto-generated method stub
		
	}

}
