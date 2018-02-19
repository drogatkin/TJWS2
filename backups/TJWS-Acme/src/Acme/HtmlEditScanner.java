// HtmlEditScanner - an HTML scanner with editing
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

package Acme;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

/// An HTML scanner with editing.
// <P>
// This is like HtmlScanner but it lets you make changes to the URLs in
// the HTML stream you are scanning. The regular scanner class lets
// you define callbacks that get called with the URLs; in this version,
// you can return substitute URLs from the callbacks, and they get
// inserted into the stream in place of the old URLs.
// <P>
// <A HREF="/resources/classes/Acme/HtmlEditScanner.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class HtmlEditScanner extends FilterInputStream implements Acme.HtmlObserver {
	
	// The underlying HtmlScanner.
	HtmlScanner scanner;
	
	// The list of HtmlEditObservers to call, paired with clientDatas.
	private Vector observers = new Vector();
	
	/// Constructor.
	// If the client is not interested in getting called back with URLs,
	// observer can be null (but then there's not much point in
	// using this class).
	public HtmlEditScanner(InputStream s, URL thisUrl, Acme.HtmlEditObserver observer) {
		this(s, thisUrl, observer, null);
	}
	
	/// Constructor with clientData.
	// If the client is not interested in getting called back with URLs,
	// observer can be null (but then there's not much point in
	// using this class).
	public HtmlEditScanner(InputStream s, URL thisUrl, Acme.HtmlEditObserver observer, Object clientData) {
		this(new HtmlScanner(s, thisUrl, null), observer, clientData);
	}
	
	/// Constructor with a pre-made HtmlScanner.
	// If the client is not interested in getting called back with URLs,
	// observer can be null (but then there's not much point in
	// using this class).
	public HtmlEditScanner(HtmlScanner scanner, Acme.HtmlEditObserver observer) {
		this(scanner, observer, null);
	}
	
	/// Constructor with a pre-made HtmlScanner, with clientData.
	// If the client is not interested in getting called back with URLs,
	// observer can be null (but then there's not much point in
	// using this class).
	public HtmlEditScanner(HtmlScanner scanner, Acme.HtmlEditObserver observer, Object clientData) {
		super(scanner);
		this.scanner = scanner;
		scanner.addObserver(this);
		if(observer != null)
			addObserver(observer, clientData);
	}
	
	/// Add an extra observer to this editor. Multiple observers get called
	// in the order they were added.
	public void addObserver(Acme.HtmlEditObserver observer) {
		addObserver(observer, null);
	}
	
	/// Add an extra observer to this editor. Multiple observers get called
	// in the order they were added.
	public void addObserver(Acme.HtmlEditObserver observer, Object clientData) {
		observers.addElement(new Acme.Pair(observer, clientData));
	}
	
	private boolean gotEOF = false;
	
	private byte[] buf = new byte[4096];
	private int bufSize = buf.length;
	private int bufOff = 0;
	private int bufLen = 0;
	
	/// Special version of read() that's careful about URLs split across
	// buffer-loads.
	public int read(byte[] b, int off, int len) throws IOException {
		int i = len;
		while((bufLen < len || scanner.gettingUrl) && !gotEOF) {
			if(bufLen >= len)
				i += 50;	// read a little extra to complete the URL
			checkBuf(i + 500);	// add a little room for expanded URLs
			int r = in.read(buf, bufOff + bufLen, i - bufLen);
			if(r == -1)
				gotEOF = true;
			else
				bufLen += r;
		}
		if(bufLen == 0)
			return -1;
		i = Math.min(len, bufLen);
		System.arraycopy(buf, bufOff, b, off, i);
		bufOff += i;
		bufLen -= i;
		return i;
	}
	
	/// Override to make sure this goes through the above
	// read( byte[], int, int) method.
	public int read() throws IOException {
		byte[] b = new byte[1];
		int r = read(b, 0, 1);
		if(r == -1)
			return -1;
		else
			return b[0];
	}
	
	// Check if we need to expand the buffer.
	private void checkBuf(int need) {
		// First, if there's no data in the buffer we can zero the offset.
		if(bufLen == 0)
			bufOff = 0;
		// Is there room?
		if(bufOff + need > bufSize) {
			// No. Should we just compactify this buffer, or make a new one?
			if(need * 2 < bufSize) {
				// Compactify.
				System.arraycopy(buf, bufOff, buf, 0, bufLen);
			} else {
				// New buffer.
				byte[] newBuf = new byte[need * 2];
				System.arraycopy(buf, bufOff, newBuf, 0, bufLen);
				buf = newBuf;
				bufSize = buf.length;
			}
			bufOff = 0;
		}
	}
	
	/// Callback from HtmlScanner.
	public void gotAHREF(String urlStr, URL contextUrl, Object junk) {
		Enumeration en = observers.elements();
		while(en.hasMoreElements()) {
			Acme.Pair pair = (Acme.Pair) en.nextElement();
			Acme.HtmlEditObserver observer = (HtmlEditObserver) pair.left();
			Object clientData = pair.right();
			String changedUrlStr = observer.editAHREF(urlStr, contextUrl, clientData);
			if(changedUrlStr != null)
				scanner.substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	/// Callback from HtmlScanner.
	public void gotIMGSRC(String urlStr, URL contextUrl, Object junk) {
		Enumeration en = observers.elements();
		while(en.hasMoreElements()) {
			Acme.Pair pair = (Acme.Pair) en.nextElement();
			Acme.HtmlEditObserver observer = (HtmlEditObserver) pair.left();
			Object clientData = pair.right();
			String changedUrlStr = observer.editIMGSRC(urlStr, contextUrl, clientData);
			if(changedUrlStr != null)
				scanner.substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	/// Callback from HtmlScanner.
	public void gotFRAMESRC(String urlStr, URL contextUrl, Object junk) {
		Enumeration en = observers.elements();
		while(en.hasMoreElements()) {
			Acme.Pair pair = (Acme.Pair) en.nextElement();
			Acme.HtmlEditObserver observer = (HtmlEditObserver) pair.left();
			Object clientData = pair.right();
			String changedUrlStr = observer.editFRAMESRC(urlStr, contextUrl, clientData);
			if(changedUrlStr != null)
				scanner.substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	/// Callback from HtmlScanner.
	public void gotBASEHREF(String urlStr, URL contextUrl, Object junk) {
		Enumeration en = observers.elements();
		while(en.hasMoreElements()) {
			Acme.Pair pair = (Acme.Pair) en.nextElement();
			Acme.HtmlEditObserver observer = (HtmlEditObserver) pair.left();
			Object clientData = pair.right();
			String changedUrlStr = observer.editBASEHREF(urlStr, contextUrl, clientData);
			if(changedUrlStr != null)
				scanner.substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	/// Callback from HtmlScanner.
	public void gotAREAHREF(String urlStr, URL contextUrl, Object junk) {
		Enumeration en = observers.elements();
		while(en.hasMoreElements()) {
			Acme.Pair pair = (Acme.Pair) en.nextElement();
			Acme.HtmlEditObserver observer = (HtmlEditObserver) pair.left();
			Object clientData = pair.right();
			String changedUrlStr = observer.editAREAHREF(urlStr, contextUrl, clientData);
			if(changedUrlStr != null)
				scanner.substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	/// Callback from HtmlScanner.
	public void gotLINKHREF(String urlStr, URL contextUrl, Object junk) {
		Enumeration en = observers.elements();
		while(en.hasMoreElements()) {
			Acme.Pair pair = (Acme.Pair) en.nextElement();
			Acme.HtmlEditObserver observer = (HtmlEditObserver) pair.left();
			Object clientData = pair.right();
			String changedUrlStr = observer.editLINKHREF(urlStr, contextUrl, clientData);
			if(changedUrlStr != null)
				scanner.substitute(urlStr.length(), changedUrlStr);
		}
	}
	
	/// Callback from HtmlScanner.
	public void gotBODYBACKGROUND(String urlStr, URL contextUrl, Object junk) {
		Enumeration en = observers.elements();
		while(en.hasMoreElements()) {
			Acme.Pair pair = (Acme.Pair) en.nextElement();
			Acme.HtmlEditObserver observer = (HtmlEditObserver) pair.left();
			Object clientData = pair.right();
			String changedUrlStr = observer.editBODYBACKGROUND(urlStr, contextUrl, clientData);
			if(changedUrlStr != null)
				scanner.substitute(urlStr.length(), changedUrlStr);
		}
	}
	
}
