// ServletRequest - this interface represents a servlet request
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

/// This interface represents a servlet request.
// <P>
// This is taken from JavaSoft's Servlet API documentation.
// <P>
// <A HREF="/resources/classes/Acme/Serve/servlet/ServletRequest.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.Servlet
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;

public interface ServletRequest {
	
	/// Returns the size of the request entity data, or -1 if not known.
	// Same as the CGI variable CONTENT_LENGTH.
	public int getContentLength();
	
	/// Returns the MIME type of the request entity data, or null if
	// not known.
	// Same as the CGI variable CONTENT_TYPE.
	public String getContentType();
	
	/// Returns the protocol and version of the request as a string of
	// the form <protocol>/<major version>.<minor version>.
	// Same as the CGI variable SERVER_PROTOCOL.
	public String getProtocol();
	
	/// Returns the scheme of the URL used in this request, for example
	// "http", "https", or "ftp". Different schemes have different rules
	// for constructing URLs, as noted in RFC 1738. The URL used to create
	// a request may be reconstructed using this scheme, the server name
	// and port, and additional information such as URIs.
	public String getScheme();
	
	/// Returns the host name of the server as used in the <host> part of
	// the request URI.
	// Same as the CGI variable SERVER_NAME.
	public String getServerName();
	
	/// Returns the port number on which this request was received as used in
	// the <port> part of the request URI.
	// Same as the CGI variable SERVER_PORT.
	public int getServerPort();
	
	/// Returns the IP address of the agent that sent the request.
	// Same as the CGI variable REMOTE_ADDR.
	public String getRemoteAddr();
	
	/// Returns the fully qualified host name of the agent that sent the
	// request.
	// Same as the CGI variable REMOTE_HOST.
	public String getRemoteHost();
	
	/// Applies alias rules to the specified virtual path and returns the
	// corresponding real path, or null if the translation can not be
	// performed for any reason. For example, an HTTP servlet would
	// resolve the path using the virtual docroot, if virtual hosting is
	// enabled, and with the default docroot otherwise. Calling this
	// method with the string "/" as an argument returns the document root.
	public String getRealPath(String path);
	
	/// Returns an input stream for reading request data.
	// @exception IllegalStateException if getReader has already been called
	// @exception IOException on other I/O-related errors
	public ServletInputStream getInputStream() throws IOException;
	
	/// Returns a buffered reader for reading request data.
	// @exception UnsupportedEncodingException if the character set encoding
	/// isn't supported
	// @exception IllegalStateException if getInputStream has already been
	/// called
	// @exception IOException on other I/O-related errors
	public BufferedReader getReader();
	
	/// Returns a string containing the value of the specified parameter,
	// or null if the parameter does not exist.
	public String getParameter(String name);
	
	/// Returns the values of the specified parameter for the request as an
	// array of strings, or null if the named parameter does not exist.
	public String[] getParameterValues(String name);
	
	/// Returns the parameter names for this request.
	public Enumeration getParameterNames();
	
	/// Returns the value of the named attribute of the request, or null if
	// the attribute does not exist. This method allows access to request
	// information not already provided by the other methods in this interface.
	public Object getAttribute(String name);
	
}
