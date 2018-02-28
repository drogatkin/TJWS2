// Application - superclass for embeddable applications
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

import java.io.InputStream;
import java.io.PrintStream;

/// Superclass for embeddable applications.
// <P>
// Make all your applications a subclass of this, and you can more
// easily embed them in larger applications or applets. What it does
// is define an enhanced main routine that gives you three standard I/O
// streams to use instead of System.in, System.out, and System.err.
// <P>
// When run from the command line there's no difference between the enhanced
// main and the regular one - you get the System streams. The point of this
// class is that you can also run your applications from applets, from other
// applications, in pipelines, and in contexts yet to be invented, and then
// you get passed other streams appropriate to those contexts.
// <P>
// There are a few simple steps to converting your application to use
// this class.
// <OL>
// <LI> Make it a subclass of this class:
// <BLOCKQUOTE><PRE>
// public class YourApp extends Acme.Application
// </PRE></BLOCKQUOTE>
// <LI> Change the declaration of your existing main() routine to look
// like this:
// <BLOCKQUOTE><PRE>
// public int newMain( String[] args )
// </PRE></BLOCKQUOTE>
// Note that the enhanced newMain() is *not* static. It must be an
// instance method so that it can be called at runtime - Java has no way
// to call a static method of a runtime-loaded class.
// <LI> Add an old-style main() that calls a compatibility routine:
// <BLOCKQUOTE><PRE>
// public static void main( String[] args )
// {
// (new YourApp()).compat( args );
// }
// </PRE></BLOCKQUOTE>
// This lets you continue to run your application from the command line.
// <LI> Change your application to use the I/O streams in, out, and err,
// instead of directly using the ones from System. These are now
// variables in your class, inherited from Acme.Application, and
// are initialized with streams appropriate to the context in which
// you are running.
// <LI> Don't call System.exit(). Instead, return your exit status
// from the newMain() routine.
// <LI> Change all your static methods and variables to be non-static.
// As noted in step 3, peculiarities of Java mean the
// enhanced main must be non-static. Since this is required, you
// might as well take advantage of it and make the rest of your
// application non-static too. The advantage is you'll be able to
// run multiple copies of it at the same time in the same Java VM;
// perhaps in a pipeline.
// <P>
// But be sure and leave the old-style main() static.
// </OL>
// And that's about it.
// <P>
// <A HREF="/resources/classes/Acme/Application.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// For an example of a non-command-line context
// that can call these enhanced Applications,
// @see Acme.ApplicationApplet

public abstract class Application {
	
	/// Compatibility gateway between old-style static main() and new-style
	// non-static. Does not return.
	public void compat(String[] args) {
		int r = compat(args, System.in, System.out, System.err);
		// This shouldn't be necessary.
		System.out.flush();
		System.err.flush();
		System.exit(r);
	}
	
	/* private */ protected InputStream in;
	/* private */ protected PrintStream out;
	/* private */ protected PrintStream err;
	
	/// Compatibility gateway for contexts other than old-style main().
	public int compat(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
		in = stdin;
		out = stdout;
		err = stderr;
		return newMain(args);
	}
	
	/// Definition for enhanced main. Subclasses implement this instead
	// of the old static version of main(). This has to be an instance
	// method instead of a static method because there's some bug with
	// inherited static main()s.
	// @return exit status, 0 for ok
	public abstract int newMain(String[] args);
	
}
