// Cookie - an HTTP cookie
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

/// An HTTP cookie.
// <P>
// This is taken from JavaSoft's Servlet API documentation.
// <P>
// <A HREF="/resources/classes/Acme/Serve/servlet/http/Cookie.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class Cookie extends Object implements Cloneable {
	
	private String name;
	private String value;
	private String comment = null;
	private String domain = null;
	private int maxAge = -1;
	private String path = null;
	private boolean secure = false;
	private int version = 0;
	
	/// Defines a cookie with an initial name and value.
	public Cookie(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	/// If a user agent (web browser) presents this cookie to a user, the
	// cookie's purpose will be described using this comment.
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	/// Returns the comment describing the purpose of this cookie, or null if
	// no such comment has been defined.
	public String getComment() {
		return comment;
	}
	
	/// This cookie should be presented only to hosts satisfying this domain
	// name pattern.
	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	/// Returns the domain of the cookie.
	public String getDomain() {
		return domain;
	}
	
	/// Sets the maximum age of the cookie.
	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}
	
	/// Returns the maximum specified age of the cookie.
	public int getMaxAge() {
		return maxAge;
	}
	
	/// This cookie should be presented only with requests beginning with
	// this URL. Read RFC 2109 for a specification of the default behaviour.
	// Basically, URLs in the same "directory" as the one which set the
	// cookie, and in subdirectories, can all see the cookie unless a
	// different path is set.
	public void setPath(String path) {
		this.path = path;
	}
	
	/// Returns the prefix of all URLs for which this cookie is targetted.
	public String getPath() {
		return path;
	}
	
	/// Indicates to the user agent that the cookie should only be sent using
	// a secure protocol (https).
	public void setSecure(boolean secure) {
		this.secure = secure;
	}
	
	/// Returns the value of the 'secure' flag.
	public boolean getSecure() {
		return secure;
	}
	
	/// Returns the name of the cookie.
	public String getName() {
		return name;
	}
	
	/// Sets the value of the cookie. BASE64 encoding is suggested for use
	// with binary values.
	public void setValue(String value) {
		this.value = value;
	}
	
	/// Returns the value of the cookie.
	public String getValue() {
		return value;
	}
	
	/// Sets the version of the cookie protocol used when this cookie saves
	// itself. Since the IETF standards are still being finalized, consider
	// version 1 as experimental; do not use it (yet) on production sites.
	public void setVersion(int vesion) {
		this.version = version;
	}
	
	/// Returns the version of the cookie. Version 1 complies with RFC 2109,
	// version 0 indicates the original version, as specified by Netscape.
	// Newly constructed cookies use version 0 by default, to maximize
	// interoperability. Cookies provided by a user agent will identify
	// the cookie version used by the browser.
	public int getVersion() {
		return version;
	}
	
	/// Copies the cookie.
	public Object clone() {
		try {
			return super.clone();
		} catch(CloneNotSupportedException e) {
			// Shouldn't happen.
			throw new InternalError(e.toString());
		}
	}
	
}
