// LocalNewsDb - local netnews database
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@mail.acme.com>. All rights
// reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import Acme.UnixUser;
import Acme.Utils;

/// Local netnews database.
// <P>
// This is a news database that uses the local filesystem.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/LocalNewsDb.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class LocalNewsDb extends NewsDb {
	
	// Typical places to look for local netnews files.
	
	private static final String SPOOLDIR_1 = "/usr/spool/news";
	private static final String SPOOLDIR_2 = "/var/spool/news";
	
	private static final String LIBDIR_1 = "/usr/lib/news";
	private static final String LIBDIR_USER_1 = "usenet";
	private static final String LIBDIR_USER_2 = "news";
	
	private static final String AUXLIBDIR_1 = "/var/news";
	
	/// The directory where articles live.
	private static String SPOOLDIR;
	
	/// The directory with most auxiliary files.
	private static String LIBDIR;
	
	/// The directory with the other auxiliary files.
	private static String AUXLIBDIR;
	
	/// The active file.
	private static File ACTIVE;
	
	/// The history file.
	private static File HISTORY;
	
	/// The active.times file, or null.
	private static File ACTIVE_TIMES;
	
	/// The distrib.pats file, or null.
	private static File DISTRIB_PATS;
	
	/// The distributions file, or null.
	private static File DISTRIBUTIONS;
	
	/// The newsgroups file, or null.
	private static File NEWSGROUPS;
	
	/// The nnrp.access file, or null.
	private static File NNRP_ACCESS;
	
	/// The overview.fmt file, or null.
	private static File OVERVIEW_FMT;
	
	private String[] overviewFmt = null;
	private OverviewCache overviewCache;
	
	// Group cache.
	private Hashtable groupTable = null;
	private long groupsFetched;
	
	// Group descriptions cache.
	private Hashtable groupDescsTable = null;
	private long groupDescsFetched;
	
	/// Constructor.
	// @param oCacheSize how many overview entries to save in the cach
	// @exception NewsDbException if something goes wrong
	public LocalNewsDb(int oCacheSize) throws NewsDbException {
		initializePaths();
		
		// See if there's an overview database.
		if(OVERVIEW_FMT != null) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(OVERVIEW_FMT));
				String[] lines = new String[100];
				int count = 0;
				while(true) {
					String line = br.readLine();
					if(line == null)
						break;
					line = line.trim();
					if(line.startsWith("#"))
						continue;
					if(line.endsWith(":"))
						line = line.substring(0, line.length() - 1);
					lines[count++] = line;
				}
				br.close();
				overviewFmt = new String[count];
				System.arraycopy(lines, 0, overviewFmt, 0, count);
				overviewCache = new OverviewCache(oCacheSize);
			} catch(IOException ignore) {
			}
		}
	}
	
	/// Attempt authorization.
	// @exception NewsDbException if something goes wrong
	public boolean authorize(String user, String password) throws NewsDbException {
		// !!!
		throw new NewsDbException("not implemented yet");
	}
	
	/// Whether posting is allowed.
	// @exception NewsDbException if something goes wrong
	public boolean getPostingOk() throws NewsDbException {
		// !!!
		return true;
	}
	
	/// Get a group by name.
	// @exception NewsDbException if something goes wrong
	public NewsDbGroup getGroup(String groupName) throws NewsDbException {
		// Check group cache.
		checkGroups();
		// And return results directly from the cache. If it's not in
		// there, we don't have any place else to look.
		return (NewsDbGroup) groupTable.get(groupName);
	}
	
	/// Get an article by group and number.
	// @exception NewsDbException if something goes wrong
	public NewsDbArticle getArticle(NewsDbGroup group, int artNum) throws NewsDbException {
		if(group.getDbStamp() != dbStamp)
			throw new NewsDbException("mismatched database stamps");
		return getArticleFromFile(group.getName().replace('.', '/') + "/" + Integer.toString(artNum));
	}
	
	/// Get an article by message-id.
	// @exception NewsDbException if something goes wrong
	public NewsDbArticle getArticle(String messageId) throws NewsDbException {
		// !!!
		throw new NewsDbException("not implemented yet");
	}
	
	/// Get an article by relative filename
	// @exception NewsDbException if something goes wrong
	private NewsDbArticle getArticleFromFile(String filename) throws NewsDbException {
		filename = SPOOLDIR + "/" + filename;
		File file = new File(filename);
		if(!(file.exists() && file.isFile() && file.canRead()))
			return null;
		try {
			InputStream is = new FileInputStream(file);
			byte[] bytes = new byte[(int) file.length()];
			Utils.readFully(is, bytes, 0, bytes.length);
			is.close();
			return new NewsDbArticle(dbStamp, new String(bytes));
		} catch(IOException e) {
			throw new NewsDbException("problem reading article: " + e);
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
	public String[][] getHeaders(String[] names, NewsDbGroup group, int firstArtNum, int lastArtNum) throws NewsDbException {
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
		for(int i = 0; i < numArts; ++i) {
			int artNum = firstArtNum + i;
			results[i] = overviewCache.getEntry(group, artNum);
		}
		return results;
	}
	
	private void readOverview(NewsDbGroup group) throws NewsDbException {
		try {
			BufferedReader br = new BufferedReader(new FileReader(SPOOLDIR + "/" + group.getName().replace('.', '/') + "/.overview"));
			while(true) {
				String line = br.readLine();
				if(line == null)
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
					continue;   // ignore malformed lines
				overviewCache.addEntry(overviewEntry, group, artNum);
			}
			br.close();
		} catch(IOException ignore) {
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
	public Enumeration getGroups(long since) throws NewsDbException {
		// !!!
		throw new NewsDbException("not implemented yet");
	}
	
	/// Get an enumeration of all groups created after a given time that match
	// a given distributions pattern.
	// @exception NewsDbException if something goes wrong
	public Enumeration getGroups(long since, String distsPat) throws NewsDbException {
		// !!!
		throw new NewsDbException("not implemented yet");
	}
	
	/// Get an enumeration of all message-ids received after a given time
	// in groups matching the given pattern.
	// @exception NewsDbException if something goes wrong
	public Enumeration getMessageIds(String groupsPat, long since) throws NewsDbException {
		// !!!
		throw new NewsDbException("not implemented yet");
	}
	
	/// Get an enumeration of all message-ids received after a given time
	// in groups matching the given pattern, that also match the
	// given distributions pattern.
	// @exception NewsDbException if something goes wrong
	public Enumeration getMessageIds(String groupsPat, long since, String distsPat) throws NewsDbException {
		// !!!
		throw new NewsDbException("not implemented yet");
	}
	
	/// Post an article.
	// @exception NewsDbException if something goes wrong
	public void post(String artText) throws NewsDbException {
		// !!!
		throw new NewsDbException("not implemented yet");
	}
	
	/// Shut down the news database.
	public void close() {
		// !!!
	}
	
	/// Initializer for paths.
	// We try to guess where the netnews files are. If we can't
	// find a vital one, we throw an exception.
	// @exception NewsDbException if something goes wrong
	private void initializePaths() throws NewsDbException {
		if((new File(SPOOLDIR_1)).exists())
			SPOOLDIR = SPOOLDIR_1;
		else if((new File(SPOOLDIR_2)).exists())
			SPOOLDIR = SPOOLDIR_2;
		else
			throw new NewsDbException("can't find spooldir");
		
		if(LIBDIR_1 != null && (new File(LIBDIR_1)).exists())
			LIBDIR = LIBDIR_1;
		else {
			try {
				LIBDIR = (new UnixUser(LIBDIR_USER_1)).getHomeDir();
			} catch(IOException e) {
				try {
					LIBDIR = (new UnixUser(LIBDIR_USER_2)).getHomeDir();
				} catch(IOException f) {
					throw new NewsDbException("can't find libdir");
				}
			}
		}
		
		if(AUXLIBDIR_1 != null && (new File(AUXLIBDIR_1)).exists())
			AUXLIBDIR = AUXLIBDIR_1;
		else
			AUXLIBDIR = LIBDIR;
		
		// Required files.
		ACTIVE = new File(LIBDIR + "/" + "active");
		if(!ACTIVE.exists()) {
			ACTIVE = new File(AUXLIBDIR + "/" + "active");
			if(!ACTIVE.exists())
				throw new NewsDbException("can't find active");
		}
		HISTORY = new File(LIBDIR + "/" + "history");
		if(!HISTORY.exists()) {
			HISTORY = new File(AUXLIBDIR + "/" + "history");
			if(!HISTORY.exists())
				throw new NewsDbException("can't find history");
		}
		
		// Optional files.
		NEWSGROUPS = new File(LIBDIR + "/" + "newsgroups");
		if(!NEWSGROUPS.exists())
			NEWSGROUPS = null;
		NNRP_ACCESS = new File(LIBDIR + "/" + "nnrp.access");
		if(!NNRP_ACCESS.exists())
			NNRP_ACCESS = null;
		ACTIVE_TIMES = new File(LIBDIR + "/" + "active.times");
		if(!ACTIVE_TIMES.exists())
			ACTIVE_TIMES = null;
		DISTRIBUTIONS = new File(LIBDIR + "/" + "distributions");
		if(!DISTRIBUTIONS.exists())
			DISTRIBUTIONS = null;
		DISTRIB_PATS = new File(LIBDIR + "/" + "distrib.pats");
		if(!DISTRIB_PATS.exists())
			DISTRIB_PATS = null;
		OVERVIEW_FMT = new File(LIBDIR + "/" + "overview.fmt");
		if(!OVERVIEW_FMT.exists())
			OVERVIEW_FMT = null;
	}
	
	/// Read the list of groups, if necessary.
	private synchronized void checkGroups() throws NewsDbException {
		checkGroupDescs();	// check group descriptions cache too
		long now = System.currentTimeMillis();
		if(groupTable != null && now - groupsFetched < NnrpdUtils.INT_CHECKGROUPS)
			return;
		groupTable = new Hashtable();
		groupsFetched = now;
		String line;
		String[] words;
		String groupName;
		int lastArtNum, firstArtNum;
		char flag;
		NewsDbGroup group;
		try {
			BufferedReader br = new BufferedReader(new FileReader(ACTIVE));
			while(true) {
				line = br.readLine();
				if(line == null)
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
				groupTable.put(group.getName(), group);
			}
			br.close();
		} catch(IOException e) {
			throw new NewsDbException("problem reading groups: " + e);
		}
	}
	
	/// Read the list of group descriptions, if necessary.
	private synchronized void checkGroupDescs() throws NewsDbException {
		long now = System.currentTimeMillis();
		if(groupDescsTable != null && now - groupDescsFetched < NnrpdUtils.INT_CHECKGROUPDESCS)
			return;
		groupDescsFetched = now;
		groupDescsTable = new Hashtable();
		if(NEWSGROUPS == null)
			return;
		String line, groupName, groupDesc;
		int ws, nws;
		try {
			BufferedReader br = new BufferedReader(new FileReader(NEWSGROUPS));
			while(true) {
				line = br.readLine();
				if(line == null)
					break;
				ws = Utils.strCSpan(line, " \t");
				nws = ws + Utils.strSpan(line, " \t", ws);
				groupName = line.substring(0, ws);
				groupDesc = line.substring(nws);
				groupDescsTable.put(groupName, groupDesc);
			}
			br.close();
		} catch(IOException e) {
			throw new NewsDbException("problem reading group descriptions: " + e);
		}
	}
	
}
