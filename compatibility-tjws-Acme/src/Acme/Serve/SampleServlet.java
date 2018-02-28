// SampleServlet - trivial servlet
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

import Acme.Serve.servlet.ServletException;
import Acme.Serve.servlet.ServletOutputStream;
import Acme.Serve.servlet.http.HttpServlet;
import Acme.Serve.servlet.http.HttpServletRequest;
import Acme.Serve.servlet.http.HttpServletResponse;

/// Trivial servlet.
// <P>
// <A HREF="/resources/classes/Acme/Serve/SampleServlet.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class SampleServlet extends HttpServlet {
	
	/// Returns a string containing information about the author, version, and
	// copyright of the servlet.
	public String getServletInfo() {
		return "trivial servlet";
	}
	
	/// Services a single request from the client.
	// @param req the servlet request
	// @param req the servlet response
	// @exception ServletException when an exception has occurred
	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		log("called");
		res.setStatus(HttpServletResponse.SC_OK);
		res.setContentType("text/html");
		ServletOutputStream p = res.getOutputStream();
		p.println("<HTML><HEAD>");
		p.println("<TITLE>Sample Servlet Output</TITLE>");
		p.println("</HEAD><BODY>");
		p.println("<H2>Sample Servlet Output</H2>");
		p.println("<P>Output from a sample servlet.");
		p.println("</BODY></HTML>");
		p.flush();
		p.close();
	}
	
}
