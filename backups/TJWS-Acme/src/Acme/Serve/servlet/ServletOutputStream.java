// ServletOutputStream - special OutputStream used by servlets
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

import java.io.IOException;
import java.io.OutputStream;

/// Special OutputStream used by servlets.
// <P>
// This adds PrintStream methods. It's also an extra layer allowing
// implementations to add hooks for things like automatically writing
// headers when output starts.
// <P>
// This is taken from JavaSoft's Servlet API documentation.
// <P>
// <A
/// HREF="/resources/classes/Acme/Serve/servlet/ServletOutputStream.java">Fetch
/// the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public abstract class ServletOutputStream extends OutputStream {
	
	/// Prints an ASCII string.
	// @param s the string to be printed
	// @exception IOException if an I/O error has occurred
	public abstract void print(String s) throws IOException;
	
	/// Prints an integer.
	// @param i the integer to be printed
	// @exception IOException if an I/O error has occurred
	public abstract void print(int i) throws IOException;
	
	/// Prints a long integer.
	// @param l the long integer to be printed
	// @exception IOException if an I/O error has occurred
	public abstract void print(long l) throws IOException;
	
	/// Prints an ASCII string followed by a CRLF.
	// @param s the string to be printed
	// @exception IOException if an I/O error has occurred
	public abstract void println(String s) throws IOException;
	
	/// Prints an integer followed by a CRLF.
	// @param i the integer to be printed.
	// @exception IOException if an I/O error has occurred
	public abstract void println(int i) throws IOException;
	
	/// Prints a long integer followed by a CRLF.
	// @param i the long integer to be printed.
	// @exception IOException if an I/O error has occurred
	public abstract void println(long l) throws IOException;
	
	/// Prints a CRLF.
	// @exception IOException if an I/O error has occurred
	public abstract void println() throws IOException;
	
}
