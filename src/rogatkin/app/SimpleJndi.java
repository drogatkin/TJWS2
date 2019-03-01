/*
 * tjws - SimpleJndi.java
 * Copyright (C) 1999-2010 Dmitriy Rogatkin. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * Visit http://tjws.sourceforge.net to get the latest information
 * about Rogatkin's products.
 * $Id: SimpleJndi.java,v 1.21 2013/03/12 07:58:20 cvs Exp $
 * Created on Mar 25, 2007
 * @author Dmitriy
 */
package rogatkin.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.spi.InitialContextFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;

import Acme.Serve.Serve;
import rogatkin.app.remote.bind;
import rogatkin.app.remote.bind_listHolder;
import rogatkin.app.remote.naming_exception;
import rogatkin.app.remote.simple_naming;
import rogatkin.app.remote.simple_namingHelper;
import rogatkin.app.remote.simple_namingPOA;

/**
 * Simple JNDI naming service
 * 
 * more details:
 * http://download.java.net/jdk8/docs/technotes/guides/jndi/jndi-cos.html#OTHERPROP
 * 
 * @author Dmitriy
 *
 */

public class SimpleJndi implements InitialContextFactory {
	public static final char NAME_SEP_CHAR = '/';
	public static final String NAME_SEP = "" + NAME_SEP_CHAR; // can be "\\."
	public static final String NAME_SEP_REGEXP = NAME_SEP;
	
	static ORB orb;
	
	static org.omg.PortableServer.POA rootPoa;
	
	protected static SimpleContext mainContext;
	
	protected static simple_naming remoteContext;
	
	static volatile boolean intitalized;
	
	public Context getInitialContext(Hashtable<?, ?> arg0) throws NamingException {
		return initMainContext(new SimpleContext(arg0));
	}
	
	private SimpleContext initMainContext(SimpleContext context) {
		if (intitalized == false)
			synchronized (SimpleJndi.class) {
				if (intitalized == false) {
					try {
						URL provUrl = new URL(System.getProperty(Context.PROVIDER_URL) == null ? (String) context.getEnvironment().get(Context.PROVIDER_URL) : System.getProperty(Context.PROVIDER_URL));
						if (true || "tjws".equals(provUrl.getProtocol())) {
							// try to connect
							try {
								if (__debug)
									System.err.printf("Url %s to connect%n", new URL("http", provUrl.getHost(), provUrl.getPort(), "getRootContext"));
								String ior = readUrltoString(new URL("http", provUrl.getHost(), provUrl.getPort(), "getRootContext"));
								if (__debug)
									System.err.printf("==========>>>>Read IOR: %s%n", ior);
								if (ior == null)
									throw new IOException("IOR is null");
								remoteContext = simple_namingHelper.narrow(orb.string_to_object(ior));
							} catch (IOException e) {
								if (__debug)
									e.printStackTrace();
								mainContext = exposeContext(context, provUrl.getHost(), provUrl.getPort());
							}
						}
					} catch (NamingException e) {
						System.err.printf("JNDI url %s%n", e);
					} catch (MalformedURLException e) {
						System.err.printf("JNDI url %s%n", e);
						if (__debug)
							e.printStackTrace();
					} finally {
						intitalized = true;
					}
				}
			}
		return context;
	}
	
	/**
	 * 
	 * @author Rohtash Singh Lakra
	 * @date 03/19/2018 05:33:35 PM
	 */
	static class RootContextServlet extends HttpServlet {
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
			if (mainContext != null) {
				try {
					resp.setContentType("text/plain");
					resp.getWriter().write(orb.object_to_string(rootPoa.servant_to_reference(mainContext)));
				} catch (org.omg.CORBA.UserException ce) {
					ce.printStackTrace();
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			} else
				resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
	}
	
	/**
	 * 
	 * @author Rohtash Singh Lakra
	 * @date 03/19/2018 05:37:46 PM
	 */
	static class ShutdownHook implements Runnable {
		final Serve serve;
		
		public ShutdownHook(final Serve serve) {
			this.serve = serve;
		}
		
