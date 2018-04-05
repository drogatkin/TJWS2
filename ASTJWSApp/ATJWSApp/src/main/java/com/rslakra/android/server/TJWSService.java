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

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;

import com.rslakra.android.framework.AndroidClassLoader;
import com.rslakra.android.framework.events.EventManager;
import com.rslakra.android.framework.events.EventType;
import com.rslakra.android.logger.LogHelper;
import com.rslakra.android.atjwsapp.TJWSApp;
import com.rslakra.android.framework.NetHelper;
import com.rslakra.android.framework.SSLHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.Properties;

import Acme.IOHelper;
import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;
import rogatkin.web.WebApp;
import rogatkin.web.WebAppServlet;

/**
 * Android TJWS server service
 *
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 03:39:08 PM*
 */
public class TJWSService extends Service {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "TJWSService";
    
    static {
        IOHelper.addBouncyCastleProvider();
    }
    
    /**
     * sslEnabled
     */
    private boolean sslEnabled;
    
    /**
     * bindAddress
     */
    private String bindAddress;
    
    /**
     * port
     */
    private int port;
    
    /**
     * logStream
     */
    private PrintStream logStream;
    
    /**
     * webServer
     */
    private TJWSServer webServer;
    
    /**
     * wifiLock
     */
    private WifiLock wifiLock;
    
    /**
     * mBinder
     */
    private final IBinder mBinder = new LocalBinder();
    
