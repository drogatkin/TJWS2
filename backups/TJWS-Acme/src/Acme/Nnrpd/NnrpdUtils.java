// NnrpdUtils - constants and static utilities for NNRP daemon
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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Date;

import Acme.Fmt;
import Acme.Utils;

/// Constants and static utilities for NNRP daemon.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/NnrpdUtils.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class NnrpdUtils {
	
	// Server identification.
	public static final String serverName = "Acme.Nnrpd";
	public static final String serverVersion = "v0.95 of 19dec96";
	public static final String serverUrl = "http://www.acme.com/java/software/Package-Acme.Nnrpd.html";
	
	/// The default port to listen on and connect to.
	public static final int DEFAULT_PORT = 119;
	
	/// The default size of the article cache, in bytes.
	public static final int DEFAULT_ACACHESIZE = 10000000;
	
	/// The default maximum-size article to cache, in bytes.
	public static final int DEFAULT_MAXARTSIZE = 100000;
	
	/// The default size of the overview cache, in entries.
	public static final int DEFAULT_OCACHESIZE = 50000;
	
	/// Idle timeout, in milliseconds.
	public static final long INT_IDLETIMEOUT = Utils.INT_MINUTE * 15;
	
	/// How often to check the list of newsgroups.
	public static final long INT_CHECKGROUPS = Utils.INT_MINUTE * 30;
	
	/// How often to check the list of newsgroup descriptions.
	public static final long INT_CHECKGROUPDESCS = Utils.INT_DAY;
	
	/// How often to check a group's overview data.
	public static final long INT_CHECKOVERVIEW = Utils.INT_DAY;
	
	// NNTP commands.
	
	public static final String CMD_ARTICLE = "ARTICLE";
	public static final String CMD_AUTHINFO = "AUTHINFO";
	public static final String CMD_BODY = "BODY";
	public static final String CMD_DATE = "DATE";
	public static final String CMD_GROUP = "GROUP";
	public static final String CMD_HEAD = "HEAD";
	public static final String CMD_HELP = "HELP";
	public static final String CMD_IHAVE = "IHAVE";
	public static final String CMD_LAST = "LAST";
	public static final String CMD_LIST = "LIST";
	public static final String CMD_LISTGROUP = "LISTGROUP";
	public static final String CMD_MODE = "MODE";
	public static final String CMD_NEWGROUPS = "NEWGROUPS";
	public static final String CMD_NEWNEWS = "NEWNEWS";
	public static final String CMD_NEXT = "NEXT";
	public static final String CMD_POST = "POST";
	public static final String CMD_QUIT = "QUIT";
	public static final String CMD_SLAVE = "SLAVE";
	public static final String CMD_STAT = "STAT";
	public static final String CMD_XGTITLE = "XGTITLE";
	public static final String CMD_XHDR = "XHDR";
	public static final String CMD_XMODE = "XMODE";
	public static final String CMD_XOVER = "XOVER";
	public static final String CMD_XREPLIC = "XREPLIC";
	public static final String CMD_XTHREAD = "XTHREAD";
	
	// Some common NNTP responses.
	
	public static final String RES_NOSUCHGROUP = "411 no such news group";
	public static final String RES_NOCURRGROUP = "412 no current newsgroup has been selected";
	public static final String RES_NOCURRARTICLE = "420 no current article has been selected";
	public static final String RES_NOSUCHARTICLENUM = "423 no such article number in this group";
	public static final String RES_NOSUCHARTICLE = "430 no such article found";
	public static final String RES_BADCOMMAND = "500 command not recognized";
	public static final String RES_UNIMPLEMENTED = "500 command not implemented";
	public static final String RES_SYNTAXERROR = "501 command syntax error";
	public static final String RES_PROGFAULT = "503 program fault - command not performed";
	
	/// Read text terminated by a "." on a line by itself.
	public static String readText(DataInputStream din) throws IOException {
		StringBuffer buf = new StringBuffer();
		while(true) {
			String line = din.readLine();
			if(line == null || line.equals("."))
				break;
			buf.append(line);
			buf.append('\n');
		}
		return buf.toString();
	}
	
	/// Convert from YYMMDD HHMMSS [GMT] format into Java time.
	public static long rfc977DateTime(String dateStr, String timeStr, boolean gmt, long def) {
		if(dateStr.length() != 6 || timeStr.length() != 6)
			return def;
		try {
			// Extract values from the strings.
			int year = Integer.parseInt(dateStr.substring(0, 2));
			int month = Integer.parseInt(dateStr.substring(2, 4));
			int day = Integer.parseInt(dateStr.substring(4, 6));
			int hour = Integer.parseInt(timeStr.substring(0, 2));
			int minute = Integer.parseInt(timeStr.substring(2, 4));
			int second = Integer.parseInt(timeStr.substring(4, 6));
			// Adjust to Date representations.
			if(year < 50)
				year += 100;
			--month;
			// And turn to a long.
			if(gmt)
				return Date.UTC(year, month, day, hour, minute, second);
			else {
				Date date = new Date(year, month, day, hour, minute, second);
				return date.getTime();
			}
		} catch(Exception e) {
			return def;
		}
	}
	
	/// Convert from Java time into YYMMDD MMHHSS GMT format.
	public static String rfc977DateTime(long time) {
		Date date = new Date(time);
		String gmtStr = date.toGMTString();
		String[] strArr = Utils.splitStr(gmtStr);
		int day = Integer.parseInt(strArr[0]);
		String months = "JanFebMarAprMayJunJulAugSepOctNovDec";
		int month = months.indexOf(strArr[1]) / 3 + 1;
		int year = Integer.parseInt(strArr[2]) - 1900;
		if(year >= 100)
			year -= 100;
		int hour = Integer.parseInt(strArr[3].substring(0, 2));
		int minute = Integer.parseInt(strArr[3].substring(3, 5));
		int second = Integer.parseInt(strArr[3].substring(6, 8));
		return Fmt.fmt(year, 2, Fmt.ZF) + Fmt.fmt(month, 2, Fmt.ZF) + Fmt.fmt(day, 2, Fmt.ZF) + " " + Fmt.fmt(hour, 2, Fmt.ZF) + Fmt.fmt(minute, 2, Fmt.ZF) + Fmt.fmt(second, 2, Fmt.ZF) + " GMT";
	}
	
}
