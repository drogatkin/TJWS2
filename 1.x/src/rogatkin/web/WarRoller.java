/* tjws - WarRoller.java
 * Copyright (C) 2004-2010 Dmitriy Rogatkin.  All rights reserved.
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
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  $Id: WarRoller.java,v 1.30 2013/07/02 07:11:28 cvs Exp $
 * Created on Dec 13, 2004
 */
package rogatkin.web;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

import javax.servlet.ServletException;

import Acme.Utils;
import Acme.Serve.Serve;
import Acme.Serve.WarDeployer;

public class WarRoller implements WarDeployer {

    public static final String DEPLOY_ARCH_EXT = ".war";

    public static final String DEPLOYMENT_DIR_TARGET = ".web-apps-target";

    public static final String DEF_DEPLOY_DYNAMICALLY = "tjws.wardeploy.dynamically";

    public static final String DEF_DEPLOY_NOINCREMENTAL = "tjws.wardeploy.noincremental";

    public static final String DEF_VIRTUAL = "tjws.virtual";

    public static final String DEPLOY_FAILED_EXT = ".failed";

    /**
     * in deploy mode scans for all wars in war directory (app deployment dir)
     * for each war looks in corresponding place of deploy directory and figures
     * a difference, like any file in war exists and no corresponding file in
     * deploy directory or it's older if difference positive, then delete target
     * deploy directory unpack war if run mode process all WEB-INF/web.xml and
     * build app descriptor, including context name, servlet names, servlet
     * urls, class parameters process every app descriptor as standard servlet
     * connection proc dispatch for every context name assigned an app
     * dispatcher, it uses the rest to find servlet and do resource mapping
     * 
     */

    public void deploy(File warDir, final File deployTarDir, final String virtualHost) {
	// by list
	if (warDir.listFiles(new FileFilter() {
	    public boolean accept(File pathname) {
		if (pathname.isFile() && pathname.getName().toLowerCase().endsWith(DEPLOY_ARCH_EXT)) {
		    deployWar(pathname, deployTarDir);
		    return true;
		}
		return false;
	    }
	}).length == 0)
	    server.log("No .war packaged web apps found in " + (virtualHost == null ? "default" : virtualHost));
	if (deployTarDir.listFiles(new FileFilter() {
	    public boolean accept(File file) {
		if (file.isDirectory())
		    try {
			attachApp(WebAppServlet.create(file, file.getName(), server, virtualHost), virtualHost);
			markSucceeded(file.getParentFile(), file.getName()); // assumes that parent always exists
			return true;
		    } catch (ServletException se) {
			server.log(
				"Deployment of aplication " + file.getName() + " failed, reason: " + se.getRootCause(),
				se.getRootCause());
		    } catch (Throwable t) {
			if (t instanceof ThreadDeath)
			    throw (ThreadDeath) t;
			server.log("Unexpected problem in deployment of application  " + file.getName(), t);
		    }
		return false;
	    }
	}).length == 0)
	    server.log("No web apps have been deployed in " + (virtualHost == null ? "default" : virtualHost));
    }

