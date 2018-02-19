// HttpSession - an association of HTTP interactions
//
// API based on documentation from JavaSoft.
//
// Copyright (C) 1998 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.
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

package Acme.Serve.servlet.http;

/// An association of HTTP interactions.
// <P>
// This is taken from JavaSoft's Servlet API documentation.
// <P>
// <A HREF="/resources/classes/Acme/Serve/servlet/http/HttpSession.java">Fetch
/// the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public interface HttpSession {
	
	/// Returns the identifier assigned to this session. An HttpSession's
	// identifier is a unique string that is created and maintained by
	// HttpSessionContext.
	public String getId();
	
	/// Returns the context in which this session is bound.
	public HttpSessionContext getSessionContext();
	
	/// Returns the time at which this session representation was created.
	public long getCreationTime();
	
	/// Returns the last time the client sent a request carrying the identifier
	// assigned to the session. Application level operations, such as getting
	// or setting a value associated with the session, does not affect the
	// access time.
	// <P>
	// This information is particularly useful in session management policies.
	// For example, a session manager could leave all sessions which have not
	// been used in a long time in a given context. The sessions can be
	// sorted according to age to optimize some task.
	public long getLastAccessedTime();
	
	/// Causes this representation of the session to be invalidated and removed
	// from its context.
	// @exception IllegalStateException if an attempt is made to access session
	/// data after the session has been invalidated
	public void invalidate();
	
	/// Binds the specified object into the session's application layer data
	// with the given name. Any existing binding with the same name is
	// replaced. New (or existing) values that implement the
	// HttpSessionBindingListener interface will call its valueBound()
	// method.
	public void putValue(String name, Object value);
	
	/// Returns the object bound to the given name in the session's application
	// layer data. Returns null if there is no such binding.
	public Object getValue(String name);
	
	/// Removes the object bound to the given name in the session's application
	// layer data. Does nothing if there is no object bound to the given name.
	// The value that implements the HttpSessionBindingListener interface will
	// call its valueUnbound() method.
	public void removeValue(String name);
	
	/// Returns an array of the names of all the application layer data objects
	// bound into the session. For example, if you want to delete all of the
	// data objects bound into the session, use this method to obtain their
	// names.
	// public String[] getValueNames();
	
	/// A session is considered to be "new" if it has been created by the
	// server, but the client has not yet acknowledged joining the session.
	// For example, if the server supported only cookie-based sessions and the
	// client had completely disabled the use of cookies, then calls to
	// HttpServletRequest.getSession() would always return "new" sessions.
	public boolean isNew();
	
}
