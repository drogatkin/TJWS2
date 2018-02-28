// MultiplexNewsDb - proxy netnews database
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
import java.util.StringTokenizer;
import java.util.Vector;

import Acme.Utils;

/// Multiplex netnews database.
// <P>
// This is a news database that forwards requests to different sub-databases
// based on the group name.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/MultiplexNewsDb.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class MultiplexNewsDb extends NewsDb {
	
	private Vector groupsPats = new Vector();
	private Vector newsDbs = new Vector();
	
	/// The standard overview.fmt. Only if all sub-DBs implement this
	// identical format do we do overviewing.
	private static String[] overviewFmt = { "Subject", "From", "Date", "Message-ID", "References", "Bytes", "Lines" };
	
	/// Constructor.
	// @exception NewsDbException if something goes wrong
	public MultiplexNewsDb() throws NewsDbException {
	}
	
	/// Add a sub-database for a given pattern of groups.
	public void add(String groupsPat, NewsDb newsDb) {
		groupsPats.addElement(groupsPat);
		newsDbs.addElement(newsDb);
	}
	
	/// Attempt authorization.
	// @exception NewsDbException if something goes wrong
	public boolean authorize(String user, String password) throws NewsDbException {
		// If authorization succeeds for all sub-dbs, we return ok.
		for(int i = 0; i < newsDbs.size(); ++i) {
			NewsDb newsDb = (NewsDb) newsDbs.elementAt(i);
			if(!newsDb.authorize(user, password))
				return false;
		}
		return true;
	}
	
	/// Whether posting is allowed.
	// @exception NewsDbException if something goes wrong
	public boolean getPostingOk() throws NewsDbException {
		// This is a guess, really - if posting is ok for any sub-db, we
		// return ok.
		for(int i = 0; i < newsDbs.size(); ++i) {
			NewsDb newsDb = (NewsDb) newsDbs.elementAt(i);
			if(newsDb.getPostingOk())
				return true;
		}
		return false;
	}
	
	/// Get a group by name.
	// @exception NewsDbException if something goes wrong
	public NewsDbGroup getGroup(String groupName) throws NewsDbException {
		NewsDb newsDb = findNewsDb(groupName);
		return newsDb.getGroup(groupName);
	}
	
	/// Get an article by group and number.
	// @exception NewsDbException if something goes wrong
	public NewsDbArticle getArticle(NewsDbGroup group, int artNum) throws NewsDbException {
		for(int i = 0; i < newsDbs.size(); ++i) {
			NewsDb newsDb = (NewsDb) newsDbs.elementAt(i);
			if(newsDb.getDbStamp() == group.getDbStamp())
				return newsDb.getArticle(group, artNum);
		}
		return null;
	}
	
	/// Get an article by message-id.
	// @exception NewsDbException if something goes wrong
	public NewsDbArticle getArticle(String messageId) throws NewsDbException {
		// This is an even more expensive operation than usual in the
		// multiplexing database. Since the dispatching is by group name
		// but you don't have a group name, you have to try each sub-db.
		for(int i = 0; i < newsDbs.size(); ++i) {
			NewsDb newsDb = (NewsDb) newsDbs.elementAt(i);
			NewsDbArticle article = newsDb.getArticle(messageId);
			if(article != null)
				return article;
		}
		return null;
	}
	
	/// Get specified headers from a range of articles by group and number.
	// Some implementations have special faster methods for getting
	// headers from articles, e.g. an XOVER command to a remote server
	// or a local overview database. If no such fast method is available,
	// this returns null and the caller should fall back on conventional
	// means.
	// @exception NewsDbException if something goes wrong
	public String[][] getHeaders(String[] names, NewsDbGroup group, int firstArtNum, int lastArtNum) throws NewsDbException {
		NewsDb newsDb = findNewsDb(group.getName());
		return newsDb.getHeaders(names, group, firstArtNum, lastArtNum);
	}
	
	/// Get an enumeration of all the groups.
	// @exception NewsDbException if something goes wrong
	public Enumeration getGroups() throws NewsDbException {
		return new MultiplexNewsDbGroups(newsDbs.elements());
	}
	
	/// Get an enumeration of all groups created after a given time.
	// @exception NewsDbException if something goes wrong
	public Enumeration getGroups(long since) throws NewsDbException {
		return new MultiplexNewsDbGroups(newsDbs.elements(), since);
	}
	
	/// Get an enumeration of all groups created after a given time that match
	// a given distributions pattern.
	// @exception NewsDbException if something goes wrong
	public Enumeration getGroups(long since, String distsPat) throws NewsDbException {
		return new MultiplexNewsDbGroups(newsDbs.elements(), since, distsPat);
	}
	
	/// Get an enumeration of all message-ids received after a given time
	// in groups matching the given pattern.
	// @exception NewsDbException if something goes wrong
	public Enumeration getMessageIds(String groupsPat, long since) throws NewsDbException {
		return new MultiplexNewsDbMessageIds(newsDbs.elements(), groupsPat, since);
	}
	
	/// Get an enumeration of all message-ids received after a given time
	// in groups matching the given pattern, that also match the
	// given distributions pattern.
	// @exception NewsDbException if something goes wrong
	public Enumeration getMessageIds(String groupsPat, long since, String distsPat) throws NewsDbException {
		return new MultiplexNewsDbMessageIds(newsDbs.elements(), groupsPat, since, distsPat);
	}
	
	/// Post an article.
	// @exception NewsDbException if something goes wrong
	public void post(String text) throws NewsDbException {
		// Find the newsgroups line in the article and parse it to
		// find a group, then post to the right db for that group.
		String newsgroups = NewsDbArticle.getHeader(text, "Newsgroups");
		if(newsgroups == null || newsgroups.length() == 0)
			throw new NewsDbException("article has no newsgroups");
		StringTokenizer st = new StringTokenizer(newsgroups, ",");
		while(st.hasMoreTokens()) {
			String groupName = st.nextToken();
			NewsDb newsDb;
			try {
				newsDb = findNewsDb(groupName);
			} catch(NewsDbException e) {
				// Try another group.
				continue;
			}
			newsDb.post(text);
			return;
		}
		throw new NewsDbException("can't find a news database supporting any group in " + newsgroups);
	}
	
	/// Shut down the news database.
	public void close() {
		for(int i = 0; i < newsDbs.size(); ++i) {
			NewsDb newsDb = (NewsDb) newsDbs.elementAt(i);
			newsDb.close();
		}
		newsDbs = null;
	}
	
	/// Find the sub-database for a given group.
	private NewsDb findNewsDb(String groupName) throws NewsDbException {
		for(int i = 0; i < groupsPats.size(); ++i) {
			String groupsPat = (String) groupsPats.elementAt(i);
			if(Utils.match(groupsPat, groupName))
				return (NewsDb) newsDbs.elementAt(i);
		}
		throw new NewsDbException("no database found for group " + groupName);
	}
	
}

