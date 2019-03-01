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

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.rslakra.android.logger.LogHelper;

/**
 * The Main Activity.
 *
 * @author Rohtash Singh
 * @version 1.0.0
 * @since Apr 28, 2015 7:14:11 PM
 */
public class MainActivity extends BaseActivity {
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "MainActivity";
    
    /**
     * webViewContainer
     */
    private FrameLayout webViewContainer;
    
    /**
     * mWebView
     */
    private WebView mWebView;
    
    public MainActivity() {
        LogHelper.i(LOG_TAG, "MainActivity()");
    }
    
    /**
     * Starts the MainActivity.
     *
     * @param activity
     * @param finishPrevious
     */
    public static void startMainActivity(final Activity activity, final boolean finishPrevious) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                activity.startActivity(intent);
                if(finishPrevious) {
                    activity.finish();
                }
            }
        });
    }
    
    /**
     * Initialize the components.
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(LOG_TAG, "+onCreate()");
        // set main activity
        setContentView(R.layout.activity_main);
        getTJWSApp().setParentActivity(this);
        
        // initialize WebView.
        initWebView();
        
        // Load WebView Contents.
        loadWebviewContents();
        LogHelper.i(LOG_TAG, "-onCreate()");
    }
    
    
    /**
     * Initializes the WebView with default settings and web view client.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webViewContainer = (FrameLayout) findViewById(R.id.webViewContainer);
        if(mWebView == null) {
            try {
                // jupWebView = (WebView) findViewById(R.id.jupWebView);
                mWebView = new WebView(this);
            } catch(Throwable ex) {
                LogHelper.e(LOG_TAG, ex);
            }
            
            mWebView.setLayoutParams(new ViewGroup.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT));
            /*
             * page loading webpage loses its focus so this line just bring the
			 * focus back to page.
			 */
            mWebView.requestFocus(View.FOCUS_DOWN);
            
            WebSettings jupWebSettings = mWebView.getSettings();
            // enable JavaScript
            jupWebSettings.setJavaScriptEnabled(true);
            jupWebSettings.setDomStorageEnabled(true);
            // loads the WebView completely zoomed out
            jupWebSettings.setLoadWithOverviewMode(true);
            
			/*
             * true makes the Webview have a normal viewport such as a normal
			 * desktop browser when false the webview will have a viewport
			 * constrained to it's own dimensions
			 */
            jupWebSettings.setUseWideViewPort(true);
            
			/*
             * While this will allow pinch-to-zoom, it will also display a zoom
			 * overlay control (Galaxy S3). In order to disable the on-screen
			 * zoom tool, but retain the pinch-to-zoom functionality, you need
			 * to call webView.setDisplayZoomControls(false) as well.
			 */
            jupWebSettings.setBuiltInZoomControls(false);
            jupWebSettings.setDisplayZoomControls(false);
            
            // override the web client to open all links in the same webview
            mWebView.setWebViewClient(new TJWSWebViewClient(this));
            mWebView.setWebChromeClient(new TJWSWebClient());
            
			/*
             * Injects the supplied Java object into this WebView. The object is
			 * injected into the JavaScript context of the main frame, using the
			 * supplied name. This allows the Java object's public methods to be
			 * accessed from JavaScript.
			 */
            WebView.setWebContentsDebuggingEnabled(true);
            //            mWebView.addJavascriptInterface(new JavaScriptProxy(getApplicationContext(), this), "Android");
            
			/* disable WebView cache */
            mWebView.clearCache(true);
            jupWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            jupWebSettings.setAppCacheEnabled(false);
            
            // jupWebSettings.setSaveFormData(false);
            // jupWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            // getApplicationContext().deleteDatabase("webview.db");
            // getApplicationContext().deleteDatabase("webviewCache.db");
            
            // stops the long selection options.
            // jupWebView.setOnLongClickListener(new OnLongClickListener() {
            // /**
            // * @see
            // android.view.View.OnLongClickListener#onLongClick(android.view.View)
            // */
            // @Override
            // public boolean onLongClick(View v) {
            // return true;
            // }
            // });
            // jupWebView.setLongClickable(false);
        } else {
            // Remove existing WebView from its placeholder.
            webViewContainer.removeView(mWebView);
        }
        
        // Attach the WebView to its placeholder
        webViewContainer.addView(mWebView);
    }
    
    /**
     * This methods loads the static contents .ZIP file from server and loads
     * the root URL of an application.
     */
    private void loadWebviewContents() {
        // get the path of home directory
        this.runOnUiThread(new Runnable() {
            /**
             * Loads the contents of the webpage.
             *
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
//                String urlString = "https://www.google.com/";
                String urlString = TJWSApp.getTJWSService().getServerUrl();
                LogHelper.i(LOG_TAG, "urlString:" + urlString);
                mWebView.loadUrl(urlString);
            }
        });
    }
    
    
    /**
     * Saves the state of the WebView.
     *
     * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the state of the WebView
        mWebView.saveState(outState);
    }
    
    /**
     * Restores the state of the WebView.
     *
     * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore the state of the WebView
        mWebView.restoreState(savedInstanceState);
    }
    
    /**
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {
        LogHelper.d(LOG_TAG, "onDestroy()");
        super.onDestroy();
    }
    
    /**
     *
     */
    @Override
    protected void onResume() {
        super.onResume();
        initWebView();
    }
    
    /**
     * @param visible
     */
    public void showHideWebView(final boolean visible) {
        this.runOnUiThread(new Runnable() {
            /**
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                if(mWebView != null) {
                    mWebView.setVisibility((visible ? View.VISIBLE : View.GONE));
                }
            }
        });
    }
    
    
    /**
     * Reloads the webView.
     */
    public void reload() {
        if(mWebView != null) {
            loadWebviewContents();
        }
    }
    
}
