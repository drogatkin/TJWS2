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
