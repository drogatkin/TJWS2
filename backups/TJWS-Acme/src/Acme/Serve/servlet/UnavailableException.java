// UnavailableException - an exception for Servlets
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

/// An exception for Servlets.
// <P>
// <A
/// HREF="/resources/classes/Acme/Serve/servlet/UnavailableException.java">Fetch
/// the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class UnavailableException extends ServletException {
	
	private boolean permanent;
	private int seconds;
	private Servlet servlet;
	
	/// Constructs an UnavailableException with the specified detail message,
	// indicating the servlet is permanently unavailable.
	public UnavailableException(Servlet servlet, String msg) {
		super(msg);
		permanent = true;
		this.servlet = servlet;
	}
	
	/// Constructs an UnavailableException with the specified detail message,
	// indicating that the servlet is temporarily unavailable and giving an
	// estimate of how long it will be unavailable. In some cases, no
	// estimate can be made; this is indicated by a non-positive time.
	public UnavailableException(int seconds, Servlet servlet, String msg) {
		super(msg);
		permanent = false;
		this.seconds = seconds;
		this.servlet = servlet;
	}
	
	/// Returns true if the servlet is "permanently" unavailable, indicating
	// that the service administrator must take some corrective action to
	// make the servlet be usable.
	public boolean isPermanent() {
		return permanent;
	}
	
	/// Returns the servlet that is reporting its unavailability.
	public Servlet getServlet() {
		return servlet;
	}
	
	/// Returns the amount of time the servlet expects to be temporarily
	// unavailable.
	public int getUnavailableSeconds() {
		return seconds;
	}
	
}
