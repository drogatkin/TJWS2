/**
 * Copyright 2011 Dmitriy Rogatkin, All rights reserved.
 * $Id: TJWSService.java,v 1.15 2012/09/15 17:47:27 dmitriy Exp $
 */
package com.rslakra.mobile.server;

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

import com.rslakra.mobile.AndroidClassLoader;

import Acme.IOHelper;
import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;
import Acme.Serve.ServeStatus;
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
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.rslakra.mobile.atjews.Config;
import com.rslakra.mobile.atjews.MainActivity;
import com.rslakra.mobile.atjews.SettingServlet;
import com.rslakra.mobile.logger.LogHelper;
import com.rslakra.mobile.utils.INetHelper;

/**
 * Android TJWS server service
 *
 * @author drogatki
 */
public class TJWSService extends Service {
    
    public enum OperCode {
        INFO, STOP, REMOVE, DEPLOY
    }
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "TJWSService";
    
    public static final String DEPLOYMENTDIR = "atjeews/webapps";
    public static final String LOGDIR = "logs";
    public static final String KEYSTORE_DIR = "key";
    public static final String KEYSTORE = "keystore";
    
    /**
     * sslEnabled
     */
    private boolean sslEnabled;
    
    /**
     * port
     */
    private int port;
    
    /**
     * logsFolder
     */
    public String logsFolder;
    /**
     * logStream
     */
    private PrintStream logStream;
    
    /**
     * deployFolder
     */
    public String deployFolder;
    public File deployFolderFile;
    
    /**
     * webServer
     */
    private TJWSServer webServer;
    /**
     * status
     */
    private static ServeStatus status = ServeStatus.IO_ERROR;
    
    /**
     * config
     */
    private Config config;
    /**
     * servletsList
     */
    private ArrayList<String> servletsList;
    /**
     * wifiLock
     */
    private WifiLock wifiLock;
    
    private TJWSService bindService;
    
    /**
     * Default Constructor.
     */
    public TJWSService() {
        LogHelper.i(LOG_TAG, "TJWSService()");
    }
    
    private final IBinder localBinder = new LocalBinder();
    
    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public TJWSService getService() {
            return TJWSService.this;
        }
    }
    
    /**
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        LogHelper.d(LOG_TAG, "Binding from " + intent.getClass().getName());
        initServer(isSSLEnabled());
        return localBinder;
    }
    
    
    /**
     * Returns the config object.
     *
     * @return
     */
    public Config getConfig() {
        return config;
    }
    
    /**
     * Returns the list of registered servlets.
     *
     * @return
     */
    public ArrayList<String> getServletsList() {
        return servletsList;
    }
    
    /**
     * @return
     */
    public boolean isSSLEnabled() {
        return sslEnabled;
    }
    
    /**
     * @param sslEnabled
     */
    public void setSSLEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }
    
    /**
     * @return
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Sets the default port.
     */
    public void setDefaultPort() {
        if(isSSLEnabled()) {
            setPort(9161);
        } else {
            setPort(5161);
        }
    }
    
    /**
     * @param port
     */
    public void setPort(int port) {
        LogHelper.d(LOG_TAG, "setPort(" + port + ")");
        this.port = port;
    }
    
    /**
     * Returns the logsFolder path.
     *
     * @return
     */
    public String getLogsFolder() {
        return logsFolder;
    }
    
    /**
     * The logsFolder to be set.
     *
     * @param logsFolder
     */
    public void setLogsFolder(final String logsFolder) {
        this.logsFolder = logsFolder;
    }
    
    /**
     * Returns the deployFolder path.
     *
     * @return
     */
    public String getDeployFolder() {
        return deployFolder;
    }
    
    /**
     * The logsFolder to be set.
     *
     * @param deployFolder
     */
    public void setDeployFolder(final String deployFolder) {
        this.deployFolder = deployFolder;
    }
    
    /**
     * Returns true if the web server is running.
     *
     * @return
     */
    public boolean isRunning() {
        return (webServer != null && status != ServeStatus.RUNNING);
    }
    
    /**
     * Stops the webServer and all it's services.
     *
     * @param stopAllServices
     */
    public void stopServer(final boolean stopAllServices) {
        if(isRunning()) {
            webServer.notifyStop();
        }
        
        if(stopAllServices) {
            webServer.destroyAllServlets();
            webServer.setDeployed(false);
            webServer = null;
            
            if(logStream != System.out && logStream != null) {
                logStream.close();
                logStream = null;
            }
        }
        
        LogHelper.i(LOG_TAG, "Web Server is stopped successfully!");
    }
    
    /**
     * Stops the web server.
     */
    public void stopServer() {
        stopServer(true);
    }
    
    /**
     *
     */
    public void startServer() {
        if(!isRunning()) {
            new Thread() {
                /**
                 * Starts the web server here.
                 *
                 * @see Thread#run()
                 */
                @Override
                public void run() {
                    int code = ServeStatus.RUNNING.getStatus();
                    getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    wifiLock = wifiManager.createWifiLock(LOG_TAG);
                    
                    try {
                        //lock on wifi to start server.
                        wifiLock.acquire();
                        code = webServer.serve();
                    } catch(Throwable throwable) {
                        LogHelper.e(LOG_TAG, throwable);
                        code = ServeStatus.IO_ERROR.getStatus();
                    } finally {
                        //unlock the wifi lock after server start call.
                        if(wifiLock != null && wifiLock.isHeld()) {
                            wifiLock.release();
                        }
                        LogHelper.i(LOG_TAG, "Serve status:" + status);
                        // TODO - Find out right way to notify the client.
                    }
                }
            }.start();
        }
    }


