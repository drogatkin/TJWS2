// Serve - minimal Java servlet container class
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@acme.com>. All rights reserved.
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
//

// All enhancements Copyright (C)1998-2014 by Dmitriy Rogatkin
// This version is compatible with JSDK 2.5
// http://tjws.sourceforge.net
// $Id: Serve.java,v 1.269 2013/08/20 04:11:09 cvs Exp $

package Acme.Serve;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.BindException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;

import javax.net.ssl.SSLSocket;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.Part;

import com.rslakra.logger.LogManager;

import Acme.IOHelper;
import Acme.ThreadPoolFactory;
import Acme.Utils;

/// Minimal Java servlet container class.
// <P>
// This class implements a very small embeddable servlet container.
// It runs Servlets compatible with the API used by Sun's
// <A HREF="http://docs.sun.com/app/docs/doc/819-3653">Java System Application
/// </A> server.
// Servlet API can be found <A
/// HREF="http://java.sun.com/products/servlet/">here</A>.
// It comes with default Servlets which provide the usual
// httpd services, returning files and directory listings.
// <P>
// This is not in any sense a competitor for Java System Application server.
// Java System Application server is a full-fledged HTTP server and more.
// Acme.Serve is tiny, about 5000 lines, and provides only the
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
// HREF="http://docs.sun.com/app/docs/doc/819-3653">JavaServer</A>.
// <LI> W3C's <A HREF="http://www.w3.org/pub/WWW/Jigsaw/">Jigsaw</A>.
// <LI> David Wilkinson's <A
// HREF="http://www.netlink.co.uk/users/cascade/http/">Cascade</A>.
// <LI> Yahoo's <A
// HREF="http://www.yahoo.com/Computers_and_Internet/Software/Internet/World_Wide_Web/Servers/Java/">list
// of Java web servers</A>.
// </UL>
// <P>
// A <A HREF="http://www.byte.com/art/9706/sec8/art1.htm">June 1997 BYTE
// magazine article</A> mentioning this server.<BR>
// A <A HREF="http://www.byte.com/art/9712/sec6/art7.htm">December 1997 BYTE
// magazine article</A> giving it an Editor's Choice Award of Distinction.<BR>
// <A HREF="/resources/classes/Acme/Serve/Serve.java">Fetch the
// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.Z">Fetch the entire Acme package.</A>
// <P>
// @see Acme.Serve.servlet.http.HttpServlet
// @see FileServlet
// @see CgiServlet
// <h3>Post notes</h3>
// Currently the server 3 more times complex and can compete with
// most popular app and web servers used for deploying of web
// Java applications.

// Inheritance can extend usage of this server
public class Serve implements ServletContext, Serializable {
	
	public static final String ARG_PORT = "port";
	public static final String ARG_THROTTLES = "throttles";
	public static final String ARG_SERVLETS = "servlets";
	public static final String ARG_REALMS = "realms";
	public static final String ARG_ALIASES = "aliases";
	public static final String ARG_BINDADDRESS = "bind-address";
	public static final String ARG_BACKLOG = "backlog";
	public static final String ARG_CGI_PATH = "cgi-path";
	public static final String ARG_ERR = "error-stream";
	public static final String ARG_OUT = "out-stream";
	public static final String ARG_SESSION_TIMEOUT = "session-timeout";
	public static final String ARG_LOG_DIR = "log-dir";
	public static final String ARG_LOG_OPTIONS = "log-options";
	public static final String ARG_LOGROLLING_LINES = "log-rolling-lines-threshold";
	
	public static final String ARG_NOHUP = "nohup";
	
	public static final String ARG_JSP = "JSP";
	public static final String ARG_WAR = "war-deployer";
	public static final String ARG_WEBSOCKET = "WEBSOCKET-PROVIDER";
	public static final String ARG_KEEPALIVE = "keep-alive";
	public static final String ARG_PROXY_CONFIG = "proxy-config";
	public static final String DEF_LOGENCODING = "tjws.serve.log.encoding";
	public static final String DEF_PROXY_CONFIG = "tjws.proxy.ssl";
	public static final String ARG_KEEPALIVE_TIMEOUT = "timeout-keep-alive";
	public static final String ARG_MAX_CONN_USE = "max-alive-conn-use";
	public static final String ARG_SESSION_PERSIST = "sssn-persistance";
	public static final String ARG_MAX_ACTIVE_SESSIONS = "max-active-sessions";
	public static final String ARG_ACCESS_LOG_FMT = "access-log-format";
	public static final String ARG_ACCEPTOR_CLASS = "acceptorImpl";
	public static final String ARG_WORK_DIRECTORY = "workdirectory";
	public static final String ARG_SESSION_SEED = "SessionSeed";
	public static final String ARG_SESSION_SEED_ALG = "SecureRandomAlgorithm";
	public static final String ARG_HTTPONLY_SC = "sessionhttponly";
	public static final String ARG_SECUREONLY_SC = "sessionsecureonly";
	
	public static final String ARG_THREAD_POOL_SIZE = Utils.ThreadPool.MAXNOTHREAD;
	// in minutes
	protected static final int DEF_SESSION_TIMEOUT = 30;
	protected static final int DEF_MIN_ACT_SESS = 10;
	protected static final int DESTROY_TIME_SEC = 15;
	protected static final int HTTP_MAX_HDR_LEN = 1024 * 1024 * 10;
	public static final int DEF_PORT = 8080;
	public static final String BGCOLOR = "BGCOLOR=\"#D1E9FE\"";
	public final static String LINE_SEP = System.getProperty("line.separator", "\n");
	
	/** max number of alive connections default value */
	protected static final int DEF_MAX_CONN_USE = 100;
	protected static final Integer INT_ZERO = new Integer(0);
	
	protected String hostName;
	private transient PrintStream logStream;
	
	private boolean useAccLog;
	private boolean keepAlive;
	private boolean proxyConfig;
	private boolean proxySSL;
	private int timeoutKeepAlive;
	private int maxAliveConnUse;
	private boolean showUserAgent;
	private boolean showReferer;
	protected String keepAliveHdrParams;
	
	protected transient PathTreeDictionary defaultRegistry;
	protected transient HashMap<String, PathTreeDictionary> virtuals;
	protected transient PathTreeDictionary realms;
	protected transient PathTreeDictionary mappingtable;
	private Hashtable<String, Object> attributes;
	
	protected transient KeepAliveCleaner keepAliveCleaner;
	protected transient ThreadGroup serverThreads;
	protected transient Utils.ThreadPool threadPool;
	protected transient Constructor<?> gzipInStreamConstr;
	private static final ThreadLocal<PathTreeDictionary> currentRegistry = new ThreadLocal<PathTreeDictionary>();
	
	// TODO for sessions consider configurable strength
	private byte[] uniqer = new byte[20];
	private SecureRandom secureRandom;
	
	protected HttpSessionContextImpl sessions;
	public int expiredIn;
	public boolean httpSessCookie;
	public boolean secureSessCookie;
	public Map<Object, Object> arguments;
	public Properties mime;
	public WebsocketProvider websocketProvider;
	
	/**
	 * 
	 * @author Rohtash Lakra (rohtash.lakra@devamatre.com)
	 * @author Rohtash Singh Lakra (rohtash.singh@gmail.com)
	 * @created 2018-03-20 07:37:46 PM
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private static class ServerThreadPoolFactory implements ThreadPoolFactory {
		
		/** serverThreadGroup */
		private final ThreadGroup serverThreadGroup;
		
		/**
		 * 
		 * @param serverThreadGroup
		 */
		public ServerThreadPoolFactory(final ThreadGroup serverThreadGroup) {
			this.serverThreadGroup = serverThreadGroup;
		}
		
