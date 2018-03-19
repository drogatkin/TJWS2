/**
 * Copyright 2012 Dmitriy Rogatkin, All rights reserved.
 * $Id: TJWSApp.java,v 1.3 2012/09/15 17:47:27 dmitriy Exp $
 */
package com.rslakra.mobile.atjews;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.devamatre.logger.LogManager;
import com.rslakra.mobile.logger.LogHelper;
import com.rslakra.mobile.logger.LogType;
import com.rslakra.mobile.server.TJWSService;

import java.lang.Thread.UncaughtExceptionHandler;

public class TJWSApp extends Application {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "TJWSApp";
    
    /**
     * tjwsService
     */
    protected TJWSService serviceControl;
    
    public TJWSApp() {
        LogManager.configure(LogManager.LOG4J_PROPERTY_FILE);
        LogHelper.setLogType(LogType.DEBUG);
    }
    
    /**
     *
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if(Thread.getDefaultUncaughtExceptionHandler() == null)
            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                
                public void uncaughtException(Thread thread, Throwable ex) {
                    LogHelper.e(LOG_TAG, "Unhandled exception " + ex + " in the thread: " + thread, ex);
                }
            });
        startServe();
    }
    
    /**
     * @return
     */
    public TJWSService getServiceControl() {
        return serviceControl;
    }
    
    public void startServe() {
        // sanity
        if(serviceControl != null) {
            return;
        }
        
        final Intent serviceIntent = new Intent(this, TJWSService.class);
        if(!isMyServiceRunning(TJWSService.class)) {
            startService(serviceIntent);
        }
        
        bindService(serviceIntent, new ServiceConnection() {
            
            public void onServiceConnected(ComponentName name, IBinder service) {
//                serviceControl = TJWSService.asInterface(service);
                // can send notification to activities here
//                serviceControl = service;
            }
            
            public void onServiceDisconnected(ComponentName name) {
                LogHelper.d(LOG_TAG, "Disconnected " + name);
                serviceControl = null;
            }
        }, BIND_AUTO_CREATE);
    }
    
    /**
     * @param serviceClass
     * @return
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        
        return false;
    }
}
