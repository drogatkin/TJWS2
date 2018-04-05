/**
 * Copyright 2012 Dmitriy Rogatkin, All rights reserved.
 * $Id: TJWSApp.java,v 1.3 2012/09/15 17:47:27 dmitriy Exp $
 */
package com.rslakra.android.tjwsasapp;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;

import com.rslakra.android.framework.events.EventManager;
import com.rslakra.android.framework.events.EventType;
import com.rslakra.android.logger.LogHelper;
import com.rslakra.android.logger.LogType;
import com.rslakra.android.server.TJWSService;

import java.io.File;

/**
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 03:39:08 PM
 */
public class TJWSApp extends Application {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "TJWSApp";
    
    /**
     * EXTERNAL_STORAGE_DATA_DIRECTORY
     */
    private static File EXTERNAL_STORAGE_DATA_DIRECTORY = Environment.getExternalStoragePublicDirectory("/Android/data");
    
    /**
     * EXTERNAL_STORAGE_DATA_DIRECTORY
     */
    private static final boolean ENABLE_EXTERNAL_STORAGE = false;
    
    /**
     * SPLASH_WAIT_TIME
     */
    private static final int SPLASH_WAIT_TIME = 1000;
    
    /**
     * instance
     */
    private static TJWSApp sInstance;
    
    /**
     * serviceControl
     */
    private static TJWSService sTJWSService;
    
    /* parentActivity */
    private static BaseActivity sParentActivity;
    
    // DEBUG_KEY
    private static final String DEBUG_KEY = "BvApplication";
    
    
    /**
     * Default Constructor.
     */
    public TJWSApp() {
    }
    
    /**
     * @return
     */
    public static TJWSApp getInstance() {
        return sInstance;
    }
    
    /**
     * @return
     */
    public static TJWSService getTJWSService() {
        return sTJWSService;
    }
    
    /**
     * @return BaseActivity
     */
    public static BaseActivity getParentActivity() {
        return sParentActivity;
    }
    
    /**
     * @param parentActivity
     */
    public static void setParentActivity(final BaseActivity parentActivity) {
        sParentActivity = parentActivity;
    }
    
    /**
     * Returns the package name of the application.
     * <p>
     * This package name must match as per the AndroidManifest.xml
     * file 'package="com.boardvantage.meetxjup"'
     *
     * @return
     */
    public String getAppPackageName() {
        return getInstance().getApplicationContext().getPackageName();
    }
    
    /**
     * Returns the application's home directory path.
     *
     * @return
     * @throws Exception
     */
    public String getAppHomeDir() {
        if(ENABLE_EXTERNAL_STORAGE) {
            return LogHelper.pathString(EXTERNAL_STORAGE_DATA_DIRECTORY.getAbsolutePath(), getAppPackageName());
        } else {
            return getInstance().getApplicationContext().getFilesDir().getAbsolutePath();
        }
    }
    
    
    /**
     * Returns the application's home directory path.
     *
     * @return
     * @throws Exception
     */
    public String getLogsFolder() {
        return LogHelper.pathString(getAppHomeDir(), "logs");
    }
    
    /**
     * @return
     */
    public String getDeployFolder() {
        return LogHelper.pathString(getAppHomeDir(), "webapps");
    }
    
    /**
     *
     */
    @Override
    public void onCreate() {
        super.onCreate();
        //initialize the singleton instance.
        if(TJWSApp.getInstance() == null) {
            synchronized(TJWSApp.class) {
                if(TJWSApp.getInstance() == null) {
                    TJWSApp.sInstance = this;
                }
            }
        }
        
        if(Thread.getDefaultUncaughtExceptionHandler() == null)
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                
                public void uncaughtException(Thread thread, Throwable ex) {
                    LogHelper.e(LOG_TAG, "Unhandled exception " + ex + " in the thread: " + thread, ex);
                }
            });
        
        
        // initialize logger
        LogHelper.log4jConfigure(getInstance().getLogsFolder(), LogType.DEBUG);
        LogHelper.i(LOG_TAG, "onCreate()");
        rslakra.logger.LogHelper.setLogEnabled(true);
        
        // initialize service
        initService();
    }
    
    /**
     * @param sleepTime
     */
    private void broadcastAppServiceConnectedEvent(final int sleepTime) {
        Thread loginStartThread = new Thread(new Runnable() {
            public void run() {
                if(sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch(InterruptedException ex) {
                        //ignore me!
                    }
                }
                
                //send notification that server is connected.
                EventManager.sendEvent(EventType.SERVICE_CONNECTED);
            }
        });
        
        loginStartThread.start();
    }
    
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LogHelper.d(LOG_TAG, "onServiceConnected(" + className + ", " + service + ")");
            // We've bound to LocalService, cast the IBinder and get
            // LocalService instance
            TJWSService.LocalBinder binder = (TJWSService.LocalBinder) service;
            sTJWSService = (TJWSService) binder.getService();
            //            sTJWSService.onServiceConnected();
            // can send notification to activities here serviceControl = service;
            broadcastAppServiceConnectedEvent(SPLASH_WAIT_TIME);
            //            sTJWSService.startLocalServer(false);
            //            MainActivity.startMainActivity(TJWSApp.getInstance().getParentActivity(), true);
        }
        
        /**
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogHelper.d(LOG_TAG, "onServiceDisconnected(" + componentName + ")");
            EventManager.sendEvent(EventType.SERVICE_DISCONNECTED);
            //            appService.onServiceDisconnected();
            sTJWSService = null;
        }
    };
    
    /**
     *
     */
    public void initService() {
        // sanity
        if(getTJWSService() != null) {
            return;
        }
        
        final Thread serviceCreationThread = new Thread(new Runnable() {
            public void run() {
                /** This starts only if t is not currently running. So we don't need to worry about multiple starts.*/
                final Intent serviceIntent = new Intent(TJWSApp.this, TJWSService.class);
                if(!isServiceRunning(TJWSService.class)) {
                    TJWSApp.this.startService(serviceIntent);
                }
                /** Defines callbacks for service binding, passed to bindService() */
                bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            }
        });
        serviceCreationThread.run();
    }
    
    /**
     * Returns true if the given class service is running otherwise false.
     *
     * @param serviceClass
     * @return
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if(serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        
        return false;
    }
    
    
    /**
     * Send myself a message with a correctly-formatted Intent.
     * To do that, I have to "start" myself, even though I'm already
     * started. No problem.
     * <p>
     * This method is intended to be used by the
     * NetworkConnectivityTester's BroadcastReceiver when it hears
     * about an actual network-reachability change.
     *
     * @param mContext The context passed to the broadcast receiver. No idea what it is, but I
     * don't care, because it lets me send myself a message.
     */
    public static void checkReachability(final Context mContext) {
        // only the key matters to me, not the payload. See onStartCommand().
        mContext.startService(new Intent(mContext, TJWSService.class));
    }
    
}