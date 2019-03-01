// Copyright (C)2018 by Rohtash Singh Lakra <rohtash.singh@gmail.com>.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the https://github.com/rslakra/TJWS2 page for up-to-date versions of
// this and other fine Java utilities.
//
// All enhancements Copyright (C)2018 by Rohtash Singh Lakra
// This version is compatible with JSDK 2.5
// https://github.com/rslakra/TJWS2
package com.rslakra.android.server;

import java.util.List;

import javax.servlet.ServletContext;

import rogatkin.wskt.SimpleProvider;

//import javax.websocket.DeploymentException;
//import javax.websocket.Endpoint;
//import javax.websocket.server.ServerApplicationConfig;
//import javax.websocket.server.ServerEndpointConfig;

public class WSProvider extends SimpleProvider {
    
    public WSProvider() {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void deploy(ServletContext servCtx, List cp) {
//		SimpleServerContainer ssc = new SimpleServerContainer(this);
//		HashSet<ServerApplicationConfig> appCfgs = new HashSet<ServerApplicationConfig>();
//		HashSet<Class<?>> annSeps = new HashSet<Class<?>>();
//		HashSet<Class<? extends Endpoint>> endps = new HashSet<Class<? extends Endpoint>>();
//		if (servCtx instanceof WebAppServlet) {
//			WebAppServlet webApp = (WebAppServlet)servCtx;
//			File wsInfo = new File(webApp.getDeploymentDir(), "META-INF/websocket/websocket.info");
//			if (wsInfo.exists()) {
//				BufferedReader fr = null;
//				try {
//					fr = new BufferedReader(new FileReader(wsInfo));
//					String s =
//					fr.readLine();
//					while(s != null) {
//						if (s.startsWith("ServerEndpoint ")) {
//							try {
//								annSeps.add(Class.forName(s.substring("ServerEndpoint ".length()), true, webApp.getClassLoader()));
//							} catch (ClassNotFoundException e) {
//								serve.log(e, "Server Endpoint not found");
//							}
//						} else if (s.startsWith("ServerApplicationConfig ")) {
//							try {
//								appCfgs.add((ServerApplicationConfig) Class.forName(s.substring("ServerApplicationConfig ".length()), true, webApp.getClassLoader()).newInstance());
//							} catch (InstantiationException e) {
//								serve.log(e, "Error at deployment");
//							} catch (IllegalAccessException e) {
//								serve.log(e, "Error at deployment");
//							} catch (ClassNotFoundException e) {
//								serve.log(e, "Endpoint config not found");
//							}
//						} else if (s.startsWith("Endpoint ")) {
//							try {
//								endps.add((Class<? extends Endpoint>) Class.forName(s.substring("Endpoint ".length()), true, webApp.getClassLoader()));
//							} catch (ClassNotFoundException e) {
//								serve.log(e, "Endpoint not found");
//							}
//						}
//						s = fr.readLine();
//					}
//				} catch(IOException e) {
//
//				} finally {
//					if (fr != null)
//						try {
//							fr.close();
//						} catch(Exception e) {
//
//						}
//				}
//			}
//		}
//		if (appCfgs.size() > 0) {
//			for (ServerApplicationConfig sac : appCfgs) {
//				for (Class<?> se : sac.getAnnotatedEndpointClasses(annSeps))
//					try {
//						ssc.addEndpoint(se);
//						serve.log("Deployed ServerEndpoint " + se);
//					} catch (DeploymentException de) {
//
//					}
//				for (ServerEndpointConfig epc : sac.getEndpointConfigs(endps))
//					try {
//						ssc.addEndpoint(epc);
//						serve.log("Deployed ServerEndpointConfig " + epc);
//					} catch (DeploymentException de) {
//
//					}
//			}
//		} else {
//			for (Class<?> se : annSeps)
//				try {
//					ssc.addEndpoint(se);
//					serve.log("Deployed ServerEndpoint " + se);
//				} catch (DeploymentException de) {
//
//				}
//		}
//		servCtx.setAttribute("javax.websocket.server.ServerContainer", ssc);
//		try {
//			servCtx.addListener(ssc);
//		} catch (Error e) {
//			// serve is still on old servlet spec
//		}
    }
    
}
