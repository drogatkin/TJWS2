// OverviewCache - a cache for netnews overview entries
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

import java.util.Hashtable;

import Acme.LruHashtable;

/// A cache for netnews overview entries.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/OverviewCache.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class OverviewCache {
	
	private Hashtable groupTimeTable = new Hashtable();
	private LruHashtable groupArtNumTable;
	
	/// Constructor.
	// @param cacheSize maximum size of the cache
	public OverviewCache(int cacheSize) {
		groupArtNumTable = new LruHashtable(cacheSize);
	}
	
	/// Get the last time we cached entries for this group.
	public long getLastTime(NewsDbGroup group) {
		Long time = (Long) groupTimeTable.get(group);
		if(time == null)
			return -1;
		return time.longValue();
	}
	
	/// Set the last time we cached entries for this group.
	public void setLastTime(NewsDbGroup group) {
		Long time = new Long(System.currentTimeMillis());
		groupTimeTable.put(group, time);
	}
	
	/// Get an overview entry by group and number.
	public String[] getEntry(NewsDbGroup group, int artNum) {
		return (String[]) groupArtNumTable.get(groupArtNumKey(group, artNum));
	}
	
	/// Add an overview entry by group and number.
	public void addEntry(String[] entry, NewsDbGroup group, int artNum) {
		groupArtNumTable.put(groupArtNumKey(group, artNum), entry);
	}
	
	/// Generate a single hashtable key for the (group,artNum) pair.
	private Integer groupArtNumKey(NewsDbGroup group, int artNum) {
		return new Integer(group.getName().hashCode() ^ artNum);
	}
	
}
