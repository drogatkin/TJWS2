// NewsDbArticle - netnews database article
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

import java.util.Hashtable;

import Acme.Utils;

/// Netnews database article.
// <P>
// This represents a news database article.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/NewsDbArticle.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class NewsDbArticle {
	
	private long dbStamp;
	private String text;
	private int headLen, bodyStart;	// these generally differ by only 1
	private Hashtable headers = new Hashtable();
	
	/// Constructor.
	// @exception NewsDbException if something goes wrong
	public NewsDbArticle(long dbStamp, String text) throws NewsDbException {
		this.dbStamp = dbStamp;
		this.text = text;
		
		// Figure out headLen/bodyStart.
		int lflf = text.indexOf("\n\n");
		if(lflf != -1) {
			headLen = lflf + 1;
			bodyStart = lflf + 2;
		} else
			throw new NewsDbException("can't find end of header");
	}
	
	/// Get the database stamp.
	// This is used internally when dealing with multiple news databases,
	// to make sure that an obect returned from one is not passed to another.
	protected long getDbStamp() {
		return dbStamp;
	}
	
	/// Get the article text.
	public String getText() {
		return text;
	}
	
	/// Get the length of the article header.
	public int getHeadLen() {
		return headLen;
	}
	
	/// Get the start position of the article body.
	public int getBodyStart() {
		return bodyStart;
	}
	
	/// Get a header field.
	public String getHeader(String name) {
		String val = (String) headers.get(name);
		if(val == null) {
			val = getHeader(text, name);
			if(val == null)
				return null;
			headers.put(name, val);
		}
		return val;
	}
	
	/// Extract the named header from the text of a netnews article.
	// Does a case-insensitive search for "<BOL>name:"
	// Stops searching at the end of the headers.
	// Returns null if the header was not found.
	public static String getHeader(String text, String name) {
		boolean bol = true;
		int nameLen = name.length();
		int textLen = text.length();
		if(name.charAt(nameLen - 1) != ':') {
			name = name + ":";
			++nameLen;
		}
		char[] nameChars = name.toLowerCase().toCharArray();
		char ch;
		int nameIdx = 0;
		for(int textIdx = 0; textIdx < textLen; ++textIdx) {
			ch = Character.toLowerCase(text.charAt(textIdx));
			if(ch == '\n') {
				if(bol)
					break;
				bol = true;
				nameIdx = 0;
				continue;
			}
			if(nameIdx == 0 && !bol)
				continue;
			bol = false;
			if(ch == nameChars[nameIdx]) {
				++nameIdx;
				if(nameIdx >= nameLen) {
					// Found it.
					++textIdx;
					int startIdx = textIdx + Utils.strSpan(text, " \t", textIdx);
					int endIdx = text.indexOf('\n', startIdx);
					return text.substring(startIdx, endIdx);
				}
			} else
				nameIdx = 0;
		}
		return null;
	}
	
}
