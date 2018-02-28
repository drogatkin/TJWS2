// HttpServletRequest - this interface represents an HTTP request
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

package Acme.Serve.servlet.http;

import java.util.Enumeration;

import Acme.Serve.servlet.ServletRequest;

/// This interface represents an HTTP request.
// <P>
// This is taken from JavaSoft's Servlet API documentation.
// <P>
// <A
/// HREF="/resources/classes/Acme/Serve/servlet/http/HttpServletRequest.java">Fetch
/// the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.Servlet

public interface HttpServletRequest extends ServletRequest {
	
	/// Gets the array of cookies found in this request.
	public Cookie[] getCookies();
	
	/// Returns the method with which the request was made. This can be "GET",
	// "HEAD", "POST", or an extension method.
	// Same as the CGI variable REQUEST_METHOD.
	public String getMethod();
	
	/// Returns the full request URI.
	public String getRequestURI();
	
	/// Returns the part of the request URI that referred to the servlet being
	// invoked.
	// Analogous to the CGI variable SCRIPT_NAME.
	public String getServletPath();
	
	/// Returns optional extra path information following the servlet path, but
	// immediately preceding the query string. Returns null if not specified.
	// Same as the CGI variable PATH_INFO.
	public String getPathInfo();
	
	/// Returns extra path information translated to a real path. Returns
	// null if no extra path information was specified.
	// Same as the CGI variable PATH_TRANSLATED.
	public String getPathTranslated();
	
	/// Returns the query string part of the servlet URI, or null if none.
	// Same as the CGI variable QUERY_STRING.
	public String getQueryString();
	
	/// Returns the name of the user making this request, or null if not known.
	// Same as the CGI variable REMOTE_USER.
	public String getRemoteUser();
	
	/// Returns the authentication scheme of the request, or null if none.
	// Same as the CGI variable AUTH_TYPE.
	public String getAuthType();
	
	/// Returns the value of a header field, or null if not known.
	// Same as the information passed in the CGI variabled HTTP_*.
	// @param name the header field name
	public String getHeader(String name);
	
	/// Returns the value of an integer header field.
	// @param name the header field name
	// @param def the integer value to return if header not found or invalid
	public int getIntHeader(String name, int def);
	
	/// Returns the value of a long header field.
	// @param name the header field name
	// @param def the long value to return if header not found or invalid
	public long getLongHeader(String name, long def);
	
	/// Returns the value of a date header field.
	// @param name the header field name
	// @param def the date value to return if header not found or invalid
	public long getDateHeader(String name, long def);
	
	/// Returns an Enumeration of the header names.
	public Enumeration getHeaderNames();
	
	// Session stuff. Not implemented, but the API is here for compatibility.
	
	/// Gets the current valid session associated with this request, if
	// create is false or, if necessary, creates a new session for the
	// request, if create is true.
	// <P>
	// Note: to ensure the session is properly maintained, the servlet
	// developer must call this method (at least once) before any output
	// is written to the response.
	// <P>
	// Additionally, application-writers need to be aware that newly
	// created sessions (that is, sessions for which HttpSession.isNew
	// returns true) do not have any application-specific state.
	public HttpSession getSession(boolean create);
	
	/// Gets the session id specified with this request. This may differ
	// from the actual session id. For example, if the request specified
	// an id for an invalid session, then this will get a new session with
	// a new id.
	public String getRequestedSessionId();
	
	/// Checks whether this request is associated with a session that is
	// valid in the current session context. If it is not valid, the
	// requested session will never be returned from the getSession
	// method.
	public boolean isRequestedSessionIdValid();
	
	/// Checks whether the session id specified by this request came in as
	// a cookie. (The requested session may not be one returned by the
	// getSession method.)
	public boolean isRequestedSessionIdFromCookie();
	
	/// Checks whether the session id specified by this request came in as
	// part of the URL. (The requested session may not be the one returned
	// by the getSession method.)
	public boolean isRequestedSessionIdFromUrl();
	
}
