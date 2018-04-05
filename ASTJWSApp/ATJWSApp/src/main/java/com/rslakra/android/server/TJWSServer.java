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

import com.rslakra.android.logger.LogHelper;
import com.rslakra.android.atjwsapp.TJWSApp;

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
     * mWarRoller
     */
    private WarRoller mWarRoller;
    
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
        return mWarRoller;
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
                if(getDeployer() == null) {
                    mWarRoller = new WarRoller();
                }
                getDeployer().deploy(this);
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