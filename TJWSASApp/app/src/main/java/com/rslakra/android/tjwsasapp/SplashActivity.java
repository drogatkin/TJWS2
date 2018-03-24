package com.rslakra.android.tjwsasapp;

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
            final boolean useWebView = false;
            if(useWebView) {
                MainActivity.startMainActivity(this, true);
            } else {
                final Context mContext = TJWSApp.getInstance().getApplicationContext();
                TestConnection testConnection = new TestConnection(mContext, true);
                final boolean checkSocketConnection = true;
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
