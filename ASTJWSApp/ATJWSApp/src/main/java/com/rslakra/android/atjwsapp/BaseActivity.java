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
