// UnixUser - a Unix user
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

package Acme;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/// A Unix user.
// This just encapsulates access to /etc/passwd, equivalent to
// the Unix getpwnam() and getpwuid() routines.
// <P>
// <A HREF="/resources/classes/Acme/UnixUser.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class UnixUser {
	
	private String userName;
	private String encPasswd;
	private int userId;
	private int groupId;
	private String realName;
	private String homeDir;
	private String loginShell;
	
	private static final String etcPasswd = "/etc/passwd";
	
	/// Constructor by username.
	// @exception IOException if something goes wrong
	public UnixUser(String name) throws IOException {
		BufferedReader br = openFile();
		while(true) {
			readFields(br);
			if(userName.equals(name))
				break;
		}
	}
	
	/// Constructor by userid.
	// @exception IOException if something goes wrong
	public UnixUser(int id) throws IOException {
		BufferedReader br = openFile();
		while(true) {
			readFields(br);
			if(userId == id)
				break;
		}
	}
	
	private BufferedReader openFile() throws IOException {
		return new BufferedReader(new FileReader("/etc/passwd"));
	}
	
	private void readFields(BufferedReader br) throws IOException {
		while(true) {
			String line = br.readLine();
			if(line == null)
				throw new IOException("unknown user: " + userName);
			String[] fields = Utils.splitStr(line, ':');
			if(fields.length != 7)
				continue;	// ignore malformed lines
			userName = fields[0];
			encPasswd = fields[1];
			userId = Integer.parseInt(fields[2]);
			groupId = Integer.parseInt(fields[3]);
			realName = fields[4];
			homeDir = fields[5];
			loginShell = fields[6];
			return;
		}
	}
	
	public String getUserName() {
		return userName;
	}
	
	public String getEncPasswd() {
		return encPasswd;
	}
	
	public int getUserId() {
		return userId;
	}
	
	public int getGroupid() {
		return groupId;
	}
	
	public String getRealName() {
		return realName;
	}
	
	public String getHomeDir() {
		return homeDir;
	}
	
	public String getLoginShell() {
		return loginShell;
	}
	
}
