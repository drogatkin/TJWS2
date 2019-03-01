/** Copyright 2011 Dmitriy Rogatkin, All rights reserved.
 *  $Id: TJWSServ.java,v 1.15 2012/09/15 17:47:27 dmitriy Exp $
 */
package rogatkin.mobile.web;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.Servlet;

import org.apache.http.conn.util.InetAddressUtils;

import rogatkin.web.WarRoller;
import rogatkin.web.WebApp;
import rogatkin.web.WebAppServlet;
import Acme.Utils;
import Acme.Serve.FileServlet;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Android TJWS server service
 * 
 * @author drogatki
 * 
 */
public class TJWSServ extends Service {
	public enum OperCode {
		info,
		stop,
		remove,
		deploy
	}
	
	static final String SERVICE_NAME = "TJWS";
	public static final String DEPLOYMENTDIR = "atjeews/webapps";
	public static final String LOGDIR = "atjeews/log";
	public static final String KEYSTORE_DIR = "key";
	public static final String KEYSTORE = "keystore";
	
	public static final int ST_RUN = 1;
	public static final int ST_STOP = 0;
	public static final int ST_ERR = -1;
	
	protected AndroidServer srv;
	protected Config config;
	
	protected ArrayList<String> servletsList;
	protected PrintStream logStream;
	public File deployDir;
	
	private int status;
	
	WifiLock wifiLock;
	
	public boolean protectedFS = android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1;
	
	@Override
	public IBinder onBind(Intent intent) {
		if (Main.DEBUG)
			Log.d(SERVICE_NAME, "Binding from " + intent.getClass().getName());
		initServ();
		
		return mBinder;
	}
	
	private final RCServ.Stub mBinder = new RCServ.Stub() {
		
		public String start() throws RemoteException {
			String result = updateNetworkSettings();
			startServ();
			return result;
		}
		
		public void stop() throws RemoteException {
			stopServ();
		}
		
		public int getStatus() throws RemoteException {
			return status;
		}
		
		public void logging(boolean enable) throws RemoteException {
			srv.setAccessLogged(enable);
		}
		
		public List<String> getApps() throws RemoteException {
			return servletsList;
		}
		
		public String deployApp(String url) throws RemoteException {
			return deployAppFrom(url);
		}
		
		public List<String> rescanApps() throws RemoteException {
			scanDeployments();
			updateServletsList();
			return servletsList;
		}
		
		public String getAppInfo(String name) throws RemoteException {
			return (String) doAppOper(OperCode.info, name);
		}
		
		public List<String> stopApp(String name) throws RemoteException {
			return (List<String>) doAppOper(OperCode.stop, name);
		}
		
		public void removeApp(String name) throws RemoteException {
			doAppOper(OperCode.remove, name);
		}
		
		public List<String> redeployApp(String name) throws RemoteException {
			return (List<String>) doAppOper(OperCode.deploy, name);
		}
	};
	
	@Override
	public void onDestroy() {
		// srv.log("Destroy commande received");
		stopServ(); // just in case
		srv.destroyAllServlets();
		if (logStream != System.out && logStream != null)
			logStream.close();
		super.onDestroy();
	}
	
	private void stopServ() {
		// srv.log(new Exception("stop"), "stop");
		if (status != ST_STOP)
			srv.notifyStop();
	}
	
	private void startServ() {
		if (status != ST_RUN) {
			new Thread() {
				@Override
				public void run() {
					status = ST_RUN;
					int code = 0;
					if (wifiLock == null) {
						WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
						wifiLock = wifiManager.createWifiLock(SERVICE_NAME);
					}
					try {
						wifiLock.acquire();
						code = srv.serve();
						if (Main.DEBUG) {
							Log.d(SERVICE_NAME, "Serve terminated with :" + code);
							srv.log("Serve terminated with :" + code);
						}
					} finally {
						if (wifiLock != null && wifiLock.isHeld())
							wifiLock.release();
						status = code == 0 ? ST_STOP : ST_ERR;
						// TODO find out how notify client
					}
				}
			}.start();
		}
	}
	
