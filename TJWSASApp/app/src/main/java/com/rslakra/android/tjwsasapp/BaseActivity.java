package com.rslakra.android.tjwsasapp;

import android.app.Activity;
import android.os.Bundle;

import com.rslakra.android.framework.events.AndroidEvent;
import com.rslakra.android.framework.events.EventListener;
import com.rslakra.android.logger.LogHelper;
import com.rslakra.android.server.TJWSService;

/**
 * @Author: Rohtash Singh Lakra
 * @Created: 2018/03/20 10:45 AM
 */
public abstract class BaseActivity extends Activity implements EventListener {
    
    /** LOG_TAG */
    private static final String LOG_TAG = "BaseActivity";
    
    //mTJWSApp
    private TJWSApp mTJWSApp;
    
    public BaseActivity() {
    
    }
    
    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //initialize the singleton instance.
        mTJWSApp = (TJWSApp) getApplication();
    }
    
    /**
     * This service is started and bound on Application start, so we don't need to rebind for every
     * activity.
     */
    public TJWSApp getTJWSApp() {
        return mTJWSApp;
    }
    
    /**
     * This service is started and bound on Application start, so we don't need to rebind for every
     * activity.
     */
    public TJWSService getTJWSService() {
        return TJWSApp.getTJWSService();
    }
    
    
    /**
     * <b>Override?</b> to receive messages from your events.
     *
     * <b>Call super( ) ?</b> Always. BVActivity provides basic functionality
     * for logging out when needed, and invokes methods enabling you to make
     * features available when the app goes online and offline.
     *
     * @param event The event object itself.
     */
    public void onEvent(AndroidEvent event) {
        LogHelper.d(LOG_TAG, event);
    }
    
}
