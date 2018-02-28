// Nnrpd - multi-threaded proxying NNRP daemon
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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;

import Acme.Fmt;
import Acme.Syslog;
import Acme.SyslogException;
import Acme.TimeKiller;
import Acme.Utils;

/// Multi-threaded proxying NNRP daemon.
// <P>
// This is a replacement for the standard C nnrpd. Sites with lots
// of readers have found that running a separate nnrpd for each reader
// puts a tremendous load on the system. All that memory, all those
// context switches. This solves both problems by having a single
// process service multiple readers simultaneously.
// <P>
// Optionally, you can also use this as an NNRP proxy, connecting to
// a remote nnrpd instead of getting articles from the local disk.
// <P>
// Logging is via syslog, and is compatible with the standard nnrpd logging.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/Nnrpd.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class Nnrpd {
	
	private static final String progName = "Nnrpd";
	
	/// Main routine, if you want to run this directly as an application.
	public static void main(String[] args) {
		// Parse args.
		int port = NnrpdUtils.DEFAULT_PORT;
		int aCacheSize = NnrpdUtils.DEFAULT_ACACHESIZE;
		int maxArtSize = NnrpdUtils.DEFAULT_MAXARTSIZE;
		int oCacheSize = NnrpdUtils.DEFAULT_OCACHESIZE;
		String proxyHost = null;
		boolean debug = false;
		int argc = args.length;
		int argn;
		for(argn = 0; argn < argc && args[argn].charAt(0) == '-'; ++argn) {
			if(args[argn].equals("-p") && argn + 1 < argc) {
				++argn;
				port = Utils.parseInt(args[argn], -1);
				if(port == -1)
					usage();
			} else if(args[argn].equals("-a") && argn + 1 < argc) {
				++argn;
				aCacheSize = Utils.parseInt(args[argn], -1);
				if(aCacheSize == -1)
					usage();
			} else if(args[argn].equals("-o") && argn + 1 < argc) {
				++argn;
				oCacheSize = Utils.parseInt(args[argn], -1);
				if(oCacheSize == -1)
					usage();
			} else if(args[argn].equals("-h") && argn + 1 < argc) {
				++argn;
				proxyHost = args[argn];
			} else if(args[argn].equals("-d"))
				debug = true;
			else
				usage();
		}
		if(argn != argc)
			usage();
		
		try {
			// Create the server.
			Nnrpd nnrpd = new Nnrpd(port, aCacheSize, maxArtSize, oCacheSize, proxyHost, debug);
			
			// And run.
			nnrpd.serve();
		} catch(NnrpdException e) {
			System.err.println(e);
			System.exit(1);
		}
		
		System.exit(0);
	}
	
	private static void usage() {
		System.err.println("usage:  " + progName + " [-p port] [-a art-cachesize] [-o ov-cachesize] [-h proxyhost]");
		System.err.println("");
		System.err.println("  port:");
		System.err.println("    Normally netnews servers run on port " + NnrpdUtils.DEFAULT_PORT + ", which is where the");
		System.err.println("    readers look for them.  You can run this on a different port if");
		System.err.println("    you like, perhaps for debugging.");
		System.err.println("  art-cachesize:");
		System.err.println("    How many bytes of articles to cache.  Default is " + NnrpdUtils.DEFAULT_ACACHESIZE + ".");
		System.err.println("  ov-cachesize:");
		System.err.println("    How many overview entries to cache.  Default is " + NnrpdUtils.DEFAULT_OCACHESIZE + ".");
		System.err.println("  proxyhost:");
		System.err.println("    The remote news server to connect to, for proxy mode.  Syntax");
		System.err.println("    is a hostname, or hostname:port for ports other than " + NnrpdUtils.DEFAULT_PORT + ".");
		System.err.println("");
		System.err.println("For more information see the web page,");
		System.err.println(NnrpdUtils.serverUrl);
		System.exit(1);
	}
	
	private int port;
	private int aCacheSize;
	private int maxArtSize;
	private int oCacheSize;
	private String proxyHost;
	private boolean debug;
	
	private Syslog syslog;
	private NewsDb newsDb;
	private ArticleCache articleCache;
	private boolean newsDbPostingOk;
	
	/// Constructor.
	// @param port Normally netnews servers run on port 119, which is where the
	/// readers look for them. You can run this on a different port if you like,
	/// perhaps for debugging.
	// @param aCacheSize How many bytes of articles to cache.
	// @param maxArtSize Articles larger than this do not get cached at all.
	// @param oCacheSize How many overview entries to cache.
	// @param proxyHost The remote news server to connect to, for proxy mode.
	/// Syntax is a hostname, or hostname:port for ports other than 119.
	// @param debug Whether to emit debugging syslogs or just dump them.
	// @exception NnrpdException if something goes wrong
	public Nnrpd(int port, int aCacheSize, int maxArtSize, int oCacheSize, String proxyHost, boolean debug) throws NnrpdException {
		this.port = port;
		this.aCacheSize = aCacheSize;
		this.maxArtSize = maxArtSize;
		this.oCacheSize = oCacheSize;
		this.proxyHost = proxyHost;
		this.debug = debug;
		
		try {
			syslog = new Syslog(progName, 0, Syslog.LOG_NEWS);
		} catch(SyslogException e) {
			throw new NnrpdException("can't initialize syslog: " + e);
		}
		
		articleCache = new ArticleCache(aCacheSize, maxArtSize);
		
		try {
			if(proxyHost != null)
				newsDb = new ProxyNewsDb(proxyHost, oCacheSize);
			else
				newsDb = new LocalNewsDb(oCacheSize);
			newsDbPostingOk = newsDb.getPostingOk();
		} catch(NewsDbException e) {
			throw new NnrpdException("problem initializing news db: " + e);
		}
	}
	
	/// Run the server. Returns only on errors.
	// @exception NnrpdException if something goes wrong
	public void serve() throws NnrpdException {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(port, 1000);
		} catch(IOException e) {
			error("problem getting server socket: " + e);
			return;
		}
		
		try {
			while(true) {
				try {
					Socket socket = serverSocket.accept();
					new NnrpdSession(this, newsDb, newsDbPostingOk, articleCache, socket);
				} catch(IOException e) {
					if(!e.getMessage().endsWith("Interrupted system call"))
						throw e;
				}
			}
		} catch(IOException e) {
			error("problem doing accept: " + e);
		} finally {
			try {
				serverSocket.close();
			} catch(IOException ignore) {
			}
		}
	}
	
	void debug(String message) throws NnrpdException {
		if(debug)
			log(Syslog.LOG_DEBUG, message);
	}
	
	void info(String message) throws NnrpdException {
		log(Syslog.LOG_INFO, message);
	}
	
	void notice(String message) throws NnrpdException {
		log(Syslog.LOG_NOTICE, message);
	}
	
	void warning(String message) throws NnrpdException {
		log(Syslog.LOG_WARNING, message);
	}
	
	void error(String message) throws NnrpdException {
		log(Syslog.LOG_ERR, message);
	}
	
	void crit(String message) throws NnrpdException {
		log(Syslog.LOG_CRIT, message);
	}
	
	void alert(String message) throws NnrpdException {
		log(Syslog.LOG_ALERT, message);
	}
	
	void emerg(String message) throws NnrpdException {
		log(Syslog.LOG_EMERG, message);
	}
	
	private void log(int priority, String message) throws NnrpdException {
		try {
			syslog.syslog(priority, message);
		} catch(SyslogException e) {
			throw new NnrpdException("syslog problem: " + e);
		}
	}
	
}