	protected void initServ() {
		if (srv != null) {
			if (Main.DEBUG)
				Log.d(SERVICE_NAME, "Serve is already initialized");
			return;
		}
		if (config == null)
			config = new Config();
		config.load(this);
		initLogging();
		// setting properties for the server, and exchangeable Acceptors
		Properties properties = new Properties();
		properties.setProperty(Acme.Serve.Serve.ARG_NOHUP, "nohup");
		// properties.put(Acme.Serve.Serve.ARG_KEEPALIVE, Boolean.FALSE);
		// log properties
		properties.setProperty(Acme.Serve.Serve.ARG_ACCESS_LOG_FMT, "{0} {2} [{3,date,dd/MMM/yy:HH:mm:ss Z}] \"{4} {5} {6}\" {7,number,#}");
		// //// JSP /////
		properties.setProperty(Acme.Serve.Serve.ARG_JSP, "org.apache.jasper.servlet.JspServlet");
		properties.setProperty("org.apache.jasper.servlet.JspServlet.classpath", "%classpath%");
		properties.setProperty("org.apache.jasper.servlet.JspServlet.scratchdir", "%deploydir%/META-INF/jsp-classes");
		// //////////
		srv = new AndroidServ(properties, logStream, (Object) this);
		// add settings servlet
		srv.addServlet("/settings", new Settings(this));
		System.setProperty(WebAppServlet.WAR_NAME_AS_CONTEXTPATH, "yes");
		// set dex class loader
		System.setProperty(WebApp.DEF_WEBAPP_CLASSLOADER, AndroidClassLoader.class.getName()); // "rogatkin.mobile.web.AndroidClassLoader"
		initDeployDirectory();
		resetServ();
	}
	
	@SuppressLint("NewApi")
	protected void initLogging() {
		if (logStream != null)
			return;
		File logDir = new File(protectedFS ? getExternalCacheDir() : Environment.getExternalStorageDirectory(), LOGDIR);
		if (Main.DEBUG)
			Log.d(SERVICE_NAME, "Open log " + logDir);
		if (logDir.exists() && logDir.isDirectory() || logDir.mkdirs()) {
			try {
				logStream = new PrintStream(new File(logDir, "access-" + System.currentTimeMillis() + ".log"), "UTF-8");
			} catch (Exception e) {
				if (Main.DEBUG)
					Log.e(SERVICE_NAME, "Can't create log file", e);
			}
		}
		if (logStream == null)
			logStream = System.out;
		else
			System.setErr(logStream);
	}
	
