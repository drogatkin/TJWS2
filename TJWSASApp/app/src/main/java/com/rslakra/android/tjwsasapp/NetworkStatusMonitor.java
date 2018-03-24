package com.rslakra.android.tjwsasapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Responds to system messages indicating a change in the
 * network's status.
 * <p/>
 *
 * This is an Android BroadcastReceiver, which means that
 * it is instantiated specifically to receive the specified
 * event. A single method is executed (onReceive()); then
 * this object is deleted. In addition, onReceive() runs
 * on the main thread. So we have to do whatever we're going
 * to do very quickly, and have one chance to get a message
 * into the body of our app before this object is killed.
 * <p/>
 *
 * Also, the documentation says that we MUST check the source of
 * the sender, because any other application could launch this
 * object. (John suggests that, in contrast, we can merely make
 * it "not exported" in the Manifest file, which means only
 * the system can use this. Awesome!)
 * <p/>
 *
 * For more information:
 * </p>
 *
 * - See onReceive ():
 * http://developer.android.com/reference/android/content/BroadcastReceiver.html#onReceive(android.content.Context,%20android.content.Intent)
 * <p/>
 *
 * - See registerReceiver ():
 * http://developer.android.com/reference/android/content/Context.html#registerReceiver(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20java.lang.String,%20android.os.Handler)
 * <p/>
 *
 * - The BroacastReceiver lifecycle:
 * http://developer.android.com/reference/android/content/BroadcastReceiver.html#ReceiverLifecycle
 *
 * @author Rohtash Singh Lakra
 */
public final class NetworkStatusMonitor extends BroadcastReceiver {
    
    /*
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        //check service is started or not.
        TJWSApp.checkReachability(context);
    }
    
    /**
     * Returns true if the device network is available otherwise false.
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cnnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cnnectivityManager.getActiveNetworkInfo();
        // should check null because in airplane mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }
    
}
