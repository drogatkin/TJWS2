package com.rslakra.mobile.logger;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;

/**
 * See
 * http://developer.android.com/reference/android/util/Log.html#isLoggable(String
 * tag, int level) for the official way to enable and disable logging. This is
 * just a start.
 *
 * @author Rohtash Singh Lakra (Rohtash.Lakra@nasdaq.com)
 * @version 1.0.0
 * @date 03/07/2017 09:55:24 AM
 */
public final class LogHelper {
    
    /**
     * EMPTY_STRING
     */
    public static final String EMPTY_STRING = "".intern();
    
    // Singleton object
    private LogHelper() {
        throw new UnsupportedOperationException("Object creation not allowed for this class.");
    }
    
    /* mLogType */
    private static LogType mLogType = LogType.INFO;
    
    /* USE_LOG4J_LOGGER */
    private final static boolean USE_LOG4J_LOGGER = true;
    
    /**
     * Returns true if the android build version is greater than 10 otherwise false.
     *
     * @return
     */
    public static final boolean isProtectedFileSystem() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1);
    }
    
    /**
     * Returns true if the object is null otherwise false.
     *
     * @param object
     * @return
     */
    public static <T> boolean isNull(T object) {
        return (object == null);
    }
    
    /**
     * Returns true if the object is not null otherwise false.
     *
     * @param object
     * @return
     */
    public static <T> boolean isNotNull(T object) {
        return (!isNull(object));
    }
    
    /**
     * Returns true if either the string is null or empty otherwise false.
     *
     * @param string
     * @return
     */
    public static final boolean isNullOrEmpty(final CharSequence string) {
        return (isNull(string) || string.length() == 0);
    }
    
    /**
     * Returns the logTyp
     *
     * @return
     */
    public static LogType getLogType() {
        return mLogType;
    }
    
    /**
     * The logType to be set.
     *
     * @param logType
     */
    public static void setLogType(final LogType logType) {
        mLogType = logType;
    }
    
    /**
     * Return true if the given <code>logType</code> is same as the default log
     * type otherwise false.
     *
     * @param logType
     * @return
     */
    public static boolean isLogType(final LogType logType) {
        return (getLogType() == logType);
    }
    
    /**
     * Returns true if the logging allowed for the <code>logType</code>
     * otherwise false.
     *
     * @param logType
     * @return
     */
    public static boolean isValidLogType(final LogType logType) {
        return (logType != null && getLogType().ordinal() >= logType.ordinal());
    }
    
    /**************************************************************************
     * Helpers methods.
     **************************************************************************/
    
    /**
     * Returns the formatted string for the given objects.
     *
     * @param format
     * @param objects
     * @return
     */
    private static final String format(String format, Object... objects) {
        return String.format(format, objects);
    }
    
    /**
     * Converts the given object into string, if its not null.
     *
     * @param object
     * @return
     */
    public static final String toString(Object object) {
        return (object == null ? EMPTY_STRING : object.toString());
    }
    
    /**
     * Returns (null) for the null string.
     *
     * @param logMessage
     * @return
     */
    private static final String validateString(final String logMessage) {
        return (logMessage == null ? EMPTY_STRING : logMessage);
    }
    
    /**
     * Logs WARNING messages.
     *
     * @param logTag
     * @param logMessage
     */
    public static void w(final String logTag, final String logMessage) {
        if(isValidLogType(LogType.WARN)) {
            if(USE_LOG4J_LOGGER) {
                AndroidLogger.getLogger(logTag).warn(logMessage);
            } else {
                Log.w(logTag, validateString(logMessage));
            }
        }
    }
    
    /**
     * Logs INFO messages.
     *
     * @param logTag
     * @param logMessage
     */
    public static void i(final String logTag, final String logMessage) {
        if(isValidLogType(LogType.INFO)) {
            if(USE_LOG4J_LOGGER) {
                AndroidLogger.getLogger(logTag).info(logMessage);
            } else {
                Log.i(logTag, validateString(logMessage));
            }
        }
    }
    
    /**
     * Logs LogHelper message.
     *
     * @param logTag
     * @param logMessage
     */
    public static void d(final String logTag, final String logMessage) {
        if(isValidLogType(LogType.DEBUG)) {
            if(USE_LOG4J_LOGGER) {
                AndroidLogger.getLogger(logTag).debug(logMessage);
            } else {
                Log.d(logTag, validateString(logMessage));
            }
        }
    }
    
    /**
     * Logs VERBOSE messages.
     *
     * @param logTag
     * @param logMessage
     */
    public static void v(final String logTag, final String logMessage) {
        if(isValidLogType(LogType.VERBOSE)) {
            if(USE_LOG4J_LOGGER) {
                AndroidLogger.getLogger(logTag).debug(logMessage);
            } else {
                Log.v(logTag, validateString(logMessage));
            }
        }
    }
    
    /**
     * Logs ERROR messages.
     *
     * @param logTag
     * @param logMessage
     */
    public static void e(final String logTag, final String logMessage) {
        if(isValidLogType(LogType.ERROR)) {
            if(USE_LOG4J_LOGGER) {
                AndroidLogger.getLogger(logTag).error(logMessage);
            } else {
                Log.e(logTag, validateString(logMessage));
            }
        }
    }
    
    /**
     * Logs ERROR messages.
     *
     * @param logTag
     * @param throwable
     * @param logMessage
     */
    public static void e(final String logTag, final Throwable throwable, final String logMessage) {
        if(isValidLogType(LogType.ERROR)) {
            if(USE_LOG4J_LOGGER) {
                AndroidLogger.getLogger(logTag).error(logMessage, throwable);
            } else {
                Log.e(logTag, validateString(logMessage), throwable);
            }
        }
    }
    
    /**************************************************************************
     * More-Flexible Helpers methods.
     **************************************************************************/
    
    /**
     * Logs WARNING messages.
     *
     * @param logTag
     * @param logMessage
     */
    public static void w(final String logTag, final Object logMessage) {
        w(logTag, toString(logMessage));
    }
    
    /**
     * Logs INFO messages.
     *
     * @param logTag
     * @param logMessage
     */
    public static void i(final String logTag, final Object logMessage) {
        i(logTag, toString(logMessage));
    }
    
    /**
     * Logs LogHelper messages.
     *
     * @param logTag
     * @param logMessage
     */
    public static void d(final String logTag, final Object logMessage) {
        d(logTag, toString(logMessage));
    }
    
    /**
     * Logs VERBOSE messages.
     *
     * @param logTag
     * @param logMessage
     */
    public static void v(final String logTag, final Object logMessage) {
        v(logTag, toString(logMessage));
    }
    
    /**
     * Logs ERROR messages.
     *
     * @param logTag
     * @param logMessage
     */
    public static void e(final String logTag, final Object logMessage) {
        e(logTag, toString(logMessage));
    }
    
    /**
     * Logs WARNING messages.
     *
     * @param logTag
     * @param format
     * @param arguments
     */
    public static void w(final String logTag, final String format, final Object... arguments) {
        w(logTag, format(format, arguments));
    }
    
    /**
     * Logs INFO messages.
     *
     * @param logTag
     * @param format
     * @param arguments
     */
    public static void i(final String logTag, final String format, final Object... arguments) {
        i(logTag, format(format, arguments));
    }
    
    /**
     * Logs LogHelper messages.
     *
     * @param logTag
     * @param format
     * @param arguments
     */
    public static void d(final String logTag, final String format, final Object... arguments) {
        d(logTag, format(format, arguments));
    }
    
    /**
     * Logs VERBOSE messages.
     *
     * @param logTag
     * @param format
     * @param arguments
     */
    public static void v(final String logTag, final String format, final Object... arguments) {
        v(logTag, format(format, arguments));
    }
    
    /**
     * Logs ERROR messages.
     *
     * @param logTag
     * @param format
     * @param arguments
     */
    public static void e(final String logTag, final String format, final Object... arguments) {
        e(logTag, format(format, arguments));
    }
    
    /**
     * Logs ERROR messages.
     *
     * @param logTag
     * @param throwable
     */
    public static void e(final String logTag, final Throwable throwable) {
        e(logTag, throwable, throwable.getLocalizedMessage());
    }
    
    /**
     * Logs ERROR messages.
     *
     * @param logTag
     * @param throwable
     * @param format
     * @param arguments
     */
    public static void e(final String logTag, final Throwable throwable, final String format, final Object... arguments) {
        e(logTag, throwable, format(format, arguments));
    }
    
    /**
     * Logs the URL details.
     *
     * @param logTag
     * @param uri
     */
    public static void logUri(final String logTag, final Uri uri) {
        d(logTag, "urlString:" + uri.toString());
        d(logTag, "Scheme:" + uri.getScheme());
        d(logTag, "Host:" + uri.getHost());
        d(logTag, "QueryParameterNames:" + uri.getQueryParameterNames());
        d(logTag, "Query:" + uri.getQuery());
    }
    
    /**
     * Logs the URL details.
     *
     * @param logTag
     * @param urlString
     */
    public static void logUri(final String logTag, final String urlString) {
        logUri(logTag, Uri.parse(urlString));
    }
    
    /**
     * Logs the <code>WebResourceRequest</code>.
     *
     * @param logTag
     * @param webRequest
     */
    @TargetApi(21)
    public static void logWebRequest(final String logTag, final WebResourceRequest webRequest) {
        d(logTag, "urlString:" + webRequest.getUrl().toString());
        d(logTag, "Method:" + webRequest.getMethod());
        d(logTag, "RequestHeaders:" + webRequest.getRequestHeaders());
        d(logTag, "QueryParameterNames:" + webRequest.getUrl().getQueryParameterNames());
        d(logTag, "Query:" + webRequest.getUrl().getQuery());
    }
    
}