		/**
		 * @param runnable
		 * @return
		 * @see Acme.ThreadFactory#create(java.lang.Runnable)
		 */
		@Override
		public Thread create(Runnable runnable) {
			final Thread result = new Thread(serverThreadGroup, runnable);
			result.setDaemon(true);
			return result;
		}
	}
	
	/**
	 * 
	 * @param arguments
	 * @param logStream
	 */
	public Serve(Map<Object, Object> arguments, PrintStream logStream) {
		this.arguments = arguments;
		this.logStream = logStream;
		defaultRegistry = new PathTreeDictionary();
		realms = new PathTreeDictionary();
		attributes = new Hashtable<String, Object>();
		serverThreads = new ThreadGroup("TJWS Threads");
		Properties props = new Properties();
		props.putAll(arguments);
		// TODO do not create thread pool unless requested
		threadPool = new Utils.ThreadPool(props, new ServerThreadPoolFactory(serverThreads));
		setAccessLogged();
		keepAlive = arguments.get(ARG_KEEPALIVE) == null || ((Boolean) arguments.get(ARG_KEEPALIVE)).booleanValue();
		int timeoutKeepAliveSec = Utils.parseInt(arguments.get(ARG_KEEPALIVE_TIMEOUT), 30);
		timeoutKeepAlive = timeoutKeepAliveSec * 1000;
		maxAliveConnUse = Utils.parseInt(arguments.get(ARG_MAX_CONN_USE), DEF_MAX_CONN_USE);
		keepAliveHdrParams = "timeout=" + timeoutKeepAliveSec + ", max=" + maxAliveConnUse;
		
		expiredIn = Utils.parseInt(arguments.get(ARG_SESSION_TIMEOUT), DEF_SESSION_TIMEOUT);
		String seed = (String) arguments.get(ARG_SESSION_SEED);
		String randomProvider = (String) arguments.get(ARG_SESSION_SEED_ALG); // "SHA1PRNG";
		int seedLength = Utils.parseInt(seed, 0);
		if (seed != null && seedLength == 0) {
			secureRandom = new SecureRandom(seed.getBytes());
		} else {
			if (randomProvider != null) {
				try {
					secureRandom = SecureRandom.getInstance(randomProvider);
				} catch (NoSuchAlgorithmException ex) {
					log("TJWS: Unsupported or incorrect secure rndom algorithm: " + randomProvider + "(" + ex + "), a default is used", ex);
				}
			}
			
			if (secureRandom == null) {
				secureRandom = new SecureRandom();
			}
			
			seedLength = seedLength <= 0 ? 100 : seedLength;
			byte bseed[] = null;
			secureRandom.setSeed(bseed = secureRandom.generateSeed(seedLength));
			secureRandom.nextBytes(bseed);
			secureRandom.setSeed(bseed);
		}
		
		httpSessCookie = arguments.get(ARG_HTTPONLY_SC) != null;
		secureSessCookie = arguments.get(ARG_SECUREONLY_SC) != null;
		try {
			gzipInStreamConstr = Class.forName("java.util.zip.GZIPInputStream").getConstructor(new Class[] { InputStream.class });
		} catch (ClassNotFoundException ex) {
			log(ex);
		} catch (NoSuchMethodException ex) {
			log(ex);
		}
		
		String proxyArg = (String) arguments.get(ARG_PROXY_CONFIG);
		if (proxyArg != null) {
			proxyConfig = true;
			proxySSL = "y".equalsIgnoreCase(proxyArg);
		}
		initMime();
	}
	
	/**
	 * Default constructor to create TJWS as a bean
	 * 
	 */
	public Serve() {
		this(new HashMap<Object, Object>(), System.err);
	}
	
	protected void setAccessLogged() {
		String logflags = (String) arguments.get(ARG_LOG_OPTIONS);
		if (logflags != null) {
			useAccLog = true;
			showUserAgent = logflags.indexOf('A') >= 0;
			showReferer = logflags.indexOf('R') >= 0;
		} else {
			useAccLog = false;
		}
	}
	
	protected boolean isAccessLogged() {
		return useAccLog;
	}
	
	protected boolean isShowReferer() {
		return showReferer;
	}
	
	protected boolean isShowUserAgent() {
		return showUserAgent;
	}
	
	protected boolean isKeepAlive() {
		return keepAlive;
	}
	
	protected int getKeepAliveDuration() {
		return timeoutKeepAlive;
	}
	
	protected String getKeepAliveParamStr() {
		return keepAliveHdrParams;
	}
	
	protected int getMaxTimesConnectionUse() {
		return maxAliveConnUse;
	}
	
	protected void initMime() {
		mime = new Properties();
		try {
			mime.load(getClass().getClassLoader().getResourceAsStream("Acme/Resource/mime.properties"));
		} catch (Exception ex) {
			log("TJWS: MIME map can't be loaded:" + ex);
		}
	}
	
	// / Register a Servlet by class name. Registration consists of a URL
	// pattern, which can contain wildcards, and the class name of the Servlet
	// to launch when a matching URL comes in. Patterns are checked for
	// matches in the order they were added, and only the first match is run.
	public ServletRegistration.Dynamic addServlet(String urlPat, String className) {
		addServlet(urlPat, className, (Hashtable) null);
		return null;
	}
	
	/**
	 * Adds a servlet to run
	 * 
	 * @param urlPat
	 *            servlet invoker URL pattern
	 * @param className
	 *            servlet class name
	 * @param initParams
	 *            servlet init parameters
	 */
	public void addServlet(String urlPat, String className, Hashtable initParams) {
		// Check if we're allowed to make one of these.
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			int i = className.lastIndexOf('.');
			if (i > 0) {
				security.checkPackageAccess(className.substring(0, i));
				security.checkPackageDefinition(className.substring(0, i));
			}
		}
		
		// Make a new one.
		try {
			addServlet(urlPat, (Servlet) Class.forName(className).newInstance(), initParams, null);
		} catch (ClassNotFoundException e) {
			log("TJWS: Class not found: " + className);
			ClassLoader cl = getClass().getClassLoader();
			log("TJWS: Class loader: " + cl);
			if (cl instanceof java.net.URLClassLoader)
				log("TJWS: CP: " + java.util.Arrays.asList(((java.net.URLClassLoader) cl).getURLs()));
		} catch (ClassCastException e) {
			log("TJWS: Servlet class doesn't implement javax.servlet.Servlet: " + e.getMessage());
		} catch (InstantiationException e) {
			log("TJWS: Can't instantiate servlet: " + e.getMessage());
		} catch (IllegalAccessException e) {
			log("TJWS: Illegal class access: " + e.getMessage());
		} catch (Exception e) {
			log("TJWS: Unexpected problem of servlet creation: " + e, e);
		}
	}
	
	/**
	 * Register a Servlet. Registration consists of a URL pattern, which can
	 * contain wildcards, and the Servlet to launch when a matching URL comes
	 * in. Patterns are checked for matches in the order they were added, and
	 * only the first match is run.
	 * 
	 * @param urlPat
	 *            servlet invoker URL pattern
	 * @param servlet
	 *            already instantiated servlet but init
	 * @param host
	 *            name the servlet attached to, when verual hosts are used, use
	 *            null for all hosts
	 */
	public void addServlet(String urlPat, Servlet servlet, String hostName) {
		addServlet(urlPat, servlet, (Hashtable) null, hostName);
	}
	
	/**
	 * Register a Servlet. Registration consists of a URL pattern, which can
	 * contain wildcards, and the Servlet to launch when a matching URL comes
	 * in. Patterns are checked for matches in the order they were added, and
	 * only the first match is run.
	 * 
	 * @param urlPat
	 *            servlet invoker URL pattern
	 * @param servlet
	 *            already instantiated servlet but init
	 */
	public javax.servlet.ServletRegistration.Dynamic addServlet(String urlPat, Servlet servlet) {
		addServlet(urlPat, servlet, (String) null);
		return null;
	}
	
	/**
	 * Register a Servlet
	 * 
	 * @param urlPat
	 * @param servlet
	 * @param initParams
	 */
	public synchronized void addServlet(String urlPath, Servlet servlet, Hashtable initParams, String virtualHost) {
		setHost(virtualHost);
		try {
			if (getServlet(urlPath) != null) {
				log("TJWS: Servlet overriden by " + servlet + ", for path:" + urlPath);
			}
			
			servlet.init(new ServeConfig((ServletContext) this, initParams, urlPath));
			if (virtualHost != null) {
				if (virtuals == null) {
					virtuals = new HashMap<String, PathTreeDictionary>();
				}
				
				virtualHost = virtualHost.toLowerCase();
				PathTreeDictionary virtualRegistry = virtuals.get(virtualHost);
				if (virtualRegistry == null) {
					virtualRegistry = new PathTreeDictionary();
					virtuals.put(virtualHost, virtualRegistry);
				}
				virtualRegistry.put(urlPath, servlet);
			} else {
				defaultRegistry.put(urlPath, servlet);
			}
		} catch (ServletException ex) { //
			// it handles UnavailableException as well without an attempt to
			// re-adding
			log("TJWS: Problem initializing servlet, it won't be used: " + ex);
		}
	}
	
	/**
	 * 
	 * @param servlet
	 * @return
	 */
	public Servlet unloadServlet(Servlet servlet) {
		PathTreeDictionary registry = currentRegistry.get();
		if (registry == null) {
			registry = defaultRegistry;
		}
		synchronized (registry) {
			return (Servlet) registry.remove(servlet)[0];
		}
	}
	
	/**
	 * 
	 * @param servletContext
	 */
	public void unloadSessions(ServletContext servletContext) {
		if (sessions == null) {
			return;
		}
		
		File store = getPersistentFile(servletContext);
		Writer storeWriter = null;
		try {
			if (store != null) {
				storeWriter = new FileWriter(store);
			}
			// TODO consider moving implementation to HttpSessionContextImpl
			// TODO think of concurrency
			Enumeration<?> itr = sessions.elements();
			int sc = 0;
			while (itr.hasMoreElements()) {
				AcmeSession session = (AcmeSession) itr.nextElement();
				if (session.getServletContext() == servletContext) {
					if (storeWriter != null)
						try {
							session.save(storeWriter);
						} catch (Throwable t) {
							if (t instanceof ThreadDeath) {
								throw (ThreadDeath) t;
							}
							log("TJWS: problem in session serialization for " + servletContext + " / " + session, t);
						}
					session.invalidate();
					sc++;
				}
			}
			
			if (sc > 0 && ssclThread != null) {
				ssclThread.interrupt();
			}
			log("TJWS: invalidated " + sc + " sessions for context " + servletContext, null);
		} catch (IOException ioe) {
			log("TJWS: problem in persisting sessions for  " + servletContext, ioe);
		} finally {
			IOHelper.closeSilently(storeWriter);
		}
	}
	
	/**
	 * 
	 * @param servletContext
	 */
	public void restoreSessions(ServletContext servletContext) {
		File store = sessions == null ? null : getPersistentFile(servletContext);
		if (store != null && store.exists()) {
			BufferedReader bReader = null;
			try {
				bReader = new BufferedReader(new FileReader(store));
				AcmeSession session;
				while ((session = AcmeSession.restore(bReader, Math.abs(expiredIn) * 60, servletContext, sessions)) != null) {
					if (session.checkExpired() == false) {
						sessions.put(session.getId(), session);
					}
				}
			} catch (IOException ioe) {
				log("TJWS: problem in sessions deserialization for " + servletContext, ioe);
			} finally {
				IOHelper.closeSilently(bReader);
			}
		}
	}
	
	// Register a standard set of Servlets. These will return
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
	// TODO: provide user specified CGI directory
	public void addDefaultServlets(String cgi) {
		try {
			addDefaultServlets(cgi, null);
		} catch (IOException ioe) {
			/* ignore, makes sense only for throtles */
		}
	}
	
	/**
	 * Register a standard set of Servlets, with optional throttles. These will
	 * return files or directory listings, and run CGI programs, much like a
	 * standard HTTP server.
	 * <P>
	 * Because of the pattern checking order, this should be called <B>after</B>
	 * you've added any custom Servlets.
	 * <P>
	 * The current set of default servlet mappings:
	 * <UL>
	 * <LI>If enabled, *.cgi goes to CgiServlet, and gets run as a CGI program.
	 * <LI>* goes to FileServlet, and gets served up as a file or directory.
	 * </UL>
	 * 
	 * @param cgi
	 *            whether to run CGI programs
	 * @param throttles
	 *            filename to read FileServlet throttle settings from, can be
	 *            null
	 * @throws IOException
	 */
	public void addDefaultServlets(String cgi, String throttles) throws IOException {
		// TODO: provide user specified CGI directory
		if (cgi != null) {
			if (getServlet("/" + cgi + "/*") == null) {
				addServlet("/" + cgi + "/*", new Acme.Serve.CgiServlet());
			} else {
				log("TJWS: Servlet for path '/" + cgi + "' already defined and no default will be used.");
			}
		}
		
		if (getServlet("/*") == null) {
			if (throttles != null) {
				addServlet("/*", new Acme.Serve.FileServlet(throttles, null));
			} else {
				addServlet("/*", new Acme.Serve.FileServlet());
			}
		} else {
			log("TJWS: Servlet for path '/' already defined and no default will be used.");
		}
	}
	
	// TODO review this method and figure throttles use
	protected void addWarDeployer(String deployerFactory, String throttles) {
		if (deployerFactory == null) { // try to use def
			deployerFactory = "rogatkin.web.WarRoller";
		}
		
		try {
			WarDeployer wd = (WarDeployer) Class.forName(deployerFactory).newInstance();
			wd.deploy(this);
		} catch (ClassNotFoundException cnf) {
			log("TJWS: Problem initializing war deployer: " + cnf);
		} catch (Throwable ex) {
			if (ex instanceof ThreadDeath) {
				throw (ThreadDeath) ex;
			}
			log("TJWS: Problem in war(s) deployment", ex);
		}
	}
	
	/**
	 * 
	 * @param provider
	 */
	protected void addWebsocketProvider(String provider) {
		if (provider == null) {
			provider = "rogatkin.wskt.SimpleProvider";
		}
		
		try {
			websocketProvider = (WebsocketProvider) Class.forName(provider).newInstance();
			websocketProvider.init(this);
			websocketProvider.deploy(this, null);
		} catch (ClassNotFoundException cnf) {
			log("TJWS: Problem finding websocket provider: " + cnf);
		} catch (Throwable t) {
			if (t instanceof ThreadDeath) {
				throw (ThreadDeath) t;
			}
			log("TJWS: Problem initializing websocket provider", t);
		}
	}
	
	protected File getPersistentFile() {
		return getPersistentFile(null);
	}
	
	/**
	 * 
	 * @param servletContext
	 * @return
	 */
	protected File getPersistentFile(ServletContext servletContext) {
		if (arguments.get(ARG_SESSION_PERSIST) == null || ((Boolean) arguments.get(ARG_SESSION_PERSIST)) == Boolean.FALSE) {
			return null;
		}
		
		String workPath = (String) arguments.get(ARG_WORK_DIRECTORY);
		if (workPath == null) {
			workPath = ".";
		}
		
		if (hostName == null) {
			hostName = "unknown";
		}
		hostName = sanitizeAsFile(hostName);
		return new File(workPath, hostName + '-' + (arguments.get(ARG_PORT) == null ? String.valueOf(DEF_PORT) : arguments.get(ARG_PORT)) + (servletContext == null ? "" : "-" + servletContext.getServletContextName()) + "-session.obj");
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	protected String sanitizeAsFile(String name) {
		return name.replaceAll("\\.|:|\\\\|/", "-");
	}
	
	// Run the server. Returns only on errors.
	private transient boolean running;
	protected transient Acceptor acceptor;
	protected transient Thread ssclThread;
	
	/**
	 * 
	 * @author Rohtash Lakra (rohtash.lakra@devamatre.com)
	 * @author Rohtash Singh Lakra (rohtash.singh@gmail.com)
	 * @created 2018-03-20 07:58:34 PM
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	protected class SessionCleaner implements Runnable {
		
		/**
		 * 
		 * @param serve
		 */
		public SessionCleaner() {
		}
		
		/**
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			while (isRunning()) {
				try {
					Thread.sleep(expiredIn * 60 * 1000);
				} catch (InterruptedException ie) {
					if (!isRunning()) {
						break;
					}
				}
				
				Enumeration<?> itr = sessions.keys();
				while (itr.hasMoreElements()) {
					Object sid = itr.nextElement();
					if (sid != null) {
						AcmeSession session = (AcmeSession) sessions.get(sid);
						if (session != null && (session.checkExpired() || !session.isValid())) {
							session = (AcmeSession) sessions.remove(sid);
							if (session != null && session.isValid()) {
								try {
									session.invalidate();
								} catch (IllegalStateException ise) {
									
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * @author Rohtash Singh Lakra
	 * @date 03/16/2018 12:00:30 PM
	 */
	public static enum Status {
		BIND_ERROR(-5),
		NOT_FINISHED_CORRECTLY(-4),
		IO_ERROR(-3),
		TERMINATED_WITH_ERROR(-2),
		ERROR(-1),
		STOPPED(0),
		RUNNING(1),
		STILL_RUNNING(2),;
		
		/** status */
		private int status;
		
		/**
		 * 
		 * @param status
		 */
		private Status(int status) {
			this.status = status;
		}
		
		/**
		 * 
		 * @return
		 */
		public int getStatus() {
			return status;
		}
		
		/**
		 * 
		 * @see java.lang.Enum#toString()
		 */
		public String toString() {
			return String.valueOf(getStatus());
		}
		
		/**
		 * 
		 * @param status
		 * @return
		 */
		public static Status toStatus(final int status) {
			Status[] serveStatus = values();
			for (int i = 0; i < serveStatus.length; i++) {
				if (serveStatus[i].getStatus() == status) {
					return serveStatus[i];
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Returns true if the server is running otherwise false.
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return running;
	}
	
	/**
	 * Launches the server It doesn't exist until server runs, so start it in a
	 * dedicated thread.
	 * 
	 * Returns the following values: -
	 * 0 if the server successfully terminated, and,
	 * 1 or 2 if it can't be started, and
	 * -1 if it was terminated during some errors, and
	 * -2 if it was ran over running instance, and
	 * -3 if not terminated correctly from previous run.
	 * 
	 * @return
	 */
	public Status serve() {
		if (isRunning()) {
			// still running
			return Status.STILL_RUNNING;
		}
		
		if (acceptor != null) {
			// not finished correctly
			return Status.NOT_FINISHED_CORRECTLY;
		}
		
		try {
			acceptor = createAcceptor();
		} catch (Throwable ex) {
			log("TJWS:  Acceptor [" + acceptor + ": " + ex.getLocalizedMessage(), ex);
			if (ex instanceof ThreadDeath) {
				throw (ThreadDeath) ex;
			}
			acceptor = null;
			if (ex instanceof BindException) {
				return Status.BIND_ERROR;
			} else if (ex instanceof IOException) {
				return Status.IO_ERROR;
			}
			
			return Status.ERROR;
		}
		
		/* if here, means no error and server is running. */
		running = true;
		if (expiredIn > 0) {
			// sessions cleaner thread
			ssclThread = new Thread(serverThreads, new SessionCleaner(), "Session Cleaner");
			ssclThread.setPriority(Thread.MIN_PRIORITY);
			// ssclThread.setDaemon(true);
			ssclThread.start();
		}
		
		/* keep alive clearner */
		keepAliveCleaner = new KeepAliveCleaner();
		keepAliveCleaner.start();
		File fileSessions = getPersistentFile();
		if (fileSessions != null && fileSessions.exists()) {
			BufferedReader bReader = null;
			try {
				bReader = new BufferedReader(new FileReader(fileSessions));
				sessions = HttpSessionContextImpl.restore(bReader, Math.abs(expiredIn) * 60, this);
			} catch (IOException ioe) {
				log("TJWS: IO error in restoring sessions.", ioe);
			} catch (Exception ex) {
				log("TJWS: Unexpected problem in restoring sessions.", ex);
			} finally {
				IOHelper.closeSilently(bReader);
			}
		}
		
		if (sessions == null) {
			sessions = new HttpSessionContextImpl();
		}
		
		// TODO: display address as name and as ip
		log("TJWS httpd " + hostName + " - " + acceptor + " is listening.");
		try {
			while (isRunning()) {
				try {
					final Socket socket = acceptor.accept();
					// TODO consider to use ServeConnection object pool
					// TODO consider req/resp objects pooling
					keepAliveCleaner.addConnection(new ServeConnection(socket, this));
				} catch (IOException ex) {
					log("TJWS: Accept: " + ex);
				} catch (SecurityException ex) {
					log("TJWS: Illegal access: " + ex);
				} catch (IllegalStateException ex) {
					log("TJWS: Illegal state: " + ex);
				}
			}
		} catch (Throwable th) {
			log("TJWS: Unhandled exception: " + th + ", server is terminating.", th);
			if (th instanceof ThreadDeath) {
				throw (Error) th;
			}
			return Status.ERROR;
		} finally {
			if (acceptor != null) {
				try {
					acceptor.destroy();
					acceptor = null;
				} catch (IOException ex) {
					log(ex);
				}
			}
			
			running = false;
			if (ssclThread != null) {
				ssclThread.interrupt();
			}
			
			ssclThread = null;
			keepAliveCleaner.interrupt();
			try {
				keepAliveCleaner.join();
			} catch (InterruptedException ex) {
			}
			
			// clear rest, although should be empty
			keepAliveCleaner.clear();
			keepAliveCleaner = null;
			if (websocketProvider != null) {
				try {
					websocketProvider.destroy();
				} catch (Exception ex) {
					// just in case
					log(ex);
				}
			}
		}
		
		return Status.STOPPED;
	}
	
	/**
	 * Tells the server to stop
	 * 
	 * @throws IOException
	 */
	public void notifyStop() {
		if (acceptor != null) {
			running = false;
			try {
				acceptor.destroy();
				acceptor = null;
			} catch (IOException ioe) {
				log("TJWS:  IO exception at destroying acceptor: " + acceptor, ioe);
			} catch (Exception e) {
				acceptor = null;
				log("TJWS: An exception at destroying acceptor: " + acceptor, e);
			}
		}
	}
	
	public static interface Acceptor {
		/**
		 * 
		 * @param inProperties
		 * @param outProperties
		 * @throws IOException
		 */
		public void init(Map<Object, Object> inProperties, Map<Object, Object> outProperties) throws IOException;
		
		/**
		 * 
		 * @return
		 * @throws IOException
		 */
		public Socket accept() throws IOException;
		
		/**
		 * 
		 * @throws IOException
		 */
		public void destroy() throws IOException;
	}
	
	public static interface AsyncCallback {
		public void notifyTimeout();
		
		public long getTimeout();
	}
	
	public static interface WebsocketProvider {
		public void init(Serve serve);
		
		public void handshake(Socket socket, String path, Servlet servlet, HttpServletRequest request, HttpServletResponse response) throws IOException;
		
		public void upgrade(Socket socket, String path, Servlet servlet, HttpServletRequest request, HttpServletResponse response) throws IOException;
		
		public void destroy();
		
		public void deploy(ServletContext servletContext, List classPathFiles);
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	protected Acceptor createAcceptor() throws IOException {
		String acceptorClass = (String) arguments.get(ARG_ACCEPTOR_CLASS);
		if (acceptorClass == null) {
			acceptorClass = "Acme.Serve.SimpleAcceptor";
		}
		// assured defaulting here
		try {
			log("Loading Acceptor:" + acceptorClass);
			acceptor = (Acceptor) Class.forName(acceptorClass).newInstance();
			log("Acceptor:" + acceptor);
		} catch (InstantiationException ex) {
			log("TJWS: Couldn't instantiate Acceptor, the Server is inoperable!", ex);
		} catch (IllegalAccessException ex) {
			log("TJWS: Error instantiate Acceptor!", ex);
			Constructor<?> objectAcceptor;
			try {
				log("Retrying to load Acceptor:" + acceptorClass);
				objectAcceptor = Class.forName(acceptorClass).getDeclaredConstructor(Utils.EMPTY_CLASSES);
				objectAcceptor.setAccessible(true);
				acceptor = (Acceptor) objectAcceptor.newInstance(Utils.EMPTY_OBJECTS);
			} catch (Throwable th) {
				log("TJWS: Acceptor is not accessable or can't be instantiated, the Server is inoperable!", th);
			}
		} catch (ClassNotFoundException ex) {
			log("TJWS: Acceptor class not found, the Server is inoperable!", ex);
			System.exit(Status.TERMINATED_WITH_ERROR.getStatus());
		}
		
		Map acceptorProperties = new Properties();
		log("TJWS: Init Acceptor:" + acceptor);
		acceptor.init(arguments, acceptorProperties);
		hostName = (String) acceptorProperties.get(ARG_BINDADDRESS);
		
		log("TJWS: Loaded Acceptor:" + acceptor);
		return acceptor;
	}
	
	/**
	 * 
	 * @param hostName
	 */
	public void setHost(String hostName) {
		if (virtuals == null) {
			currentRegistry.set(defaultRegistry);
		} else {
			if (hostName == null) {
				currentRegistry.set(defaultRegistry);
			} else {
				// separate port part
				int colonIndex = hostName.indexOf(':');
				if (colonIndex > 0) {
					hostName = hostName.substring(0, colonIndex);
				}
				
				PathTreeDictionary registry = virtuals.get(hostName.toLowerCase());
				if (registry != null) {
					currentRegistry.set(registry);
				} else {
					currentRegistry.set(defaultRegistry);
				}
			}
		}
	}
	
	// ///////////////// Methods from ServletContext /////////////////////
	
	// / Gets a servlet by name.
	// @param name the servlet name
	// @return null if the servlet does not exist
	public Servlet getServlet(String name) {
		PathTreeDictionary registry = currentRegistry.get();
		if (registry == null) {
			registry = defaultRegistry;
		}
		
		try {
			return (Servlet) registry.get(name)[0];
		} catch (NullPointerException ex) {
			log(ex);
		}
		
		return null;
	}
	
	// / Enumerates the servlets in this context (server). Only servlets that
	// are accessible will be returned. This enumeration always includes the
	// servlet itself.
	public Enumeration getServlets() {
		PathTreeDictionary registry = currentRegistry.get();
		if (registry == null) {
			registry = defaultRegistry;
		}
		
		return registry.elements();
	}
	
	// / Enumerates the names of the servlets in this context (server). Only
	// servlets that are accessible will be returned. This enumeration always
	// includes the servlet itself.
	public Enumeration<String> getServletNames() {
		PathTreeDictionary registry = currentRegistry.get();
		if (registry == null) {
			registry = defaultRegistry;
		}
		
		return registry.keys();
	}
	
	// / Destroys all currently-loaded servlets.
	public synchronized void destroyAllServlets() {
		// log("Entering destroyAllServlets()", new
		// Exception("Entering destroyAllServlets()"));
		// serialize sessions
		// invalidate all sessions
		// TODO consider merging two pieces below, generally if session is
		// stored,
		// it shouldn't be invalidated
		File fileSessions = getPersistentFile();
		if (fileSessions != null && sessions != null) {
			Writer writer = null;
			try {
				writer = new FileWriter(fileSessions);
				sessions.save(writer);
				log("TJWS: Sessions stored.");
			} catch (IOException ioe) {
				log("TJWS: IO error in storing sessions " + ioe);
			} catch (Throwable th) {
				log("TJWS: Unexpected problem in storing sessions " + th);
				if (th instanceof ThreadDeath) {
					throw (ThreadDeath) th;
				}
			} finally {
				IOHelper.closeSilently(writer);
			}
			
			Enumeration<?> e = sessions.keys();
			while (e.hasMoreElements()) {
				Object sid = e.nextElement();
				if (sid != null) {
					AcmeSession as = (AcmeSession) sessions.get(sid);
					if (as != null) {
						as = (AcmeSession) sessions.remove(sid);
						if (as != null && as.isValid()) {
							try {
								as.invalidate();
							} catch (IllegalStateException ex) {
								log(ex);
							}
						}
					}
				}
			}
		}
		
		// destroy servlets
		destroyAll(defaultRegistry);
		if (virtuals != null) {
			Iterator<PathTreeDictionary> itr = virtuals.values().iterator();
			while (itr.hasNext()) {
				destroyAll(itr.next());
			}
		}
		
		// clean access tree
		defaultRegistry = new PathTreeDictionary();
		virtuals = null;
	}
	
	/**
	 * 
	 * @author Rohtash Lakra (rohtash.lakra@devamatre.com)
	 * @author Rohtash Singh Lakra (rohtash.singh@gmail.com)
	 * @created 2018-03-20 08:11:08 PM
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	private class ServletDestroyer implements Runnable {
		/** servletIterator */
		private final Enumeration<Object> servletIterator;
		
		/**
		 * 
		 * @param servletIterator
		 */
		ServletDestroyer(final Enumeration<Object> servletIterator) {
			this.servletIterator = servletIterator;
		}
		
		public void run() {
			if (servletIterator != null) {
				((Servlet) servletIterator.nextElement()).destroy();
			}
		}
	}
	
	/**
	 * 
	 * @param registry
	 */
	private void destroyAll(PathTreeDictionary registry) {
		final Enumeration<Object> servletIterator = registry.elements();
		int dhc = 0;
		while (servletIterator.hasMoreElements()) {
			Thread destroyThread = new Thread(new ServletDestroyer(servletIterator), "Destroy");
			destroyThread.setDaemon(true);
			destroyThread.start();
			try {
				destroyThread.join(DESTROY_TIME_SEC * 1000);
			} catch (InterruptedException e) {
			}
			
			if (destroyThread.isAlive()) {
				log("TJWS: Destroying thread didn't terminate in " + DESTROY_TIME_SEC);
				destroyThread.setName("Destroying took too long " + (dhc++));
			}
		}
	}
	
	protected void setMappingTable(PathTreeDictionary mappingtable) {
		this.mappingtable = mappingtable;
	}
	
	protected void setRealms(PathTreeDictionary realms) {
		this.realms = realms;
	}
	
	AcmeSession getSession(String id) {
		return (AcmeSession) sessions.get(id);
	}
	
	HttpSession createSession() {
		Integer maxActiveSessions = ((Integer) this.arguments.get(ARG_MAX_ACTIVE_SESSIONS));
		if (maxActiveSessions != null && maxActiveSessions.intValue() < sessions.size()) {
			return null;
		}
		
		HttpSession result = new AcmeSession(generateSessionId(), Math.abs(expiredIn) * 60, this, sessions);
		synchronized (sessions) {
			sessions.put(result.getId(), result);
		}
		
		return result;
	}
	
	void removeSession(String id) {
		synchronized (sessions) {
			sessions.remove(id);
		}
	}
	
	/**
	 * Writes logs information in the servet logs.
	 * 
	 * @param message
	 * @param logInAllLoggers
	 */
	public void log(String message, final boolean logInAllLoggers) {
		message = "[" + new Date().toString() + "] " + message;
		if (logInAllLoggers) {
			System.out.println(message);
		}
		logStream.println(message);
	}
	
	/**
	 * @see javax.servlet.ServletContext#log(java.lang.String)
	 */
	public void log(final String message) {
		log(message, true);
	}
	
	public void log(String message, Throwable throwable) {
		if (throwable != null) {
			message += LINE_SEP + IOHelper.toString(throwable);
		}
		log(message);
	}
	
	// / Write a stack trace to the servlet log.
	// @param exception where to get the stack trace
	// @param message the message to log
	public void log(Exception exception, String message) {
		log(message, exception);
	}
	
	/**
	 * 
	 * @param throwable
	 */
	public void log(Throwable throwable) {
		if (throwable != null) {
			log(LINE_SEP + IOHelper.toString(throwable));
		}
	}
	
	// / Applies alias rules to the specified virtual path and returns the
	// corresponding real path. It returns null if the translation
	// cannot be performed.
	// @param path the path to be translated
	public String getRealPath(String path) {
		// System.err.print("[" + path + "]->[");
		path = Utils.canonicalizePath(path);
		if (path != null && mappingtable != null) {
			// try find first sub-path
			Object[] os = mappingtable.get(path);
			// System.err.println("Searching for path: "+path+" found: "+os[0]);
			if (os[0] == null) {
				return null;
			}
			
			int slpos = ((Integer) os[1]).intValue();
			int pl = path.length();
			if (slpos > 0) {
				if (path.length() > slpos) {
					path = path.substring(slpos + 1);
				} else {
					path = "";
				}
			} else if (pl > 0) {
				for (int i = 0; i < pl; i++) {
					char s = path.charAt(i);
					if (s == '/' || s == '\\') {
						continue;
					} else {
						if (i > 0) {
							path = path.substring(i);
						}
						break;
					}
				}
			}
			// System.err.println("Path after processing :"+path+" slash was at
			// "+slpos);
			return new File((File) os[0], path).getPath();
		}
		
		return path;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getContextPath() {
		return "";
	}
	
	// / Returns the MIME type of the specified file.
	// @param file file name whose MIME type is required
	public String getMimeType(String file) {
		int dp = file.lastIndexOf('.');
		if (dp > 0) {
			return mime.getProperty(file.substring(dp + 1).toUpperCase());
		}
		
		return null;
	}
	
	// / Returns the name and version of the web server under which the servlet
	// is running.
	// Same as the CGI variable SERVER_SOFTWARE.
	public String getServerInfo() {
		return Serve.Identification.serverName + " " + Serve.Identification.serverVersion + " (" + Serve.Identification.serverUrl + ")";
	}
	
	// / Returns the value of the named attribute of the network service, or
	// null if the attribute does not exist. This method allows access to
	// additional information about the service, not already provided by
	// the other methods in this interface.
	public Object getAttribute(String name) {
		return attributes.get(name);
	}
	
	// ///////////////// JSDK 2.1 extensions //////////////////////////
	public void removeAttribute(String name) {
		attributes.remove(name);
	}
	
	public void setAttribute(String name, Object object) {
		if (object != null) {
			attributes.put(name, object);
		} else {
			attributes.remove(name);
		}
	}
	
	public Enumeration<String> getAttributeNames() {
		return attributes.keys();
	}
	
	// TODO check webapp servlets to find out conexts for uri
	public ServletContext getContext(String uripath) {
		// only root context supported
		return this;
	}
	
	public int getMajorVersion() {
		// support 2.x
		return 2;
	}
	
	public int getMinorVersion() {
		// support 2.5
		return 5;
	}
	
	// 2.3
	
	/**
	 * Returns a directory-like listing of all the paths to resources within the
	 * web application whose longest sub-path matches the supplied path
	 * argument. Paths indicating subdirectory paths end with a '/'. The
	 * returned paths are all relative to the root of the web application and
	 * have a leading '/'. For example, for a web application containing
	 * <p>
	 * /welcome.html <br>
	 * /catalog/index.html <br>
	 * /catalog/products.html <br>
	 * /catalog/offers/books.html <br>
	 * /catalog/offers/music.html <br>
	 * /customer/login.jsp <br>
	 * /WEB-INF/web.xml <br>
	 * /WEB-INF/classes/com.acme.OrderServlet.class,
	 * <p>
	 * getResourcePaths("/") returns {"/welcome.html", "/catalog/",
	 * "/customer/", "/WEB-INF/"} <br>
	 * getResourcePaths("/catalog/") returns {"/catalog/index.html",
	 * "/catalog/products.html", "/catalog/offers/"}.
	 * <p>
	 * 
	 * @param the
	 *            - partial path used to match the resources, which must start
	 *            with a /
	 * @return a Set containing the directory listing, or null if there are no
	 *         resources in the web application whose path begins with the
	 *         supplied path.
	 * @since Servlet 2.3
	 * 
	 */
	public Set<String> getResourcePaths(String path) {
		String realPath = getRealPath(path);
		if (realPath != null) {
			String[] dirPaths = new File(realPath).list();
			if (dirPaths.length > 0) {
				Set<String> dirPathSet = new HashSet<String>(dirPaths.length);
				for (int i = 0; i < dirPaths.length; i++) {
					dirPathSet.add(dirPaths[i]);
				}
				
				return dirPathSet;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the name of this web application corresponding to this
	 * ServletContext as specified in the deployment descriptor for this web
	 * application by the display-name element.
	 * 
	 * @return The name of the web application or null if no name has been
	 *         declared in the deployment descriptor.
	 * 
	 * @since Servlet 2.3
	 */
	public String getServletContextName() {
		return null;
	}
	
	/**
	 * Returns a URL to the resource that is mapped to a specified path. The
	 * path must begin with a "/" and is interpreted as relative to the current
	 * context root.
	 * 
	 * <p>
	 * This method allows the servlet container to make a resource available to
	 * servlets from any source. Resources can be located on a local or remote
	 * file system, in a database, or in a <code>.war</code> file.
	 * 
	 * <p>
	 * The servlet container must implement the URL handlers and
	 * <code>URLConnection</code> objects that are necessary to access the
	 * resource.
	 * 
	 * <p>
	 * This method returns <code>null</code> if no resource is mapped to the
	 * pathname.
	 * 
	 * <p>
	 * Some containers may allow writing to the URL returned by this method
	 * using the methods of the URL class.
	 * 
	 * <p>
	 * The resource content is returned directly, so be aware that requesting a
	 * <code>.jsp</code> page returns the JSP source code. Use a
	 * <code>RequestDispatcher</code> instead to include results of an
	 * execution.
	 * 
	 * <p>
	 * This method has a different purpose than
	 * <code>java.lang.Class.getResource</code>, which looks up resources based
	 * on a class loader. This method does not use class loaders.
	 * 
	 * @param path
	 *            a <code>String</code> specifying the path to the resource
	 * 
	 * @return the resource located at the named path, or <code>null</code> if
	 *         there is no resource at that path
	 * 
	 * @exception MalformedURLException
	 *                if the pathname is not given in the correct form
	 * 
	 * 
	 */
	public URL getResource(String path) throws MalformedURLException {
		if (path == null || path.length() == 0 || path.charAt(0) != '/') {
			throw new MalformedURLException("Path " + path + " is not in acceptable form.");
		}
		
		File resFile = new File(getRealPath(path));
		if (resFile.exists()) {// TODO get canonical path is more robust
			return new URL("file", "localhost", resFile.getPath());
		}
		
		return null;
	}
	
	/**
	 * Returns the resource located at the named path as an
	 * <code>InputStream</code> object.
	 * 
	 * <p>
	 * The data in the <code>InputStream</code> can be of any type or length.
	 * The path must be specified according to the rules given in
	 * <code>getResource</code>. This method returns <code>null</code> if no
	 * resource exists at the specified path.
	 * 
	 * <p>
	 * Meta-information such as content length and content type that is
	 * available via <code>getResource</code> method is lost when using this
	 * method.
	 * 
	 * <p>
	 * The servlet container must implement the URL handlers and
	 * <code>URLConnection</code> objects necessary to access the resource.
	 * 
	 * <p>
	 * This method is different from
	 * <code>java.lang.Class.getResourceAsStream</code>, which uses a class
	 * loader. This method allows servlet containers to make a resource
	 * available to a servlet from any location, without using a class loader.
	 * 
	 * 
	 * @param path
	 *            a <code>String</code> specifying the path to the resource
	 * 
	 * @return the <code>InputStream</code> returned to the servlet, or
	 *         <code>null</code> if no resource exists at the specified path
	 * 
	 * 
	 */
	public InputStream getResourceAsStream(String path) {
		try {
			return getResource(path).openStream();
		} catch (Exception ex) {
			log(ex);
		}
		
		return null;
	}
	
	public RequestDispatcher getRequestDispatcher(String urlpath) {
		if (urlpath == null || urlpath.length() == 0 || urlpath.charAt(0) != '/') {
			return null;
		}
		
		try {
			return new SimpleRequestDispatcher(urlpath);
		} catch (NullPointerException ex) {
			log(ex);
			return null;
		}
	}
	
	// no way to specify parameters for context
	public String getInitParameter(String param) {
		return null;
	}
	
	public Enumeration getInitParameterNames() {
		return Utils.EMPTY_ENUMERATION;
	}
	
	public RequestDispatcher getNamedDispatcher(String name) {
		// named resources are not supported
		return null;
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized String generateSessionId() {
		secureRandom.nextBytes(uniqer);
		// TODO swap randomly bytes
		return Utils.base64Encode(uniqer);
	}
	
	/**
	 * 
	 * @author Rohtash Lakra (rohtash.lakra@devamatre.com)
	 * @author Rohtash Singh Lakra (rohtash.singh@gmail.com)
	 * @created 2018-03-20 07:45:28 PM
	 * @version 1.0.0
	 * @since 1.0.0
	 */
	protected class ServletRequestWrapper extends HttpServletRequestWrapper {
		
		/** requestDispatcher */
		private final SimpleRequestDispatcher requestDispatcher;
		
		/**
		 * 
		 * @param servletRequest
		 * @param requestDispatcher
		 */
		public ServletRequestWrapper(final HttpServletRequest servletRequest, final SimpleRequestDispatcher requestDispatcher) {
			super(servletRequest);
			this.requestDispatcher = requestDispatcher;
		}
		
		public String getPathInfo() {
			return requestDispatcher.dispatchLen >= requestDispatcher.dispatchPath.length() ? null : requestDispatcher.dispatchPath.substring(requestDispatcher.dispatchLen);
		}
		
		public String getRequestURI() {
			return requestDispatcher.dispatchPath;
		}
		
		public String getQueryString() {
			return requestDispatcher.dispatchQuery;
		}
		
		public String getPathTranslated() {
			// System.out.println("Path t path i: "+getPathInfo()+", dp:
			// "+dispatchPath);
			return getRequest().getRealPath(getPathInfo());
		}
		
		// TODO implement getPathInfo
		
		public String getServletPath() {
			return requestDispatcher.dispatchLen <= 0 ? "" : requestDispatcher.dispatchPath.substring(0, requestDispatcher.dispatchLen);
		}
		
		public synchronized Enumeration getAttributeNames() {
			if (super.getAttribute("javax.servlet.forward.request_uri") == null) {
				setAttribute("javax.servlet.forward.request_uri", super.getRequestURI());
				setAttribute("javax.servlet.forward.context_path", this.getContextPath());
				setAttribute("javax.servlet.forward.servlet_path", super.getServletPath());
				setAttribute("javax.servlet.forward.path_info", super.getPathInfo());
				setAttribute("javax.servlet.forward.query_string", super.getQueryString());
			}
			return super.getAttributeNames();
		}
		
		public Object getAttribute(String name) {
			getAttributeNames(); // here is some overhead
			return super.getAttribute(name);
		}
		
		public String[] getParameterValues(String name) {
			Map params = createParameters();
			String[] result = (String[]) params.get(name);
			if (result != null)
				return result;
			return super.getParameterValues(name);
		}
		
		public String getParameter(String name) {
			Map params = createParameters();
			String[] result = (String[]) params.get(name);
			if (result != null) {
				return result[0];
			}
			return super.getParameter(name);
		}
		
		public Map getParameterMap() {
			HashMap result = new HashMap();
			result.putAll(super.getParameterMap());
			result.putAll(createParameters());
			return result;
		}
		
		public Enumeration getParameterNames() {
			Map params = getParameterMap();
			Hashtable result = new Hashtable();
			result.putAll(params);
			return result.keys();
		}
		
		synchronized protected Map<String, String[]> createParameters() {
			if (requestDispatcher.parameters == null) {
				String query = getQueryString();
				if (query != null) {
					requestDispatcher.parameters = Utils.parseQueryString(query, null);
				} else {
					requestDispatcher.parameters = new Hashtable<String, String[]>();
				}
			}
			
			return requestDispatcher.parameters;
		}
	}
	
	protected class SimpleRequestDispatcher implements RequestDispatcher {
		HttpServlet servlet;
		String dispatchPath;
		String dispatchQuery;
		int dispatchLen;
		Map<String, String[]> parameters;
		
		SimpleRequestDispatcher(String path) {
			PathTreeDictionary registry = (PathTreeDictionary) currentRegistry.get();
			if (registry == null) {
				registry = defaultRegistry;
			}
			Object[] os = registry.get(path);
			servlet = (HttpServlet) os[0];
			// log("Dispatch to: " + path + ", servlet "+servlet);
			if (servlet == null) {
				throw new NullPointerException();
			}
			dispatchLen = ((Integer) os[1]).intValue();
			int qmp = path.indexOf('?');
			
			if (qmp < 0 || qmp >= path.length() - 1) {
				dispatchPath = path;
			} else {
				dispatchPath = path.substring(0, qmp);
				dispatchQuery = path.substring(qmp + 1);
			}
		}
		
		/**
		 * 
		 * @param _request
		 * @param _response
		 * @throws ServletException
		 * @throws java.io.IOException
		 * @see javax.servlet.RequestDispatcher#forward(javax.servlet.ServletRequest,
		 *      javax.servlet.ServletResponse)
		 */
		public void forward(ServletRequest request, ServletResponse response) throws ServletException, java.io.IOException {
			// reset in case of nested
			request.removeAttribute("javax.servlet.forward.request_uri");
			response.reset();
			servlet.service(new ServletRequestWrapper((HttpServletRequest) request, this), response);
			// TODO think when response isn't actual response ServeConnection
			// ((ServeConnection) _response).closeStreams(); // do not allow to
			// continue
		}
		
		/**
		 * 
		 * @param request
		 * @param response
		 * @throws ServletException
		 * @throws IOException
		 * @see javax.servlet.RequestDispatcher#include(javax.servlet.ServletRequest,
		 *      javax.servlet.ServletResponse)
		 */
		public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
			request.removeAttribute("javax.servlet.include.request_uri"); // reset
			// in case of nested
			((Serve.ServeConnection) response).setInInclude(true);
			try {
				servlet.service(new HttpServletRequestWrapper((HttpServletRequest) request) {
					public synchronized Enumeration getAttributeNames() {
						if (super.getAttribute("javax.servlet.include.request_uri") == null) {
							setAttribute("javax.servlet.include.request_uri", dispatchPath);
							setAttribute("javax.servlet.include.context_path", this.getContextPath());
							setAttribute("javax.servlet.include.servlet_path", dispatchLen <= 0 ? "" : dispatchPath.substring(0, dispatchLen));
							setAttribute("javax.servlet.include.path_info", dispatchLen >= dispatchPath.length() ? null : dispatchPath.substring(dispatchLen));
							setAttribute("javax.servlet.include.query_string", dispatchQuery);
						}
						return super.getAttributeNames();
					}
					
					public Object getAttribute(String name) {
						getAttributeNames(); // here is some overhead
						return super.getAttribute(name);
					}
					
				}, response);
			} finally {
				((Serve.ServeConnection) response).setInInclude(false);
			}
		}
		
	}
	
	/**
	 * Keep Alive supporter, JDK 1.4 based for backward compatibility
	 * 
	 * @author Rohtash Singh Lakra
	 * @date 03/16/2018 12:02:56 PM
	 */
	class KeepAliveCleaner extends Thread {
		protected List<ServeConnection> connections;
		protected List<ServeConnection> ingoings;
		protected boolean stopped;
		private boolean noCheckClose;
		
		KeepAliveCleaner() {
			super("KeepAlive cleaner");
			connections = new LinkedList<ServeConnection>();
			ingoings = new LinkedList<ServeConnection>();
			setDaemon(true);
		}
		
		/**
		 * 
		 * @param serveConnection
		 */
		void addConnection(ServeConnection serveConnection) {
			synchronized (ingoings) {
				if (stopped == false) {
					ingoings.add(serveConnection);
				}
			}
		}
		
		void clear() {
			if (stopped == false) {
				new IllegalStateException("Can't clear running cleaner");
			}
			clear(ingoings);
			clear(connections);
		}
		
		/**
		 * Clears the server connections.
		 * 
		 * @param serveConnections
		 */
		private void clear(List<ServeConnection> serveConnections) {
			Iterator<ServeConnection> itr = serveConnections.iterator();
			while (itr.hasNext()) {
				ServeConnection serveConnection = itr.next();
				itr.remove();
				serveConnection.close();
			}
		}
		
		public void run() {
			// int maxUse = getMaxTimesConnectionUse();
			while (true) {
				synchronized (ingoings) {
					Iterator<ServeConnection> itr = ingoings.iterator();
					while (itr.hasNext()) {
						connections.add(itr.next());
						itr.remove();
					}
				}
				
				Iterator<ServeConnection> itr = connections.iterator();
				long ct = System.currentTimeMillis();
				long keepAliveDuration = getKeepAliveDuration();
				// System.err.println("===> keep alive time"+d);
				while (itr.hasNext()) {
					ServeConnection serveConnection = itr.next();
					boolean closed = serveConnection.socket == null;
					if (noCheckClose == false) {
						synchronized (serveConnection) {
							if (serveConnection.socket != null) {
								try {
									closed = ((Boolean) serveConnection.socket.getClass().getMethod("isClosed", Utils.EMPTY_CLASSES).invoke(serveConnection.socket, Utils.EMPTY_OBJECTS)).booleanValue();
								} catch (NoSuchMethodException e) {
									noCheckClose = true;
								} catch (Exception e) {
								}
							}
						}
					}
					
					if (serveConnection.lastRun < serveConnection.lastWait && (closed || serveConnection.keepAlive == false || (ct - serveConnection.lastWait > keepAliveDuration)) || stopped) {
						itr.remove();
						synchronized (serveConnection) {
							if (serveConnection.socket != null) {
								try {
									// System.err.println("Closing
									// socket:"+conn.socket.getClass().getName());
									// // !!!
									// conn.socket.close();
									serveConnection.socket.getInputStream().close();
								} catch (IOException ioe) {
									// ignore
								}
								// System.err.println("done");
							}
						}
						// System.err.println("===> Removing and closing
						// con"+conn+" of "+conn.keepAlive);
					}
					
					if (serveConnection.asyncTimeout > 0) {
						if (ct >= serveConnection.asyncTimeout) {
							if (serveConnection.asyncMode != null) {
								serveConnection.asyncMode.notifyTimeout();
								serveConnection.keepAlive = false;
								if (serveConnection.webSocketUpgrade) {
									serveConnection.asyncMode = null;
								}
								serveConnection.joinAsync();
							}
							
							/*
							 * else if (conn.websocketUpgrade) {
							 * try {
							 * conn.keepAlive = false;
							 * //conn.socket.getChannel().close(); // TODO
							 * perhaps use normal close call
							 * //conn.socket = null;
							 * } catch(Exception e) {
							 * }
							 * }
							 */
						} else {
							long nd = serveConnection.asyncTimeout - ct;
							if (nd < keepAliveDuration) {
								keepAliveDuration = nd;
							}
						}
					}
				}
				
				// TODO stopped can be enough, since clear method
				if (stopped && connections.size() == 0) {
					break;
				}
				
				try {
					sleep(keepAliveDuration);
				} catch (InterruptedException ie) {
					// not thread safe
					stopped = true;
				}
			}
		}
	}
	
	public static interface Identification {
		public static final String serverName = "D. Rogatkin's TJWS (+Android, JSR340, JSR356) https://github.com/drogatkin/TJWS2.git";
		public static final String serverVersion = "Version 1.115 (nightly)";
		public static final String serverUrl = "http://tjws.sourceforge.net";
		public static final String serverIdHtml = "<ADDRESS><A HREF=\"" + serverUrl + "\">" + serverName + " " + serverVersion + "</A></ADDRESS>";
	}
	
	// ////////////////////////////////////////////////////////////////
	
	protected static class ServeConfig implements ServletConfig {
		
		private ServletContext context;
		private Hashtable initParams;
		private String servletName;
		
		public ServeConfig(ServletContext context) {
			this(context, null, "undefined");
		}
		
		public ServeConfig(ServletContext context, Hashtable initParams, String servletName) {
			this.context = context;
			this.initParams = initParams;
			this.servletName = servletName;
		}
		
		// Methods from ServletConfig.
		
		// / Returns the context for the servlet.
		public ServletContext getServletContext() {
			return context;
		}
		
		// / Gets an initialization parameter of the servlet.
		// @param name the parameter name
		public String getInitParameter(String name) {
			// This server supports servlet init params. :)
			if (initParams != null) {
				return (String) initParams.get(name);
			}
			
			return null;
		}
		
		// / Gets the names of the initialization parameters of the servlet.
		// @param name the parameter name
		public Enumeration getInitParameterNames() {
			// This server does:) support servlet init params.
			if (initParams != null) {
				return initParams.keys();
			}
			
			return new Vector().elements();
		}
		
		// 2.2
		public String getServletName() {
			return servletName;
		}
	}
	
	// /////////////////////////////////////////////////////////////////////
	/**
	 * provides request/response
	 */
	public static class ServeConnection implements Runnable, HttpServletRequest, HttpServletResponse {
		
		public final static String WWWFORMURLENCODE = "application/x-www-form-urlencoded";
		public final static String TRANSFERENCODING = "transfer-encoding".toLowerCase();
		public final static String KEEPALIVE = "Keep-Alive".toLowerCase();
		public final static String CONTENT_ENCODING = "Content-Encoding".toLowerCase();
		public final static String CONNECTION = "Connection".toLowerCase();
		public final static String CHUNKED = "chunked";
		public final static String CONTENTLENGTH = "Content-Length".toLowerCase();
		public final static String CONTENTTYPE = "Content-Type".toLowerCase();
		public final static String SETCOOKIE = "Set-Cookie".toLowerCase();
		public final static String HOST = "Host".toLowerCase();
		public final static String COOKIE = "Cookie".toLowerCase();
		public final static String UPGRADE = "Upgrade".toLowerCase();
		public final static String WEBSOCKET = "websocket".toLowerCase();
		public final static String ACCEPT_LANGUAGE = "Accept-Language".toLowerCase();
		public final static String SESSION_COOKIE_NAME = "JSESSIONID";
		public final static String SESSION_URL_NAME = ";$sessionid$"; // ;jsessionid=
		public final static String FORWARDED_FOR = "x-Forwarded-for".toLowerCase();
		public final static String FORWARDED_HOST = "X-Forwarded-Host".toLowerCase();
		public final static String FORWARDED_SERVER = "X-Forwarded-Server".toLowerCase();
		
		private static final Map EMPTYHASHTABLE = new Hashtable();
		private Socket socket;
		private Hashtable<String, Object> sslAttributes;
		private Serve serve;
		private ServletInputStream inputStream;
		private ServletOutputStream outputStream;
		private String scheme;
		private AsyncCallback asyncMode;
		private Thread requestThread;
		private long asyncTimeout;
		private String reqMethod; // == null by default
		private String reqUriPath, reqUriPathUn;
		private String reqProtocol;
		private String charEncoding; // req and resp
		private String remoteUser;
		private String authType;
		// HTTP/1.1 or better
		private boolean oneOne;
		private boolean reqMime;
		private boolean webSocketUpgrade;
		private Vector<String> reqHeaderNames = new Vector<String>();
		private Vector<String> reqHeaderValues = new Vector<String>();
		private Locale locale;
		private int uriLen;
		private HttpServlet servlet;
		
		protected boolean keepAlive = true;
		protected int keepAliveRequestedTime;
		protected long lastRun, lastWait;
		private Vector<Cookie> outCookies;
		private Vector<Cookie> inCookies;
		private String sessionCookieValue, sessionUrlValue, sessionValue,
						reqSessionValue;
		protected String reqQuery;
		private PrintWriter pw;
		private ServletOutputStream rout;
		private Map formParameters;
		private Hashtable<String, Object> attributes = new Hashtable<String, Object>();
		private int resCode = -1;
		private String resMessage;
		
		private Hashtable<String, Object> resHeaderNames = new Hashtable<String, Object>();
		private String[] postCache;
		private boolean headersWritten;
		private MessageFormat accessLogFormat;
		private Object[] logPlaceHolders;
		
		// TODO consider creation an instance per thread in a pool, thread
		// memory can be used
		
		// used for cookie
		private final SimpleDateFormat expdatefmt = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'", Locale.US);
		
		// rfc850-date
		private final SimpleDateFormat rfc850DateFmt = new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss 'GMT'", Locale.US);
		
		// rfc1123-date
		private final SimpleDateFormat headerdateformat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		
		// ASCII date, used in headers
		private final SimpleDateFormat asciiDateFmt = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.US);
		private static final TimeZone tz = TimeZone.getTimeZone("GMT");
		static {
			tz.setID("GMT");
		}
		
		// / Constructor.
		public ServeConnection(Socket socket, Serve serve) {
			LogManager.debug("ServeConnection(" + socket + ", " + serve + ")");
			// serve.log("+++++++"+this);
			// Save arguments.
			this.socket = socket;
			this.serve = serve;
			expdatefmt.setTimeZone(tz);
			headerdateformat.setTimeZone(tz);
			rfc850DateFmt.setTimeZone(tz);
			asciiDateFmt.setTimeZone(tz);
			if (serve.isAccessLogged()) {
				// note format string must be not null
				accessLogFormat = new MessageFormat((String) serve.arguments.get(ARG_ACCESS_LOG_FMT));
				logPlaceHolders = new Object[12];
			}
			try {
				// start handshake
				if (this.socket instanceof SSLSocket) {
					final SSLSocket sslSocket = ((SSLSocket) this.socket);
					LogManager.debug("EnabledCipherSuites:" + IOHelper.toString(sslSocket.getEnabledCipherSuites()));
					sslSocket.setEnabledCipherSuites(sslSocket.getEnabledCipherSuites());
					LogManager.debug("EnabledProtocols:" + IOHelper.toString(sslSocket.getEnabledProtocols()));
					sslSocket.setEnabledProtocols(sslSocket.getEnabledProtocols());
					LogManager.debug("startHandshake() ...");
					sslSocket.startHandshake();
				}
				
				// initialize streams
				inputStream = new ServeInputStream(socket.getInputStream(), this);
				outputStream = new ServeOutputStream(socket.getOutputStream(), this);
			} catch (IOException ex) {
				serve.log(ex);
				close();
				return;
			}
			serve.threadPool.executeThread(this);
		}
		
		private void initSSLAttrs() {
			if (isSSLSocket() && sslAttributes == null) {
				try {
					sslAttributes = new Hashtable<String, Object>();
					Object sslSession = socket.getClass().getMethod("getSession", Utils.EMPTY_CLASSES).invoke(socket, Utils.EMPTY_OBJECTS);
					if (sslSession != null) {
						sslAttributes.put("javax.net.ssl.session", sslSession);
						Method m = sslSession.getClass().getMethod("getCipherSuite", Utils.EMPTY_CLASSES);
						m.setAccessible(true);
						sslAttributes.put("javax.net.ssl.cipher_suite", m.invoke(sslSession, Utils.EMPTY_OBJECTS));
						m = sslSession.getClass().getMethod("getPeerCertificates", Utils.EMPTY_CLASSES);
						m.setAccessible(true);
						sslAttributes.put("javax.net.ssl.peer_certificates", m.invoke(sslSession, Utils.EMPTY_OBJECTS));
					}
				} catch (IllegalAccessException iae) {
					sslAttributes = null;
					// iae.printStackTrace();
				} catch (NoSuchMethodException nsme) {
					sslAttributes = null;
					// nsme.printStackTrace();
				} catch (InvocationTargetException ite) {
					// note we do not clear attributes, because
					// SSLPeerUnverifiedException
					// happens in the last call, when no client sertificate
					// sslAttributes = null;
					// ite.printStackTrace();
				} catch (IllegalArgumentException iae) {
					// sslAttributes = null;
					// iae.printStackTrace();
				}
				// System.err.println("Socket SSL attrs: "+sslAttributes);
			} // else TODO take attributes from SSLEngine when used
		}
		
		/**
		 * it closes stream awaring of keep -alive
		 * 
		 * @throws IOException
		 */
		private void closeStreams() throws IOException {
			// System.err.println("===>CLOSE()");
			IOException ioe = null;
			try {
				if (pw != null) {
					pw.flush();
				} else {
					outputStream.flush();
				}
			} catch (IOException io1) {
				ioe = io1;
			}
			
			try {
				outputStream.close();
			} catch (IOException io1) {
				if (ioe != null) {
					ioe = (IOException) ioe.initCause(io1);
				} else {
					ioe = io1;
				}
			}
			try {
				inputStream.close();
			} catch (IOException io1) {
				if (ioe != null) {
					ioe = (IOException) ioe.initCause(io1);
				} else {
					ioe = io1;
				}
			}
			
			if (ioe != null) {
				serve.log(ioe);
				throw ioe;
			}
		}
		
		/*
		 * open for debug only
		 * protected void finalize() throws Throwable {
		 * System.err.println("Connection object gone"); // !!!
		 * super.finalize();
		 * }
		 */
		
		private void restart() throws IOException {
			// new Exception("RESTART").printStackTrace();
			reqMethod = null;
			reqUriPath = reqUriPathUn = null;
			reqProtocol = null;
			charEncoding = null;
			remoteUser = null;
			authType = null;
			servlet = null;
			oneOne = false;
			reqMime = false;
			
			// considering that clear() works faster than new
			if (reqHeaderNames == null) {
				reqHeaderNames = new Vector<String>();
			} else {
				reqHeaderNames.clear();
			}
			
			if (reqHeaderValues == null) {
				reqHeaderValues = new Vector<String>();
			} else {
				reqHeaderValues.clear();
			}
			
			locale = null;
			uriLen = 0;
			outCookies = null;
			inCookies = null;
			// requested
			sessionCookieValue = null;
			// requested
			sessionUrlValue = null;
			// actual used
			sessionValue = null;
			// requested and used
			reqSessionValue = null;
			reqQuery = null;
			pw = null;
			rout = null;
			formParameters = null;
			
			if (attributes == null) {
				attributes = new Hashtable<String, Object>();
			} else {
				attributes.clear();
			}
			
			if (sslAttributes != null) {
				attributes.putAll(sslAttributes);
			}
			
			resCode = -1;
			resMessage = null;
			resHeaderNames.clear();
			headersWritten = false;
			webSocketUpgrade = false;
			postCache = null;
			if (asyncMode != null) {
				serve.log("TJWS: debug", new Exception("Restarting without clean async mode"));
				asyncMode = null;
			}
			requestThread = Thread.currentThread();
			((ServeInputStream) inputStream).refresh();
			outputStream = new ServeOutputStream(socket.getOutputStream(), this);
			// stream can be still used asyncronously, so
			// ((ServeOutputStream) out).refresh();
		}
		
		// Methods from Runnable.
		public void run() {
			if (socket == null) {
				return;
			}
			
			try {
				do {
					restart();
					// Get the streams.
					parseRequest();
					// serve.log("A>"+asyncMode);
					if (asyncMode != null) {
						asyncTimeout = asyncMode.getTimeout();
						if (asyncTimeout > 0) {
							asyncTimeout += System.currentTimeMillis();
						}
						return;
					}
					
					finalizeRequest();
					if (webSocketUpgrade) {
						outputStream.flush();
						try {
							serve.websocketProvider.upgrade(socket, reqUriPath, servlet, this, this);
							return;
						} catch (Exception ex) {
							serve.log("TJWS: websocket upgrade protocol error: " + ex, ex);
							webSocketUpgrade = false;
						}
					}
				} while (keepAlive && serve.isKeepAlive() && keepAliveRequestedTime < serve.getMaxTimesConnectionUse());
			} catch (IOException ex) {
				/* Print if required in future. */
				// serve.log(ioe);
				if (ex instanceof SocketTimeoutException) {
					/* Keep commented, if running as Android embedded server. */
					if (!IOHelper.isAndroid()) {
						serve.log("Keepalive timeout, asyncMode:" + asyncMode, ex);
					}
				} else {
					String errMsg = ex.getMessage();
					if ((errMsg == null || errMsg.indexOf("ocket closed") < 0) && ex instanceof java.nio.channels.AsynchronousCloseException == false) {
						if (socket != null) {
							serve.log("TJWS: IO error: " + ex + " in processing a request " + (reqUriPathUn == null ? "(NULL)" : reqUriPathUn) + " from " + socket.getInetAddress() + ":" + socket.getLocalPort() + " / " + socket.getClass().getName());
						} else {
							serve.log("TJWS: IO error: " + ex + "(socket NULL)");
						}
					} else {
						synchronized (this) {
							socket = null;
						}
					}
				}
			} finally {
				if (asyncMode == null && !webSocketUpgrade) {
					close();
				}
			}
		}
		
		synchronized final void close() {
			IOHelper.closeSilently(socket);
			socket = null;
		}
		
		private void parseRequest() throws IOException {
			byte[] lineBytes = new byte[4096];
			// TODO put time mark here for start waiting for receiving requests
			lastWait = System.currentTimeMillis();
			// Read the first line of the request.
			socket.setSoTimeout(serve.timeoutKeepAlive);
			int length = inputStream.readLine(lineBytes, 0, lineBytes.length);
			if (length <= 0) {
				if (keepAlive) {
					keepAlive = false;
					// connection seems be closed
				} else {
					problem("Status-Code 400: Bad Request(empty)", SC_BAD_REQUEST);
				}
				
				return;
			}
			
			if (length >= lineBytes.length) {
				problem("Status-Code 414: Request-URI Too Long", SC_REQUEST_URI_TOO_LONG);
				return;
			}
			
			String line = new String(lineBytes, 0, length, IOHelper.UTF_8);
			// serve.log("R>"+line);
			final StringTokenizer lineTokens = new StringTokenizer(line);
			if (lineTokens.hasMoreTokens()) {
				reqMethod = lineTokens.nextToken();
				if (lineTokens.hasMoreTokens()) {
					reqUriPathUn = lineTokens.nextToken();
					// TODO make it only when URL overwrite enabled
					int uop = reqUriPathUn.indexOf(SESSION_URL_NAME);
					if (uop > 0) {
						sessionUrlValue = reqUriPathUn.substring(uop + SESSION_URL_NAME.length());
						reqUriPathUn = reqUriPathUn.substring(0, uop);
						try {
							serve.getSession(sessionUrlValue).userTouch();
						} catch (NullPointerException npe) {
						} catch (IllegalStateException ise) {
						}
					}
					
					if (lineTokens.hasMoreTokens()) {
						reqProtocol = lineTokens.nextToken();
						oneOne = !reqProtocol.toUpperCase().equals("HTTP/1.0");
						reqMime = true;
						
						// Read the rest of the lines.
						String lineSegment;
						while ((lineSegment = ((ServeInputStream) inputStream).readLine(HTTP_MAX_HDR_LEN)) != null) {
							// serve.log("H>"+s);
							if (lineSegment.length() == 0) {
								break;
							}
							int c = lineSegment.indexOf(':', 0);
							if (c > 0) {
								String key = lineSegment.substring(0, c).trim().toLowerCase();
								String value = lineSegment.substring(c + 1).trim();
								reqHeaderNames.addElement(key);
								reqHeaderValues.addElement(value);
							} else {
								serve.log("TJWS: header field '" + lineSegment + "' without ':'");
							}
						}
					} else {
						reqProtocol = "HTTP/0.9";
						oneOne = false;
						reqMime = false;
						// keep alive supposes to be false already
					}
				}
			}
			
			if (reqProtocol == null) {
				problem("Status-Code 400: Malformed request line:" + line, SC_BAD_REQUEST);
				return;
			}
			// Check Host: header in HTTP/1.1 requests.
			if (oneOne) {
				String strHeader = getHeader(HOST);
				if (strHeader == null) {
					problem("Status-Code 400: 'Host' header is missing in HTTP/1.1 request", SC_BAD_REQUEST);
					return;
				}
				strHeader = getHeader(CONNECTION);
				if (strHeader != null) {
					strHeader = strHeader.toLowerCase();
				}
				
				webSocketUpgrade = strHeader != null && strHeader.indexOf(UPGRADE) >= 0 && WEBSOCKET.equalsIgnoreCase(getHeader(UPGRADE));
				keepAlive = "close".equalsIgnoreCase(strHeader) == false;
				if (keepAlive) {
					strHeader = getHeader(KEEPALIVE);
					// serve.log("upgrading protocol:" + strHeader);
					/*
					 * FF specific ? parse value to extract the connection
					 * specific timeoutKeepAlive and maxAliveConnUse todo that
					 * introduce the value in req/resp and copy defaults from
					 * Serve
					 */
				}
			} else {
				keepAlive = false;
			}
			
			// Split off query string, if any.
			int mark = reqUriPathUn.indexOf('?');
			if (mark > -1) {
				if (mark < reqUriPathUn.length() - 1) {
					reqQuery = reqUriPathUn.substring(mark + 1);
				}
				reqUriPathUn = reqUriPathUn.substring(0, mark);
			}
			reqUriPath = Utils.decode(reqUriPathUn, IOHelper.UTF_8);
			// TDOD check if reqUriPathUn starts with http://host:port
			if (CHUNKED.equalsIgnoreCase(getHeader(TRANSFERENCODING))) {
				setHeader(CONTENTLENGTH, null);
				((ServeInputStream) inputStream).chunking(true);
			}
			String contentEncoding = extractEncodingFromContentType(getHeader(CONTENTTYPE));
			// TODO: encoding in request can be invalid, then do default
			setCharacterEncoding(contentEncoding != null ? contentEncoding : IOHelper.UTF_8);
			String contentLength = getHeader(CONTENTLENGTH);
			if (contentLength != null) {
				try {
					((ServeInputStream) inputStream).setContentLength(Long.parseLong(contentLength));
				} catch (NumberFormatException nfe) {
					serve.log("TJWS: Invalid value of input content-length: " + contentLength);
				}
			}
			
			// the code was originally in processing headers loop, however hhas
			// been moved here
			String encoding = getHeader(CONTENT_ENCODING);
			if (encoding != null) {
				if ((encoding.equalsIgnoreCase("gzip") || encoding.equalsIgnoreCase("compressed")) && null != serve.gzipInStreamConstr && ((ServeInputStream) inputStream).compressed(true)) {
				} else {
					problem("Status-Code 415: Unsupported media type:" + encoding, SC_UNSUPPORTED_MEDIA_TYPE);
					return;
				}
			}
			
			if (assureHeaders() && socket.getKeepAlive() == false) {
				socket.setKeepAlive(true);
			}
			
			socket.setSoTimeout(0);
			serve.setHost(getHeader(HOST));
			PathTreeDictionary registry = (PathTreeDictionary) currentRegistry.get();
			lastRun = System.currentTimeMillis();
			try {
				// TODO new
				// SimpleRequestDispatcher(reqUriPathUn).forward((ServletRequest)
				// this, (ServletResponse) this);
				Object[] os = registry.get(reqUriPath);
				if (webSocketUpgrade) {
					webSocketUpgrade = false;
					if (serve.websocketProvider != null) {
						try {
							serve.websocketProvider.handshake(socket, reqUriPath, servlet = (HttpServlet) os[0], this, this);
							webSocketUpgrade = (resCode == SC_SWITCHING_PROTOCOLS);
							// System.err.println("hs code:"+resCode);
						} catch (Exception wse) {
							problem("Can't handshake " + wse, SC_INTERNAL_SERVER_ERROR, wse);
						}
					} else {
						problem("Websocket support is not configured", SC_NOT_IMPLEMENTED);
					}
				} else {
					if (os[0] != null) { // note, os always not null
						// / TODO put time mark here to monitor actual servicing
						
						// System.err.println("Servlet "+os[0]+" for path
						// "+reqUriPath);
						uriLen = ((Integer) os[1]).intValue();
						initSSLAttrs();
						runServlet((HttpServlet) os[0]);
					} else {
						problem("No any servlet found for serving " + reqUriPath, SC_BAD_REQUEST);
					}
				}
			} finally {
				currentRegistry.set(null); // remove
			}
		}
		
		/**
		 * 
		 * @throws IOException
		 */
		private void finalizeRequest() throws IOException {
			if (reqMethod != null && serve.isAccessLogged()) {
				// TODO avoid hardcoded indecies, give them name as LOG_IP = 0,
				// LOG_IDENT = 1, LOG_REMOTE_USER = 2
				// consider caching socket stuff for faster logging
				// {0} {1} {2} [{3,date,dd/MMM/yyyy:HH:mm:ss Z}] \"{4} {5} {6}\"
				// {7,number,#} {8,number} {9} {10}
				// ARG_ACCESS_LOG_FMT
				logPlaceHolders[0] = socket.getInetAddress(); // IP
				logPlaceHolders[1] = "-"; // the RFC 1413 identity of the
				// client, TODO get it from BASIC auth
				logPlaceHolders[2] = remoteUser == null ? "-" : remoteUser; // remote
				// user
				logPlaceHolders[3] = new Date(lastRun); // time stamp
				// {3,date,dd/MMM/yyyy:HH:mm:ss Z} {3,time,}
				logPlaceHolders[4] = reqMethod; // method
				logPlaceHolders[5] = reqUriPathUn; // resource
				logPlaceHolders[6] = reqProtocol; // protocol
				logPlaceHolders[7] = new Integer(resCode); // res code
				logPlaceHolders[8] = new Long(((ServeOutputStream) outputStream).lengthWritten());
				logPlaceHolders[9] = new Integer(socket.getLocalPort());
				logPlaceHolders[10] = serve.isShowReferer() ? getHeader("Referer") : "-";
				logPlaceHolders[11] = serve.isShowUserAgent() ? getHeader("User-Agent") : "-";
				serve.logStream.println(accessLogFormat.format(logPlaceHolders));
			}
			
			if (!webSocketUpgrade) {
				lastRun = 0;
				keepAliveRequestedTime++;
				closeStreams();
			}
		}
		
		private boolean assureHeaders() {
			if (reqMime) {
				setHeader("MIME-Version", "1.0");
			}
			
			setDateHeader("Date", System.currentTimeMillis());
			setHeader("Server", Serve.Identification.serverName + "/" + Serve.Identification.serverVersion);
			if (keepAlive && serve.isKeepAlive() && !webSocketUpgrade) {
				if (reqMime) {
					// set for 1.1 too, because some client do not follow a
					// standard
					setHeader(CONNECTION, KEEPALIVE);
					if (oneOne) {
						setHeader(KEEPALIVE, serve.getKeepAliveParamStr());
					}
				}
				return true;
			} else if (webSocketUpgrade) {
				setHeader(CONNECTION, UPGRADE);
				return true;
			} else {
				setHeader(CONNECTION, "close");
			}
			
			return false;
		}
		
		/**
		 * Runs the current <code>servlet</code>
		 * 
		 * @param servlet
		 * @throws IOException
		 */
		private void runServlet(HttpServlet servlet) throws IOException {
			// Set default response fields.
			setStatus(SC_OK);
			try {
				parseCookies();
				if (reqSessionValue == null) {// not from cookie
					reqSessionValue = sessionUrlValue;
				}
				
				sessionValue = reqSessionValue;
				if (authenificate()) {
					if (servlet instanceof SingleThreadModel) {
						synchronized (servlet) {
							servlet.service((ServletRequest) this, (ServletResponse) this);
						}
					} else {
						servlet.service((ServletRequest) this, (ServletResponse) this);
					}
				}
				// old close
			} catch (UnavailableException ex) {
				if (ex.isPermanent()) {
					serve.unloadServlet(servlet);
					servlet.destroy();
				} else if (ex.getUnavailableSeconds() > 0) {
					serve.log("TJWS: Temporary unavailability feature is not supported " + servlet);
				}
				problem(ex.getMessage(), SC_SERVICE_UNAVAILABLE);
			} catch (ServletException e) {
				serve.log("TJWS: Servlet exception", e);
				Throwable rootCause = e.getRootCause();
				while (rootCause != null) {
					serve.log("Caused by", rootCause);
					if (rootCause instanceof ServletException) {
						rootCause = ((ServletException) rootCause).getRootCause();
					} else {
						/* 1.4 */
						rootCause = rootCause.getCause();
					}
				}
				problem(e.toString(), SC_INTERNAL_SERVER_ERROR);
			} catch (IOException ioe) {
				throw ioe;
			} catch (Exception e) {
				serve.log("TJWS: Unexpected problem running servlet", e);
				problem("Unexpected problem running servlet: " + e.toString(), SC_INTERNAL_SERVER_ERROR);
			} finally {
				// closeStreams();
				// socket will be closed by a caller if no keep-alive
			}
		}
		
		/**
		 * 
		 * @return
		 * @throws IOException
		 */
		private boolean authenificate() throws IOException {
			final Object[] o = serve.realms.get(reqUriPath); // by Niel Markwick
			BasicAuthRealm realm = null;
			if (o != null) {
				realm = (BasicAuthRealm) o[0];
			}
			
			// System.err.println("looking for realm for path "+getPathInfo()+"
			// in "+serve.realms+" found "+realm);
			if (realm == null) {
				return true;
			}
			
			String credentials = getHeader("Authorization");
			if (credentials != null) {
				credentials = Utils.base64Decode(credentials.substring(credentials.indexOf(' ') + 1), getCharacterEncoding());
				int i = credentials.indexOf(':');
				String user = credentials.substring(0, i);
				String password = credentials.substring(i + 1);
				remoteUser = user;
				authType = "BASIC"; // support only basic authenification (FORM,
				// CLIENT_CERT, DIGEST )
				String realPassword = (String) realm.get(user);
				// System.err.println("User "+user+" Password "+password+" real
				// "+realPassword);
				if (realPassword != null && realPassword.equals(password)) {
					return true;
				}
			}
			
			setStatus(SC_UNAUTHORIZED);
			setHeader("WWW-Authenticate", "basic realm=\"" + realm.name() + '"');
			// writeHeaders(); // because sendError() is used
			realSendError();
			return false;
		}
		
		private void problem(String logMessage, int resCode, Throwable t) {
			serve.log("TJWS: " + logMessage, t);
			try {
				sendError(resCode, logMessage);
			} catch (IllegalStateException e) { /* ignore */
			} catch (IOException e) { /* ignore */
			}
		}
		
		private void problem(String logMessage, int resCode) {
			problem(logMessage, resCode, null);
		}
		
		public void setInInclude(boolean set) {
			((ServeOutputStream) outputStream).setInInclude(set);
		}
		
		public void spawnAsync(AsyncCallback setAsync) {
			// System.err.println("SPAWN==");
			asyncMode = setAsync;
		}
		
		public void joinAsync() {
			// System.err.println("JOIN==");
			// new Exception("JOIN==").printStackTrace();
			synchronized (this) {
				if (asyncMode == null) {
					return;
				}
				asyncMode = null;
			}
			// System.err.println("Comparing request with current "+
			// requestThread+" "+ Thread.currentThread());
			if (requestThread == Thread.currentThread()) {
				// detecting if called within request processing thread
				return;
			}
			
			try {
				finalizeRequest();
				if (keepAlive && serve.isKeepAlive() && keepAliveRequestedTime < serve.getMaxTimesConnectionUse()) {
					serve.threadPool.executeThread(this);
				} else {
					close();
				}
			} catch (IOException ioe) {
				serve.log("TJWS: " + ioe);
			}
		}
		
		public void extendAsyncTimeout(long period) {
			if (period > 0) {
				asyncTimeout = System.currentTimeMillis() + period;
			} else if (period < 0) {
				asyncTimeout = 0;
			}
		}
		
		private static final int MAYBEVERSION = 1;
		private static final int INVERSION = 2;
		private static final int OLD_INNAME = 3;
		private static final int OLD_INVAL = 4;
		private static final int INVERSIONNUM = 5;
		private static final int RECOVER = 6;
		private static final int NEW_INNAME = 7;
		private static final int NEW_INVAL = 8;
		private static final int INPATH = 9;
		private static final int MAYBEINPATH = 10;
		private static final int INPATHVALUE = 11;
		private static final int MAYBEPORT = 12;
		private static final int INDOMAIN = 13;
		private static final int MAYBEDOMAIN = 14;
		private static final int INPORT = 15;
		private static final int INDOMAINVALUE = 16;
		private static final int INPORTVALUE = 17;
		
		// TODO see if it can be simplified
		// TODO check if HTTPOnly can be transfered back
		private void parseCookies() throws IOException {
			if (inCookies == null) {
				inCookies = new Vector<Cookie>();
			}
			
			String cookies = getHeader(COOKIE);
			if (LogManager.isDebugEnabled()) {
				serve.log("cookies:" + cookies);
			}
			
			if (cookies == null) {
				return;
			}
			
			try {
				String cookieName = null;
				String cookieValue = null;
				String cookiePath = null;
				String cookieDomain = null;
				// boolean httpOnly = false;
				if (cookies.length() > 300 * 4096) {
					throw new IOException("Cookie string too long:" + cookies.length());
				}
				
				// System.err.println("We received:" + cookies);
				char[] cookiesChars = cookies.toCharArray();
				int state = MAYBEVERSION;
				StringBuffer token = new StringBuffer(256);
				boolean quoted = false;
				for (int i = 0; i < cookiesChars.length; i++) {
					char c = cookiesChars[i];
					switch (state) {
						case MAYBEVERSION:
							if (c != ' ') {
								token.append(c);
								if (c == '$') {
									// RFC 2965
									state = INVERSION;
								} else {
									// RFC 2109
									state = OLD_INNAME;
								}
							}
							break;
						case OLD_INNAME:
							if (c == '=') {
								state = OLD_INVAL;
								cookieName = token.toString();
								token.setLength(0);
							} else if (c != ' ' || token.length() > 0) {
								token.append(c);
							}
							break;
						// TODO introduce val_start. then quoted value and value
						case OLD_INVAL:
							if (quoted == false) {
								if (c == ';') {
									state = OLD_INNAME;
									cookieValue = token.toString();
									token.setLength(0);
									addCookie(cookieName, cookieValue, null, null);
								} else if (c == '"' && token.length() == 0) {
									quoted = true;
								} else {
									token.append(c);
								}
							} else {
								if (c == '"') {
									quoted = false;
								} else {
									token.append(c);
								}
							}
							break;
						case INVERSION:
							if (c == '=') {
								if ("$Version".equals(token.toString())) {
									state = INVERSIONNUM;
								} else {
									// consider name starts with $
									state = OLD_INVAL;
									cookieName = token.toString();
								}
								token.setLength(0);
							} else {
								token.append(c);
							}
							break;
						case INVERSIONNUM:
							if (c == ',' || c == ';') {
								token.setLength(0);
								state = NEW_INNAME;
							} else if (Character.isDigit(c) == false) {
								state = RECOVER;
							} else {
								token.append(c);
							}
							break;
						case NEW_INNAME:
							if (c == '=') {
								state = NEW_INVAL;
								cookieName = token.toString();
								token.setLength(0);
							} else if (c != ' ' || token.length() > 0) {
								token.append(c);
							}
							break;
						case NEW_INVAL:
							if (c == ';') {
								state = MAYBEINPATH;
								cookieValue = token.toString();
								token.setLength(0);
								cookiePath = null;
							} else if (c == ',') {
								state = NEW_INNAME;
								cookieValue = token.toString();
								token.setLength(0);
								addCookie(cookieName, cookieValue, null, null);
							} else {
								token.append(c);
							}
							break;
						case MAYBEINPATH:
							if (c != ' ') {
								token.append(c);
								if (c == '$') {
									state = INPATH;
								} else {
									addCookie(cookieName, cookieValue, null, null);
									state = NEW_INNAME;
								}
							}
							break;
						case INPATH:
							if (c == '=') {
								if ("$Path".equals(token.toString()))
									state = INPATHVALUE;
								else {
									addCookie(cookieName, cookieValue, null, null);
									// consider name starts with $
									state = NEW_INVAL;
									cookieName = token.toString();
								}
								token.setLength(0);
							} else {
								token.append(c);
							}
							break;
						case INPATHVALUE:
							if (c == ',') {
								cookiePath = token.toString();
								state = NEW_INNAME;
								addCookie(cookieName, cookieValue, cookiePath, null);
								token.setLength(0);
							} else if (c == ';') {
								state = MAYBEDOMAIN;
								cookiePath = token.toString();
								token.setLength(0);
							} else {
								token.append(c);
							}
							break;
						case MAYBEDOMAIN:
							if (c != ' ') {
								token.append(c);
								if (c == '$') {
									state = INDOMAIN;
								} else {
									addCookie(cookieName, cookieValue, cookiePath, null);
									state = NEW_INNAME;
								}
							}
							break;
						case INDOMAIN:
							if (c == '=') {
								if ("$Domain".equals(token.toString())) {
									state = INDOMAINVALUE;
								} else {
									addCookie(cookieName, cookieValue, cookiePath, null);
									// consider name starts with $
									state = NEW_INVAL;
									cookieName = token.toString();
								}
								token.setLength(0);
							}
							break;
						case INDOMAINVALUE:
							if (c == ',') {
								state = NEW_INNAME;
								addCookie(cookieName, cookieValue, cookiePath, token.toString());
								token.setLength(0);
							} else if (c == ';') {
								cookieDomain = token.toString();
								state = MAYBEPORT;
							} else {
								token.append(c);
							}
							break;
						case MAYBEPORT:
							if (c != ' ') {
								token.append(c);
								if (c == '$') {
									state = INPORT;
								} else {
									addCookie(cookieName, cookieValue, cookiePath, cookieDomain);
									state = NEW_INNAME;
								}
							}
							break;
						case INPORT:
							if (c == '=') {
								if ("$Port".equals(token.toString()))
									state = INPORTVALUE;
								else {
									addCookie(cookieName, cookieValue, cookiePath, cookieDomain);
									// consider name starts with $
									state = NEW_INVAL;
									cookieName = token.toString();
								}
								token.setLength(0);
							}
							break;
						case INPORTVALUE:
							if (c == ',' || c == ';') {
								int port = Integer.parseInt(token.toString());
								state = NEW_INNAME;
								addCookie(cookieName, cookieValue, cookiePath, cookieDomain);
								token.setLength(0);
							} else if (Character.isDigit(c) == false) {
								state = RECOVER;
							} else {
								token.append(c);
							}
							break;
						case RECOVER:
							serve.log("TJWS: Parsing recover of cookie string " + cookies, null);
							if (c == ';' || c == ',') {
								token.setLength(0);
								state = NEW_INNAME;
							}
							break;
					}
				}
				
				if (state == OLD_INVAL || state == NEW_INVAL) {
					cookieValue = token.toString();
					addCookie(cookieName, cookieValue, null, null);
				} else if (state == INPATHVALUE) {
					addCookie(cookieName, cookieValue, token.toString(), null);
				} else if (state == INDOMAINVALUE) {
					addCookie(cookieName, cookieValue, cookiePath, token.toString());
				} else if (state == INPORTVALUE) {
					addCookie(cookieName, cookieValue, cookiePath, cookieDomain);
				}
			} catch (Error ex) {
				serve.log("TJWS: Error in parsing cookies: " + cookies, ex);
			} catch (Exception ex) {
				serve.log("TJWS: An exception in parsing cookies: " + cookies, ex);
			}
		}
		
		/**
		 * 
		 * @param name
		 * @param value
		 * @param path
		 * @param domain
		 */
		private void addCookie(String name, String value, String path, String domain) {
			if (LogManager.isDebugEnabled()) {
				serve.log("addCookie(" + name + ", " + value + ", " + path + ", " + domain + ")");
			}
			if (SESSION_COOKIE_NAME.equals(name) && sessionCookieValue == null) {
				sessionCookieValue = value;
				try {
					serve.getSession(sessionCookieValue).userTouch();
					reqSessionValue = sessionCookieValue;
				} catch (IllegalStateException ex) {
					if (LogManager.isDebugEnabled()) {
						serve.log(ex);
					}
				} catch (NullPointerException ex) {
					if (LogManager.isDebugEnabled()) {
						serve.log(ex);
					}
				}
			} else {
				final Cookie mCookie = new Cookie(name, value);
				inCookies.addElement(mCookie);
				if (path != null) {
					mCookie.setPath(path);
					if (domain != null) {
						mCookie.setDomain(domain);
					}
				}
			}
		}
		
		// Methods from ServletRequest.
		
		// / Returns the size of the request entity data, or -1 if not known.
		// Same as the CGI variable CONTENT_LENGTH.
		public int getContentLength() {
			try {
				return getIntHeader(CONTENTLENGTH);
			} catch (NumberFormatException nfe) {
				
			}
			return -1;
		}
		
		// / Returns the MIME type of the request entity data, or null if
		// not known.
		// Same as the CGI variable CONTENT_TYPE.
		public String getContentType() {
			return getHeader(CONTENTTYPE);
		}
		
		// / Returns the protocol and version of the request as a string of
		// the form <protocol>/<major version>.<minor version>.
		// Same as the CGI variable SERVER_PROTOCOL.
		public String getProtocol() {
			return reqProtocol;
		}
		
		// / Returns the scheme of the URL used in this request, for example
		// "http", "https", or "ftp". Different schemes have different rules
		// for constructing URLs, as noted in RFC 1738. The URL used to create
		// a request may be reconstructed using this scheme, the server name
		// and port, and additional information such as URIs.
		public String getScheme() {
			if (scheme == null) {
				// lazy stuf dlc
				synchronized (this) {
					if (scheme == null)
						scheme = isSSLSocket() || (serve.proxySSL) ? "https" : "http";
				}
			}
			
			return scheme;
		}
		
		boolean isSSLSocket() {
			return socket.getClass().getName().indexOf("SSLSocket") > 0 || socket.getClass().getName().indexOf("SSLChannel") > 0;
		}
		
		// / Returns the host name of the server as used in the <host> part of
		// the request URI.
		// Same as the CGI variable SERVER_NAME.
		public String getServerName() {
			String serverName = getHeader(HOST);
			if (serverName != null && serverName.length() > 0) {
				int colon = serverName.lastIndexOf(':');
				if (colon >= 0) {
					if (colon < serverName.length()) {
						serverName = serverName.substring(0, colon);
					}
				}
			}
			
			if (serverName == null) {
				if (serve.proxyConfig)
					serverName = getHeader(FORWARDED_SERVER);
				if (serverName == null) {
					try {
						serverName = InetAddress.getLocalHost().getHostName();
					} catch (java.net.UnknownHostException ignore) {
						serverName = "localhost";
					}
				}
			}
			
			int slash = serverName.indexOf("/");
			if (slash >= 0) {
				serverName = serverName.substring(slash + 1);
			}
			return serverName;
		}
		
		// / Returns the port number on which this request was received as used
		// in
		// the <port> part of the request URI.
		// Same as the CGI variable SERVER_PORT.
		public int getServerPort() {
			final String serverName = getHeader(HOST);
			if (!IOHelper.isNullOrEmpty(serverName)) {
				int colon = serverName.indexOf(':');
				if (colon >= 0) {
					try {
						return Integer.parseInt(serverName.substring(colon + 1).trim());
					} catch (NumberFormatException ex) {
						if (LogManager.isDebugEnabled()) {
							serve.log(ex);
						}
					}
				} else {
					if ("https".equals(getScheme())) {
						return 443;
					}
					
					return 80;
				}
			}
			
			return socket.getLocalPort();
		}
		
		// / Returns the Internet Protocol (IP) address of the client or last
		// proxy that sent the request.
		// Same as the CGI variable REMOTE_ADDR.
		public String getRemoteAddr() {
			if (serve.proxyConfig && getHeader(FORWARDED_FOR) != null) {
				return getHeader(FORWARDED_FOR);
			}
			
			return socket.getInetAddress().getHostAddress();
		}
		
		// / Returns the fully qualified name of the client or the last proxy
		// that sent the request.
		//
		// Same as the CGI variable REMOTE_HOST.
		public String getRemoteHost() {
			if (serve.proxyConfig && getHeader(FORWARDED_FOR) != null) {
				// TODO resolve name by IP address from X-Forwarded-For
				return getHeader(FORWARDED_FOR);
			}
			
			String result = socket.getInetAddress().getHostName();
			return result != null ? result : getRemoteAddr();
		}
		
		// / Applies alias rules to the specified virtual path and returns the
		// corresponding real path, or null if the translation can not be
		// performed for any reason. For example, an HTTP servlet would
		// resolve the path using the virtual docroot, if virtual hosting is
		// enabled, and with the default docroot otherwise. Calling this
		// method with the string "/" as an argument returns the document root.
		public String getRealPath(String path) {
			return serve.getRealPath(path);
		}
		
		// / Returns an input stream for reading request data.
		// @exception IllegalStateException if getReader has already been called
		// @exception IOException on other I/O-related errors
		public ServletInputStream getInputStream() throws IOException {
			synchronized (inputStream) {
				if (((ServeInputStream) inputStream).isReturnedAsReader()) {
					throw new IllegalStateException("Already returned as a reader.");
				}
				((ServeInputStream) inputStream).setReturnedAsStream(true);
			}
			
			return inputStream;
		}
		
		// / Returns a buffered reader for reading request data.
		// @exception UnsupportedEncodingException if the character set encoding
		// isn't supported
		// @exception IllegalStateException if getInputStream has already been
		// called
		// @exception IOException on other I/O-related errors
		public BufferedReader getReader() {
			synchronized (inputStream) {
				if (((ServeInputStream) inputStream).isReturnedAsStream()) {
					throw new IllegalStateException("Already returned as a stream.");
				}
				((ServeInputStream) inputStream).setReturnedAsReader(true);
			}
			
			if (charEncoding != null) {
				try {
					return new BufferedReader(new InputStreamReader(inputStream, charEncoding));
				} catch (UnsupportedEncodingException ex) {
					if (LogManager.isDebugEnabled()) {
						serve.log(ex);
					}
				}
			}
			
			return new BufferedReader(new InputStreamReader(inputStream));
		}
		
		private Map assureParametersFromRequest() {
			synchronized (resHeaderNames) { // supposes to not be null
				if (formParameters == null) {
					if ("GET".equals(reqMethod) || "HEAD".equals(reqMethod)) {
						if (reqQuery != null) {
							try {
								formParameters = Utils.parseQueryString(reqQuery, charEncoding);
							} catch (IllegalArgumentException ex) {
								serve.log("TJWS: Exception " + ex + " at parsing 'get|head' data " + reqQuery);
							}
						}
					} else if ("POST".equals(reqMethod)) {
						String contentType = getContentType();
						if (contentType != null && WWWFORMURLENCODE.regionMatches(true, 0, contentType, 0, WWWFORMURLENCODE.length())) {
							if (postCache == null) {
								postCache = new String[1];
								try {
									final InputStream is = getInputStream();
									formParameters = Utils.parsePostData(getContentLength(), is, charEncoding, postCache);
								} catch (Exception ex) {
									serve.log("TJWS: Exception " + ex + " at parsing 'POST' data of length " + getContentLength());
									// TODO propagate the exception ?
									formParameters = EMPTYHASHTABLE;
								}
							} else {
								formParameters = Utils.parseQueryString(postCache[0], charEncoding);
							}
							
							if (!IOHelper.isNullOrEmpty(reqQuery)) {
								formParameters.putAll(Utils.parseQueryString(reqQuery, charEncoding));
							}
						} else if (reqQuery != null) {
							formParameters = Utils.parseQueryString(reqQuery, charEncoding);
						}
					} else {
						throw new IllegalArgumentException("Request parameters are not supported for method:" + reqMethod);
					}
				}
			}
			
			if (formParameters == null) {
				formParameters = EMPTYHASHTABLE;
			}
			
			return formParameters;
		}
		
		// / Returns the parameter names for this request.
		public Enumeration getParameterNames() {
			assureParametersFromRequest();
			return ((Hashtable) formParameters).keys();
		}
		
		// / Returns the value of the specified query string parameter, or null
		// if not found.
		// @param name the parameter name
		public String getParameter(String name) {
			String[] params = getParameterValues(name);
			if (params == null || params.length == 0) {
				return null;
			}
			
			return params[0];
		}
		
		// / Returns the values of the specified parameter for the request as an
		// array of strings, or null if the named parameter does not exist.
		public String[] getParameterValues(String name) {
			assureParametersFromRequest();
			return (String[]) formParameters.get(name);
		}
		
		// / Returns the value of the named attribute of the request, or null if
		// the attribute does not exist. This method allows access to request
		// information not already provided by the other methods in this
		// interface.
		public Object getAttribute(String name) {
			// System.err.println("!!!Get att
			// orig:"+name+"="+attributes.get(name));
			return attributes.get(name);
		}
		
		// Methods from HttpServletRequest.
		
		// / Gets the array of cookies found in this request.
		public Cookie[] getCookies() {
			Cookie[] cookieArray = new Cookie[inCookies.size()];
			inCookies.copyInto(cookieArray);
			return cookieArray;
		}
		
		// / Returns the method with which the request was made. This can be
		// "GET",
		// "HEAD", "POST", or an extension method.
		// Same as the CGI variable REQUEST_METHOD.
		public String getMethod() {
			return reqMethod;
		}
		
		/*******************************************************************************************************************************************************
		 * Returns the part of this request's URL from the protocol name up to
		 * the query string in the first line of the HTTP request. To
		 * reconstruct an URL with a scheme and host, use
		 * HttpUtils.getRequestURL(javax.servlet.http.HttpServletRequest).
		 */
		// / Returns the full request URI.
		public String getRequestURI() {
			return reqUriPathUn;
		}
		
		/**
		 * Reconstructs the URL the client used to make the request. The
		 * returned URL contains a protocol, server name, port number, and
		 * server path, but it does not include query string parameters. <br>
		 * Because this method returns a StringBuffer, not a string, you can
		 * modify the URL easily, for example, to append query parameters.
		 * <p>
		 * This method is useful for creating redirect messages and for
		 * reporting errors.
		 * 
		 * @return a StringBuffer object containing the reconstructed URL
		 * @since 2.3
		 */
		public java.lang.StringBuffer getRequestURL() {
			int port = getServerPort();
			return new StringBuffer().append(getScheme()).append("://").append(getServerName()).append("https".equals(getScheme()) && port == 443 || port == 80 ? "" : ":" + String.valueOf(port)).append(getRequestURI());
		}
		
		// / Returns the part of the request URI that referred to the servlet
		// being
		// invoked.
		// Analogous to the CGI variable SCRIPT_NAME.
		public String getServletPath() {
			// In this server, the entire path is regexp-matched against the
			// servlet pattern, so there's no good way to distinguish which
			// part refers to the servlet.
			return uriLen > 0 ? reqUriPath.substring(0, uriLen) : "";
		}
		
		// / Returns optional extra path information following the servlet path,
		// but
		// immediately preceding the query string. Returns null if not
		// specified.
		// Same as the CGI variable PATH_INFO.
		public String getPathInfo() {
			// In this server, the entire path is regexp-matched against the
			// servlet pattern, so there's no good way to distinguish which
			// part refers to the servlet.
			return uriLen >= reqUriPath.length() ? null : reqUriPath.substring(uriLen);
		}
		
		// / Returns extra path information translated to a real path. Returns
		// null if no extra path information was specified.
		// Same as the CGI variable PATH_TRANSLATED.
		public String getPathTranslated() {
			// In this server, the entire path is regexp-matched against the
			// servlet pattern, so there's no good way to distinguish which
			// part refers to the servlet.
			return getRealPath(getPathInfo());
		}
		
		// / Returns the query string part of the servlet URI, or null if not
		// known.
		// Same as the CGI variable QUERY_STRING.
		public String getQueryString() {
			return reqQuery;
		}
		
		// / Returns the name of the user making this request, or null if not
		// known.
		// Same as the CGI variable REMOTE_USER.
		public String getRemoteUser() {
			return remoteUser;
		}
		
		// / Returns the authentication scheme of the request, or null if none.
		// Same as the CGI variable AUTH_TYPE.
		public String getAuthType() {
			return authType;
		}
		
		// / Returns the value of a header field, or null if not known.
		// Same as the information passed in the CGI variabled HTTP_*.
		// @param name the header field name
		public String getHeader(String name) {
			name = name.toLowerCase();
			int i = -1;
			if (serve.proxyConfig && HOST.equals(name))
				i = reqHeaderNames.indexOf(FORWARDED_HOST);
			if (i < 0)
				i = reqHeaderNames.indexOf(name);
			if (i < 0)
				return null;
			return (String) reqHeaderValues.elementAt(i);
		}
		
		public int getIntHeader(String name) {
			String val = getHeader(name);
			if (val == null)
				return -1;
			return Integer.parseInt(val);
		}
		
		public long getDateHeader(String name) {
			String val = getHeader(name);
			if (val == null)
				return -1;
			try {
				return headerdateformat.parse(val).getTime();
			} catch (ParseException pe) {
				try {
					return rfc850DateFmt.parse(val).getTime();
				} catch (ParseException pe1) {
					try {
						return asciiDateFmt.parse(val).getTime();
					} catch (ParseException pe3) {
						throw new IllegalArgumentException("Value " + val + " can't be converted to Date using any of formats: [" + headerdateformat.toPattern() + "][ " + rfc850DateFmt.toPattern() + "][" + asciiDateFmt.toPattern());
					}
				}
			}
		}
		
		/**
		 * 
		 * @param header
		 * @return
		 */
		public Enumeration<String> getRequestHeaders(String header) {
			Vector<String> result = new Vector<String>();
			int i = -1;
			while ((i = reqHeaderNames.indexOf(header.toLowerCase(), i + 1)) >= 0) {
				result.addElement(reqHeaderValues.elementAt(i));
			}
			return result.elements();
		}
		
		/**
		 * 
		 * @return
		 */
		public Enumeration<String> getRequestHeaderNames() {
			return reqHeaderNames.elements();
		}
		
		// / Gets the current valid session associated with this request, if
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
		public synchronized HttpSession getSession(boolean create) {
			HttpSession result = null;
			if (sessionValue != null) {
				result = serve.getSession(sessionValue);
				if (result != null && ((AcmeSession) result).isValid() == false) {
					serve.removeSession(sessionValue);
					result = null;
				}
				// System.err.println("^^^^^^^req sess: "+sessionValue+",
				// found:"+result);
			}
			
			if (result == null && create) {
				result = serve.createSession();
				if (result != null) {
					sessionValue = result.getId();
				} else {
					throw new RuntimeException("A session can't be created");
				}
				// System.err.println("^~~~~~created: "+sessionValue);
			}
			return result;
		}
		
		// JSDK 2.1
		public HttpSession getSession() {
			return getSession(true);
		}
		
		public boolean isRequestedSessionIdFromURL() {
			return isRequestedSessionIdFromUrl();
		}
		
		// from ServletRequest
		public Enumeration<String> getAttributeNames() {
			return attributes.keys();
		}
		
		/**
		 * Stores an attribute in this request. Attributes are reset between
		 * requests. This method is most often used in conjunction with
		 * RequestDispatcher.
		 * <p>
		 * Attribute names should follow the same conventions as package names.
		 * Names beginning with java.*, javax.*, and com.sun.*, are reserved for
		 * use by Sun Microsystems. If the object passed in is null, the effect
		 * is the same as calling removeAttribute(java.lang.String).
		 * <p>
		 * It is warned that when the request is dispatched from the servlet
		 * resides in a different web application by RequestDispatcher, the
		 * object set by this method may not be correctly retrieved in the
		 * caller servlet.
		 * 
		 * @param name
		 *            - a String specifying the name of the attribute
		 * @param o
		 *            - the Object to be stored
		 */
		public void setAttribute(String key, Object value) {
			// System.err.println("!!!Set att orig:"+key+"="+o);
			// if ("javax.servlet.jsp.jspException".equals(key) && o instanceof
			// Throwable)
			// ((Throwable)o).printStackTrace();
			
			if (value != null) {
				attributes.put(key, value);
			} else {
				attributes.remove(key);
			}
		}
		
		// / Gets the session id specified with this request. This may differ
		// from the actual session id. For example, if the request specified
		// an id for an invalid session, then this will get a new session with
		// a new id.
		public String getRequestedSessionId() {
			return reqSessionValue;
		}
		
		// / Checks whether this request is associated with a session that is
		// valid in the current session context. If it is not valid, the
		// requested session will never be returned from the getSession
		// method.
		public boolean isRequestedSessionIdValid() {
			if (reqSessionValue != null) {
				AcmeSession session = serve.getSession(reqSessionValue);
				return (session != null && session.isValid());
			}
			return false;
		}
		
		/**
		 * Checks whether the session id specified by this request came in as a
		 * cookie. (The requested session may not be one returned by the
		 * getSession method.)
		 */
		public boolean isRequestedSessionIdFromCookie() {
			return sessionCookieValue != null;
		}
		
		// / Checks whether the session id specified by this request came in as
		// part of the URL. (The requested session may not be the one returned
		// by the getSession method.)
		public boolean isRequestedSessionIdFromUrl() {
			return sessionUrlValue != null && sessionCookieValue == null;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.http.HttpServletRequest#authenticate(javax.servlet.http.HttpServletResponse)
		 */
		@Override
		public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
			// TODO Auto-generated method stub
			return false;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.http.HttpServletRequest#getPart(java.lang.String)
		 */
		@Override
		public Part getPart(String arg0) throws IOException, ServletException {
			// TODO Auto-generated method stub
			return null;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.http.HttpServletRequest#getParts()
		 */
		@Override
		public Collection<Part> getParts() throws IOException, ServletException {
			// TODO Auto-generated method stub
			return null;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.http.HttpServletRequest#login(java.lang.String,
		 *      java.lang.String)
		 */
		@Override
		public void login(String arg0, String arg1) throws ServletException {
			// TODO Auto-generated method stub
			
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.http.HttpServletRequest#logout()
		 */
		@Override
		public void logout() throws ServletException {
			// TODO Auto-generated method stub
			
		}
		
		// Methods from ServletResponse.
		
		// / Sets the content length for this response.
		// @param length the content length
		public void setContentLength(int length) {
			if (length >= 0)
				setIntHeader(CONTENTLENGTH, length);
			else
				setHeader(CONTENTLENGTH, null);
		}
		
		// / Sets the content type for this response.
		// @param type the content type
		public void setContentType(String type) {
			setHeader(CONTENTTYPE, type);
		}
		
		// / Returns an output stream for writing response data.
		public ServletOutputStream getOutputStream() {
			synchronized (outputStream) {
				if (rout == null) {
					if (pw != null)
						throw new IllegalStateException("Already returned as a writer");
					rout = outputStream;
				}
			}
			return rout;
		}
		
		// / Returns a print writer for writing response data. The MIME type of
		// the response will be modified, if necessary, to reflect the character
		// encoding used, through the charset=... property. This means that the
		// content type must be set before calling this method.
		// @exception UnsupportedEncodingException if no such encoding can be
		// provided
		// @exception IllegalStateException if getOutputStream has been called
		// @exception IOException on other I/O errors
		public PrintWriter getWriter() throws IOException {
			synchronized (outputStream) {
				if (pw == null) {
					if (rout != null)
						throw new IllegalStateException("Already was returned as servlet output stream");
					String encoding = getCharacterEncoding();
					if (encoding != null)
						pw = new PrintWriter(new OutputStreamWriter(outputStream, encoding));
					else
						pw = new PrintWriter(outputStream);
				}
			}
			return pw;
		}
		
		// / Returns the character set encoding used for this MIME body. The
		// character encoding is either the one specified in the assigned
		// content type, or one which the client understands. If no content
		// type has yet been assigned, it is implicitly set to text/plain.
		public String getCharacterEncoding() {
			final String contenType = (String) resHeaderNames.get(CONTENTTYPE.toLowerCase());
			if (contenType != null) {
				String encoding = extractEncodingFromContentType(contenType);
				if (encoding != null) {
					return encoding;
				}
			}
			
			return charEncoding;
		}
		
		/**
		 * 
		 * @param contenType
		 * @return
		 */
		private String extractEncodingFromContentType(String contenType) {
			if (contenType == null) {
				return null;
			}
			
			int index = contenType.indexOf(';');
			if (index > 0) {
				index = contenType.toLowerCase().indexOf("charset=", index);
				if (index >= 0) {
					contenType = contenType.substring(index + "charset=".length()).trim();
					index = contenType.indexOf(';');
					if (index > 0) {
						contenType = contenType.substring(0, index);
					}
					
					int lenContenType = contenType.length();
					if (lenContenType > 2 && contenType.charAt(0) == '"') {
						return contenType.substring(1, lenContenType - 1);
					}
					return contenType;
				}
			}
			
			return null;
		}
		
		// 2.2 - do not use buffer
		public void flushBuffer() throws java.io.IOException {
			((ServeOutputStream) outputStream).flush();
		}
		
		/**
		 * Clears the content of the underlying buffer in the response without
		 * clearing headers or status code. If the response has been committed,
		 * this method throws an IllegalStateException.
		 * 
		 * @since 2.3
		 */
		public void resetBuffer() {
			((ServeOutputStream) outputStream).reset();
			synchronized (this) {
				// TODO check if stream was flushed
				headersWritten = false;
			}
		}
		
		public int getBufferSize() {
			return ((ServeOutputStream) outputStream).getBufferSize();
		}
		
		public void setBufferSize(int size) {
			((ServeOutputStream) outputStream).setBufferSize(size);
		}
		
		/**
		 * Returns a boolean indicating if the response has been committed. A
		 * commited response has already had its status code and headers
		 * written.
		 * 
		 * @return a boolean indicating if the response has been committed
		 * @see setBufferSize(int), getBufferSize(), flushBuffer(), reset()
		 */
		// a caller should think about syncronization
		public boolean isCommitted() {
			return headersWritten && ((ServeOutputStream) outputStream).lengthWritten() > 0;
		}
		
		/**
		 * Clears any data that exists in the buffer as well as the status code
		 * and headers. If the response has been committed, this method throws
		 * an IllegalStateException.
		 * 
		 * @throws java.lang.IllegalStateException
		 *             - if the response has already been committed
		 * @see setBufferSize(int), getBufferSize(), flushBuffer(),
		 *      isCommitted()
		 */
		public void reset() throws IllegalStateException {
			// new Exception("RESET").printStackTrace();
			if (!isCommitted()) {
				if (outCookies != null) {
					outCookies.clear();
				}
				resHeaderNames.clear();
				pw = null;
				rout = null;
				((ServeOutputStream) outputStream).reset();
				assureHeaders();
			} else {
				throw new IllegalStateException("Header have already been committed.");
			}
		}
		
		/**
		 * Sets the locale of the response, setting the headers (including the
		 * Content-Type's charset) as appropriate. This method should be called
		 * before a call to getWriter(). By default, the response locale is the
		 * default locale for the server.
		 * 
		 * @param loc
		 *            - the locale of the response
		 * @see getLocale()
		 */
		public void setLocale(java.util.Locale locale) {
			this.locale = locale;
		}
		
		/**
		 * For request: Returns the preferred Locale that the client will accept
		 * content in, based on the Accept-Language header. If the client
		 * request doesn't provide an Accept-Language header, this method
		 * returns the default locale for the server.
		 * 
		 * For response: Returns the locale specified for this response using
		 * the setLocale(java.util.Locale) method. Calls made to setLocale after
		 * the response is committed have no effect. If no locale has been
		 * specified, the container's default locale is returned.
		 */
		public java.util.Locale getLocale() {
			if (locale != null) {
				return locale;
			}
			
			Enumeration itr = getLocales();
			if (itr.hasMoreElements()) {
				return (Locale) itr.nextElement();
			}
			
			return Locale.getDefault();
		}
		
		/**
		 * Returns an Enumeration of Locale objects indicating, in decreasing
		 * order starting with the preferred locale, the locales that are
		 * acceptable to the client based on the Accept-Language header. If the
		 * client request doesn't provide an Accept-Language header, this method
		 * returns an Enumeration containing one Locale, the default locale for
		 * the server.
		 */
		public Enumeration getLocales() {
			// TODO: cache result
			final String acceptLanguage = getHeader(ACCEPT_LANGUAGE);
			if (LogManager.isDebugEnabled()) {
				serve.log("acceptLanguage:" + acceptLanguage);
			}
			
			final TreeSet<LocaleWithWeight> localeWithWeights = new TreeSet<LocaleWithWeight>();
			if (acceptLanguage != null) {
				final StringTokenizer sTokenizer = new StringTokenizer(acceptLanguage, ";", false);
				try {
					while (sTokenizer.hasMoreTokens()) {
						final String languages = sTokenizer.nextToken(";");
						// System.err.println("Langs:"+langs);
						String strEqual = sTokenizer.nextToken(";=");
						// System.err.println("q:"+q);
						strEqual = sTokenizer.nextToken("=,");
						// System.err.println("q:"+q);
						float weight = 0;
						try {
							weight = Float.valueOf(strEqual).floatValue();
						} catch (NumberFormatException ex) {
							if (LogManager.isDebugEnabled()) {
								serve.log(ex);
							}
						}
						
						if (weight > 0) {
							final StringTokenizer langTokenizer = new StringTokenizer(languages, ", ", false);
							while (langTokenizer.hasMoreTokens()) {
								final String lan = langTokenizer.nextToken();
								int dIndex = lan.indexOf('-');
								if (dIndex < 0) {
									// 1. 4
									localeWithWeights.add(new LocaleWithWeight(new Locale(lan.trim()), weight));
								} else {
									localeWithWeights.add(new LocaleWithWeight(new Locale(lan.substring(0, dIndex), lan.substring(dIndex + 1).trim().toUpperCase()), weight));
								}
							}
						}
					}
				} catch (NoSuchElementException ex) {
					// can't parse
					if (LogManager.isDebugEnabled()) {
						serve.log(ex);
					}
				}
			}
			
			if (localeWithWeights.size() == 0) {
				localeWithWeights.add(new LocaleWithWeight(Locale.getDefault(), 1));
			}
			
			return new AcceptLocaleEnumeration(localeWithWeights);
		}
		
		/**
		 * Overrides the name of the character encoding used in the body of this
		 * request. This method must be called prior to reading request
		 * parameters or reading input using getReader().
		 * 
		 * @param a
		 *            - String containing the name of the chararacter encoding.
		 * @throws java.io.UnsupportedEncodingException
		 *             - if this is not a valid encoding
		 * @since JSDK 2.3
		 */
		public void setCharacterEncoding(String _enc) {
			// TODO: check if encoding is valid
			// TODO separate encoding came from page and set programatically
			charEncoding = _enc;
			synchronized (this) {
				formParameters = null;
			}
		}
		
		public void addDateHeader(String header, long date) {
			addHeader(header, headerdateformat.format(new Date(date)));
		}
		
		public void addHeader(String header, String value) {
			header = header.trim().toLowerCase();
			Object object = resHeaderNames.get(header);
			if (object == null) {
				setHeader(header, value);
			} else {
				if (object instanceof String[]) {
					String[] oldVal = (String[]) object;
					String[] newVal = new String[oldVal.length + 1];
					System.arraycopy(oldVal, 0, newVal, 0, oldVal.length);
					newVal[oldVal.length] = value;
					resHeaderNames.put(header, newVal);
				} else if (object instanceof String) {
					String[] newVal = new String[2];
					newVal[0] = (String) object;
					newVal[1] = value;
					resHeaderNames.put(header, newVal);
				} else
					throw new RuntimeException("Invalid content of header hash - " + object.getClass().getName());
			}
		}
		
		public void addIntHeader(String header, int value) {
			addHeader(header, Integer.toString(value));
		}
		
		public RequestDispatcher getRequestDispatcher(String urlpath) {
			if (urlpath.length() > 0 && urlpath.charAt(0) != '/') {
				String dispatchPath = getContextPath();
				String pathInfo = getPathInfo();
				String servletPath = getServletPath();
				;
				if (pathInfo != null) {
					dispatchPath += servletPath;
					int slp = pathInfo.indexOf('/', 1);
					if (slp > 0) // can it ever happen?
						dispatchPath += pathInfo.substring(0, slp - 1);
				} else {
					int spsp = servletPath.lastIndexOf('/');
					if (spsp >= 0)
						dispatchPath += servletPath.substring(0, spsp);
				}
				// serve.log("Dispatch path:"+dispatchPath);
				urlpath = dispatchPath + '/' + urlpath;
			}
			return serve.getRequestDispatcher(urlpath);
		}
		
		public boolean isSecure() {
			return "https".equals(getScheme());
		}
		
		public void removeAttribute(String name) {
			attributes.remove(name);
		}
		
		// only root context supported
		public String getContextPath() {
			return "";
		}
		
		public Principal getUserPrincipal() {
			return null;
		}
		
		public boolean isUserInRole(String user) {
			return false;
		}
		
		/**
		 * Returns a java.util.Map of the parameters of this request. Request
		 * parameters are extra information sent with the request. For HTTP
		 * servlets, parameters are contained in the query string or posted form
		 * data.
		 * 
		 * @return an immutable java.util.Map containing parameter names as keys
		 *         and parameter values as map values. The keys in the parameter
		 *         map are of type String. The values in the parameter map are
		 *         of type String array.
		 * @since 2.3
		 */
		public Map getParameterMap() {
			assureParametersFromRequest();
			return formParameters;
		}
		
		// Methods from HttpServletResponse.
		
		// / Adds the specified cookie to the response. It can be called
		// multiple times to set more than one cookie.
		public void addCookie(Cookie cookie) {
			if (outCookies == null) {
				outCookies = new Vector<Cookie>();
			}
			
			outCookies.addElement(cookie);
		}
		
		// / Checks whether the response message header has a field with the
		// specified name.
		public boolean containsHeader(String name) {
			return resHeaderNames.contains(name);
		}
		
		// JSDK 2.1 extension
		public String encodeURL(String url) {
			int uop = url.indexOf(SESSION_URL_NAME);
			// TODO not robust enough
			if (uop > 0) {
				url = url.substring(0, uop);
			}
			
			if (sessionValue == null || isRequestedSessionIdFromCookie()) {
				return url;
			}
			
			try {
				new URL(url); // for testing syntac
				int ehp = url.indexOf('/');
				if (ehp < 0) {
					ehp = url.indexOf('?');
				}
				if (ehp < 0) {
					ehp = url.indexOf('#');
				}
				if (ehp < 0) {
					ehp = url.length();
				}
				if (url.regionMatches(true, 0, getRequestURL().toString(), 0, ehp) == false) {
					return url;
				}
			} catch (MalformedURLException e) {
			}
			
			return url + SESSION_URL_NAME + sessionValue;
		}
		
		public String encodeRedirectURL(String url) {
			return encodeURL(url);
		}
		
		/**
		 * Returns the Internet Protocol (IP) source port of the client or last
		 * proxy that sent the request.
		 * 
		 * @return an integer specifying the port number
		 * 
		 * @since 2.4
		 */
		public int getRemotePort() {
			// TODO not quite robust
			return getServerPort();
		}
		
		/**
		 * Returns the host name of the Internet Protocol (IP) interface on
		 * which the request was received.
		 * 
		 * @return a <code>String</code> containing the host name of the IP on
		 *         which the request was received.
		 * 
		 * @since 2.4
		 */
		public String getLocalName() {
			InetAddress localAddress = socket.getLocalAddress();
			/* 1.4 */
			return (localAddress == null ? null : localAddress.getCanonicalHostName());
		}
		
		/**
		 * Returns the Internet Protocol (IP) address of the interface on which
		 * the request was received.
		 * 
		 * @return a <code>String</code> containing the IP address on which the
		 *         request was received.
		 * 
		 * @since 2.4
		 * 
		 */
		public String getLocalAddr() {
			InetAddress localAddress = socket.getLocalAddress();
			return (localAddress == null ? null : localAddress.getHostAddress());
		}
		
		/**
		 * Returns the Internet Protocol (IP) port number of the interface on
		 * which the request was received.
		 * 
		 * @return an integer specifying the port number
		 * 
		 * @since 2.4
		 */
		public int getLocalPort() {
			return socket.getLocalPort();
		}
		
		// / Sets the status code and message for this response.
		// @param resCode the status code
		// @param resMessage the status message
		public void setStatus(int resCode, String resMessage) {
			// if (((ServeOutputStream) out).isInInclude())
			// return;
			this.resCode = resCode;
			this.resMessage = resMessage;
		}
		
		// / Sets the status code and a default message for this response.
		// @param resCode the status code
		public void setStatus(int resCode) {
			switch (resCode) {
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
					setStatus(resCode, "Request URI too long");
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
				case 207:
					setStatus(resCode, "Multi Status");
					break;
				default:
					setStatus(resCode, "");
					break;
			}
		}
		
		// / Sets the value of a header field.
		// @param name the header field name
		// @param value the header field value
		public void setHeader(String header, String value) {
			header = header.trim().toLowerCase(); // normilize header
			if (value == null) {
				resHeaderNames.remove(header);
			} else {
				resHeaderNames.put(header, value);
				// if (header.equals(CONTENTTYPE)) {
				// String enc = extractEncodingFromContentType(value);
				// if (enc != null)
				// setCharacterEncoding(enc);
				// }
			}
		}
		
		// / Sets the value of an integer header field.
		// @param name the header field name
		// @param value the header field integer value
		public void setIntHeader(String header, int value) {
			setHeader(header, Integer.toString(value));
		}
		
		// / Sets the value of a long header field.
		// @param name the header field name
		// @param value the header field long value
		public void setLongHeader(String header, long value) {
			setHeader(header, Long.toString(value));
		}
		
		// / Sets the value of a date header field.
		// @param name the header field name
		// @param value the header field date value
		public void setDateHeader(String header, long value) {
			setHeader(header, headerdateformat.format(new Date(value)));
		}
		
		// / Writes the status line and message headers for this response to the
		// output stream.
		// @exception IOException if an I/O error has occurred
		void writeHeaders() throws IOException {
			synchronized (this) {
				// TODO: possible to write trailer when chunked out,
				// so chunked out should be global flag
				if (headersWritten)
					return;
				// new Exception("headers").printStackTrace();
				headersWritten = true;
			}
			
			if (reqMime) {
				boolean chunked_out = false;
				long contentLen = -1;
				if (resMessage.length() < 256) {
					outputStream.println(reqProtocol + " " + resCode + " " + resMessage.replace('\r', '/').replace('\n', '/'));
				} else {
					outputStream.println(reqProtocol + " " + resCode + " " + resMessage.substring(0, 255).replace('\r', '/').replace('\n', '/'));
				}
				
				final Enumeration<String> headerNames = resHeaderNames.keys();
				while (headerNames.hasMoreElements()) {
					final String name = headerNames.nextElement();
					// skip header until make decision
					if (CONNECTION.equals(name) || KEEPALIVE.equals(name)) {
						continue;
					}
					
					final Object headerValue = resHeaderNames.get(name);
					if (headerValue instanceof String) {
						String value = (String) headerValue;
						if (value != null) {// just in case
							if (CONTENTTYPE.equals(name)) {
								if (charEncoding != null && value.startsWith("text/")) {
									int p = value.indexOf(';');
									if (p > 0) {
										value = value.substring(0, p);
									}
									value += "; charset=" + charEncoding;
								}
								
								// TODO check locale and can take from it as
								// well based on mapping locale charset
							}
							// some overhead can be here for checking every
							// header, so possibly do checks after loop
							if (CONTENTLENGTH.equals(name)) {
								if (contentLen < 0)
									try {
										contentLen = Long.parseLong(value);
									} catch (NumberFormatException nfe) {
									}
							} else
								outputStream.println(name + ": " + value);
							if (chunked_out == false) {
								if (TRANSFERENCODING.equals(name) && CHUNKED.equals(value)) {
									chunked_out = true;
								}
							}
						}
					} else if (headerValue instanceof String[]) {
						String[] values = (String[]) headerValue;
						if ("set-cookie".equals(name)) {
							for (int i = 0; i < values.length; i++) {
								outputStream.print(name);
								outputStream.print(": ");
								outputStream.println(values[i]);
							}
						} else {
							outputStream.print(name + ": " + values[0]);
							for (int i = 1; i < values.length; i++) {
								outputStream.print("," + values[i]);
							}
							outputStream.println();
						}
					}
				}
				
				StringBuffer setCookieBuilder = null;
				StringBuffer cookieBuilderVersion1 = null;
				Cookie cookie = null;
				// add session cookie
				if (sessionValue != null) {
					HttpSession session = serve.getSession(sessionValue);
					if (session != null) {
						if (((AcmeSession) session).isValid()) {
							if (session.isNew()) {
								cookie = new AcmeCookie(SESSION_COOKIE_NAME, sessionValue);
								if (serve.expiredIn < 0) {
									cookie.setMaxAge(Math.abs(serve.expiredIn) * 60);
								}
								
								((AcmeCookie) cookie).setHttpOnly(serve.httpSessCookie);
								if (serve.secureSessCookie) {
									((AcmeCookie) cookie).setSecure(true);
								}
								
								ServletContext sc = ((AcmeSession) session).getServletContext();
								try {
									String cp = (String) sc.getClass().getMethod("getContextPath", Utils.EMPTY_CLASSES).invoke(sc, Utils.EMPTY_OBJECTS);
									if (cp.length() == 0) {
										cp = "/";
									}
									cookie.setPath(cp);
								} catch (Exception e) {
									
								}
								addCookie(cookie);
							}
						} else {
							cookie = new AcmeCookie(SESSION_COOKIE_NAME, "");
							cookie.setMaxAge(0);
							((AcmeCookie) cookie).setHttpOnly(serve.httpSessCookie);
							addCookie(cookie);
						}
					}
				}
				
				// how to remove a cookie
				// cc = new Cookie(cookieName, "");
				// cc.setMaxAge(0);
				//
				for (int i = 0; outCookies != null && i < outCookies.size(); i++) {
					cookie = (Cookie) outCookies.elementAt(i);
					if (cookie.getSecure() && isSecure() == false) {
						continue;
					}
					
					int version = cookie.getVersion();
					boolean httpOnly = false;
					try {
						httpOnly = Boolean.TRUE.equals(cookie.getClass().getMethod("isHttpOnly", Utils.EMPTY_CLASSES).invoke(cookie, Utils.EMPTY_OBJECTS));
					} catch (Exception ex) {
						// ignore me!
					}
					
					String token;
					if (version > 1) {
						if (cookieBuilderVersion1 == null) {
							cookieBuilderVersion1 = new StringBuffer(SETCOOKIE + "2: ");
						} else {
							cookieBuilderVersion1.append(',');
						}
						cookieBuilderVersion1.append(cookie.getName());
						cookieBuilderVersion1.append("=\"");
						cookieBuilderVersion1.append(cookie.getValue()).append('"');
						token = cookie.getComment();
						if (token != null) {
							cookieBuilderVersion1.append("; Comment=\"").append(token).append('"');
						}
						token = cookie.getDomain();
						if (token != null) {
							cookieBuilderVersion1.append("; Domain=\"").append(token).append('"');
						}
						if (cookie.getMaxAge() >= 0) {
							cookieBuilderVersion1.append("; Max-Age=\"").append(cookie.getMaxAge()).append('"');
						}
						token = cookie.getPath();
						if (token != null) {
							cookieBuilderVersion1.append("; Path=\"").append(token).append('"');
						}
						if (cookie.getSecure()) {
							cookieBuilderVersion1.append("; Secure");
						}
						if (httpOnly) {
							cookieBuilderVersion1.append("; HttpOnly");
						}
						cookieBuilderVersion1.append("; Version=\"").append(version).append('"');
					} else {
						if (setCookieBuilder == null) {
							setCookieBuilder = new StringBuffer(SETCOOKIE + ": ");
						} else {
							// for IE not
							setCookieBuilder.append("\r\n" + SETCOOKIE + ": ");
						}
						setCookieBuilder.append(cookie.getName());
						setCookieBuilder.append('=');
						setCookieBuilder.append(cookie.getValue());// .append('"');
						if (!IOHelper.isNullOrEmpty(cookie.getDomain())) {
							setCookieBuilder.append("; domain=" + cookie.getDomain());
						}
						if (cookie.getMaxAge() >= 0) {
							setCookieBuilder.append("; expires=");
							setCookieBuilder.append(expdatefmt.format(new Date(new Date().getTime() + 1000l * cookie.getMaxAge())));
						}
						
						if (!IOHelper.isNullOrEmpty(cookie.getPath())) {
							setCookieBuilder.append("; path=" + cookie.getPath());
						}
						if (cookie.getSecure()) {
							setCookieBuilder.append("; secure");
						}
						
						if (httpOnly) {
							setCookieBuilder.append("; HttpOnly");
						}
					}
				}
				if (setCookieBuilder != null) {
					outputStream.println(setCookieBuilder.toString());
					// System.err.println("We sent cookies: " + sb);
				}
				
				if (cookieBuilderVersion1 != null) {
					outputStream.println(cookieBuilderVersion1.toString());
					// System.err.println("We sent cookies 2: " + sb2);
				}
				
				if (!webSocketUpgrade)
					// setHeader(KEEPALIVE, "timeout=30000");
					if (chunked_out == false) {
						if (contentLen < 0)
							if (serve.isKeepAlive() && oneOne) {
								if ((resCode != SC_NO_CONTENT && !"HEAD".equals(reqMethod)) || resCode != SC_NOT_MODIFIED) {
									outputStream.println(TRANSFERENCODING + ": " + CHUNKED);
									chunked_out = true;
								}
							} else {
								keepAlive = false;
								setHeader(CONNECTION, "close");
								setHeader(KEEPALIVE, null);
							}
						else {
							outputStream.print(CONTENTLENGTH);
							outputStream.print(": ");
							outputStream.println(String.valueOf(contentLen));
						}
					}
				// process keep alive headers
				Object object = resHeaderNames.get(CONNECTION);
				if (object instanceof String) {
					outputStream.println(CONNECTION + ": " + object);
				}
				object = resHeaderNames.get(KEEPALIVE);
				if (object instanceof String) {
					outputStream.println(KEEPALIVE + ": " + object);
				}
				outputStream.println();
				outputStream.flush();
				if (resCode == SC_NO_CONTENT || resCode == SC_NOT_MODIFIED || "HEAD".equals(reqMethod)) {
					outputStream.close();
				} else {
					((ServeOutputStream) outputStream).setChunked(chunked_out);
				}
			}
		}
		
		// / Writes an error response using the specified status code and
		// message.
		// @param resCode the status code
		// @param resMessage the status message
		// @exception IOException if an I/O error has occurred
		public void sendError(int resCode, String resMessage) throws IOException {
			setStatus(resCode, resMessage);
			realSendError();
		}
		
		// / Writes an error response using the specified status code and a
		// default
		// message.
		// @param resCode the status code
		// @exception IOException if an I/O error has occurred
		public void sendError(int resCode) throws IOException {
			setStatus(resCode);
			realSendError();
		}
		
		private void realSendError() throws IOException {
			if (isCommitted())
				throw new IllegalStateException("Can not send an error (" + resCode + ") - " + resMessage + ", headers have been already written");
			// if (((ServeOutputStream) out).isInInclude()) // ignore
			// return;
			setContentType("text/html");
			StringBuffer sb = new StringBuffer(100);
			int lsp = resMessage.indexOf('\n');
			sb.append("<HTML><HEAD>").append("<TITLE>" + resCode + " " + (lsp < 0 ? resMessage : resMessage.substring(0, lsp)) + "</TITLE>").append("</HEAD><BODY " + BGCOLOR).append("><H2>" + resCode + " " + (lsp < 0 ? resMessage : resMessage.substring(0, lsp)) + "</H2>");
			if (lsp > 0)
				sb.append("<PRE>").append(Utils.htmlEncode(resMessage.substring(lsp), false)).append("</PRE>");
			sb.append("<HR>");
			sendEnd(sb);
		}
		
		// / Sends a redirect message to the client using the specified redirect
		// location URL.
		// @param location the redirect location URL
		// @exception IOException if an I/O error has occurred
		public void sendRedirect(String location) throws IOException {
			if (isCommitted())
				throw new IllegalStateException("Can not redirect, headers have been already written");
			if (location.indexOf(":/") < 0) { // relative
				String portString = "";
				if ("https".equalsIgnoreCase(getScheme())) {
					if (getServerPort() != 443)
						portString = ":" + getServerPort();
				} else if (getServerPort() != 80)
					portString = ":" + getServerPort();
				
				if (location.length() > 0 && location.charAt(0) == '/') {
					location = getScheme() + "://" + getServerName() + portString + location;
				} else {
					int sp = reqUriPathUn.lastIndexOf('/');
					String uri;
					if (sp < 0) {
						uri = reqUriPathUn + '/';
						sp = uri.length();
					} else {
						uri = reqUriPathUn;
						sp++;
					}
					location = getScheme() + "://" + getServerName() + portString + uri.substring(0, sp) + location;
				}
			}
			// serve.log("location:"+location);
			setHeader("Location", location);
			setStatus(SC_MOVED_TEMPORARILY);
			setContentType("text/html");
			StringBuffer sb = new StringBuffer(200);
			sb.append("<HTML><HEAD>" + "<TITLE>" + SC_MOVED_TEMPORARILY + " Moved</TITLE>" + "</HEAD><BODY " + BGCOLOR + "><H2>" + SC_MOVED_TEMPORARILY + " Moved</H2>" + "This document has moved <a href=\"" + location + "\">here.<HR>");
			sendEnd(sb);
		}
		
		private void sendEnd(StringBuffer sendContent) throws IOException {
			sendContent.append(Identification.serverIdHtml);
			sendContent.append("</BODY></HTML>");
			setContentLength(sendContent.length());
			// to avoid further out
			if (!oneOne) {
				keepAlive = false;
				setHeader(CONNECTION, "close");
				setHeader(KEEPALIVE, null);
			}
			outputStream.print(sendContent.toString());
			outputStream.close();
		}
		
		// URL rewriting
		// http://www.myserver.com/catalog/index.html;jsessionid=mysession1928
		// like:
		// http://www.sun.com/2001-0227/sunblade/;$sessionid$AD5RQ0IAADJAZAMTA1LU5YQ
		
		// / Encodes the specified URL by including the session ID in it, or, if
		// encoding is not needed, returns the URL unchanged. The
		// implementation of this method should include the logic to determine
		// whether the session ID needs to be encoded in the URL. For example,
		// if the browser supports cookies, or session tracking is turned off,
		// URL encoding is unnecessary.
		// <P>
		// All URLs emitted by a Servlet should be run through this method.
		// Otherwise, URL rewriting cannot be used with browsers which do not
		// support cookies.
		// @deprecated
		public String encodeUrl(String url) {
			return encodeURL(url);
		}
		
		// / Encodes the specified URL for use in the sendRedirect method or, if
		// encoding is not needed, returns the URL unchanged. The
		// implementation of this method should include the logic to determine
		// whether the session ID needs to be encoded in the URL. Because the
		// rules for making this determination differ from those used to
		// decide whether to encode a normal link, this method is seperate
		// from the encodeUrl method.
		// <P>
		// All URLs sent to the sendRedirect method should
		// be
		// run through this method. Otherwise, URL rewriting cannot be used with
		// browsers which do not support cookies.
		public String encodeRedirectUrl(String url) {
			return encodeRedirectURL(url);
		}
		
		public Socket getSocket() {
			// TODO apply security check
			return socket;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.ServletRequest#getAsyncContext()
		 */
		@Override
		public AsyncContext getAsyncContext() {
			// TODO Auto-generated method stub
			return null;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.ServletRequest#getDispatcherType()
		 */
		@Override
		public DispatcherType getDispatcherType() {
			// TODO Auto-generated method stub
			return null;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.ServletRequest#getServletContext()
		 */
		@Override
		public ServletContext getServletContext() {
			// TODO Auto-generated method stub
			return null;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.ServletRequest#isAsyncStarted()
		 */
		@Override
		public boolean isAsyncStarted() {
			// TODO Auto-generated method stub
			return false;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.ServletRequest#isAsyncSupported()
		 */
		@Override
		public boolean isAsyncSupported() {
			// TODO Auto-generated method stub
			return false;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.ServletRequest#startAsync()
		 */
		@Override
		public AsyncContext startAsync() throws IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.ServletRequest#startAsync(javax.servlet.ServletRequest,
		 *      javax.servlet.ServletResponse)
		 */
		@Override
		public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
			// TODO Auto-generated method stub
			return null;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.http.HttpServletResponse#getStatus()
		 */
		public int getStatus() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
		 */
		@Override
		public Enumeration<String> getHeaderNames() {
			// TODO Auto-generated method stub
			return null;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
		 */
		@Override
		public Enumeration<String> getHeaders(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	public static class BasicAuthRealm extends Hashtable {
		private String name;
		
		public BasicAuthRealm(String name) {
			this.name = name;
		}
		
		String name() {
			return name;
		}
	}
	
	public static class ServeInputStream extends ServletInputStream {
		private final static boolean STREAM_DEBUG = false;
		
		/**
		 * The actual input stream (buffered).
		 */
		private InputStream inStream, origInStream;
		private ServeConnection serveConnection;
		private int chunksize = 0;
		private boolean chunking = false, compressed;
		private boolean returnedAsReader, returnedAsStream;
		private long contentLength = -1;
		private long readCount;
		private byte[] oneReadBuffer = new byte[1];
		private boolean closed;
		
		/* ------------------------------------------------------------ */
		/**
		 * Constructor
		 */
		public ServeInputStream(InputStream inStream, final ServeConnection serveConnection) {
			this.serveConnection = serveConnection;
			this.inStream = inStream;
		}
		
		void refresh() {
			returnedAsReader = false;
			returnedAsStream = false;
			contentLength = -1;
			readCount = 0;
			chunksize = 0;
			closed = false;
			compressed(false);
		}
		
		/* ------------------------------------------------------------ */
		/**
		 * @param chunking
		 */
		public void chunking(boolean chunking) {
			if (contentLength == -1) {
				this.chunking = chunking;
			}
		}
		
		boolean compressed(boolean on) {
			if (on) {
				if (compressed == false) {
					origInStream = inStream;
					try {
						ServeInputStream serveInputStream = new ServeInputStream(inStream, serveConnection);
						if (chunking) {
							serveInputStream.chunking(true);
							chunking(false);
						}
						inStream = (InputStream) serveConnection.serve.gzipInStreamConstr.newInstance(new Object[] { serveInputStream });
						compressed = true;
						// conn.serve.log("Compressed stream was created with
						// success",
						// null);
					} catch (Exception ex) {
						if (ex instanceof InvocationTargetException) {
							serveConnection.serve.log("TJWS: Problem in compressed stream creation", ((InvocationTargetException) ex).getTargetException());
						} else {
							serveConnection.serve.log("TJWS: Problem in compressed stream obtaining", ex);
						}
					}
				}
			} else if (compressed) {
				compressed = false;
				inStream = origInStream;
			}
			
			return compressed;
		}
		
		/**
		 * sets max read byte in input
		 */
		void setContentLength(long contentLength) {
			if (this.contentLength == -1 && contentLength >= 0 && chunking == false) {
				// if (STREAM_DEBUG) {
				// new Exception("Set content
				// length:"+contentLength).printStackTrace();
				// }
				this.contentLength = contentLength;
				readCount = 0;
			}
			
			// else if (STREAM_DEBUG || true) {
			// new Exception("Igonore Set content length:"+contentLength+"
			// for "+this.contentLength).printStackTrace();
			// }
		}
		
		/* ------------------------------------------------------------ */
		/**
		 * Read a line ended by CRLF, used internally only for reading headers.
		 * No char encoding, ASCII only
		 */
		protected String readLine(int maxLen) throws IOException {
			if (maxLen <= 0) {
				throw new IllegalArgumentException("Max len:" + maxLen);
			}
			
			final StringBuffer lineBuffer = new StringBuffer(Math.min(8192, maxLen));
			
			int c;
			boolean newLine = false;
			int i = 0;
			while ((c = inStream.read()) != -1) {
				if (c == 10) { // LF
					if (newLine) {
						break;
					}
					break;
					// throw new IOException ("LF without CR");
				} else if (c == 13) { // CR
					newLine = true;
				} else {
					// if (cr) throw new IOException ("CR without LF");
					// see
					// http://www.w3.org/Protocols/HTTP/1.1/rfc2616bis/draft-lafon-rfc2616bis-03.html#tolerant.applications
					newLine = false;
					if (i >= maxLen) {
						throw new IOException("Line lenght exceeds " + maxLen);
					}
					
					lineBuffer.append((char) c);
					i++;
				}
			}
			
			if (STREAM_DEBUG) {
				System.err.println(lineBuffer);
			}
			
			if (c == -1 && lineBuffer.length() == 0) {
				return null;
			}
			
			return lineBuffer.toString();
		}
		
		/**
		 * Reads the stream bytes.
		 * 
		 * @see java.io.InputStream#read()
		 */
		public int read() throws IOException {
			int result = read(oneReadBuffer, 0, 1);
			if (result == 1) {
				return 255 & oneReadBuffer[0];
			}
			
			return -1;
		}
		
		/**
		 * Reads the stream bytes.
		 * 
		 * @see java.io.InputStream#read(byte[])
		 */
		public int read(byte b[]) throws IOException {
			return read(b, 0, b.length);
		}
		
		/**
		 * Reads the stream bytes.
		 * 
		 * @see java.io.InputStream#read(byte[], int, int)
		 */
		public synchronized int read(byte[] dataBytes, int offset, int length) throws IOException {
			if (closed) {
				throw new IOException("The stream is already closed");
			}
			
			if (chunking) {
				if (chunksize <= 0 && getChunkSize() <= 0) {
					return -1;
				}
				if (length > chunksize) {
					length = chunksize;
				}
				length = inStream.read(dataBytes, offset, length);
				chunksize = (length < 0) ? -1 : (chunksize - length);
			} else {
				if (contentLength >= 0) {
					if (readCount >= contentLength) {
						if (STREAM_DEBUG) {
							System.err.print("EOF at " + contentLength);
						}
						return -1;
					}
					
					if (contentLength - length < readCount) {
						length = (int) (contentLength - readCount);
					}
					
					length = inStream.read(dataBytes, offset, length);
					if (length > 0) {
						readCount += length;
					}
				} else {
					// to avoid extra if
					length = inStream.read(dataBytes, offset, length);
				}
			}
			
			if (STREAM_DEBUG && length > 0) {
				System.err.print(new String(dataBytes, offset, length));
			}
			
			return length;
		}
		
		/* ------------------------------------------------------------ */
		public long skip(long length) throws IOException {
			if (STREAM_DEBUG) {
				System.err.println("instream.skip() :" + length);
			}
			if (closed) {
				throw new IOException("The stream is already closed");
			}
			
			if (chunking) {
				if (chunksize <= 0 && getChunkSize() <= 0) {
					return -1;
				}
				
				if (length > chunksize) {
					length = chunksize;
				}
				length = inStream.skip(length);
				chunksize = (length < 0) ? -1 : (chunksize - (int) length);
			} else {
				if (contentLength >= 0) {
					length = Math.min(length, contentLength - readCount);
					if (length <= 0) {
						return -1;
					}
					length = inStream.skip(length);
					readCount += length;
				} else {
					length = inStream.skip(length);
				}
			}
			
			return length;
		}
		
		/* ------------------------------------------------------------ */
		/**
		 * Available bytes to read without blocking. If you are unlucky may
		 * return 0 when there are more
		 */
		public int available() throws IOException {
			if (STREAM_DEBUG) {
				System.err.println("instream.available()");
			}
			
			if (closed) {
				// throw new IOException("The stream is already closed");
				return 0;
			}
			
			if (chunking) {
				int len = inStream.available();
				if (len <= chunksize) {
					return len;
				}
				return chunksize;
			}
			
			if (contentLength >= 0) {
				int len = inStream.available();
				if (contentLength - readCount < Integer.MAX_VALUE) {
					return Math.min(len, (int) (contentLength - readCount));
				}
				
				return len;
			} else {
				return inStream.available();
			}
		}
		
		/* ------------------------------------------------------------ */
		public void close() throws IOException {
			// keep alive, will be closed by socket
			// in.close();
			if (STREAM_DEBUG) {
				System.err.println("instream.close() " + closed);
			}
			// new Exception("instream.close()").printStackTrace();
			if (closed) {
				return;
			}
			// throw new
			// IOException("The stream is already closed");
			// read until end of chunks or content length
			if (chunking) {
				while (read() >= 0) {
					;
				}
			} else if (contentLength < 0) {
				;
			} else {
				long skipCount = contentLength - readCount;
				while (skipCount > 0) {
					long skipped = skip(skipCount);
					if (skipped <= 0) {
						break;
					}
					skipCount -= skipped;
				}
			}
			
			if (serveConnection.keepAlive == false) {
				inStream.close();
			}
			closed = true;
		}
		
		/* ------------------------------------------------------------ */
		/**
		 * Mark is not supported
		 * 
		 * @return false
		 */
		public boolean markSupported() {
			return false;
		}
		
		/* ------------------------------------------------------------ */
		/**
			 * 
			 */
		public void reset() throws IOException {
			// no buffering, so not possible
			if (closed) {
				throw new IOException("The stream is already closed");
			}
			if (STREAM_DEBUG) {
				System.err.println("instream.reset()");
			}
			inStream.reset();
		}
		
		/* ------------------------------------------------------------ */
		/**
		 * Not Implemented
		 * 
		 * @param readlimit
		 */
		public void mark(int readlimit) {
			// not supported
			if (STREAM_DEBUG)
				System.err.println("instream.mark(" + readlimit + ")");
		}
		
		/* ------------------------------------------------------------ */
		private int getChunkSize() throws IOException {
			if (chunksize < 0)
				return -1;
			
			chunksize = -1;
			
			// Get next non blank line
			chunking = false;
			String line = readLine(60);
			while (line != null && line.length() == 0)
				line = readLine(60);
			chunking = true;
			
			// Handle early EOF or error in format
			if (line == null)
				return -1;
			
			// Get chunksize
			int i = line.indexOf(';');
			if (i > 0)
				line = line.substring(0, i).trim();
			try {
				chunksize = Integer.parseInt(line, 16);
			} catch (NumberFormatException nfe) {
				throw new IOException("Chunked stream is broken, " + line);
			}
			
			// check for EOF
			if (chunksize == 0) {
				chunksize = -1;
				// Look for footers
				readLine(60);
				chunking = false;
			}
			return chunksize;
		}
		
		boolean isReturnedAsStream() {
			return returnedAsStream;
		}
		
		void setReturnedAsStream(boolean _on) {
			returnedAsStream = _on;
		}
		
		boolean isReturnedAsReader() {
			return returnedAsReader;
		}
		
		void setReturnedAsReader(boolean _on) {
			returnedAsReader = _on;
		}
	}
	
	public static class ServeOutputStream extends ServletOutputStream {
		
		private static final boolean STREAM_DEBUG = false;
		private boolean chunked;
		private boolean closed;
		
		// TODO: predefine as static byte[] used by chunked
		// underneath stream
		private OutputStream outputStream;
		// private BufferedWriter writer; // for top speed
		private ServeConnection serveConnection;
		private int inInclude;
		private String encoding;
		private long longBytes;
		private Utils.SimpleBuffer buffer;
		
		public ServeOutputStream(final OutputStream outputStream, ServeConnection serveConnection) {
			this.outputStream = outputStream;
			this.serveConnection = serveConnection;
			buffer = new Utils.SimpleBuffer();
			encoding = serveConnection.getCharacterEncoding();
			if (encoding == null) {
				encoding = IOHelper.ISO_8859_1;
			}
		}
		
		/*
		 * void refresh() { chunked = false; closed = false; inInclude = 0;
		 * lbytes = 0; buffer.reset(); encoding = conn.getCharacterEncoding();
		 * if (encoding == null) encoding = Utils.ISO_8859_1; }
		 */
		
		protected void reset() {
			if (longBytes == 0) {
				buffer.reset();
			} else {
				throw new IllegalStateException("Result was already committed");
			}
		}
		
		protected int getBufferSize() {
			return buffer.getSize();
		}
		
		protected void setBufferSize(int size) {
			if (longBytes > 0) {
				throw new IllegalStateException("Bytes already written in response");
			}
			buffer.setSize(size);
		}
		
		protected void setChunked(boolean set) {
			chunked = set;
		}
		
		public void print(String s) throws IOException {
			write(s.getBytes(encoding));
		}
		
		public void write(int b) throws IOException {
			write(new byte[] { (byte) b }, 0, 1);
		}
		
		public void write(byte[] b) throws IOException {
			write(b, 0, b.length);
		}
		
		public void write(byte[] b, int off, int len) throws IOException {
			if (closed) {
				if (STREAM_DEBUG) {
					System.err.println((b == null ? "null" : new String(b, off, len)) + "\n won't be written, stream closed.");
				}
				throw new IOException("An attempt of writing " + len + " bytes to a closed out.");
			}
			
			if (len == 0) {
				return;
			}
			
			// write connection headers.
			serveConnection.writeHeaders();
			b = buffer.put(b, off, len);
			len = b.length;
			if (len == 0) {
				return;
			}
			
			off = 0;
			if (chunked) {
				String hexl = Integer.toHexString(len);
				// no encoding Ok
				outputStream.write((hexl + "\r\n").getBytes());
				longBytes += 2 + hexl.length();
				outputStream.write(b, off, len);
				longBytes += len;
				outputStream.write("\r\n".getBytes());
				longBytes += 2;
			} else {
				outputStream.write(b, off, len);
				longBytes += len;
			}
			
			if (STREAM_DEBUG) {
				if (chunked) {
					System.err.println(Integer.toHexString(len));
				}
				System.err.print(new String(b, off, len));
				if (chunked) {
					System.err.println();
				}
			}
		}
		
		public void flush() throws IOException {
			// boolean cl = closed;
			if (closed) {
				return;
			}
			
			// throw new IOException("An attempt of flushig closed out.");
			serveConnection.writeHeaders();
			if (closed) {
				return;
			}
			
			byte[] b = buffer.get();
			if (b.length > 0) {
				if (chunked) {
					String hexl = Integer.toHexString(b.length);
					// no encoding Ok
					outputStream.write((hexl + "\r\n").getBytes());
					longBytes += 2 + hexl.length();
					outputStream.write(b);
					longBytes += b.length;
					outputStream.write("\r\n".getBytes());
					longBytes += 2;
					if (STREAM_DEBUG) {
						System.err.println(hexl);
						System.err.print(new String(b));
						System.err.println();
					}
				} else {
					outputStream.write(b);
					longBytes += b.length;
					if (STREAM_DEBUG) {
						System.err.print(new String(b));
					}
				}
			}
			
			// System.err.println("Was "+cl+" now "+closed);
			outputStream.flush();
		}
		
		public void close() throws IOException {
			if (closed) {
				return;
			}
			
			// throw new IOException("Stream is already closed.");
			// new IOException("Stream closing").printStackTrace();
			try {
				flush();
				if (inInclude == 0) {
					if (chunked) {
						outputStream.write("0\r\n\r\n".getBytes());
						longBytes += 5;
						if (STREAM_DEBUG) {
							System.err.print("0\r\n\r\n");
						}
						
						// TODO: here is possible to write trailer headers
						outputStream.flush();
					}
					
					if (serveConnection.keepAlive == false) {
						outputStream.close();
					} else {
						outputStream = null;
						// the stream has to be recreated after closing
						serveConnection = null;
					}
				}
			} finally {
				closed = true;
			}
		}
		
		private long lengthWritten() {
			return longBytes;
		}
		
		boolean isInInclude() {
			return inInclude == 0;
		}
		
		void setInInclude(boolean _set) {
			inInclude = _set ? 1 : 0;
			/*
			 * if (_set) inInclude++; else inInclude--; if (inInclude < 0) throw
			 * new IllegalStateException("Not matching include set");
			 */
		}
	}
	
	/**
	 * Class PathTreeDictionary - this class allows to put path elements in
	 * format n1/n2/n2[/*.ext] and get match to a pattern and a unmatched tail
	 */
	public static class PathTreeDictionary {
		Node rootNode;
		Node ext;
		Object ctx;
		
		public PathTreeDictionary() {
			rootNode = new Node();
		}
		
		/**
		 * Manages a tree of web path entries with the following cases
		 * <ul>
		 * <li>/path1/path2... - exact match entry
		 * <li>anything ending by /*
		 * <li>*.ext - extension entry applied only for last part
		 * <li>- empty entry context root servicing only /contextpath/
		 * <li>/ - default servlet when nothing else is matching
		 * </ul>
		 * 
		 * @param path
		 * @param value
		 */
		public synchronized Object[] put(String path, Object value) {
			// TODO make returning Object[] for cconsitency
			if (path.length() == 0) { // context
				if (ctx == null) {
					ctx = new Node();
				}
				
				Object result = ctx;
				ctx = value;
				return new Object[] { result, INT_ZERO };
			}
			if (path.charAt(0) == '*') {
				String ext_str = null;
				if (path.length() > 2 && path.charAt(1) == '.') {
					ext_str = path.substring(1);
				} else {
					throw new IllegalArgumentException("No extension specified for * starting pattern:" + path);
				}
				
				if (ext == null) {
					ext = new Node();
				}
				return new Object[] { ext.put(ext_str, value), INT_ZERO };
			}
			// System.out.println("==>PUT path : "+path);
			StringTokenizer st = new StringTokenizer(path, "\\/");
			Node curNode = rootNode;
			while (st.hasMoreTokens()) {
				String nodename = st.nextToken();
				// System.out.println("PUT curr node : "+nodename);
				int wci = nodename.indexOf('*');
				if (wci == 0) {
					if (nodename.length() > 1 || st.hasMoreTokens()) {
						throw new IllegalArgumentException("Using * in other than ending /* for path:" + path);
					}
					
					nodename = "";
				} else if (wci > 0) {
					throw new IllegalArgumentException("Using * in other than ending /* for path:" + path);
				}
				
				Node node = (Node) curNode.get(nodename);
				if (node == null) {
					node = new Node();
					curNode.put(nodename, node);
				}
				curNode = node;
			}
			
			Object result = curNode.object;
			curNode.object = value;
			return new Object[] { result, INT_ZERO };
		}
		
		public synchronized Object[] remove(Object value) {
			Object[] result = remove(rootNode, value);
			if (result[0] == null) {
				result = remove(null, value);
			}
			if (result[0] == null) {
				return remove(ext, value);
			}
			
			return result;
		}
		
		public synchronized Object[] remove(String path) {
			Object[] result = get(path);
			if (result[0] != null) {
				return remove(result[0]);
			}
			return result;
		}
		
		public Object[] remove(Node node, Object value) {
			// TODO make full path, not only last element
			if (node == null) {
				if (ctx == value) {
					ctx = null;
					return new Object[] { value, new Integer(0) };
				}
				return new Object[] { null, null };
			}
			
			if (node == ext) {
				// TODO potential bug since look for first entry
				Enumeration e = ext.keys();
				while (e.hasMoreElements()) {
					Object key = e.nextElement();
					if (ext.get(key) == value) {
						return new Object[] { ext.remove(key), new Integer(0) };
					}
				}
				return new Object[] { null, null };
			}
			
			if (node.object == value) {
				node.object = null;
				return new Object[] { value, new Integer(0) };
			}
			Enumeration e = node.keys();
			while (e.hasMoreElements()) {
				Object[] result = remove((Node) node.get((String) e.nextElement()), value);
				if (result[0] != null) {
					return result;
				}
			}
			return new Object[] { null, null };
		}
		
		/**
		 * This function looks up in the directory to find the perfect match and
		 * remove matching part from path, so if you need to keep original path,
		 * save it somewhere
		 */
		public Object[] get(String path) {
			// System.out.println("==>GET " + path);
			// new Exception("GET " + path).printStackTrace();
			Object[] result = new Object[2];
			if (path == null)
				return result;
			if ((path.length() == 0 || path.equals("/")) && ctx != null) {
				result[0] = ctx;
				result[1] = INT_ZERO;
				return result;
			}
			char[] ps = path.toCharArray();
			Node curNode = rootNode; // default servlet
			int p0 = 0, lm = 0; // last match
			
			boolean div_state = true;
			for (int i = 0; i < ps.length; i++) {
				// System.out.println("GET "+ps[i]);
				if (ps[i] == '/' || ps[i] == '\\') { // next divider
					if (div_state) {
						continue;
					}
					Node node = (Node) curNode.get(new String(ps, p0, i - p0));
					// System.out.println("GET Node " + node + " for " + new
					// String(ps, p0, i - p0));
					
					if (node == null) { // exact
						node = (Node) curNode.get(""); // for *
						if (node != null && node.object != null) {
							result[0] = node.object;
							// System.out.println("GET * for " + node);
						}
						break;
					}
					curNode = node;
					div_state = true;
					p0 = i + 1;
				} else {
					if (div_state) {
						p0 = i;
						div_state = false;
					}
				}
			}
			
			String last_part = new String(ps, p0, ps.length - p0);
			Node lastNode = (Node) curNode.get(last_part);
			// System.out.println("GET cur node : " + last_node + " for: " +
			// last_part + " root ext:" + ext+" and pos:"+p0);
			if (lastNode != null) {
				if (lastNode.object == null) {
					lastNode = (Node) lastNode.get(""); // check for *
				}
				if (lastNode != null && lastNode.object != null) {
					result[0] = lastNode.object;
					lm = last_part.length() > 0 ? ps.length : p0 - 1;
				}
			} else {
				lastNode = (Node) curNode.get("");
				if (lastNode != null && lastNode.object != null) {
					result[0] = lastNode.object;
					lm = p0 > 0 ? p0 - 1 : 0;
				}
			}
			
			// try ext
			if (result[0] == null) {
				lm = ps.length;
				if (ext != null) {
					int ldi = last_part.lastIndexOf('.');
					if (ldi > 0) { // ignoring cases /.extension
						result[0] = ext.get(last_part.substring(ldi));
						// System.out.println("GET ext node: " +
						// last_part.substring(ldi));
					}
				}
				if (result[0] == null) { // look for default servlet
					lastNode = (Node) rootNode.get("");
					if (lastNode != null)
						result[0] = lastNode.object;
					if (result[0] == null) {
						result[0] = rootNode.object;
					}
				}
			}
			// System.out.println("GET pos: "+lm);
			result[1] = new Integer(lm);
			return result;
		}
		
		public Enumeration<String> keys() {
			Vector<String> result = new Vector<String>();
			if (ctx != null) {
				result.addElement("");
			}
			if (rootNode.object != null) {
				result.addElement("/");
			}
			addSiblingNames(rootNode, result, "");
			if (ext != null) {
				Enumeration e = ext.keys();
				while (e.hasMoreElements()) {
					result.addElement("*" + e.nextElement());
				}
			}
			
			return result.elements();
		}
		
		public void addSiblingNames(Node node, Vector result, String path) {
			Enumeration e = node.keys();
			while (e.hasMoreElements()) {
				String pc = (String) e.nextElement();
				Node childNode = (Node) node.get(pc);
				pc = path + '/' + (pc.length() == 0 ? "*" : pc);
				if (childNode.object != null) {
					result.addElement(pc);
				}
				
				addSiblingNames(childNode, result, pc);
			}
		}
		
		public Enumeration<Object> elements() {
			Vector<Object> result = new Vector<Object>();
			if (rootNode.object != null) {
				result.add(rootNode.object);
			}
			
			addSiblingObjects(rootNode, result);
			return result.elements();
		}
		
		public void addSiblingObjects(Node node, Vector<Object> result) {
			Enumeration e = node.keys();
			while (e.hasMoreElements()) {
				Node childNode = (Node) node.get(e.nextElement());
				if (childNode.object != null) {
					result.addElement(childNode.object);
				}
				addSiblingObjects(childNode, result);
			}
		}
		
		class Node extends Hashtable {
			Object object;
			String name;
		}
	}
	
	/**
	 * Http session support
	 * 
	 * TODO: provide lazy session restoring, it should allow to load classes
	 * from wars 1st step it read serialization data and store under session
	 * attribute 2nd when the session requested, it tries to deserialize all
	 * session attributes considered that all classes available
	 */
	public static class AcmeSession extends Hashtable implements HttpSession {
		private long createTime;
		private long lastAccessTime;
		private String id;
		private int inactiveInterval; // in seconds
		private boolean expired;
		private transient ServletContext servletContext;
		private transient HttpSessionContext sessionContext;
		private transient List listeners;
		
		// TODO: check in documentation what is default inactive interval and
		// what
		// means 0
		// and what is mesurement unit
		AcmeSession(String id, ServletContext servletContext, HttpSessionContext sessionContext) {
			this(id, 0, servletContext, sessionContext);
		}
		
		AcmeSession(String id, int inactiveInterval, ServletContext servletContext, HttpSessionContext sessionContext) {
			// new
			// Exception("Session created with:
			// "+servletContext).printStackTrace();
			// //!!!
			createTime = System.currentTimeMillis();
			this.id = id;
			this.inactiveInterval = inactiveInterval;
			this.servletContext = servletContext;
			this.sessionContext = sessionContext;
		}
		
		public long getCreationTime() {
			return createTime;
		}
		
		public String getId() {
			return id;
		}
		
		public long getLastAccessedTime() {
			return lastAccessTime;
		}
		
		public void setMaxInactiveInterval(int interval) {
			inactiveInterval = interval;
		}
		
		public int getMaxInactiveInterval() {
			return inactiveInterval;
		}
		
		/**
		 * @deprecated
		 */
		public HttpSessionContext getSessionContext() {
			return sessionContext;
		}
		
		/**
		 * Returns the ServletContext to which this session belongs.
		 * 
		 * @return The ServletContext object for the web application
		 * @ince 2.3
		 */
		public ServletContext getServletContext() {
			// System.err.println("ctx from:"+servletContext); //!!!
			return servletContext;
		}
		
		public Object getAttribute(String name) throws IllegalStateException {
			if (expired)
				throw new IllegalStateException();
			return get((Object) name);
		}
		
		public Object getValue(String name) throws IllegalStateException {
			return getAttribute(name);
		}
		
		public Enumeration getAttributeNames() throws IllegalStateException {
			if (expired) {
				throw new IllegalStateException();
			}
			
			return keys();
		}
		
		public String[] getValueNames() throws IllegalStateException {
			Enumeration e = getAttributeNames();
			Vector names = new Vector();
			while (e.hasMoreElements()) {
				names.addElement(e.nextElement());
			}
			String[] result = new String[names.size()];
			names.copyInto(result);
			return result;
		}
		
		public void setAttribute(String name, Object value) throws IllegalStateException {
			if (expired) {
				throw new IllegalStateException();
			}
			
			Object oldValue = value != null ? put((Object) name, value) : remove(name);
			if (oldValue != null) {
				if (oldValue instanceof HttpSessionBindingListener) {
					((HttpSessionBindingListener) oldValue).valueUnbound(new HttpSessionBindingEvent(this, name));
				}
			}
			if (value != null) {
				if (value instanceof HttpSessionBindingListener) {
					((HttpSessionBindingListener) value).valueBound(new HttpSessionBindingEvent(this, name));
				}
				notifyListeners(name, oldValue, value);
			} else {
				notifyListeners(name, oldValue);
			}
		}
		
		public void putValue(String name, Object value) throws IllegalStateException {
			setAttribute(name, value);
		}
		
		public void removeAttribute(String name) throws IllegalStateException {
			if (expired) {
				throw new IllegalStateException();
			}
			
			Object value = remove((Object) name);
			if (value != null) {
				if (value instanceof HttpSessionBindingListener) {
					((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name));
				}
				notifyListeners(name, value);
			}
		}
		
		public void removeValue(String name) throws IllegalStateException {
			removeAttribute(name);
		}
		
		public synchronized void invalidate() throws IllegalStateException {
			if (expired) {
				throw new IllegalStateException();
			}
			
			notifyListeners();
			Enumeration e = getAttributeNames();
			while (e.hasMoreElements()) {
				removeAttribute((String) e.nextElement());
			}
			listeners = null;
			setExpired(true);
			// would be nice remove it from hash table also
		}
		
		public boolean isNew() throws IllegalStateException {
			if (expired) {
				throw new IllegalStateException();
			}
			
			return (lastAccessTime == 0);
		}
		
		/**
		 * 
		 * @param listeners
		 */
		public synchronized void setListeners(List<?> listeners) {
			if (this.listeners == null) {
				this.listeners = listeners;
				if (this.listeners != null) {
					HttpSessionEvent localHttpSessionEvent = new HttpSessionEvent(this);
					for (int i = 0; i < this.listeners.size(); i++) {
						try {
							((HttpSessionListener) this.listeners.get(i)).sessionCreated(localHttpSessionEvent);
						} catch (ClassCastException classCastException) {
							// servletContext.log("Wrong session listener type:"
							// + classCastException, classCastException);
						} catch (NullPointerException nullPointerException) {
							// servletContext.log("Null session listener!",
							// nullPointerException);
						}
					}
				}
			}
		}
		
		/**
		 * something hack, to update servlet context since session created out
		 * of scope
		 * 
		 * @param servletContext
		 */
		public synchronized void setServletContext(ServletContext servletContext) {
			// System.err.println("ctx to:"+servletContext); //!!!
			this.servletContext = servletContext;
		}
		
		private void notifyListeners() {
			if (listeners != null) {
				HttpSessionEvent event = new HttpSessionEvent(this);
				for (int i = 0; i < listeners.size(); i++) {
					try {
						((HttpSessionListener) listeners.get(i)).sessionDestroyed(event);
					} catch (ClassCastException ex) {
						// servletContext.log("Wrong session listener
						// type."+cce);
					} catch (NullPointerException ex) {
						// servletContext. log("Null session listener.");
					}
				}
			}
		}
		
		/**
		 * 
		 * @param name
		 * @param value
		 */
		private void notifyListeners(String name, Object value) {
			if (listeners != null) {
				HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, name, value);
				for (int i = 0, n = listeners.size(); i < n; i++) {
					try {
						((HttpSessionAttributeListener) listeners.get(i)).attributeRemoved(event);
					} catch (ClassCastException cce) {
					} catch (NullPointerException npe) {
					}
				}
			}
		}
		
		/**
		 * 
		 * @param name
		 * @param oldValue
		 * @param value
		 */
		private void notifyListeners(String name, Object oldValue, Object value) {
			if (listeners != null) {
				HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, name, value);
				HttpSessionBindingEvent oldEvent = oldValue == null ? null : new HttpSessionBindingEvent(this, name, oldValue);
				for (int i = 0, n = listeners.size(); i < n; i++) {
					try {
						HttpSessionAttributeListener sessionListener = (HttpSessionAttributeListener) listeners.get(i);
						if (oldEvent != null) {
							sessionListener.attributeReplaced(oldEvent);
						}
						
						sessionListener.attributeAdded(event);
					} catch (ClassCastException cce) {
					} catch (NullPointerException npe) {
					}
				}
			}
		}
		
		private void setExpired(boolean expired) {
			this.expired = expired;
		}
		
		boolean isValid() {
			return !expired;
		}
		
		boolean checkExpired() {
			return (inactiveInterval > 0 && (inactiveInterval * 1000 < System.currentTimeMillis() - lastAccessTime));
		}
		
		void userTouch() {
			if (isValid()) {
				lastAccessTime = System.currentTimeMillis();
			} else {
				throw new IllegalStateException();
			}
		}
		
		// storing session in format
		// id:latency:contextname:tttt
		// entry:base64 ser data
		// entry:base64 ser data
		void save(Writer w) throws IOException {
			if (expired) {
				return;
			}
			
			// can't use append because old JDK
			w.write(id);
			w.write(':');
			w.write(Integer.toString(inactiveInterval));
			w.write(':');
			w.write(servletContext == null || servletContext.getServletContextName() == null ? "" : servletContext.getServletContextName());
			w.write(':');
			w.write(Long.toString(lastAccessTime));
			w.write("\r\n");
			
			Enumeration e = getAttributeNames();
			ByteArrayOutputStream os = new ByteArrayOutputStream(1024 * 16);
			while (e.hasMoreElements()) {
				String aname = (String) e.nextElement();
				Object so = get(aname);
				if (so instanceof Serializable) {
					os.reset();
					ObjectOutputStream outputStream = new ObjectOutputStream(os);
					try {
						outputStream.writeObject(so);
						w.write(aname);
						w.write(":");
						w.write(Utils.base64Encode(os.toByteArray()));
						w.write("\r\n");
					} catch (IOException ioe) {
						servletContext.log("TJWS: Can't replicate/store a session value of '" + aname + "' class:" + so.getClass().getName(), ioe);
					}
				} else {
					servletContext.log("TJWS: Non serializable session object has been " + so.getClass().getName() + " skiped in storing of " + aname, null);
				}
				
				if (so instanceof HttpSessionActivationListener) {
					((HttpSessionActivationListener) so).sessionWillPassivate(new HttpSessionEvent(this));
				}
			}
			w.write("$$\r\n");
		}
		
		/**
		 * 
		 * @param r
		 * @param inactiveInterval
		 * @param servletContext
		 * @param sessionContext
		 * @return
		 * @throws IOException
		 */
		static AcmeSession restore(BufferedReader r, int inactiveInterval, ServletContext servletContext, HttpSessionContext sessionContext) throws IOException {
			String s = r.readLine();
			if (s == null) {// eos
				return null;
			}
			
			int cp = s.indexOf(':');
			if (cp < 0) {
				throw new IOException("Invalid format for a session header, no session id: " + s);
			}
			
			String id = s.substring(0, cp);
			int cp2 = s.indexOf(':', cp + 1);
			if (cp2 < 0)
				throw new IOException("Invalid format for a session header, no latency: " + s);
			try {
				inactiveInterval = Integer.parseInt(s.substring(cp + 1, cp2));
			} catch (NumberFormatException nfe) {
				servletContext.log("TJWS: Session latency is invalid:" + s.substring(cp + 1, cp2) + " " + nfe);
			}
			cp = s.indexOf(':', cp2 + 1);
			if (cp < 0)
				throw new IOException("TJWS: Invalid format for a session header, context name: " + s);
			String contextName = s.substring(cp2 + 1, cp);
			// consider servletContext.getContext("/"+contextName)
			AcmeSession result = new AcmeSession(id, inactiveInterval, contextName.length() == 0 ? servletContext : null, sessionContext);
			try {
				result.lastAccessTime = Long.parseLong(s.substring(cp + 1));
			} catch (NumberFormatException nfe) {
				servletContext.log("TJWS: Last access time is invalid:" + s.substring(cp + 1) + " " + nfe);
			}
			do {
				s = r.readLine();
				if (s == null) {
					throw new IOException("Unexpected end of a stream.");
				}
				if ("$$".equals(s)) {
					return result;
				}
				
				cp = s.indexOf(':');
				if (cp < 0) {
					throw new IOException("Invalid format for a session entry: " + s);
				}
				
				String aname = s.substring(0, cp);
				// if (lazyRestore)
				// result.put(aname, s.substring(cp+1));
				ObjectInputStream oInputStream = new ObjectInputStream(new ByteArrayInputStream(Utils.decode64(s.substring(cp + 1))));
				Throwable restoreError;
				try {
					Object so = oInputStream.readObject();
					result.put(aname, so);
					restoreError = null;
					if (so instanceof HttpSessionActivationListener) {
						((HttpSessionActivationListener) so).sessionDidActivate(new HttpSessionEvent(result));
					}
					
				} catch (ClassNotFoundException cnfe) {
					restoreError = cnfe;
				} catch (NoClassDefFoundError ncdfe) {
					restoreError = ncdfe;
				} catch (IOException ioe) {
					restoreError = ioe;
				}
				
				if (restoreError != null) {
					servletContext.log("TJWS: Can't restore :" + aname + ", " + restoreError);
				}
				
			} while (true);
		}
	}
	
	protected static class AcmeCookie extends Cookie {
		private boolean httpOnly;
		
		public AcmeCookie(String name, String value) {
			super(name, value);
		}
		
		public boolean isHttpOnly() {
			return httpOnly;
		}
		
		public void setHttpOnly(boolean isHttpOnly) {
			httpOnly = isHttpOnly;
		}
	}
	
	protected static class LocaleWithWeight implements Comparable {
		// should be int
		protected float weight;
		protected Locale locale;
		
		LocaleWithWeight(final Locale locale, float weight) {
			this.locale = locale;
			this.weight = weight;
		}
		
		public int compareTo(Object object) {
			if (object instanceof LocaleWithWeight) {
				return (int) (((LocaleWithWeight) object).weight - weight) * 100;
			}
			
			throw new IllegalArgumentException();
		}
		
		public Locale getLocale() {
			return locale;
		}
	}
	
	protected static class AcceptLocaleEnumeration implements Enumeration {
		private Iterator itr;
		
		public AcceptLocaleEnumeration(TreeSet treeSet) {
			itr = treeSet.iterator();
		}
		
		public boolean hasMoreElements() {
			return itr.hasNext();
		}
		
		public Object nextElement() {
			return ((LocaleWithWeight) itr.next()).getLocale();
		}
	}
	
	/**
	 * TODO: reconsider implementation by providing inner class implementing
	 * HttpSessionContext and returning it on request to avoid casting this
	 * class to Hashtable.
	 */
	protected static class HttpSessionContextImpl extends Hashtable implements HttpSessionContext {
		
		public Enumeration getIds() {
			return keys();
		}
		
		public HttpSession getSession(final String sessionId) {
			return (HttpSession) get(sessionId);
		}
		
		void save(final Writer writer) throws IOException {
			Enumeration itr = elements();
			while (itr.hasMoreElements()) {
				((AcmeSession) itr.nextElement()).save(writer);
			}
		}
		
		static HttpSessionContextImpl restore(final BufferedReader bReader, int inactiveInterval, ServletContext servletContext) throws IOException {
			HttpSessionContextImpl result = new HttpSessionContextImpl();
			AcmeSession session;
			while ((session = AcmeSession.restore(bReader, inactiveInterval, servletContext, result)) != null) {
				if (session.checkExpired() == false) {
					result.put(session.getId(), session);
				}
			}
			
			return result;
		}
	}
	
	/**
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletContext#getEffectiveMajorVersion()
	 */
	@Override
	public int getEffectiveMajorVersion() {
		return 0;
	}
	
	/**
	 * @see javax.servlet.ServletContext#getEffectiveMinorVersion()
	 */
	@Override
	public int getEffectiveMinorVersion() {
		return 0;
	}
	
	/**
	 * @see javax.servlet.ServletContext#setInitParameter(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public boolean setInitParameter(String paramString1, String paramString2) {
		return false;
	}
	
	/**
	 * @see javax.servlet.ServletContext#addServlet(java.lang.String,
	 *      java.lang.Class)
	 */
	@Override
	public Dynamic addServlet(String paramString, Class<? extends Servlet> paramClass) {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#createServlet(java.lang.Class)
	 */
	@Override
	public <T extends Servlet> T createServlet(Class<T> paramClass) throws ServletException {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#getServletRegistration(java.lang.String)
	 */
	@Override
	public ServletRegistration getServletRegistration(String paramString) {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#getServletRegistrations()
	 */
	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#addFilter(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public javax.servlet.FilterRegistration.Dynamic addFilter(String paramString1, String paramString2) {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#addFilter(java.lang.String,
	 *      javax.servlet.Filter)
	 */
	@Override
	public javax.servlet.FilterRegistration.Dynamic addFilter(String paramString, Filter paramFilter) {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#addFilter(java.lang.String,
	 *      java.lang.Class)
	 */
	@Override
	public javax.servlet.FilterRegistration.Dynamic addFilter(String paramString, Class<? extends Filter> paramClass) {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#createFilter(java.lang.Class)
	 */
	@Override
	public <T extends Filter> T createFilter(Class<T> paramClass) throws ServletException {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#getFilterRegistration(java.lang.String)
	 */
	@Override
	public FilterRegistration getFilterRegistration(String paramString) {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#getFilterRegistrations()
	 */
	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#getSessionCookieConfig()
	 */
	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#setSessionTrackingModes(java.util.Set)
	 */
	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> paramSet) {
	}
	
	/**
	 * @see javax.servlet.ServletContext#getDefaultSessionTrackingModes()
	 */
	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#getEffectiveSessionTrackingModes()
	 */
	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#addListener(java.lang.String)
	 */
	@Override
	public void addListener(String paramString) {
	}
	
	/**
	 * @see javax.servlet.ServletContext#addListener(java.util.EventListener)
	 */
	@Override
	public <T extends EventListener> void addListener(T paramT) {
	}
	
	/**
	 * @see javax.servlet.ServletContext#addListener(java.lang.Class)
	 */
	@Override
	public void addListener(Class<? extends EventListener> paramClass) {
	}
	
	/**
	 * @see javax.servlet.ServletContext#createListener(java.lang.Class)
	 */
	@Override
	public <T extends EventListener> T createListener(Class<T> paramClass) throws ServletException {
		return null;
	}
	
	/**
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletContext#getJspConfigDescriptor()
	 */
	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		return null;
	}
	
	/**
	 * @see javax.servlet.ServletContext#getClassLoader()
	 */
	@Override
	public ClassLoader getClassLoader() {
		return null;
	}
	
	/**
	 * 
	 * @see javax.servlet.ServletContext#declareRoles(java.lang.String[])
	 */
	@Override
	public void declareRoles(String... paramVarArgs) {
	}
}