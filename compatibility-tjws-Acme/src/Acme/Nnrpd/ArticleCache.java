// ArticleCache - a cache for netnews database articles
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

/// A cache for netnews database articles.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/ArticleCache.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class ArticleCache {
	
	private int aCacheSize;
	private int maxArtSize;
	private Hashtable articles = new Hashtable();
	private LruHashtable groupArtNumTable;
	private LruHashtable messageIdTable;
	
	/// Constructor.
	// @param aCacheSize maximum size of the cache
	// @param maxArtSize maximum-size article to cache
	public ArticleCache(int aCacheSize, int maxArtSize) {
		this.aCacheSize = aCacheSize;
		this.maxArtSize = maxArtSize;
		int numArtsEstimate = aCacheSize / 2000;
		groupArtNumTable = new LruHashtable(numArtsEstimate);
		messageIdTable = new LruHashtable(numArtsEstimate);
	}
	
	/// Get an article by group and number.
	public NewsDbArticle getArticle(NewsDbGroup group, int artNum) {
		Integer n = (Integer) groupArtNumTable.get(groupArtNumKey(group, artNum));
		if(n == null)
			return null;
		return (NewsDbArticle) articles.get(n);
	}
	
	/// Get an article by message-id.
	public NewsDbArticle getArticle(String messageId) {
		Integer n = (Integer) messageIdTable.get(messageId);
		if(n == null)
			return null;
		return (NewsDbArticle) articles.get(n);
	}
	
	/// Add an article by group and number.
	public void addArticle(NewsDbArticle article, NewsDbGroup group, int artNum) {
		Integer n = addArticle(article);
		if(n == null)
			return;
		groupArtNumTable.put(groupArtNumKey(group, artNum), n);
	}
	
	/// Add an article by message-id.
	public void addArticle(NewsDbArticle article, String messageId) {
		Integer n = addArticle(article);
		if(n == null)
			return;
		messageIdTable.put(messageId, n);
	}
	
	private int usage = 0;
	private int firstNumber = 0;
	private int lastNumber = 0;
	
	/// Internal routine to add an article to the queue and assign it a
	// unique immutable number for putting into the search tables.
	// This two-layer approach provides weak-referencing, so when the cache
	// gets too large we can throw away articles here without bothering to
	// clean up the references to them in the search tables, which do
	// their own LRU expiry.
	private Integer addArticle(NewsDbArticle article) {
		int len = article.getText().length();
		if(len > maxArtSize)
			return null;
		Integer l = new Integer(lastNumber++);
		articles.put(l, article);
		usage += len;
		while(usage > aCacheSize) {
			Integer f = new Integer(firstNumber++);
			if(article != null) {
				article = (NewsDbArticle) articles.get(f);
				len = article.getText().length();
				usage -= len;
				articles.remove(f);
			}
		}
		return l;
	}
	
	/// Generate a single hashtable key for the (group,artNum) pair.
	private Integer groupArtNumKey(NewsDbGroup group, int artNum) {
		return new Integer(group.getName().hashCode() ^ artNum);
	}
	
}