	protected void initDeployDirectory() {
		if (Main.DEBUG)
			Log.d(SERVICE_NAME, "deploy dir:" + deployDir + ", for app:" + getFilesDir());
		if (deployDir != null && deployDir.exists())
			return;
		if (Main.DEBUG)
			Log.d(SERVICE_NAME, "use sd:" + config.useSD + ", deployed to:" + deployDir);
		config.useSD = !protectedFS;
		if (config.useSD) {
			deployDir = new File(Environment.getExternalStorageDirectory(), DEPLOYMENTDIR);
			if (deployDir.exists() || deployDir.mkdirs())
				System.setProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR, deployDir.getPath());
			else
				config.useSD = false;
		}
		if (config.useSD == false) {
			// deployDir = new File(System.getProperty(Config.APP_HOME),
			// DEPLOYMENTDIR);
			deployDir = new File(getFilesDir(), DEPLOYMENTDIR);
			if (deployDir.exists() == false && deployDir.mkdirs() == false) {
				if (Main.DEBUG)
					Log.e(SERVICE_NAME, "Can't establish web apps deployment directory:" + deployDir);
				deployDir = new File("/sdcard", DEPLOYMENTDIR);
			}
			System.setProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR, deployDir.getPath());
		}
		if (Main.DEBUG)
			Log.d(SERVICE_NAME, "deploy dir " + deployDir + " is " + deployDir.exists());
	}
	
	protected void updateWWWServlet() {
		Servlet rootServlet = srv.getServlet("/*");
		if (Main.DEBUG)
			Log.d(SERVICE_NAME, "Root app :" + config.rootApp + ", servlet / " + rootServlet);
		if ("/".equals(config.rootApp)) {
			
			Acme.Serve.Serve.PathTreeDictionary aliases = new Acme.Serve.Serve.PathTreeDictionary();
			aliases.put("/*", new File(config.wwwFolder));
			srv.setMappingTable(aliases);
			if (rootServlet instanceof FileServlet == false) {
				if (rootServlet != null)
					srv.unloadServlet(rootServlet);
				srv.addDefaultServlets(null); // optional file servlet
			}
		} else {
			if (rootServlet != null) {
				srv.unloadServlet(rootServlet);
				rootServlet.destroy();
				srv.unloadSessions(rootServlet.getServletConfig().getServletContext());
			}
			if (config.rootApp != null) {
				System.setProperty(WebAppServlet.WAR_DEPLOY_IN_ROOT, config.rootApp.substring(1));
			} else {
				System.getProperties().remove(WebAppServlet.WAR_DEPLOY_IN_ROOT);
			}
		}
	}
	
	protected void updateRealm() {
		Acme.Serve.Serve.PathTreeDictionary realms = new Acme.Serve.Serve.PathTreeDictionary();
		if (config.password != null && config.password.length() > 0) {
			Acme.Serve.Serve.BasicAuthRealm realm;
			realms.put("/settings", realm = new Acme.Serve.Serve.BasicAuthRealm(Main.APP_NAME));
			realm.put("", config.password);
		}
		srv.setRealms(realms);
	}
	
	protected File getKeyDir() {
		File result = new File(deployDir, "../" + KEYSTORE_DIR);
		if (!result.exists())
			result.mkdirs();
		return result;
	}
	
	void storeConfig() {
		config.store(this);
	}
	
	public InetAddress getLocalIpAddress() {
		try {
			if (config.bindAddr != null)
				return InetAddress.getByName(config.bindAddr);
		} catch (UnknownHostException e) {
			if (Main.DEBUG)
				Log.e(SERVICE_NAME, "Can't resolve :" + config.bindAddr + " " + e.toString());
			return null;
		}
		return getLookbackAddress();
		// return getNonLookupAddress();
	}
	
	public static InetAddress getLookbackAddress() {
		InetAddress result = null;
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (inetAddress.isLoopbackAddress()) {
						if (InetAddressUtils.isIPv4Address(inetAddress.getHostAddress()))
							return inetAddress;
						result = inetAddress;
						// if (Main.DEBUG)
						// Log.e(SERVICE_NAME, "processed addr:"+result);
					}
				}
			}
		} catch (SocketException ex) {
			if (Main.DEBUG)
				Log.e(SERVICE_NAME, ex.toString());
		}
		return result;
	}
	
	public static InetAddress getNonLookupAddress(boolean any) {
		InetAddress result = null;
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						if ((inetAddress.isSiteLocalAddress() == false || any) && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress()))
							return inetAddress;
						result = inetAddress;
					}
				}
			}
		} catch (SocketException ex) {
			if (Main.DEBUG)
				Log.e(SERVICE_NAME, ex.toString());
		}
		return result;
	}
	
	String updateNetworkSettings() {
		config.load(this);
		// port
		srv.arguments.put(Acme.Serve.Serve.ARG_PORT, config.port);
		// SSL
		if (config.ssl) {
			srv.arguments.put(Acme.Serve.Serve.ARG_ACCEPTOR_CLASS, config.webSocketEnabled ? "rogatkin.wskt.SSLSelectorAcceptor" : "Acme.Serve.SSLAcceptor");
			srv.arguments.put(Acme.Serve.SSLAcceptor.ARG_KEYSTOREFILE, new File(getKeyDir(), KEYSTORE).getPath());
			srv.arguments.put(Acme.Serve.SSLAcceptor.ARG_KEYSTOREPASS, config.password == null || "".equals(config.password) ? "changeme" : config.password);
			srv.arguments.put(Acme.Serve.SSLAcceptor.ARG_KEYSTORETYPE, "BKS");
			if (Main.DEBUG)
				srv.log("SSL configured as:" + srv.arguments);
		} else if (config.webSocketEnabled)
			srv.arguments.put(Acme.Serve.Serve.ARG_ACCEPTOR_CLASS, "Acme.Serve.SelectorAcceptor");
		else
			srv.arguments.remove(Acme.Serve.Serve.ARG_ACCEPTOR_CLASS);
		srv.setAccessLogged(config.logEnabled);
		
		// bind address
		if (Main.DEBUG)
			Log.d(SERVICE_NAME, "bind to " + config.bindAddr + ",use sd:" + config.useSD + ", deployed to:" + deployDir);
		resetServ();
		/*
		 * if (config.bindAddr == null) {
		 * srv.arguments.put(Acme.Serve.Serve.ARG_BINDADDRESS, "127:0:0:1");
		 * return "localhost"; }
		 */
		InetAddress iadr = getLocalIpAddress();
		if (iadr != null) {
			String canonicalAddr = iadr.getCanonicalHostName();
			if (canonicalAddr != null && "null".equals(canonicalAddr) == false) { // Android
																					 // bug
				if (iadr.isAnyLocalAddress() == false) {
					srv.arguments.put(Acme.Serve.Serve.ARG_BINDADDRESS, iadr.getHostAddress());
					if (Main.DEBUG)
						Log.e(SERVICE_NAME, "bound:" + canonicalAddr);
					return canonicalAddr;
				} else {
					srv.arguments.remove(Acme.Serve.Serve.ARG_BINDADDRESS);
					iadr = getNonLookupAddress(false);
					if (iadr != null && InetAddressUtils.isIPv4Address(iadr.getHostAddress()))
						return iadr.getHostAddress();
					// else
				}
			}
		}
		srv.arguments.remove(Acme.Serve.Serve.ARG_BINDADDRESS);
		if (Main.DEBUG)
			Log.e(SERVICE_NAME, "No address bound");
		// return "127:0:0:1";
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "127:0:0:1"; // "::"
		}
	}
	
	void resetServ() {
		updateRealm();
		updateWWWServlet();
		srv.addWebsocketProvider(null);
		srv.deployApps();
		updateServletsList();
	}
	
	protected String deployAppFrom(String u) {
		FileOutputStream fos = null;
		InputStream cis = null;
		try {
			// TODO consider using DownloadManager
			URL url = new URL(u);
			String appFile = url.getFile();
			int sp = appFile.lastIndexOf('/');
			if (sp >= 0)
				appFile = appFile.substring(sp + 1);
			File warFile;
			URLConnection ucon = url.openConnection();
			ucon.setConnectTimeout(30 * 1000);
			Utils.copyStream(cis = ucon.getInputStream(), fos = new FileOutputStream(warFile = new File(deployDir, appFile)), 1024 * 1024 * 512);
			if (appFile.endsWith(WarRoller.DEPLOY_ARCH_EXT) == false || appFile.length() <= WarRoller.DEPLOY_ARCH_EXT.length()) {
				if (Main.DEBUG)
					Log.e(SERVICE_NAME, " Invalid extension for web archive file: " + appFile + ", it is stored but not deployed");
				return "Invalid extension for web archive file: " + appFile;
			}
			redeploy(appFile.substring(0, appFile.length() - WarRoller.DEPLOY_ARCH_EXT.length()));
			if (Main.DEBUG)
				Log.d(SERVICE_NAME, appFile + " has been deployed");
			// update list
			updateServletsList();
		} catch (IOException ioe) {
			if (Main.DEBUG)
				Log.e(SERVICE_NAME, "Could't deploy " + u, ioe);
			return "" + ioe;
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
				}
			if (cis != null)
				try {
					cis.close();
				} catch (IOException e) {
				}
		}
		return null;
	}
	
	void scanDeployments() {
		deployDir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				String fileName = pathname.getName();
				String appName = pathname.isFile() && fileName.toLowerCase().endsWith(WarRoller.DEPLOY_ARCH_EXT) ? fileName.substring(0, fileName.length() - WarRoller.DEPLOY_ARCH_EXT.length()) : null;
				if (appName != null && findServlet("/" + appName + "/*") == false) {
					srv.getDeployer().deployWar(pathname, new File(deployDir, WarRoller.DEPLOYMENT_DIR_TARGET));
					if (Main.DEBUG)
						Log.d(SERVICE_NAME, "Found a not deployed app  " + appName);
					return true;
				}
				return false;
			}
		});
		// scan for already deployed but not launched
		File[] deployedFiles = new File(deployDir, WarRoller.DEPLOYMENT_DIR_TARGET).listFiles();
		if (deployedFiles == null) {
			if (Main.DEBUG)
				Log.e(SERVICE_NAME, "Invaid deploy directory: " + new File(deployDir, WarRoller.DEPLOYMENT_DIR_TARGET));
			return;
		}
		
		for (File deployedFile : deployedFiles) {
			if (deployedFile.isDirectory() && findServlet("/" + deployedFile.getName() + "/*") == false) {
				if (Main.DEBUG)
					Log.d(SERVICE_NAME, "Found not deployed app  " + deployedFile);
				srv.deployApp(deployedFile.getName());
			}
		}
	}
	
	protected Object doAppOper(OperCode oc, String appName) {
		Servlet servlet = srv.getServlet(appName);
		if (servlet == null) {
			if (Main.DEBUG)
				Log.e(SERVICE_NAME, "No servlet found for " + appName);
			return null;
		}
		WebAppServlet appServlet = servlet instanceof WebAppServlet ? (WebAppServlet) servlet : null;
		switch (oc) {
			case info:
				return servlet.getServletInfo();
			case deploy:
				if (appServlet != null) {
					redeploy(appName.substring(0, appName.length() - "/*".length()));
					updateServletsList();
				}
				return servletsList;
			case remove:
				if (appServlet != null) {
					if (appName.endsWith("/*"))
						appName = appName.substring(1, appName.length() - 2);
					else if (appName.endsWith("/"))
						appName = appName.substring(1, appName.length() - 1);
					File appWar = new File(deployDir, appName + WarRoller.DEPLOY_ARCH_EXT);
					if (appWar.delete() == false)
						if (Main.DEBUG)
							Log.e(SERVICE_NAME, "File can't be deleted " + appWar);
						else {
							File appFile = new File(new File(deployDir, WarRoller.DEPLOYMENT_DIR_TARGET), appName);
							if (deleteRecursively(appFile) == false) {
								if (Main.DEBUG)
									Log.e(SERVICE_NAME, "File can't be deleted " + appFile);
							}
						}
				} else if (Main.DEBUG)
					Log.e(SERVICE_NAME, "Can't find app " + appName + " to remove");
				return null;
			case stop:
				servlet = srv.unloadServlet(servlet);
				if (servlet != null) {
					servlet.destroy();
					srv.unloadSessions(servlet.getServletConfig().getServletContext());
				} else if (Main.DEBUG)
					Log.e(SERVICE_NAME, "Couldn't unload servlet for " + appName);
				updateServletsList();
				return servletsList;
		}
		return null;
	}
	
	protected void redeploy(String appName) {
		Servlet servlet = srv.getServlet(appName + "/*");
		// TODO use this code for context menu reload to avoid crash
		if (servlet != null) {
			servlet = srv.unloadServlet(servlet);
			if (servlet != null && servlet instanceof WebAppServlet) {
				servlet.destroy();
				srv.unloadSessions(servlet.getServletConfig().getServletContext());
				File dexCacheDir = new File(new File(new File(deployDir, WarRoller.DEPLOYMENT_DIR_TARGET), appName), "META-INF/DEX/" + SERVICE_NAME);
				// Log.d(APP_NAME, ""+dexCacheDir);
				if (dexCacheDir.exists() && dexCacheDir.isAbsolute()) {
					deleteRecursively(dexCacheDir);
				}
			}
		}
		srv.getDeployer().deployWar(new File(deployDir, appName + WarRoller.DEPLOY_ARCH_EXT), new File(deployDir, WarRoller.DEPLOYMENT_DIR_TARGET));
		srv.deployApp(appName);
	}
	
	private boolean deleteRecursively(final File file) {
		for (File curf : file.listFiles()) {
			if (curf.isFile()) {
				if (curf.delete() == false) {
					return false;
				}
			} else if (curf.isDirectory()) {
				if (deleteRecursively(curf) == false) {
					return false;
				}
			} else {
				return false;
			}
		}
		
		return file.delete();
	}
	
	private void updateServletsList() {
		if (servletsList == null){
			servletsList = new ArrayList<String>();
		}
		else {
			servletsList.clear();
		}
		Enumeration servlets = srv.getServletNames();
		while (servlets.hasMoreElements()) {
			servletsList.add((String) servlets.nextElement());
		}
	}
	
	/**
	 * 
	 * @param servletName
	 * @return
	 */
	private boolean findServlet(String servletName) {
		for (String curName : servletsList) {
			if (curName.equals(servletName)) {
				return true;
			}
		}
		return false;
	}
	
	class AndroidServer extends Acme.Serve.Serve {
		/** deployer */
		private WarRoller deployer;
		
		public AndroidServ(Properties arguments, PrintStream logStream, Object runtime) {
			super(arguments, logStream);
			// provide servlet context Android environment access
			WebAppServlet.setRuntimeEnv(runtime);
			// addWebsocketProvider(WSProvider.class.toString());
		}
		
		@Override
		protected void addWebsocketProvider(String provider) {
			if (config.webSocketEnabled) {
				websocketProvider = new WSProvider();
				websocketProvider.init(this);
				websocketProvider.deploy(this, null);
			} else {
				websocketProvider = null;
			}
		}
		
		// Overriding method for public access
		@Override
		public void setMappingTable(PathTreeDictionary mappingtable) {
			super.setMappingTable(mappingtable);
		}
		
		@Override
		protected void setRealms(PathTreeDictionary realms) {
			super.setRealms(realms);
		}
		
		public synchronized void deployApps() {
			if (deployer == null)
				deployer = new WarRoller();
			try {
				deployer.deploy(this);
			} catch (Throwable t) {
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
				if (Main.DEBUG)
					Log.e(SERVICE_NAME, "Unexpected problem in deploying apps", t);
			}
		}
		
		public synchronized boolean deployApp(String appName) {
			// deployer must be not null
			try {
				WebAppServlet webAppServlet = WebAppServlet.create(new File(new File(deployDir, WarRoller.DEPLOYMENT_DIR_TARGET), appName), appName, this, null);
				addServlet(webAppServlet.getContextPath() + "/*", webAppServlet, null);
				return true;
			} catch (Throwable t) {
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
				if (Main.DEBUG)
					Log.e(SERVICE_NAME, "Problem in deployment " + appName, t);
			}
			return false;
		}
		
		WarRoller getDeployer() {
			return deployer;
		}
		
		protected void setAccessLogged(boolean on) {
			if (on)
				arguments.put(ARG_LOG_OPTIONS, "L");
			else
				arguments.remove(ARG_LOG_OPTIONS);
			setAccessLogged();
		}
	}
}
