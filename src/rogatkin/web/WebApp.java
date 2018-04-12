/* tjws - WebApp.java
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
 *  $Id: WebApp.java,v 1.22 2013/03/12 07:58:19 cvs Exp $                
 *  Created on Jun 12, 2006
 *  @author dmitriy
 */
package rogatkin.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;

import Acme.IOHelper;
import Acme.Utils;
import Acme.Serve.Main;

public class WebApp {
	Main main;
	
	public static final String RUN_DESCRIPTOR = "rundescriptor";
	public static final String DEF_WEBAPP_AUTODEPLOY_DIR = "tjws.webappdir";
	public static final String DEF_WEBAPP_CLASSLOADER = "tjws.webclassloader";
	static boolean restart;
	
	/**
	 * @param args,
	 *            1st specifies .war file location others can match standard If
	 *            no parameters specifies it considered as all in one and takes
	 *            run descriptor
	 *            from /app/rundescriptor
	 *            <p>
	 *            rundescriptor file has multiple lines, 1st define command line
	 *            argument<br>
	 *            following define names of .wars in /app/
	 */
	public static void main(String[] args) {
		File deployDir;
		if (System.getProperty(DEF_WEBAPP_AUTODEPLOY_DIR) == null) {
			deployDir = getDeployDirectory("tjws-web-apps");
			if (deployDir == null) {
				System.exit(1); // message already printed
				return;
			}
			// deployDir.deleteOnExit(); // TODO make it more consistent and
			// provide directory deletion in on exit hook
			System.setProperty(DEF_WEBAPP_AUTODEPLOY_DIR, deployDir.getPath());
		} else {
			deployDir = new File(System.getProperty(DEF_WEBAPP_AUTODEPLOY_DIR));
		}
		
		ServiceController ctrl = null;
		try {
			ctrl = (ServiceController) Class.forName("rogatkin.web.SysTrayControl").newInstance();
		} catch (Exception ex) {
			// ex.printStackTrace();
		}
		
		URL warUrl = null;
		if (args.length == 0) {
			String[] descr = readDescriptor();
			if (descr == null) {
				System.exit(-2);
				return; // for sanity, it is never called
			} else {
				warUrl = WebApp.class.getClassLoader().getResource(descr[1]);
				args = Utils.splitStr(descr[0], "\"");
			}
		} else {
			try {
				warUrl = new File(args[0]).toURI().toURL();
				System.out.printf("Launching %s...%n", warUrl);
				args = Utils.copyOfRange(args, 1, args.length - 1);
			} catch (java.net.MalformedURLException mfe) {
				System.exit(-3);
				return;
			}
		}
		
		if (warUrl != null)
			try {
				copyWar(warUrl, deployDir);
				if (ctrl != null) {
					String[] webapps = deployDir.list(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.toLowerCase().endsWith(WarRoller.DEPLOY_ARCH_EXT);
						}
					});
					for (int i = 0; i < webapps.length; i++)
						webapps[i] = webapps[i].substring(0, webapps[i].length() - WarRoller.DEPLOY_ARCH_EXT.length());
					ctrl.attachServe(getStopMethod(), getRestartMethod(), webapps);
				}
				
				do {
					restart = false;
					if (Main.runMain(ctrl.massageSettings(args)) == 3 && ctrl != null) {
						if (ctrl.reportError(3, "Port conflict"))
							restart = true;
					}
				} while (restart);
			} catch (IOException ioe) {
				System.err.printf("Can't copy war %s file to the deployment directory %s exception %s%n", warUrl, deployDir, ioe);
				System.exit(2);
			}
	}
	
	public static String[] readDescriptor() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(WebApp.class.getClassLoader().getResourceAsStream(RUN_DESCRIPTOR), "utf-8"));
			// read CLA
			String parameters = br.readLine();
			// can be loop if more than one war
			return new String[] { parameters, br.readLine() };
		} catch (NullPointerException npe) {
			System.err.printf("No .war file argument provided and there is no '%s' descriptor for embedded app in the jar packaging%n", RUN_DESCRIPTOR);
			return null;
		} catch (UnsupportedEncodingException e) {
			System.err.printf("Unsupported ecoding %s%n", e);
			return null;
		} catch (IOException ioe) {
			System.err.printf("IO error (%s) at reading app descriptor%n", ioe);
			return null;
		} finally {
			IOHelper.closeSilently(br);
		}
	}
	
	private static Method getStopMethod() {
		try {
			return Main.class.getDeclaredMethod("stop");
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
		
		return null;
	}
	
	private static Method getRestartMethod() {
		try {
			return WebApp.class.getDeclaredMethod("setRestart");
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}
		return null;
	}
	
	public static void setRestart() {
		restart = true;
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 */
	public static File getDeployDirectory(String key) {
		String dirName = System.getProperty("java.io.tmpdir");
		if (dirName == null) {
			dirName = System.getProperty("user.home");
			if (dirName == null) {
				dirName = ".";
			}
		}
		
		File result = new File(dirName, key);
		try {
			result = result.getCanonicalFile();
			// no check because can be existent
			result.mkdirs();
			return result;
		} catch (IOException e) {
			System.err.printf("Can't create a deployment directory: %s %s%n", e, result);
		}
		return null;
	}
	
	/**
	 * 
	 * @param sourceWar
	 * @param deploymentDir
	 * @throws IOException
	 */
	public static void copyWar(URL sourceWar, File deploymentDir) throws IOException {
		File targetWar = new File(deploymentDir, new File(sourceWar.getFile()).getName());
		URLConnection uc = sourceWar.openConnection();
		if (targetWar.exists() && targetWar.lastModified() >= uc.getLastModified()) {
			return;
		}
		
		OutputStream os = null;
		InputStream is = null;
		try {
			Utils.copyStream(is = uc.getInputStream(), os = new FileOutputStream(targetWar), -1);
		} finally {
			IOHelper.closeSilently(os);
			IOHelper.closeSilently(is);
		}
		targetWar.setLastModified(uc.getLastModified());
	}
	
	public static interface ServiceController {
		void attachServe(Method stop, Method restart, String[] contextPaths);
		
		String[] massageSettings(String[] args);
		
		boolean reportError(int code, String message);
	}
	
	/**
	 * provides connection to Context.xml
	 * 
	 * @author dmitriy
	 *
	 */
	public static class MetaContext {
		private String path;
		private ClassLoader appClassLoader;
		
		public MetaContext(String contextPath, ClassLoader cl) {
			path = contextPath;
			appClassLoader = cl;
		}
		
		public String getPath() {
			return path;
		}
		
		public ClassLoader getClassLoader() {
			return appClassLoader;
		}
	}
}
