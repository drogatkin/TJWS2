// ServeUtils - static utilities for minimal Java HTTP server
//
// Copyright (C) 1996 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.
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
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import Acme.Serve.servlet.http.HttpServletRequest;
import Acme.Serve.servlet.http.HttpServletResponse;

/// Static utilities for minimal Java HTTP server.
// <P>
// <A HREF="/resources/classes/Acme/Serve/ServeUtils.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class ServeUtils {
	
	// Server identification.
	public static final String serverName = "Acme.Serve";
	public static final String serverVersion = "v1.7 of 13nov96";
	public static final String serverUrl = "http://www.acme.com/java/software/Acme.Serve.Serve.html";
	
	/// Write a standard-format HTML address for this server.
	public static void writeAddress(OutputStream o) throws IOException {
		PrintStream p = new PrintStream(o);
		p.println("<ADDRESS><A HREF=\"" + serverUrl + "\">" + serverName + " " + serverVersion + "</A></ADDRESS>");
	}
	
	/// Get a cookie of a given name.
	public static String getCookie(HttpServletRequest req, String name) {
		String h = req.getHeader("Cookie");
		if(h == null)
			return null;
		StringTokenizer st = new StringTokenizer(h, "; ");
		while(st.hasMoreTokens()) {
			String tk = st.nextToken();
			int eq = tk.indexOf('=');
			String n, v;
			if(eq == -1) {
				n = tk;
				v = "";
			} else {
				n = tk.substring(0, eq);
				v = tk.substring(eq + 1);
			}
			if(name.equals(n))
				return v;
		}
		return null;
	}
	
	/// Set a cookie.
	public static void setCookie(HttpServletResponse res, String name, String value) {
		res.setHeader("Set-Cookie", name + "=" + value);
	}
	
}
