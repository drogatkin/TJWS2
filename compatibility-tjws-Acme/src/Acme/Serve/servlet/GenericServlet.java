// GenericServlet - convenience class for Servlet implementors
//
// API based on documentation from JavaSoft.
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

package Acme.Serve.servlet;

import java.io.IOException;
import java.util.Enumeration;

/// Convenience class for Servlet implementors.
// <P>
// This is an abstract object that implements the Servlet interface.
// Servlet has to be an interface so that things that must inherit
// from another object, such as RMI, can also be Servlets. However,
// most of the time a Servlet will not need to be anything else, and
// it will be somewhat more convenient to extend this class and just
// provide the service() routine.
// <P>
// This is taken from JavaSoft's Servlet API documentation.
// <P>
// <A HREF="/resources/classes/Acme/Serve/servlet/GenericServlet.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.Servlet

public abstract class GenericServlet implements Servlet {
	
	/// Methods a Servlet author might want to override.
	
	/// Returns a string containing information about the author, version, and
	// copyright of the servlet.
	public String getServletInfo() {
		return null;
	}
	
	/// Initializes the servlet.
	// This is called by the system when the servlet is first loaded.
	// @exception ServletException when an exception has occurred
	public void init() throws ServletException {
	}
	
	/// Services a single request from the client.
	// <P>
	// Note that the server only creates a single instance of your Servlet
	// subclass, and calls the service() method of that one instance multiple
	// times, possibly at the same time in different threads. This is somewhat
	// unusual in the Java world. The implication is that any instance
	// variables in your class behave more like class variables - they are
	// shared among multiple concurrent calls. So, be careful.
	// @param req the servlet request
	// @param req the servlet response
	// @exception ServletException when a servlet exception has occurred
	// @exception IOException when an I/O exception has occurred
	public abstract void service(ServletRequest req, ServletResponse res) throws ServletException, IOException;
	
	/// Destroys the servlet and cleans up whatever resources are being held.
	// This is called by the system when the servlet is being destroyed.
	public void destroy() {
	}
	
	/// The rest will not generally be overridden.
	
	private ServletConfig config;
	
	/// Initializes the servlet.
	// This is called by the system when the servlet is first loaded.
	// @param config servlet initialization information
	// @exception ServletException when an exception has occurred
	public void init(ServletConfig config) throws ServletException {
		this.config = config;
		init();
	}
	
	/// Returns a servlet config object, which contains any initialization
	// parameters and startup configuration for this servlet.
	public ServletConfig getServletConfig() {
		return config;
	}
	
	/// Write information to the servlet log.
	// @param message the message to log
	public void log(String message) {
		getServletContext().log(message);
	}
	
	/// Returns the servlet context. The servlet context lets a servlet
	// access the environment of the server in which it is running.
	public ServletContext getServletContext() {
		return config.getServletContext();
	}
	
	/// Gets an initialization parameter of the servlet.
	// @param name the parameter name
	public String getInitParameter(String name) {
		return config.getInitParameter(name);
	}
	
	/// Gets a hash table of the initialization parameters of the servlet.
	// @param name the parameter name
	public Enumeration getInitParameterNames() {
		return config.getInitParameterNames();
	}
	
}
