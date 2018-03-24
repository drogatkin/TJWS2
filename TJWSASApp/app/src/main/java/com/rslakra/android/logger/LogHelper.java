package com.rslakra.android.logger;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.widget.Toast;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * See
 * http://developer.android.com/reference/android/util/Log.html#isLoggable(String
 * tag, int level) for the official way to enable and disable logging. This is
 * just a start.
 *
 * This class handles the logs for LogCat as well as file system logs.
 * <p>
 * The logger can also be used as: <code>
 * private static Logger log = AndroidLogger.getLogger(AndroidLogger.class);
 * log.LogHelper("Logging Message Here");
 * </code>*
 *
 * @author Rohtash Singh Lakra
 * @version 1.0.0
 * @since Nov 16, 2015 12:12:22 PM
 */
public final class LogHelper {
    
    /**
     * LOG_TAG
     */
    public static final String LOG_TAG = "LogHelper".intern();
    
    /**
     * EMPTY_STRING
     */
    public static final String EMPTY_STRING = "".intern();
    
    /** LOG_FILE_NAME */
    private final static String LOG_FILE_NAME = "android.log";
    /* maximum 3 files */
    private final static int MAX_BACKUP_FILES = 3;
    /* 5 MB */
    private final static long MAX_FILE_SIZE = 1024 * 1024 * 5;
    /* LOG_PATTERN */
    private final static String LOG_PATTERN = "[%d{yyyy-MM-dd HH:mm:ss.S zzz}] %5p [%c{1}(%L)] - %m%n";
    
    /* mLogType */
    private static LogType sLogType = LogType.INFO;
    
    /* log4JLogsEnabled */
    private static boolean sLog4JLogsEnabled = true;
    
    /* sLog4jConfigurator */
    private final static AndroidConfigurator sLog4jConfigurator = new AndroidConfigurator();
    
    /** Singleton object */
    private LogHelper() {
        throw new UnsupportedOperationException("Object creation not allowed for this class.");
    }
    
