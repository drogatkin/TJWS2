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
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.rslakra.android.framework.SSLHelper;
import com.rslakra.android.logger.LogHelper;

import java.io.InputStream;
import java.security.cert.Certificate;

/**
 * Provides an opportunity to intercept the WebView calls.
 *
 * @author Rohtash Singh
 * @version 1.0.0
 * @since Apr 28, 2015 7:16:05 PM
 */
public class TJWSWebViewClient extends WebViewClient {
    
    /** LOG_TAG */
    private static final String LOG_TAG = "TJWSWebViewClient";
    
    /** mParentActivity */
    private final Activity mParentActivity;
    
    /** mProgressDialog */
    private ProgressDialog mProgressDialog;
    
    /**
     *
     */
    public TJWSWebViewClient(Activity parentActivity) {
        super();
        this.mParentActivity = parentActivity;
        if(mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mParentActivity);
            mProgressDialog.setTitle(mParentActivity.getString(R.string.loading));
            mProgressDialog.setMessage(mParentActivity.getString(R.string.please_wait));
        }
    }
    
    /**
     * Shows or Hides the progress dialog.
     *
     * @param show
     */
    private void showHideProgressDialog(final boolean show) {
        if(show) {
            if(mProgressDialog != null && !mProgressDialog.isShowing()) {
                mProgressDialog.show();
            }
        } else {
            if(mParentActivity != null && !mParentActivity.isFinishing()) {
                if(mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }
        }
    }
    
    /**
     * Notify the host application that a page has started loading. This method
     * is called once for each main frame load so a page with iframes or
     * framesets will call onPageStarted one time for the main frame. This also
     * means that onPageStarted will not be called when the contents of an
     * embedded frame changes, i.e. clicking a link whose target is an iframe.
     *
     * @see android.webkit.WebViewClient#onPageStarted(android.webkit.WebView, java.lang.String, android.graphics.Bitmap)
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        LogHelper.d(LOG_TAG, "onPageStarted(" + view + ", " + url + ", " + favicon + ")");
        showHideProgressDialog(true);
    }
    
    /**
     * @see android.webkit.WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView, java.lang.String)
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        LogHelper.d(LOG_TAG, "shouldOverrideUrlLoading(" + view + ", " + url + ")");
        return true;
    }
    
    /**
     * Notify the host application that a page has finished loading. This method
     * is called only for main frame. When onPageFinished() is called, the
     * rendering picture may not be updated yet. To get the notification for the
     * new Picture, use {@link WebView.PictureListener#onNewPicture}.
     *
     * @see android.webkit.WebViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
     */
    @Override
    public void onPageFinished(WebView view, String urlString) {
        LogHelper.d(LOG_TAG, "onPageFinished(" + view + ", " + urlString + ")");
        // super.onPageFinished(view, url);
        showHideProgressDialog(false);
    }
    
    /**
     * onReceivedError
     */
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        LogHelper.d(LOG_TAG, "onReceivedError(" + view + ", " + errorCode + ", " + description + ", " + failingUrl + ")");
        LogHelper.showToastMessage(view.getContext(), description);
    }
    
    @Override
    public void onReceivedSslError(WebView mWebView, SslErrorHandler handler, SslError error) {
        LogHelper.d(LOG_TAG, "onReceivedSslError(" + mWebView + ", " + handler + ", " + error + ")");
        try {
            /**
             * Ignore SSL certificate errors (if this is only for self-signed certificate)
             *
             * The MITM attacks (where the attacker secretly relays and possibly alters the
             * communication between two parties who believe they are directly communicating with
             * each other) are not hidden and to handle them seamlessly, the best solution is to
             * pinning the certificates.
             *
             * NOTE: Sadly, SSLError gives us an SslCertificate when you call getCertificate().
             * SslCertificate is kind of useless. It's public API doesn't allow you to verify the
             * public key, only the created on, expired date, issued to, issued by, etc..
             * However, if you open up this class you will see an X509Certificate member variable
             * that is un-exposed.
             *
             * Instead of going into it deep, lets implement the solution. But there is an API for
             * getting the Bundle, and that X509 Certificate member variable gets stored in there.
             * So we access it that way, because the Certificate has a lot more useful methods on it.
             */
            //            if(error != null && error.getUrl() != null && error.getUrl().contains("localhost") && error.getCertificate() != null && "RSLakra Inc.".equals(error.getCertificate().getIssuedBy().getOName())) {
            if(error != null && error.getUrl() != null && error.getUrl().contains("localhost")) {
                InputStream certStream = LogHelper.readAssets(mWebView.getContext(), "client.pem");
                final Certificate pinnedCertificate = SSLHelper.toCertificate(SSLHelper.loadPEMCertificate(certStream));
                final Certificate serverCertificate = SSLHelper.toCertificate(error.getCertificate());
                if(pinnedCertificate.equals(serverCertificate)) {
                    handler.proceed();
                } else {
                    super.onReceivedSslError(mWebView, handler, error);
                }
            } else {
                super.onReceivedSslError(mWebView, handler, error);
            }
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        }
    }
    
    @Override
    public void onReceivedClientCertRequest(WebView view, ClientCertRequest certRequest) {
        LogHelper.d(LOG_TAG, "onReceivedClientCertRequest(" + view + ", " + certRequest + ")");
    }
    
    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        LogHelper.d(LOG_TAG, "onReceivedHttpAuthRequest(" + view + ", " + handler + ", " + host + ", " + realm + ")");
    }
}