    /**
     * Default Constructor.
     */
    public TJWSService() {
        LogHelper.i(LOG_TAG, "TJWSService()");
    }
    
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
        return mBinder;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    
    /**
     * Starts the local server.
     *
     * @param sslEnabled
     */
    public void startLocalServer(final boolean sslEnabled) {
        if(!isWebServerRunning()) {
            initServer(sslEnabled);
            updateNetworkSettings();
            try {
                startServer();
            } finally {
                if(isWebServerRunning()) {
                    LogHelper.i(LOG_TAG, "TJWSServer has started!!!");
                    EventManager.sendEvent(EventType.SERVER_STARTED);
                }
            }
        } else {
            LogHelper.w(LOG_TAG, "TJWSServer is already running!!!");
        }
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
    public String getBindAddress() {
        return bindAddress;
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
     * Returns true if the web server is running.
     *
     * @return
     */
    public boolean isWebServerRunning() {
        return (webServer != null && webServer.isRunning());
    }
    
    /**
     * @return
     */
    public String getScheme() {
        return (isSSLEnabled() ? "https" : "http");
    }
    
    /**
     * Returns the local server URL string.
     *
     * @return
     */
    public String getServerUrl() {
        return (getScheme() + "://" + getBindAddress() + ":" + getPort() + "/");
    }
    
    /**
     * Initializes the logging stream.
     */
    // @SuppressLint("NewApi")
    private void initLogging() {
        if(logStream != null) {
            return;
        }
        
        File logDir = new File(TJWSApp.getInstance().getLogsFolder());
        // make sure, the folder exists.
        if(!logDir.exists()) {
            LogHelper.w(LOG_TAG, "The logs folder [" + logDir.getAbsolutePath() + "] does not exist!");
        }
        
        // initialize the logs file.
        try {
            logStream = new PrintStream(new File(logDir, "access-" + System.currentTimeMillis() + ".log"), "UTF-8");
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
        
        setSSLEnabled(sslEnabled);
        initLogging();
        setDefaultPort();
        
        File deployFolder = new File(TJWSApp.getInstance().getDeployFolder());
        // make sure, the folder exists.
        if(!deployFolder.exists()) {
            if(!deployFolder.mkdir()) {
                LogHelper.w(LOG_TAG, "Unable to create deploy folder:" + deployFolder.getAbsolutePath());
            }
        }
        
        // setting properties for the server, and exchangeable Acceptors
        Properties properties = new Properties();
        properties.setProperty(Serve.ARG_NOHUP, Serve.ARG_NOHUP);
        
        // log properties
        properties.setProperty(Serve.ARG_ACCESS_LOG_FMT, "{0} {2} [{3,date,yyyy/MM/dd HH:mm:ss Z}] \"{4} {5} {6}\" {7,number,#}");
        properties.setProperty(Serve.ARG_LOG_DIR, TJWSApp.getInstance().getLogsFolder());
        
        /* JSP */
        //            properties.setProperty(Serve.ARG_JSP, "org.apache.jasper.servlet.JspServlet");
        //            properties.setProperty("org.apache.jasper.servlet.JspServlet.classpath", "%classpath%");
        //            properties.setProperty("org.apache.jasper.servlet.JspServlet.scratchdir", "%deploydir%/META-INF/jsp-classes");
        
        if(isSSLEnabled()) {
            final boolean useAssetsPath = false;
            // SSL configurations.
            properties.setProperty(Serve.ARG_ACCEPTOR_CLASS, "Acme.Serve.SSLAcceptor");
            //            properties.setProperty(Serve.ARG_ACCEPTOR_CLASS, "rogatkin.wskt.SSLSelectorAcceptor");
            
            final String keyStoreFile = SSLHelper.SERVER_KEY_STORE_FILE;
            String keyStoreFilePath = null;
            if(useAssetsPath) {
                String parentFolderPath = IOHelper.pathString(TJWSService.class);
                LogHelper.d(LOG_TAG, "parentFolderPath:" + parentFolderPath);
                keyStoreFilePath = IOHelper.pathString(parentFolderPath, keyStoreFile);
                LogHelper.d(LOG_TAG, "keyStoreFilePath:" + keyStoreFilePath);
            } else {
                keyStoreFilePath = IOHelper.pathString(TJWSApp.getInstance().getDeployFolder(), keyStoreFile);
                final File fileKeyStore = new File(keyStoreFilePath);
                if(!fileKeyStore.getParentFile().exists()) {
                    if(!fileKeyStore.getParentFile().mkdirs()) {
                        LogHelper.d(LOG_TAG, "Unable to create folder:" + fileKeyStore.getParentFile().getAbsolutePath());
                    }
                }
                try {
//                    final InputStream keyStoreStream = LogHelper.readAssets(TJWSApp.getInstance().getApplicationContext(), keyStoreFile);
                    final InputStream keyStoreStream = LogHelper.readRAWResources(TJWSApp.getInstance().getApplicationContext(), "client");
//                    SSLHelper.setSSLTrustStore(keyStoreFilePath);
                    int keyStoreBytes = IOHelper.copyStream(keyStoreStream, new FileOutputStream(keyStoreFilePath), true);
                    LogHelper.d(LOG_TAG, "keyStoreBytes:" + keyStoreBytes);
                } catch(IOException ex) {
                    LogHelper.e(LOG_TAG, ex);
                } finally {
                    LogHelper.d(LOG_TAG, "fileKeyStore:" + fileKeyStore);
                    if(!fileKeyStore.exists()) {
                        LogHelper.w(LOG_TAG, "File does not exists at keyStoreFilePath:" + fileKeyStore.getAbsolutePath());
                    }
                }
            }
            
            properties.setProperty(SSLAcceptor.ARG_KEYSTOREFILE, keyStoreFilePath);
            properties.setProperty(SSLAcceptor.ARG_KEYSTORETYPE, KeyStore.getDefaultType());
            properties.setProperty(SSLAcceptor.ARG_PROTOCOL, TJWSServer.PROTOCOLS[3]);
            // properties.setProperty(SSLAcceptor.ARG_PROTOCOL,
            // TJWSServer.PROTOCOLS[1]);
            // properties.setProperty(SSLAcceptor.ARG_PROTOCOL,
            // TJWSServer.PROTOCOLS[2]);
            properties.setProperty(SSLAcceptor.ARG_CLIENTAUTH, "false");
            properties.setProperty(SSLAcceptor.ARG_KEYSTOREPASS, "password");
        } else {
            properties.remove(Serve.ARG_ACCEPTOR_CLASS);
            // this acceptor is required for web-socket support
            // properties.setProperty("acceptorImpl",
            // "Acme.Serve.SelectorAcceptor");
        }
        
        // set port
        properties.setProperty(Serve.ARG_PORT, String.valueOf(getPort()));
        
        webServer = new TJWSServer(properties, logStream, this);
        // Add root servlet
        webServer.addServlet("/", new EmbeddedServlet());
        
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
        System.setProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR, TJWSApp.getInstance().getDeployFolder());
        LogHelper.d(LOG_TAG, "webappdir:" + System.getProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR) + ", for app:" + TJWSApp.getInstance().getDeployFolder());
        
        //        // initialize the deploy directory
        //        webServer.deployApplications();
    }
    
    /**
     * Returns the binding address.
     *
     * @return
     */
    private InetAddress getLocalIPAddress() {
        try {
            if(LogHelper.isNullOrEmpty(getBindAddress())) {
                return NetHelper.getLookBackAddress();
            } else {
                return InetAddress.getByName(getBindAddress());
            }
        } catch(UnknownHostException ex) {
            LogHelper.e(LOG_TAG, "Unable to resolve IP address!", ex);
            return null;
        }
    }
    
    /**
     * Returns the hostname.
     *
     * @return
     */
    public void updateNetworkSettings() {
        InetAddress localAddress = getLocalIPAddress();
        if(localAddress != null) {
            bindAddress = localAddress.getCanonicalHostName();
            // Android bug
            if(!LogHelper.isNullOrEmpty(bindAddress) && "null".equals(bindAddress) == false) {
                if(!localAddress.isAnyLocalAddress()) {
                    webServer.setProperty(Serve.ARG_BINDADDRESS, localAddress.getHostAddress());
                    LogHelper.d(LOG_TAG, "hostName:" + getBindAddress() + ", bindAddress:" + localAddress.getHostAddress());
                } else {
                    webServer.removeProperty(Serve.ARG_BINDADDRESS);
                    localAddress = NetHelper.lookupINetAddress(false);
                    if(localAddress != null && NetHelper.isIPv4Address(localAddress.getHostAddress())) {
                        LogHelper.d(LOG_TAG, "hostName:" + getBindAddress() + ", bindAddress:" + localAddress.getHostAddress());
                    }
                }
            }
        }
        
        if(LogHelper.isNullOrEmpty(bindAddress)) {
            webServer.removeProperty(Serve.ARG_BINDADDRESS);
            LogHelper.e(LOG_TAG, "No address bound");
            try {
                bindAddress = InetAddress.getLocalHost().getHostAddress();
            } catch(UnknownHostException e) {
                bindAddress = "127:0:0:1";
            }
        }
        
        if(!LogHelper.isNullOrEmpty(bindAddress)) {
            // bind address
            LogHelper.d(LOG_TAG, "bind to " + getBindAddress() + ", deployed to:" + TJWSApp.getInstance().getDeployFolder());
            // logging configurations.
            webServer.setLogEnabled(true);
            
            // port configurations.
            if(isSSLEnabled()) {
                webServer.setProperty(SSLAcceptor.ARG_PORT, getPort());
            } else {
                webServer.setProperty(Serve.ARG_PORT, getPort());
            }
        }
        
        // initialize the deploy directory
        webServer.setProperty(SSLAcceptor.ARG_IFADDRESS, webServer.getProperty(Serve.ARG_BINDADDRESS));
        webServer.deployApplications();
    }
    
    /**
     * Starts the server.
     */
    private void startServer() {
        if(!isWebServerRunning()) {
            /** thread to check server status. */
            new Thread() {
                /**
                 * Check the staus of the server.
                 *
                 * @see Thread#run()
                 */
                @Override
                public void run() {
                    try {
                        // give a chance to server to run
                        Thread.sleep(1000);
                    } catch(InterruptedException ex) {
                        // ignore me!
                    } finally {
                        LogHelper.i(LOG_TAG, "Serve Running:" + webServer.isRunning());
                        if(isWebServerRunning()) {
                            LogHelper.i(LOG_TAG, "TJWSServer has started!!!");
                            EventManager.sendEvent(EventType.SERVER_STARTED);
                        } else {
                            LogHelper.i(LOG_TAG, "TJWSServer has not started!!!");
                            EventManager.sendEvent(EventType.SERVER_STOPPED);
                        }
                    }
                }
            }.start();
            
            /** thread to run the server. */
            new Thread() {
                /**
                 * Starts the web server here.
                 *
                 * @see Thread#run()
                 */
                @Override
                public void run() {
                    // getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    // WifiManager wifiManager = (WifiManager)
                    // getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    // wifiLock = wifiManager.createWifiLock(LOG_TAG);
                    //
                    try {
                        // lock on wifi to start server.
                        // wifiLock.acquire();
                        Serve.Status result = webServer.serve();
                        LogHelper.i(LOG_TAG, "Serve result:" + result);
                    } catch(Throwable throwable) {
                        LogHelper.e(LOG_TAG, throwable);
                    } finally {
                        // unlock the wifi lock after server start call.
                        // if(wifiLock != null && wifiLock.isHeld()) {
                        // wifiLock.release();
                        // }
                    }
                }
            }.start();
        }
    }
    
    /**
     * Stops the webServer and all it's services.
     *
     * @param stopAllServices
     */
    public void stopServer(final boolean stopAllServices) {
        if(isWebServerRunning()) {
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
     * Destroys the current service.
     */
    @Override
    public void onDestroy() {
        LogHelper.d(LOG_TAG, "Service Destroyed!");
        // just in case, not stoppped yet.
        stopServer(true);
        super.onDestroy();
    }
}
