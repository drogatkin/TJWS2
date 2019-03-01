/* tjws - WebAppServlet.java
 * Copyright (C) 1999-2010 Dmitriy Rogatkin.  All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  $Id: WebAppServlet.java,v 1.120 2013/06/08 06:07:45 cvs Exp $
 * Created on Dec 14, 2004
 */

package rogatkin.web;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.Part;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import Acme.IOHelper;
import Acme.Utils;
import Acme.Serve.FileServlet;
import Acme.Serve.Serve;

/**
 * @author dmitriy
 * 
 * 
 */
public class WebAppServlet extends HttpServlet implements ServletContext {
	public static final String DEF_DEBUG = "tjws.webapp.debug";
	
	public static final String WAR_NAME_AS_CONTEXTPATH = "tjws.wardeploy.warname-as-context";
	
	public static final String WAR_DEPLOY_IN_ROOT = "tjws.wardeploy.as-root";
	
	public static final String RUNTIMEENV_ATTR = "##RuntimeEnv";
	
	static final String MULTIPART_ERR_MSQ = "Request isn't multipart/form-data type or processing it is not enabled in deplyment descriptor web.xml";
	
	protected static final String WEBAPPCLASSLOADER = "rogatkin.webapp.AppClassLoader";
	
	protected static final String WEBAPPINITTIMEOUT = "tjws.webapp.%s.init.timeout"; // in
																						// seconds
	
	List<ServletAccessDescr> servlets;
	
	List<FilterAccessDescriptor> filters;
	
	URL[] cpUrls;
	
	ClassLoader ucl;
	
	private static AppContextDelegator appContextDelegator;
	
	private static Object runtimeEnv;
	
	File deployDir;
	
	Serve server;
	
	int sessionTimeout;
	
	int initTimeout;
	
	boolean noAnnot; // ignore @WebServlet @WebFilter and @WebListener
	
	ThreadPoolExecutor asyncThreads;
	
	// / context methods
	protected String contextName;
	
	protected String contextPath;
	
	protected String virtualHost;
	
	// protected String origContextName, origContextPath;
	
	protected String description;
	
	protected Hashtable<String, Object> attributes;
	
	protected Hashtable<String, String> contextParameters;
	
	protected List<String> welcomeFiles;
	
	protected List<ErrorPageDescr> errorPages;
	
	protected List<EventListener> listeners;
	
	protected List<EventListener> sessionListeners;
	
	protected ArrayList<ServletRequestListener> requestListeners;
	
	protected ArrayList<ServletRequestAttributeListener> attributeListeners;
	
	protected Map<String, String> mimes;
	
	protected SessionCookieConfig scc;
	
	protected Set<SessionTrackingMode> dstm = EnumSet.of(SessionTrackingMode.URL, SessionTrackingMode.COOKIE),
					stm;
	
	private boolean applyCompression;
	
	// ** interface to decouple from J2EE features
	
	public static interface AppContextDelegator {
		/**
		 * searches object in context
		 * 
		 * @param name
		 * @return
		 */
		Object lookup(String name);
		
		/**
		 * looking for a link with name in context
		 * 
		 * @param name
		 * @return
		 */
		Object lookupLink(String name);
		
		/**
		 * add object to be available from context with name
		 * 
		 * @param name
		 * @param obj
		 */
		void add(String name, Object obj);
		
		/**
		 * remove app object from container
		 * 
		 * @param name
		 * @return stored object or null
		 */
		Object remove(String name);
	}
	
	protected static interface Openable {
		Object getOrigin();
	}
	
	protected static class MappingEntry {
		String servPath;
		
		Pattern pathPat;
		
		MappingEntry(String path, String pattern) {
			servPath = path;
			pathPat = Pattern.compile(pattern);
		}
		
		public String toString() {
			return String.format("Mapping of %s with regexp pat %s", servPath, pathPat);
		}
	}
	
	protected class ServletAccessDescr implements ServletConfig, Comparable<ServletAccessDescr> {
		String className;
		String name;
		Servlet instance;
		MappingEntry[] mapping;
		Map<String, String> initParams;
		String label;
		int loadOnStart;
		boolean asyncSupported;
		String runAs;
		File multipartLocation;
		long multipartMaxFile;
		long multipartMaxRequest;
		int multipartThreshold;
		boolean multipartEnabled;
		String descr;
		// if servlet suspended
		long timeToReactivate;
		
		@Override
		public String getServletName() {
			return name;
		}
		
		public Enumeration getInitParameterNames() {
			return new Enumeration<String>() {
				Iterator<String> i;
				{
					i = initParams.keySet().iterator();
				}
				
				public boolean hasMoreElements() {
					return i.hasNext();
				}
				
				public String nextElement() {
					return i.next();
				}
			};
		}
		
		@Override
		public ServletContext getServletContext() {
			return WebAppServlet.this;
		}
		
		@Override
		public String getInitParameter(String name) {
			return initParams.get(name);
		}
		
		public void add(MappingEntry entry) {
			if (mapping == null)
				mapping = new MappingEntry[1];
			else { // can't use copyOf in 1.5
				MappingEntry[] copy = new MappingEntry[mapping.length + 1];
				System.arraycopy(mapping, 0, copy, 0, mapping.length);
				mapping = copy;
			}
			mapping[mapping.length - 1] = entry;
		}
		
		public int compareTo(ServletAccessDescr sad) {
			return loadOnStart - sad.loadOnStart;
		}
		
		int matchPath(String path) {
			if (mapping == null)
				return -1;
			for (int i = 0; i < mapping.length; i++)
				if (mapping[i].pathPat.matcher(path).matches())
					return i;
			return -1;
		}
		
		protected Servlet newInstance() throws ServletException {
			try {
				// System.err.printf("new instance %s %s%n", descr.className,
				// Arrays.toString(ucl.getURLs()));
				Class<Servlet> servletClass = (Class<Servlet>) ucl.loadClass(className);
				if (noAnnot == false) {
					WebServlet servletAnnot = servletClass.getAnnotation(WebServlet.class);
					if (servletAnnot != null) {
						asyncSupported = servletAnnot.asyncSupported();
					}
				}
				MultipartConfig multipartAnnot = servletClass.getAnnotation(MultipartConfig.class);
				if (multipartAnnot != null) {
					multipartEnabled = true;
					if (multipartAnnot.location().length() > 0)
						multipartLocation = new File(multipartAnnot.location());
					multipartThreshold = multipartAnnot.fileSizeThreshold();
					multipartMaxFile = multipartAnnot.maxFileSize();
					multipartMaxRequest = multipartAnnot.maxRequestSize();
				}
				instance = servletClass.newInstance();
				final ServletException[] exHolder = new ServletException[1];
				Thread initThread = new Thread("Init thread of " + contextName) {
					public void run() {
						try {
							instance.init(ServletAccessDescr.this);
						} catch (ServletException se) {
							exHolder[0] = se;
						}
					}
				};
				initThread.start();
				initThread.join(initTimeout * 1000);
				if (exHolder[0] == null)
					if (initThread.isAlive() == true)
						exHolder[0] = new ServletException(String.format("Initialization of %s in context %s exceeded allocated time (%dsecs)", name, contextName, initTimeout));
				if (exHolder[0] != null) {
					instance = null;
					throw exHolder[0];
				}
				return instance;
				// TODO think about setting back context loader
			} catch (InstantiationException ie) {
				throw new ServletException("Servlet class " + className + " can't instantiate. ", ie);
			} catch (IllegalAccessException iae) {
				throw new ServletException("Servlet class " + className + " can't access. ", iae);
			} catch (ClassNotFoundException cnfe) {
				log("", cnfe);
				throw new ServletException("Servlet class " + className + " not found. ", cnfe);
			} catch (Error e) {
				throw new ServletException("Servlet class " + className + " can't be instantiated or initialized due an error.", e);
			} catch (Throwable t) {
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
				throw new ServletException("Servlet class " + className + " can't be instantiated or initialized due an exception.", t);
			}
		}
		
		public String toString() {
			return "Servlet " + name + " class " + className + " path/patern " + Arrays.toString(mapping) + " init" + initParams + " inst " + instance;
		}
	}
	
	protected class FilterAccessDescriptor extends ServletAccessDescr implements FilterConfig {
		String[] servletNames;
		
		Filter filterInstance;
		
		DispatcherType[] dispatchTypes;
		
		public java.lang.String getFilterName() {
			return name;
		}
		
		public void add(String name) {
			// note the local name shadows name as class memeber
			if (servletNames == null)
				servletNames = new String[1];
			else
				servletNames = Utils.copyOf(servletNames, servletNames.length + 1);
			servletNames[servletNames.length - 1] = name;
		}
		
		public void add(DispatcherType dispatcher) {
			if (dispatchTypes == null)
				dispatchTypes = new DispatcherType[1];
			else {
				DispatcherType[] copy = new DispatcherType[dispatchTypes.length + 1];
				System.arraycopy(dispatchTypes, 0, copy, 0, dispatchTypes.length);
				dispatchTypes = copy;
			}
			dispatchTypes[dispatchTypes.length - 1] = dispatcher;
		}
		
		int matchServlet(String servletName) {
			if (servletNames == null)
				return -1;
			for (int i = 0; i < this.servletNames.length; i++)
				if (servletNames[i].equals(servletName))
					return i;
			return -1;
		}
		
		boolean matchDispatcher(DispatcherType dispatcher) {
			if (dispatchTypes == null)
				if (dispatcher.equals(DispatcherType.REQUEST))
					return true;
				else
					return false;
			for (int i = 0; i < dispatchTypes.length; i++)
				if (dispatcher.equals(dispatchTypes[i]))
					return true;
			return false;
		}
		
		protected Filter newFilterInstance() throws ServletException {
			try {
				Class<Filter> filterClass = (Class<Filter>) ucl.loadClass(className);
				WebFilter annot = filterClass.getAnnotation(WebFilter.class);
				if (annot != null)
					asyncSupported = annot.asyncSupported();
				filterInstance = filterClass.newInstance();
				filterInstance.init(this);
			} catch (InstantiationException ie) {
				throw new ServletException("Filter class " + className + " can't instantiate. ", ie);
			} catch (IllegalAccessException iae) {
				throw new ServletException("Filter class " + className + " can't access. ", iae);
			} catch (ClassNotFoundException cnfe) {
				throw new ServletException("Filter class " + className + " not found. ", cnfe);
			}
			return filterInstance;
		}
		
		public String toString() {
			return String.format("Filter for servlets %s and types %s based on %s", Arrays.toString(servletNames), Arrays.toString(dispatchTypes), super.toString());
		}
	}
	
	protected static class ErrorPageDescr {
		String errorPage;
		Class<?> exception;
		int errorCode;
		
		ErrorPageDescr(String page, String exClass, String code) {
			if (page == null || page.length() == 0 || page.charAt(0) != '/')
				throw new IllegalArgumentException("Error page path '" + page + "' must start with '/'");
			if (page.charAt(0) == '/') {
				errorPage = page;
			} else {
				errorPage = "/" + page;
			}
			
			try {
				exception = Class.forName(exClass);
			} catch (Exception e) {
				
			}
			try {
				errorCode = Integer.parseInt(code);
			} catch (Exception e) {
				
			}
		}
	}
	
	protected static class JspForwarder extends HttpServlet {
		String jsp;
		
		@Override
		public void init(ServletConfig conf) throws ServletException {
			super.init(conf);
			jsp = conf.getInitParameter("jsp-file");
			if (jsp == null || jsp.toLowerCase().endsWith(".jsp") == false)
				throw new ServletException("Not properly configured JSP forwarder");
			if (jsp.startsWith("/") == false)
				jsp = "/" + jsp;
		}
		
		@Override
		protected void service(HttpServletRequest hreq, HttpServletResponse hresp) throws ServletException, IOException {
			// TODO clean forward attributes wrapping request
			// System.err.printf("%s-%s%n", hreq.getRequestURI(),
			// hreq.getRequestURL());
			hreq.setAttribute("javax.servlet.tjws.servlet-jsp", true);
			hreq.getRequestDispatcher(jsp).forward(hreq, hresp);
		}
	}
	
