/**
 *
 */
package com.rslakra.android.server;

import android.content.Context;

import com.rslakra.android.logger.LogHelper;
import com.rslakra.android.tjwsasapp.TJWSApp;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import Acme.IOHelper;

/**
 * The <code>EmbeddedServlet</code> handles all local requests.
 *
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 03:39:08 PM
 */
public final class EmbeddedServlet extends HttpServlet {
    
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    /**
     * LOG_TAG
     */
    private static final String LOG_TAG = "EmbeddedServlet";
    
    /**
     * INVALID_REQUEST
     */
    private static final byte[] INVALID_REQUEST = "Invalid Request".intern().getBytes();
    
    public EmbeddedServlet() {
        LogHelper.i(LOG_TAG, "EmbeddedServlet()");
    }
    
    /**
     * @param servletConfig
     * @throws ServletException
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        LogHelper.d(LOG_TAG, "init(" + servletConfig + ")");
    }
    
    /**
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }
    
    /**
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }
    
    /**
     * Processes all requests.
     *
     * @param servletRequest
     * @param servletResponse
     * @throws ServletException
     * @throws IOException
     */
    private void process(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        LogHelper.d(LOG_TAG, "process(" + servletRequest + "," + servletResponse + ")");
        try {
            byte[] dataBytes = INVALID_REQUEST;
            final Context mContext = TJWSApp.getParentActivity().getApplicationContext();
            String pathSegment = servletRequest.getRequestURI();
            LogHelper.d(LOG_TAG, "pathSegment:" + pathSegment);
            if(pathSegment.endsWith("/") || pathSegment.endsWith("html")) {
                dataBytes = IOHelper.readBytes(LogHelper.readAssets(mContext, "web/index.html"), true);
                LogHelper.d(LOG_TAG, "dataBytes:\n" + IOHelper.toUTF8String(dataBytes) + "\n");
                IOHelper.sendResponse(IOHelper.CONTENT_TYPE_HTML, dataBytes, servletResponse);
            } else if(pathSegment.endsWith("favicon.ico")) {
                IOHelper.sendResponse(IOHelper.CONTENT_TYPE_ICON, IOHelper.readFavIconBytes(), servletResponse);
            } else if(pathSegment.endsWith(".js")) {
                if(pathSegment.startsWith("/")) {
                    pathSegment = pathSegment.substring(1);
                }
                dataBytes = IOHelper.readBytes(LogHelper.readAssets(mContext, pathSegment), true);
                IOHelper.sendResponse(IOHelper.CONTENT_TYPE_JSON, dataBytes, servletResponse);
            } else {
                IOHelper.sendResponse(IOHelper.CONTENT_TYPE_HTML, dataBytes, servletResponse);
            }
        } catch(Exception ex) {
            LogHelper.e(LOG_TAG, ex);
        }
    }
}
