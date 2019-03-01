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
package com.rslakra.android.atjwsapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.rslakra.android.framework.events.AndroidEvent;
import com.rslakra.android.framework.events.EventManager;
import com.rslakra.android.framework.events.EventType;
import com.rslakra.android.logger.LogHelper;

public class SplashActivity extends BaseActivity {
    
    /** LOG_TAG */
    private static final String LOG_TAG = "SplashActivity";
    
    public SplashActivity() {
        LogHelper.i(LOG_TAG, "SplashActivity()");
    }
    
    
    /**
     * Starts Next activity.
     */
    private void startNextActivity() {
        LogHelper.d(LOG_TAG, "startNextActivity");
        
        final boolean withService = false;
        EventManager.subscribe(this, EventType.SERVER_STARTED, EventType.ERROR);
        if(withService) {
            this.startService(new Intent(this, LocalServerService.class));
        } else {
            getTJWSService().startLocalServer(true);
        }
    }
    
    
    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(LOG_TAG, "onCreate()");
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);
        TJWSApp.setParentActivity(this);
        
//        try {
//            ProviderInstaller.installIfNeeded(this);
//        } catch(GooglePlayServicesRepairableException ex) {
//            // Thrown when Google Play Services is not installed, up-to-date, or enabled
//            // Show dialog to allow users to install, update, or otherwise enable Google Play services.
//            GooglePlayServicesUtil.getErrorDialog(ex.getConnectionStatusCode(), this, 0);
//        } catch(GooglePlayServicesNotAvailableException ex) {
//            LogHelper.e(LOG_TAG, "Google Play Services not available.", ex);
//        }
        
        if(getTJWSService() != null) {
            startNextActivity();
        } else {
            EventManager.subscribe(this, EventType.SERVICE_CONNECTED);
        }
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
        switch(event.getType()) {
            case SERVICE_CONNECTED:
                startNextActivity();
                break;
            
            case SERVER_STARTED:
                startMainActivity();
                break;
            
            case SERVER_STOPPED:
            case ERROR:
                LogHelper.d(LOG_TAG, "Show Error Message!", event);
                handleError(this, false);
                break;
            
            default:
                break;
        }
    }
    
    
    /**
     * Starts the main activity.
     */
    private void startMainActivity() {
        // check if the server is started properly
        if(TJWSApp.getTJWSService().isWebServerRunning()) {
            EventManager.unsubscribe(this, EventType.SERVICE_CONNECTED, EventType.SERVER_STARTED);
            final boolean useWebView = true;
            if(useWebView) {
                MainActivity.startMainActivity(this, true);
            } else {
                final Context mContext = TJWSApp.getInstance().getApplicationContext();
                TestConnection testConnection = new TestConnection(mContext, true);
                final boolean checkSocketConnection = false;
                if(checkSocketConnection) {
                    testConnection.testSSLSocketConnection();
                } else {
                    testConnection.testSSLConnection();
                }
            }
        } else {
            LogHelper.i(LOG_TAG, "The local server hasn't started yet!");
            handleError(this, false);
        }
    }
    
    /**
     * Shows the provided error message.
     * Closes the progress dialog, if visible.
     *
     * @param activity
     * @param exitApplication
     */
    private void handleError(final BaseActivity activity, final boolean exitApplication) {
        try {
            LogHelper.i(LOG_TAG, "handleError(" + activity + ", " + exitApplication + ")");
            activity.runOnUiThread(new Runnable() {
                /**
                 * @see java.lang.Runnable#run()
                 */
                @Override
                public void run() {
                    // now show error message.
                    final AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(activity);
                    mAlertBuilder.setCancelable(true);
                    
                    String errorMessage = "ERROR!";
                    if(exitApplication) {
                        errorMessage = errorMessage = activity.getString(R.string.fatalErrorMessage);
                    } else {
                        errorMessage = activity.getString(R.string.errorMessage);
                    }
                    
                    LogHelper.i(LOG_TAG, "errorMessage:" + errorMessage);
                    mAlertBuilder.setMessage(errorMessage);
                    mAlertBuilder.setTitle(activity.getString(R.string.errorDialogTitle));
                    mAlertBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        
                        /**
                         *
                         */
                        public void onClick(DialogInterface dialog, int id) {
                            if(exitApplication) {
                                LogHelper.w(LOG_TAG, "Killing the app!");
                                
                                // final int mErrorDisplayTimeout =
                                // StringUtils.stringAsInteger(activity.getString(R.string.error_display_timeout));
                                // Debug.w(DEBUG_KEY, "mErrorDisplayTimeout:" +
                                // mErrorDisplayTimeout);
                                // Thread.sleep(mErrorDisplayTimeout);
                                System.exit(0);
                            }
                        }
                    });
                    mAlertBuilder.show();
                }
            });
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        }
    }
    
}
