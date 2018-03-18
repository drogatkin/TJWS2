// Serve - minimal Java servlet container class
//
// Copyright (C)2018 by Rohtash Singh Lakra <rohtash.singh@gmail.com>. All rights reserved.
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

package javax.servlet.http;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletResponse;

/**
 * @author Rohtash Lakra (rohtash.lakra@devamatre.com)
 * @author Rohtash Singh Lakra (rohtash.singh@gmail.com)
 * @created 2018-03-18 11:40:14 AM
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract interface HttpServletResponse extends ServletResponse {
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
	public static final int SC_FOUND = 302;
	public static final int SC_SEE_OTHER = 303;
	public static final int SC_NOT_MODIFIED = 304;
	public static final int SC_USE_PROXY = 305;
	public static final int SC_TEMPORARY_REDIRECT = 307;
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
	public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
	public static final int SC_EXPECTATION_FAILED = 417;
	public static final int SC_INTERNAL_SERVER_ERROR = 500;
	public static final int SC_NOT_IMPLEMENTED = 501;
	public static final int SC_BAD_GATEWAY = 502;
	public static final int SC_SERVICE_UNAVAILABLE = 503;
	public static final int SC_GATEWAY_TIMEOUT = 504;
	public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;
	
	public abstract void addCookie(Cookie paramCookie);
	
	public abstract boolean containsHeader(String paramString);
	
	public abstract String encodeURL(String paramString);
	
	public abstract String encodeRedirectURL(String paramString);
	
	/**
	 * @deprecated
	 */
	public abstract String encodeUrl(String paramString);
	
	/**
	 * @deprecated
	 */
	public abstract String encodeRedirectUrl(String paramString);
	
	public abstract void sendError(int paramInt, String paramString) throws IOException;
	
	public abstract void sendError(int paramInt) throws IOException;
	
	public abstract void sendRedirect(String paramString) throws IOException;
	
	public abstract void setDateHeader(String paramString, long paramLong);
	
	public abstract void addDateHeader(String paramString, long paramLong);
	
	public abstract void setHeader(String paramString1, String paramString2);
	
	public abstract void addHeader(String paramString1, String paramString2);
	
	public abstract void setIntHeader(String paramString, int paramInt);
	
	public abstract void addIntHeader(String paramString, int paramInt);
	
	public abstract void setStatus(int paramInt);
	
	/**
	 * @deprecated
	 */
	public abstract void setStatus(int paramInt, String paramString);
	
	public abstract int getStatus();
	
	public abstract String getHeader(String paramString);
	
	public abstract Enumeration<String> getHeaders(String paramString);
	
	public abstract Enumeration<String> getHeaderNames();
}
