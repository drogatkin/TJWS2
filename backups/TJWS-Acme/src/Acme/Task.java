// Task - a periodic action
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

import java.util.Vector;

/// A periodic action.
// <P>
// A Task represents a bit of code that you want to run repeatedly at
// equally-timed intervals. Doing the timing yourself via sleeps is
// either inaccurate, since it depends on how long the task takes to
// run, or else complicated. Well, this class does the timing accurately
// but hides the complexity from you.
// <P>
// Tasks use the Runnable interface, just like Threads. This means that
// just like with Threads, there are two different ways you can create
// a Task:
// <UL>
// <LI>
// You can have your class implement the Runnable interface and add
// a run() method:
// <BLOCKQUOTE><CODE><PRE>
// class Anim implements Runnable
// {
// public void run()
// {
// // do an animation frame
// }
// }
// </PRE></CODE></BLOCKQUOTE>
// To start this task you would use something like this:
// <BLOCKQUOTE><CODE><PRE>
// Anim a = new Anim();
// Acme.Task t = new Acme.Task( a, 1000 );
// </PRE></CODE></BLOCKQUOTE>
// Or if, as is likely, your class starts the task in its constructor:
// <BLOCKQUOTE><CODE><PRE>
// Acme.Task t = new Acme.Task( this, 1000 );
// </PRE></CODE></BLOCKQUOTE>
// <LI>
// The other way to create a Task is to make a subclass of Task and override
// the run() method:
// <BLOCKQUOTE><CODE><PRE>
// class AnimTask extends Acme.Task
// {
// public void run()
// {
// // do an animation frame
// }
// }
// </PRE></CODE></BLOCKQUOTE>
// Then you would start it like so:
// <BLOCKQUOTE><CODE><PRE>
// Acme.Task t = new AnimTask( 1000 );
// </PRE></CODE></BLOCKQUOTE>
// </UL>
// <P>
// <A HREF="/resources/classes/Acme/Task.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class Task implements Runnable {
	
	/* private */ protected Runnable runnable;
	/* private */ protected long msPeriod;
	/* private */ protected TaskRunner runner;
	/* private */ protected long nextRun;
	
	/// Constructor for subclasses that override run().
	public Task(long msPeriod) {
		this(null, msPeriod);
	}
	
	/// Constructor with a Runnable specified.
	public Task(Runnable runnable, long msPeriod) {
		this.runnable = runnable;
		this.msPeriod = msPeriod;
		runner = new TaskRunner(this);
		TaskTimer.add(this);
	}
	
	/// The body of the Task.
	// You must either override this method by subclassing class Task,
	// or you must create the Task with a Runnable target.
	public void run() {
		if(runnable == null)
			throw new RuntimeException();	// didn't override
		runnable.run();
	}
	
	/// Stops a Task from running in the future.
	public synchronized void deschedule() {
		TaskTimer.deschedule(this);
	}
	
}

// TaskRunner - thread for running a task
//
// There's one of these for each Task. However, we can't put these methods
// into the Task class itself because the run() method has to be a loop here.

class TaskRunner extends Thread {
	
	private Task task;
	/* private */ protected boolean done;
	
	public TaskRunner(Task task) {
		this.task = task;
		done = false;
		this.start();
	}
	
	public synchronized void run() {
		while(true) {
			try {
				wait();
			} catch(InterruptedException e) {
			}
			if(done)
				break;
			task.run();
		}
	}
	
	public synchronized void notifier() {
		notify();
	}
	
}

// TaskTimer - trigger TaskRunners at the right moment
//
// Only one of these gets created, and it handles the timing of all the
// Tasks.

class TaskTimer extends Thread {
	
	private static boolean done = true;
	private static Vector tasks = null;
	private static TaskTimer timer = null;
	
	public static synchronized void add(Task task) {
		task.nextRun = System.currentTimeMillis() + task.msPeriod;
		if(tasks == null)
			tasks = new Vector();
		tasks.addElement(task);
		sortTasks(tasks);
		if(timer == null) {
			done = false;
			timer = new TaskTimer();
		}
	}
	
	public static synchronized void deschedule(Task task) {
		task.runner.done = true;
		task.runner.notifier();
		tasks.removeElement(task);
		if(tasks.size() == 0) {
			done = true;
			tasks = null;
			timer.notifier();
			timer = null;
		}
	}
	
	public TaskTimer() {
		this.setPriority(Thread.MAX_PRIORITY);
		this.start();
	}
	
	public synchronized void run() {
		while(true) {
			if(done)
				break;
				
			// The tasks are kept sorted by nextRun, so the next runnable
			// task is the first one in the list.
			Task nextTask = (Task) tasks.elementAt(0);
			long nextRun = nextTask.nextRun;
			
			// Sleep until it's time to run it.
			try {
				wait(nextRun - System.currentTimeMillis());
			} catch(InterruptedException e) {
			}
			
			if(done)
				break;
			
			// Run all tasks that should be run.
			long now = System.currentTimeMillis();
			for(int i = 0; i < tasks.size(); ++i) {
				Task task = (Task) tasks.elementAt(i);
				if(task.nextRun > now)
					break;
				task.runner.notifier();
				task.nextRun += task.msPeriod;
			}
			// And re-sort, since the nextRuns have changed.
			sortTasks(tasks);
		}
	}
	
	/// Stupid bubble sort. Maybe someday Java will have a Sortable interface.
	private static void sortTasks(Vector tasks) {
		for(int i = 0; i < tasks.size() - i; ++i) {
			Task taski = (Task) tasks.elementAt(i);
			for(int j = i + 1; j < tasks.size(); ++j) {
				Task taskj = (Task) tasks.elementAt(j);
				if(taskj.nextRun < taski.nextRun) {
					tasks.setElementAt(taskj, i);
					tasks.setElementAt(taski, j);
					taski = taskj;
				}
			}
		}
	}
	
	public synchronized void notifier() {
		notify();
	}
	
}
