// ProxyNewsDb - proxy netnews database
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import Acme.Utils;

/// Proxy netnews database.
// <P>
// This is a news database that forwards requests to a remote news server.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/ProxyNewsDb.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class ProxyNewsDb extends NewsDb {
	
	// This switch is for getting around the "LIKE_PULLERS DONT" option
	// in INN 1.5+. In INN 1.5.1 the option is completely broken, and
	// admins have to set "LIKE_PULLERS DO" in order to get acceptable
	// reader performance. In versions after that, ...?
	private static final boolean avoidArticleHack = false;
	
	private String host;
	private int port;
	private Socket socket;
	private boolean postingOk;
	private String[] overviewFmt = null;
	private OverviewCache overviewCache;
	
	// The object's synchronization is basically on the I/O streams.
	private DataInputStream din;
	private PrintStream pout;
	
	// Group cache.
	private Hashtable groupTable = null;
	private long groupsFetched;
	
	// Group descriptions cache.
	private Hashtable groupDescsTable = null;
	private long groupDescsFetched;
	
	// State variables.
	private NewsDbGroup currentGroup = null;
	private int currentArtNum = -1;
	
	/// Constructor.
	// The proxyHost string should be "hostname:portnum", or just a
	// hostname in which case the default netnews port will be used.
	// @param proxyHost the remote news server to connect to
	// @param oCacheSize how many overview entries to save in the cache
	// @exception NewsDbException if something goes wrong
	public ProxyNewsDb(String proxyHost, int oCacheSize) throws NewsDbException {
		int colon = proxyHost.indexOf(':');
		if(colon == -1) {
			host = proxyHost;
			port = NnrpdUtils.DEFAULT_PORT;
		} else {
			host = proxyHost.substring(0, colon);
			port = Utils.parseInt(proxyHost.substring(colon + 1), NnrpdUtils.DEFAULT_PORT);
		}
		
		try {
			socket = new Socket(host, port);
			din = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			pout = new PrintStream(new CrLfOutputStream(new BufferedOutputStream(socket.getOutputStream())));
		} catch(IOException e) {
			throw new NewsDbException("problem opening socket to " + host + ": " + e);
		}
		
		// Read initial response.
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 200:
				postingOk = true;
				break;
			case 201:
				postingOk = false;
				break;
			case 502:
				throw new NewsDbException("no read permission on " + host);
			default:
				throw new NewsDbException("unexpected response to " + host + " initial connect: " + Utils.flattenStrarr(resp));
		}
		
		// Do SLAVE command - it probably does nothing, but it's in the spec.
		pout.println(NnrpdUtils.CMD_SLAVE);
		resp = readResp();
		// Ignore response.
		
		// See if there's an overview database. First try a LIST schema.
		pout.println(NnrpdUtils.CMD_LIST + " schema");
		resp = readResp();
		respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 215:
				try {
					overviewFmt = Utils.splitStr(NnrpdUtils.readText(din));
				} catch(IOException ignore) {
				}
				break;
			default:
				// That didn't work. Now try a LIST overview.fmt.
				pout.println(NnrpdUtils.CMD_LIST + " overview.fmt");
				resp = readResp();
				respNum = Utils.parseInt(resp[0], -1);
				switch(respNum) {
					case 215:
						try {
							overviewFmt = Utils.splitStr(NnrpdUtils.readText(din));
						} catch(IOException ignore) {
						}
						break;
					default:
						break;
				}
				break;
		}
		if(overviewFmt != null) {
			for(int i = 0; i < overviewFmt.length; ++i)
				if(overviewFmt[i].endsWith(":"))
					overviewFmt[i] = overviewFmt[i].substring(0, overviewFmt[i].length() - 1);
			overviewCache = new OverviewCache(oCacheSize);
		}
	}
	
	/// Attempt authorization.
	// @exception NewsDbException if something goes wrong
	public boolean authorize(String user, String password) throws NewsDbException {
		pout.println(NnrpdUtils.CMD_AUTHINFO + " USER " + user);
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 381:
				pout.println(NnrpdUtils.CMD_AUTHINFO + " PASS " + password);
				resp = readResp();
				respNum = Utils.parseInt(resp[0], -1);
				switch(respNum) {
					case 281:
						return true;
					case 502:
						return false;
					default:
						throw new NewsDbException("unexpected response to AUTHINFO command: " + Utils.flattenStrarr(resp));
				}
			default:
				throw new NewsDbException("unexpected response to AUTHINFO command: " + Utils.flattenStrarr(resp));
		}
	}
	
	/// Whether posting is allowed.
	// @exception NewsDbException if something goes wrong
	public boolean getPostingOk() throws NewsDbException {
		return postingOk;
	}
	
	/// Get a group by name.
	// @exception NewsDbException if something goes wrong
	public synchronized NewsDbGroup getGroup(String groupName) throws NewsDbException {
		// First check current group.
		if(currentGroup != null && groupName.equals(currentGroup.getName()))
			return currentGroup;
		// Next check group cache.
		checkGroups();
		NewsDbGroup group = (NewsDbGroup) groupTable.get(groupName);
		if(group != null)
			return group;
		// Not there. Try a GROUP command just to be sure.
		return setGroup(groupName);
	}
	
	/// Set the remote server's current group.
	private synchronized NewsDbGroup setGroup(String groupName) throws NewsDbException {
		pout.println(NnrpdUtils.CMD_GROUP + " " + groupName);
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 211:
				try {
					currentGroup = new NewsDbGroup(dbStamp, resp[4], Integer.parseInt(resp[1]), Integer.parseInt(resp[2]), Integer.parseInt(resp[3]), '?', (String) groupDescsTable.get(groupName));
				} catch(NumberFormatException e) {
					throw new NewsDbException("unparsable numbers in response to GROUP command: " + Utils.flattenStrarr(resp));
				}
				currentArtNum = currentGroup.getFirstArtNum();
				// Might as well put it in the group cache.
				groupTable.put(currentGroup.getName(), currentGroup);
				return currentGroup;
			case 411:
				return null;
			default:
				throw new NewsDbException("unexpected response to GROUP command: " + Utils.flattenStrarr(resp));
		}
	}
	
	/// Get an article by group and number.
	// @exception NewsDbException if something goes wrong
	public synchronized NewsDbArticle getArticle(NewsDbGroup group, int artNum) throws NewsDbException {
		// Check that this group is one of ours.
		if(group.getDbStamp() != dbStamp)
			throw new NewsDbException("mismatched database stamps");
		// Make sure it's the current group on the remote server.
		if(currentGroup == null || !group.getName().equals(currentGroup.getName()))
			setGroup(group.getName());
		// Do the ARTICLE command.
		pout.println(NnrpdUtils.CMD_ARTICLE + " " + artNum);
		// Get and interpret results.
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 220:
				currentArtNum = artNum;
				String text;
				try {
					text = NnrpdUtils.readText(din);
				} catch(IOException e) {
					throw new NewsDbException("problem fetching article: " + e);
				}
				return new NewsDbArticle(dbStamp, text);
			case 423:
				return null;
			default:
				throw new NewsDbException("unexpected response to ARTICLE command: " + Utils.flattenStrarr(resp));
		}
	}
	
	/// Get an article by message-id.
	// @exception NewsDbException if something goes wrong
	public synchronized NewsDbArticle getArticle(String messageId) throws NewsDbException {
		// Do the ARTICLE command.
		pout.println(NnrpdUtils.CMD_ARTICLE + " " + messageId);
		// Get and interpret results.
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 220:
				String text;
				try {
					text = NnrpdUtils.readText(din);
				} catch(IOException e) {
					throw new NewsDbException("problem fetching article: " + e);
				}
				return new NewsDbArticle(dbStamp, text);
			case 430:
				return null;
			default:
				throw new NewsDbException("unexpected response to ARTICLE command: " + Utils.flattenStrarr(resp));
		}
	}
	
	// Remember the most recent names array that matched, so we can do
	// a quick address compare instead of a more expensive content compare.
	private String[] lastOkNames = null;
	
	/// Get specified headers from a range of articles by group and number.
	// Some implementations have special faster methods for getting
	// headers from articles, e.g. an XOVER command to a remote server
	// or a local overview database. If no such fast method is available,
	// this returns null and the caller should fall back on conventional
	// means.
	// @exception NewsDbException if something goes wrong
	public synchronized String[][] getHeaders(String[] names, NewsDbGroup group, int firstArtNum, int lastArtNum) throws NewsDbException {
		// Do we have an XOVER database?
		if(overviewFmt == null)
			return null;
		// Is the names list the same as overviewFmt?
		if(names != lastOkNames) {
			if(!Utils.equalsStrings(names, overviewFmt))
				return null;
			lastOkNames = names;
		}
		// Check that this group is one of ours.
		if(group.getDbStamp() != dbStamp)
			throw new NewsDbException("mismatched database stamps");
		// Do we need to refresh overview data for this group?
		long now = System.currentTimeMillis();
		long time = overviewCache.getLastTime(group);
		if(time == -1 || now - time >= NnrpdUtils.INT_CHECKOVERVIEW) {
			readOverview(group);
			overviewCache.setLastTime(group);
		}
		// Now just get articles from the cache.
		int numArts = lastArtNum - firstArtNum + 1;
		String[][] results = new String[numArts][];
		// Check if we already have cached results for any of these articles.
		for(int i = 0; i < numArts; ++i) {
			int artNum = firstArtNum + i;
			results[i] = overviewCache.getEntry(group, artNum);
		}
		return results;
	}
	
	private void readOverview(NewsDbGroup group) throws NewsDbException {
		// Make sure it's the current group on the remote server.
		if(currentGroup == null || !group.getName().equals(currentGroup.getName()))
			setGroup(group.getName());
		// Do the XOVER command.
		pout.println(NnrpdUtils.CMD_XOVER + " " + group.getFirstArtNum() + "-" + group.getLastArtNum());
		// Get and interpret results.
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 224:
				while(true) {
					String line;
					try {
						line = din.readLine();
					} catch(IOException e) {
						break;
					}
					if(line == null || line.equals("."))
						break;
					int tab = line.indexOf('\t');
					int artNum;
					try {
						artNum = Integer.parseInt(line.substring(0, tab));
					} catch(NumberFormatException e) {
						continue;
					}
					String[] overviewEntry = Utils.splitStr(line.substring(tab + 1), '\t');
					if(overviewEntry.length != overviewFmt.length)
						continue;	// ignore malformed lines
					overviewCache.addEntry(overviewEntry, group, artNum);
				}
				break;
			default:
				throw new NewsDbException("unexpected response to XOVER command: " + Utils.flattenStrarr(resp));
		}
	}
	
	/// Get an enumeration of all the groups.
	// @exception NewsDbException if something goes wrong
	public Enumeration getGroups() throws NewsDbException {
		checkGroups();
		return groupTable.elements();
	}
	
	/// Get an enumeration of all groups created after a given time.
	// @exception NewsDbException if something goes wrong
	public synchronized Enumeration getGroups(long since) throws NewsDbException {
		// Do the NEWGROUPS command.
		pout.println(NnrpdUtils.CMD_NEWGROUPS + " " + NnrpdUtils.rfc977DateTime(since));
		// Get and interpret results.
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 231:
				return readGroups().elements();
			default:
				throw new NewsDbException("unexpected response to NEWGROUPS command: " + Utils.flattenStrarr(resp));
		}
	}
	
	/// Get an enumeration of all groups created after a given time that match
	// a given distributions pattern.
	// @exception NewsDbException if something goes wrong
	public synchronized Enumeration getGroups(long since, String distsPat) throws NewsDbException {
		// Do the NEWGROUPS command.
		pout.println(NnrpdUtils.CMD_NEWGROUPS + " " + NnrpdUtils.rfc977DateTime(since) + " " + distsPat);
		// Get and interpret results.
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 231:
				return readGroups().elements();
			default:
				throw new NewsDbException("unexpected response to NEWGROUPS command: " + Utils.flattenStrarr(resp));
		}
	}
	
	/// Get an enumeration of all message-ids received after a given time
	// in groups matching the given pattern.
	// @exception NewsDbException if something goes wrong
	public Enumeration getMessageIds(String groupsPat, long since) throws NewsDbException {
		// !!! This is purposely not implemented yet, because the NEWNEWS
		// command takes too long and would tie up the connection to the
		// remote server. At some point I'll do an implementation which
		// opens up a second connection just for this one command.
		Vector messageIds = new Vector();
		return messageIds.elements();
	}
	
	/// Get an enumeration of all message-ids received after a given time
	// in groups matching the given pattern, that also match the
	// given distributions pattern.
	// @exception NewsDbException if something goes wrong
	public Enumeration getMessageIds(String groupsPat, long since, String distsPat) throws NewsDbException {
		// !!! This is purposely not implemented yet, because the NEWNEWS
		// command takes too long and would tie up the connection to the
		// remote server. At some point I'll do an implementation which
		// opens up a second connection just for this one command.
		Vector messageIds = new Vector();
		return messageIds.elements();
	}
	
	/// Post an article.
	// @exception NewsDbException if something goes wrong
	public synchronized void post(String artText) throws NewsDbException {
		// Do the POST command.
		pout.println(NnrpdUtils.CMD_POST);
		// Get and interpret results.
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 340:
				pout.print(artText);
				pout.println(".");
				// Get and interpret results.
				resp = readResp();
				respNum = Utils.parseInt(resp[0], -1);
				switch(respNum) {
					case 240:
						// Ok!
						break;
					case 441:
						throw new NewsDbException("post failed: " + Utils.flattenStrarr(resp));
					default:
						throw new NewsDbException("unexpected response to posted article: " + Utils.flattenStrarr(resp));
				}
				break;
			default:
				throw new NewsDbException("unexpected response to POST command: " + Utils.flattenStrarr(resp));
		}
	}
	
	/// Shut down the news database.
	public synchronized void close() {
		pout.println(NnrpdUtils.CMD_QUIT);
		try {
			din.close();
			pout.flush();
			pout.close();
			socket.close();
		} catch(IOException ignore) {
		}
	}
	
	/// Read a response from the server, and return it split into words.
	private synchronized String[] readResp() throws NewsDbException {
		pout.flush();
		String line;
		try {
			line = din.readLine();
		} catch(IOException e) {
			throw new NewsDbException("problem reading remote response: " + e);
		}
		if(line == null || line.length() == 0)
			throw new NewsDbException("problem reading remote response");
		return Utils.splitStr(line);
	}
	
	/// Read the list of groups, if necessary.
	private synchronized void checkGroups() throws NewsDbException {
		checkGroupDescs();	// check group descriptions cache too
		long now = System.currentTimeMillis();
		if(groupTable != null && now - groupsFetched < NnrpdUtils.INT_CHECKGROUPS)
			return;
		// Do the LIST command.
		pout.println(NnrpdUtils.CMD_LIST);
		// Get and interpret results.
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 215:
				groupTable = readGroups();
				groupsFetched = now;
				return;
			default:
				throw new NewsDbException("unexpected response to LIST command: " + Utils.flattenStrarr(resp));
		}
	}
	
	/// Read the list of groups descriptions, if necessary.
	private synchronized void checkGroupDescs() throws NewsDbException {
		long now = System.currentTimeMillis();
		if(groupDescsTable != null && now - groupDescsFetched < NnrpdUtils.INT_CHECKGROUPDESCS)
			return;
		// Do the LIST newsgroups command.
		pout.println(NnrpdUtils.CMD_LIST + " newsgroups");
		// Get and interpret results.
		String[] resp = readResp();
		int respNum = Utils.parseInt(resp[0], -1);
		switch(respNum) {
			case 215:
				groupDescsTable = new Hashtable();
				String line, groupName, groupDesc;
				int ws, nws;
				while(true) {
					try {
						line = din.readLine();
					} catch(IOException e) {
						break;
					}
					if(line == null || line.equals("."))
						break;
					ws = Utils.strCSpan(line, " \t");
					nws = ws + Utils.strSpan(line, " \t", ws);
					groupName = line.substring(0, ws);
					groupDesc = line.substring(nws);
					groupDescsTable.put(groupName, groupDesc);
				}
				groupDescsFetched = now;
				return;
			default:
				throw new NewsDbException("unexpected response to LIST newsgroups command: " + Utils.flattenStrarr(resp));
		}
	}
	
	/// Read a list of groups.
	private synchronized Hashtable readGroups() throws NewsDbException {
		Hashtable gt = new Hashtable();
		String line;
		String[] words;
		String groupName;
		int lastArtNum, firstArtNum;
		char flag;
		NewsDbGroup group;
		while(true) {
			try {
				line = din.readLine();
			} catch(IOException e) {
				break;
			}
			if(line == null || line.equals("."))
				break;
			words = Utils.splitStr(line);
			groupName = words[0];
			try {
				lastArtNum = Integer.parseInt(words[1]);
				firstArtNum = Integer.parseInt(words[2]);
			} catch(NumberFormatException e) {
				continue;
			}
			flag = words[3].charAt(0);
			group = new NewsDbGroup(dbStamp, groupName, lastArtNum - firstArtNum + 1, firstArtNum, lastArtNum, flag, (String) groupDescsTable.get(groupName));
			gt.put(group.getName(), group);
		}
		return gt;
	}
	
}
