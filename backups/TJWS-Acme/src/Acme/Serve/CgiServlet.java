// CgiServlet - runs CGI programs
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

package Acme.Serve;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import Acme.Serve.servlet.ServletException;
import Acme.Serve.servlet.http.HttpServlet;
import Acme.Serve.servlet.http.HttpServletRequest;
import Acme.Serve.servlet.http.HttpServletResponse;

/// Runs CGI programs.
// <P>
// Note: although many implementations of CGI set the working directory of
// the subprocess to be the directory containing the executable file, the
// CGI spec actually says nothing about the working directory. Since
// Java has no method for setting the working directory, this implementation
// does not set it.
// <P>
// <A HREF="/resources/classes/Acme/Serve/CgiServlet.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.Serve

public class CgiServlet extends HttpServlet {
	
	/// Returns a string containing information about the author, version, and
	// copyright of the servlet.
	public String getServletInfo() {
		return "runs CGI programs";
	}
	
	/// Services a single request from the client.
	// @param req the servlet request
	// @param req the servlet response
	// @exception ServletException when an exception has occurred
	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if(!(req.getMethod().equalsIgnoreCase("get") || req.getMethod().equalsIgnoreCase("post"))) {
			res.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
			return;
		}
		
		String path = req.getServletPath();
		if(path == null || path.charAt(0) != '/') {
			res.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		if(path.indexOf("/../") != -1 || path.endsWith("/..")) {
			res.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		// Make a version without the leading /.
		String pathname = path;
		while(pathname.length() > 0 && pathname.charAt(0) == '/')
			pathname = pathname.substring(1);
		if(pathname.length() == 0)
			pathname = "./";
		
		dispatchPathname(req, res, path, pathname);
	}
	
	private void dispatchPathname(HttpServletRequest req, HttpServletResponse res, String path, String pathname) throws IOException {
		String filename = pathname.replace('/', File.separatorChar);
		if(filename.charAt(filename.length() - 1) == File.separatorChar)
			filename = filename.substring(0, filename.length() - 1);
		filename = getServletContext().getRealPath(filename);
		File file = new File(filename);
		if(file.exists())
			serveFile(req, res, path, filename, file);
		else
			res.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	
	private void serveFile(HttpServletRequest req, HttpServletResponse res, String path, String filename, File file) throws IOException {
		String queryString = req.getQueryString();
		int contentLength = req.getContentLength();
		int c;
		
		log("running " + path);
		
		// Make argument list.
		Vector argVec = new Vector();
		argVec.addElement(filename);
		if(queryString != null && queryString.indexOf("=") == -1) {
			Enumeration enumeration = new StringTokenizer(queryString, "+");
			while(enumeration.hasMoreElements())
				argVec.addElement((String) enumeration.nextElement());
		}
		String argList[] = makeList(argVec);
		
		// Make environment list.
		Vector envVec = new Vector();
		envVec.addElement(makeEnv("PATH", "/usr/local/bin:/usr/ucb:/bin:/usr/bin"));
		envVec.addElement(makeEnv("GATEWAY_INTERFACE", "CGI/1.1"));
		envVec.addElement(makeEnv("SERVER_SOFTWARE", getServletContext().getServerInfo()));
		envVec.addElement(makeEnv("SERVER_NAME", req.getServerName()));
		envVec.addElement(makeEnv("SERVER_PORT", Integer.toString(req.getServerPort())));
		envVec.addElement(makeEnv("REMOTE_ADDR", req.getRemoteAddr()));
		envVec.addElement(makeEnv("REMOTE_HOST", req.getRemoteHost()));
		envVec.addElement(makeEnv("REQUEST_METHOD", req.getMethod()));
		if(contentLength != -1)
			envVec.addElement(makeEnv("CONTENT_LENGTH", Integer.toString(contentLength)));
		if(req.getContentType() != null)
			envVec.addElement(makeEnv("CONTENT_TYPE", req.getContentType()));
		envVec.addElement(makeEnv("SCRIPT_NAME", req.getServletPath()));
		if(req.getPathInfo() != null)
			envVec.addElement(makeEnv("PATH_INFO", req.getPathInfo()));
		if(req.getPathTranslated() != null)
			envVec.addElement(makeEnv("PATH_TRANSLATED", req.getPathTranslated()));
		if(queryString != null)
			envVec.addElement(makeEnv("QUERY_STRING", queryString));
		envVec.addElement(makeEnv("SERVER_PROTOCOL", req.getProtocol()));
		if(req.getRemoteUser() != null)
			envVec.addElement(makeEnv("REMOTE_USER", req.getRemoteUser()));
		if(req.getAuthType() != null)
			envVec.addElement(makeEnv("AUTH_TYPE", req.getAuthType()));
		Enumeration enumeration = req.getHeaderNames();
		while(enumeration.hasMoreElements()) {
			String name = (String) enumeration.nextElement();
			String value = req.getHeader(name);
			if(value == null)
				value = "";
			envVec.addElement(makeEnv("HTTP_" + name.toUpperCase().replace('-', '_'), value));
		}
		String envList[] = makeList(envVec);
		
		// Start the command.
		Process proc = Runtime.getRuntime().exec(argList, envList);
		
		try {
			// If it's a POST, copy the request data to the process.
			if(req.getMethod().equalsIgnoreCase("post")) {
				InputStream reqIn = req.getInputStream();
				OutputStream procOut = proc.getOutputStream();
				for(int i = 0; i < contentLength; ++i) {
					c = reqIn.read();
					if(c == -1)
						break;
					procOut.write(c);
				}
				procOut.close();
			}
			
			// Now read the response from the process.
			BufferedReader procIn = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			OutputStream resOut = res.getOutputStream();
			// Some of the headers have to be intercepted and handled.
			boolean firstLine = true;
			while(true) {
				String line = procIn.readLine();
				if(line == null)
					break;
				line = line.trim();
				if(line.equals(""))
					break;
				int colon = line.indexOf(":");
				if(colon == -1) {
					// No colon. If it's the first line, parse it for status.
					if(firstLine) {
						StringTokenizer tok = new StringTokenizer(line, " ");
						try {
							switch(tok.countTokens()) {
								case 2:
									tok.nextToken();
									res.setStatus(Integer.parseInt(tok.nextToken()));
									break;
								case 3:
									tok.nextToken();
									res.setStatus(Integer.parseInt(tok.nextToken()), tok.nextToken());
									break;
							}
						} catch(NumberFormatException ignore) {
						}
					} else {
						// No colon and it's not the first line? Ignore.
					}
				} else {
					// There's a colon. Check for certain special headers.
					String name = line.substring(0, colon);
					String value = line.substring(colon + 1).trim();
					if(name.equalsIgnoreCase("Status")) {
						StringTokenizer tok = new StringTokenizer(value, " ");
						try {
							switch(tok.countTokens()) {
								case 1:
									res.setStatus(Integer.parseInt(tok.nextToken()));
									break;
								case 2:
									res.setStatus(Integer.parseInt(tok.nextToken()), tok.nextToken());
									break;
							}
						} catch(NumberFormatException ignore) {
						}
					} else if(name.equalsIgnoreCase("Content-Type")) {
						res.setContentType(value);
					} else if(name.equalsIgnoreCase("Content-Length")) {
						try {
							res.setContentLength(Integer.parseInt(value));
						} catch(NumberFormatException ignore) {
						}
					} else if(name.equalsIgnoreCase("Location")) {
						res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
						res.setHeader(name, value);
					} else {
						// Not a special header. Just set it.
						res.setHeader(name, value);
					}
				}
			}
			// Copy the rest of the data uninterpreted.
			Acme.Utils.copyStream(procIn, resOut);
			procIn.close();
			resOut.close();
		} catch(IOException e) {
			// res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
			// There's some weird bug in Java, when reading from a Process
			// you get a spurious IOException. We have to ignore it.
		}
	}
	
	private static String makeEnv(String name, String value) {
		return name + "=" + value;
	}
	
	private static String[] makeList(Vector vec) {
		String list[] = new String[vec.size()];
		for(int i = 0; i < vec.size(); ++i)
			list[i] = (String) vec.elementAt(i);
		return list;
	}
	
}
