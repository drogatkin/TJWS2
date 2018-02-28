// TextcompOutputStream - use a TextComponent as the sink of an OutputStream
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

import java.awt.TextArea;
import java.awt.TextComponent;
import java.io.IOException;
import java.io.OutputStream;

/// Use a TextComponent as the sink of an OutputStream.
// <P>
// When you write to this OutputStream, the text appears in the
// associated TextComponent.
// <P>
// <A HREF="/resources/classes/Acme/TextcompOutputStream.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see TextcompInputStream

public class TextcompOutputStream extends OutputStream {
	
	// It would be nice if TextComponent had an append() method,
	// but we can check whether it's a TextArea and use that class's
	// append().
	
	TextComponent textComponent = null;
	TextArea textArea = null;
	StringBuffer buf = null;
	
	public TextcompOutputStream(TextComponent textComponent) {
		if(textComponent instanceof TextArea)
			textArea = (TextArea) textComponent;
		else {
			this.textComponent = textComponent;
			buf = new StringBuffer();
		}
		textComponent.setText("");
	}
	
	public void write(int i) throws IOException {
		if(textArea != null)
			textArea.appendText((new Character((char) i)).toString());
		// textArea.append( ( new Character( (char) i ) ).toString() );
		// (There isn't a static toString( char ) method.)
		else {
			buf.append((char) i);
			textComponent.setText(buf.toString());
		}
	}
	
	public void write(byte[] b) throws IOException {
		if(textArea != null)
			textArea.appendText(new String(b));
		// textArea.append( new String( b ) );
		else {
			buf.append(new String(b));
			textComponent.setText(buf.toString());
		}
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		if(textArea != null)
			textArea.appendText(new String(b, off, len));
		// textArea.append( new String( b, off, len ) );
		else {
			buf.append(new String(b, off, len));
			textComponent.setText(buf.toString());
		}
	}
	
	public String toString() {
		return textComponent.getText();
	}
	
}
