/*
 * tjws - ObjectPool.java
 * Copyright (C) 2010 Dmitriy Rogatkin. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * Visit http://tjws.sourceforge.net to get the latest information
 * about Rogatkin's products.
 * $Id: ObjectPool.java,v 1.15 2012/06/23 06:59:29 dmitriy Exp $
 * Created on Mar 5, 2008
 * @author Dmitriy
 */
package rogatkin.app;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class ObjectPool<O> {
	
	protected String[] descardMethods = { "destroy", "close" };
	protected BlockingQueue<O> pool;
	protected ArrayList<O> borrowed;
	protected int timeout;
	private boolean monitor;
	public static Class<?> Wrapper;
	
	static {
		try {
			Wrapper = Class.forName("java.sql.Wrapper");
		} catch (ClassNotFoundException e) {
			System.err.printf("Your Java runtime doesn't support JDBC 3.0 (%s), some functionality can be supressed.", e);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	protected abstract O create();
	
	/**
	 * 
	 * @param object
	 */
	protected void discard(O object) {
		// System.err.printf("Discard %s%n", obj);
		if (Wrapper != null && Wrapper.isAssignableFrom(object.getClass()))
			try {
				// obj = (O)((Wrapper)obj).unwrap(obj.getClass());
				object = (O) object.getClass().getMethod("unwrap", Class.class).invoke(object, object.getClass());
			} catch (Exception e1) {
			}
		for (int i = 0; i < descardMethods.length; i++) {
			try {
				object.getClass().getMethod(descardMethods[i], new Class[] {}).invoke(object, new Object[] {});
				break;
			} catch (IllegalArgumentException e) {
			} catch (SecurityException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			} catch (NoSuchMethodException e) {
			}
		}
	}
	
	public ObjectPool(BlockingQueue<O> blockingQueue) {
		if (blockingQueue != null) {
			pool = blockingQueue;
			borrowed = new ArrayList<O>(blockingQueue.size());
		} else {
			throw new NullPointerException();
		}
	}
	
	public O get() {
		O result = get1();
		if (result != null) {
			assert (borrowed.contains(result) == false);
			synchronized (borrowed) {
				borrowed.add(result);
				// new Exception(String.format("Added in used %s size %d",
				// result, borrowed.size())).printStackTrace();
			}
			if (monitor) {
				// get stack trace for identifying a caller and store the
				// information among with request time
				if (result instanceof Monitor)
					((Monitor) result).putMark(getClass().getName());
			}
		}
		return result;
	}
	
	private O get1() {
		if (pool.isEmpty() && borrowed.size() < getCapacity()) {
			return create();
			// System.err.printf("get %s%n", o);
		}
		
		try {
			if (timeout > 0) {
				return pool.poll(timeout, TimeUnit.MILLISECONDS);
			}
			return pool.take();
		} catch (InterruptedException e) {
		}
		return null;
	}
	
	public void put(O obj) {
		assert borrowed.contains(obj);
		synchronized (borrowed) {
			if (borrowed.remove(obj) == false) {
				return; // connection already removed
			}
		}
		
		/*
		 * no synchronization between increasing limit and offering the
		 * connection for consumption is considered
		 * as acceptable, offering connection first and then decreasing limit
		 * can issue objects starvation,
		 * it will also require using set for borrowing list
		 * new Exception(String.format("returned %s, still in use: %d", obj,
		 * borrowed.size())).printStackTrace();
		 */
		if (pool.offer(obj) == false) {
			// no room, discard the object
			discard(obj);
		}
	}
	
	public void remove(O obj) {
		if (borrowed.contains(obj)) {
			synchronized (borrowed) {
				if (borrowed.remove(obj) == false) {
					System.err.println("Object " + obj + " wasn't removed");
				}
			}
		} else {
			if (pool.remove(obj) == false) {
				System.err.println("Object " + obj + " wasn't removed from pool");
			}
		}
		
		// TODO generally discard can have side effect
		discard(obj);
	}
	
	public void setTimeout(int to) {
		timeout = to;
	}
	
	/**
	 * resizes the pool
	 * 
	 * @param newSize
	 */
	public abstract int getCapacity();
	
	public void invalidate() {
		ArrayList<O> forDiscard = new ArrayList<O>();
		pool.drainTo(forDiscard);
		for (O o : forDiscard)
			discard(o);
		if (borrowed.size() > 0)
			throw new IllegalStateException("Pool invalidate with borrowed objects");
	}
	
	/**
	 * The interface is used for marking pooled object by requester id
	 * and access time
	 * 
	 * @author dmitriy
	 *
	 */
	public static interface Monitor {
		void putMark(String boundaryClassName);
		
		String getMarkCaller();
		
		long getMarkTime();
	}
	
}