class NnrpdSession implements Runnable {
	
	/// The overview.fmt we implement. It seems that some netnews user agents
	// just assume this format, rather than asking the backend what format
	// it provides. Therefore we hardcode the format.
	private static String[] overviewFmt = { "Subject", "From", "Date", "Message-ID", "References", "Bytes", "Lines" };
	
	private Nnrpd nnrpd;
	private NewsDb newsDb;
	private boolean newsDbPostingOk;
	private ArticleCache articleCache;
	private Socket socket;
	private String clientHost;
	
	/// Constructor.
	public NnrpdSession(Nnrpd nnrpd, NewsDb newsDb, boolean newsDbPostingOk, ArticleCache articleCache, Socket socket) {
		// Save arguments.
		this.nnrpd = nnrpd;
		this.newsDb = newsDb;
		this.newsDbPostingOk = newsDbPostingOk;
		this.articleCache = articleCache;
		this.socket = socket;
		
		// Start a thread.
		Thread thread = new Thread(this);
		thread.start();
	}
	
	private DataInputStream din;
	private PrintStream pout;
	
	public void run() {
		try {
			clientHost = socket.getInetAddress().getHostName();
			notice(clientHost + " connect");
			try {
				// Get the streams.
				din = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				pout = new PrintStream(new CrLfOutputStream(new BufferedOutputStream(socket.getOutputStream())));
			} catch(IOException e) {
				warning(clientHost + " problem getting streams: " + e);
				return;
			}
			
			handleSession();
			
			try {
				din.close();
				pout.flush();
				pout.close();
				socket.close();
			} catch(IOException ignore) {
			}
		} catch(NnrpdException e) {
			System.err.println("uncaught exception: " + e);
		}
	}
	