		/**
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			if (serve != null) {
				serve.notifyStop();
				serve.destroyAllServlets();
			}
		}
	}
	
	private SimpleContext exposeContext(SimpleContext context, String host, int port) {
		final Serve srv = new Acme.Serve.Serve();
		Properties properties = new java.util.Properties();
		properties.put(Serve.ARG_PORT, port);
		properties.put(Serve.ARG_NOHUP, Serve.ARG_NOHUP);
		if (host != null && host.length() > 0 && "localhost".equals(host) == false) {
			properties.put(Serve.ARG_BINDADDRESS, host);
		}
		srv.arguments = properties;
		srv.addServlet("/getRootContext", new RootContextServlet());
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(srv)));
		Thread exposerThread = new Thread("ContextExposer") {
			public void run() {
				srv.serve();
			}
		};
		exposerThread.setDaemon(true);
		exposerThread.setPriority(Thread.MIN_PRIORITY);
		exposerThread.start();
		return context;
	}
	
	/**
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private byte[] readUrl(URL url) throws IOException {
		URLConnection uc = url.openConnection();
		uc.setRequestProperty("connection", "close");
		uc.connect();
		if (((HttpURLConnection) uc).getResponseCode() == HttpURLConnection.HTTP_OK) {
			int cl = uc.getContentLength();
			if (cl > 0) {
				byte[] bytes = new byte[cl];
				int pos = 0;
				int l = bytes.length;
				do {
					int k = uc.getInputStream().read(bytes, pos, l);
					if (k <= 0)
						break;
					pos += k;
					l -= k;
				} while (l > 0);
				return bytes;
			} else {
				ByteArrayOutputStream bas = new ByteArrayOutputStream(512);
				byte[] bytes = new byte[256];
				InputStream is = uc.getInputStream();
				int l;
				while ((l = is.read(bytes)) > 0)
					bas.write(bytes, 0, l);
				return bas.toByteArray();
			}
		} else
			System.err.printf("Respose code %d%n", ((HttpURLConnection) uc).getResponseCode());
		return null;
	}
	
	private String readUrltoString(URL url) throws IOException {
		byte[] bytes = readUrl(url);
		if (bytes != null)
			return new String(bytes);
		return null;
	}
	
	public static void main(String... args) {
		try {
			System.getProperties().setProperty(Context.INITIAL_CONTEXT_FACTORY, SimpleJndi.class.getName());
			new SimpleDataSource(args[0], null);
			
			Connection con = ((DataSource) new InitialContext().lookup("jdbc/access/MediaChest")).getConnection();
			System.out.printf("Connection taken %s\n", con);
			con.close();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
	
	static class SimpleContext extends simple_namingPOA implements Context {
		private static Hashtable<String, Object> environment = new Hashtable<String, Object>();
		
		private static HashMap<String, Object> directory = new HashMap<String, Object>();
		
		SimpleContext(Hashtable<?, ?> env) {
			if (rootPoa == null)
				synchronized (SimpleContext.class) {
					if (rootPoa == null) {
						try {
							String orbArgs = System.getProperty("tjws.app.orb.arguments");
							// TODO: add properties
							if (orbArgs == null)
								orb = ORB.init(new String[] {}, new Properties());
							else {
								orb = ORB.init(orbArgs.split(","), new Properties());
							}
							rootPoa = org.omg.PortableServer.POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
							rootPoa.the_POAManager().activate();
						} catch (SystemException se) {
							se.printStackTrace();
						} catch (org.omg.CORBA.UserException ue) {
							ue.printStackTrace();
						}
					}
				}
			for (Object key : env.keySet()) {
				environment.put(key.toString(), env.get(key));
			}
		}
		
		public Object addToEnvironment(String arg0, Object arg1) throws NamingException {
			return environment.put(arg0, arg1);
		}
		
		public void bind(Name arg0, Object arg1) throws NamingException {
			bind(arg0.toString(), arg1);
		}
		
		public void bind(String arg0, Object arg1) throws NamingException {
			if (__debug)
				System.err.printf("===== Binding: %s at %s%n", arg0, arg1); ///////////////////////
				
			arg0 = arg0.trim();
			try {
				if (lookup(arg0) != null)
					throw new NameAlreadyBoundException();
			} catch (NameNotFoundException ne) {
				if (__debug)
					System.err.printf("Remote ctx %s%n", remoteContext);
				if (remoteContext != null)
					if (arg1 instanceof org.omg.CORBA.Object)
						try {// System.err.printf("-->Bind%s%n", arg1);
							remoteContext.bind1(arg0, (org.omg.CORBA.Object) arg1);
						} catch (naming_exception ne1) {
							throw new javax.naming.NameNotFoundException("Can't bind in remote repository");
						}
					else if (arg1 instanceof org.omg.PortableServer.Servant)
						try {// System.err.printf("!-->Bind%s%n", arg1);
							remoteContext.bind1(arg0, rootPoa.servant_to_reference((org.omg.PortableServer.Servant) arg1));
						} catch (naming_exception ne1) {
							throw new javax.naming.NameNotFoundException("Can't bind in remote repository");
						} catch (org.omg.CORBA.UserException ce) {
							ce.printStackTrace();
							throw new NamingException();
						}
					else
						synchronized (directory) {// System.err.printf("++-->Bind%s%n",
													// arg1);
							directory.put(arg0, arg1);
						}
				else
					synchronized (directory) {// System.err.printf("~~-->Bind%s%n",
												// arg1);
						directory.put(arg0, arg1);
					}
			}
		}
		
		public void close() throws NamingException {
			
			// TODO disconnect all remote objects
			if (remoteContext != null) {
				// deactivate
				orb.disconnect(remoteContext);
			}
		}
		
		public Name composeName(Name arg0, Name arg1) throws NamingException {
			return new SimpleName(composeName(arg0.toString(), arg1.toString()));
		}
		
		public String composeName(String arg0, String arg1) throws NamingException {
			return arg0 + NAME_SEP + arg1;
		}
		
		public Context createSubcontext(Name arg0) throws NamingException {
			return createSubcontext(arg0.toString());
		}
		
		public Context createSubcontext(String arg0) throws NamingException {
			final String subcontextName = arg0.endsWith(NAME_SEP) ? arg0 : arg0 + NAME_SEP;
			return (Context) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Context.class }, new InvocationHandler() {
				public Object invoke(Object conn, Method methd, Object[] params) throws Throwable {
					for (int i = 0; i < params.length; i++)
						if (params[i] instanceof String)
							params[i] = subcontextName + (String) params[i];
						else if (params[i] instanceof Name)
							params[i] = new SimpleName(subcontextName + params[i].toString());
					try {
						return methd.invoke(SimpleContext.this, params);
					} catch (InvocationTargetException ite) {
						throw ite.getCause();
					}
				}
			});
		}
		
		public void destroySubcontext(Name arg0) throws NamingException {
			destroySubcontext(arg0.toString());
		}
		
		public void destroySubcontext(String arg0) throws NamingException {
			String subcontextName = arg0.endsWith(NAME_SEP) ? arg0 : arg0 + NAME_SEP;
			synchronized (directory) {
				for (String key : directory.keySet())
					if (key.startsWith(subcontextName))
						directory.remove(key);
			}
		}
		
		public Hashtable<?, ?> getEnvironment() throws NamingException {
			return environment;
		}
		
		public String getNameInNamespace() throws NamingException {
			throw new OperationNotSupportedException();
		}
		
		public NameParser getNameParser(Name arg0) throws NamingException {
			return new SimpleNameParser(arg0.toString());
		}
		
		public NameParser getNameParser(String arg0) throws NamingException {
			return new SimpleNameParser(arg0);
		}
		
		public NamingEnumeration<NameClassPair> list(Name arg0) throws NamingException {
			return list(arg0.toString());
		}
		
		public NamingEnumeration<NameClassPair> list(final String arg0) throws NamingException {
			return new SimpleNamingEnumeration<NameClassPair>(narrowLocal(arg0)) {
				public NameClassPair createEntry(String name, Object v) {
					return new NameClassPair(name, v == null ? null : v.getClass().getName());
				}
				
				public bind[] getRemoteDinds() {
					bind_listHolder h = new bind_listHolder();
					try {
						remoteContext.list1(arg0, h);
						return h.value;
					} catch (naming_exception ne) {
						
					} catch (COMM_FAILURE cfe) {
						// Can't connect to remote JNDI server:
					}
					return null;
				}
			};
			
		}
		
		public NamingEnumeration<Binding> listBindings(Name arg0) throws NamingException {
			return listBindings(arg0.toString());
		}
		
		public NamingEnumeration<Binding> listBindings(final String arg0) throws NamingException {
			return new SimpleNamingEnumeration<Binding>(narrowLocal(arg0)) {
				public Binding createEntry(String name, Object v) {
					return new Binding(name, v);
				}
				
				public bind[] getRemoteDinds() {
					if (remoteContext != null)
						try {
							bind_listHolder h = new bind_listHolder();
							remoteContext.list1(arg0, h);
							return h.value;
						} catch (naming_exception ne) {
							
						} catch (COMM_FAILURE cfe) {
							// Can't connect to remote JNDI server:
						}
					return null;
				}
			};
		}
		
		public Object lookup(Name arg0) throws NamingException {
			return lookup(arg0.toString());
		}
		
		public Object lookup(String arg0) throws NamingException {
			if (__debug)
				System.err.printf("Requested %s = %s%n", arg0, directory.get(arg0)); ///////////////
			if (arg0.length() == 0)
				return this;
			Object result = directory.get(arg0.trim());
			if (result == null && remoteContext != null) {
				try {
					if (__debug)
						System.err.printf("Looking remotely%n");
					result = remoteContext.lookup1(arg0);
				} catch (naming_exception ne) {
				} catch (COMM_FAILURE cfe) {
				}
			}
			if (result == null)
				throw new NameNotFoundException(arg0 + " not found");
			return result;
		}
		
		public Object lookupLink(Name arg0) throws NamingException {
			return lookupLink(arg0.toString());
		}
		
		public Object lookupLink(String arg0) throws NamingException {
			// links not supported, only local and remote objects
			return lookup(arg0);
		}
		
		public void rebind(Name arg0, Object arg1) throws NamingException {
			rebind(arg0.toString(), arg1);
		}
		
		public void rebind(String arg0, Object arg1) throws NamingException {
			unbind(arg0);
			bind(arg0, arg1);
		}
		
		public Object removeFromEnvironment(String arg0) throws NamingException {
			return environment.remove(arg0);
		}
		
		public void rename(Name arg0, Name arg1) throws NamingException {
			rename(arg0.toString(), arg1.toString());
		}
		
		public void rename(String arg0, String arg1) throws NamingException {
			Object o = lookup(arg0);
			unbind(arg0);
			bind(arg1, o);
		}
		
		public void unbind(Name arg0) throws NamingException {
			unbind(arg0.toString());
		}
		
		public void unbind(String arg0) throws NamingException {
			if (remoteContext != null)
				try {
					remoteContext.unbind1(arg0);
					return; // success
				} catch (naming_exception e) {
				}
			if (directory.remove(arg0) == null)
				throw new NamingException("Not found: " + arg0);
		}
		
		private Iterator<Map.Entry<String, Object>> narrowLocal(String prefix) {
			ArrayList<Map.Entry<String, Object>> result = new ArrayList<Map.Entry<String, Object>>(directory.size());
			synchronized (directory) {
				if (prefix.length() == 0) {
					for (Map.Entry<String, Object> e : directory.entrySet())
						result.add(e);
				} else {
					for (Map.Entry<String, Object> e : directory.entrySet())
						if (e.getKey().startsWith(prefix))
							result.add(e);
				}
			}
			return result.iterator();
		}
		
		///////////////////////// CORBA methods //////////////////////////////
		public org.omg.CORBA.Object lookup1(String name) throws naming_exception {
			name = name.trim();
			if (__debug)
				System.err.printf("Remote LOOKUP1 %s%n", name);
			// TODO name.length() == 0 -> return this
			Object result = directory.get(name.trim());
			if (result instanceof org.omg.CORBA.Object)
				return (org.omg.CORBA.Object) result;
			throw new naming_exception();
		}
		
		public void unbind1(String name) throws naming_exception {
			Object result = lookup1(name);
			if (result instanceof org.omg.CORBA.Object) {
				synchronized (directory) {
					directory.remove(name.trim());
				}
				try {
					rootPoa.deactivate_object(rootPoa.reference_to_id((org.omg.CORBA.Object) result));
				} catch (org.omg.CORBA.UserException ce) {
					
				}
			} else
				throw new naming_exception();
		}
		
		public void bind1(String name, org.omg.CORBA.Object o) throws naming_exception {
			if (__debug)
				System.err.printf("Remote bind %s%n", name);
			synchronized (directory) {
				if (directory.containsKey(name.trim()) == false)
					directory.put(name.trim(), o);
				else
					throw new naming_exception();
			}
		}
		
		public void list1(String filter, bind_listHolder binds) {
			if (__debug)
				System.err.printf("list1 called!!!!!!%n");
			Iterator<Map.Entry<String, Object>> i = narrowLocal(filter);
			ArrayList<bind> resBinds = new ArrayList<bind>(directory.size());
			while (i.hasNext()) {
				Map.Entry<String, Object> e = i.next();
				if (e.getValue() instanceof org.omg.CORBA.Object)
					resBinds.add(new bind(e.getKey(), (org.omg.CORBA.Object) e.getValue()));
			}
			binds.value = (bind[]) resBinds.toArray(new bind[resBinds.size()]);
		}
		//////////////////////////////////////////////////////////////////////////////////////////////////
	}
	
	protected static abstract class SimpleNamingEnumeration<T> implements NamingEnumeration<T> {
		Iterator<Map.Entry<String, Object>> i;
		
		// simple_naming r;
		
		bind[] remoteBinds;
		
		int bi;
		
		SimpleNamingEnumeration(Iterator<Map.Entry<String, Object>> ei) {
			i = ei;
		}
		
		abstract T createEntry(String name, Object v);
		
		abstract bind[] getRemoteDinds();
		
		public void close() {
			i = null;
		}
		
		public boolean hasMore() {
			boolean result = false;
			if (i != null) {
				result = i.hasNext();
				if (result == false) {
					remoteBinds = getRemoteDinds();
					bi = 0;
					i = null;
				} else
					return result;
			}
			if (remoteBinds != null)
				return remoteBinds.length > bi;
			return result;
		}
		
		public boolean hasMoreElements() {
			return hasMore();
		}
		
		public T next() {
			if (hasMore()) {
				if (i != null) {
					Map.Entry<String, Object> e = i.next();
					return createEntry(e.getKey(), e.getValue());
				}
				if (remoteBinds != null) {
					bind b = remoteBinds[bi];
					bi++;
					return createEntry(b.name, b.o);
				}
			}
			throw new NoSuchElementException();
		}
		
		public T nextElement() {
			return next();
		}
	}
	
	protected static class SimpleNameParser implements NameParser {
		String parent;
		
		public SimpleNameParser(String name) {
			parent = name;
		}
		
		public Name parse(String arg0) throws NamingException {
			return new SimpleName(parent + NAME_SEP + arg0);
		}
	}
	
	static class SimpleName implements Name {
		private String name;
		
		public SimpleName(String n) {
			name = n;
		}
		
		public String toString() {
			return name;
		}
		
		public Name add(String arg0) throws InvalidNameException {
			if (arg0.indexOf(NAME_SEP_CHAR) >= 0)
				throw new InvalidNameException();
			name = name + NAME_SEP + arg0;
			return this;
		}
		
		public Name add(int arg0, String arg1) throws InvalidNameException {
			if (arg1.indexOf(NAME_SEP_CHAR) >= 0)
				throw new InvalidNameException();
			String[] cn = name.split(NAME_SEP);
			name = connect(cn, 0, arg0) + NAME_SEP + arg1 + NAME_SEP + connect(cn, arg0, cn.length - arg0);
			return this;
		}
		
		public Name addAll(Name arg0) throws InvalidNameException {
			name = name + NAME_SEP + arg0.toString();
			return this;
		}
		
		public Name addAll(int arg0, Name arg1) throws InvalidNameException {
			return add(arg0, arg1.toString());
		}
		
		public int compareTo(Object arg0) {
			return name.compareTo(arg0.toString());
		}
		
		public boolean endsWith(Name arg0) {
			return name.endsWith(arg0.toString());
		}
		
		public String get(int arg0) {
			String[] cn = name.split(NAME_SEP);
			return cn[arg0];
		}
		
		public Enumeration<String> getAll() {
			return new Enumeration<String>() {
				String cn[];
				
				int i = 0;
				
				public boolean hasMoreElements() {
					if (cn == null) {
						cn = name.split("");
						i = 0;
					}
					return i < cn.length;
				}
				
				public String nextElement() {
					if (hasMoreElements()) {
						return cn[i++];
					}
					throw new NoSuchElementException();
				}
			};
		}
		
		public Name getPrefix(int arg0) {
			String[] cn = name.split(NAME_SEP);
			return new SimpleName(connect(cn, 0, arg0));
		}
		
		public Name getSuffix(int arg0) {
			String[] cn = name.split(NAME_SEP);
			return new SimpleName(connect(cn, arg0, cn.length - arg0));
		}
		
		public boolean isEmpty() {
			return name.length() == 0;
		}
		
		public Object remove(int arg0) throws InvalidNameException {
			String[] cn = name.split(NAME_SEP);
			name = connect(cn, 0, arg0) + NAME_SEP + connect(cn, arg0, cn.length - arg0);
			return cn[arg0];
		}
		
		public int size() {
			return name.split(NAME_SEP).length;
		}
		
		public boolean startsWith(Name arg0) {
			return name.startsWith(arg0.toString());
		}
		
		public Object clone() {
			return new SimpleName(name);
		}
		
		private String connect(String[] parts, int s, int len) throws ArrayIndexOutOfBoundsException {
			if (parts == null || parts.length == 0)
				return "";
			if (len < 0 || s < 0 || s + len > parts.length)
				throw new ArrayIndexOutOfBoundsException(String.format("s=%d, l=%d, nl=%d", s, len, parts.length));
			String result = parts[s];
			for (int i = s + 1; i < parts.length && i < len + s; i++)
				result += NAME_SEP + parts[i];
			return result;
		}
	}
	
	final private static boolean __debug = false;
	
}
