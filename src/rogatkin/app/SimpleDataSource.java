/*
 * tjws - SimpleDataSource.java
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
 * $Id: SimpleDataSource.java,v 1.30 2013/03/20 03:49:46 cvs Exp $
 * Created on Mar 25, 2007
 * @author Dmitriy
 */
package rogatkin.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.sql.RowSet;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The class presents data source, which is created based on a property file
 * <p>
 * The following properties are allowed: <br>
 * <ul>
 * <li><i>jndi-name</i> - under this name the data source will be registered in
 * JNDI if name starts with <strong>jdbc</strong>, then prefix
 * <i>java:comp/env/</i> will be added.
 * <li><i>driver-class</i> - class name of JDBC driver
 * <li><i>url</i> - JDBC connection URL
 * <li><i>user</i> - connection user
 * <li><i>password</i> - connection password
 * <li><i>pool-size</i> - max number of allocated connections, 0 = no size
 * limitation, -1 = no pool used
 * <li><i>access-timeout</i> - timeout in ms before getting an exception on
 * connection request when no connections are available, 0 means wait forever
 * <li><i>driver-class-path</i> - defines class path to driver archive, unless
 * it is already in boot classpath
 * <li><i>prob-query</i> defines a query to verify that given connection is
 * valid, executed at time getting a connection from pool. If
 * <strong>isValid</strong> is specified as value, then isValid() method of SQL
 * connection is used (note that it is available only in Java 6 drivers). If
 * <strong>isClosed</strong> is specified, then isClosed() method used for a
 * connection validation.
 * <li><i>exception-handler</i> - a name of class implementing static public
 * method boolean validate(SQLException, Connection). This class is used to
 * verify if SQLException indicates that the connection isn't valid anymore and
 * has to be removed from the pool. The method returns true, if the connection
 * still good for further use.
 * <li><i>pool-shrink-size</> - max available connections in pool (not
 * implemented yet)
 * </ul>
 * 
 * @author dmitriy
 * 
 */
public class SimpleDataSource extends ObjectPool<Connection> implements DataSource {
	
	public final static String RW_ISVALID = "isValid";
	public final static String RW_ISCLOSED = "isClosed";
	public final static String CP_DEFAULT = "application";
	protected final static int DEFAULT_CAPACITY = 20;
	protected Properties dataSourceProperties, conectionProperties;
	protected Driver driver;
	private int capacity;
	private PrintWriter logWriter;
	private Method connectionValidateMethod;
	private String validateQuery;
	private boolean appClassPath;
	
