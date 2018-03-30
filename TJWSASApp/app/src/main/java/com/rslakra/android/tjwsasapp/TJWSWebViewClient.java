package com.rslakra.android.tjwsasapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.rslakra.android.logger.LogHelper;

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
     * @see android.webkit.WebViewClient#onPageStarted(android.webkit.WebView, * java.lang.String,
     * android.graphics.Bitmap)
     */
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        LogHelper.d(LOG_TAG, "onPageStarted(" + view + ", " + url + ", " + favicon + ")");
        showHideProgressDialog(true);
    }
    
    /**
     * @see android.webkit.WebViewClient#shouldOverrideUrlLoading(android.webkit.WebView, *
     * java.lang.String)
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
        Toast.makeText(view.getContext(), description, Toast.LENGTH_LONG).show();
    }
    
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        LogHelper.d(LOG_TAG, "onReceivedSslError(" + view + ", " + handler + ", " + error + ")");
        handler.proceed(); // Ignore SSL certificate errors
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
