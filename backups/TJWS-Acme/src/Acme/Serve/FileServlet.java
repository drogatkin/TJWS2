// FileServlet - servlet similar to a standard httpd
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;

import Acme.Serve.servlet.ServletException;
import Acme.Serve.servlet.http.HttpServlet;
import Acme.Serve.servlet.http.HttpServletRequest;
import Acme.Serve.servlet.http.HttpServletResponse;

/// Servlet similar to a standard httpd.
// <P>
// Implements the "GET" and "HEAD" methods for files and directories.
// Handles index.html.
// Redirects directory URLs that lack a trailing /.
// Handles If-Modified-Since and Range.
// <P>
// <A HREF="/resources/classes/Acme/Serve/FileServlet.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.Serve

public class FileServlet extends HttpServlet {
	
	// We keep a single throttle table for all instances of the servlet.
	// Normally there is only one instance; the exception is subclasses.
	static Acme.WildcardDictionary throttleTab = null;
	
	/// Constructor.
	public FileServlet() {
	}
	
	/// Constructor with throttling.
	// @param throttles filename containing throttle settings
	// @see ThrottledOutputStream
	public FileServlet(String throttles) throws IOException {
		this();
		readThrottles(throttles);
	}
	
	private void readThrottles(String throttles) throws IOException {
		Acme.WildcardDictionary newThrottleTab = ThrottledOutputStream.parseThrottleFile(throttles);
		if(throttleTab == null)
			throttleTab = newThrottleTab;
		else {
			// Merge the new one into the old one.
			Enumeration keys = newThrottleTab.keys();
			Enumeration elements = newThrottleTab.elements();
			while(keys.hasMoreElements()) {
				Object key = keys.nextElement();
				Object element = elements.nextElement();
				throttleTab.put(key, element);
			}
		}
	}
	
	/// Returns a string containing information about the author, version, and
	// copyright of the servlet.
	public String getServletInfo() {
		return "servlet similar to a standard httpd";
	}
	
	/// Services a single request from the client.
	// @param req the servlet request
	// @param req the servlet response
	// @exception ServletException when an exception has occurred
	public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		boolean headOnly;
		if(req.getMethod().equalsIgnoreCase("get"))
			headOnly = false;
		else if(req.getMethod().equalsIgnoreCase("head"))
			headOnly = true;
		else {
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
		
		dispatchPathname(req, res, headOnly, path, pathname);
	}
	
