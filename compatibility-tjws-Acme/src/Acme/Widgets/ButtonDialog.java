// ButtonDialog - a generic modal button box
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
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;

/// A generic modal button box.
// <P>
// Puts up a dialog with a message and client-specified buttons.
// All user input is locked out. The program can retrieve the user's
// answer via the getAnswer() method.
// <P>
// NOTE: do not use this inside an event handler call! With some browsers
// this will cause a deadlock. Instead, have your event handler start a new
// thread to run the button box.
// <P>
// <A HREF="/resources/classes/Acme/Widgets/ButtonDialog.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class ButtonDialog extends Dialog implements Runnable {
	
	private String[] buttons;
	private int[] answers;
	
	/// Constructor, no title, one button.
	public ButtonDialog(Frame parent, String message, String button1, int answer1) {
		super(parent, true);
		build(message, button1, answer1);
	}
	
	/// Constructor, specified title, one button.
	public ButtonDialog(Frame parent, String title, String message, String button1, int answer1) {
		super(parent, title, true);
		build(message, button1, answer1);
	}
	
	/// Build routine, one button.
	private void build(String message, String button1, int answer1) {
		buttons = new String[1];
		answers = new int[1];
		buttons[0] = button1;
		answers[0] = answer1;
		build(message);
	}
	
	/// Constructor, no title, two buttons.
	public ButtonDialog(Frame parent, String message, String button1, int answer1, String button2, int answer2) {
		super(parent, true);
		build(message, button1, answer1, button2, answer2);
	}
	
	/// Constructor, specified title, two buttons.
	public ButtonDialog(Frame parent, String title, String message, String button1, int answer1, String button2, int answer2) {
		super(parent, title, true);
		build(message, button1, answer1, button2, answer2);
	}
	
	/// Build routine, two buttons.
	private void build(String message, String button1, int answer1, String button2, int answer2) {
		buttons = new String[2];
		answers = new int[2];
		buttons[0] = button1;
		answers[0] = answer1;
		buttons[1] = button2;
		answers[1] = answer2;
		build(message);
	}
	
	/// Constructor, no title, three buttons.
	public ButtonDialog(Frame parent, String message, String button1, int answer1, String button2, int answer2, String button3, int answer3) {
		super(parent, true);
		build(message, button1, answer1, button2, answer2, button3, answer3);
	}
	
	/// Constructor, specified title, three buttons.
	public ButtonDialog(Frame parent, String title, String message, String button1, int answer1, String button2, int answer2, String button3, int answer3) {
		super(parent, title, true);
		build(message, button1, answer1, button2, answer2, button3, answer3);
	}
	
	/// Build routine, three buttons.
	private void build(String message, String button1, int answer1, String button2, int answer2, String button3, int answer3) {
		buttons = new String[3];
		answers = new int[3];
		buttons[0] = button1;
		answers[0] = answer1;
		buttons[1] = button2;
		answers[1] = answer2;
		buttons[2] = button3;
		answers[2] = answer3;
		build(message);
	}
	
	/// Constructor, no title, four buttons.
	public ButtonDialog(Frame parent, String message, String button1, int answer1, String button2, int answer2, String button3, int answer3, String button4, int answer4) {
		super(parent, true);
		build(message, button1, answer1, button2, answer2, button3, answer3, button4, answer4);
	}
	
	/// Constructor, specified title, four buttons.
	public ButtonDialog(Frame parent, String title, String message, String button1, int answer1, String button2, int answer2, String button3, int answer3, String button4, int answer4) {
		super(parent, title, true);
		build(message, button1, answer1, button2, answer2, button3, answer3, button4, answer4);
	}
	
	/// Build routine, four buttons.
	private void build(String message, String button1, int answer1, String button2, int answer2, String button3, int answer3, String button4, int answer4) {
		buttons = new String[4];
		answers = new int[4];
		buttons[0] = button1;
		answers[0] = answer1;
		buttons[1] = button2;
		answers[1] = answer2;
		buttons[2] = button3;
		answers[2] = answer3;
		buttons[3] = button4;
		answers[3] = answer4;
		build(message);
	}
	
	/// Common build routine.
	private void build(String message) {
		setResizable(false);
		this.setBackground(Acme.GuiUtils.qmarkColor);
		
		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		
		ImageLabel imageLabel = new ImageLabel(Acme.GuiUtils.qmarkIcon(this));
		gb.setConstraints(imageLabel, gbc);
		add(imageLabel);
		Label label = new Label(message);
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gb.setConstraints(label, gbc);
		add(label);
		Panel buttonPanel = new Panel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		for(int i = 0; i < buttons.length; ++i)
			buttonPanel.add(new Button(buttons[i]));
		gb.setConstraints(buttonPanel, gbc);
		add(buttonPanel);
	}
	
	/// Show the box.
	public void show() {
		pack();
		validate();
		// We do the super.show() in another thread, cause for modal dialogs
		// it doesn't return until the dialog goes away. This isn't strictly
		// a problem for this class, cause we're going to do our own
		// wait-for-done below, but some clients may want to do some
		// processing before waiting.
		(new Thread(this)).start();
	}
	
	public void run() {
		super.show();
	}
	
	/// Event handler.
	public boolean handleEvent(Event evt) {
		switch(evt.id) {
			case Event.ACTION_EVENT:
				for(int i = 0; i < buttons.length; ++i)
					if(evt.arg.equals(buttons[i])) {
						click(answers[i]);
						return true;
					}
				break;
		}
		return super.handleEvent(evt);
	}
	
	private boolean clicked = false;
	private int answer;
	
	private synchronized void click(int val) {
		clicked = true;
		notify();
		answer = val;
		hide();
		// setVisible( false );
		dispose();
	}
	
	/// Get the answer.
	public synchronized int getAnswer() {
		while(!clicked) {
			try {
				wait();
			} catch(InterruptedException ignore) {
			}
		}
		return answer;
	}
	
}