    /**
     * Returns true if the android version is greater than 10 (GINGERBREAD_MR1) otherwise false.
     *
     * @return
     */
    public static final boolean isProtectedFileSystem() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1);
    }
    
    /**
     * Returns true if the JVM is android (dalvik) otherwise false.
     *
     * @return
     */
    public static boolean isAndroid() {
        return (System.getProperty("java.vm.name").startsWith("Dalvik".intern()));
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
     * Returns the path combined with the fileName.
     *
     * @param parentFolder
     * @param fileName
     * @return
     */
    public static String pathString(final String parentFolder, final String fileName) {
        if(isNullOrEmpty(parentFolder)) {
            return fileName;
        } else if(isNullOrEmpty(fileName)) {
            return parentFolder;
        } else if(parentFolder.endsWith(fileName)) {
            return parentFolder;
        } else if(fileName.startsWith(parentFolder)) {
            return fileName;
        } else if(parentFolder.endsWith(File.separator) || fileName.startsWith(File.separator)) {
            return parentFolder + fileName;
        } else {
            return parentFolder + File.separator + fileName;
        }
    }
    
    /**
     * Creates the folders/directory, if it does not exist.
     *
     * a/b/c/d/file.log
     *
     * @param mFile
     * @return
     */
    public static boolean makeFolders(final File mFile) {
        if(isNotNull(mFile) && !mFile.exists()) {
            if(!mFile.getParentFile().exists()) {
                makeFolders(mFile.getParentFile());
            }
            
            if(mFile.getParentFile().exists() && !mFile.exists()) {
                if(!mFile.mkdir()) {
                    LogHelper.w(LOG_TAG, "Unable to create [" + mFile.getAbsolutePath() + "] folder.");
                }
            }
            
        }
        
        return true;
    }
    
    /**
     * Deletes the files recursively.
     *
     * @param rootFile
     * @param deleteRecursively
     * @return
     */
    public static boolean deleteRecursively(final File rootFile, final boolean deleteRecursively) {
        if(rootFile != null && rootFile.exists()) {
            if(rootFile.isDirectory()) {
                for(File nextFile : rootFile.listFiles()) {
                    if(nextFile.isDirectory() && deleteRecursively) {
                        if(!deleteRecursively(nextFile, deleteRecursively)) {
                            return false;
                        }
                    } else if(nextFile.isFile()) {
                        if(!nextFile.delete()) {
                            return false;
                        }
                    }
                }
                if(!rootFile.delete()) {
                    return false;
                }
            } else if(rootFile.isFile()) {
                if(!rootFile.delete()) {
                    return false;
                }
            }
            
        }
        
        return true;
    }
    
    
    /**
     * Returns the sLogType value.
     *
     * @return
     */
    public static LogType getLogType() {
        return sLogType;
    }
    
    /**
     * The sLogType to be set.
     *
     * @param logType
     */
    public static void setLogType(final LogType logType) {
        sLogType = logType;
    }
    
    /**
     * Returns the sLog4JLogsEnabled value.
     *
     * @return
     */
    public static boolean isLog4JLogsEnabled() {
        return sLog4JLogsEnabled;
    }
    
    /**
     * The sLog4JLogsEnabled to be set.
     *
     * @param log4JLogsEnabled
     */
    public static void setLog4JLogsEnabled(final boolean log4JLogsEnabled) {
        sLog4JLogsEnabled = log4JLogsEnabled;
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
    public static boolean isLogEnabledFor(final LogType logType) {
        return (logType != null && getLogType().ordinal() >= logType.ordinal());
    }
    
    /**************************************************************************
     * Configure Log4J logger
     **************************************************************************/
    
    /**
     * Configures the logger with the given settings.
     *
     * @param logFolderPath
     * @param fileName
     * @param logLevel
     * @param logPattern
     * @param maxBackupFiles
     * @param maxFileSize
     */
    public static void log4jConfigure(final String logFolderPath, final String fileName, final LogType logLevel, final String logPattern, final int maxBackupFiles, final long maxFileSize) {
        setLogType(logLevel);
        sLog4jConfigurator.configure(logFolderPath, fileName, Level.toLevel(logLevel.toString()), logPattern, maxBackupFiles, maxFileSize);
    }
    
    /**
     * Configures the logger with the given settings.
     *
     * @param logFolderPath
     * @param fileName
     * @param logLevel
     * @param logPattern
     * @param maxBackupFiles
     */
    public static void log4jConfigure(final String logFolderPath, final String fileName, final LogType logLevel, final String logPattern, final int maxBackupFiles) {
        log4jConfigure(logFolderPath, fileName, logLevel, logPattern, maxBackupFiles, MAX_FILE_SIZE);
    }
    
    /**
     * Configures the logger with the given settings.
     *
     * @param logFolderPath
     * @param fileName
     * @param logLevel
     * @param logPattern
     */
    public static void log4jConfigure(final String logFolderPath, final String fileName, final LogType logLevel, final String logPattern) {
        log4jConfigure(logFolderPath, fileName, logLevel, logPattern, MAX_BACKUP_FILES);
    }
    
    /**
     * Configures the logger with the given settings.
     *
     * @param logFolderPath
     * @param fileName
     * @param logLevel
     */
    public static void log4jConfigure(final String logFolderPath, final String fileName, final LogType logLevel) {
        log4jConfigure(logFolderPath, fileName, logLevel, LOG_PATTERN);
    }
    
    /**
     * Creates the logs in the given logFileName under the parentFolder.
     *
     * @param logFolderPath
     * @param logLevel
     */
    public static void log4jConfigure(final String logFolderPath, final LogType logLevel) {
        log4jConfigure(logFolderPath, LOG_FILE_NAME, logLevel);
    }
    
    /**
     * Creates the logs in the given logFolderPath under the /Android/data folder.
     *
     * @param logFolderPath
     */
    public static void log4jConfigure(final String logFolderPath) {
        log4jConfigure(logFolderPath, LogType.INFO);
    }
    
    /**
     * Returns the logger for this given className.
     *
     * @param className
     * @return
     */
    public static Logger getLogger(String className) {
        return Logger.getLogger(className);
    }
    
    /**
     * Returns the logger for this given class.
     *
     * @param logClass
     * @return
     */
    public static Logger getLogger(Class<?> logClass) {
        return Logger.getLogger(logClass);
    }
    
    /**
     * Returns true if the log-level is >= Level.INFO otherwise false.
     *
     * @return
     */
    public static boolean isLogEnabledForProduction() {
        return (sLog4jConfigurator.isLogEnabledFor(Level.INFO));
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
        if(isLogEnabledFor(LogType.WARN)) {
            if(isLog4JLogsEnabled()) {
                getLogger(logTag).warn(logMessage);
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
        if(isLogEnabledFor(LogType.INFO)) {
            if(isLog4JLogsEnabled()) {
                getLogger(logTag).info(logMessage);
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
        if(isLogEnabledFor(LogType.DEBUG)) {
            if(isLog4JLogsEnabled()) {
                getLogger(logTag).debug(logMessage);
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
        if(isLogEnabledFor(LogType.VERBOSE)) {
            if(isLog4JLogsEnabled()) {
                getLogger(logTag).debug(logMessage);
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
        if(isLogEnabledFor(LogType.ERROR)) {
            if(isLog4JLogsEnabled()) {
                getLogger(logTag).error(logMessage);
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
        if(isLogEnabledFor(LogType.ERROR)) {
            if(isLog4JLogsEnabled()) {
                getLogger(logTag).error(logMessage, throwable);
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
    @TargetApi(21)
    public static void logUri(final String logTag, final Uri uri) {
        if(isNotNull(uri) && !isNullOrEmpty(logTag)) {
            d(logTag, "urlString:" + uri.toString());
            d(logTag, "Scheme:" + uri.getScheme());
            d(logTag, "Host:" + uri.getHost());
            d(logTag, "QueryParameterNames:" + uri.getQueryParameterNames());
            d(logTag, "Query:" + uri.getQuery());
        }
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
        if(!isNullOrEmpty(logTag)) {
            d(logTag, "urlString:" + webRequest.getUrl().toString());
            d(logTag, "Method:" + webRequest.getMethod());
            d(logTag, "RequestHeaders:" + webRequest.getRequestHeaders());
            d(logTag, "QueryParameterNames:" + webRequest.getUrl().getQueryParameterNames());
            d(logTag, "Query:" + webRequest.getUrl().getQuery());
        }
    }
    
    /**
     * Returns the given pathString from assets folder and returns it's input stream.
     *
     * @param pathString
     * @return byte[]
     */
    public static InputStream readAssets(final Context mContext, final String pathString) {
        InputStream assetStream = null;
        if(isNotNull(mContext) && !isNullOrEmpty(pathString)) {
            try {
                assetStream = mContext.getAssets().open(pathString);
            } catch(IOException ex) {
                e(LOG_TAG, ex);
            }
        }
        
        return assetStream;
    }
    
    /**
     * Displays the given message as toast for short period of time.
     *
     * @param context
     * @param message
     */
    public static void showToastMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Lock and unlocks the orientation based on the given
     * <code>lockOrientation</code> parameter.
     *
     * @param parentActivity
     * @param lockOrientation
     */
    public static void lockUnlockOrientation(Activity parentActivity, boolean lockOrientation) {
        d(LOG_TAG, "lockUnlockOrientation(" + parentActivity + ", " + lockOrientation + ")");
        if(isNotNull(parentActivity)) {
            if(lockOrientation) {
                if(parentActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                } else {
                    parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            } else {
                parentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    }
    
}