//    private final TJWSService.Stub mBinder = new TJWSService.Stub() {
//
//        public String start() throws RemoteException {
//            String result = updateNetworkSettings();
//            startServer();
//            return result;
//        }
//
//        public void stop() throws RemoteException {
//            stopServer();
//        }
//
//        public int getStatus() throws RemoteException {
//            return status.getStatus();
//        }
//
//        public void logging(boolean enable) throws RemoteException {
//            srv.setAccessLogged(enable);
//        }
//
//        public List<String> getApps() throws RemoteException {
//            return servletsList;
//        }
//
//        public String deployApp(String url) throws RemoteException {
//            return deployAppFrom(url);
//        }
//
//        public List<String> rescanApps() throws RemoteException {
//            scanDeployments();
//            updateServletsList();
//            return servletsList;
//        }
//
//        public String getAppInfo(String name) throws RemoteException {
//            return (String) doAppOper(OperCode.info, name);
//        }
//
//        public List<String> stopApp(String name) throws RemoteException {
//            return (List<String>) doAppOper(OperCode.stop, name);
//        }
//
//        public void removeApp(String name) throws RemoteException {
//            doAppOper(OperCode.remove, name);
//        }
//
//        public List<String> redeployApp(String name) throws RemoteException {
//            return (List<String>) doAppOper(OperCode.deploy, name);
//        }
//    };
    
    /**
     * Destroys the current service.
     */
    @Override
    public void onDestroy() {
        LogHelper.d(LOG_TAG, "Service Destroyed!");
        // just in case, not stoppped yet.
        stopServer(true);
        super.onDestroy();
    }
    
    /**
     * Initializes the logging stream.
     */
