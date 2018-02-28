// OkCancelBox - an ok/cancel box
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

package Acme.Widgets;

import java.awt.Frame;

/// An ok/cancel box.
// <P>
// Puts up a dialog with a specified message and two buttons, Ok and Cancel.
// All user input is locked out. The program can retrieve the user's answer
// via the getAnswer() method.
// <P>
// Sample usage:
// <IMG ALIGN=RIGHT WIDTH=179 HEIGHT=79 SRC="OkCancelBox.gif">
// <BLOCKQUOTE><PRE><CODE>
// OkCancelBox oc = new OkCancelBox( this, "Sample OkCancelBox" );
// oc.show();
// switch ( oc.getAnswer() )
// {
// case OkCancelBox.OK:
// [your code here]
// break;
// case OkCancelBox.CANCEL:
// [your code here]
// break;
// }
// </CODE></PRE></BLOCKQUOTE>
// <P>
// <A HREF="/resources/classes/Acme/Widgets/OkCancelBox.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class OkCancelBox extends ButtonDialog {
	
	// Enumerated values.
	
	public static final int OK = WidgetUtils.OK;
	public static final int CANCEL = WidgetUtils.CANCEL;
	
	/// Constructor, default title.
	public OkCancelBox(Frame parent, String message) {
		this(parent, "Ok/Cancel", message);
	}
	
	/// Constructor, specified title.
	public OkCancelBox(Frame parent, String title, String message) {
		super(parent, title, message, "Ok", OK, "Cancel", CANCEL);
	}
	
}
