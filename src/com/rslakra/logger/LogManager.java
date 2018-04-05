// Copyright (C)2018 by Rohtash Singh Lakra <rohtash.singh@gmail.com>.
// All rights reserved.
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
// Visit the https://github.com/rslakra/TJWS2 page for up-to-date versions of
// this and other fine Java utilities.
//
// All enhancements Copyright (C)2018 by Rohtash Singh Lakra
// This version is compatible with JSDK 2.5
// https://github.com/rslakra/TJWS2
package com.rslakra.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * @author Rohtash Singh Lakra (Rohtash.Lakra@nasdaq.com)
 * @date 03/21/2018 10:48:44 AM
 */
public final class LogManager {
	
	/* LOG_PATTERN */
	private static final String LOG_PATTERN = "[%d{MM-dd-yyyy hh:mm:ss.S a}] %5p [%t] [%c{1}(%L)] - %m%n";
	
	/* consoleAppender */
	private static final ConsoleAppender consoleAppender = new ConsoleAppender();
	
	/* mCachedLoggers. */
	private static final Map<String, Logger> mCachedLoggers = new ConcurrentHashMap<String, Logger>();
	
	/** mRootLogger */
	private static Logger mRootLogger;
	
	/** singleton instance. */
	private LogManager() {
		throw new RuntimeException("Object creation is not allowed for this object!");
	}
	
	/**
	 * Returns the singleton instance of the root logger.
	 *
	 * @return
	 */
	private static final Logger getRootLogger() {
		if (mRootLogger == null) {
			synchronized (LogManager.class) {
				if (mRootLogger == null) {
					mRootLogger = Logger.getRootLogger();
					mRootLogger.getLoggerRepository().resetConfiguration();
					// configure the appender
					consoleAppender.setLayout(new PatternLayout(LOG_PATTERN));
					consoleAppender.activateOptions();
					// add appender to any Logger (here is root)
					mRootLogger.addAppender(consoleAppender);
					/* DEFAULT LOG LEVEL. */
					mRootLogger.setLevel(Level.WARN);
				}
			}
		}
		
		return mRootLogger;
	}
	
	/**
	 * Return the log level of the root logger
	 *
	 * @return Log level of the root logger
	 */
	private static Level getLogLevel() {
		return getRootLogger().getLevel();
	}
	
	/**
	 * Sets log level for the root logger
	 *
	 * @param logLevel
	 *            Log level for the root logger
	 */
	public static void setLogLevel(final Level logLevel) {
		getRootLogger().setLevel(logLevel);
		consoleAppender.setThreshold(logLevel);
	}
	
	/**
	 * Returns true if the current logLevel is >= the given logLevel otherwise
	 * false.
	 *
	 * @param logLevel
	 * @return
	 */
	public static boolean isLogEnabledFor(final Level logLevel) {
		return (logLevel != null && logLevel.toInt() >= getLogLevel().toInt());
	}
	
	/**
	 * Returns the <code>Logger</code> object for the specified
	 * <code>logClass</code> class.
	 *
	 * @param logClass
	 * @return
	 */
	public static Logger getLogger(Class<?> logClass) {
		Logger logger = null;
		if (logClass == null) {
			throw new IllegalArgumentException("logClass is NULL! it must provide!");
		}
		
		logger = mCachedLoggers.get(logClass.getName());
		if (logger == null) {
			synchronized (mCachedLoggers) {
				if (logger == null) {
					logger = Logger.getLogger(logClass.getName());
					/* cache this class logger to reuse */
					mCachedLoggers.put(logClass.getName(), logger);
				}
			}
		}
		
		return logger;
	}
	
	/**
	 * Returns the <code>Logger</code> object for the specified
	 * <code>logClassName</code> class name.
	 * <p>
	 *
	 * @param logClassName
	 * @return
	 */
	public static Logger getLogger(final String logClassName) {
		Logger logger = null;
		if (logClassName == null || logClassName.trim().length() == 0) {
			throw new IllegalArgumentException("logClass is NULL! it must provide!");
		}
		
		logger = mCachedLoggers.get(logClassName);
		if (logClassName == null || logClassName.trim().length() == 0) {
			synchronized (mCachedLoggers) {
				if (logClassName == null || logClassName.trim().length() == 0) {
					logger = Logger.getLogger(logClassName);
					/* cache this class logger to reuse */
					mCachedLoggers.put(logClassName, logger);
				}
			}
		}
		
		return logger;
	}
	
