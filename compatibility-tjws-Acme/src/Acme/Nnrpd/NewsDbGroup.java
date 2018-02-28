// NewsDbGroup - netnews database group
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

/// Netnews database group.
// <P>
// This represents a news database group.
// <P>
// <A HREF="/resources/classes/Acme/Nnrpd/NewsDbGroup.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class NewsDbGroup {
	
	private long dbStamp;
	private String name;
	private int numArts, firstArtNum, lastArtNum;
	private char flag;
	private String description;
	
	/// Constructor.
	public NewsDbGroup(long dbStamp, String name, int numArts, int firstArtNum, int lastArtNum, char flag, String description) {
		this.dbStamp = dbStamp;
		this.name = name;
		this.numArts = numArts;
		this.firstArtNum = firstArtNum;
		this.lastArtNum = lastArtNum;
		this.flag = flag;
		this.description = description;
	}
	
	/// Get the database stamp.
	// This is used internally when dealing with multiple news databases,
	// to make sure that an obect returned from one is not passed to another.
	protected long getDbStamp() {
		return dbStamp;
	}
	
	/// Get the group name.
	public String getName() {
		return name;
	}
	
	/// Get the estimated number of articles in the group.
	public int getNumArts() {
		return numArts;
	}
	
	/// Get the number of the first article in the group.
	public int getFirstArtNum() {
		return firstArtNum;
	}
	
	/// Get the number of the last article in the group.
	public int getLastArtNum() {
		return lastArtNum;
	}
	
	/// The group type - 'y', 'm', etc.
	public char getFlag() {
		return flag;
	}
	
	/// The group's description.
	public String getDescription() {
		return description;
	}
	
}
