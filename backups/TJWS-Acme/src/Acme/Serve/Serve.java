// Serve - minimal Java HTTP server class
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
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import Acme.Serve.servlet.Servlet;
import Acme.Serve.servlet.ServletConfig;
import Acme.Serve.servlet.ServletContext;
import Acme.Serve.servlet.ServletException;
import Acme.Serve.servlet.ServletInputStream;
import Acme.Serve.servlet.ServletOutputStream;
import Acme.Serve.servlet.http.Cookie;
import Acme.Serve.servlet.http.HttpServlet;
import Acme.Serve.servlet.http.HttpServletRequest;
import Acme.Serve.servlet.http.HttpServletResponse;
import Acme.Serve.servlet.http.HttpSession;

/// Minimal Java HTTP server class.
// <P>
// This class implements a very small embeddable HTTP server.
// It runs Servlets compatible with the API used by JavaSoft's
// <A HREF="http://java.sun.com/products/java-server/">JavaServer</A> server.
// It comes with default Servlets which provide the usual
// httpd services, returning files and directory listings.
// <P>
// This is not in any sense a competitor for JavaServer.
// JavaServer is a full-fledged HTTP server and more.
// Acme.Serve is tiny, about 1500 lines, and provides only the
// functionality necessary to deliver an Applet's .class files
// and then start up a Servlet talking to the Applet.
// They are both written in Java, they are both web servers, and
// they both implement the Servlet API; other than that they couldn't
// be more different.
// <P>
// This is actually the second HTTP server I've written.
// The other one is called
// <A HREF="http://www.acme.com/software/thttpd/">thttpd</A>,
// it's written in C, and is also pretty small although much more
// featureful than this.
// <P>
// Other Java HTTP servers:
// <UL>
// <LI> The above-mentioned <A
/// HREF="http://java.sun.com/products/java-server/">JavaServer</A>.
// <LI> W3C's <A HREF="http://www.w3.org/pub/WWW/Jigsaw/">Jigsaw</A>.
// <LI> David Wilkinson's <A
/// HREF="http://www.netlink.co.uk/users/cascade/http/">Cascade</A>.
// <LI> Yahoo's <A
/// HREF="http://www.yahoo.com/Computers_and_Internet/Software/Internet/World_Wide_Web/Servers/Java/">list
/// of Java web servers</A>.
// </UL>
// <P>
// A <A HREF="http://www.byte.com/art/9706/sec8/art1.htm">June 1997 BYTE
/// magazine article</A> mentioning this server.<BR>
// A <A HREF="http://www.byte.com/art/9712/sec6/art7.htm">December 1997 BYTE
/// magazine article</A> giving it an Editor's Choice Award of Distinction.<BR>
// <A HREF="/resources/classes/Acme/Serve/Serve.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.http.HttpServlet
// @see FileServlet
// @see CgiServlet

public class Serve implements ServletContext {
	
	private static final String progName = "Serve";
	
	/// Main routine, if you want to run this directly as an application.
	public static void main(String[] args) {
		// Parse args.
		int port = 9090;
		String throttles = null;
		int argc = args.length;
		int argn;
		for(argn = 0; argn < argc && args[argn].charAt(0) == '-'; ++argn) {
			if(args[argn].equals("-p") && argn + 1 < argc) {
				++argn;
				port = Integer.parseInt(args[argn]);
			} else if(args[argn].equals("-t") && argn + 1 < argc) {
				++argn;
				throttles = args[argn];
			} else
				usage();
		}
		if(argn != argc)
			usage();
		
		// Create the server.
		Serve serve = new Serve(port);
		
		// Any custom Servlets should be added here.
		serve.addServlet("/SampleServlet", new Acme.Serve.SampleServlet());
		Servlet ts = new Acme.Serve.TestServlet();
		serve.addServlet("/TestServlet", ts);
		serve.addServlet("/TestServlet/*", ts);
		
		// And add the standard Servlets.
		if(throttles == null)
			serve.addDefaultServlets(true);
		else
			try {
				serve.addDefaultServlets(true, throttles);
			} catch(IOException e) {
				System.err.println("Problem reading throttles file: " + e);
				System.exit(1);
			}
		
		// And run.
		serve.serve();
		
		System.exit(0);
	}
	
	private static void usage() {
		System.err.println("usage:  " + progName + " [-p port]");
		System.exit(1);
	}
	
	private int port;
	private PrintStream logStream;
	Acme.WildcardDictionary registry;
	
	/// Constructor.
	public Serve(int port, PrintStream logStream) {
		this.port = port;
		this.logStream = logStream;
		registry = new Acme.WildcardDictionary();
	}
	
	/// Constructor, default log stream.
	public Serve(int port) {
		this(port, System.err);
	}
	
	/// Constructor, default port and log stream.
	// We don't use 80 as the default port because we don't want to
	// encourage people to run a Java web server as root because Java
	// currently has no way of giving up root privs! Instead, the
	// current default port is 9090.
	public Serve() {
		this(9090, System.err);
	}
	
