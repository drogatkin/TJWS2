package rogatkin.wskt;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

/** The container is created one per web application and one for default application
 * 
 * @author Dmitriy
 *
 */
public class SimpleServerContainer implements ServerContainer, ServletContextListener {

	HashMap<String, ServerEndpointConfig> endpoints;
	SimpleProvider provider;
	
	HashSet<SimpleSession> sessions;
	
	ExecutorService asyncService;
	
	SimpleServerContainer(SimpleProvider simpleProvider) {
		provider = simpleProvider;
		endpoints = new HashMap<String, ServerEndpointConfig>();
		sessions = new HashSet<SimpleSession>(); 
		asyncService = Executors.newSingleThreadExecutor();
	}

	@Override
	public void addEndpoint(Class<?> arg0) throws DeploymentException {
		if (arg0.isAssignableFrom(ServerEndpointConfig.class)) {
			try {
				addEndpoint((ServerEndpointConfig)arg0.newInstance());
			} catch (InstantiationException e) {
				throw new DeploymentException( "Error in deployment end point", e);
			} catch (IllegalAccessException e) {
				throw new DeploymentException( "Error in deployment end point", e);
			}
		} else {
			ServerEndpoint sep = arg0.getAnnotation(ServerEndpoint.class);
			String path = sep.value();
			if (path == null || path.startsWith("/") == false)
					throw new DeploymentException("Invalid path "+path);
			//addEndpoint(ServerEndpointConfig.Builder.create(arg0, path).build());

			addEndpoint(new SimpleServerEndpointConfig(arg0));
		}
		
	}

	@Override
	public void addEndpoint(ServerEndpointConfig arg0) throws DeploymentException {
		if (endpoints.containsKey(arg0.getPath()))
			throw new DeploymentException("More than one end points use same path "+arg0.getPath());
		endpoints.put(arg0.getPath(), arg0);
	}

	@Override
	public Session connectToServer(Object arg0, URI arg1) throws DeploymentException, IOException {
		throw new UnsupportedOperationException("No client websocket support");
	}

	@Override
	public Session connectToServer(Class<?> arg0, URI arg1) throws DeploymentException, IOException {
		throw new UnsupportedOperationException("No client websocket support");
	}

	@Override
	public Session connectToServer(Endpoint arg0, ClientEndpointConfig arg1, URI arg2) throws DeploymentException,
			IOException {
		throw new UnsupportedOperationException("No client websocket support");
	}

	@Override
	public Session connectToServer(Class<? extends Endpoint> arg0, ClientEndpointConfig arg1, URI arg2)
			throws DeploymentException, IOException {
		throw new UnsupportedOperationException("No client websocket support");
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
	
	void log(String msg, Object ...params) {
		log(null, msg, params);
	}
	
	void log(Throwable e, String msg, Object ...params) {
		msg = "websocket : "+msg;
		if (e == null)
			if (params == null || params.length == 0)
				provider.serve.log(msg);
			else
				provider.serve.log(String.format(msg, params));
		else
			if (params == null || params.length == 0)
				provider.serve.log(msg, e);
			else
				provider.serve.log(String.format(msg, params), e);
	}

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		CloseReason cr = new CloseReason(CloseCodes.SERVICE_RESTART,"");
		//ArrayList <SimpleSession> copyss = new ArrayList<SimpleSession>(sessions);
		for(SimpleSession ss:new ArrayList<SimpleSession>(sessions))
			try {
				System.err.printf("Closing session %s%n", ss);
				ss.close(cr);
			} catch (IOException e) {
				
			}
		sessions.clear();
		asyncService.shutdown();
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		
	}
	
	void addSession(SimpleSession ss) {
		sessions.add(ss);
	}
	
	void removeSession(SimpleSession ss) {
		sessions.remove(ss);
	}
	
	


}
