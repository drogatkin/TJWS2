// BusyBox - a busy-box
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

package Acme.Widgets;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;

/// A busy-box.
// <P>
// Puts up a dialog with a specified message. All user input is locked
// out. When the program is done with its task, it can unlock things by
// calling the done() method.
// <P>
// Sample usage:
// <IMG ALIGN=RIGHT WIDTH=151 HEIGHT=32 SRC="BusyBox.gif">
// <BLOCKQUOTE><PRE><CODE>
// BusyBox b = new BusyBox( this, "Sample BusyBox" );
// b.show();
// [your task here]
// b.done();
// </CODE></PRE></BLOCKQUOTE>
// <P>
// NOTE: do not use this inside an event handler call! With some browsers
// this will cause a deadlock. Instead, have your event handler start a new
// thread to run the busy box and task.
// <P>
// <A HREF="/resources/classes/Acme/Widgets/BusyBox.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class BusyBox extends Dialog implements Runnable {
	
	/// Constructor, default title.
	public BusyBox(Frame parent, String message) {
		this(parent, "Busy", message);
	}
	
	/// Constructor, specified title.
	public BusyBox(Frame parent, String title, String message) {
		super(parent, title, true);
		
		setResizable(false);
		setBackground(Acme.GuiUtils.busyColor);
		
		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		
		ImageLabel imageLabel = new ImageLabel(Acme.GuiUtils.busyIcon(this));
		gb.setConstraints(imageLabel, gbc);
		add(imageLabel);
		Label label = new Label(message);
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gb.setConstraints(label, gbc);
		add(label);
	}
	
	/// Show the box.
	public void show() {
		pack();
		validate();
		// We have to do the super.show() in another thread, cause for modal
		// dialogs it doesn't return until the dialog goes away.
		(new Thread(this)).start();
	}
	
	private boolean isDone = false;
	
	public void run() {
		if(!isDone)
			super.show();
	}
	
	/// Task is done, get rid of the busy-box.
	public void done() {
		isDone = true;
		hide();
		// setVisible( false );
		dispose();
	}
	
}