//    @SuppressLint("NewApi")
    public void initLogging() {
        if(logStream != null) {
            return;
        }
        
        File logDir = null;
        if(!LogHelper.isNullOrEmpty(getLogsFolder())) {
            logDir = new File(getLogsFolder(), LOGDIR);
        } else if(LogHelper.isProtectedFileSystem()) {
            logDir = new File(getExternalCacheDir(), LOGDIR);
        } else {
            logDir = new File(Environment.getExternalStorageDirectory(), LOGDIR);
        }
        
        //make sure, the folder exists.
        if(!logDir.exists()) {
            LogHelper.d(LOG_TAG, "Creating [" + logDir.getAbsolutePath() + "] folder ...");
            logDir.mkdirs();
        }
        LogHelper.d(LOG_TAG, "Open log [" + logDir.getAbsolutePath() + "]");
        
        //initialize the logs file.
        try {
            logStream = new PrintStream(new File(logDir, "access-"
                    + System.currentTimeMillis() + ".log"), "UTF-8");
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, "Unable to create log file!", ex);
        }
        
        if(logStream == null) {
            logStream = System.out;
        } else {
            System.setErr(logStream);
        }
    }
    
    /**
     * Initializes the embedded web server.
     *
     * @param sslEnabled
     */
    public void initServer(final boolean sslEnabled) {
        if(webServer != null) {
            LogHelper.i(LOG_TAG, "Web server is already initialized!");
            return;
        }
        
        //init configuration
        if(config == null) {
            config = new Config();
        }
        config.load(getApplicationContext());
        setSSLEnabled(sslEnabled);
        
        initLogging();
        setDefaultPort();
        
        
        // setting properties for the server, and exchangeable Acceptors
        Properties properties = new Properties();
        properties.setProperty(Serve.ARG_NOHUP, Serve.ARG_NOHUP);
        
        // log properties
        properties.setProperty(Serve.ARG_ACCESS_LOG_FMT, "{0} {2} [{3,date,yyyy/MM/dd HH:mm:ss Z}] \"{4} {5} {6}\" {7,number,#}");
        properties.setProperty(Serve.ARG_LOG_DIR, getLogsFolder());
            
            /* JSP */
//            properties.setProperty(Serve.ARG_JSP, "org.apache.jasper.servlet.JspServlet");
//            properties.setProperty("org.apache.jasper.servlet.JspServlet.classpath", "%classpath%");
//            properties.setProperty("org.apache.jasper.servlet.JspServlet.scratchdir", "%deploydir%/META-INF/jsp-classes");
        
        if(isSSLEnabled()) {
            // SSL configurations.
            properties.setProperty(Serve.ARG_PORT, String.valueOf(getPort()));
            // properties.setProperty(SSLAcceptor.ARG_PORT,
            // String.valueOf(getPort()));
            properties.setProperty(Serve.ARG_ACCEPTOR_CLASS, "Acme.Serve.SSLAcceptor");
            // properties.setProperty(Serve.ARG_ACCEPTOR_CLASS,
            // "rogatkin.wskt.SSLSelectorAcceptor");
            String parentFolderPath = IOHelper.pathString(TJWSService.class);
            LogHelper.d(LOG_TAG, "parentFolderPath:" + parentFolderPath);
            final String keyStoreFilePath = IOHelper.pathString(parentFolderPath, "conf/tjws.bks");
            LogHelper.d(LOG_TAG, "keyStoreFilePath:" + keyStoreFilePath);
            properties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, keyStoreFilePath);
            properties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, "BKS");
            properties.setProperty(SSLAcceptor.ARG_CLIENTAUTH, "no");
            properties.setProperty(SSLAcceptor.ARG_KEYSTOREPASS, "password");
        } else {
            properties.remove(Serve.ARG_ACCEPTOR_CLASS);
            // this acceptor is required for web-socket support
            // properties.setProperty("acceptorImpl",
            // "Acme.Serve.SelectorAcceptor");
        }
        
        webServer = new TJWSServer(properties, logStream, this);
        //Add root servlet
        webServer.addServlet("/", new EmbeddedServlet());
        // add settings servlet
        webServer.addServlet("/settings", new SettingServlet(this));
        
        webServer.setProperty(Serve.ARG_PORT, getPort());
        
        // add shutdown hook.
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                stopServer(true);
            }
        }));
        
        /** Set System properties. */
        System.setProperty(WebAppServlet.WAR_NAME_AS_CONTEXTPATH, "yes");
        // set dex class loader 'AndroidClassLoader'
        System.setProperty(WebApp.DEF_WEBAPP_CLASSLOADER, AndroidClassLoader.class.getName());
        System.setProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR, getDeployFolder());
        LogHelper.d(LOG_TAG, "webappdir:" + System.getProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR) + ", for app:" + getDeployFolder());
        
        //initialize the deploy directory
        initDeployDirectory();
        resetServer();
        webServer.deployApplications();
    }
    
    /**
     *
     */
    public void initDeployDirectory() {
        LogHelper.d(LOG_TAG, "deploy dir:" + getDeployFolder() + ", for app:" + getFilesDir());
        if(deployFolderFile != null && deployFolderFile.exists()) {
            return;
        }
        
        LogHelper.d(LOG_TAG, "use sd:" + config.useSD + ", deployed to:" + getDeployFolder());
        config.useSD = !LogHelper.isProtectedFileSystem();
        if(config.useSD) {
            deployFolderFile = new File(Environment.getExternalStorageDirectory(), DEPLOYMENTDIR);
            if(deployFolderFile.exists() || deployFolderFile.mkdirs()) {
                System.setProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR, deployFolderFile.getPath());
            } else {
                config.useSD = false;
            }
        }
        
        if(config.useSD == false) {
            deployFolderFile = new File(getFilesDir(), DEPLOYMENTDIR);
            if(deployFolderFile.exists() == false && deployFolderFile.mkdirs() == false) {
                LogHelper.d(LOG_TAG, "Can't establish web apps deployment directory:" + deployFolderFile);
                deployFolderFile = new File("/sdcard", DEPLOYMENTDIR);
            }
            System.setProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR, deployFolderFile.getPath());
        }
        
        LogHelper.d(LOG_TAG, "deploy dir " + deployFolderFile + " is " + deployFolderFile.exists());
    }
    
    public void updateWWWServlet() {
        Servlet rootServlet = webServer.getServlet("/*");
        LogHelper.d(LOG_TAG, "Root app :" + config.rootApp + ", servlet / " + rootServlet);
        if("/".equals(config.rootApp)) {
            Acme.Serve.Serve.PathTreeDictionary aliases = new Acme.Serve.Serve.PathTreeDictionary();
            aliases.put("/*", new File(config.wwwFolder));
            webServer.setMappingTable(aliases);
            if(rootServlet instanceof FileServlet == false) {
                if(rootServlet != null) {
                    webServer.unloadServlet(rootServlet);
                }
                // optional file servlet
                webServer.addDefaultServlets(null);
            }
        } else {
            if(rootServlet != null) {
                webServer.unloadServlet(rootServlet);
                rootServlet.destroy();
                webServer.unloadSessions(rootServlet.getServletConfig().getServletContext());
            }
            if(config.rootApp != null) {
                System.setProperty(WebAppServlet.WAR_DEPLOY_IN_ROOT, config.rootApp.substring(1));
            } else {
                System.getProperties().remove(WebAppServlet.WAR_DEPLOY_IN_ROOT);
            }
        }
    }
    
    public void updateRealm() {
        Acme.Serve.Serve.PathTreeDictionary realms = new Acme.Serve.Serve.PathTreeDictionary();
        if(config.password != null && config.password.length() > 0) {
            Acme.Serve.Serve.BasicAuthRealm realm;
            realms.put("/settings", realm = new Serve.BasicAuthRealm(MainActivity.APP_NAME));
            realm.put("", config.password);
        }
        webServer.setRealms(realms);
    }
    
    public File getKeyDir() {
        File result = new File(deployFolderFile, "../" + KEYSTORE_DIR);
        if(!result.exists()) {
            result.mkdirs();
        }
        
        return result;
    }
    
    void storeConfig() {
        config.store(this);
    }
    
    public InetAddress getLocalIpAddress() {
        try {
            if(config.bindAddr != null)
                return InetAddress.getByName(config.bindAddr);
        } catch(UnknownHostException ex) {
            LogHelper.e(LOG_TAG, "Can't resolve :" + config.bindAddr + " " + ex.toString());
            return null;
        }
        return getLookbackAddress();
        // return getNonLookupAddress();
    }
    
    public static InetAddress getLookbackAddress() {
        InetAddress result = null;
        try {
            for(Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for(Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(inetAddress.isLoopbackAddress()) {
                        if(INetHelper.isIPv4Address(inetAddress.getHostAddress())) {
                            return inetAddress;
                        }
                        
                        result = inetAddress;
                        // if (MainActivity.DEBUG)
                        // Log.e(SERVICE_NAME, "processed addr:"+result);
                    }
                }
            }
        } catch(SocketException ex) {
            LogHelper.e(LOG_TAG, ex);
        }
        
        return result;
    }
    
    public static InetAddress getNonLookupAddress(boolean any) {
        InetAddress result = null;
        try {
            for(Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for(Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if(!inetAddress.isLoopbackAddress()) {
                        if((inetAddress.isSiteLocalAddress() == false || any) && INetHelper.isIPv4Address(inetAddress.getHostAddress())) {
                            return inetAddress;
                        }
                        result = inetAddress;
                    }
                }
            }
        } catch(SocketException ex) {
            LogHelper.e(LOG_TAG, ex);
        }
        
        return result;
    }
    
    /**
     * Returns the network settings.
     *
     * @return
     */
    public String updateNetworkSettings() {
        config.load(this);
        // port
        webServer.setProperty(Serve.ARG_PORT, config.port);
        // SSL
        if(config.ssl) {
            webServer.setProperty(Serve.ARG_ACCEPTOR_CLASS, config.websocket_enab ? "rogatkin.wskt.SSLSelectorAcceptor" : "Acme.Serve.SSLAcceptor");
            webServer.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, new File(getKeyDir(), KEYSTORE).getPath());
            webServer.setProperty(SSLAcceptor.ARG_KEYSTOREPASS, config.password == null || "".equals(config.password) ? "changeme" : config.password);
            webServer.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, "BKS");
            LogHelper.e(LOG_TAG, "SSL configured as:" + webServer.arguments);
        } else if(config.websocket_enab)
            webServer.setProperty(Serve.ARG_ACCEPTOR_CLASS, "Acme.Serve.SelectorAcceptor");
        else
            webServer.removeProperty(Serve.ARG_ACCEPTOR_CLASS);
        webServer.setLogEnabled(config.logEnabled);
        
        // bind address
        LogHelper.e(LOG_TAG, "bind to " + config.bindAddr + ",use sd:" + config.useSD + ", deployed to:" + deployFolderFile);
        resetServer();
        /*
         * if (config.bindAddr == null) {
		 * srv.arguments.put(Acme.Serve.Serve.ARG_BINDADDRESS, "127:0:0:1");
		 * return "localhost"; }
		 */
        InetAddress iadr = getLocalIpAddress();
        if(iadr != null) {
            String canonicalAddr = iadr.getCanonicalHostName();
            if(canonicalAddr != null && "null".equals(canonicalAddr) == false) { // Android
                // bug
                if(iadr.isAnyLocalAddress() == false) {
                    webServer.arguments.put(Acme.Serve.Serve.ARG_BINDADDRESS, iadr.getHostAddress());
                    LogHelper.d(LOG_TAG, "bound:" + canonicalAddr);
                    return canonicalAddr;
                } else {
                    webServer.arguments.remove(Acme.Serve.Serve.ARG_BINDADDRESS);
                    iadr = getNonLookupAddress(false);
                    if(iadr != null && INetHelper.isIPv4Address(iadr.getHostAddress())) {
                        return iadr.getHostAddress();
                    }
                }
            }
        }
        webServer.removeProperty(Serve.ARG_BINDADDRESS);
        LogHelper.e(LOG_TAG, "No address bound");
        //return "127:0:0:1";
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch(UnknownHostException e) {
            return "127:0:0:1"; // "::"
        }
    }
    
    /**
     * Resets the server and it's settings.
     */
    public void resetServer() {
        updateRealm();
        updateWWWServlet();
        webServer.addWebsocketProvider(null);
        webServer.deployApplications();
        updateServletsList();
    }
    
    public String deployAppFrom(String urlString) {
        FileOutputStream fos = null;
        InputStream cis = null;
        try {
            // TODO consider using DownloadManager
            URL url = new URL(urlString);
            String appFile = url.getFile();
            int sp = appFile.lastIndexOf('/');
            if(sp >= 0) {
                appFile = appFile.substring(sp + 1);
            }
            
            File warFile;
            URLConnection ucon = url.openConnection();
            ucon.setConnectTimeout(30 * 1000);
            Utils.copyStream(cis = ucon.getInputStream(), fos = new FileOutputStream(warFile = new File(deployFolderFile, appFile)), 1024 * 1024 * 512);
            if(appFile.endsWith(WarRoller.DEPLOY_ARCH_EXT) == false || appFile.length() <= WarRoller.DEPLOY_ARCH_EXT.length()) {
                LogHelper.d(LOG_TAG, " Invalid extension for web archive file: " + appFile + ", it is stored but not deployed");
                return "Invalid extension for web archive file: " + appFile;
            }
            redeploy(appFile.substring(0, appFile.length() - WarRoller.DEPLOY_ARCH_EXT.length()));
            LogHelper.d(LOG_TAG, appFile + " has been deployed");
            // update list
            updateServletsList();
        } catch(IOException ex) {
            LogHelper.e(LOG_TAG, ex);
            return "" + ex;
        } finally {
            IOHelper.safeClose(fos);
            IOHelper.safeClose(cis);
        }
        
        return null;
    }
    
    /**
     *
     */
    public void scanDeployments() {
        deployFolderFile.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                String fileName = pathname.getName();
                String appName = pathname.isFile() && fileName.toLowerCase().endsWith(WarRoller.DEPLOY_ARCH_EXT) ? fileName.substring(0, fileName.length() - WarRoller.DEPLOY_ARCH_EXT.length()) : null;
                if(appName != null && findServlet("/" + appName + "/*") == false) {
                    webServer.getDeployer().deployWar(pathname, new File(deployFolderFile, WarRoller.DEPLOYMENT_DIR_TARGET));
                    LogHelper.d(LOG_TAG, "Found a not deployed app  " + appName);
                    return true;
                }
                return false;
            }
        });
        
        // scan for already deployed but not launched
        File[] deployedFiles = new File(deployFolderFile, WarRoller.DEPLOYMENT_DIR_TARGET).listFiles();
        if(deployedFiles == null) {
            LogHelper.d(LOG_TAG, "Invaid deploy directory: " + new File(deployFolderFile, WarRoller.DEPLOYMENT_DIR_TARGET));
            return;
        }
        
        for(File deployedFile : deployedFiles) {
            if(deployedFile.isDirectory() && findServlet("/" + deployedFile.getName() + "/*") == false) {
                LogHelper.d(LOG_TAG, "Found not deployed app  " + deployedFile);
                webServer.deployApplication(deployedFile.getName());
            }
        }
    }
    
    /**
     * Executes the given operation for the given app.
     *
     * @param operCode
     * @param appName
     * @return
     */
    public Object doAppOperation(OperCode operCode, String appName) {
        Servlet servlet = webServer.getServlet(appName);
        if(servlet == null) {
            LogHelper.e(LOG_TAG, "No servlet found for " + appName);
            return null;
        }
        
        WebAppServlet appServlet = servlet instanceof WebAppServlet ? (WebAppServlet) servlet : null;
        
        switch(operCode) {
            case INFO:
                return servlet.getServletInfo();
            case DEPLOY:
                if(appServlet != null) {
                    redeploy(appName.substring(0, appName.length() - "/*".length()));
                    updateServletsList();
                }
                return servletsList;
            case REMOVE:
                if(appServlet != null) {
                    if(appName.endsWith("/*"))
                        appName = appName.substring(1, appName.length() - 2);
                    else if(appName.endsWith("/"))
                        appName = appName.substring(1, appName.length() - 1);
                    File appWar = new File(getDeployFolder(), appName + WarRoller.DEPLOY_ARCH_EXT);
                    if(appWar.delete() == false) {
                        LogHelper.d(LOG_TAG, "File can't be deleted " + appWar);
                    } else {
                        File appFile = new File(new File(deployFolderFile, WarRoller.DEPLOYMENT_DIR_TARGET), appName);
                        if(deleteRecursively(appFile) == false) {
                            LogHelper.d(LOG_TAG, "File can't be deleted " + appFile);
                        }
                    }
                } else {
                    LogHelper.d(LOG_TAG, "Can't find app " + appName + " to remove");
                }
                return null;
            case STOP:
                servlet = webServer.unloadServlet(servlet);
                if(servlet != null) {
                    servlet.destroy();
                    webServer.unloadSessions(servlet.getServletConfig().getServletContext());
                } else {
                    LogHelper.d(LOG_TAG, "Couldn't unload servlet for " + appName);
                }
                
                updateServletsList();
                return servletsList;
        }
        
        return null;
    }
    
    public void redeploy(String appName) {
        Servlet servlet = webServer.getServlet(appName + "/*");
        // TODO use this code for context menu reload to avoid crash
        if(servlet != null) {
            servlet = webServer.unloadServlet(servlet);
            if(servlet != null && servlet instanceof WebAppServlet) {
                servlet.destroy();
                webServer.unloadSessions(servlet.getServletConfig().getServletContext());
                File dexCacheDir = new File(new File(new File(deployFolderFile, WarRoller.DEPLOYMENT_DIR_TARGET), appName), "META-INF/DEX/" + LOG_TAG);
                // Log.d(APP_NAME, ""+dexCacheDir);
                if(dexCacheDir.exists() && dexCacheDir.isAbsolute()) {
                    deleteRecursively(dexCacheDir);
                }
            }
        }
        webServer.getDeployer().deployWar(new File(deployFolderFile, appName + WarRoller.DEPLOY_ARCH_EXT), new File(deployFolderFile, WarRoller.DEPLOYMENT_DIR_TARGET));
        webServer.deployApplication(appName);
    }
    
    public boolean deleteRecursively(File topf) {
        for(File curf : topf.listFiles()) {
            if(curf.isFile()) {
                if(curf.delete() == false)
                    return false;
            } else if(curf.isDirectory()) {
                if(deleteRecursively(curf) == false)
                    return false;
            } else
                return false;
        }
        
        return topf.delete();
    }
    
    public void updateServletsList() {
        if(servletsList == null)
            servletsList = new ArrayList<String>();
        else
            servletsList.clear();
        Enumeration servlets = webServer.getServletNames();
        while(servlets.hasMoreElements()) {
            servletsList.add((String) servlets.nextElement());
        }
    }
    
    public boolean findServlet(String servletName) {
        for(String curName : servletsList) {
            if(curName.equals(servletName))
                return true;
        }
        
        return false;
    }
}
