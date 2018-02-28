// HttpServletResponse - this interface represents an HTTP response
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

import java.io.IOException;

import Acme.Serve.servlet.ServletResponse;

/// This interface represents an HTTP response.
// <P>
// This is taken from JavaSoft's Servlet API documentation.
// <P>
// <A
/// HREF="/resources/classes/Acme/Serve/servlet/http/HttpServletResponse.java">Fetch
/// the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.Servlet

public interface HttpServletResponse extends ServletResponse {
	
	/// Adds the specified cookie to the response. It can be called
	// multiple times to set more than one cookie.
	public void addCookie(Cookie cookie);
	
	/// Checks whether the response message header has a field with the
	// specified name.
	public boolean containsHeader(String name);
	
	/// Sets the status code and message for this response.
	// @param code the status code
	// @param msg the status message
	public void setStatus(int code, String msg);
	
	/// Sets the status code and a default message for this response.
	// @param code the status code
	public void setStatus(int code);
	
	/// Sets the value of a header field.
	// @param name the header field name
	// @param value the header field value
	public void setHeader(String name, String value);
	
	/// Sets the value of an integer header field.
	// @param name the header field name
	// @param value the header field integer value
	public void setIntHeader(String name, int value);
	
	/// Sets the value of a long header field.
	// @param name the header field name
	// @param value the header field long value
	public void setLongHeader(String name, long value);
	
	/// Sets the value of a date header field.
	// @param name the header field name
	// @param value the header field date value
	public void setDateHeader(String name, long date);
	
	/// Writes an error response using the specified status code and message.
	// @param code the status code
	// @param msg the status message
	// @exception IOException if an I/O error has occurred
	public void sendError(int code, String msg) throws IOException;
	
	/// Writes an error response using the specified status code and a default
	// message.
	// @param code the status code
	// @exception IOException if an I/O error has occurred
	public void sendError(int code) throws IOException;
	
	/// Sends a redirect message to the client using the specified redirect
	// location URL.
	// @param location the redirect location URL
	// @exception IOException if an I/O error has occurred
	public void sendRedirect(String location) throws IOException;
	
	// URL session-encoding stuff. Not implemented, but the API is here
	// for compatibility.
	
	/// Encodes the specified URL by including the session ID in it, or, if
	// encoding is not needed, returns the URL unchanged. The
	// implementation of this method should include the logic to determine
	// whether the session ID needs to be encoded in the URL. For example,
	// if the browser supports cookies, or session tracking is turned off,
	// URL encoding is unnecessary.
	// <P>
	// All URLs emitted by a Servlet should be run through this method.
	// Otherwise, URL rewriting cannot be used with browsers which do not
	// support cookies.
	public String encodeUrl(String url);
	
	/// Encodes the specified URL for use in the sendRedirect method or, if
	// encoding is not needed, returns the URL unchanged. The
	// implementation of this method should include the logic to determine
	// whether the session ID needs to be encoded in the URL. Because the
	// rules for making this determination differ from those used to
	// decide whether to encode a normal link, this method is seperate
	// from the encodeUrl method.
	// <P>
	// All URLs sent to the HttpServletResponse.sendRedirect method should be
	// run through this method. Otherwise, URL rewriting cannot be used with
	// browsers which do not support cookies.
	public String encodeRedirectUrl(String url);
	
	// Status codes.
	public static final int SC_CONTINUE = 100;
	public static final int SC_SWITCHING_PROTOCOLS = 101;
	public static final int SC_OK = 200;
	public static final int SC_CREATED = 201;
	public static final int SC_ACCEPTED = 202;
	public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;
	public static final int SC_NO_CONTENT = 204;
	public static final int SC_RESET_CONTENT = 205;
	public static final int SC_PARTIAL_CONTENT = 206;
	public static final int SC_MULTIPLE_CHOICES = 300;
	public static final int SC_MOVED_PERMANENTLY = 301;
	public static final int SC_MOVED_TEMPORARILY = 302;
	public static final int SC_SEE_OTHER = 303;
	public static final int SC_NOT_MODIFIED = 304;
	public static final int SC_USE_PROXY = 305;
	public static final int SC_BAD_REQUEST = 400;
	public static final int SC_UNAUTHORIZED = 401;
	public static final int SC_PAYMENT_REQUIRED = 402;
	public static final int SC_FORBIDDEN = 403;
	public static final int SC_NOT_FOUND = 404;
	public static final int SC_METHOD_NOT_ALLOWED = 405;
	public static final int SC_NOT_ACCEPTABLE = 406;
	public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
	public static final int SC_REQUEST_TIMEOUT = 408;
	public static final int SC_CONFLICT = 409;
	public static final int SC_GONE = 410;
	public static final int SC_LENGTH_REQUIRED = 411;
	public static final int SC_PRECONDITION_FAILED = 412;
	public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;
	public static final int SC_REQUEST_URI_TOO_LONG = 414;
	public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
	public static final int SC_INTERNAL_SERVER_ERROR = 500;
	public static final int SC_NOT_IMPLEMENTED = 501;
	public static final int SC_BAD_GATEWAY = 502;
	public static final int SC_SERVICE_UNAVAILABLE = 503;
	public static final int SC_GATEWAY_TIMEOUT = 504;
	public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;
	
}
