// TestServlet - simple servlet that tests the Servlet API
//
// Copyright (C) 1996,1998 by Jef Poskanzer <jef@mail.acme.com>. All rights
// reserved.
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

package Acme.Serve;

import java.io.IOException;
import java.util.Enumeration;

import Acme.Serve.servlet.ServletException;
import Acme.Serve.servlet.ServletOutputStream;
import Acme.Serve.servlet.http.HttpServlet;
import Acme.Serve.servlet.http.HttpServletRequest;
import Acme.Serve.servlet.http.HttpServletResponse;

/// Simple servlet that tests the Servlet API.
// Sample output:
// <PRE>
// getContentLength(): -1
// getContentType(): null
// getProtocol(): HTTP/1.0
// getScheme(): http
// getServerName(): www.acme.com
// getServerPort(): 1234
// getRemoteAddr(): 192.100.66.1
// getRemoteHost(): acme.com
// getMethod(): GET
// getRequestURI(): http://www.acme.com:1234/TestServlet?foo=bar
// getServletPath(): /TestServlet
// getPathInfo(): null
// getPathTranslated(): null
// getQueryString(): foo=bar
// getRemoteUser(): null
// getAuthType(): null
//
// Parameters:
// foo = bar
//
// Header:
// accept: text/html, image/gif, image/jpeg, *; q=.2
// user-agent: Java1.0.2
// </PRE>
// <A HREF="/resources/classes/Acme/Serve/TestServlet.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class TestServlet extends HttpServlet {
	
	/// Returns a string containing information about the author, version, and
	// copyright of the servlet.
	public String getServletInfo() {
		return "simple servlet that tests the Servlet API";
	}
	
	/// Services a single request from the client.
	// @param req the servlet request
	// @param req the servlet response
	// @exception ServletException when an exception has occurred
	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		Enumeration en;
		log("called");
		res.setStatus(HttpServletResponse.SC_OK);
		res.setContentType("text/html");
		ServletOutputStream p = res.getOutputStream();
		p.println("<HTML><HEAD>");
		p.println("<TITLE>Test Servlet Output</TITLE>");
		p.println("</HEAD><BODY>");
		p.println("<H2>Test Servlet Output</H2>");
		p.println("<HR>");
		p.println("<PRE>");
		p.println("getContentLength(): " + req.getContentLength());
		p.println("getContentType(): " + req.getContentType());
		p.println("getProtocol(): " + req.getProtocol());
		p.println("getScheme(): " + req.getScheme());
		p.println("getServerName(): " + req.getServerName());
		p.println("getServerPort(): " + req.getServerPort());
		p.println("getRemoteAddr(): " + req.getRemoteAddr());
		p.println("getRemoteHost(): " + req.getRemoteHost());
		p.println("getMethod(): " + req.getMethod());
		p.println("getRequestURI(): " + req.getRequestURI());
		p.println("getServletPath(): " + req.getServletPath());
		p.println("getPathInfo(): " + req.getPathInfo());
		p.println("getPathTranslated(): " + req.getPathTranslated());
		p.println("getQueryString(): " + req.getQueryString());
		p.println("getRemoteUser(): " + req.getRemoteUser());
		p.println("getAuthType(): " + req.getAuthType());
		p.println("");
		p.println("Parameters:");
		en = req.getParameterNames();
		while(en.hasMoreElements()) {
			String name = (String) en.nextElement();
			p.println("    " + name + " = " + req.getParameter(name));
		}
		p.println("");
		p.println("Headers:");
		en = req.getHeaderNames();
		while(en.hasMoreElements()) {
			String name = (String) en.nextElement();
			p.println("    " + name + ": " + req.getHeader(name));
		}
		p.println("</PRE>");
		p.println("<HR>");
		p.println("</BODY></HTML>");
		p.flush();
		p.close();
	}
	
}
