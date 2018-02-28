// WidgetTest - test routine for the ACME widgets
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

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;

/// Test routine for the ACME widgets.
// <P>
// <A HREF="/resources/classes/Acme/Widgets/WidgetTest.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class WidgetTest extends Frame {
	
	public static void main(String[] args) {
		new WidgetTest();
	}
	
	/// Constructor.
	public WidgetTest() {
		Panel panel;
		BorderPanel borderPanel;
		
		setTitle("WidgetTest");
		
		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets(5, 5, 5, 5);
		
		panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new ImageLabel(Acme.GuiUtils.noticeIcon(this)));
		panel.add(new Slider(0, 100));
		gb.setConstraints(panel, gbc);
		add(panel);
		
		panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		borderPanel = new BorderPanel(BorderPanel.SOLID);
		borderPanel.add(new Label("Solid"));
		panel.add(borderPanel);
		
		borderPanel = new BorderPanel(BorderPanel.RAISED);
		borderPanel.add(new Label("Raised"));
		panel.add(borderPanel);
		
		borderPanel = new BorderPanel(BorderPanel.LOWERED);
		borderPanel.add(new Label("Lowered"));
		panel.add(borderPanel);
		
		borderPanel = new BorderPanel(BorderPanel.IN);
		borderPanel.add(new Label("In"));
		panel.add(borderPanel);
		
		borderPanel = new BorderPanel(BorderPanel.OUT);
		borderPanel.add(new Label("Out"));
		panel.add(borderPanel);
		
		gb.setConstraints(panel, gbc);
		add(panel);
		
		borderPanel = new BorderPanel(BorderPanel.LOWERED, 1);
		borderPanel.setLayout(new BorderLayout());
		borderPanel.add("Center", new Spacer(300, 1));
		gb.setConstraints(borderPanel, gbc);
		add(borderPanel);
		
		panel = new Panel();
		panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		panel.add(new Button("Busy"));
		panel.add(new Button("Notice"));
		panel.add(new Button("Warning"));
		panel.add(new Button("Error"));
		panel.add(new Button("YesNo"));
		panel.add(new Button("YesNoCancel"));
		panel.add(new Button("OkCancel"));
		gb.setConstraints(panel, gbc);
		add(panel);
		
		Button button = new Button("Quit");
		gb.setConstraints(button, gbc);
		add(button);
		
		pack();
		validate();
		show();
	}
	
	/// Event handler.
	public boolean handleEvent(Event evt) {
		switch(evt.id) {
			case Event.ACTION_EVENT:
				if(evt.arg.equals("Busy")) {
					BusyBox b = new BusyBox(this, "Sample BusyBox");
					System.err.println("BusyBox created.");
					b.show();
					System.err.println("BusyBox shown.");
					try {
						Thread.sleep(2000);
					} catch(InterruptedException ignore) {
					}
					b.done();
					System.err.println("BusyBox done.");
					return true;
				} else if(evt.arg.equals("Notice")) {
					NoticeBox n = new NoticeBox(this, "Sample NoticeBox");
					System.err.println("NoticeBox created.");
					n.show();
					System.err.println("NoticeBox shown.");
					return true;
				} else if(evt.arg.equals("Warning")) {
					WarningBox w = new WarningBox(this, "Sample WarningBox");
					System.err.println("WarningBox created.");
					w.show();
					System.err.println("WarningBox shown.");
					return true;
				} else if(evt.arg.equals("Error")) {
					ErrorBox e = new ErrorBox(this, "Sample ErrorBox");
					System.err.println("ErrorBox created.");
					e.show();
					System.err.println("ErrorBox shown.");
					return true;
				} else if(evt.arg.equals("YesNo")) {
					YesNoBox yn = new YesNoBox(this, "Sample YesNoBox");
					System.err.println("YesNoBox created.");
					yn.show();
					System.err.println("YesNoBox shown.");
					System.err.println("Answer is " + WidgetUtils.enumToString(yn.getAnswer()));
					return true;
				} else if(evt.arg.equals("YesNoCancel")) {
					YesNoCancelBox ync = new YesNoCancelBox(this, "Sample YesNoCancelBox");
					System.err.println("YesNoCancelBox created.");
					ync.show();
					System.err.println("YesNoCancelBox shown.");
					System.err.println("Answer is " + WidgetUtils.enumToString(ync.getAnswer()));
					return true;
				} else if(evt.arg.equals("OkCancel")) {
					OkCancelBox oc = new OkCancelBox(this, "Sample OkCancelBox");
					System.err.println("OkCancelBox created.");
					oc.show();
					System.err.println("OkCancelBox shown.");
					System.err.println("Answer is " + WidgetUtils.enumToString(oc.getAnswer()));
					return true;
				} else if(evt.arg.equals("Quit"))
					System.exit(0);
				break;
		}
		return super.handleEvent(evt);
	}
	
}
