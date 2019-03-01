/* tjws - AsyncContextImpl.java
 * Copyright (C) 2004-2010 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  $Id: AsyncContextImpl.java,v 1.6 2010/07/24 07:36:05 dmitriy Exp $
 * Created on May 11, 2010
 */
package rogatkin.web;

import java.util.LinkedList;
import java.util.HashMap;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.AsyncEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import Acme.Serve.Serve;

public class AsyncContextImpl implements AsyncContext, Serve.AsyncCallback {
	private ServletRequest request;

	private ServletResponse response;

	private Serve.ServeConnection servConn;

	private long timeout;

	private LinkedList<AsyncListener> listeners;

	private HashMap<AsyncListener, Object[]> associatedData;

	private boolean completed;

	AsyncContextImpl(ServletRequest request, ServletResponse response, Serve.ServeConnection servConn) {
		this.request = request;
		this.response = response;
		this.servConn = servConn;
		listeners = new LinkedList<AsyncListener>();
		associatedData = new HashMap<AsyncListener, Object[]>();
		servConn.spawnAsync(this);
	}

	@Override
	public void addListener(AsyncListener listener) {
		synchronized (listeners) {
			// TODO should be used Set (not List)?
			if (listeners.contains(listener) == false)
				listeners.add(listener);
		}

	}

	@Override
	public void addListener(AsyncListener listener, ServletRequest req, ServletResponse resp) {
		addListener(listener);
		Object[] data = new Object[2];
		data[0] = req;
		data[1] = resp;
		associatedData.put(listener, data);
	}

	@Override
	public void complete() {
		for (AsyncListener listener : listeners)
			try {
				Object[] data = associatedData.get(listener);
				listener.onComplete(data == null ? new AsyncEvent(this) : new AsyncEvent(this,
						(ServletRequest) data[0], (ServletResponse) data[1]));
			} catch (Throwable t) {
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
			}
		servConn.joinAsync();
		completed = true;
	}

	@Override
	public <T extends AsyncListener> T createListener(Class<T> listenerClass) throws ServletException {
		try {
			return listenerClass.newInstance();
		} catch (Exception e) {
			throw new ServletException("Can't create " + listenerClass + ", cause: " + e);
		}

	}

	@Override
	public void dispatch() {
		dispatch(((HttpServletRequest) request).getServletPath() + ((HttpServletRequest) request).getPathInfo());
		//dispatch(((HttpServletRequest) request).getRequestURI());
	}

	@Override
	public void dispatch(String path) {
		dispatch(request.getServletContext(), path);
	}

	@Override
	public void dispatch(ServletContext servletContext, String path) {
		if (completed)
			throw new IllegalStateException("Can't process dispatch after complete()");
		try {
			// TODO check if error handling
			((WebAppServlet)servletContext).dispatch(path, request, response);
		} catch (Exception e) {
			for (AsyncListener listener : listeners)
				try {
					Object[] data = associatedData.get(listener);
					listener.onError(data == null ? new AsyncEvent(this, e) : new AsyncEvent(this,
							(ServletRequest) data[0], (ServletResponse) data[1], e));
				} catch (Throwable t) {
					if (t instanceof ThreadDeath)
						throw (ThreadDeath) t;
				}
		}
	}

	@Override
	public ServletRequest getRequest() {
		return request;
	}

	@Override
	public ServletResponse getResponse() {
		return response;
	}

	@Override
	public long getTimeout() {
		return timeout;
	}

	@Override
	public boolean hasOriginalRequestAndResponse() {
		return request == servConn && response == servConn;
	}

	@Override
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public void start(Runnable r) {
		((WebAppServlet) request.getServletContext()).asyncThreads.execute(r);
	}

	public void notifyTimeout() {
		for (AsyncListener listener : listeners)
			try {
				Object[] data = associatedData.get(listener);
				listener.onTimeout(data == null ? new AsyncEvent(this) : new AsyncEvent(this, (ServletRequest) data[0],
						(ServletResponse) data[1]));
			} catch (Throwable t) {
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
			}
	}

	public void notifyStart() {
		for (AsyncListener listener : listeners)
			try {
				Object[] data = associatedData.get(listener);
				listener.onStartAsync(data == null ? new AsyncEvent(this) : new AsyncEvent(this,
						(ServletRequest) data[0], (ServletResponse) data[1]));
			} catch (Throwable t) {
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
			}
	}
}
