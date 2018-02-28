// ServletContext - an interface for defining a servlet's environment
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

/// An interface for defining a servlet's environment.
// It can be used by the servlet to obtain information about the
// environment (i.e. web server) in which it is running.
// <P>
// This is taken from JavaSoft's Servlet API documentation.
// <P>
// <A HREF="/resources/classes/Acme/Serve/servlet/ServletContext.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.Servlet
import java.util.Enumeration;

public interface ServletContext {
	
	/// Gets a servlet by name.
	// @param name the servlet name
	// @return null if the servlet does not exist
	// @exception ServletException if the servlet could not be initialized
	public Servlet getServlet(String name) throws ServletException;
	
	/// Enumerates the servlets in this context (server). Only servlets that
	// are accesible will be returned. This enumeration always includes the
	// servlet itself.
	public Enumeration getServlets();
	
	/// Enumerates the names of the servlets in this context (server).
	// Only servlets that are accesible will be returned. This enumeration
	// always includes the servlet itself.
	public Enumeration getServletNames();
	
	/// Write information to the servlet log.
	// @param message the message to log
	public void log(String message);
	
	/// Write a stack trace to the servlet log.
	// @param exception the exception to get the stack trace from
	// @param message the message to log
	public void log(Exception exception, String message);
	
	/// Applies alias rules to the specified virtual path and returns the
	// corresponding real path. It returns null if the translation
	// cannot be performed.
	// @param path the path to be translated
	public String getRealPath(String path);
	
	/// Returns the MIME type of the specified file.
	// @param file file name whose MIME type is required
	public String getMimeType(String file);
	
	/// Returns the name and version of the web server under which the servlet
	// is running.
	// Same as the CGI variable SERVER_SOFTWARE.
	public String getServerInfo();
	
	/// Returns the value of the named attribute of the network service, or
	// null if the attribute does not exist. This method allows access to
	// additional information about the service, not already provided by
	// the other methods in this interface.
	public Object getAttribute(String name);
	
}
