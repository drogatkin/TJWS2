/* tjws - SessionCookieConfigImpl.java
 * Copyright (C) 2004-2010 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  $Id: SessionCookieConfigImpl.java,v 1.2 2010/05/21 03:07:29 dmitriy Exp $
 * Created on May 11, 2010
 */
package rogatkin.web;

import javax.servlet.SessionCookieConfig;

public class SessionCookieConfigImpl implements SessionCookieConfig {
	private String comment;

	private String domain;

	private int maxAge;

	private String name;

	private String path;

	private boolean http;

	private boolean secure;

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public String getDomain() {
		return domain;
	}

	@Override
	public int getMaxAge() {
		return maxAge;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public boolean isHttpOnly() {
		return http;
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public void setDomain(String domain) {
		this.domain = domain;
	}

	@Override
	public void setHttpOnly(boolean set) {
		this.http = set;
	}

	@Override
	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

}
