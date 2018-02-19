// TimeKiller - kill a thread after a given timeout has elapsed
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

/// Kill a thread after a given timeout has elapsed
// <P>
// A simple timeout class. You give it a thread to watch and a timeout
// in milliseconds. After the timeout has elapsed, the thread is killed
// with a Thread.stop(). If the thread finishes successfully before then,
// you can cancel the timeout with a done() call; you can also re-use the
// timeout on the same thread with the reset() call.
// <P>
// <A HREF="/resources/classes/Acme/TimeKiller.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class TimeKiller implements Runnable {
	
	private Thread targetThread;
	private long millis;
	private Thread watcherThread;
	private boolean loop;
	private boolean enabled;
	
	/// Constructor. Give it a thread to watch, and a timeout in milliseconds.
	// After the timeout has elapsed, the thread gets killed. If you want
	// to cancel the kill, just call done().
	public TimeKiller(Thread targetThread, long millis) {
		this.targetThread = targetThread;
		this.millis = millis;
		watcherThread = new Thread(this);
		enabled = true;
		watcherThread.start();
		// Hack - pause a bit to let the watcher thread get started.
		try {
			Thread.sleep(100);
		} catch(InterruptedException e) {
		}
	}
	
	/// Constructor, current thread.
	public TimeKiller(long millis) {
		this(Thread.currentThread(), millis);
	}
	
	/// Call this when the target thread has finished.
	public synchronized void done() {
		loop = false;
		enabled = false;
		notify();
	}
	
	/// Call this to restart the wait from zero.
	public synchronized void reset() {
		loop = true;
		notify();
	}
	
	/// Call this to restart the wait from zero with a different timeout value.
	public synchronized void reset(long millis) {
		this.millis = millis;
		reset();
	}
	
	/// The watcher thread - from the Runnable interface.
	// This has to be pretty anal to avoid monitor lockup, lost
	// threads, etc.
	public synchronized void run() {
		Thread me = Thread.currentThread();
		me.setPriority(Thread.MAX_PRIORITY);
		if(enabled) {
			do {
				loop = false;
				try {
					wait(millis);
				} catch(InterruptedException e) {
				}
			} while(enabled && loop);
		}
		if(enabled && targetThread.isAlive())
			targetThread.stop();
	}
	
	/******************************************************************************
	 * /// Test routine.
	 * public static void main( String[] args )
	 * {
	 * System.out.println( (new Date()) + " Setting ten-second timeout..." );
	 * TimeKiller tk = new TimeKiller( 10000 );
	 * try
	 * {
	 * System.out.println(
	 * (new Date()) + " Starting twenty-second pause..." );
	 * Thread.sleep( 20000 );
	 * System.out.println(
	 * (new Date()) + " Another twenty-second pause..." );
	 * Thread.sleep( 20000 );
	 * }
	 * catch ( InterruptedException e )
	 * {
	 * System.out.println(
	 * (new Date()) + " Caught InterruptedException" );
	 * }
	 * catch ( ThreadDeath e )
	 * {
	 * System.out.println( (new Date()) + " Caught ThreadDeath" );
	 * throw e;
	 * }
	 * System.out.println( (new Date()) + " Oops - pauses finished!" );
	 * }
	 ******************************************************************************/
	
}