	/// Register a Servlet by class name. Registration consists of a URL
	// pattern, which can contain wildcards, and the class name of the Servlet
	// to launch when a matching URL comes in. Patterns are checked for
	// matches in the order they were added, and only the first match is run.
	public void addServlet(String urlPat, String className) {
		// See if we have already instantiated this one.
		Servlet servlet = (Servlet) servlets.get(className);
		if(servlet != null) {
			addServlet(urlPat, servlet);
			return;
		}
		
		// Check if we're allowed to make one of these.
		SecurityManager security = System.getSecurityManager();
		if(security != null) {
			int i = className.lastIndexOf('.');
			if(i != -1) {
				security.checkPackageAccess(className.substring(0, i));
				security.checkPackageDefinition(className.substring(0, i));
			}
		}
		
		// Make a new one.
		try {
			servlet = (Servlet) Class.forName(className).newInstance();
			addServlet(urlPat, servlet);
			return;
		} catch(ClassNotFoundException e) {
			log("Class not found: " + className);
		} catch(ClassCastException e) {
			log("Class cast problem: " + e.getMessage());
		} catch(InstantiationException e) {
			log("Instantiation problem: " + e.getMessage());
		} catch(IllegalAccessException e) {
			log("Illegal class access: " + e.getMessage());
		} catch(Exception e) {
			log("Unexpected problem creating servlet: " + e);
		}
	}
	
	/// Register a Servlet. Registration consists of a URL pattern,
	// which can contain wildcards, and the Servlet to
	// launch when a matching URL comes in. Patterns are checked for
	// matches in the order they were added, and only the first match is run.
	public void addServlet(String urlPat, Servlet servlet) {
		try {
			servlet.init(new ServeConfig((ServletContext) this));
			registry.put(urlPat, servlet);
			servlets.put(servlet.getClass().getName(), servlet);
		} catch(ServletException e) {
			log("Problem initializing servlet: " + e);
		}
	}
	
	/// Register a standard set of Servlets. These will return
	// files or directory listings, and run CGI programs, much like a
	// standard HTTP server.
	// <P>
	// Because of the pattern checking order, this should be called
	// <B>after</B> you've added any custom Servlets.
	// <P>
	// The current set of default servlet mappings:
	// <UL>
	// <LI> If enabled, *.cgi goes to CgiServlet, and gets run as a CGI program.
	// <LI> * goes to FileServlet, and gets served up as a file or directory.
	// </UL>
	// @param cgi whether to run CGI programs
	public void addDefaultServlets(boolean cgi) {
		if(cgi)
			addServlet("*.cgi", new Acme.Serve.CgiServlet());
		addServlet("*", new Acme.Serve.FileServlet());
	}
	
	/// Register a standard set of Servlets, with throttles.
	// @param cgi whether to run CGI programs
	// @param throttles filename to read FileServlet throttle settings from
	public void addDefaultServlets(boolean cgi, String throttles) throws IOException {
		if(cgi)
			addServlet("*.cgi", new Acme.Serve.CgiServlet());
		addServlet("*", new Acme.Serve.FileServlet(throttles));
	}
	
	/// Run the server. Returns only on errors.
	public void serve() {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(port, 1000);
		} catch(IOException e) {
			log("Server socket: " + e);
			return;
		}
		
		try {
			while(true) {
				Socket socket = serverSocket.accept();
				new ServeConnection(socket, this);
			}
		} catch(IOException e) {
			log("Accept: " + e);
		} finally {
			try {
				serverSocket.close();
				destroyAllServlets();
			} catch(IOException e) {
			}
		}
	}
	
	// Methods from ServletContext.
	
	protected Hashtable servlets = new Hashtable();
	
	/// Gets a servlet by name.
	// @param name the servlet name
	// @return null if the servlet does not exist
	public Servlet getServlet(String name) {
		return (Servlet) servlets.get(name);
	}
	
	/// Enumerates the servlets in this context (server). Only servlets that
	// are accesible will be returned. This enumeration always includes the
	// servlet itself.
	public Enumeration getServlets() {
		return servlets.elements();
	}
	
	/// Enumerates the names of the servlets in this context (server). Only
	// servlets that are accesible will be returned. This enumeration always
	// includes the servlet itself.
	public Enumeration getServletNames() {
		return servlets.keys();
	}
	
	/// Destroys all currently-loaded servlets.
	public void destroyAllServlets() {
		Enumeration en = servlets.elements();
		while(en.hasMoreElements()) {
			Servlet servlet = (Servlet) en.nextElement();
			servlet.destroy();
		}
		servlets.clear();
	}
	
	/// Write information to the servlet log.
	// @param message the message to log
	public void log(String message) {
		Date date = new Date(System.currentTimeMillis());
		logStream.println("[" + date.toString() + "] " + message);
	}
	
	/// Write a stack trace to the servlet log.
	// @param exception where to get the stack trace
	// @param message the message to log
	public void log(Exception exception, String message) {
		// !!!
		log(message);
	}
	
	/// Applies alias rules to the specified virtual path and returns the
	// corresponding real path. It returns null if the translation
	// cannot be performed.
	// @param path the path to be translated
	public String getRealPath(String path) {
		// No mapping.
		return path;
	}
	
	/// Returns the MIME type of the specified file.
	// @param file file name whose MIME type is required
	public String getMimeType(String file) {
		int lastDot = file.lastIndexOf('.');
		int lastSep = file.lastIndexOf(File.separatorChar);
		if(lastDot == -1 || (lastSep != -1 && lastDot < lastSep))
			return "text/plain; charset=iso-8859-1";
		String extension = file.substring(lastDot + 1);
		if(extension.equals("html") || extension.equals("htm"))
			return "text/html; charset=iso-8859-1";
		if(extension.equals("gif"))
			return "image/gif";
		if(extension.equals("jpg") || extension.equals("jpeg"))
			return "image/jpeg";
		if(extension.equals("au"))
			return "audio/basic";
		if(extension.equals("ra") || extension.equals("ram"))
			return "audio/x-pn-realaudio";
		if(extension.equals("wav"))
			return "audio/wav";
		if(extension.equals("mpg") || extension.equals("mpeg"))
			return "video/mpeg";
		if(extension.equals("qt") || extension.equals("mov"))
			return "video/quicktime";
		if(extension.equals("class"))
			return "application/octet-stream";
		if(extension.equals("ps"))
			return "application/postscript";
		if(extension.equals("wrl"))
			return "x-world/x-vrml";
		if(extension.equals("pac"))
			return "application/x-ns-proxy-autoconfig";
		return "text/plain; charset=iso-8859-1";
	}
	
	/// Returns the name and version of the web server under which the servlet
	// is running.
	// Same as the CGI variable SERVER_SOFTWARE.
	public String getServerInfo() {
		return ServeUtils.serverName + " " + ServeUtils.serverVersion + " (" + ServeUtils.serverUrl + ")";
	}
	
	/// Returns the value of the named attribute of the network service, or
	// null if the attribute does not exist. This method allows access to
	// additional information about the service, not already provided by
	// the other methods in this interface.
	public Object getAttribute(String name) {
		// This server does not support attributes.
		return null;
	}
	
}

