// HtmlEditObserver - callback interface for HtmlEditScanner
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

import java.net.URL;

/// Callback interface for HtmlEditScanner.
// <P>
// Clients of HtmlEditScanner implement this in order to get URLs passed back
// to them.
// <P>
// <A HREF="/resources/classes/Acme/HtmlEditObserver.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see HtmlEditScanner

public interface HtmlEditObserver {
	
	/// This gets called when the scanner finds an &lt;A HREF=""&gt; URL.
	// If you want to change the URL in the stream you can return a
	// new string; otherwise you should return null.
	public String editAHREF(String urlStr, URL contextUrl, Object clientData);
	
	/// This gets called when the scanner finds an &lt;IMG SRC=""&gt; URL.
	// If you want to change the URL in the stream you can return a
	// new string; otherwise you should return null.
	public String editIMGSRC(String urlStr, URL contextUrl, Object clientData);
	
	/// This gets called when the scanner finds an &lt;FRAME SRC=""&gt; URL.
	// If you want to change the URL in the stream you can return a
	// new string; otherwise you should return null.
	public String editFRAMESRC(String urlStr, URL contextUrl, Object clientData);
	
	/// This gets called when the scanner finds a &lt;BASE HREF=""&gt; URL.
	// If you want to change the URL in the stream you can return a
	// new string; otherwise you should return null.
	public String editBASEHREF(String urlStr, URL contextUrl, Object clientData);
	
	/// This gets called when the scanner finds a &lt;AREA HREF=""&gt; URL.
	// If you want to change the URL in the stream you can return a
	// new string; otherwise you should return null.
	public String editAREAHREF(String urlStr, URL contextUrl, Object clientData);
	
	/// This gets called when the scanner finds a &lt;LINK HREF=""&gt; URL.
	// If you want to change the URL in the stream you can return a
	// new string; otherwise you should return null.
	public String editLINKHREF(String urlStr, URL contextUrl, Object clientData);
	
	/// This gets called when the scanner finds a &lt;BODY BACKGROUND=""&gt;
	/// URL.
	// If you want to change the URL in the stream you can return a
	// new string; otherwise you should return null.
	public String editBODYBACKGROUND(String urlStr, URL contextUrl, Object clientData);
	
}
