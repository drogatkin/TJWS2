/**
 *
 */
package com.rslakra.android.server;

import com.rslakra.android.logger.LogHelper;
import com.rslakra.android.tjwsasapp.TJWSApp;
import java.io.File;
import java.io.PrintStream;
import java.util.Properties;

import Acme.IOHelper;
import rogatkin.web.WarRoller;
import rogatkin.web.WebApp;
import rogatkin.web.WebAppServlet;

/**
 * @author Rohtash Singh Lakra
 * @version 1.0.0
 * @date 03/15/2018 04:00:49 PM
 */
public final class TJWSServer extends Acme.Serve.Serve {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "TJWSServer";
    
    /** PROTOCOLS */
    public static final String[] PROTOCOLS = new String[]{"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"};
    
    /**
     * warDeployer
     */
    private WarRoller warDeployer;
    
    /**
     * deployed
     */
    private boolean deployed;
    
    /**
     * The default constructor which is the only one that works in android.
     *
     * @param arguments
     * @param logStream
     * @param runtime
     */
    public TJWSServer(final Properties arguments, PrintStream logStream, Object runtime) {
        super(arguments, logStream);
        /* provide SERVLET context for Android environment access */
        try {
            WebAppServlet.setRuntimeEnv(runtime);
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        }
        //addWebsocketProvider(WSProvider.class.toString());
        LogHelper.i(LOG_TAG, "TJWSServer(" + arguments + ", " + logStream + ", " + runtime + ")");
    }
    
    /**
     * @return
     */
    public boolean isDeployed() {
        return deployed;
    }
    
    /**
     * The deployed to be set.
     *
     * @param deployed
     */
    public void setDeployed(boolean deployed) {
        LogHelper.d(LOG_TAG, "setDeployed(" + deployed + ")");
        this.deployed = deployed;
    }
    
    /**
     * Returns the WAR deployer.
     *
     * @return
     */
    public WarRoller getDeployer() {
        return warDeployer;
    }
    
    /**
     * Returns the web-server deployment folder path.
     *
     * @return
     */
    public final String getDeployFolder() {
        return (String) getProperty(WebApp.DEF_WEBAPP_AUTODEPLOY_DIR);
    }
    
    @Override
    public void initMime() {
        mime = new Properties();
        try {
            mime.load(LogHelper.readAssets(TJWSApp.getInstance().getApplicationContext(), "mime.properties"));
            LogHelper.d(LOG_TAG, "MIME map is loaded successfully!");
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex, "MIME map can't be loaded!");
        }
    }
    
    /**
     * @param provider
     */
    @Override
    public void addWebsocketProvider(String provider) {
        if(IOHelper.isNullOrEmpty(provider)) {
            websocketProvider = null;
        } else {
            websocketProvider = new WSProvider();
            websocketProvider.init(this);
            websocketProvider.deploy(this, null);
        }
    }
    
    /**
     * Overriding method for public access
     *
     * @param mappingTable
     * @see Acme.Serve.Serve#setMappingTable(Acme.Serve.Serve.PathTreeDictionary)
     */
    @Override
    public void setMappingTable(PathTreeDictionary mappingTable) {
        LogHelper.d(LOG_TAG, "setMappingTable(" + mappingTable + ")");
        super.setMappingTable(mappingTable);
    }
    
    /**
     * (non-Javadoc)
     *
     * @see Acme.Serve.Serve#setRealms(Acme.Serve.Serve.PathTreeDictionary)
     */
    @Override
    protected void setRealms(PathTreeDictionary realms) {
        LogHelper.d(LOG_TAG, "setRealms(" + realms + ")");
        super.setRealms(realms);
    }
    
    /**
     * Returns the value of the given property.
     *
     * @param key
     * @return
     */
    public synchronized final Object getProperty(final Object key) {
        return super.arguments.get(key);
    }
    
    /**
     * @param key
     * @param value
     */
    @SuppressWarnings("unchecked")
    public synchronized void setProperty(Object key, Object value) {
        LogHelper.d(LOG_TAG, "setProperty(" + key + ", " + value + ")");
        if(super.arguments == null) {
            super.arguments = new Properties();
        }
        
        super.arguments.put(key, value);
    }
    
    /**
     * Removes the specified property.
     *
     * @param key
     */
    public synchronized void removeProperty(Object key) {
        LogHelper.d(LOG_TAG, "removeProperty(" + key + ")");
        if(super.arguments != null) {
            super.arguments.remove(key);
        }
    }
    
    /**
     * Deploy the local web server into the warDeployer.
     */
    public synchronized void deployApplications() {
        LogHelper.d(LOG_TAG, "+deployApplications(), deployed:" + isDeployed());
        
        if(!isDeployed()) {
            try {
                if(warDeployer == null) {
                    warDeployer = new WarRoller();
                }
                warDeployer.deploy(this);
                setDeployed(true);
            } catch(Throwable ex) {
                LogHelper.e(LOG_TAG, "Unexpected problem in deployment!", ex);
                if(ex instanceof ThreadDeath) {
                    throw (ThreadDeath) ex;
                }
            }
        }
        
        LogHelper.d(LOG_TAG, "-deployApplications(), deployed:" + isDeployed());
    }
    
    /**
     * Deploys the given app in the local web-server.
     *
     * @param appName
     * @return
     */
    public synchronized boolean deployApplication(String appName) {
        // deployer must be not null
        try {
            WebAppServlet webAppServlet = WebAppServlet.create(new File(new File(getDeployFolder(), WarRoller.DEPLOYMENT_DIR_TARGET), appName), appName, this, null);
            addServlet(webAppServlet.getContextPath() + "/*", webAppServlet, null);
            return true;
        } catch(Throwable ex) {
            LogHelper.e(LOG_TAG, "Unexpected problem in deployment appName:" + appName, ex);
            if(ex instanceof ThreadDeath) {
                throw (ThreadDeath) ex;
            }
        }
        
        return false;
    }
    
    /**
     * @param logEnabled
     */
    protected void setLogEnabled(boolean logEnabled) {
        if(logEnabled) {
            setProperty(ARG_LOG_OPTIONS, "L");
        } else {
            removeProperty(ARG_LOG_OPTIONS);
        }
        
        setAccessLogged();
    }
    
}