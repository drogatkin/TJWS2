// ApplicationApplet - run an Application in an Applet
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

package Acme;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.TextArea;
import java.io.InputStream;
import java.io.PrintStream;

/// Run an Application in an Applet.
// <P>
// This applet lets any command-line application that uses a specially-enhanced
// main() routine automatically run in a web browser or appletviewer.
// Here's an example:
// <BLOCKQUOTE>
// <APPLET WIDTH=400 HEIGHT=150 CODEBASE="/resources/classes"
/// CODE="Acme.ApplicationApplet"><PARAM NAME="bgcolor" VALUE="#99cc99">
// <PARAM NAME="class" VALUE="WebList">
// <PARAM NAME="args" VALUE="http://www.acme.com/jef/flow/">
// </APPLET>
// </BLOCKQUOTE>
// That's a command-line application running right now in your own browser.
// <P>
// The applet takes two parameters:
// <DL>
// <DT> CLASS
// <DD> The name of the class to run - your Application's name.
// <DT> ARGS
// <DD> The arguments to pass to the Application.
// </DL>
// <P>
// <A HREF="/resources/classes/Acme/ApplicationApplet.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// For details on how to adapt your application so that it can be run by
// ApplicationApplet,
// @see Application

public class ApplicationApplet extends Applet implements Runnable {
	
	/// Applet info.
	public String getAppletInfo() {
		return getClass().getName() + " - running via ApplicationApplet - Copyright (C) 1996 by Jef Poskanzer <jef@mail.acme.com>.  All rights reserved.";
	}
	
	/// Parameter info.
	public String[][] getParameterInfo() {
		String[][] info = { { "CLASS", "Class", "the application to load" }, { "ARGS", "string", "the argument list" }, };
		return info;
	}
	
	TextArea textArea = null;
	
	Application app = null;
	String[] args = null;
	
	/// Called when the applet is first created. This could be overridden
	// by a subclass to define what application to run and what args to
	// give it. This default version gets the application name and args
	// list from the parameters. A sample overriding version:
	// <BLOCKQUOTE><PRE>
	// public void init()
	// {
	// app = new MyApp(); // make an instance of this application
	// getArgs(); // get args from the applet parameters
	// }
	// </PRE></BLOCKQUOTE>
	// Pretty simple.
	public void init() {
		Acme.GuiUtils.handleBgcolor(this);
		
		if(textArea == null)
			makeTextArea();
		
		String appName = getParameter("CLASS");
		if(appName == null) {
			showStatus("Missing CLASS parameter");
			return;
		}
		// Hack to accomodate those who use file names instead of class names.
		if(appName.endsWith(".class"))
			appName = appName.substring(0, appName.length() - 6).replace('/', '.');
		if(appName.endsWith(".java"))
			appName = appName.substring(0, appName.length() - 5).replace('/', '.');
		
		try {
			app = (Application) Class.forName(appName).newInstance();
		} catch(ClassNotFoundException e) {
			showStatus("Class not found: " + appName);
		} catch(ClassCastException e) {
			showStatus("Class cast problem: " + e.getMessage());
		} catch(InstantiationException e) {
			showStatus("Instantiation problem - " + e.getMessage());
		} catch(IllegalAccessException e) {
			showStatus("Illegal class access - " + e.getMessage());
		}
		
		getArgs();
	}
	
	private void makeTextArea() {
		setLayout(new BorderLayout());
		textArea = new TextArea();
		textArea.setEditable(false);
		textArea.setFont(new Font("Courier", Font.PLAIN, 10));
		add("Center", textArea);
		validate();
	}
	
	/// Get the ARGS parameter and parse it into an array of Strings.
	public void getArgs() {
		String argList = getParameter("ARGS");
		if(argList == null)
			args = new String[0];
		else
			args = Acme.Utils.splitStr(argList);
	}
	
	Thread thread = null;
	
	/// Called when the applet should start itself.
	public void start() {
		if(thread == null) {
			// Start the thread.
			thread = new Thread(this);
			thread.start();
		}
	}
	
	/// Called when the applet should stop itself.
	public void stop() {
		if(thread != null) {
			// Stop the thread.
			thread.stop();
			thread = null;
		}
	}
	
	/// This is the part of Runnable that we implement - the routine that
	// gets called when the thread is started.
	public void run() {
		if(textArea == null)
			makeTextArea();
		if(app == null) {
			showStatus("No application defined");
			return;
		}
		if(args == null) {
			showStatus("No arguments defined");
			return;
		}
		
		InputStream in = new NullInputStream();
		PrintStream out = new APrintStream(new TextcompOutputStream(textArea));
		
		String appName = app.getClass().getName();
		showStatus(appName + " running...");
		out.println("% " + appName + " " + Acme.Utils.flattenStrarr(args));
		app.compat(args, in, out, out);
		out.print("% ");
		showStatus(appName + " done.");
	}
	
	/// Main program, so we can run as an application too.
	public static void main(String[] args) {
		new MainFrame(new ApplicationApplet(), args, 400, 500);
	}
	
}