class ServeConfig implements ServletConfig {
	
	private ServletContext context;
	
	public ServeConfig(ServletContext context) {
		this.context = context;
	}
	
	// Methods from ServletConfig.
	
	/// Returns the context for the servlet.
	public ServletContext getServletContext() {
		return context;
	}
	
	/// Gets an initialization parameter of the servlet.
	// @param name the parameter name
	public String getInitParameter(String name) {
		// This server doesn't support servlet init params.
		return null;
	}
	
	/// Gets the names of the initialization parameters of the servlet.
	// @param name the parameter name
	public Enumeration getInitParameterNames() {
		// This server doesn't support servlet init params.
		return new Vector().elements();
	}
	
}

class ServeConnection implements Runnable, HttpServletRequest, HttpServletResponse {
	
	private Socket socket;
	private Serve serve;
	
	private ServletInputStream in;
	private ServletOutputStream out;
	
	private Vector cookies = new Vector(); // !!!
	
	/// Constructor.
	public ServeConnection(Socket socket, Serve serve) {
		// Save arguments.
		this.socket = socket;
		this.serve = serve;
		
		// Start a separate thread to read and handle the request.
		Thread thread = new Thread(this);
		thread.start();
	}
	
	// Methods from Runnable.
	
	private String reqMethod = null;
	private String reqUriPath = null;
	private String reqProtocol = null;
	private boolean oneOne;		// HTTP/1.1 or better
	private boolean reqMime;
	String reqQuery = null;
	private Vector reqHeaderNames = new Vector();
	private Vector reqHeaderValues = new Vector();
	
	public void run() {
		try {
			// Get the streams.
			in = new ServeInputStream(socket.getInputStream());
			out = new ServeOutputStream(socket.getOutputStream(), this);
		} catch(IOException e) {
			problem("Getting streams: " + e.getMessage(), SC_BAD_REQUEST);
		}
		
		parseRequest();
		
		try {
			socket.close();
		} catch(IOException e) {
			/* ignore */ }
	}
	
	private void parseRequest() {
		byte[] lineBytes = new byte[4096];
		int len;
		String line;
		
		try {
			// Read the first line of the request.
			len = in.readLine(lineBytes, 0, lineBytes.length);
			if(len == -1 || len == 0) {
				problem("Empty request", SC_BAD_REQUEST);
				return;
			}
			line = new String(lineBytes, 0, len);
			String[] tokens = Acme.Utils.splitStr(line);
			switch(tokens.length) {
				case 2:
					// Two tokens means the protocol is HTTP/0.9.
					reqProtocol = "HTTP/0.9";
					oneOne = false;
					reqMime = false;
					break;
				case 3:
					reqProtocol = tokens[2];
					oneOne = !reqProtocol.toUpperCase().equals("HTTP/1.0");
					reqMime = true;
					// Read the rest of the lines.
					while(true) {
						len = in.readLine(lineBytes, 0, lineBytes.length);
						if(len == -1 || len == 0)
							break;
						line = new String(lineBytes, 0, len);
						int colonBlank = line.indexOf(": ");
						if(colonBlank != -1) {
							String name = line.substring(0, colonBlank);
							String value = line.substring(colonBlank + 2);
							reqHeaderNames.addElement(name.toLowerCase());
							reqHeaderValues.addElement(value);
						}
					}
					break;
				default:
					problem("Malformed request line", SC_BAD_REQUEST);
					return;
			}
			reqMethod = tokens[0];
			reqUriPath = tokens[1];
			
			// Check Host: header in HTTP/1.1 requests.
			if(oneOne) {
				String host = getHeader("host");
				if(host == null) {
					problem("Host header missing on HTTP/1.1 request", SC_BAD_REQUEST);
					return;
				}
				// !!!
			}
			
			// Split off query string, if any.
			int qmark = reqUriPath.indexOf('?');
			if(qmark != -1) {
				reqQuery = reqUriPath.substring(qmark + 1);
				reqUriPath = reqUriPath.substring(0, qmark);
			}
			
			// Decode %-sequences.
			reqUriPath = decode(reqUriPath);
			
			Servlet servlet = (Servlet) serve.registry.get(reqUriPath);
			if(servlet != null)
				runServlet((HttpServlet) servlet);
		} catch(IOException e) {
			problem("Reading request: " + e.getMessage(), SC_BAD_REQUEST);
		}
	}
	