	/// Read and handle requests until done.
	private void handleSession() throws NnrpdException {
		if(newsDbPostingOk)
			response("200 " + NnrpdUtils.serverName + " " + NnrpdUtils.serverVersion + " ready - posting allowed");
		else
			response("201 " + NnrpdUtils.serverName + " " + NnrpdUtils.serverVersion + " ready - no posting allowed");
		
		TimeKiller tk = new TimeKiller(NnrpdUtils.INT_IDLETIMEOUT);
		while(true) {
			// Flush previous output.
			pout.flush();
			// Reset idle timer.
			tk.reset();
			// Read a command line.
			String commandLine;
			try {
				commandLine = din.readLine();
			} catch(IOException e) {
				warning(clientHost + " problem reading command: " + e);
				break;
			}
			debug(clientHost + " < " + commandLine);
			try {
				if(commandLine == null) {
					// End of file - treat as a QUIT.
					cmdQuit(null);
					break;
				}
				
				// Split up the command word and the rest of the line.
				int ws = Utils.strCSpan(commandLine, " \t");
				String command = commandLine.substring(0, ws);
				String[] args = Utils.splitStr(commandLine.substring(ws).trim());
				
				// Dispatch the command to the right subroutine.
				if(command.equalsIgnoreCase(NnrpdUtils.CMD_ARTICLE))
					cmdArticle(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_AUTHINFO))
					cmdAuthinfo(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_BODY))
					cmdBody(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_DATE))
					cmdDate(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_GROUP))
					cmdGroup(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_HEAD))
					cmdHead(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_HELP))
					cmdHelp(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_LAST))
					cmdLast(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_LIST))
					cmdList(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_LISTGROUP))
					cmdListgroup(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_MODE) || command.equalsIgnoreCase(NnrpdUtils.CMD_XMODE))
					cmdMode(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_NEWGROUPS))
					cmdNewgroups(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_NEWNEWS))
					cmdNewnews(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_NEXT))
					cmdNext(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_POST))
					cmdPost(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_QUIT)) {
					cmdQuit(args);
					break;
				} else if(command.equalsIgnoreCase(NnrpdUtils.CMD_SLAVE))
					cmdSlave(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_STAT))
					cmdStat(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_XGTITLE))
					cmdXgtitle(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_XHDR))
					cmdXhdr(args);
				else if(command.equalsIgnoreCase(NnrpdUtils.CMD_XOVER))
					cmdXover(args);
				else {
					notice(clientHost + " unrecognized " + commandLine);
					response(NnrpdUtils.RES_BADCOMMAND);
				}
			} catch(NewsDbException e) {
				warning(clientHost + " " + e);
				response(NnrpdUtils.RES_PROGFAULT + ": " + e);
			} catch(NnrpdException e) {
				warning(clientHost + " " + e);
				response(NnrpdUtils.RES_PROGFAULT + ": " + e);
			}
		}
		tk.done();
	}
	
	// State variables.
	private NewsDbGroup currentGroup = null;
	private int currentArtNum = -1;
	
	// Stats variables.
	private long startTime = System.currentTimeMillis();
	private int groupCount = 0;
	private int artCount = 0;
	private int postsOk = 0;
	private int postsBad = 0;
	private int groupArts;
	
	// Command routines.
	
	private void cmdArticle(String[] args) throws NewsDbException, NnrpdException {
		cmdAhbs(args, 'a');
	}
	
	private String user = null;
	private String pass = null;
	
	private void cmdAuthinfo(String[] args) throws NewsDbException, NnrpdException {
		if(args.length != 2) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		if(args[0].equalsIgnoreCase("user")) {
			user = args[1];
			response("381 AUTHINFO PASS required");
		} else if(args[0].equalsIgnoreCase("pass")) {
			if(user == null)
				response("482 AUTHINFO USER required");
			else {
				if(newsDb.authorize(user, pass))
					response("281 authentication ok");
				else
					response("502 authentication failed");
			}
		} else
			response(NnrpdUtils.RES_SYNTAXERROR);
	}
	
	private void cmdBody(String[] args) throws NewsDbException, NnrpdException {
		cmdAhbs(args, 'b');
	}
	
	private void cmdDate(String[] args) throws NewsDbException, NnrpdException {
		if(args.length != 0) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		Date date = new Date();
		String gmtStr = date.toGMTString();
		String[] strArr = Utils.splitStr(gmtStr);
		int day = Integer.parseInt(strArr[0]);
		String months = "JanFebMarAprMayJunJulAugSepOctNovDec";
		int month = months.indexOf(strArr[1]) / 3 + 1;
		int year = Integer.parseInt(strArr[2]);
		int hour = Integer.parseInt(strArr[3].substring(0, 2));
		int minute = Integer.parseInt(strArr[3].substring(3, 5));
		int second = Integer.parseInt(strArr[3].substring(6, 8));
		response("111 " + Fmt.fmt(year, 4, Fmt.ZF) + Fmt.fmt(month, 2, Fmt.ZF) + Fmt.fmt(day, 2, Fmt.ZF) + Fmt.fmt(hour, 2, Fmt.ZF) + Fmt.fmt(minute, 2, Fmt.ZF) + Fmt.fmt(second, 2, Fmt.ZF));
	}
	
	private void cmdGroup(String[] args) throws NewsDbException, NnrpdException {
		if(args.length != 1) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		NewsDbGroup group = newsDb.getGroup(args[0]);
		if(group == null)
			response(NnrpdUtils.RES_NOSUCHGROUP);
		else {
			groupLog();
			currentGroup = group;
			currentArtNum = currentGroup.getFirstArtNum();
			response("211 " + currentGroup.getNumArts() + " " + currentArtNum + " " + currentGroup.getLastArtNum() + " " + currentGroup.getName());
		}
	}
	
	private void cmdHead(String[] args) throws NewsDbException, NnrpdException {
		cmdAhbs(args, 'h');
	}
	
	private void cmdHelp(String[] args) throws NewsDbException, NnrpdException {
		response("100 help text follows");
		pout.println("ARTICLE [<messageid>|number]");
		pout.println("AUTHINFO user name|pass password");
		pout.println("BODY [<messageid>|number]");
		pout.println("DATE");
		pout.println("GROUP newsgroup");
		pout.println("HEAD [<messageid>|number]");
		pout.println("HELP");
		pout.println("LAST");
		pout.println("LIST [active|newsgroups|distributions|schema] [group_pattern]");
		pout.println("LISTGROUP newsgroup");
		pout.println("MODE reader");
		pout.println("NEWGROUPS yymmdd hhmmss [GMT] [distributions]");
		pout.println("NEWNEWS newsgroups yymmdd hhmmss [GMT] [distributions]");
		pout.println("NEXT");
		pout.println("POST");
		pout.println("QUIT");
		pout.println("SLAVE");
		pout.println("STAT [<messageid>|number]");
		pout.println("XGTITLE [group_pattern]");
		pout.println("XHDR header [range|<messageid>]");
		pout.println("XOVER [range]");
		pout.println(".");
	}
	
	private void cmdLast(String[] args) throws NewsDbException, NnrpdException {
		if(args.length != 0) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		if(currentGroup == null)
			response(NnrpdUtils.RES_NOCURRGROUP);
		else if(currentArtNum == -1)
			response(NnrpdUtils.RES_NOCURRARTICLE);
		else {
			int artNum = currentArtNum;
			while(--artNum >= currentGroup.getFirstArtNum()) {
				NewsDbArticle article = getArticle(currentGroup, artNum);
				if(article == null)
					continue;
				currentArtNum = artNum;
				sendAhbs(article, currentArtNum, 's');
				return;
			}
			response("422 no previous article in this group");
		}
	}
	
	private void cmdList(String[] args) throws NewsDbException, NnrpdException {
		if(args.length > 2) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		if(args.length == 0 || args[0].equalsIgnoreCase("active")) {
			response("215 list of newsgroups follows");
			Enumeration en = newsDb.getGroups();
			while(en.hasMoreElements()) {
				NewsDbGroup group = (NewsDbGroup) en.nextElement();
				if(args.length == 2 && !Utils.match(args[1], group.getName()))
					continue;
				sendGroup(group);
			}
			pout.println(".");
		} else if(args[0].equalsIgnoreCase("active.times")) {
			response(NnrpdUtils.RES_UNIMPLEMENTED);
		} else if(args[0].equalsIgnoreCase("newsgroups")) {
			response("215 list of descriptions follows");
			Enumeration en = newsDb.getGroups();
			while(en.hasMoreElements()) {
				NewsDbGroup group = (NewsDbGroup) en.nextElement();
				String groupName = group.getName();
				if(args.length == 2 && !Utils.match(args[1], groupName))
					continue;
				String description = group.getDescription();
				if(description != null)
					pout.println(groupName + "\t" + group.getDescription());
			}
			pout.println(".");
		} else if(args[0].equalsIgnoreCase("distributions")) {
			response(NnrpdUtils.RES_UNIMPLEMENTED);
		} else if(args[0].equalsIgnoreCase("distrib.pats")) {
			response(NnrpdUtils.RES_UNIMPLEMENTED);
		} else if(args[0].equalsIgnoreCase("schema") || args[0].equalsIgnoreCase("overview.fmt")) {
			response("215 Order of fields in overview database");
			for(int i = 0; i < overviewFmt.length; ++i)
				pout.println(overviewFmt[i] + ":");
			pout.println(".");
		} else
			response(NnrpdUtils.RES_SYNTAXERROR);
	}
	
	private void cmdListgroup(String[] args) throws NewsDbException, NnrpdException {
		if(args.length != 1) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		NewsDbGroup group = newsDb.getGroup(args[0]);
		if(group == null)
			response(NnrpdUtils.RES_NOSUCHGROUP);
		else {
			groupLog();
			currentGroup = group;
			currentArtNum = currentGroup.getFirstArtNum();
			response("211 article list follows");
			// We're going to do the cheap thing here and just count from
			// the first article to the last article.
			for(int artNum = currentGroup.getFirstArtNum(); artNum <= currentGroup.getLastArtNum(); ++artNum)
				pout.println(artNum);
			pout.println(".");
		}
	}
	
	private void cmdMode(String[] args) throws NewsDbException, NnrpdException {
		if(args.length != 1) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		if(args[0].equalsIgnoreCase("reader"))
			response("200 reading mode acknowledged");
		else
			response(NnrpdUtils.RES_SYNTAXERROR);
	}
	
	private void cmdNewgroups(String[] args) throws NewsDbException, NnrpdException {
		if(args.length < 2 || args.length > 4) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		String date = args[0];
		String time = args[1];
		boolean gmt = false;
		String distsPat = null;
		if(args.length >= 3)
			if(args[2].equalsIgnoreCase("GMT")) {
				gmt = true;
				if(args.length == 4)
					distsPat = args[3];
			} else {
				if(args.length == 4) {
					response(NnrpdUtils.RES_SYNTAXERROR);
					return;
				}
				distsPat = args[4];
			}
		long since = NnrpdUtils.rfc977DateTime(date, time, gmt, -1);
		if(since == -1) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		response("231 list of new newsgroups follows");
		Enumeration en;
		if(distsPat == null)
			en = newsDb.getGroups(since);
		else
			en = newsDb.getGroups(since, distsPat);
		while(en.hasMoreElements()) {
			NewsDbGroup group = (NewsDbGroup) en.nextElement();
			sendGroup(group);
		}
		pout.println(".");
	}
	
	private void cmdNewnews(String[] args) throws NewsDbException, NnrpdException {
		if(args.length < 3 || args.length > 5) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		String groupsPat = args[0];
		String date = args[1];
		String time = args[2];
		boolean gmt = false;
		String distsPat = null;
		if(args.length >= 4)
			if(args[3].equalsIgnoreCase("GMT")) {
				gmt = true;
				if(args.length == 5)
					distsPat = args[4];
			} else {
				if(args.length == 5) {
					response(NnrpdUtils.RES_SYNTAXERROR);
					return;
				}
				distsPat = args[5];
			}
		long since = NnrpdUtils.rfc977DateTime(date, time, gmt, -1);
		if(since == -1) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		notice(clientHost + " newnews " + groupsPat + " " + date + " " + time + " " + (gmt ? "GMT" : "local") + (distsPat != null ? distsPat : "none"));
		response("230 list of new articles by message-id follows");
		Enumeration en;
		if(distsPat == null)
			en = newsDb.getMessageIds(groupsPat, since);
		else
			en = newsDb.getMessageIds(groupsPat, since, distsPat);
		while(en.hasMoreElements()) {
			String messageId = (String) en.nextElement();
			pout.println(messageId);
		}
		pout.println(".");
	}
	
	private void cmdNext(String[] args) throws NewsDbException, NnrpdException {
		if(args.length != 0) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		if(currentGroup == null)
			response(NnrpdUtils.RES_NOCURRGROUP);
		else if(currentArtNum == -1)
			response(NnrpdUtils.RES_NOCURRARTICLE);
		else {
			int artNum = currentArtNum;
			while(++artNum <= currentGroup.getLastArtNum()) {
				NewsDbArticle article = getArticle(currentGroup, artNum);
				if(article == null)
					continue;
				currentArtNum = artNum;
				sendAhbs(article, currentArtNum, 's');
				return;
			}
			response("421 no next article in this group");
		}
	}
	
	private void cmdPost(String[] args) throws NewsDbException, NnrpdException {
		if(args.length != 0) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		if(!newsDb.getPostingOk()) {
			notice(clientHost + " noperm post without permission");
			response("440 posting not allowed");
			return;
		}
		response("340 send article to be posted. End with <CR-LF>.<CR-LF>");
		pout.flush();
		String text;
		try {
			text = NnrpdUtils.readText(din);
		} catch(IOException e) {
			warning(clientHost + "problem reading post: " + e);
			++postsBad;
			response("441 posting failed: " + e);
			return;
		}
		try {
			newsDb.post(text);
			notice(clientHost + " post ok");
			++postsOk;
			response("240 article posted ok");
		} catch(NewsDbException e) {
			notice(clientHost + " post failed: " + e);
			++postsBad;
			response("441 posting failed: " + e);
		}
	}
	
	private void cmdSlave(String[] args) throws NewsDbException, NnrpdException {
		if(args.length != 0) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		response("202 slave status noted");
	}
	
	private void cmdQuit(String[] args) throws NewsDbException, NnrpdException {
		sessionLog();
		response("205 closing connection - goodbye!");
	}
	
	private void cmdStat(String[] args) throws NewsDbException, NnrpdException {
		cmdAhbs(args, 's');
	}
	
	private void cmdXgtitle(String[] args) throws NewsDbException, NnrpdException {
		if(args.length > 1) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		String groupsPat;
		if(args.length == 1)
			groupsPat = args[0];
		else {
			if(currentGroup == null) {
				response(NnrpdUtils.RES_NOCURRGROUP);
				return;
			}
			groupsPat = currentGroup.getName();
		}
		response("282 list follows");
		Enumeration en = newsDb.getGroups();
		while(en.hasMoreElements()) {
			NewsDbGroup group = (NewsDbGroup) en.nextElement();
			String groupName = group.getName();
			if(Utils.match(groupsPat, groupName)) {
				String description = group.getDescription();
				if(description != null)
					pout.println(groupName + "\t" + group.getDescription());
			}
		}
		pout.println(".");
	}
	
	private void cmdXhdr(String[] args) throws NewsDbException, NnrpdException {
		if(args.length < 1 || args.length > 2) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		String header = args[0];
		if(args.length == 1) {
			// Use current article.
			if(currentGroup == null)
				response(NnrpdUtils.RES_NOCURRGROUP);
			else if(currentArtNum == -1)
				response(NnrpdUtils.RES_NOCURRARTICLE);
			else
				sendXhdrRange(currentArtNum, currentArtNum, header);
		} else if(args[1].charAt(0) == '<') {
			// By message-id.
			String messageId = args[1];
			NewsDbArticle article = getArticle(messageId);
			if(article == null)
				response(NnrpdUtils.RES_NOSUCHARTICLE);
			else {
				response("221 " + header + " header of article " + messageId);
				sendXhdrLine(article, header, messageId);
				pout.println(".");
			}
		} else {
			int[] range = parseRange(args[1]);
			if(range == null)
				response(NnrpdUtils.RES_SYNTAXERROR);
			else
				sendXhdrRange(range[0], range[1], header);
		}
	}
	
	private void sendXhdrRange(int firstArtNum, int lastArtNum, String header) throws NewsDbException, NnrpdException {
		response("221 " + header + " fields follow");
		for(int artNum = firstArtNum; artNum <= lastArtNum; ++artNum) {
			NewsDbArticle article = getArticle(currentGroup, artNum);
			if(article != null)
				sendXhdrLine(article, header, Integer.toString(artNum));
		}
		pout.println(".");
	}
	
	private void sendXhdrLine(NewsDbArticle article, String header, String ident) {
		String val = article.getHeader(header);
		if(val == null)
			pout.println(ident + " (none)");
		else
			pout.println(ident + " " + val);
	}
	
	private void cmdXover(String[] args) throws NewsDbException, NnrpdException {
		if(args.length > 1) {
			response(NnrpdUtils.RES_SYNTAXERROR);
			return;
		}
		if(args.length == 0) {
			// Use current article.
			if(currentGroup == null)
				response(NnrpdUtils.RES_NOCURRGROUP);
			else if(currentArtNum == -1)
				response(NnrpdUtils.RES_NOCURRARTICLE);
			else
				sendXoverRange(currentGroup, currentArtNum, currentArtNum);
		} else {
			int[] range = parseRange(args[0]);
			if(range == null)
				response(NnrpdUtils.RES_SYNTAXERROR);
			else
				sendXoverRange(currentGroup, range[0], range[1]);
		}
	}
	
	private void sendXoverRange(NewsDbGroup group, int firstArtNum, int lastArtNum) throws NewsDbException, NnrpdException {
		int numArts = lastArtNum - firstArtNum + 1;
		String[][] results = newsDb.getHeaders(overviewFmt, group, firstArtNum, lastArtNum);
		if(results == null) {
			// Fall back on conventional weapons.
			results = new String[numArts][overviewFmt.length];
			for(int i = 0; i < numArts; ++i) {
				int artNum = firstArtNum + i;
				NewsDbArticle article = getArticle(group, artNum);
				if(article != null)
					for(int j = 0; j < overviewFmt.length; ++j)
						results[i][j] = article.getHeader(overviewFmt[j]);
			}
		}
		response("224 data follows");
		for(int i = 0; i < numArts; ++i) {
			if(results[i] == null)
				continue;	// non-existent article
			int artNum = firstArtNum + i;
			StringBuffer buf = new StringBuffer();
			buf.append(Integer.toString(artNum));
			for(int j = 0; j < overviewFmt.length; ++j) {
				buf.append('\t');
				buf.append(results[i][j]);
			}
			pout.println(buf.toString());
		}
		pout.println(".");
	}
	
	private void cmdAhbs(String[] args, char which) throws NewsDbException, NnrpdException {
		if(args.length == 0)
			if(currentArtNum == -1)
				response(NnrpdUtils.RES_NOCURRARTICLE);
			else
				cmdAhbsNum(currentArtNum, which);
		else if(args.length > 1)
			response(NnrpdUtils.RES_SYNTAXERROR);
		else if(args[0].charAt(0) == '<')
			cmdAhbsId(args[0], which);
		else {
			int artNum = Utils.parseInt(args[0], -1);
			if(artNum == -1) {
				response(NnrpdUtils.RES_SYNTAXERROR);
				return;
			}
			cmdAhbsNum(artNum, which);
		}
	}
	
	private void cmdAhbsNum(int artNum, char which) throws NewsDbException, NnrpdException {
		if(currentGroup == null)
			response(NnrpdUtils.RES_NOCURRGROUP);
		else {
			NewsDbArticle article = getArticle(currentGroup, artNum);
			if(article == null) {
				response(NnrpdUtils.RES_NOSUCHARTICLENUM);
				return;
			}
			currentArtNum = artNum;
			sendAhbs(article, artNum, which);
		}
	}
	
	private void cmdAhbsId(String messageId, char which) throws NewsDbException, NnrpdException {
		NewsDbArticle article = getArticle(messageId);
		if(article == null) {
			response(NnrpdUtils.RES_NOSUCHARTICLE);
			return;
		}
		sendAhbs(article, -1, which);
	}
	
	private void sendAhbs(NewsDbArticle article, int artNum, char which) throws NewsDbException, NnrpdException {
		artLog();
		response(whichStatus(which) + " " + artNum + " " + article.getHeader("Message-ID") + whichStr(which));
		switch(which) {
			case 'a':
				pout.print(article.getText());
				break;
			case 'h':
				pout.print(article.getText().substring(0, article.getHeadLen()));
				break;
			case 'b':
				pout.print(article.getText().substring(article.getBodyStart()));
				break;
			case 's':
				return;
		}
		pout.println(".");
	}
	
	private static int whichStatus(char which) {
		switch(which) {
			case 'a':
				return 220;
			case 'h':
				return 221;
			case 'b':
				return 222;
			case 's':
				return 223;
		}
		return 500;
	}
	
	private static String whichStr(char which) {
		switch(which) {
			case 'a':
				return " head and body follow";
			case 'h':
				return " head follows";
			case 'b':
				return " body follows";
			case 's':
				return " request text separately";
		}
		return "???";
	}
	
	private void sendGroup(NewsDbGroup group) {
		pout.println(group.getName() + " " + group.getLastArtNum() + " " + group.getFirstArtNum() + " " + group.getFlag());
	}
	
	/// Send a response line.
	private void response(String resp) throws NnrpdException {
		debug(clientHost + " > " + resp);
		pout.println(resp);
	}
	
	private NewsDbArticle getArticle(NewsDbGroup group, int artNum) throws NewsDbException {
		NewsDbArticle article = articleCache.getArticle(group, artNum);
		if(article == null) {
			article = newsDb.getArticle(group, artNum);
			if(article != null)
				articleCache.addArticle(article, group, artNum);
		}
		return article;
	}
	
	private NewsDbArticle getArticle(String messageId) throws NewsDbException {
		NewsDbArticle article = articleCache.getArticle(messageId);
		if(article == null) {
			article = newsDb.getArticle(messageId);
			if(article != null)
				articleCache.addArticle(article, messageId);
		}
		return article;
	}
	
	/// Parse a range spec for the XHDR and XOVER commands.
	private int[] parseRange(String rangeStr) {
		try {
			int[] range = new int[2];
			int dash = rangeStr.indexOf('-');
			if(dash == -1) {
				range[0] = range[1] = Integer.parseInt(rangeStr);
			} else {
				String firstStr = rangeStr.substring(0, dash);
				String lastStr = rangeStr.substring(dash + 1);
				if(firstStr.length() == 0)
					range[0] = currentGroup.getFirstArtNum();
				else
					range[0] = Integer.parseInt(firstStr);
				if(lastStr.length() == 0)
					range[1] = currentGroup.getLastArtNum();
				else
					range[1] = Integer.parseInt(lastStr);
			}
			return range;
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	/// Log an article transfer.
	private void artLog() throws NnrpdException {
		++artCount;
		++groupArts;
	}
	
	/// Log group stats. Call just before you change groups, or exit.
	private void groupLog() throws NnrpdException {
		if(currentGroup != null) {
			notice(clientHost + " group " + currentGroup.getName() + " " + groupArts);
			++groupCount;
		}
		groupArts = 0;
	}
	
	/// Log session stats. Call upon exit.
	private void sessionLog() throws NnrpdException {
		long now = System.currentTimeMillis();
		groupLog();
		notice(clientHost + " exit articles " + artCount + " groups " + groupCount);
		if(postsOk != 0 || postsBad != 0)
			notice(clientHost + " posts received " + postsOk + " rejected " + postsBad);
		notice(clientHost + " elapsed " + (now - startTime) / 1000);
	}
	
	private void debug(String message) throws NnrpdException {
		nnrpd.debug(message);
	}
	
	private void info(String message) throws NnrpdException {
		nnrpd.info(message);
	}
	
	private void notice(String message) throws NnrpdException {
		nnrpd.notice(message);
	}
	
	private void warning(String message) throws NnrpdException {
		nnrpd.warning(message);
	}
	
	private void error(String message) throws NnrpdException {
		nnrpd.error(message);
	}
	
	private void crit(String message) throws NnrpdException {
		nnrpd.crit(message);
	}
	
	private void alert(String message) throws NnrpdException {
		nnrpd.alert(message);
	}
	
	private void emerg(String message) throws NnrpdException {
		nnrpd.emerg(message);
	}
	
}
