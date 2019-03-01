/** Copyright 2015 Dmitriy Rogatkin, All rights reserved.
 *  $Id: WSProvider.java,v 1.3 2012/09/15 17:47:27 dmitriy Exp $
 */package rogatkin.mobile.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import javax.servlet.ServletContext;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import rogatkin.web.WebAppServlet;
import rogatkin.wskt.SimpleProvider;
import rogatkin.wskt.SimpleServerContainer;

public class WSProvider extends SimpleProvider {

	public WSProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void deploy(ServletContext servCtx, List cp) {
		SimpleServerContainer ssc = new SimpleServerContainer(this);                          
		HashSet<ServerApplicationConfig> appCfgs = new HashSet<ServerApplicationConfig>();    
		HashSet<Class<?>> annSeps = new HashSet<Class<?>>();                                  
		HashSet<Class<? extends Endpoint>> endps = new HashSet<Class<? extends Endpoint>>(); 
		if (servCtx instanceof WebAppServlet) {
			WebAppServlet webApp = (WebAppServlet)servCtx; 
			File wsInfo = new File(webApp.getDeploymentDir(), "META-INF/websocket/websocket.info");
			if (wsInfo.exists()) {
				BufferedReader fr = null;
				try {
					fr = new BufferedReader(new FileReader(wsInfo));
					String s =
					fr.readLine();
					while(s != null) {
						if (s.startsWith("ServerEndpoint ")) {
							try {
								annSeps.add(Class.forName(s.substring("ServerEndpoint ".length()), true, webApp.getClassLoader()));
							} catch (ClassNotFoundException e) {
								serve.log(e, "Server Endpoint not found");
							}
						} else if (s.startsWith("ServerApplicationConfig ")) {
							try {
								appCfgs.add((ServerApplicationConfig) Class.forName(s.substring("ServerApplicationConfig ".length()), true, webApp.getClassLoader()).newInstance());
							} catch (InstantiationException e) {
								serve.log(e, "Error at deployment");
							} catch (IllegalAccessException e) {
								serve.log(e, "Error at deployment");
							} catch (ClassNotFoundException e) {
								serve.log(e, "Endpoint config not found");
							}
						} else if (s.startsWith("Endpoint ")) {
							try {
								endps.add((Class<? extends Endpoint>) Class.forName(s.substring("Endpoint ".length()), true, webApp.getClassLoader()));
							} catch (ClassNotFoundException e) {
								serve.log(e, "Endpoint not found");
							}
						}
						s = fr.readLine();
					}
				} catch(IOException e) {
					
				} finally {
					if (fr != null) 
						try {
							fr.close();
						} catch(Exception e) {
							
						}
				}
			}
		}
		if (appCfgs.size() > 0) {
			for (ServerApplicationConfig sac : appCfgs) {
				for (Class<?> se : sac.getAnnotatedEndpointClasses(annSeps))
					try {
						ssc.addEndpoint(se);
						serve.log("Deployed ServerEndpoint " + se);
					} catch (DeploymentException de) {

					}
				for (ServerEndpointConfig epc : sac.getEndpointConfigs(endps))
					try {
						ssc.addEndpoint(epc);
						serve.log("Deployed ServerEndpointConfig " + epc);
					} catch (DeploymentException de) {

					}
			}
		} else {
			for (Class<?> se : annSeps)
				try {
					ssc.addEndpoint(se);
					serve.log("Deployed ServerEndpoint " + se);
				} catch (DeploymentException de) {

				}
		}
		servCtx.setAttribute("javax.websocket.server.ServerContainer", ssc);
		try {
			servCtx.addListener(ssc);
		} catch (Error e) {
			// serve is still on old servlet spec
		}
	}

}
