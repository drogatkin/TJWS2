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
package com.rslakra.android.server;

import android.content.Context;

import com.rslakra.android.atjwsapp.TJWSApp;
import com.rslakra.android.logger.LogHelper;

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
                dataBytes = IOHelper.readIconBytes(LogHelper.readAssets(mContext, "icon.png"));
                IOHelper.sendResponse(IOHelper.CONTENT_TYPE_ICON, dataBytes, servletResponse);
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
