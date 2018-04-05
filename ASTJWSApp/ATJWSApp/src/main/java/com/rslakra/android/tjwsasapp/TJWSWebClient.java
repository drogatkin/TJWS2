package com.rslakra.android.tjwsasapp;

import android.app.AlertDialog;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.rslakra.android.logger.LogHelper;

/**
 * @author Rohtash Singh
 * @version 1.0.0
 * @since Jun 15, 2015 1:04:16 PM
 */
public class TJWSWebClient extends WebChromeClient {
    
    /** LOG_TAG */
    private static final String LOG_TAG = "TJWSWebClient";
    
    /**
     * Displays alert message in Web View.
     *
     * @param webView
     * @param url
     * @param message
     */
    @Override
    public boolean onJsAlert(WebView webView, String url, String message, JsResult jsResult) {
        LogHelper.d(LOG_TAG, "onJsAlert(" + webView + ", " + url + ", " + message + ", " + jsResult + ")");
        new AlertDialog.Builder(webView.getContext()).setMessage(message).setCancelable(true).show();
        jsResult.confirm();
        return true;
    }
}
