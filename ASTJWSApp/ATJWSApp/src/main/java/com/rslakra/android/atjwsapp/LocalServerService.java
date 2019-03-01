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

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.rslakra.android.logger.LogHelper;

/**
 * @Author: Rohtash Singh Lakra
 * @Created: 2018/03/21 2:36 PM
 */
public class LocalServerService extends IntentService {

    /** LOG_TAG */
    private static final String LOG_TAG = "LocalServerService";

    /** mBinder */
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocalServerService getService() {
            // Return this instance of LocalService so clients can call public
            // methods
            return LocalServerService.this;
        }
    }

    /**
     * @param name
     */
    public LocalServerService(final String name) {
        super(name);
    }

    /**
     *
     */
    public LocalServerService() {
        this(LOG_TAG);

    }

    // ----------------------------------------------------------------
    // System startup messages
    // ----------------------------------------------------------------

    /**
     * @see android.app.IntentService#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        LogHelper.i(LOG_TAG, "JUPService is created.");
    }

    /**
     * Call this method to (a) wake up the service if it isn't already awake,
     * and (b) get an object which lets you call methods directly on the
     * service.
     *
     * @param intent
     * @return
     * @see android.app.IntentService#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * @param intent
     * @return
     * @see android.app.Service#onUnbind(android.content.Intent)
     */
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * This method is invoked on my process' UI thread. (!!!) That means: if
     * this method does something long-running, it will interrupt any Activity
     * currently visible, even though this is "in the background." So we have to
     * get off the main thread as fast as possible.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     * @see android.app.IntentService#onStartCommand(android.content.Intent, * int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Cleans up our threads when the Service is stopped, whether by the user,
     * an internal configuration, the system, or the control panel.
     *
     * WARNING! If we do NOT clean up the threads, they seem to live FOREVER.
     */
    @Override
    public void onDestroy() {
        LogHelper.i(LOG_TAG, "JUPService is destroyed.");
        super.onDestroy();
    }

    /**
     * @param intent
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        TJWSApp.getTJWSService().startLocalServer(false);
    }

}
