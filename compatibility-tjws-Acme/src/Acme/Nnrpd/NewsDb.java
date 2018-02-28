// NewsDb - netnews database template
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

package Acme.Nnrpd;

import java.util.Enumeration;
import java.util.Random;

/// Netnews database template.
// <P>
// This is an abstract API representing a news database.
// Just the back-end, no user interface stuff at all.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/NewsDb.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

abstract public class NewsDb {
	
	long dbStamp;
	
	/// Constructor.
	// @exception NewsDbException if something goes wrong
	public NewsDb() throws NewsDbException {
		dbStamp = (new Random()).nextLong();
	}
	
	/// Get the database stamp.
	// This is used internally when dealing with multiple news databases,
	// to make sure that an obect returned from one is not passed to another.
	// @exception NewsDbException if something goes wrong
	protected long getDbStamp() throws NewsDbException {
		return dbStamp;
	}
	
	/// Attempt authorization.
	// This is similar to the NNTP "AUTHINFO" command.
	// @exception NewsDbException if something goes wrong
	abstract public boolean authorize(String user, String password) throws NewsDbException;
	
	/// Whether posting is allowed.
	// @exception NewsDbException if something goes wrong
	abstract public boolean getPostingOk() throws NewsDbException;
	
	/// Get a group by name.
	// This is similar to the NNTP "GROUP" command.
	// @exception NewsDbException if something goes wrong
	abstract public NewsDbGroup getGroup(String groupName) throws NewsDbException;
	
	/// Get an article by group and number.
	// This is similar to the NNTP "ARTICLE" command.
	// @exception NewsDbException if something goes wrong
	abstract public NewsDbArticle getArticle(NewsDbGroup group, int artNum) throws NewsDbException;
	
	/// Get an article by message-id.
	// This is similar to the NNTP "ARTICLE" command.
	// @exception NewsDbException if something goes wrong
	abstract public NewsDbArticle getArticle(String messageId) throws NewsDbException;
	
	/// Get a specified header from an article by group and number.
	// @exception NewsDbException if something goes wrong
	public String getHeader(String name, NewsDbGroup group, int artNum) throws NewsDbException {
		// First try fast method.
		String[] names = { name };
		String[][] result = getHeaders(names, group, artNum, artNum);
		if(result != null)
			return result[0][0];
		// Use other means.
		NewsDbArticle article = getArticle(group, artNum);
		if(article != null)
			return article.getHeader(name);
		return null;
	}
	
	/// Get specified headers from a range of articles by group and number.
	// Some implementations have special faster methods for getting
	// headers from articles, e.g. an XOVER command to a remote server
	// or a local overview database. If no such fast method is available,
	// this returns null and the caller should fall back on conventional
	// means.
	// @exception NewsDbException if something goes wrong
	abstract public String[][] getHeaders(String[] names, NewsDbGroup group, int firstArtNum, int lastArtNum) throws NewsDbException;
	
	/// Get an enumeration of all the groups.
	// This is similar to the NNTP "LIST active" command.
	// @exception NewsDbException if something goes wrong
	abstract public Enumeration getGroups() throws NewsDbException;
	
	/// Get an enumeration of all groups created after a given time.
	// This is similar to the NNTP "NEWGROUPS" command.
	// @exception NewsDbException if something goes wrong
	abstract public Enumeration getGroups(long since) throws NewsDbException;
	
	/// Get an enumeration of all groups created after a given time that match
	// the given distributions pattern.
	// This is similar to the NNTP "NEWGROUPS" command.
	// @exception NewsDbException if something goes wrong
	abstract public Enumeration getGroups(long since, String distsPat) throws NewsDbException;
	
	/// Get an enumeration of all message-ids received after a given time
	// in groups matching the given pattern.
	// This is similar to the NNTP "NEWNEWS" command.
	// @exception NewsDbException if something goes wrong
	abstract public Enumeration getMessageIds(String groupsPat, long since) throws NewsDbException;
	
	/// Get an enumeration of all message-ids received after a given time
	// in groups matching the given pattern, that also match the
	// This is similar to the NNTP "NEWNEWS" command.
	// given distributions pattern.
	// @exception NewsDbException if something goes wrong
	abstract public Enumeration getMessageIds(String groupsPat, long since, String distsPat) throws NewsDbException;
	
	/// Post an article.
	// This is similar to the NNTP "POST" command.
	// @exception NewsDbException if something goes wrong
	abstract public void post(String artText) throws NewsDbException;
	
	/// Shut down the news database.
	abstract public void close();
	
}