	/**
	 * 
	 * @param definitionPropertiesLocation
	 * @param classLoader
	 */
	public SimpleDataSource(String definitionPropertiesLocation, ClassLoader classLoader) {
		super(new ArrayBlockingQueue<Connection>(DEFAULT_CAPACITY));
		logWriter = new PrintWriter(System.out);
		InputStream propertiesStream = null;
		File file = new File(definitionPropertiesLocation);
		try {
			if (file.exists()) {
				propertiesStream = new FileInputStream(file);
			} else {
				propertiesStream = new URL(definitionPropertiesLocation).openStream();
			}
			dataSourceProperties = new Properties();
			if (definitionPropertiesLocation.toLowerCase().endsWith("context.xml")) {
				contextToProperties(propertiesStream);
			} else if (definitionPropertiesLocation.toLowerCase().endsWith(".xml")) {
				dataSourceProperties.loadFromXML(propertiesStream);
			} else {
				dataSourceProperties.load(propertiesStream);
			}
			init(classLoader);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException(e);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Data source properties file doesn't exist, and can't be resolved as URL", e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Connection validator class problem", e);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		} finally {
			if (propertiesStream != null) {
				try {
					propertiesStream.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * 
	 * @param classLoader
	 * @throws Exception
	 */
	protected void init(ClassLoader classLoader) throws Exception {
		String classPath = dataSourceProperties.getProperty("driver-class-path");
		if (classPath == null) {
			// if (classLoader != null)
			// Class.forName(dataSourceProperties.getProperty("driver-class"),
			// true, classLoader);
			// else
			String driverClass = dataSourceProperties.getProperty("driver-class");
			if (driverClass == null) {
				return; // no data source
			}
			Class.forName(driverClass);
			driver = DriverManager.getDriver(dataSourceProperties.getProperty("url"));
		} else {
			String[] classPaths = classPath.split(File.pathSeparator);
			URL[] urls = new URL[classPaths.length];
			for (int i = 0; i < urls.length; i++) {
				urls[i] = new URL("file:" + classPaths[i]);
			}
			if (CP_DEFAULT.equalsIgnoreCase(classPath)) {
				driver = (Driver) Class.forName(dataSourceProperties.getProperty("driver-class"), true, classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader).newInstance();
				appClassPath = true;
			} else {
				driver = (Driver) Class.forName(dataSourceProperties.getProperty("driver-class"), true, classLoader = new URLClassLoader(urls, DriverManager.class.getClassLoader())).newInstance();
			}
		}
		conectionProperties = new Properties();
		if (dataSourceProperties.getProperty("user") != null) {
			conectionProperties.setProperty("user", dataSourceProperties.getProperty("user"));
			if (dataSourceProperties.getProperty("password") != null) {
				conectionProperties.setProperty("password", dataSourceProperties.getProperty("password"));
			}
		}
		try {
			setTimeout(Integer.parseInt(dataSourceProperties.getProperty("access-timeout")));
		} catch (Exception e) {
		}
		
		try {
			capacity = Integer.parseInt(dataSourceProperties.getProperty("pool-size"));
			this.pool = new ArrayBlockingQueue<Connection>(capacity, true);
			this.borrowed = new ArrayList<Connection>(capacity);
		} catch (Exception e) {
			capacity = DEFAULT_CAPACITY;
		}
		validateQuery = dataSourceProperties.getProperty("prob-query");
		String conValClass = dataSourceProperties.getProperty("exception-handler");
		if (conValClass != null) {
			connectionValidateMethod = (classLoader == null ? Class.forName(conValClass) : Class.forName(conValClass, true, classLoader)).getMethod("validate", SQLException.class, Connection.class);
		}
		
		String jndiName = dataSourceProperties.getProperty("jndi-name");
		if (jndiName != null) {
			if (jndiName.startsWith("jdbc/")) {
				jndiName = "java:comp/env/" + jndiName;
			}
			
			InitialContext ic = new InitialContext();
			try {
				ic.lookup(jndiName);
				ic.rebind(jndiName, this);
			} catch (NamingException ne) {
				ic.bind(jndiName, this);
			}
		}
	}
	
	public Connection getConnection() throws SQLException {
		Connection realConn = validateQuery == null ? get() : getValidated();
		return (Connection) Proxy.newProxyInstance(realConn.getClass().getClassLoader(), Wrapper != null ? new Class[] { Connection.class, Wrapper } : new Class[] { Connection.class }, new ConnectionWrapperHandler(realConn));
	}
	
	@Override
	public String toString() {
		Properties masked = (Properties) dataSourceProperties.clone();
		masked.setProperty("password", "*******");
		Properties maskedConn = (Properties) conectionProperties.clone();
		maskedConn.setProperty("password", "*******");
		return "Pooled data source : " + masked + "\n" + maskedConn + "\n capacity:" + capacity + ", available: " + pool.size() + ", borrowed: " + borrowed.size();
	}
	
	public boolean isScopeApp() {
		return appClassPath;
	}
	
	private Connection getValidated() {
		boolean bad = true;
		do {
			Connection result = get();
			Statement statement = null;
			try {
				if (validateQuery.equals(RW_ISVALID))
					try {
						if ((Boolean) result.getClass().getMethod("isValid", int.class).invoke(10)) {
							return result;
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				else if (validateQuery.equals(RW_ISCLOSED)) {
					if (result.isClosed() == false) {
						return result;
					}
				} else {
					/*
					 * TODO it can be reasonable to execute the query in a
					 * thread and join in millis
					 * because dropped connection can hung a query
					 */
					statement = result.createStatement();
					statement.execute(validateQuery);
					return result;
				}
			} catch (SQLException ex) {
				log("Discarding connection %s because %s%n", null, result, ex);
			} finally {
				if (statement != null) {
					try {
						statement.close();
					} catch (SQLException e) {
					}
				}
			}
			remove(result);
		} while (bad);
		
		throw new IllegalStateException();
	}
	
	public Connection getConnection(String user, String password) throws SQLException {
		conectionProperties.setProperty("user", user);
		conectionProperties.setProperty("password", password);
		return getConnection();
	}
	
	public PrintWriter getLogWriter() throws SQLException {
		return logWriter;
	}
	
	public int getLoginTimeout() throws SQLException {
		return timeout;
	}
	
	public void setLogWriter(PrintWriter timeout) throws SQLException {
		logWriter = timeout;
	}
	
	public void setLoginTimeout(int timeout) throws SQLException {
		// not quite what it is
		setTimeout(timeout);
	}
	
	public boolean isWrapperFor(Class<?> _class) throws SQLException {
		return DataSource.class.equals(_class) || ObjectPool.class.equals(_class);
	}
	
	public <T> T unwrap(Class<T> _class) throws SQLException {
		if (isWrapperFor(_class)) {
			return (T) this;
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param message
	 * @param ex
	 * @param args
	 */
	protected void log(String message, Throwable ex, Object... args) {
		if (args == null || args.length == 0) {
			logWriter.write(message + "\n"); // ?? lineSeparator?
		} else {
			logWriter.write(String.format(message, args));
		}
		if (ex != null) {
			ex.printStackTrace(logWriter);
		}
		
		logWriter.flush();
	}
	
	@Override
	protected void discard(Connection obj) {
		Connection unwrapped = null;
		try {// System.err.printf("Discarding %s%n", obj);
			unwrapped = (Connection) obj.getClass().getMethod("unwrap", Class.class).invoke(obj, Connection.class);
			unwrapped.close();
		} catch (Exception e) {// e.printStackTrace();
			if (unwrapped == null) {
				try {
					obj.close();
				} catch (SQLException e1) {
					// e1.printStackTrace();
				}
			}
		}
	}
	
	@Override
	protected Connection create() {
		try {
			return driver.connect(dataSourceProperties.getProperty("url"), conectionProperties);
		} catch (SQLException e) {
			log("Can't create connection for %s%n", e, dataSourceProperties.getProperty("url"));
			throw new IllegalArgumentException("Can't create connection, check connection parameters and class path for JDBC driver", e);
		}
	}
	
	private Throwable processException(InvocationTargetException ite, Connection conn, Connection proxyConn) throws IllegalArgumentException, IllegalAccessException {
		if (connectionValidateMethod != null) {
			Throwable se = ite.getCause();
			// System.err.println("Cause*********"+se+" instance sql:"+(se
			// instanceof SQLException));
			try {
				if (se instanceof SQLException && connectionValidateMethod.invoke(null, se, conn).equals(Boolean.FALSE)) {
					remove(conn);
				}
			} catch (InvocationTargetException e) {
			}
			
			return se;
		}
		return ite.getCause();
	}
	
	@Override
	public int getCapacity() {
		return capacity;
	}
	
	// @Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}
	
	private void contextToProperties(InputStream contextXmlStream) throws XPathExpressionException {
		XPath xp = XPathFactory.newInstance().newXPath();
		Node document = (Node) xp.evaluate("/Context", new InputSource(contextXmlStream), XPathConstants.NODE);
		NodeList nodes = (NodeList) xp.evaluate("Resource", document, XPathConstants.NODESET);
		int nodesLen = nodes.getLength();
		if (nodesLen > 1) {
			throw new IllegalArgumentException("Only one resource is supported");
		}
		
		for (int p = 0; p < nodesLen; p++) {
			NamedNodeMap attrs = nodes.item(p).getAttributes();
			Node metadataAttr = attrs.getNamedItem("name");
			dataSourceProperties.setProperty("jndi-name", "java:comp/env/" + metadataAttr.getTextContent());
			metadataAttr = attrs.getNamedItem("type");
			if ("javax.sql.DataSource".equals(metadataAttr.getTextContent()) == false) {
				throw new IllegalArgumentException("Only SQL data sources are supported");
			}
			
			metadataAttr = attrs.getNamedItem("auth");
			metadataAttr = attrs.getNamedItem("username");
			if (metadataAttr != null) {
				dataSourceProperties.setProperty("user", metadataAttr.getTextContent());
			}
			
			metadataAttr = attrs.getNamedItem("password");
			if (metadataAttr != null) {
				dataSourceProperties.setProperty("password", metadataAttr.getTextContent());
			}
			
			metadataAttr = attrs.getNamedItem("driverClassName");
			if (metadataAttr != null) {
				dataSourceProperties.setProperty("driver-class", metadataAttr.getTextContent());
			}
			
			metadataAttr = attrs.getNamedItem("url");
			if (metadataAttr == null) {
				throw new IllegalArgumentException("Data source URL is required");
			}
			
			dataSourceProperties.setProperty("url", metadataAttr.getTextContent());
			metadataAttr = attrs.getNamedItem("driverClassPath");
			if (metadataAttr != null) {
				dataSourceProperties.setProperty("driver-class-path", metadataAttr.getTextContent());
			}
			
			metadataAttr = attrs.getNamedItem("validationQuery");
			if (metadataAttr != null) {
				dataSourceProperties.setProperty("prob-query", metadataAttr.getTextContent());
			}
			
			metadataAttr = attrs.getNamedItem("maxActive");
			if (metadataAttr != null) {
				dataSourceProperties.setProperty("pool-size", metadataAttr.getTextContent());
			}
			
			metadataAttr = attrs.getNamedItem("maxIdle");
			if (metadataAttr != null) {
				dataSourceProperties.setProperty("pool-shrink-size", metadataAttr.getTextContent());
			}
		}
	}
	
	class ConnectionWrapperHandler implements InvocationHandler {
		
		/* realConnection */
		private Connection realConnection;
		
		ConnectionWrapperHandler(Connection realConnection) {
			this.realConnection = realConnection;
		}
		
		public Object invoke(final Object proxyConn, Method methd, Object[] params) throws Throwable {
			if (realConnection == null) {
				throw new SQLException("The connection is closed");
			}
			
			if (methd.getName().equals("close")) {
				// log("Closing %s%n", null, proxyConn);
				if (realConnection.getAutoCommit() == false) {
					try {
						realConnection.rollback();
					} catch (SQLException se) {
						
					}
				}
				
				put(realConnection);
				realConnection = null;
			} else if (methd.getName().equals("unwrap")) { // &&
				return realConnection;
			} else if (methd.getName().equals("isWrapperFor")) {
				return ((Class<?>) params[0]).isInstance(realConnection);
			} else if (methd.getName().equals("equals")) {
				return proxyConn == params[0];
			} else {
				try {
					final Object realStmt = methd.invoke(realConnection, params);
					if (realStmt instanceof Statement == false) {
						return realStmt;
					}
					
					// wrap statement
					return Proxy.newProxyInstance(realStmt.getClass().getClassLoader(), Wrapper != null ? new Class[] { CallableStatement.class, PreparedStatement.class, Statement.class, Wrapper } : new Class[] { CallableStatement.class, PreparedStatement.class, Statement.class }, new InvocationHandler() {
						public Object invoke(final Object proxyStmt, Method methd, Object[] params) throws Throwable {
							if (methd.getName().equals("getConnection")) {
								return proxyConn;
							} else if (methd.getName().equals("unwrap")) {
								return realStmt; // real statement
							} else if (methd.getName().equals("isWrapperFor")) {
								return ((Class<?>) params[0]).isInstance(realStmt);
							}
							
							try {
								final Object realRS = methd.invoke(realStmt, params);
								if (realRS instanceof ResultSet == false) {
									return realRS;
								}
								
								return Proxy.newProxyInstance(realRS.getClass().getClassLoader(), Wrapper != null ? new Class[] { RowSet.class, ResultSet.class, Wrapper } : new Class[] { RowSet.class, ResultSet.class }, new InvocationHandler() {
									public Object invoke(final Object proxyRS, Method methd, Object[] params) throws Throwable {
										if (methd.getName().equals("getStatement")) {
											return proxyStmt;
										} else if (methd.getName().equals("unwrap")) {
											return realRS; // resultset
										} else if (methd.getName().equals("isWrapperFor")) {
											return ((Class<?>) params[0]).isInstance(realRS);
										}
										
										try {
											return methd.invoke(realRS, params);
										} catch (InvocationTargetException ite) {
											throw processException(ite, realConnection, (Connection) proxyConn);
										}
									}
								});
							} catch (InvocationTargetException ite) {
								throw processException(ite, realConnection, (Connection) proxyConn);
							}
						}
					});
				} catch (InvocationTargetException ite) {
					throw processException(ite, realConnection, (Connection) proxyConn);
				}
			}
			
			return null;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		// System.err.printf("finilize%s%n", dataSourceProperties);
		invalidate();
		super.finalize();
	}
	
}