class MultiplexNewsDbGroups implements Enumeration {
	
	private Enumeration dbEnum;
	private long since;
	private String distsPat;
	
	private Enumeration groupEnum;
	private NewsDbGroup group;
	
	public MultiplexNewsDbGroups(Enumeration dbEnum) {
		this(dbEnum, -1, null);
	}
	
	public MultiplexNewsDbGroups(Enumeration dbEnum, long since) {
		this(dbEnum, since, null);
	}
	
	public MultiplexNewsDbGroups(Enumeration dbEnum, long since, String distsPat) {
		this.dbEnum = dbEnum;
		this.since = since;
		this.distsPat = distsPat;
		groupEnum = null;
		group = null;
	}
	
	public boolean hasMoreElements() {
		if(group != null)
			return true;
		while(true) {
			if(groupEnum == null) {
				if(dbEnum == null)
					return false;
				if(!dbEnum.hasMoreElements()) {
					dbEnum = null;
					return false;
				}
				try {
					NewsDb db = (NewsDb) dbEnum.nextElement();
					if(since != -1 && distsPat != null)
						groupEnum = db.getGroups(since, distsPat);
					else if(since != -1)
						groupEnum = db.getGroups(since);
					else
						groupEnum = db.getGroups();
				} catch(NewsDbException e) {
					continue;
				}
			}
			if(!groupEnum.hasMoreElements()) {
				groupEnum = null;
				continue;
			}
			group = (NewsDbGroup) groupEnum.nextElement();
			return true;
		}
	}
	
	public Object nextElement() {
		if(!hasMoreElements())
			return null;
		NewsDbGroup g = group;
		group = null;
		return g;
	}
	
}

class MultiplexNewsDbMessageIds implements Enumeration {
	
	private Enumeration dbEnum;
	private String groupsPat;
	private long since;
	private String distsPat;
	
	private Enumeration messageIdEnum;
	private String messageId;
	
	public MultiplexNewsDbMessageIds(Enumeration dbEnum, String groupsPat, long since) {
		this(dbEnum, groupsPat, since, null);
	}
	
	public MultiplexNewsDbMessageIds(Enumeration dbEnum, String groupsPat, long since, String distsPat) {
		this.dbEnum = dbEnum;
		this.groupsPat = groupsPat;
		this.since = since;
		this.distsPat = distsPat;
		messageIdEnum = null;
		messageId = null;
	}
	
	public boolean hasMoreElements() {
		if(messageId != null)
			return true;
		while(true) {
			if(messageIdEnum == null) {
				if(dbEnum == null)
					return false;
				if(!dbEnum.hasMoreElements()) {
					dbEnum = null;
					return false;
				}
				try {
					NewsDb db = (NewsDb) dbEnum.nextElement();
					if(distsPat != null)
						messageIdEnum = db.getMessageIds(groupsPat, since, distsPat);
					else
						messageIdEnum = db.getMessageIds(groupsPat, since);
				} catch(NewsDbException e) {
					continue;
				}
			}
			if(!messageIdEnum.hasMoreElements()) {
				messageIdEnum = null;
				continue;
			}
			messageId = (String) messageIdEnum.nextElement();
			return true;
		}
	}
	
	public Object nextElement() {
		if(!hasMoreElements())
			return null;
		String mid = messageId;
		messageId = null;
		return mid;
	}
	
}