	protected WebAppServlet(String context) {
		this.contextPath = "/" + context;
		attributes = new Hashtable<String, Object>();
		contextParameters = new Hashtable<String, String>();
		applyCompression = System.getProperty("tjws.webapp." + context + ".compressresponse") != null;
		if (applyCompression == false)
			applyCompression = System.getProperty(FileServlet.DEF_USE_COMPRESSION) != null;
		// TODO consider
		// _DEBUG = System.getProperty(getClass().getName() + ".debug") != null;
		String threadPoolSets[] = System.getProperty("tjws.webapp." + context + ".threadpoolsets", System.getProperty("tjws.webapp.*.threadpoolsets", "20,50,300")).split(",");
		if (threadPoolSets.length != 3)
			throw new IllegalArgumentException("Illegal thread pool settings:" + System.getProperty("tjws.webapp." + context + ".threadpoolsets"));
		asyncThreads = new ThreadPoolExecutor(Integer.parseInt(threadPoolSets[0]), Integer.parseInt(threadPoolSets[1]), Integer.parseInt(threadPoolSets[2]), TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread result = new Thread(r, "Pooled-t-" + contextPath);
				result.setDaemon(true);
				// result.setContextClassLoader(ucl);
				return result;
			}
			
		});
		System.getProperty("tjws.webapp." + context + ".multipartssets", System.getProperty("tjws.webapp.*.multipartssets", "$temp,50m,100m,2m"));
	}
	
	/**
	 * Initializes singleton delegator
	 * 
	 * @param acd
	 */
	public static void setAppContextDelegator(AppContextDelegator acd) {
		if (appContextDelegator == null)
			appContextDelegator = acd;
	}
	
	public static WebAppServlet create(File deployDir, String context, Serve server, String virtualHost) throws ServletException {
		// TODO add method initFromDeployDirectory and move all static calls
		// there,
		// so it should look like
		// WebAppServlet result = new WebAppServlet(context);
		// result.initFromDeployDirectory(deployDir, server);
		// TODO split also the method on sections of web.xml
		XPath xp = XPathFactory.newInstance().newXPath();
		final WebAppServlet result = new WebAppServlet(context);
		result.server = server;
		FileInputStream webxml = null;
		boolean error = true;
		try {
			// initialize deployDir
			result.makeCP(deployDir); // /web-app
			if (appContextDelegator != null) {
				File contextDef = new File(deployDir, "META-INF/context.xml");
				if (contextDef.exists())
					appContextDelegator.add(context, new WebApp.MetaContext(contextDef.getPath(), result.ucl));
			}
			Node document = (Node) xp.evaluate("/*", new InputSource(webxml = new FileInputStream(new File(deployDir, "WEB-INF/web.xml"))), XPathConstants.NODE);
			// TODO process "web-fragment.xml" as well
			final String namespaceURI = document.getNamespaceURI();
			String prefix = namespaceURI == null ? "" : "j2ee:";
			xp.setNamespaceContext(new NamespaceContext() {
				public String getNamespaceURI(String prefix) {
					// System.err.printf("Resolver called with %s%n", prefix);
					if (prefix == null)
						throw new IllegalArgumentException("Namespace prefix is null.");
					if (namespaceURI == null)
						return XMLConstants.NULL_NS_URI;
					if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX))
						return namespaceURI;
					if ("j2ee".equals(prefix))
						return namespaceURI;
					return XMLConstants.NULL_NS_URI;
				}
				
				public String getPrefix(String arg0) {
					throw new UnsupportedOperationException("getPrefix(" + arg0 + ");");
				}
				
				public Iterator getPrefixes(String arg0) {
					throw new UnsupportedOperationException("getPrefixes(" + arg0 + ");");
				}
			});
			document = (Node) xp.evaluate("//" + prefix + "web-app", document, XPathConstants.NODE);
			if ("yes".equals(System.getProperty(WAR_NAME_AS_CONTEXTPATH)) == false)
				result.contextName = (String) xp.evaluate(prefix + "display-name", document, XPathConstants.STRING);
			if (result.contextName == null || result.contextName.length() == 0)
				result.contextName = context;
			else
				result.contextPath = "/" + result.contextName;
			// result.origContextName = result.contextName;
			// result.origContextPath = result.contextPath;
			if (result.contextName.equals(System.getProperty(WAR_DEPLOY_IN_ROOT + (virtualHost == null ? "" : '.' + virtualHost)))) {
				result.log(String.format("Conext %s deployed as root ", result.contextName, virtualHost == null ? "" : virtualHost == null), null);
				result.contextPath = "";
				result.contextName = "";
			}
			result.virtualHost = virtualHost;
			Node metadataAttr = document.getAttributes().getNamedItem("metadata-complete");
			if (metadataAttr != null)
				result.noAnnot = "true".equals(metadataAttr.getTextContent());
			result.description = (String) xp.evaluate(prefix + "description", document, XPathConstants.STRING);
			if (result.description == null || result.description.length() == 0)
				result.description = "Web application :" + result.contextName;
			// context parameters
			NodeList nodes = (NodeList) xp.evaluate(prefix + "context-param", document, XPathConstants.NODESET);
			int nodesLen = nodes.getLength();
			for (int p = 0; p < nodesLen; p++) {
				result.contextParameters.put((String) xp.evaluate(prefix + "param-name", nodes.item(p), XPathConstants.STRING), (String) xp.evaluate(prefix + "param-value", nodes.item(p), XPathConstants.STRING));
			}
			// session-config <session-timeout>
			Number num = (Number) xp.evaluate(prefix + "session-config/" + prefix + "session-timeout", document, XPathConstants.NUMBER);
			if (num != null)
				result.sessionTimeout = num.intValue();
			if (result.sessionTimeout < 0)
				result.sessionTimeout = 0;
			else
				result.sessionTimeout *= 60;
			result.initTimeout = 10;
			Integer initTimeout = Integer.getInteger(String.format(WEBAPPINITTIMEOUT, result.contextName));
			
			if (initTimeout == null)
				initTimeout = Integer.getInteger(String.format(WEBAPPINITTIMEOUT, "*"));
			if (initTimeout != null)
				result.initTimeout = initTimeout;
			String flag = (String) xp.evaluate(prefix + "session-config/" + prefix + "cookie-config/" + prefix + "http-only", document, XPathConstants.STRING);
			if (flag.length() > 0) {
				server.httpSessCookie = "YES".equalsIgnoreCase(flag) || "TRUE".equalsIgnoreCase(flag);
			}
			flag = (String) xp.evaluate(prefix + "session-config/" + prefix + "cookie-config/" + prefix + "secure", document, XPathConstants.STRING);
			if (flag.length() > 0) {
				server.secureSessCookie = "YES".equalsIgnoreCase(flag) || "TRUE".equalsIgnoreCase(flag);
			}
			// TODO decide 1) if it is right place 2) do check for Android
			Thread.currentThread().setContextClassLoader(result.ucl);
			try {
				nodes = (NodeList) xp.evaluate(prefix + "resource-env-ref", document, XPathConstants.NODESET);
				nodesLen = nodes.getLength();
				for (int i = 0; i < nodesLen; i++) {
					Node n = nodes.item(i);
					result.log(String.format("Processing env-ref-%s", xp.evaluate(prefix + "description", n, XPathConstants.STRING)));
					Object link = appContextDelegator == null ? null : appContextDelegator.lookupLink((String) xp.evaluate(prefix + "resource-env-ref-name", n, XPathConstants.STRING));
					if (link == null || link.getClass().getName().equals(xp.evaluate(prefix + "resource-env-ref-type", n, XPathConstants.STRING)) == false)
						result.log(String.format("Web container doesn't provide an administered object %s of %s", xp.evaluate(prefix + "resource-env-ref-name", n, XPathConstants.STRING), xp.evaluate(prefix + "resource-env-ref-type", n, XPathConstants.STRING)));
				}
				nodes = (NodeList) xp.evaluate(prefix + "resource-ref", document, XPathConstants.NODESET);
				nodesLen = nodes.getLength();
				for (int i = 0; i < nodesLen; i++) {
					Node n = nodes.item(i);
					result.log(String.format("Processing resource-ref-%s", xp.evaluate(prefix + "description", n, XPathConstants.STRING)));
					String name = (String) xp.evaluate(prefix + "res-ref-name", n, XPathConstants.STRING);
					String type = toNormalizedString(xp.evaluate(prefix + "res-type", n, XPathConstants.STRING));
					String auth = toNormalizedString(xp.evaluate(prefix + "res-auth", n, XPathConstants.STRING));
					String scope = toNormalizedString(xp.evaluate(prefix + "res-sharing-scope", n, XPathConstants.STRING));
					Object res = appContextDelegator == null ? null : appContextDelegator.lookup(name);
					if (res == null)
						result.log(String.format("No resource %s is available", name));
					else {
						Class<?> typeClass = null;
						if (type != null && type.length() > 0) {
							try {
								typeClass = Class.forName(type);
							} catch (ClassNotFoundException cne) {
								result.log(String.format("No definition of class %s found, type check is bypassed", type));
								// TODO res.getClasses()
							}
						}
						
						if (typeClass != null && typeClass.isInstance(res) == false) {
							result.log(String.format("No resource %s of %s is available", name, type));
						} else {
							result.log(String.format("Confirmed availability of %s of %s authorized by %s in scope of %s", name, type, auth, scope));
						}
					}
				}
				if (appContextDelegator != null) {
					nodes = (NodeList) xp.evaluate(prefix + "env-entry", document, XPathConstants.NODESET);
					nodesLen = nodes.getLength();
					for (int i = 0; i < nodesLen; i++) {
						Node n = nodes.item(i);
						result.log(String.format("Processing env-entry-%s", xp.evaluate(prefix + "description", n, XPathConstants.STRING)));
						Object value = xp.evaluate(prefix + "env-entry-value", n, XPathConstants.STRING);
						if (value != null) {
							String type = (String) xp.evaluate(prefix + "env-entry-type", n, XPathConstants.STRING);
							if (type != null && ("java.lang.String".equals(type)) == false) {
								// TODO can use reflection for shortness,
								// however we should check allowed types
								if ("java.lang.Boolean".equals(type))
									value = new Boolean((String) value);
								else if ("java.lang.Byte".equals(type))
									value = new Byte((String) value);
								else if ("java.lang.Character".equals(type) && ((String) value).length() == 1)
									value = new Character(((String) value).charAt(0));
								else if ("java.lang.Short".equals(type))
									value = new Short((String) value);
								else if ("java.lang.Integer".equals(type))
									value = new Integer((String) value);
								else if ("java.lang.Long".equals(type))
									value = new Long((String) value);
								else if ("java.lang.Float".equals(type))
									value = new Float((String) value);
								else if ("java.lang.Double".equals(type))
									value = new Double((String) value);
							}
							appContextDelegator.add((String) xp.evaluate(prefix + "env-entry-name", n, XPathConstants.STRING), value);
						}
					}
				}
			} catch (Exception e) {
				result.log("A problem in obtaining context, all context related settings will be ignored", e);
			}
			// bypass EJB stuff
			if (nodesLen > 0)
				result.log("EJB references are not supported");
			nodes = (NodeList) xp.evaluate(prefix + "ejb-local-ref", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			if (nodesLen > 0)
				result.log("Local EJB references are not supported");
			// /////////////////////////////////////////////////////////////////
			// listeners listener-class
			nodes = (NodeList) xp.evaluate(prefix + "listener/" + prefix + "listener-class", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			if (nodesLen > 0) {
				result.listeners = new ArrayList<EventListener>(nodesLen);
				for (int i = 0; i < nodesLen; i++)
					try {
						EventListener eventListener = (EventListener) result.ucl.loadClass(toNormalizedString(nodes.item(i).getTextContent())).newInstance();
						if (eventListener instanceof HttpSessionListener || eventListener instanceof HttpSessionAttributeListener) {
							if (result.sessionListeners == null)
								result.sessionListeners = new ArrayList<EventListener>(nodesLen);
							result.sessionListeners.add((EventListener) eventListener);
						}
						
						if (eventListener instanceof ServletRequestListener) {
							if (result.requestListeners == null)
								result.requestListeners = new ArrayList<ServletRequestListener>(nodesLen);
							result.requestListeners.add((ServletRequestListener) eventListener);
						}
						
						if (eventListener instanceof ServletRequestAttributeListener) {
							if (result.attributeListeners == null)
								result.attributeListeners = new ArrayList<ServletRequestAttributeListener>(nodesLen);
							result.attributeListeners.add((ServletRequestAttributeListener) eventListener);
						}
						result.listeners.add(eventListener); // because the same
																// class can
																// implement
																// other
																// listener
																// interfaces
					} catch (Exception e) {
						result.log("Event listener " + nodes.item(i).getTextContent() + " can't be created due an exception.", e);
					} catch (Error e) {
						result.log("Event listener " + nodes.item(i).getTextContent() + " can't be created due an error.", e);
					}
			}
			// restore sessions for this context
			// serve.sessions.restore for the current context
			
			// notify context listeners
			if (result.listeners != null)
				for (EventListener listener : result.listeners) {
					if (listener instanceof ServletContextListener) {
						final ServletContextListener contListener = (ServletContextListener) listener;
						contListener.contextInitialized(new ServletContextEvent(result));
					}
				}
			
			// read global multi part
			Node mp = (Node) xp.evaluate(prefix + "multipart-form", document, XPathConstants.NODE);
			boolean casualMultipart = mp != null && mp.hasAttributes() && mp.getAttributes().getNamedItem("enable") != null && "true".equals(mp.getAttributes().getNamedItem("enable").getTextContent());
			long maxUpload = 0;
			if (casualMultipart) {
				mp = mp.getAttributes().getNamedItem("upload-max");
				if (mp != null)
					try {
						maxUpload = Long.parseLong(mp.getTextContent());
						if (maxUpload < 0)
							maxUpload = 0;
					} catch (Exception e) {
						
					}
			}
			// process filters
			nodes = (NodeList) xp.evaluate(prefix + "filter", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			result.filters = new ArrayList<FilterAccessDescriptor>(nodesLen);
			for (int i = 0; i < nodesLen; i++) {
				Node n = nodes.item(i);
				FilterAccessDescriptor fad = result.createFilterDescriptor();
				fad.name = (String) xp.evaluate(prefix + "filter-name", n, XPathConstants.STRING);
				fad.className = toNormalizedString(xp.evaluate(prefix + "filter-class", n, XPathConstants.STRING));
				if (fad.className == null)
					throw new ServletException(String.format("Filter %s specified without or empty class.", fad.name));
				String value = (String) xp.evaluate(prefix + "async-supported", n, XPathConstants.STRING);
				if (value.length() > 0) {
					value = value.toUpperCase();
					fad.asyncSupported = "YES".equals(value) || "TRUE".equals(value);
				}
				fad.label = (String) xp.evaluate(prefix + "display-name", n, XPathConstants.STRING);
				fad.descr = (String) xp.evaluate(prefix + "description", n, XPathConstants.STRING);
				NodeList params = (NodeList) xp.evaluate(prefix + "init-param", n, XPathConstants.NODESET);
				fad.initParams = new HashMap<String, String>(params.getLength());
				for (int p = 0; p < params.getLength(); p++) {
					fad.initParams.put((String) xp.evaluate(prefix + "param-name", params.item(p), XPathConstants.STRING), (String) xp.evaluate(prefix + "param-value", params.item(p), XPathConstants.STRING));
				}
				fad.multipartEnabled = casualMultipart;
				fad.multipartMaxRequest = maxUpload << 10;
				result.filters.add(fad);
			}
			// process filter's mapping
			for (FilterAccessDescriptor fad : result.filters) {
				nodes = (NodeList) xp.evaluate(prefix + "filter-mapping[" + prefix + "filter-name=\"" + fad.name + "\"]", document, XPathConstants.NODESET);
				nodesLen = nodes.getLength();
				if (nodesLen == 0)
					throw new ServletException(String.format("No mappings were found for the filter %s", fad.name));
				for (int i = 0; i < nodesLen; i++) {
					Node n = nodes.item(i);
					NodeList clarifications = (NodeList) xp.evaluate(prefix + "url-pattern", n, XPathConstants.NODESET);
					int claLen = clarifications.getLength();
					for (int j = 0; j < claLen; j++) {
						String mapUrl = clarifications.item(j).getTextContent();
						if (mapUrl == null || mapUrl.length() == 0)
							continue;
						fad.add(new MappingEntry(clearPath(mapUrl), buildREbyPathPatt(mapUrl)));
					}
					clarifications = (NodeList) xp.evaluate(prefix + "dispatcher", n, XPathConstants.NODESET);
					claLen = clarifications.getLength();
					for (int j = 0; j < claLen; j++) {
						String filterType = clarifications.item(j).getTextContent();
						if (filterType == null || filterType.length() == 0)
							fad.add(DispatcherType.REQUEST);
						else
							fad.add(DispatcherType.valueOf(filterType));
					}
					clarifications = (NodeList) xp.evaluate(prefix + "servlet-name", n, XPathConstants.NODESET);
					claLen = clarifications.getLength();
					for (int j = 0; j < claLen; j++) {
						// adding servlet name
						fad.add(clarifications.item(j).getTextContent());
					}
				}
				fad.newFilterInstance();
			}
			// servlets
			nodes = (NodeList) xp.evaluate(prefix + "servlet", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			result.servlets = new ArrayList<ServletAccessDescr>(nodesLen + 1); // +jsp
			for (int i = 0; i < nodesLen; i++) {
				Node n = nodes.item(i);
				ServletAccessDescr sad = result.createDescriptor();
				sad.name = (String) xp.evaluate(prefix + "servlet-name", n, XPathConstants.STRING);
				sad.className = toNormalizedString(xp.evaluate(prefix + "servlet-class", n, XPathConstants.STRING));
				String jspFile = null;
				if (sad.className == null || sad.className.length() == 0) {
					jspFile = (String) xp.evaluate(prefix + "jsp-file", n, XPathConstants.STRING);
					if (jspFile != null) {
						sad.className = JspForwarder.class.getName();
					} else
						throw new ServletException(String.format("Servlet %s specified without class or jsp file.", sad.name));
				}
				sad.label = (String) xp.evaluate(prefix + "display-name", n, XPathConstants.STRING);
				sad.descr = (String) xp.evaluate(prefix + "description", n, XPathConstants.STRING);
				String loadOnStartVal = toNormalizedString(xp.evaluate(prefix + "load-on-startup", n, XPathConstants.STRING));
				try {
					sad.loadOnStart = Integer.parseInt(loadOnStartVal);
				} catch (NumberFormatException nfe) {
					loadOnStartVal = loadOnStartVal.toUpperCase();
					sad.loadOnStart = "YES".equals(loadOnStartVal) || "TRUE".equals(loadOnStartVal) ? 0 : -1;
				}
				String value = toNormalizedString(xp.evaluate(prefix + "async-supported", n, XPathConstants.STRING));
				if (value.length() > 0) {
					sad.asyncSupported = "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value);
				}
				sad.runAs = (String) xp.evaluate(prefix + "run-as", n, XPathConstants.STRING);
				value = (String) xp.evaluate(prefix + "enabled", n, XPathConstants.STRING);
				
				NodeList params = (NodeList) xp.evaluate(prefix + "init-param", n, XPathConstants.NODESET);
				sad.initParams = new HashMap<String, String>(params.getLength() + (jspFile == null ? 0 : 1));
				for (int p = 0; p < params.getLength(); p++) {
					sad.initParams.put((String) xp.evaluate(prefix + "param-name", params.item(p), XPathConstants.STRING), (String) xp.evaluate(prefix + "param-value", params.item(p), XPathConstants.STRING));
				}
				if (jspFile != null) {
					if (sad.initParams.containsKey("jsp-file"))
						throw new ServletException("Conflicting iniit parameter jsp-file for JSP servlet " + sad);
					sad.initParams.put("jsp-file", jspFile);
				}
				NodeList multiparts = (NodeList) xp.evaluate(prefix + "multipart-config", n, XPathConstants.NODESET);
				if (multiparts.getLength() == 1) {
					sad.multipartEnabled = true;
					Node multipart = (Node) multiparts.item(0);
					value = (String) xp.evaluate(prefix + "location", multipart, XPathConstants.STRING);
					if (value.length() > 0)
						sad.multipartLocation = new File(value);
					value = toNormalizedString(xp.evaluate(prefix + "max-file-size", multipart, XPathConstants.STRING));
					if (value.length() > 0)
						sad.multipartMaxFile = Long.parseLong(value);
					value = toNormalizedString(xp.evaluate(prefix + "max-request-size", multipart, XPathConstants.STRING));
					if (value.length() > 0)
						sad.multipartMaxRequest = Long.parseLong(value);
					value = toNormalizedString(xp.evaluate(prefix + "file-size-threshold", multipart, XPathConstants.STRING));
					if (value.length() > 0)
						sad.multipartThreshold = Integer.parseInt(value);
				}
				NodeList securityRoles = (NodeList) xp.evaluate(prefix + "security-role-ref", n, XPathConstants.NODESET);
				NodeList descriptionGroups = (NodeList) xp.evaluate(prefix + "descriptionGroup", n, XPathConstants.NODESET);
				result.servlets.add(sad);
			}
			// assure order of initialization
			Collections.sort(result.servlets);
			// get mappings
			ServletAccessDescr wasDefault = null;
			for (ServletAccessDescr sad : result.servlets) {
				nodes = (NodeList) xp.evaluate(prefix + "servlet-mapping[" + prefix + "servlet-name=\"" + sad.name + "\"]", document, XPathConstants.NODESET);
				nodesLen = nodes.getLength();
				// System.err.printf("Found %d mappings for %s%n", nodesLen,
				// sad);
				if (nodesLen == 0) {
					// no mapping at all
					String urlPat = "/" + sad.name + "/*";
					sad.add(new MappingEntry(clearPath(urlPat), buildREbyPathPatt(urlPat)));
				} else
					for (int i = 0; i < nodesLen; i++) {
						NodeList maps = (NodeList) xp.evaluate(prefix + "url-pattern", nodes.item(i), XPathConstants.NODESET);
						int mapsLen = maps.getLength();
						// System.err.printf("Found %d patterns for %s%n",
						// mapsLen, sad);
						if (mapsLen == 0) {
							// mapping with empty pattern
							String urlPat = "/" + sad.name + "/*";
							sad.add(new MappingEntry(clearPath(urlPat), buildREbyPathPatt(urlPat)));
						} else {
							for (int j = 0; j < mapsLen; j++) {
								String urlPat = maps.item(j).getTextContent();
								if (urlPat.equals("/"))
									if (wasDefault != null)
										throw new ServletException("More than one default servlets defined " + sad);
									else
										wasDefault = sad;
								sad.add(new MappingEntry(clearPath(urlPat), buildREbyPathPatt(urlPat)));
							}
						}
					}
				// System.err.printf("Servlet %s, path:%s\n", sad,
				// sad.servPath);
				if (sad.loadOnStart >= 0)
					sad.newInstance();
			}
			// additional jsp mapping
			nodes = (NodeList) xp.evaluate(prefix + "jsp-config/" + prefix + "jsp-property-group/" + prefix + "url-pattern", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			ServletAccessDescr jsp;
			if (nodesLen > 0) {
				List<String> jspPats = new ArrayList<String>(nodesLen);
				for (int i = 0; i < nodesLen; i++) {
					jspPats.add(nodes.item(i).getTextContent());
				}
				jsp = result.addJSPServlet(jspPats);
			} else
				jsp = result.addJSPServlet(null);
			jsp.multipartEnabled = casualMultipart;
			jsp.multipartMaxRequest = maxUpload << 10;
			
			if (wasDefault != null) {
				// re-add at the end
				result.servlets.remove(wasDefault);
				result.servlets.add(wasDefault);
			}
			// welcome files
			nodes = (NodeList) xp.evaluate(prefix + "welcome-file-list/" + prefix + "welcome-file", document, XPathConstants.NODESET);
			result.welcomeFiles = new ArrayList<String>(nodes.getLength() + 1);
			nodesLen = nodes.getLength();
			if (nodesLen > 0)
				for (int wfi = 0; wfi < nodesLen; wfi++)
					result.welcomeFiles.add(nodes.item(wfi).getTextContent());
			else {
				result.welcomeFiles.add("index.html");
				result.welcomeFiles.add("index.jsp");
			}
			// error pages
			nodes = (NodeList) xp.evaluate(prefix + "error-page", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			if (nodesLen > 0) {
				result.errorPages = new ArrayList<ErrorPageDescr>(nodesLen);
				for (int i = 0; i < nodesLen; i++) {
					Node n = nodes.item(i);
					result.errorPages.add(new WebAppServlet.ErrorPageDescr((String) xp.evaluate(prefix + "location", n, XPathConstants.STRING), (String) xp.evaluate(prefix + "exception-type", n, XPathConstants.STRING), (String) xp.evaluate(prefix + "error-code", n, XPathConstants.STRING)));
				}
			}
			// mime types
			nodes = (NodeList) xp.evaluate(prefix + "mime-mapping", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			if (nodesLen > 0) {
				result.mimes = new HashMap<String, String>(nodesLen);
				for (int i = 0; i < nodesLen; i++) {
					Node n = nodes.item(i);
					result.mimes.put(((String) xp.evaluate(prefix + "extension", n, XPathConstants.STRING)).toLowerCase(), (String) xp.evaluate(prefix + "mime-type", n, XPathConstants.STRING));
				}
			}
			// bypass security stuff
			nodes = (NodeList) xp.evaluate(prefix + "security-constraint", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			if (nodesLen > 0)
				result.log("Security constraints are not supported");
			if (xp.evaluate(prefix + "login-config", document, XPathConstants.NODE) != null)
				result.log("Login config is not supported");
			nodes = (NodeList) xp.evaluate(prefix + "security-role", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			if (nodesLen > 0)
				result.log("Security roles are not supported");
			nodes = (NodeList) xp.evaluate(prefix + "ejb-ref", document, XPathConstants.NODESET);
			nodesLen = nodes.getLength();
			error = false;
		} catch (IOException ioe) {
			throw new ServletException("A problem in reading web.xml.", ioe);
		} catch (XPathExpressionException xpe) {
			// server.log("", xpe);
			throw new ServletException("A problem in parsing web.xml.", xpe);
		} finally {
			if (webxml != null)
				try {
					webxml.close();
				} catch (Exception e) {
				}
			if (error)
				try {
					result.destroy();
				} catch (Exception e) {
				}
		}
		result.scc = new SessionCookieConfigImpl(result);
		return deploywebsocket(result, deployDir, context, server, virtualHost);
	}
	
	public static WebAppServlet deploywebsocket(final WebAppServlet webApp, final File deployDir, String context, final Serve server, String virtualHost) throws ServletException {
		if (webApp.server.websocketProvider != null) {
			File file = new File(deployDir, "WEB-INF/lib");
			ArrayList<File> result = file.exists() && file.isDirectory() ? new ArrayList<File>(Arrays.asList(file.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					String name = pathname.getName().toLowerCase();
					return pathname.isFile() && (name.endsWith(".jar") || name.endsWith(".zip"));
				}
			}))) : new ArrayList<File>();
			file = new File(deployDir, "WEB-INF/classes");
			if (file.exists() && file.isDirectory())
				result.add(file);
			if (webApp.listeners == null)
				webApp.listeners = new ArrayList<EventListener>(2);
			webApp.server.websocketProvider.deploy(webApp, result);
		}
		return webApp;
	}
	
	static String toNormalizedString(Object o) {
		if (o == null)
			return "";
		if (o instanceof String)
			return ((String) o).trim();
		return o.toString().trim();
	}
	
	static public void setRuntimeEnv(Object rte) {
		runtimeEnv = rte;
	}
	
	static <D extends ServletAccessDescr> void addMultiple(NodeList list, D d) {
		// TODO can be solution for more compact code
	}
	
	static public String buildREbyPathPatt(String pathPat) {
		if (pathPat.length() == 0)
			return "/"; // context servlet
		if (pathPat.equals("/")) // default servlet
			return "/.*";
		if (pathPat.startsWith("*."))
			return pathPat.replace(".", "\\.").replace("?", ".").replace("*", ".*").replace("|", "\\|"); // +"\\??.*";
		// TODO think more
		int wcp = pathPat.indexOf('*');
		// if (wcp > 0 && pathPat.charAt(wcp - 1) == '/')
		// pathPat = pathPat.substring(0, wcp - 1) + '*';
		pathPat = pathPat.replace(".", "\\.").replace("?", ".").replace("*", ".*");
		if (wcp < 0)
			if (pathPat.endsWith("/") == false)
				pathPat += "/?";
		return pathPat;
	}
	
	static public String clearPath(String pathMask) {
		if (pathMask.equals("/"))
			return pathMask;
		if (pathMask.startsWith("*."))
			return "/";
		int wcp = pathMask.indexOf('*');
		if (wcp < 0)
			return pathMask;
		if (wcp == 1 && pathMask.charAt(0) == '/')
			return "";
		return pathMask.substring(0, wcp);
	}
	
	/**
	 * Converts the proxy object into <code>Serve.ServeConnection</code> class
	 * object.
	 * 
	 * @param proxy
	 * @return
	 */
	protected static Serve.ServeConnection toServeConnection(Object proxy) {
		if (proxy instanceof Serve.ServeConnection) {
			return (Serve.ServeConnection) proxy;
		} else if (proxy instanceof Openable) {
			final Object openableServeConnection = ((Openable) proxy).getOrigin();
			if (openableServeConnection instanceof Serve.ServeConnection) {
				return (Serve.ServeConnection) openableServeConnection;
			}
		}
		
		return null;
	}
	
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		// new Exception("call trace").printStackTrace();
		// TODO check access rights
		Thread.currentThread().setContextClassLoader(ucl);
		if (req.isSecure())
			fillSecureAttrs(req);
		final HttpServletRequest hreq = (HttpServletRequest) req;
		if (this.requestListeners != null) {
			ServletRequestEvent e = new ServletRequestEvent(this, hreq);
			for (ServletRequestListener rlistener : requestListeners)
				rlistener.requestInitialized(e);
		}
		
		try {
			String path = hreq.getPathInfo();
			// TODO: wrap request to implement methods like
			// getRequestDispatcher()
			// which supports relative path, no leading / means relative to
			// currently called
			if (_DEBUG)
				System.err.printf("Full req:%s, ContextPath: %s, ServletPath:%s, pathInfo:%s\n", hreq.getRequestURI(), hreq.getContextPath(), hreq.getServletPath(), path);
			SimpleFilterChain sfc = new SimpleFilterChain();
			if (path != null) {
				// note a limitation, servlet name can't start with /WEB-INF
				if (path.regionMatches(true, 0, "/WEB-INF", 0, "/WEB-INF".length()) || path.regionMatches(true, 0, "/META-INF", 0, "/META-INF".length())) {
					((HttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);
					return;
				}
				for (FilterAccessDescriptor fad : filters)
					if (fad.matchDispatcher(DispatcherType.REQUEST) && fad.matchPath(path) >= 0)
						sfc.add(fad);
				for (ServletAccessDescr sad : servlets) {
					if (_DEBUG)
						System.err.println("Trying matching " + path + " to " + Arrays.toString(sad.mapping) + " = " + sad.matchPath(path));
					int patIndex;
					if ((patIndex = sad.matchPath(path)) >= 0) {
						if (sad.instance == null) {
							if (sad.loadOnStart < 0)
								synchronized (sad) {
									if (sad.instance == null)
										sad.newInstance();
								}
							if (sad.instance == null) {
								sad.loadOnStart = Integer.MAX_VALUE; // mark
																		// unsuccessful
																		// instantiation
																		// and
																		// ban
																		// the
																		// servlet?
								((HttpServletResponse) res).sendError(HttpServletResponse.SC_GONE, "Servlet " + sad.name + " hasn't been instantiated successfully or has been unloaded.");
								return;
							}
						} else {
							if (sad.timeToReactivate > 0) {
								if (sad.timeToReactivate > System.currentTimeMillis()) {
									((HttpServletResponse) res).setIntHeader("Retry-After", (int) (sad.timeToReactivate - System.currentTimeMillis()) / 1000 + 1);
									// ((HttpServletResponse)
									// res).setDateHeader("Retry-After", new
									// Date(sad.timeToReactivate));
									((HttpServletResponse) res).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
									return;
								} else
									sad.timeToReactivate = 0;
							}
						}
						for (FilterAccessDescriptor fad : filters)
							if (fad.matchDispatcher(DispatcherType.REQUEST) && fad.matchServlet(sad.name) >= 0)
								sfc.add(fad);
						// sfc.add(fad.filterInstance);
						// System.err.println("used:"+
						// sad.servPath+", wanted:"+((WebAppServlet)
						// sad.getServletContext()).contextPath);
						sfc.setFilter(new WebAppContextFilter(sad.mapping[patIndex].servPath));
						// add servlet in chain
						sfc.setServlet(sad);
						sfc.reset();
						sfc.doFilter(req, res);
						return;
					}
				}
			} else {
				((HttpServletResponse) res).sendRedirect(hreq.getRequestURI() + "/");
				return;
			}
			
			// no matching, process as file
			sfc.setFilter(new WebAppContextFilter());
			sfc.setServlet(new HttpServlet() {
				public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
					String path = ((HttpServletRequest) req).getPathTranslated();
					returnFileContent(path, (HttpServletRequest) req, (HttpServletResponse) res);
				}
			});
			sfc.reset();
			sfc.doFilter(req, res);
		} finally {
			if (this.requestListeners != null) {
				ServletRequestEvent e = new ServletRequestEvent(this, hreq);
				for (ServletRequestListener rlistener : requestListeners)
					rlistener.requestDestroyed(e);
			}
		}
	}
	
	protected void fillSecureAttrs(ServletRequest req) {
		Serve.ServeConnection scon = toServeConnection(req);
		if (scon != null) {
			if (scon.getSocket() instanceof SSLSocket) {
				SSLSocket ssocket = (SSLSocket) scon.getSocket();
				SSLSession ssess = ssocket.getSession();
				String cipherSuite = ssess.getCipherSuite();
				req.setAttribute("javax.servlet.request.cipher_suite", cipherSuite);
				int cipherBits = 0;
				// TODO cache in session
				if (cipherSuite.indexOf("128") > 0)
					cipherBits = 128;
				else if (cipherSuite.indexOf("40") > 0)
					cipherBits = 40;
				else if (cipherSuite.indexOf("3DES") > 0)
					cipherBits = 168;
				else if (cipherSuite.indexOf("IDEA") > 0)
					cipherBits = 128;
				else if (cipherSuite.indexOf("DES") > 0)
					cipherBits = 56;
				req.setAttribute("javax.servlet.request.key_size", cipherBits);
				try {
					req.setAttribute("javax.servlet.request.X509Certificate", ssess.getPeerCertificateChain());
				} catch (SSLPeerUnverifiedException e) {
				}
			}
		} else
			log("Can't obtain an original request for " + req);
	}
	
	protected SimpleFilterChain buildFilterChain(String servletName, String requestPath, DispatcherType filterType) {
		SimpleFilterChain sfc = new SimpleFilterChain();
		// add path filters
		if (requestPath != null)
			for (FilterAccessDescriptor fad : filters)
				if (fad.matchDispatcher(filterType) && fad.matchPath(requestPath) >= 0)
					sfc.add(fad);
		/*
		 * if (filterType == error) {
		 * System.err.printf("JSP error %s ---> %s%n", request
		 * .getAttribute("javax.servlet.jsp.jspException"), request
		 * .getAttribute("javax.servlet.error.status_code"));
		 * ((Throwable)request
		 * .getAttribute("javax.servlet.jsp.jspException")).printStackTrace(); }
		 */
		
		// add name filters
		if (servletName != null)
			for (FilterAccessDescriptor fad : filters)
				if (fad.matchDispatcher(DispatcherType.REQUEST) && fad.matchServlet(servletName) >= 0)
					sfc.add(fad);
		return sfc;
	}
	
	protected void returnFileContent(String path, HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		// Note : can't call or forward to file servlet since it can be not
		// installed
		File fpath = new File(path);
		if (fpath.isDirectory()) {
			File baseDir = fpath;
			for (String indexPage : welcomeFiles) {
				fpath = new File(baseDir, indexPage);
				if (fpath.exists() && fpath.isFile()) {
					if (indexPage.charAt(0) != '/')
						indexPage = "/" + indexPage;
					RequestDispatcher rd = req.getRequestDispatcher(indexPage);
					if (rd != null) {
						rd.forward(req, res);
						return;
					}
					break;
				}
			}
		}
		if (fpath.exists() == false) {
			res.sendError(res.SC_NOT_FOUND);
			return;
		}
		if (fpath.isFile() == false) {
			res.sendError(res.SC_FORBIDDEN);
			return;
		}
		
		String temp = getMimeType(fpath.getName());
		res.setContentType(temp);
		
		long lastMod = fpath.lastModified();
		res.setDateHeader("Last-modified", lastMod);
		String ifModSinceStr = req.getHeader("If-Modified-Since");
		long ifModSince = -1;
		if (ifModSinceStr != null) {
			int semi = ifModSinceStr.indexOf(';');
			if (semi != -1)
				ifModSinceStr = ifModSinceStr.substring(0, semi);
			try {
				ifModSince = DateFormat.getDateInstance().parse(ifModSinceStr).getTime();
			} catch (Exception ignore) {
			}
		}
		if (ifModSince != -1 && ifModSince >= lastMod) {
			res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		// TODO add range handling
		boolean doCompress = false;
		if (applyCompression && temp != null && temp.startsWith("text")) {
			if (Utils.isGzipAccepted(req.getHeader("Accept-Encoding")) > 0) {
				res.setHeader("Content-Encoding", "gzip");
				doCompress = true;
			}
		}
		
		if ("HEAD".equals(req.getMethod())) {
			res.setHeader("Content-Length", Long.toString(fpath.length()));
			return;
		}
		OutputStream os = null;
		InputStream is = null;
		try {
			is = new FileInputStream(fpath);
			os = res.getOutputStream();
			if (doCompress)
				os = new GZIPOutputStream(os);
			else
				res.setHeader("Content-Length", Long.toString(fpath.length()));
			WarRoller.copyStream(is, os);
			if (doCompress)
				((GZIPOutputStream) os).finish();
		} catch (IllegalStateException ise) {
			// assure length
			res.setHeader("Content-Length", Long.toString(fpath.length()));
			PrintWriter pw = res.getWriter();
			// TODO decide on encoding/charset used by the reader
			String charSetName = res.getCharacterEncoding();
			if (charSetName == null) {
				charSetName = IOHelper.ISO_8859_1;
			}
			Utils.copyStream(new InputStreamReader(is, charSetName), pw);
			// consider Writer is OK to do not close
			// consider underneath stream closing OK
		} finally {
			try {
				is.close();
			} catch (Exception x) {
			}
			try {
				if (os != null)
					os.close();
			} catch (Exception x) {
			}
		}
	}
	
	protected ServletAccessDescr addJSPServlet(List<String> patterns) {
		ServletAccessDescr sad = createDescriptor();
		// get class name from serve
		sad.initParams = new HashMap<String, String>(10);
		Map<Object, Object> arguments = (Map<Object, Object>) server.arguments;
		sad.className = String.valueOf(arguments.get(Serve.ARG_JSP));
		if (sad.className == null) {
			log("No JSP engine specified, Apache Jasper is assumed by default", null);
			sad.className = "org.apache.jasper.servlet.JspServlet";
			sad.initParams.put("scratchdir", new File(deployDir, "META-INF/Jasper").getPath());
			sad.initParams.put("debug", System.getProperty(getClass().getName() + ".debug") != null ? "yes" : "no");
			sad.initParams.put("classpath", Utils.calculateClassPath(ucl));
		} else {
			String pnpx = sad.className + '.';
			int cnl = pnpx.length();
			String classPath = Utils.calculateClassPath(ucl);
			Iterator<Object> itr = arguments.keySet().iterator();
			while (itr.hasNext()) {
				String ipn = (String) itr.next();
				sad.initParams.put(ipn.substring(cnl), ((String) arguments.get(ipn)).replace("%context%", contextName).replace("%deploydir%", deployDir.getPath()).replace("%classloader%", WEBAPPCLASSLOADER).replace("%classpath%", classPath));
			}
		}
		
		sad.descr = "JSP support servlet";
		sad.label = "JSP";
		sad.loadOnStart = -1;
		sad.name = "jsp";
		String jspPat;
		if (patterns == null || patterns.size() == 0) {
			jspPat = "/.*\\.jsp.*";
		} else {
			jspPat = buildREbyPathPatt(patterns.get(0));
			for (int i = 1; i < patterns.size(); i++) {
				jspPat += "|" + buildREbyPathPatt(patterns.get(i));
			}
		}
		sad.add(new MappingEntry("/", jspPat));
		servlets.add(sad);
		return sad;
	}
	
	protected ServletAccessDescr createDescriptor() {
		return new ServletAccessDescr();
	}
	
	protected FilterAccessDescriptor createFilterDescriptor() {
		return new FilterAccessDescriptor();
	}
	
	protected void makeCP(File dd) throws IOException {
		deployDir = dd.getCanonicalFile();
		final List<URL> urls = new ArrayList<URL>();
		// add servlet classes
		ClassLoader cl = getClass().getClassLoader();
		while (cl != null) {
			if (cl instanceof URLClassLoader) {
				if (((URLClassLoader) cl).findResource("javax/servlet/jsp/JspPage.class") != null || ((URLClassLoader) cl).findResource("javax/servlet/http/HttpServlet.class") != null) {
					for (URL url : ((URLClassLoader) cl).getURLs())
						urls.add(url);
				}
			}
			cl = cl.getParent();
		}
		File classesFile = new File(deployDir, "WEB-INF/classes");
		if (classesFile.exists() && classesFile.isDirectory())
			try {
				urls.add(classesFile.toURL());
			} catch (java.net.MalformedURLException mfe) {
				
			}
		File libFile = new File(deployDir, "WEB-INF/lib");
		libFile.listFiles(new FileFilter() {
			public boolean accept(File file) {
				String name = file.getName().toLowerCase();
				if (name.endsWith(".jar") || name.endsWith(".zip"))
					try {
						urls.add(file.toURL());
					} catch (java.net.MalformedURLException mfe) {
						
					}
				return false;
			}
		});
		cpUrls = urls.toArray(new URL[urls.size()]);
		
		setAttribute(WEBAPPCLASSLOADER, ucl = createClassLoader(cpUrls, getClass().getClassLoader()));
		// System.err.println("CP "+urls+"\nLoader:"+ucl);
	}
	
	ClassLoader createClassLoader(URL[] classPath, ClassLoader parent) {
		String classLoaderClassName = System.getProperty(WebApp.DEF_WEBAPP_CLASSLOADER); // WEBAPPCLASSLOADER
		// ClassLoader result = null;
		if (classLoaderClassName != null) {
			try {
				// TODO consider constructor extra parameter
				// PermissionCollection permissionCollection
				return (ClassLoader) Class.forName(classLoaderClassName, true, parent).getConstructor(URL[].class, ClassLoader.class).newInstance(classPath, parent);
			} catch (Exception e) {
				log("Creation of custom class loader " + classLoaderClassName + " failed", e);
			}
		}
		return new URLClassLoader(cpUrls, getClass().getClassLoader()) {
			@Override
			public URL getResource(String name) {
				URL url = super.getResource(name);
				if (url == null && name.startsWith("/")) {
					url = super.getResource(name.substring(1));
				}
				return url;
			}
		};
	}
	
	public File getDeploymentDir() {
		return deployDir;
	}
	
	void dispatch(String path, ServletRequest request, ServletResponse response) throws ServletException, IOException {
		((SimpleDispatcher) request.getRequestDispatcher(path)).dispatch(request, response, DispatcherType.ASYNC);
	}
	
	/*
	 * protected URL toURL(File file) throws MalformedURLException {
	 * System.err.println
	 * ("file:/"+file.getAbsolutePath()+(file.isDirectory()?"/":"")); return new
	 * URL("file:/"+file.getAbsolutePath()+(file.isDirectory()?"/":"")); }
	 */
	
	// /////////////////////////////////////////////////////////////////////////////////
	// context methods
	@Override
	public String getContextPath() {
		return contextPath;
	}
	
	@Override
	public String getServletContextName() {
		return contextName;
	}
	
	@Override
	public String getServletInfo() {
		return description;
	}
	
	@Override
	public String getServletName() {
		return contextName;
	}
	
	@Override
	public void removeAttribute(String name) {
		Object value = attributes.remove(name);
		if (listeners != null)
			for (EventListener listener : listeners)
				if (listener instanceof ServletContextAttributeListener)
					((ServletContextAttributeListener) listener).attributeRemoved(new ServletContextAttributeEvent(this, name, value));
	}
	
	@Override
	public void setAttribute(String name, Object object) {
		// log("Set attr:"+name+" to "+object);
		if (object == null) {
			removeAttribute(name);
			return;
		}
		Object oldObj = attributes.put(name, object);
		if (listeners != null)
			for (EventListener listener : listeners) {
				if (listener instanceof ServletContextAttributeListener)
					if (oldObj == null)
						((ServletContextAttributeListener) listener).attributeAdded(new ServletContextAttributeEvent(this, name, object));
					else
						((ServletContextAttributeListener) listener).attributeReplaced(new ServletContextAttributeEvent(this, name, object));
			}
	}
	
	@Override
	public Enumeration getAttributeNames() {
		return attributes.keys();
	}
	
	@Override
	public Object getAttribute(String name) {
		// log("context: "+this+" return attr:"+name+" as
		// "+attributes.get(name));
		if (runtimeEnv != null && RUNTIMEENV_ATTR.equals(name))
			return runtimeEnv;
		return attributes.get(name);
	}
	
	@Override
	public String getServerInfo() {
		return "TJWS/J2EE container, Copyright &copy; 2010 - 2016 Dmitriy Rogatkin";
	}
	
	@Override
	public ServletContext getServletContext() {
		return this;
	}
	
	@Override
	public ServletConfig getServletConfig() {
		return this;
	}
	
	@Override
	public String getRealPath(String path) {
		path = validatePath(path);
		if (path == null)
			return null;
		else
			return new File(deployDir, path).getPath();
	}
	
	@Override
	public void log(String msg) {
		server.log((contextName == null ? "" : contextName) + "> " + msg);
	}
	
	@Override
	public void log(Exception exception, String msg) {
		server.log(exception, (contextName == null ? "" : contextName) + "> " + msg);
	}
	
	@Override
	public void log(String message, Throwable throwable) {
		server.log((contextName == null ? "" : contextName) + "> " + message, throwable);
	}
	
	@Override
	public Enumeration getServletNames() {
		Vector<String> result = new Vector<String>();
		for (ServletAccessDescr sad : servlets)
			result.add(sad.name);
		return result.elements();
	}
	
	@Override
	public Enumeration getServlets() {
		Vector<Servlet> result = new Vector<Servlet>();
		for (ServletAccessDescr sad : servlets)
			result.add(sad.instance);
		return result.elements();
		
	}
	
	@Override
	public Servlet getServlet(String name) throws ServletException {
		for (ServletAccessDescr sad : servlets)
			if (name.equals(sad.name))
				return sad.instance;
		throw new ServletException("No servlet " + name);
	}
	
	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		for (ServletAccessDescr sad : servlets)
			if (name.equals(sad.name)) {
				if (sad.instance == null && sad.loadOnStart < 0)
					try {
						sad.newInstance();
					} catch (ServletException se) {
					}
				if (sad.instance != null)
					return new SimpleDispatcher(name, sad.instance);
				else
					break;
			}
		return null;
	}
	
	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		if (_DEBUG)
			System.err.printf("getRequestDispatcher(%s)%n", path);
		if (path == null || path.length() == 0 || path.charAt(0) != '/')
			return null; // path must start with / for call from context
		// look for servlets first
		String clearPath = extractQueryAnchor(path, false);
		for (ServletAccessDescr sad : servlets) {
			if (_DEBUG)
				System.err.printf("For dispatcher trying match %s (%s) %s = %d%n", path, clearPath, Arrays.toString(sad.mapping), sad.matchPath(clearPath));
			int patIndex;
			if ((patIndex = sad.matchPath(clearPath)) >= 0) {
				if (sad.instance == null && sad.loadOnStart < 0)
					try {
						synchronized (sad) {
							if (sad.instance == null)
								sad.newInstance();
						}
					} catch (ServletException se) {
						log(String.format("Can't instantiate a %s exception %s", sad, se), se.getRootCause());
					}
				if (_DEBUG)
					System.err.printf("Found processing instance %s of %s%n", sad.instance, sad);
				if (sad.instance != null)
					return new SimpleDispatcher(sad.instance, sad.mapping[patIndex].servPath, path);
				else
					return null; // servlet not working
			}
		}
		// no matching servlets, check for resources
		try {
			if (_DEBUG)
				System.err.printf("Dispatching to resource %s%n", path);
			if (getResource(path) == null)
				throw new MalformedURLException(); // check path is valid
			return new SimpleDispatcher(new HttpServlet() {
				public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
					String path;
					if (((HttpServletRequest) req).getAttribute("javax.servlet.include.request_uri") != null)
						path = req.getRealPath((String) req.getAttribute("javax.servlet.include.path_info"));
					else
						path = ((HttpServletRequest) req).getPathTranslated();
					if (_DEBUG)
						System.err.printf("Dispatched file servlet for %s translated %s%n", path, ((HttpServletRequest) req).getPathTranslated());
					returnFileContent(path, (HttpServletRequest) req, (HttpServletResponse) res);
				}
			}, path);
		} catch (MalformedURLException mfe) {
		}
		return null;
	}
	
	@Override
	public InputStream getResourceAsStream(String path) {
		try {
			return getResource(path).openStream();
		} catch (NullPointerException npe) {
			if (_DEBUG)
				System.err.println("URL can't be created for :" + path);
		} catch (IOException ioe) {
			if (_DEBUG)
				ioe.printStackTrace();
		}
		return null;
	}
	
	@Override
	public URL getResource(String path) throws MalformedURLException {
		if (path.charAt(0) != '/') {
			throw new MalformedURLException("Path: " + path + " has to start with '/'");
		}
		path = extractQueryAnchor(path, false);
		// int ji = path.indexOf(".jar!/");
		try {
			File resFile = new File(getRealPath(path)).getCanonicalFile();
			if (resFile.exists()) {
				return resFile.toURL();
			}
		} catch (IOException io) {
		}
		
		return null;
	}
	
	@Override
	public Set getResourcePaths(String path) {
		if (path.charAt(0) != '/') {
			throw new IllegalArgumentException("getResourcePaths: path parameters must begin with '/'");
		}
		
		path = extractQueryAnchor(path, false);
		int jarIndex = path.indexOf(".jar!/");
		String jarPath = "";
		if (jarIndex > 0) {
			if (path.length() > jarIndex + ".jar!/".length()) {
				jarPath = path.substring(jarIndex + ".jar!/".length());
			}
			path = path.substring(0, jarIndex + 4);
		}
		
		File dir = new File(getRealPath(path));
		if (dir.exists() == false) {
			return null;
		}
		log("Path:" + path + ", dir:" + dir + ", jarPath:" + jarPath);
		Set<String> set = null;
		if (dir.isDirectory() == false) {
			if (jarIndex > 0) {
				set = new TreeSet<String>();
				try {
					JarFile jarFile = new JarFile(dir);
					int cp = jarPath.endsWith("/") ? jarPath.length() : jarPath.length() + 1;
					for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
						JarEntry entry = (JarEntry) entries.nextElement();
						String entryPath = entry.getName();
						if (cp == 1 || entryPath.startsWith(jarPath)) {
							if (entryPath.length() == cp) {
								continue;
							}
							
							int ns = entryPath.indexOf('/', cp + 1);
							// log("e Path:"+entryPath+", ns "+ns);
							if (ns > 0) {
								entryPath = entryPath.substring(0, ns + 1);
							}
							set.add(new URL("jar:file:/" + dir.getPath() + "!/" + entryPath).toString());
						}
					}
					jarFile.close();
				} catch (Exception ex) {
					log("Problem: " + ex);
				}
				
			}
			
			return set;
		}
		
		set = new TreeSet<String>();
		String[] els = dir.list();
		for (String el : els) {
			String fp = path + "/" + el;
			if (new File(getRealPath(fp)).isDirectory()) {
				fp += "/";
			}
			set.add("/" + fp);
		}
		
		return set;
	}
	
	@Override
	public String getMimeType(String file) {
		if (mimes != null && file != null) {
			int lastIndex = file.lastIndexOf('.');
			if (lastIndex > 0) {
				String result = mimes.get(file.substring(lastIndex).toLowerCase());
				if (result != null) {
					return result;
				}
			}
		}
		
		return server.getMimeType(file);
	}
	
	@Override
	public int getMinorVersion() {
		return 0;
	}
	
	@Override
	public int getMajorVersion() {
		return 3;
	}
	
	@Override
	public ServletContext getContext(String uripath) {
		Servlet servlet = server.getServlet(uripath);
		if (servlet != null) {
			return servlet.getServletConfig().getServletContext();
		}
		return null;
	}
	
	@Override
	public String getInitParameter(String name) {
		return contextParameters.get(name);
	}
	
	@Override
	public Enumeration getInitParameterNames() {
		return contextParameters.keys();
	}
	
	// ////////////// servlet spec 3.0 ///////////////////////
	
	/**
	 * Gets the minor version of the Servlet specification that the application
	 * represented by this ServletContext is based on.
	 */
	@Override
	public int getEffectiveMinorVersion() {
		return 0;
	}
	
	/**
	 * Gets the major version of the Servlet specification that the application
	 * represented by this ServletContext is based on.
	 * 
	 */
	@Override
	public int getEffectiveMajorVersion() {
		return 3;
	}
	
	/**
	 * Sets the context initialization parameter with the given name and value
	 * on this ServletContext.
	 * 
	 * @param name
	 *            of parameter
	 * @param value
	 *            of parameter
	 * @return true if parameter set
	 */
	@Override
	public boolean setInitParameter(java.lang.String name, java.lang.String value) {
		if (contextParameters != null)
			throw new IllegalStateException();
		return false;
	}
	
	/**
	 * Adds the servlet with the given name and class name to this servlet
	 * context.
	 * 
	 */
	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, String className) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Registers the given servlet instance with this ServletContext under the
	 * given servletName.
	 * 
	 */
	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Adds the servlet with the given name and class type to this servlet
	 * context.
	 * <p>
	 * The registered servlet may be further configured via the returned
	 * ServletRegistration object.
	 */
	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Instantiates the given Servlet class.
	 * <p>
	 * The returned Servlet instance may be further customized before it is
	 * registered with this ServletContext via a call to
	 * addServlet(String,Servlet).
	 */
	@Override
	public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Gets the ServletRegistration corresponding to the servlet with the given
	 * servletName.
	 * 
	 */
	@Override
	public ServletRegistration getServletRegistration(java.lang.String servletName) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Gets a (possibly empty) Map of the ServletRegistration objects (keyed by
	 * servlet name) corresponding to all servlets registered with this
	 * ServletContext.
	 */
	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Adds the filter with the given name and class name to this servlet
	 * context.
	 * 
	 */
	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, String className) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Registers the given filter instance with this ServletContext under the
	 * given filterName.
	 * 
	 */
	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Adds the filter with the given name and class type to this servlet
	 * context.
	 * 
	 */
	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Instantiates the given Filter class.
	 * 
	 */
	@Override
	public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Gets the FilterRegistration corresponding to the filter with the given
	 * filterName.
	 * 
	 */
	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Gets a (possibly empty) Map of the FilterRegistration objects (keyed by
	 * filter name) corresponding to all filters registered with this
	 * ServletContext.
	 * 
	 */
	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		throw new UnsupportedOperationException();
	}
	
	public String getVirtualServerName() {
		return virtualHost;
	}
	
	/**
	 * Gets the SessionCookieConfig object through which various properties of
	 * the session tracking cookies created on behalf of this ServletContext may
	 * be configured
	 * <p>
	 * Repeated invocations of this method will return the same
	 * SessionCookieConfig instance.
	 * 
	 */
	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		return scc;
	}
	
	/**
	 * Sets the session tracking modes that are to become effective for this
	 * ServletContext
	 * 
	 */
	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
		
	}
	
	/**
	 * Gets the session tracking modes that are supported by default for this
	 * ServletContext.
	 * 
	 */
	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		return dstm;
	}
	
	/**
	 * Gets the session tracking modes that are in effect for this
	 * ServletContext
	 * 
	 */
	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		return stm;
	}
	
	/**
	 * Adds the listener with the given class name to this ServletContext.
	 * 
	 */
	@Override
	public void addListener(String className) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Adds the given listener to this ServletContext.
	 * 
	 */
	@Override
	public <T extends EventListener> void addListener(T t) {
		listeners.add(t);
	}
	
	/**
	 * Adds a listener of the given class type to this ServletContext.
	 * <p>
	 * The given listenerClass must implement one or more of the following
	 * interfaces:
	 * <p>
	 * <ul>
	 * <li>ServletContextAttributeListener
	 * <li>ServletRequestListener
	 * <li>ServletRequestAttributeListener
	 * <li>HttpSessionListener
	 * <li>HttpSessionAttributeListener
	 * </ul>
	 */
	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Instantiates the given EventListener class.
	 * 
	 */
	@Override
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Gets the <jsp-config> related configuration that was aggregated from the
	 * web.xml and web-fragment.xml descriptor files of the web application
	 * represented by this ServletContext.
	 * 
	 */
	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Gets the class loader of the web application represented by this
	 * ServletContext.
	 * 
	 */
	@Override
	public ClassLoader getClassLoader() {
		// TODO figure out when SecurityException should be thrown
		return ucl;
	}
	
	/**
	 * Declares role names that are tested using isUserInRole.
	 * 
	 */
	@Override
	public void declareRoles(String... roleNames) {
		throw new UnsupportedOperationException();
	}
	
	protected void setErrorAttributes(ServletRequest req, int status, String msg, String servletName, String requestURI, Throwable t, Class<?> eclass) {
		req.setAttribute("javax.servlet.error.status_code", status);
		req.setAttribute("javax.servlet.error.exception_type ", eclass);
		req.setAttribute("javax.servlet.error.message", msg);
		req.setAttribute("javax.servlet.error.exception", t);
		req.setAttribute("javax.servlet.error.request_uri", requestURI);
		req.setAttribute("javax.servlet.error.servlet_name", servletName);
	}
	
	public static String validatePath(String path) {
		return Utils.canonicalizePath(path);
	}
	
	@Override
	public void destroy() {
		Thread.currentThread().setContextClassLoader(ucl);
		asyncThreads.shutdown();
		if (filters != null) {
			for (FilterAccessDescriptor fad : filters) {
				try {
					if (fad.filterInstance != null)
						fad.filterInstance.destroy();
				} catch (Exception e) {
					log("Exception in filter destroy", e);
				}
			}
		}
		
		for (ServletAccessDescr sad : servlets) {
			try {
				if (sad.instance != null)
					sad.instance.destroy();
			} catch (Exception e) {
				log("Exception in servlet destroy", e);
			}
		}
		
		if (requestListeners != null) {
			requestListeners.clear();
		}
		
		if (sessionListeners != null) {
			// no notification since session can persist
			sessionListeners.clear();
		}
		if (listeners != null) {
			for (int i = listeners.size() - 1; i > -1; i--) {
				EventListener listener = listeners.get(i);
				if (listener instanceof ServletContextListener) {
					try {
						((ServletContextListener) listener).contextDestroyed(new ServletContextEvent(this));
					} catch (Exception e) {
						log("Exception in context destroy notification", e);
					}
				}
			}
			
			listeners.clear();
		}
		Enumeration e = getAttributeNames();
		while (e.hasMoreElements())
			removeAttribute((String) e.nextElement());
		if (attributeListeners != null)
			attributeListeners.clear();
		if (appContextDelegator != null) {
			if (appContextDelegator.remove(contextName) == null)
				log(String.format("Context data for %s not found and not released from app container", contextName));
		}
		// log("Destroy");
	}
	
	protected class SimpleDispatcher implements RequestDispatcher {
		Servlet servlet;
		String servletPath;
		String path;
		String named;
		
		SimpleDispatcher(Servlet servlet, String path) {
			this(servlet, null, path);
		}
		
		SimpleDispatcher(String n, Servlet servlet) {
			this(servlet, null, null);
			named = n;
		}
		
		SimpleDispatcher(Servlet servlet, String servletPath, String path) {
			this.servlet = servlet;
			this.path = path;
			this.servletPath = servletPath;
			// if (servletPath.length() > 1 && servletPath.endsWith("/"))
			// servletPath = servletPath.substring(0, servletPath.length()-1);
			// ending '/' adjustment done on demand
		}
		
		@Override
		public String toString() {
			return String.format("Dispatcher for %s path %s name %s servlet %s", servlet, path, named, servletPath);
		}
		
		// //////////////////////////////////////////////////////////////////
		// interface RequestDispatcher
		// //////////////////////////////////////////////////////////////////
		
		/**
		 * 
		 * @param request
		 * @param response
		 * @throws ServletException
		 * @throws IOException
		 * @see javax.servlet.RequestDispatcher#forward(javax.servlet.ServletRequest,
		 *      javax.servlet.ServletResponse)
		 */
		@Override
		public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
			dispatch(request, response, DispatcherType.FORWARD);
		}
		
		/**
		 * 
		 * @param request
		 * @param response
		 * @param dispType
		 * @throws ServletException
		 * @throws IOException
		 */
		public void dispatch(ServletRequest request, ServletResponse response, DispatcherType dispType) throws ServletException, IOException {
			if (_DEBUG) {
				System.err.printf("%s path: %s, servlet: %s%n", dispType.equals(DispatcherType.ASYNC) ? "ASYNC_DISPATCH" : "FORWARD", path, servlet);
			}
			
			response.reset(); // drop all previously putting data and headers
			SimpleFilterChain sfc = buildFilterChain(named, path, request.getAttribute("javax.servlet.error.status_code") == null ? dispType : DispatcherType.ERROR);
			sfc.setServlet(servlet);
			sfc.reset();
			if (_DEBUG) {
				printRequestChain(request);
			}
			// try{Thread.sleep(1000);}catch(Exception e) {}
			boolean toJsp = request.getAttribute("javax.servlet.tjws.servlet-jsp") != null;
			sfc.doFilter(new DispatchedRequest((HttpServletRequest) request, toJsp ? DispatcherType.INCLUDE : dispType), response);
			// servlet.service(new DispatchedRequest((HttpServletRequest)
			// request, true), response);
		}
		
		void printRequestChain(ServletRequest servletRequest) {
			if (servletRequest instanceof DispatchedRequest) {
				ServletRequest tempServletRequest = servletRequest;
				while (tempServletRequest instanceof DispatchedRequest) {
					System.err.println("Wrapper req:" + tempServletRequest);
					tempServletRequest = ((DispatchedRequest) tempServletRequest).getRequest();
				}
				System.err.println("Original request:" + tempServletRequest);
			} else {
				System.err.println("Just request:" + servletRequest);
			}
		}
		
		@Override
		public void include(ServletRequest request, final ServletResponse response) throws ServletException, java.io.IOException {
			Serve.ServeConnection serveConnection = toServeConnection(response);
			if (serveConnection != null) {
				serveConnection.setInInclude(true);
			}
			if (_DEBUG) {
				System.err.printf("INCLUDE path: %s, servlet: %s, servlet path %s, name:%s%n", path, servlet, servletPath, named);
			}
			
			try {
				SimpleFilterChain filterChain = buildFilterChain(named, path, DispatcherType.INCLUDE);
				filterChain.setServlet(servlet);
				filterChain.reset();
				
				if (_DEBUG) {
					printRequestChain(request);
				}
				
				// send request to next filter, if any.
				filterChain.doFilter(new DispatchedRequest((HttpServletRequest) request, DispatcherType.INCLUDE), (HttpServletResponse) response);
			} finally {
				if (serveConnection != null) {
					serveConnection.setInInclude(false);
				}
			}
		}
		
		class DispatchedRequest extends HttpServletRequestWrapper {
			DispatcherType dispType;
			boolean forward;
			
			DispatchedRequest(HttpServletRequest request, DispatcherType dispType) {
				super(request);
				this.dispType = dispType;
				forward = dispType.equals(DispatcherType.FORWARD) || dispType.equals(DispatcherType.ASYNC);
				// System.err.printf("Created !!!!1 %s%n", dispType);
			}
			
			@Override
			public String getPathInfo() {
				return (forward ? getPathInfo1() : super.getPathInfo());
			}
			
			public String getPathInfo1() {
				if (path == null) {
					return super.getPathInfo();
				}
				
				if ("/".equals(servletPath)) {
					return null;
				}
				
				if (servletPath != null && servletPath.length() == 0 && "/".equals(path)) {
					return path;
				}
				
				int qPath = path.indexOf('?');
				// if (qp < 0)
				// qp = path.indexOf('#');
				int servletPathIndex = servletPath == null ? -1 : path.indexOf(servletPath);
				if (servletPathIndex >= 0) {
					servletPathIndex += servletPath.length() - (servletPath.endsWith("/") ? 1 : 0);
					if (_DEBUG) {
						System.err.printf("FORWARD getPathinfo() path %s, servlet %s, sp %d, res %s%n", path, servletPath, servletPathIndex, path.substring(servletPathIndex));
					}
					
					if (qPath > servletPathIndex) {
						return path.substring(servletPathIndex, qPath);
					} else {
						return path.substring(servletPathIndex);
					}
				}
				
				if (_DEBUG) {
					System.err.printf("FORWARD get pathinfo ret: %s%n", path);
				}
				
				if (qPath > 0) {
					return path.substring(0, qPath);
				}
				
				return path;
			}
			
			@Override
			public String getPathTranslated() {
				return getRealPath(getPathInfo());
			}
			
			@Override
			public String getRealPath(String path) {
				return WebAppServlet.this.getRealPath(path);
			}
			
			@Override
			public String getServletPath() {
				if (forward)
					return getServletPath1();
				return super.getServletPath();
			}
			
			public String getServletPath1() {
				if (servletPath != null)
					if (servletPath.equals("/"))
						return extractQueryAnchor(path, false);
					else
						return servletPath.endsWith("/") ? servletPath.substring(0, servletPath.length() - 1) : servletPath;
					
				return super.getServletPath();
			}
			
			public String getRequestURI1() {
				if (path == null)
					if (servletPath != null)
						return servletPath;
					else
						return null;
				return contextPath + extractQueryAnchor(path, false);
			}
			
			@Override
			public String getRequestURI() {
				return (forward ? getRequestURI1() : super.getRequestURI());
			}
			
			@Override
			public String getContextPath() {
				return contextPath;
			}
			
			public String getQueryString1() {
				return (path == null ? null : extractQueryAnchor(path, true));
			}
			
			@Override
			public String getQueryString() {
				if (forward) {
					return getQueryString1();
				}
				
				String queryString = super.getQueryString();
				if (queryString == null || queryString.isEmpty()) {
					queryString = getQueryString1();
				} else {
					queryString += "&" + getQueryString1();
				}
				
				return queryString;
			}
			
			@Override
			public Enumeration<String> getAttributeNames() {
				List<String> attributes = new ArrayList<String>(10);
				if (named == null) {
					if (forward) {
						if (dispType.equals(DispatcherType.ASYNC)) {
							attributes.add(AsyncContext.ASYNC_CONTEXT_PATH);
							attributes.add(AsyncContext.ASYNC_PATH_INFO);
							attributes.add(AsyncContext.ASYNC_QUERY_STRING);
							attributes.add(AsyncContext.ASYNC_REQUEST_URI);
							attributes.add(AsyncContext.ASYNC_SERVLET_PATH);
						} else {
							attributes.add("javax.servlet.forward.request_uri");
							attributes.add("javax.servlet.forward.context_path");
							attributes.add("javax.servlet.forward.servlet_path");
							attributes.add("javax.servlet.forward.path_info");
							attributes.add("javax.servlet.forward.query_string");
						}
					} else {
						attributes.add("javax.servlet.include.request_uri");
						attributes.add("javax.servlet.include.path_info");
						attributes.add("javax.servlet.include.context_path");
						attributes.add("javax.servlet.include.servlet_path");
						attributes.add("javax.servlet.include.query_string");
					}
				}
				
				Enumeration<String> e = super.getAttributeNames();
				while (e.hasMoreElements()) {
					attributes.add((String) e.nextElement());
				}
				
				return Collections.enumeration(attributes);
			}
			
			@Override
			public Object getAttribute(String name) {
				if (named == null) {
					// System.err.printf("-->requested attr:%s%n", name);
					if (forward) {
						if (dispType.equals(DispatcherType.ASYNC)) {
							if (AsyncContext.ASYNC_REQUEST_URI.equals(name))
								return super.getRequestURI();
							else if (AsyncContext.ASYNC_CONTEXT_PATH.equals(name))
								return super.getContextPath();
							else if (AsyncContext.ASYNC_SERVLET_PATH.equals(name))
								return super.getServletPath();
							else if (AsyncContext.ASYNC_PATH_INFO.equals(name))
								return super.getPathInfo();
							else if (AsyncContext.ASYNC_QUERY_STRING.equals(name))
								return super.getQueryString();
						} else {
							if ("javax.servlet.forward.request_uri".equals(name))
								return super.getRequestURI();
							else if ("javax.servlet.forward.context_path".equals(name))
								return super.getContextPath();
							else if ("javax.servlet.forward.servlet_path".equals(name))
								return super.getServletPath();
							else if ("javax.servlet.forward.path_info".equals(name))
								return super.getPathInfo();
							else if ("javax.servlet.forward.query_string".equals(name))
								return super.getQueryString();
							else if ("javax.servlet.include.servlet_path".equals(name) || "javax.servlet.include.request_uri".equals(name))
								return null;
						}
					} else {
						if ("javax.servlet.include.request_uri".equals(name))
							return getRequestURI1();
						else if ("javax.servlet.include.path_info".equals(name))
							return getPathInfo1();
						else if ("javax.servlet.include.context_path".equals(name))
							return getContextPath();
						else if ("javax.servlet.include.query_string".equals(name))
							return getQueryString1();
						else if ("javax.servlet.include.servlet_path".equals(name))
							return getServletPath1();
						else if ("javax.servlet.forward.request_uri".equals(name) || "javax.servlet.forward.servlet_path".equals(name))
							return null;
					}
				}
				// System.err.printf("!!!return attr:%s=%s%n", name,
				// super.getAttribute(name));
				return super.getAttribute(name);
			}
			
			@Override
			public void removeAttribute(String name) {
				if (_DEBUG && name.startsWith("javax.servlet.")) {
					System.err.printf("An attempt to remove systen  ATTR: %s in mode %s = %s%n", name, forward ? "FORWARD" : "INCLUDE", getAttribute(name));
				}
				super.removeAttribute(name);
			}
			
			// @Override
			// public void setAttribute(String name, Object value) {
			// System.err.printf("!!!Set attr %s=%s%n", name, value);
			// super.setAttribute(name, value);
			// }
			
			@Override
			public RequestDispatcher getRequestDispatcher(String path) {
				if (_DEBUG) {
					System.err.printf("Request %s processing from %s%n", path, forward ? "FORWARD" : "INCLUDE");
				}
				
				if (path.charAt(0) != '/') {
					String sp = getServletPath();
					String pi = getPathInfo();
					if (pi == null) {
						int lsp = sp.lastIndexOf('/');
						if (lsp >= 0) {
							path = sp.substring(0, lsp) + '/' + path;
						} else {
							path = '/' + path;
						}
					} else {
						int lsp = pi.lastIndexOf('/');
						if (lsp >= 0) {
							path = sp + pi.substring(0, lsp) + '/' + path;
						} else {
							path = sp + '/' + path;
						}
					}
					
					// System.err.printf("DEBUG: sp: %sp, pi: %s, p: %s%n", sp,
					// pi, path);
				}
				return WebAppServlet.this.getRequestDispatcher(path);
			}
			
			/**
			 * 
			 * @param name
			 * @return
			 * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
			 */
			@Override
			public String getParameter(String name) {
				Map<String, String[]> params = createParameters();
				String[] result = params.get(name);
				if (result != null) {
					return result[0];
				}
				
				return super.getParameter(name);
			}
			
			@Override
			public Map<String, String[]> getParameterMap() {
				HashMap<String, String[]> result = new HashMap<String, String[]>();
				result.putAll(super.getParameterMap());
				result.putAll(createParameters());
				return result;
			}
			
			@Override
			public Enumeration getParameterNames() {
				Map params = getParameterMap();
				Hashtable result = new Hashtable();
				result.putAll(params);
				return result.keys();
			}
			
			@Override
			public String[] getParameterValues(String name) {
				Map<String, String[]> params = createParameters();
				String[] result = params.get(name);
				if (result != null)
					return result;
				return super.getParameterValues(name);
			}
			
			// //////////////// servlet spec 3.0 ///////////////////
			
			/**
			 * Use the container login mechanism configured for the
			 * ServletContext to authenticate the user making this request.
			 * 
			 */
			public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
				return super.authenticate(response);
			}
			
			/**
			 * Validate the provided username and password in the password
			 * validation realm used by the web container login mechanism
			 * configured for the ServletContext.
			 * 
			 */
			public void login(String username, String password) throws ServletException {
				super.login(username, password);
			}
			
			/**
			 * Establish null as the value returned when getUserPrincipal,
			 * getRemoteUser, and getAuthType is called on the request.
			 * 
			 */
			public void logout() throws ServletException {
				super.logout();
			}
			
			/**
			 * Gets all the Part components of this request, provided that it is
			 * of type multipart/form-data.
			 */
			public Collection<Part> getParts() throws java.io.IOException, ServletException {
				return super.getParts();
			}
			
			/**
			 * Gets the Part with the given name.
			 * 
			 */
			public Part getPart(String name) throws java.io.IOException, ServletException {
				return super.getPart(name);
			}
			
			/**
			 * Gets the dispatcher type of this request.
			 * 
			 */
			public DispatcherType getDispatcherType() {
				return forward ? DispatcherType.FORWARD : DispatcherType.INCLUDE;
			}
			
			/**
			 * Gets the AsyncContext that was created or reinitialized by the
			 * most recent invocation of startAsync() or
			 * startAsync(ServletRequest,ServletResponse) on this request.
			 * 
			 * @return the AsyncContext that was created or reinitialized by the
			 *         most recent invocation of startAsync() or
			 *         startAsync(ServletRequest,ServletResponse) on this
			 *         request
			 * @throws IllegalStateException
			 *             - if this request has not been put into asynchronous
			 *             mode, i.e., if neither startAsync() nor
			 *             startAsync(ServletRequest,ServletResponse) has been
			 *             called
			 */
			public AsyncContext getAsyncContext() {
				return super.getAsyncContext();
			}
			
			/**
			 * Puts this request into asynchronous mode, and initializes its
			 * AsyncContext with the original (unwrapped) ServletRequest and
			 * ServletResponse objects.
			 * 
			 * @return
			 * @throws IllegalStateException
			 */
			public AsyncContext startAsync() throws IllegalStateException {
				return super.startAsync();
			}
			
			/**
			 * Puts this request into asynchronous mode, and initializes its
			 * AsyncContext with the given request and response objects. The
			 * ServletRequest and ServletResponse arguments must be the same
			 * instances, or instances of ServletRequestWrapper and
			 * ServletResponseWrapper that wrap them, that were passed to the
			 * service method of the Servlet or the doFilter method of the
			 * Filter, respectively, in whose scope this method is being called.
			 * 
			 * @param servletRequest
			 * @param servletResponse
			 * @return
			 * @throws IllegalStateException
			 */
			public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
				return super.startAsync(servletRequest, servletResponse);
			}
			
			/**
			 * Checks if this request has been put into asynchronous mode.
			 * 
			 * @return
			 */
			public boolean isAsyncStarted() {
				return super.isAsyncStarted();
			}
			
			protected Map<String, String[]> createParameters() {
				String query = getQueryString();
				if (query != null) {
					return Utils.parseQueryString(query, null);
				}
				
				return new Hashtable<String, String[]>();
			}
			
			@Override
			public String toString() {
				return String.format("Dispatching request attached to %s", SimpleDispatcher.this);
			}
		}
	}
	
	protected static class SessionCookieConfigImpl implements SessionCookieConfig {
		String comment;
		WebAppServlet was;
		
		SessionCookieConfigImpl(WebAppServlet s) {
			was = s;
		}
		
		@Override
		public String getComment() {
			return comment;
		}
		
		@Override
		public String getDomain() {
			return null;
		}
		
		@Override
		public int getMaxAge() {
			if (was.server.expiredIn < 0)
				return -was.server.expiredIn;
			return 0;
		}
		
		@Override
		public String getName() {
			return Serve.ServeConnection.SESSION_COOKIE_NAME;
		}
		
		@Override
		public String getPath() {
			return was.contextPath;
		}
		
		@Override
		public boolean isHttpOnly() {
			return was.server.httpSessCookie;
		}
		
		@Override
		public boolean isSecure() {
			return was.server.secureSessCookie;
		}
		
		@Override
		public void setComment(String arg0) {
			throw new IllegalStateException();
		}
		
		@Override
		public void setDomain(String arg0) {
			throw new IllegalStateException();
		}
		
		@Override
		public void setHttpOnly(boolean arg0) {
			throw new IllegalStateException();
		}
		
		@Override
		public void setMaxAge(int arg0) {
			throw new IllegalStateException();
		}
		
		@Override
		public void setName(String arg0) {
			throw new IllegalStateException();
		}
		
		@Override
		public void setPath(String arg0) {
			throw new IllegalStateException();
		}
		
		@Override
		public void setSecure(boolean arg0) {
			throw new IllegalStateException();
		}
	}
	
	// ////////////// Filter methods /////////////////////
	protected class WebAppContextFilter implements Filter {
		String servPathHolder;
		
		WebAppContextFilter(String servletPath) {
			// TODO move to init()
			if (servletPath != null)
				servPathHolder = servletPath;
			else
				throw new NullPointerException("Servlet path is null");
		}
		
		WebAppContextFilter() {
			this("/");
		}
		
		public void init(FilterConfig filterConfig) throws ServletException {
			// no init for the filter
		}
		
		/**
		 * this is mandatory filter converting TJWS base servlet requests to web
		 * application context relative request
		 */
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws java.io.IOException, ServletException {
			final HttpServletRequest hreq = (HttpServletRequest) request;
			final HttpServletResponse hres = (HttpServletResponse) response;
			final HttpServletResponse[] proxiedRespHolder = new HttpServletResponse[1];
			// TODO a research if request wrapper is more efficient
			chain.doFilter((HttpServletRequest) Proxy.newProxyInstance(javax.servlet.http.HttpServletRequest.class.getClassLoader(), new Class[] { javax.servlet.http.HttpServletRequest.class, Openable.class }, new InvocationHandler() {
				AsyncContextImpl asyncCtx;
				
				boolean asyncEnabled;
				
				Multipart multiparts;
				
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					String mn = method.getName();
					if (mn.equals("getServletPath")) {
						if (_DEBUG)
							System.err.println("getServletPath() " + extractPath(hreq.getRequestURI(), contextPath, servPathHolder, false));
						return extractPath(hreq.getRequestURI(), contextPath, servPathHolder, false);
					} else if (mn.equals("getPathInfo")) {
						if (_DEBUG)
							System.err.println("getPathInfo() " + extractPath(hreq.getRequestURI(), contextPath, servPathHolder, true));
						return extractPath(hreq.getRequestURI(), contextPath, servPathHolder, true);
					} else if (mn.equals("getRealPath")) {
						if (_DEBUG)
							System.err.println("Path:" + args[0]);
						return getRealPath((String) args[0]);
					} else if (mn.equals("getPathTranslated")) {
						return getRealPath(hreq.getPathInfo());
					} else if (mn.equals("getRequestDispatcher")) {
						String url = (String) args[0];
						if (url.charAt(0) != '/') {
							String bp = extractPath(hreq.getRequestURI(), contextPath, servPathHolder, false);
							int lsp = bp.lastIndexOf('/');
							if (lsp > 0)
								bp = bp.substring(0, lsp);
							url = bp + '/' + url;
							if (_DEBUG)
								System.err.printf("req.getDispatcher(%s), %s %n", url, bp);
						}
						return getRequestDispatcher(url);
					} else if (mn.equals("getContextPath")) {
						return contextPath;
					} else if (mn.equals("getSession")) {
						HttpSession session = (HttpSession) method.invoke(hreq, args);
						// System.err.println("getsession:"+session);
						// TODO some overhead is here, context
						// and listeners will be overloaded each
						// time
						// time of accessing session while it's
						// new
						if (session instanceof Serve.AcmeSession && (session.getServletContext() == null || session.isNew())) {
							// System.err.println("set listeners & context");
							((Serve.AcmeSession) session).setListeners(WebAppServlet.this.sessionListeners);
							((Serve.AcmeSession) session).setServletContext(WebAppServlet.this);
							if (sessionTimeout > 0)
								session.setMaxInactiveInterval(sessionTimeout);
						}
						return session;
					} else if (mn.equals("startAsync")) {
						// Async
						// System.err.printf("STARTTTTTTTT %b %s %n",
						// asyncEnabled, asyncCtx);
						if (asyncEnabled == false)
							throw new IllegalStateException("Async mode not enabled in app descriptor");
						if (asyncCtx == null) {
							if (args == null || args.length == 0)
								asyncCtx = new AsyncContextImpl((ServletRequest) proxy, proxiedRespHolder[0], (Serve.ServeConnection) hreq);
							else
								asyncCtx = new AsyncContextImpl((ServletRequest) args[0], (ServletResponse) args[1], (Serve.ServeConnection) hreq);
						}
						asyncCtx.notifyStart();
						return asyncCtx;
					} else if (mn.equals("getServletContext")) {
						return WebAppServlet.this;
					} else if (mn.equals("isAsyncSupported")) {
						return asyncEnabled;
					} else if (mn.equals("isAsyncStarted")) {
						return asyncCtx != null;
					} else if (mn.equals("getAsyncContext")) {
						if (asyncCtx != null)
							return asyncCtx;
						throw new IllegalStateException("Request has not been set in async mode");
					} else if (mn.equals("getDispatcherType")) {
						return DispatcherType.REQUEST;
					} else if (mn.equals("getParts")) { // ////////////////////
														// Multi
														// part
														// ///////////////////////////
						if (multiparts != null)
							return multiparts.getParts();
						throw new ServletException(MULTIPART_ERR_MSQ);
					} else if (mn.equals("getPart")) {
						if (multiparts != null)
							return multiparts.getPart((String) args[0]);
						throw new ServletException(MULTIPART_ERR_MSQ);
					} else if (attributeListeners != null) {
						if (mn.equals("setAttribute")) {
							Object av = hreq.getAttribute((String) args[0]);
							hreq.setAttribute((String) args[0], args[1]);
							if (av == null) {
								ServletRequestAttributeEvent e = new ServletRequestAttributeEvent(WebAppServlet.this, hreq, (String) args[0], args[1]);
								for (ServletRequestAttributeListener sarl : attributeListeners)
									sarl.attributeAdded(e);
							} else {
								ServletRequestAttributeEvent e = new ServletRequestAttributeEvent(WebAppServlet.this, hreq, (String) args[0], av);
								for (ServletRequestAttributeListener sarl : attributeListeners)
									sarl.attributeReplaced(e);
							}
							return null;
						} else if (mn.equals("removeAttribute")) {
							Object av = hreq.getAttribute((String) args[0]);
							hreq.removeAttribute((String) args[0]);
							ServletRequestAttributeEvent e = new ServletRequestAttributeEvent(WebAppServlet.this, hreq, (String) args[0], av);
							for (ServletRequestAttributeListener sarl : attributeListeners)
								sarl.attributeRemoved(e);
							return null;
						} else if (mn.equals("getOrigin")) {
							Object origin = hreq;
							while (origin instanceof Openable)
								origin = ((Openable) origin).getOrigin();
							return origin;
						}
					}
					try {
						return method.invoke(hreq, args);
					} catch (InvocationTargetException ite) {
						throw ite.getTargetException();
					}
				}
			}), // response);
							proxiedRespHolder[0] = (HttpServletResponse) Proxy.newProxyInstance(javax.servlet.http.HttpServletResponse.class.getClassLoader(), new Class[] { javax.servlet.http.HttpServletResponse.class, Openable.class }, new InvocationHandler() {
								public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
									String mn = method.getName();
									if (mn.equals("sendError")) {
										if (errorPages != null)
											for (ErrorPageDescr epd : errorPages)
												if (epd.errorCode == ((Integer) args[0]).intValue()) {
													setErrorAttributes(hreq, (Integer) args[0], args.length > 1 ? (String) args[1] : "", getServletName(), hreq.getRequestURI(), null, null);
													// System.err.printf("ERROR
													// Forwarding to %s for
													// %d%n",epd.errorPage,
													// args[0]);
													getRequestDispatcher(epd.errorPage).forward(hreq, hres);
													return null;
												}
									} else if (mn.equals("getOrigin")) {
										Object origin = hres;
										while (origin instanceof Openable)
											origin = ((Openable) origin).getOrigin();
										return origin;
										
									} // else if
										// (mn.equals("sendRedirect")) {
										// System.err.printf("Redirect
										// to:%s%n",args[0]);
										// }
									return method.invoke(hres, args);
								}
							}));
		}
		
		public void destroy() {
			// destroy context filter
		}
	}
	
	/**
	 * This function extract meaningful path or query
	 * 
	 * @param path
	 *            path to extract from
	 * @param query
	 *            true if extract query
	 * @return extraction or null
	 */
	public static String extractQueryAnchor(String path, boolean query) {
		int qp = path.indexOf('?');
		if (query) {
			if (qp >= 0 && path.length() > qp + 1)
				return path.substring(qp + 1);
			return null;
		}
		int hp = -1;// path.indexOf('#');
		if (qp >= 0) {
			if (hp >= 0 && hp < qp)
				return path.substring(0, hp);
			return path.substring(0, qp);
		} else if (hp >= 0)
			return path.substring(0, hp);
		return path;
	}
	
	/**
	 * This function extract certain path from request URI, URI is considered
	 * and not decoded
	 * 
	 * @param uri
	 *            - URI
	 * @param context
	 *            - context name
	 * @param servlet
	 *            - servlet name
	 * @param info
	 * @return
	 */
	static public String extractPath(String uri, String context, String servlet, boolean info) throws UnsupportedEncodingException {
		uri = Utils.decode(uri, IOHelper.UTF_8);
		if (_DEBUG) {
			System.err.printf("Extract path URI: %s, context: %s, servlet: %s, action: %b\n", uri, context, servlet, info);
		}
		
		int cl = context.length();
		int sl = servlet.length();
		int sp = uri.indexOf(servlet, cl);
		if (_DEBUG) {
			System.err.printf("servlet pos: %d%n", sp);
		}
		
		if (sp < 0) {
			// if ("/".equals(servlet))
			// sp = cl-1;
			// else
			sp = cl;
		}
		int pp = uri.indexOf('?', sp); // + sl
		// query is already separated and can be only part of forward
		pp = -1;
		// uri.indexOf('#', sp); // + sl
		int ph = -1;
		if (ph >= 0 && ((pp >= 0 && ph < pp) || pp < 0)) {
			pp = ph;
		}
		
		int ip = uri.indexOf('/', sp + sl - (servlet.endsWith("/") ? 1 : 0));
		if (_DEBUG) {
			System.err.printf("servlet pos %d, info pos: %d, param pos: %d %n", sp, ip, pp);
		}
		
		if (info == false) {
			if (servlet.equals("/") || ip < 0) {
				if (pp > 0) {
					return uri.substring(sp, pp);
				} else {
					return uri.substring(sp);
				}
			}
			
			if (pp < 0) {
				return uri.substring(sp, ip);
			}
			
			return uri.substring(sp, pp);
		}
		
		if (servlet.equals("/") || ip < 0 || (pp > 0 && ip > pp)) {
			return null;
		}
		
		if (pp < 0) {
			return uri.substring(ip);
		}
		
		return uri.substring(ip, pp);
	}
	
	protected class SimpleFilterChain implements FilterChain {
		List<FilterAccessDescriptor> filters;
		
		Iterator<FilterAccessDescriptor> iterator;
		
		Servlet servlet;
		
		Filter filter;
		
		// FilterAccessDescriptor
		
		Filter nextFilter;
		
		ServletAccessDescr sad;
		
		SimpleFilterChain() {
			filters = new ArrayList<FilterAccessDescriptor>();
		}
		
		public void setFilter(Filter filter) {
			this.filter = filter;
		}
		
		public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
			// TODO decide if wrap in a object returning isAsyncSupported based
			// on asyncSupported
			// for servlet or filter without descriptor asyncSupported = false
			if (nextFilter != null) {
				nextFilter = null;
				filter.doFilter(request, response, this);
			} else if (iterator.hasNext()) {
				FilterAccessDescriptor fad = iterator.next();
				try {
					fad.filterInstance.doFilter(applyAsyncSet(request, fad.asyncSupported), response, this);
				} catch (UnavailableException ue) {
					if (ue.isPermanent()) {
						synchronized (fad) {
							if (fad.filterInstance != null) {
								fad.filterInstance.destroy();
								fad.filterInstance = null;
							}
						}
					} else {
						fad.timeToReactivate = System.currentTimeMillis() + ue.getUnavailableSeconds() * 1000l;
					}
					doFilter(request, response);
					// iterator.remove();
				}
			} else
				// TODO figure out error handler needed for filters, it should
				// also handle UnavailableException
				// call sevlet
				try {
					// new Exception("==RUN AS SERVLET==").printStackTrace();
					servlet.service(applyAsyncSet(applyMultipart(request), sad == null ? false : sad.asyncSupported), response);
				} catch (IOException ioe) {
					if (handleError(ioe, request, response) == false)
						throw ioe;
				} catch (UnavailableException ue) {
					// log("Servlet " + servlet + " asked to be unavailable",
					// ue);
					if (sad != null) {
						if (ue.isPermanent()) {
							synchronized (sad) {
								if (sad.instance != null) {
									sad.instance.destroy();
									sad.instance = null;
								}
							}
						} else {
							sad.timeToReactivate = System.currentTimeMillis() + ue.getUnavailableSeconds() * 1000l;
						}
					}
					((HttpServletResponse) response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, ue.getMessage());
					// allowing custom handling?
					// eating an exception to avoid removing entire webapp
					// servlet throw ue;
				} catch (ServletException se) {
					if (handleError(se, request, response) == false)
						throw se;
				} catch (Throwable re) {
					if (re instanceof ThreadDeath)
						throw (ThreadDeath) re;
					if (handleError(re, request, response) == false)
						throw new RuntimeException(re);
				}
		}
		
		protected ServletRequest applyAsyncSet(ServletRequest request, boolean asyncEnabled) {
			// TODO decide if add wrapper
			if (asyncEnabled) {
				if (Proxy.isProxyClass(request.getClass())) {
					InvocationHandler handler = Proxy.getInvocationHandler(request);
					try {
						handler.getClass().getDeclaredField("asyncEnabled").set(handler, Boolean.TRUE);
					} catch (Exception e) {
						log("", e);
					}
				}
			}
			return request;
		}
		
		protected ServletRequest applyMultipart(ServletRequest request) throws IOException {
			if (sad == null || sad.multipartEnabled == false)
				return request;
			
			String contentType = request.getContentType();
			if (contentType != null) {
				contentType = contentType.toLowerCase();
				int pi = contentType.indexOf("multipart/form-data");
				if (pi < 0)
					return request;
				if (Proxy.isProxyClass(request.getClass()) == false) {
					log(String.format("Request object isn't a proxy class (%s), multipart data can't be added", request.getClass()), null);
					return request;
				}
				
				pi = contentType.indexOf("boundary=", pi + "multipart/form-data".length());
				if (pi <= 0) // invalid multipart request no boundary
					throw new IOException("Boundary attribute is missed in " + contentType);
				int ei = contentType.indexOf(';', pi + "boundary=".length());
				contentType = request.getContentType(); // since to lower case
														// was applied
				String boundary = ei < 0 ? contentType.substring(pi + "boundary=".length()) : contentType.substring(pi + "boundary=".length(), ei);
				InvocationHandler handler = Proxy.getInvocationHandler(request);
				try {
					handler.getClass().getDeclaredField("multiparts").set(handler, new Multipart(request, boundary, sad));
				} catch (Exception e) {
					log("", e);
				}
			}
			return request;
		}
		
		protected boolean handleError(Throwable t, ServletRequest request, ServletResponse response) throws java.io.IOException, ServletException {
			if (errorPages != null) {
				Class<?> eclass = t.getClass();
				for (ErrorPageDescr epd : errorPages) {
					if (epd.exception != null && eclass.equals(epd.exception)) {
						log("forward to " + epd.errorPage, t);
						((HttpServletResponse) response).sendRedirect(epd.errorPage);
						setErrorAttributes(request, -1, t.getMessage(), getServletName(), ((HttpServletRequest) request).getRequestURI(), t, t.getClass());
						getRequestDispatcher(epd.errorPage).forward(request, response);
						return true;
					}
				}
				Class<?>[] peclasses = eclass.getClasses();
				for (Class<?> peclass : peclasses)
					for (ErrorPageDescr epd : errorPages) {
						if (epd.exception != null && peclass.equals(epd.exception)) {
							log("forward to " + epd.errorPage, t);
							((HttpServletResponse) response).sendRedirect(epd.errorPage);
							setErrorAttributes(request, -1, t.getMessage(), getServletName(), ((HttpServletRequest) request).getRequestURI(), t, t.getClass());
							getRequestDispatcher(epd.errorPage).forward(request, response);
							return true;
						}
					}
				
			}
			return false;
		}
		
		protected void reset() {
			iterator = filters.iterator();
			nextFilter = filter;
		}
		
		protected void add(FilterAccessDescriptor fad) {
			if (fad.timeToReactivate > 0 && fad.timeToReactivate > System.currentTimeMillis())
				return;
			if (filters.contains(fad) == false)
				filters.add(fad);
		}
		
		protected void setServlet(Servlet servlet) {
			this.servlet = servlet;
			sad = null;
		}
		
		protected void setServlet(ServletAccessDescr sad) {
			this.servlet = sad.instance;
			this.sad = sad;
		}
	}
	
	private final static boolean _DEBUG = false;
	private final static boolean __DEBUG = "yes".equals(System.getProperty(DEF_DEBUG)) || _DEBUG;
}