	private void dispatchPathname(HttpServletRequest req, HttpServletResponse res, boolean headOnly, String path, String pathname) throws IOException {
		String filename = pathname.replace('/', File.separatorChar);
		if(filename.charAt(filename.length() - 1) == File.separatorChar)
			filename = filename.substring(0, filename.length() - 1);
		filename = getServletContext().getRealPath(filename);
		File file = new File(filename);
		if(file.exists()) {
			if(!file.isDirectory())
				serveFile(req, res, headOnly, path, filename, file);
			else {
				if(pathname.charAt(pathname.length() - 1) != '/')
					redirectDirectory(req, res, path, file);
				else {
					String indexFilename = filename + File.separatorChar + "index.html";
					File indexFile = new File(indexFilename);
					if(indexFile.exists())
						serveFile(req, res, headOnly, path, indexFilename, indexFile);
					else
						serveDirectory(req, res, headOnly, path, filename, file);
				}
			}
		} else {
			if(pathname.endsWith("/index.html"))
				dispatchPathname(req, res, headOnly, path, pathname.substring(0, pathname.length() - 10));
			else if(pathname.equals("index.html"))
				dispatchPathname(req, res, headOnly, path, "./");
			else
				res.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}
	
	private void serveFile(HttpServletRequest req, HttpServletResponse res, boolean headOnly, String path, String filename, File file) throws IOException {
		log("getting " + path);
		if(!file.canRead()) {
			res.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		
		// Handle If-Modified-Since.
		res.setStatus(HttpServletResponse.SC_OK);
		long lastMod = file.lastModified();
		String ifModSinceStr = req.getHeader("If-Modified-Since");
		long ifModSince = -1;
		if(ifModSinceStr != null) {
			int semi = ifModSinceStr.indexOf(';');
			if(semi != -1)
				ifModSinceStr = ifModSinceStr.substring(0, semi);
			try {
				ifModSince = DateFormat.getDateInstance().parse(ifModSinceStr).getTime();
			} catch(Exception ignore) {
			}
		}
		if(ifModSince != -1 && ifModSince >= lastMod) {
			res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			headOnly = true;
		}
		
		String rangeStr = req.getHeader("Range");
		if(rangeStr != null) {
			// !!!
		}
		
		res.setContentType(getServletContext().getMimeType(filename));
		res.setContentLength((int) file.length());
		res.setDateHeader("Last-modified", lastMod);
		OutputStream out = res.getOutputStream();
		if(!headOnly) {
			// Check throttle.
			if(throttleTab != null) {
				ThrottleItem throttleItem = (ThrottleItem) throttleTab.get(path);
				if(throttleItem != null) {
					// !!! Need to account for multiple simultaneous fetches.
					out = new ThrottledOutputStream(out, throttleItem.getMaxBps());
				}
			}
			
			InputStream in = new FileInputStream(file);
			copyStream(in, out);
			in.close();
		}
		out.close();
	}
	
	/// Copy a file from in to out.
	// Sub-classes can override this in order to do filtering of some sort.
	public void copyStream(InputStream in, OutputStream out) throws IOException {
		Acme.Utils.copyStream(in, out);
	}
	
	private void serveDirectory(HttpServletRequest req, HttpServletResponse res, boolean headOnly, String path, String filename, File file) throws IOException {
		log("indexing " + path);
		if(!file.canRead()) {
			res.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		res.setStatus(HttpServletResponse.SC_OK);
		res.setContentType("text/html");
		OutputStream out = res.getOutputStream();
		if(!headOnly) {
			PrintStream p = new PrintStream(new BufferedOutputStream(out));
			p.println("<HTML><HEAD>");
			p.println("<TITLE>Index of " + path + "</TITLE>");
			p.println("</HEAD><BODY BGCOLOR=\"#99cc99\">");
			p.println("<H2>Index of " + path + "</H2>");
			p.println("<PRE>");
			p.println("mode     bytes  last-changed  name");
			p.println("<HR>");
			String[] names = file.list();
			Acme.Utils.sortStrings(names);
			for(int i = 0; i < names.length; ++i) {
				String aFilename = filename + File.separatorChar + names[i];
				File aFile = new File(aFilename);
				String aFileType;
				if(aFile.isDirectory())
					aFileType = "d";
				else if(aFile.isFile())
					aFileType = "-";
				else
					aFileType = "?";
				String aFileRead = (aFile.canRead() ? "r" : "-");
				String aFileWrite = (aFile.canWrite() ? "w" : "-");
				String aFileExe = "-";
				String aFileSize = Acme.Fmt.fmt(aFile.length(), 8);
				String aFileDate = Acme.Utils.lsDateStr(new Date(aFile.lastModified()));
				String aFileDirsuf = (aFile.isDirectory() ? "/" : "");
				String aFileSuf = (aFile.isDirectory() ? "/" : "");
				p.println(aFileType + aFileRead + aFileWrite + aFileExe + "  " + aFileSize + "  " + aFileDate + "  " + "<A HREF=\"" + names[i] + aFileDirsuf + "\">" + names[i] + aFileSuf + "</A>");
			}
			p.println("</PRE>");
			p.println("<HR>");
			ServeUtils.writeAddress(p);
			p.println("</BODY></HTML>");
			p.flush();
		}
		out.close();
	}
	
	private void redirectDirectory(HttpServletRequest req, HttpServletResponse res, String path, File file) throws IOException {
		log("redirecting " + path);
		res.sendRedirect(path + "/");
	}
	
}
