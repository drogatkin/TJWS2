// APrintStream - more efficient PrintStream
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@mail.acme.com>. All rights
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

package Acme;

import java.io.OutputStream;
import java.io.PrintStream;

/// More efficient PrintStream.
// <P>
// This class reimplements two methods from PrintStream, resulting in
// vastly improved performance.
// <P>
// The two reimplemented methods are:
// <UL>
// <LI> public void print( String s )
// <LI> public void print( char[] c )
// </UL>
// In the standard PrintStream, these are implemented by writing each
// character with a separate call to <CODE>write(int b)</CODE>. All we do
// here is make a single <CODE>write(byte[] b, int off, int len)</CODE>
// call instead, avoiding all that routine-call overhead for each character.
// <P>
// The API is identical to java.io.PrintStream.
// <P>
// <A HREF="/resources/classes/Acme/APrintStream.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class APrintStream extends PrintStream {
	
	public APrintStream(OutputStream out) {
		super(out);
	}
	
	public APrintStream(OutputStream out, boolean autoflush) {
		super(out, autoflush);
	}
	
	public void print(String s) {
		if(s == null)
			s = "null";
		
		byte[] b = s.getBytes();
		write(b, 0, b.length);
	}
	
	public void print(char[] c) {
		print(new String(c));
	}
	
}
