// ServletResponse - this interface represents a servlet response
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

/// This interface represents a servlet response.
// <P>
// This is taken from JavaSoft's Servlet API documentation.
// <P>
// <A HREF="/resources/classes/Acme/Serve/servlet/ServletResponse.java">Fetch
/// the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.Servlet
import java.io.IOException;
import java.io.PrintWriter;

public interface ServletResponse {
	
	/// Sets the content length for this response.
	// @param len the content length
	public void setContentLength(int len);
	
	/// Sets the content type for this response. This type may later be
	// implicitly modified by addition of properties such as the MIME
	// charset=<value> if the service finds it necessary, and the appropriate
	// media type property has not been set. This response property may
	// only be assigned one time.
	// @param type the content type
	public void setContentType(String type);
	
	/// Returns an output stream for writing response data.
	// @exception IllegalStateException if getWriter has been called
	// @exception IOException on other I/O errors
	public ServletOutputStream getOutputStream() throws IOException;
	
	/// Returns a print writer for writing response data. The MIME type of
	// the response will be modified, if necessary, to reflect the character
	// encoding used, through the charset=... property. This means that the
	// content type must be set before calling this method.
	// @exception UnsupportedEncodingException if no such encoding can be
	/// provided
	// @exception IllegalStateException if getOutputStream has been called
	// @exception IOException on other I/O errors
	public PrintWriter getWriter() throws IOException;
	
	/// Returns the character set encoding used for this MIME body. The
	// character encoding is either the one specified in the assigned
	// content type, or one which the client understands. If no content
	// type has yet been assigned, it is implicitly set to text/plain.
	public String getCharacterEncoding();
	
}