	private void runServlet(HttpServlet servlet) {
		// Set default response fields.
		setStatus(SC_OK);
		setDateHeader("Date", System.currentTimeMillis());
		setHeader("Server", ServeUtils.serverName + "/" + ServeUtils.serverVersion);
		setHeader("Connection", "close");
		try {
			servlet.service(this, this);
		} catch(IOException e) {
			problem("IO problem running servlet: " + e.toString(), SC_BAD_REQUEST);
		} catch(ServletException e) {
			problem("problem running servlet: " + e.toString(), SC_BAD_REQUEST);
		} catch(Exception e) {
			problem("unexpected problem running servlet: " + e.toString(), SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	private void problem(String logMessage, int resCode) {
		serve.log(logMessage);
		try {
			sendError(resCode);
		} catch(IOException e) {
			/* ignore */ }
	}
	
	private String decode(String str) {
		StringBuffer result = new StringBuffer();
		int l = str.length();
		for(int i = 0; i < l; ++i) {
			char c = str.charAt(i);
			if(c == '%' && i + 2 < l) {
				char c1 = str.charAt(i + 1);
				char c2 = str.charAt(i + 2);
				if(isHexit(c1) && isHexit(c2)) {
					result.append((char) (hexit(c1) * 16 + hexit(c2)));
					i += 2;
				} else
					result.append(c);
			} else
				result.append(c);
		}
		return result.toString();
	}
	
	private boolean isHexit(char c) {
		String legalChars = "0123456789abcdefABCDEF";
		return (legalChars.indexOf(c) != -1);
	}
	
	private int hexit(char c) {
		if(c >= '0' && c <= '9')
			return c - '0';
		if(c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		if(c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		return 0;	// shouldn't happen, we're guarded by isHexit()
	}
	
	// Methods from ServletRequest.
	
	/// Returns the size of the request entity data, or -1 if not known.
	// Same as the CGI variable CONTENT_LENGTH.
	public int getContentLength() {
		return getIntHeader("content-length", -1);
	}
	
	/// Returns the MIME type of the request entity data, or null if
	// not known.
	// Same as the CGI variable CONTENT_TYPE.
	public String getContentType() {
		return getHeader("content-type");
	}
	
	/// Returns the protocol and version of the request as a string of
	// the form <protocol>/<major version>.<minor version>.
	// Same as the CGI variable SERVER_PROTOCOL.
	public String getProtocol() {
		return reqProtocol;
	}
	
	/// Returns the scheme of the URL used in this request, for example
	// "http", "https", or "ftp". Different schemes have different rules
	// for constructing URLs, as noted in RFC 1738. The URL used to create
	// a request may be reconstructed using this scheme, the server name
	// and port, and additional information such as URIs.
	public String getScheme() {
		return "http";
	}
	
	/// Returns the host name of the server as used in the <host> part of
	// the request URI.
	// Same as the CGI variable SERVER_NAME.
	public String getServerName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch(UnknownHostException e) {
			return null;
		}
	}
	
	/// Returns the port number on which this request was received as used in
	// the <port> part of the request URI.
	// Same as the CGI variable SERVER_PORT.
	public int getServerPort() {
		return socket.getLocalPort();
	}
	
	/// Returns the IP address of the agent that sent the request.
	// Same as the CGI variable REMOTE_ADDR.
	public String getRemoteAddr() {
		return socket.getInetAddress().toString();
	}
	
	/// Returns the fully qualified host name of the agent that sent the
	// request.
	// Same as the CGI variable REMOTE_HOST.
	public String getRemoteHost() {
		return socket.getInetAddress().getHostName();
	}
	
	/// Applies alias rules to the specified virtual path and returns the
	// corresponding real path, or null if the translation can not be
	// performed for any reason. For example, an HTTP servlet would
	// resolve the path using the virtual docroot, if virtual hosting is
	// enabled, and with the default docroot otherwise. Calling this
	// method with the string "/" as an argument returns the document root.
	public String getRealPath(String path) {
		return serve.getRealPath(path);
	}
	
	/// Returns an input stream for reading request data.
	// @exception IllegalStateException if getReader has already been called
	// @exception IOException on other I/O-related errors
	public ServletInputStream getInputStream() throws IOException {
		return in;
	}
	
	/// Returns a buffered reader for reading request data.
	// @exception UnsupportedEncodingException if the character set encoding
	/// isn't supported
	// @exception IllegalStateException if getInputStream has already been
	/// called
	// @exception IOException on other I/O-related errors
	public BufferedReader getReader() {
		// !!!
		return null;
	}
	
	Vector queryNames = null;
	Vector queryValues = null;
	
	/// Returns the parameter names for this request.
	public Enumeration getParameterNames() {
		if(queryNames == null) {
			queryNames = new Vector();
			queryValues = new Vector();
			String qs = getQueryString();
			if(qs != null) {
				Enumeration en = new StringTokenizer(qs, "&");
				while(en.hasMoreElements()) {
					String nv = (String) en.nextElement();
					int eq = nv.indexOf('=');
					String name, value;
					if(eq == -1) {
						name = nv;
						value = "";
					} else {
						name = nv.substring(0, eq);
						value = nv.substring(eq + 1);
					}
					queryNames.addElement(name);
					queryValues.addElement(value);
				}
			}
		}
		return queryNames.elements();
	}
	
	/// Returns the value of the specified query string parameter, or null
	// if not found.
	// @param name the parameter name
	public String getParameter(String name) {
		Enumeration en = getParameterNames();
		int i = queryNames.indexOf(name);
		if(i == -1)
			return null;
		else
			return (String) queryValues.elementAt(i);
	}
	
	/// Returns the values of the specified parameter for the request as an
	// array of strings, or null if the named parameter does not exist.
	public String[] getParameterValues(String name) {
		Vector v = new Vector();
		Enumeration en = getParameterNames();
		for(int i = 0; i < queryNames.size(); ++i) {
			String n = (String) queryNames.elementAt(i);
			if(name.equals(n))
				v.addElement(queryValues.elementAt(i));
		}
		if(v.size() == 0)
			return null;
		String[] vArray = new String[v.size()];
		v.copyInto(vArray);
		return vArray;
	}
	
	/// Returns the value of the named attribute of the request, or null if
	// the attribute does not exist. This method allows access to request
	// information not already provided by the other methods in this interface.
	public Object getAttribute(String name) {
		// This server does not implement attributes.
		return null;
	}
	
	// Methods from HttpServletRequest.
	
	/// Gets the array of cookies found in this request.
	public Cookie[] getCookies() {
		Cookie[] cookieArray = new Cookie[cookies.size()];
		cookies.copyInto(cookieArray);
		return cookieArray;
	}
	
	/// Returns the method with which the request was made. This can be "GET",
	// "HEAD", "POST", or an extension method.
	// Same as the CGI variable REQUEST_METHOD.
	public String getMethod() {
		return reqMethod;
	}
	
	/// Returns the full request URI.
	public String getRequestURI() {
		String portPart = "";
		int port = getServerPort();
		if(port != 80)
			portPart = ":" + port;
		String queryPart = "";
		String queryString = getQueryString();
		if(queryString != null && queryString.length() > 0)
			queryPart = "?" + queryString;
		return "http://" + getServerName() + portPart + reqUriPath + queryPart;
	}
	
	/// Returns the part of the request URI that referred to the servlet being
	// invoked.
	// Analogous to the CGI variable SCRIPT_NAME.
	public String getServletPath() {
		// In this server, the entire path is regexp-matched against the
		// servlet pattern, so there's no good way to distinguish which
		// part refers to the servlet.
		return reqUriPath;
	}
	
	/// Returns optional extra path information following the servlet path, but
	// immediately preceding the query string. Returns null if not specified.
	// Same as the CGI variable PATH_INFO.
	public String getPathInfo() {
		// In this server, the entire path is regexp-matched against the
		// servlet pattern, so there's no good way to distinguish which
		// part refers to the servlet.
		return null;
	}
	
	/// Returns extra path information translated to a real path. Returns
	// null if no extra path information was specified.
	// Same as the CGI variable PATH_TRANSLATED.
	public String getPathTranslated() {
		// In this server, the entire path is regexp-matched against the
		// servlet pattern, so there's no good way to distinguish which
		// part refers to the servlet.
		return null;
	}
	
	/// Returns the query string part of the servlet URI, or null if not known.
	// Same as the CGI variable QUERY_STRING.
	public String getQueryString() {
		return reqQuery;
	}
	
	/// Returns the name of the user making this request, or null if not known.
	// Same as the CGI variable REMOTE_USER.
	public String getRemoteUser() {
		// This server does not support authentication, so even if a username
		// is supplied in the headers we don't want to look at it.
		return null;
	}
	
	/// Returns the authentication scheme of the request, or null if none.
	// Same as the CGI variable AUTH_TYPE.
	public String getAuthType() {
		// This server does not support authentication.
		return null;
	}
	
	/// Returns the value of a header field, or null if not known.
	// Same as the information passed in the CGI variabled HTTP_*.
	// @param name the header field name
	public String getHeader(String name) {
		int i = reqHeaderNames.indexOf(name.toLowerCase());
		if(i == -1)
			return null;
		return (String) reqHeaderValues.elementAt(i);
	}
	
	/// Returns the value of an integer header field.
	// @param name the header field name
	// @param def the integer value to return if header not found or invalid
	public int getIntHeader(String name, int def) {
		String val = getHeader(name);
		if(val == null)
			return def;
		try {
			return Integer.parseInt(val);
		} catch(Exception e) {
			return def;
		}
	}
	
	/// Returns the value of a long header field.
	// @param name the header field name
	// @param def the long value to return if header not found or invalid
	public long getLongHeader(String name, long def) {
		String val = getHeader(name);
		if(val == null)
			return def;
		try {
			return Long.parseLong(val);
		} catch(Exception e) {
			return def;
		}
	}
	
	/// Returns the value of a date header field.
	// @param name the header field name
	// @param def the date value to return if header not found or invalid
	public long getDateHeader(String name, long def) {
		String val = getHeader(name);
		if(val == null)
			return def;
		try {
			return DateFormat.getDateInstance().parse(val).getTime();
		} catch(Exception e) {
			return def;
		}
	}
	
	/// Returns an Enumeration of the header names.
	public Enumeration getHeaderNames() {
		return reqHeaderNames.elements();
	}
	
	// Session stuff. Not implemented, but the API is here for compatibility.
	
	/// Gets the current valid session associated with this request, if
	// create is false or, if necessary, creates a new session for the
	// request, if create is true.
	// <P>
	// Note: to ensure the session is properly maintained, the servlet
	// developer must call this method (at least once) before any output
	// is written to the response.
	// <P>
	// Additionally, application-writers need to be aware that newly
	// created sessions (that is, sessions for which HttpSession.isNew
	// returns true) do not have any application-specific state.
	public HttpSession getSession(boolean create) {
		return null;
	}
	
	/// Gets the session id specified with this request. This may differ
	// from the actual session id. For example, if the request specified
	// an id for an invalid session, then this will get a new session with
	// a new id.
	public String getRequestedSessionId() {
		return null;
	}
	
	/// Checks whether this request is associated with a session that is
	// valid in the current session context. If it is not valid, the
	// requested session will never be returned from the getSession
	// method.
	public boolean isRequestedSessionIdValid() {
		return false;
	}
	
	/// Checks whether the session id specified by this request came in as
	// a cookie. (The requested session may not be one returned by the
	// getSession method.)
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}
	
	/// Checks whether the session id specified by this request came in as
	// part of the URL. (The requested session may not be the one returned
	// by the getSession method.)
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}
	
	// Methods from ServletResponse.
	
	/// Sets the content length for this response.
	// @param length the content length
	public void setContentLength(int length) {
		setIntHeader("Content-Length", length);
	}
	
	/// Sets the content type for this response.
	// @param type the content type
	public void setContentType(String type) {
		setHeader("Content-Type", type);
	}
	
	/// Returns an output stream for writing response data.
	public ServletOutputStream getOutputStream() {
		return out;
	}
	
	/// Returns a print writer for writing response data. The MIME type of
	// the response will be modified, if necessary, to reflect the character
	// encoding used, through the charset=... property. This means that the
	// content type must be set before calling this method.
	// @exception UnsupportedEncodingException if no such encoding can be
	/// provided
	// @exception IllegalStateException if getOutputStream has been called
	// @exception IOException on other I/O errors
	public PrintWriter getWriter() throws IOException {
		// !!!
		return null;
	}
	
	/// Returns the character set encoding used for this MIME body. The
	// character encoding is either the one specified in the assigned
	// content type, or one which the client understands. If no content
	// type has yet been assigned, it is implicitly set to text/plain.
	public String getCharacterEncoding() {
		// !!!
		return null;
	}
	
	// Methods from HttpServletResponse.
	
	/// Adds the specified cookie to the response. It can be called
	// multiple times to set more than one cookie.
	public void addCookie(Cookie cookie) {
		cookies.addElement(cookie);
	}
	
	/// Checks whether the response message header has a field with the
	// specified name.
	public boolean containsHeader(String name) {
		return resHeaderNames.contains(name);
	}
	
	private int resCode = -1;
	private String resMessage = null;
	private Vector resHeaderNames = new Vector();
	private Vector resHeaderValues = new Vector();
	
	/// Sets the status code and message for this response.
	// @param resCode the status code
	// @param resMessage the status message
	public void setStatus(int resCode, String resMessage) {
		this.resCode = resCode;
		this.resMessage = resMessage;
	}
	
	/// Sets the status code and a default message for this response.
	// @param resCode the status code
	public void setStatus(int resCode) {
		switch(resCode) {
			case SC_CONTINUE:
				setStatus(resCode, "Continue");
				break;
			case SC_SWITCHING_PROTOCOLS:
				setStatus(resCode, "Switching protocols");
				break;
			case SC_OK:
				setStatus(resCode, "Ok");
				break;
			case SC_CREATED:
				setStatus(resCode, "Created");
				break;
			case SC_ACCEPTED:
				setStatus(resCode, "Accepted");
				break;
			case SC_NON_AUTHORITATIVE_INFORMATION:
				setStatus(resCode, "Non-authoritative");
				break;
			case SC_NO_CONTENT:
				setStatus(resCode, "No content");
				break;
			case SC_RESET_CONTENT:
				setStatus(resCode, "Reset content");
				break;
			case SC_PARTIAL_CONTENT:
				setStatus(resCode, "Partial content");
				break;
			case SC_MULTIPLE_CHOICES:
				setStatus(resCode, "Multiple choices");
				break;
			case SC_MOVED_PERMANENTLY:
				setStatus(resCode, "Moved permanentently");
				break;
			case SC_MOVED_TEMPORARILY:
				setStatus(resCode, "Moved temporarily");
				break;
			case SC_SEE_OTHER:
				setStatus(resCode, "See other");
				break;
			case SC_NOT_MODIFIED:
				setStatus(resCode, "Not modified");
				break;
			case SC_USE_PROXY:
				setStatus(resCode, "Use proxy");
				break;
			case SC_BAD_REQUEST:
				setStatus(resCode, "Bad request");
				break;
			case SC_UNAUTHORIZED:
				setStatus(resCode, "Unauthorized");
				break;
			case SC_PAYMENT_REQUIRED:
				setStatus(resCode, "Payment required");
				break;
			case SC_FORBIDDEN:
				setStatus(resCode, "Forbidden");
				break;
			case SC_NOT_FOUND:
				setStatus(resCode, "Not found");
				break;
			case SC_METHOD_NOT_ALLOWED:
				setStatus(resCode, "Method not allowed");
				break;
			case SC_NOT_ACCEPTABLE:
				setStatus(resCode, "Not acceptable");
				break;
			case SC_PROXY_AUTHENTICATION_REQUIRED:
				setStatus(resCode, "Proxy auth required");
				break;
			case SC_REQUEST_TIMEOUT:
				setStatus(resCode, "Request timeout");
				break;
			case SC_CONFLICT:
				setStatus(resCode, "Conflict");
				break;
			case SC_GONE:
				setStatus(resCode, "Gone");
				break;
			case SC_LENGTH_REQUIRED:
				setStatus(resCode, "Length required");
				break;
			case SC_PRECONDITION_FAILED:
				setStatus(resCode, "Precondition failed");
				break;
			case SC_REQUEST_ENTITY_TOO_LARGE:
				setStatus(resCode, "Request entity too large");
				break;
			case SC_REQUEST_URI_TOO_LONG:
				setStatus(resCode, "Request URI too large");
				break;
			case SC_UNSUPPORTED_MEDIA_TYPE:
				setStatus(resCode, "Unsupported media type");
				break;
			case SC_INTERNAL_SERVER_ERROR:
				setStatus(resCode, "Internal server error");
				break;
			case SC_NOT_IMPLEMENTED:
				setStatus(resCode, "Not implemented");
				break;
			case SC_BAD_GATEWAY:
				setStatus(resCode, "Bad gateway");
				break;
			case SC_SERVICE_UNAVAILABLE:
				setStatus(resCode, "Service unavailable");
				break;
			case SC_GATEWAY_TIMEOUT:
				setStatus(resCode, "Gateway timeout");
				break;
			case SC_HTTP_VERSION_NOT_SUPPORTED:
				setStatus(resCode, "HTTP version not supported");
				break;
			default:
				setStatus(resCode, "");
				break;
		}
	}
	
	/// Sets the value of a header field.
	// @param name the header field name
	// @param value the header field value
	public void setHeader(String name, String value) {
		resHeaderNames.addElement(name);
		resHeaderValues.addElement(value);
	}
	
	/// Sets the value of an integer header field.
	// @param name the header field name
	// @param value the header field integer value
	public void setIntHeader(String name, int value) {
		setHeader(name, Integer.toString(value));
	}
	
	/// Sets the value of a long header field.
	// @param name the header field name
	// @param value the header field long value
	public void setLongHeader(String name, long value) {
		setHeader(name, Long.toString(value));
	}
	
	/// Sets the value of a date header field.
	// @param name the header field name
	// @param value the header field date value
	public void setDateHeader(String name, long value) {
		setHeader(name, to1123String(new Date(value)));
	}
	
	private static final String[] weekdays = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	
	/// Converts a Date into an RFC-1123 string.
	private static String to1123String(Date date) {
		// We have to go through some machinations here to get the
		// correct day of the week in GMT. getDay() gives the day in
		// local time. getDate() gives the day of the month in local
		// time. toGMTString() gives a formatted string in GMT. So, we
		// extract the day of the month from the GMT string, and if it
		// doesn't match the local one we change the local day of the
		// week accordingly.
		//
		// The Date class sucks.
		int localDay = date.getDay();
		int localDate = date.getDate();
		String gmtStr = date.toGMTString();
		int blank = gmtStr.indexOf(' ');
		int gmtDate = Integer.parseInt(gmtStr.substring(0, blank));
		int gmtDay;
		if(gmtDate > localDate || (gmtDate < localDate && gmtDate == 1))
			gmtDay = (localDay + 1) % 7;
		else if(localDate > gmtDate || (localDate < gmtDate && localDate == 1))
			gmtDay = (localDay + 6) % 7;
		else
			gmtDay = localDay;
		return weekdays[gmtDay] + (gmtDate < 10 ? ", 0" : ", ") + gmtStr;
	}
	
	private boolean headersWritten = false;
	
	/// Writes the status line and message headers for this response to the
	// output stream.
	// @exception IOException if an I/O error has occurred
	void writeHeaders() throws IOException {
		if(headersWritten)
			return;
		headersWritten = true;
		if(reqMime) {
			out.println(reqProtocol + " " + resCode + " " + resMessage);
			for(int i = 0; i < resHeaderNames.size(); ++i) {
				String name = (String) resHeaderNames.elementAt(i);
				String value = (String) resHeaderValues.elementAt(i);
				if(value != null)	// just in case
					out.println(name + ": " + value);
			}
			out.println("");
			out.flush();
		}
	}
	
	/// Writes an error response using the specified status code and message.
	// @param resCode the status code
	// @param resMessage the status message
	// @exception IOException if an I/O error has occurred
	public void sendError(int resCode, String resMessage) throws IOException {
		setStatus(resCode, resMessage);
		realSendError();
	}
	
	/// Writes an error response using the specified status code and a default
	// message.
	// @param resCode the status code
	// @exception IOException if an I/O error has occurred
	public void sendError(int resCode) throws IOException {
		setStatus(resCode);
		realSendError();
	}
	
	private void realSendError() throws IOException {
		setContentType("text/html");
		out.println("<HTML><HEAD>");
		out.println("<TITLE>" + resCode + " " + resMessage + "</TITLE>");
		out.println("</HEAD><BODY BGCOLOR=\"#99cc99\">");
		out.println("<H2>" + resCode + " " + resMessage + "</H2>");
		String ua = getHeader("user-agent");
		if(ua != null && Acme.Utils.match("*MSIE*", ua)) {
			out.println("<!--");
			for(int i = 0; i < 6; ++i)
				out.println("Padding so that MSIE deigns to show this error instead of its own canned one.");
			out.println("-->");
		}
		out.println("<HR>");
		ServeUtils.writeAddress(out);
		out.println("</BODY></HTML>");
		out.flush();
	}
	
	/// Sends a redirect message to the client using the specified redirect
	// location URL.
	// @param location the redirect location URL
	// @exception IOException if an I/O error has occurred
	public void sendRedirect(String location) throws IOException {
		setHeader("Location", location);
		sendError(SC_MOVED_TEMPORARILY);
	}
	
	// URL session-encoding stuff. Not implemented, but the API is here
	// for compatibility.
	
	/// Encodes the specified URL by including the session ID in it, or, if
	// encoding is not needed, returns the URL unchanged. The
	// implementation of this method should include the logic to determine
	// whether the session ID needs to be encoded in the URL. For example,
	// if the browser supports cookies, or session tracking is turned off,
	// URL encoding is unnecessary.
	// <P>
	// All URLs emitted by a Servlet should be run through this method.
	// Otherwise, URL rewriting cannot be used with browsers which do not
	// support cookies.
	public String encodeUrl(String url) {
		return url;
	}
	
	/// Encodes the specified URL for use in the sendRedirect method or, if
	// encoding is not needed, returns the URL unchanged. The
	// implementation of this method should include the logic to determine
	// whether the session ID needs to be encoded in the URL. Because the
	// rules for making this determination differ from those used to
	// decide whether to encode a normal link, this method is seperate
	// from the encodeUrl method.
	// <P>
	// All URLs sent to the HttpServletResponse.sendRedirect method should be
	// run through this method. Otherwise, URL rewriting cannot be used with
	// browsers which do not support cookies.
	public String encodeRedirectUrl(String url) {
		return url;
	}
	
}

class ServeInputStream extends ServletInputStream {
	
	private InputStream in;
	
	public ServeInputStream(InputStream in) {
		this.in = in;
	}
	
	public int readLine(byte[] b, int off, int len) throws IOException {
		int off2 = off;
		while(off2 - off < len) {
			int r = read();
			if(r == -1) {
				if(off2 == off)
					return -1;
				break;
			}
			if(r == 13)
				continue;
			if(r == 10)
				break;
			b[off2] = (byte) r;
			++off2;
		}
		return off2 - off;
	}
	
	public int read() throws IOException {
		return in.read();
	}
	
	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}
	
	public int available() throws IOException {
		return in.available();
	}
	
	public void close() throws IOException {
		in.close();
	}
	
}

class ServeOutputStream extends ServletOutputStream {
	
	private PrintStream out;
	private ServeConnection conn;
	
	public ServeOutputStream(OutputStream out, ServeConnection conn) {
		this.out = new PrintStream(out);
		this.conn = conn;
	}
	
	public void write(int b) throws IOException {
		conn.writeHeaders();
		out.write(b);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		conn.writeHeaders();
		out.write(b, off, len);
	}
	
	public void flush() throws IOException {
		conn.writeHeaders();
		out.flush();
	}
	
	public void close() throws IOException {
		conn.writeHeaders();
		out.close();
	}
	
	public void print(String s) throws IOException {
		conn.writeHeaders();
		out.print(s);
	}
	
	public void print(int i) throws IOException {
		conn.writeHeaders();
		out.print(i);
	}
	
	public void print(long l) throws IOException {
		conn.writeHeaders();
		out.print(l);
	}
	
	public void println(String s) throws IOException {
		conn.writeHeaders();
		out.println(s);
	}
	
	public void println(int i) throws IOException {
		conn.writeHeaders();
		out.println(i);
	}
	
	public void println(long l) throws IOException {
		conn.writeHeaders();
		out.println(l);
	}
	
	public void println() throws IOException {
		conn.writeHeaders();
		out.println();
	}
	
}