	/**
	 * Returns the string representation of the specified <code>object</code>
	 * object, if it's not
	 * null otherwise empty string.
	 *
	 * @param object
	 * @return
	 */
	public static final String toString(final Object object) {
		return (object == null ? "".intern() : object.toString());
	}
	
	/**
	 * Returns the string representation of the <code>throwable</code> object.
	 * 
	 * @param throwable
	 * @return
	 */
	public static final String toString(final Throwable throwable) {
		if (throwable == null) {
			return "".intern();
		} else {
			final StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			throwable.printStackTrace(printWriter);
			printWriter.close();
			return stringWriter.toString();
		}
	}
	
	/**
	 * Returns the value of the <code>sDebugEnabled</code> property.
	 * 
	 * @return
	 */
	public static boolean isDebugEnabled() {
		return isLogEnabledFor(Level.DEBUG);
	}
	
	/**
	 * Returns the value of the <code>sDebugEnabled</code> property.
	 * 
	 * @return
	 */
	public static boolean isInfoEnabled() {
		return isLogEnabledFor(Level.INFO);
	}
	
	/**
	 * 
	 * @param object
	 */
	public static void fatal(final Object object) {
		getRootLogger().fatal(toString(object));
	}
	
	/**
	 * 
	 * @param object
	 * @param throwable
	 */
	public static void fatal(final Object object, final Throwable throwable) {
		getRootLogger().fatal(toString(object), throwable);
	}
	
	/**
	 * 
	 * @param object
	 * @return
	 */
	public static void error(final Object object) {
		getRootLogger().error(((object instanceof Throwable) ? toString((Throwable) object) : toString(object)));
	}
	
	/**
	 * 
	 * @param object
	 * @param throwable
	 */
	public static void error(final Throwable throwable) {
		getRootLogger().error(toString(throwable));
	}
	
	/**
	 * 
	 * @param object
	 * @param throwable
	 */
	public static void error(final Object object, final Throwable throwable) {
		getRootLogger().error(toString(object), throwable);
	}
	
	/**
	 * 
	 * @param object
	 */
	public static void warn(final Object object) {
		getRootLogger().warn(toString(object));
	}
	
	/**
	 * 
	 * @param object
	 * @param throwable
	 */
	public static void warn(final Object object, final Throwable throwable) {
		getRootLogger().warn(toString(object), throwable);
	}
	
	/**
	 * 
	 * @param object
	 */
	public static void info(final Object object) {
		getRootLogger().info(toString(object));
	}
	
	/**
	 * 
	 * @param object
	 * @param throwable
	 */
	public static void info(final Object object, final Throwable throwable) {
		getRootLogger().info(toString(object), throwable);
	}
	
	/**
	 * 
	 * @param object
	 */
	public static void debug(final Object object) {
		getRootLogger().debug(toString(object));
	}
	
	/**
	 * 
	 * @param object
	 * @param throwable
	 */
	public static void debug(final Object object, final Throwable throwable) {
		getRootLogger().debug(toString(object), throwable);
	}
	
	// /**
	// *
	// * @param message
	// */
	// public static final void log(String message) {
	// if (isLogEnabled()) {
	// if (getLogStream() != null) {
	// getLogStream().println("[" + new Date().toString() + "] " + message);
	// } else {
	// System.out.println("[" + new Date().toString() + "] " + message);
	// }
	// }
	// }
	//
	// /**
	// * Logs the given message.
	// *
	// * @see javax.servlet.ServletContext#log(java.lang.String,
	// * java.lang.Throwable)
	// */
	// public static void log(String message, Throwable throwable) {
	// if (throwable != null) {
	// message = message + IOHelper.getLineSeparator() +
	// IOHelper.toString(throwable);
	// }
	// log(message);
	// }
	//
	// /**
	// * Logs the given message.
	// *
	// * @param throwable
	// */
	// public static void log(Throwable throwable) {
	// if (throwable != null) {
	// log(IOHelper.getLineSeparator() + IOHelper.toString(throwable));
	// }
	// }
	
}
