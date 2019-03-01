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
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/
//

// All enhancements Copyright (C)2018 by Rohtash Singh Lakra
// This version is compatible with JSDK 2.5
// http://tjws.sourceforge.net
package tjws.embedded;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rslakra.logger.LogManager;

import Acme.IOHelper;

/**
 * The <code>EmbeddedServlet</code> handles all local requests.
 * 
 * @author Rohtash Singh Lakra
 * @date 03/15/2018 03:39:08 PM
 */
public class EmbeddedServlet extends HttpServlet {
	
	/** serialVersionUID */
	private static final long serialVersionUID = 1L;
	
	public EmbeddedServlet() {
	}
	
	/**
	 * @param servletConfig
	 * @throws ServletException
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
	}
	
	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      *
	 *      javax.servlet.http.HttpServletResponse)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}
	
	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
	 *      *
	 *      javax.servlet.http.HttpServletResponse)
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
		try {
			String pathSegment = servletRequest.getRequestURI();
			if (pathSegment.endsWith("/") || pathSegment.endsWith("html")) {
				byte[] dataBytes = IOHelper.readBytes(EmbeddedServlet.class.getResourceAsStream("web/index.html"), true);
				LogManager.info("dataBytes:\n" + IOHelper.toUTF8String(dataBytes) + "\n");
				IOHelper.sendResponse(IOHelper.CONTENT_TYPE_HTML, dataBytes, servletResponse);
			} else if (pathSegment.endsWith("favicon.ico")) {
				IOHelper.sendResponse(IOHelper.CONTENT_TYPE_ICON, IOHelper.readIconBytes(), servletResponse);
			} else if (pathSegment.endsWith(".js")) {
				if (pathSegment.startsWith("/")) {
					pathSegment = pathSegment.substring(1);
				}
				byte[] dataBytes = IOHelper.readBytes(EmbeddedServlet.class.getResourceAsStream(pathSegment), true);
				IOHelper.sendResponse(IOHelper.CONTENT_TYPE_JSON, dataBytes, servletResponse);
			} else {
				IOHelper.sendResponse(IOHelper.CONTENT_TYPE_HTML, "Invalid Request".getBytes(), servletResponse);
			}
		} catch (Exception ex) {
			LogManager.error(ex);
		}
	}
}
