/* tjws - Main.java
 * Copyright (C) 1999-2010 Dmitriy Rogatkin.  All rights reserved.
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 *  OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  
 *  Visit http://tjws.sourceforge.net to get the latest information
 *  about Rogatkin's products.                                                        
 *  $Id: Main.java,v 1.16 2013/03/20 03:49:46 cvs Exp $                
 *  Created on Mar 27, 2007
 *  @author Dmitriy
 */
package rogatkin.app;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import rogatkin.web.WebApp;
import rogatkin.web.WebAppServlet;
import Acme.Utils;

public class Main {
	public static final String APP_MAIN_CLASS = "tjws.app.main";

	public static final String APP_MAIN_CLASSPATH = "tjws.app.main.classpath";

	public static final String APP_MAIN_STRIP_PARAM_RIGHT = "tjws.app.main.striprightparam";

	public static final String APP_MAIN_STRIP_PARAM_LEFT = "tjws.app.main.stripleftparam";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String mainClass = System.getProperty(APP_MAIN_CLASS);
		if (mainClass == null)
			Acme.Serve.Main.main(initAppServer(args));
		else {
			URLClassLoader cl = null;
			try {
				String cp = System.getProperty(APP_MAIN_CLASSPATH);
				if (cp != null) {
					String[] cps = cp.split(File.pathSeparator);
					URL urls[] = new URL[cps.length];
					for (int i = 0; i < cps.length; i++) {
						if (cps[i].startsWith("file:") == false && cps[i].startsWith("http:") == false
								&& cps[i].startsWith("https:") == false)
							urls[i] = new URL("file:/" + cps[i]); // new URL("file", "localhost/", cps[i]) 
						else
							urls[i] = new URL(cps[i]);
					}
					cl = new URLClassLoader(urls);
				}
				Class main = cl == null ? Class.forName(mainClass) : Class.forName(mainClass, true, cl);
				if (cl != null)
					Thread.currentThread().setContextClassLoader(cl);
				main.getDeclaredMethod("main", String[].class).invoke(null,
						new Object[] { rangeParam(initAppServer(args)) });
			} catch (Exception e) {
				System.err.printf("Can't launch a user app %s (%s) due: %s", mainClass,
						Arrays.toString(cl == null ? new URL[] {} : cl.getURLs()), e);
				e.printStackTrace();
			}
		}
	}

	protected static String[] rangeParam(String... params) {
		String range = System.getProperty(APP_MAIN_STRIP_PARAM_RIGHT);
		if (range != null)
			return Utils.copyOf(params, Integer.parseInt(range));
		range = System.getProperty(APP_MAIN_STRIP_PARAM_LEFT);
		if (range != null)
			return Utils.copyOfRange(params, Integer.parseInt(range), params.length);
		return params;
	}

	protected static String[] initAppServer(String... args) {
		// defaulting JNDI
		if (System.getProperty(Context.INITIAL_CONTEXT_FACTORY) == null)
			System.getProperties().setProperty(Context.INITIAL_CONTEXT_FACTORY, SimpleJndi.class.getName());
		if (System.getProperty(Context.PROVIDER_URL) == null)
			System.getProperties()
					.setProperty(Context.PROVIDER_URL, "http://localhost:1221"/*"tjws://localhost:1221"*/);
		try {
			final Context namingContext = new InitialContext();
			WebAppServlet.setAppContextDelegator(new WebAppServlet.AppContextDelegator() {
				public Object lookup(String name) {
					try {
						return namingContext.lookup(name);
					} catch (NamingException nce) {
						throw new RuntimeException("Can't delegate naming context operation", nce);
					}
				}

				public Object lookupLink(String name) {
					try {
						return namingContext.lookupLink(name);
					} catch (NamingException nce) {
						throw new RuntimeException("Can't delegate naming context operation", nce);
					}
				}

				public void add(String name, Object obj) {
				    
					try {
						if (obj instanceof WebApp.MetaContext) {
							SimpleDataSource sds = new SimpleDataSource(((WebApp.MetaContext) obj).getPath(), ((WebApp.MetaContext) obj).getClassLoader());
							// TODO all data sources created form App class path have to be destroyed at the app destroy
							if (sds.isScopeApp()) {
								HashSet<SimpleDataSource> dsmap = (HashSet<SimpleDataSource>)namingContext.getEnvironment().get(name);
								if (dsmap == null) {
									dsmap = new HashSet<SimpleDataSource>();
									namingContext.addToEnvironment(name, dsmap);
								}
								dsmap.add(sds); //System.err.printf("Adding %s for %s%n", sds, name);
							}
						} else
							namingContext.addToEnvironment(name, obj);
					} catch (NamingException nce) {
						throw new RuntimeException("Can't delegate naming context operation", nce);
					}
				}

				@Override
				public Object remove(String name) {
					Object result = null;
					try {
						result = namingContext.removeFromEnvironment(name);
						if (result instanceof HashSet) {
							for (SimpleDataSource sds:(HashSet<SimpleDataSource>)result) //{
								sds.invalidate(); //System.err.printf("Invalidating %s for %s%n", sds, name);}
						}
						return result;
					} catch (NamingException e) {
						//throw new RuntimeException("Can't resolve context in environment");
					}
					return result;
				}

			});
		} catch (NamingException nce) {
			System.err.printf("Can not obtain initial naming context (%s) because %s%n",
					System.getProperty(Context.INITIAL_CONTEXT_FACTORY), nce);
		}
		// Perhaps it should be set Context.URL_PKG_PREFIXES
		//System.out.println("Xmx set "+Runtime.getRuntime().maxMemory());
		if (args.length == 0)
			args = Acme.Serve.Main.readArguments(System.getProperty("user.dir", "."), Acme.Serve.Main.CLI_FILENAME);
		if (args != null)
			for (int i = 0; i < args.length; i++) {
				if ("-dataSource".equals(args[i])) {
					try {
						new SimpleDataSource(args[++i], null);
					} catch (IllegalArgumentException e) {
						System.err.printf("Data source %s wasn't created because %s%n", args[i], e);
					}
				}
			}
		return args;
	}
}
