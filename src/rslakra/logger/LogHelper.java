/**
 * 
 */
package rslakra.logger;

import java.io.PrintStream;
import java.util.Date;

import Acme.IOHelper;

/**
 * @author Rohtash Singh Lakra (Rohtash.Lakra@nasdaq.com)
 * @date 03/21/2018 10:48:44 AM
 */
public final class LogHelper {
	
	private static boolean sLogEnabled = false;
	private static transient PrintStream sLogStream;
	
	/** singleton instance. */
	private LogHelper() {
		throw new RuntimeException("Object creation is not allowed for this object!");
	}
	
	/**
	 * Returns the sLogEnabled value.
	 * 
	 * @return
	 */
	public static boolean isLogEnabled() {
		return sLogEnabled;
	}
	
	/**
	 * The sLogEnabled to be set.
	 * 
	 * @param logEnabled
	 */
	public static void setLogEnabled(final boolean logEnabled) {
		sLogEnabled = logEnabled;
	}
	
	/**
	 * Returns the sLogStream value.
	 * 
	 * @return
	 */
	public static PrintStream getLogStream() {
		return sLogStream;
	}
	
	/**
	 * The sLogStream to be set.
	 * 
	 * @param logStream
	 */
	public static void setLogStream(PrintStream logStream) {
		sLogStream = logStream;
	}
	
	/**
	 * Logs the given message.
	 * 
	 * @param message
	 */
	public static final void log(String message) {
		if (isLogEnabled()) {
			if (getLogStream() != null) {
				getLogStream().println("[" + new Date().toString() + "] " + message);
			} else {
				System.out.println("[" + new Date().toString() + "] " + message);
			}
		}
	}
	
	/**
	 * Logs the given message.
	 * 
	 * @see javax.servlet.ServletContext#log(java.lang.String,
	 *      java.lang.Throwable)
	 */
	public static void log(String message, Throwable throwable) {
		if (throwable != null) {
			message = message + IOHelper.getLineSeparator() + IOHelper.toString(throwable);
		}
		log(message);
	}
	
	/**
	 * Logs the given message.
	 * 
	 * @param throwable
	 */
	public static void log(Throwable throwable) {
		if (throwable != null) {
			log(IOHelper.getLineSeparator() + IOHelper.toString(throwable));
		}
	}
	
}