    public boolean deployWar(File warFile, File deployTarDir) {
	String context = warFile.getName();
	assert context.toLowerCase().endsWith(DEPLOY_ARCH_EXT);
	context = context.substring(0, context.length() - DEPLOY_ARCH_EXT.length());
	File failedMark = new File(deployTarDir, context + DEPLOY_FAILED_EXT); 
	if (failedMark.exists() && failedMark.lastModified() > warFile.lastModified())
	    return false; // skipping deploy failed

	server.log("Deploying " + context);
	ZipFile zipFile = null;
	File deployDir = new File(deployTarDir, context);
	boolean noincremental = System.getProperty(DEF_DEPLOY_NOINCREMENTAL) != null;
	if (assureDir(deployDir) == false) {
	    server.log("Can't reach deployment dir " + deployDir);
	    return false;
	}
	Exception lastException = null;
	deploy: do {
	    try {
		// some overhead didn't check that doesn't exist
		zipFile = new ZipFile(warFile);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
		    ZipEntry ze = entries.nextElement();
		    String en = ze.getName();
		    if (File.separatorChar == '/')
			en = en.replace('\\', File.separatorChar);
		    if (en.contains("../") || en.contains("/..")) 
		    	throw new IOException("The file name " + en + " contains .. which can lead to a Path Traversal vulnerability");
		    File outFile = new File(deployDir, en);
		    if (ze.isDirectory()) {
			outFile.mkdirs();
		    } else {
			OutputStream os = null;
			InputStream is = null;
			File parentFile = outFile.getParentFile();
			if (parentFile.exists() == false)
			    parentFile.mkdirs();
			if (outFile.exists() && outFile.lastModified() >= ze.getTime()) {
			    continue;
			}
			if (noincremental) {
			    deleteFiles(deployDir, deployDir.list());
			    noincremental = false;
			    continue deploy;
			}
			try {
			    os = new FileOutputStream(outFile);
			    is = zipFile.getInputStream(ze);
			    copyStream(is, os);
			} catch (IOException ioe2) {
			    server.log("Problem in extracting " + en + " " + ioe2);			    
			    // TODO decide to propagate the exception up and stop deployment?
			    lastException = ioe2;
			} finally {
			    try {
				os.close();
			    } catch (Exception e2) {

			    }
			    try {
				is.close();
			    } catch (Exception e2) {

			    }
			}
			outFile.setLastModified(ze.getTime());
		    }
		}
	    } catch (ZipException ze) {
		server.log("Invalid .war format");
		lastException = ze;
	    } catch (IOException ioe) {
		server.log("Can't read " + warFile + "/ " + ioe);
		lastException = ioe;
	    } finally {
		try {
		    zipFile.close();
		} catch (Exception e) {

		}
		zipFile = null;
	    }
	} while (false);
	if (lastException == null) {
		deployDir.setLastModified(warFile.lastModified());
		return true;
	} 
	deployDir.setLastModified(0);
	return false;
    }

    protected void attachApp(WebAppServlet appServlet, String virtualHost) {
	server.addServlet(appServlet.contextPath + "/*", appServlet, virtualHost);
    }

    /** Returns auto deployment directory
     * <p>
     * The method can be overriden to give more control of choosing the directory
     * @return autodeployment directory location as local file system string
     */
    protected String getDeployDirectory() {
    	String webapp_dir = System.getProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR);
    	if (webapp_dir == null)
    	    webapp_dir = System.getProperty("user.dir") + File.separator + "webapps";
    	return webapp_dir;
    }
    
    public void deploy(Serve server) {
	this.server = server;
	final File file_webapp = new File(getDeployDirectory());
	if (assureDir(file_webapp) == false) {
	    server.log("Deployment source location " + file_webapp + " isn't a directory, deployment is impossible.");
	    return;
	}
	final File file_deployDir = new File(file_webapp, DEPLOYMENT_DIR_TARGET);
	if (assureDir(file_deployDir) == false) {
	    server.log("Target deployment location " + file_deployDir + " isn't a directory, deployment is impossible.");
	    return;
	}
	deploy(file_webapp, file_deployDir, null);

	int td = 0;
	if (System.getProperty(DEF_DEPLOY_DYNAMICALLY) != null) {
	    td = 20;
	    try {
		td = Integer.parseInt(System.getProperty(DEF_DEPLOY_DYNAMICALLY));
	    } catch (NumberFormatException nfe) {
		server.log("Default redeployment check interval: " + td + " is used");
	    }
	}
	final int interval = td * 1000;
	createWatcherThread(file_webapp, file_deployDir, interval, null);
	if (null != System.getProperty(DEF_VIRTUAL)) {
	    file_webapp.listFiles(new FileFilter() {
		@Override
		public boolean accept(File pathname) {
		    String virtualHost;
		    if (pathname.isDirectory()
			    && (virtualHost = pathname.getName()).equals(DEPLOYMENT_DIR_TARGET) == false) {

			final File file_deployDir = new File(pathname, DEPLOYMENT_DIR_TARGET);
			if (assureDir(file_deployDir) == false) {
			    WarRoller.this.server.log("Target deployment location " + file_deployDir
				    + " isn't a directory, deployment is impossible.");
			} else {
			    deploy(pathname, file_deployDir, virtualHost);
			    createWatcherThread(pathname, file_deployDir, interval, virtualHost);
			    return true;
			}
		    }
		    return false;
		}
	    });
	}
    }

    protected void createWatcherThread(final File file_webapp, final File file_deployDir, final int interval,
	    final String virtualHost) {
	if (interval <= 0)
	    return;
	Thread watcher = new Thread("Deploy update watcher for " + (virtualHost == null ? "main" : virtualHost)) {
	    public void run() {
		for (;;)
		    try {
			deployWatch(file_webapp, file_deployDir, virtualHost);
		    } catch (Throwable t) {
			if (t instanceof ThreadDeath)
			    throw (ThreadDeath) t;
			WarRoller.this.server.log("Unhandled " + t, t);
		    } finally {
			try {
			    Thread.sleep(interval);
			} catch (InterruptedException e) {
			    break;
			}
		    }
	    }
	};
	watcher.setDaemon(true);
	watcher.start();
    }

    protected boolean assureDir(File fileDir) {
	if (fileDir.exists() == false)
	    fileDir.mkdirs();
	return fileDir.isDirectory();
    }

    protected synchronized void deployWatch(File warDir, final File deployTarDir, String virtualHost) {
	server.setHost(virtualHost);
	final HashSet<String> apps = new HashSet<String>();
	warDir.listFiles(new FileFilter() {
	    public boolean accept(File file) {
		if (file.isDirectory() == false) {
		    String name = file.getName();
		    if (name.endsWith(DEPLOY_ARCH_EXT))
			apps.add(name.substring(0, name.length() - DEPLOY_ARCH_EXT.length()));
		}
		return false;
	    }
	});
	Enumeration se = server.getServlets();
	ArrayList<WebAppServlet> markedServlets = new ArrayList<WebAppServlet>(10);
	while (se.hasMoreElements()) {
	    Object servlet = se.nextElement();
	    if (servlet instanceof WebAppServlet) {
		WebAppServlet was = (WebAppServlet) servlet;
		String name = was.deployDir.getName();
		File war = new File(warDir, name + DEPLOY_ARCH_EXT);
		apps.remove(name);
		if (war.exists() && war.lastModified() > was.deployDir.lastModified()) {
		    // deployWar(new File(warDir, was.deployDir.getName() +
		    // DEPLOY_ARCH_EXT), deployTarDir);
		    markedServlets.add(was);
		}
	    }
	}
	for (WebAppServlet was : markedServlets) {
	    redeploy(warDir, deployTarDir, was, virtualHost);
	}
	for (String name : apps) {
	    // remaining not deployed yet apps
	    try {
		if (deployWar(new File(warDir, name + DEPLOY_ARCH_EXT), deployTarDir)) {
		    WebAppServlet was = WebAppServlet.create(new File(deployTarDir, name), name, server, virtualHost);		    
		    attachApp(was, virtualHost);
		}
	    } catch (Throwable t) {
		if (t instanceof ThreadDeath)
		    throw (ThreadDeath) t;
		markFailed(deployTarDir, name);
		server.log("Unexpected problem in deployment of aplication  " + name, t);
	    }
	}
    }

    public void redeploy(File warDir, File deployTarDir, WebAppServlet was, String virtualHost) {
	was = (WebAppServlet) server.unloadServlet(was);
	if (was == null)
	    return;
	server.unloadSessions(was.getServletContext());
	was.destroy();

	// TODO use pre-saved war name
	if (deployWar(new File(warDir, was.deployDir.getName() + DEPLOY_ARCH_EXT), deployTarDir))
	try {
	    was = WebAppServlet.create(was.deployDir, was.deployDir.getName(), server, virtualHost);
	    attachApp(was, virtualHost);
	    server.restoreSessions(was.getServletContext());
	    markSucceeded(deployTarDir, was.deployDir.getName());
	} catch (ServletException sex) {
	    markFailed(deployTarDir, was.deployDir.getName());
	    server.log("Deployment of a web app " + was.contextName + " failed due " + sex.getRootCause(),
		    sex.getRootCause());
	} catch (Throwable t) {
	    if (t instanceof ThreadDeath)
		throw (ThreadDeath) t;
	    markFailed(deployTarDir, was.deployDir.getName());
	    server.log("Unexpected problem in deployment of aplication  " + was.contextName, t);
	}
    }

    static private boolean markFailed(File deployTarDir, String appName) {
    	File markFile = new File(deployTarDir, appName + DEPLOY_FAILED_EXT);
    
	if (markFile.exists()) {
		File appDeployDir = new File(deployTarDir, appName);
		if (appDeployDir.exists())
		markFile.setLastModified(appDeployDir.lastModified()+1);
	    return true;
	}
	try {
	    return markFile.createNewFile();
	} catch (IOException e) {
	    return false;
	}
    }

    static private boolean markSucceeded(File deployTarDir, String appName) {
	if (new File(deployTarDir, appName + DEPLOY_FAILED_EXT).exists())
	    return new File(deployTarDir, appName+DEPLOY_FAILED_EXT).delete();
	return true;
    }

    static void copyStream(InputStream is, OutputStream os) throws IOException {
	Utils.copyStream(is, os, -1);
    }

    static void deleteFiles(File folder, String[] files) throws IOException {
	for (String fn : files) {
	    File f = new File(folder, fn);
	    if (f.isDirectory()) {
		deleteFiles(f, f.list());
		if (f.delete() == false)
		    throw new IOException("Can't delete :" + f);
	    } else {
		if (f.delete() == false)
		    throw new IOException("Can't delete :" + f);
	    }
	}
    }

    protected Serve server;
}