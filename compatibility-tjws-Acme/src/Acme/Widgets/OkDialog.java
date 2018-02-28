// OkDialog - a dialog with an icon and an Ok button
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

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;

/// A dialog with an icon and an Ok button.
// <P>
// Puts up a dialog with a client-specified icon and an Ok button.
// All user input is locked out until the button is clicked.
// <P>
// <A HREF="/resources/classes/Acme/Widgets/OkDialog.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class OkDialog extends Dialog {
	
	/// Constructor, no title.
	public OkDialog(Frame parent, Image icon, String message) {
		super(parent, true);
		build(icon, message);
	}
	
	/// Constructor, specified title.
	public OkDialog(Frame parent, String title, Image icon, String message) {
		super(parent, title, true);
		build(icon, message);
	}
	
	/// Common build routine.
	private void build(Image icon, String message) {
		setResizable(false);
		
		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		
		ImageLabel imageLabel = new ImageLabel(icon);
		gb.setConstraints(imageLabel, gbc);
		add(imageLabel);
		Label label = new Label(message);
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gb.setConstraints(label, gbc);
		add(label);
		Button button = new Button("Ok");
		gb.setConstraints(button, gbc);
		add(button);
	}
	
	// Show the box.
	public void show() {
		pack();
		validate();
		super.show();
	}
	
	/// Event handler.
	public boolean handleEvent(Event evt) {
		switch(evt.id) {
			case Event.ACTION_EVENT:
				if(evt.arg.equals("Ok")) {
					hide();
					// setVisible( false );
					dispose();
					return true;
				}
				break;
		}
		return super.handleEvent(evt);
	}
	
}
