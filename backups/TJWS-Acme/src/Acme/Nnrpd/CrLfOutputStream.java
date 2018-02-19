// CrLfOutputStream - RFC977-style output stream
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

package Acme.Nnrpd;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/// RFC977-style output stream.
// <P>
// NNTP requires that all lines be terminated by CR LF. This stream
// takes standard Java LF-terminated lines and inserts the CRs.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/CrLfOutputStream.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class CrLfOutputStream extends FilterOutputStream {
	
	static final byte cr = 13;
	static final byte lf = 10;
	static final byte[] crlf = { cr, lf };
	
	/// Constructor.
	public CrLfOutputStream(OutputStream out) {
		super(out);
	}
	
	/// Writes a byte. This method will block until the byte is actually
	// written.
	// @param b the byte to be written
	// @exception IOException if an I/O error has occurred
	public void write(int b) throws IOException {
		if(b == lf)
			out.write(crlf, 0, crlf.length);
		else
			out.write(b);
	}
	
	/// Writes a subarray of bytes.
	// @param b the data to be written
	// @param off the start offset in the data
	// @param len the number of bytes that are written
	// @exception IOException if an I/O error has occurred
	public void write(byte b[], int off, int len) throws IOException {
		int endOff = off + len;
		int writtenOff = off;
		for(int i = off; i < len; ++i) {
			if(b[i] == lf) {
				if(writtenOff < i)
					out.write(b, writtenOff, i - writtenOff);
				out.write(crlf, 0, crlf.length);
				writtenOff = i + 1;
			}
		}
		if(writtenOff < endOff)
			out.write(b, writtenOff, endOff - writtenOff);
	}
	
}
