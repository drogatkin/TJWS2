package com.rslakra.mobile.logger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import Acme.IOHelper;

/**
 * This class handles the logs for LogCat as well as file system logs.
 * <p>
 * The logger can also be used as: <code>
 * private static Logger log = AndroidLogger.getLogger(AndroidLogger.class);
 * log.LogHelper("Logging Message Here");
 * </code>
 *
 * @author Rohtash Singh (rsingh@boardvantage.com)
 * @version 1.0.0
 * @since Nov 16, 2015 12:12:22 PM
 */
public final class AndroidLogger {
    
	/* Log4j Log Levels: ALL, TRACE, LogHelper, INFO, WARN, ERROR, FATAL, OFF */
    
    // Default logging configurations.
    private final static String LOG_FILE_NAME = "androidLogger.log";
    
    // Default logging configurations.
    private final static int MAX_BACKUP_FILES = 3;
    
    /* 5 MB */
    private final static long MAX_FILE_SIZE = 1024 * 1024 * 5;
    private final static String LOG_PATTERN = "[%d{yyyy-MM-dd HH:mm:ss.S zzz}] %5p [%c{1}(%L)] - %m%n";
    
    /* log4jConfigurator */
    private final static AndroidConfigurator sLog4jConfigurator = new AndroidConfigurator();
    
    // Singleton object
    private AndroidLogger() {
        throw new UnsupportedOperationException("Object creation is not allowed!");
    }
    
    /**
     * Configures the logger with the given settings.
     *
     * @param fileName
     * @param filePattern
     * @param maxBackupFiles
     * @param maxFileSize
     * @param logLevel
     */
    public static void log4jConfigure(String fileName, String filePattern, int maxBackupFiles, long maxFileSize, Level logLevel) {
        if(logLevel == null) {
            throw new NullPointerException("The root logLevel should not be NULL!");
        }
        sLog4jConfigurator.setRootLevel(logLevel);
        
        // set the name of the log file
        sLog4jConfigurator.setFileName(fileName);
        // set output format of the log line
        sLog4jConfigurator.setFilePattern(filePattern);
        // Maximum number of backed up log files
        sLog4jConfigurator.setMaxBackupSize(maxBackupFiles);
        // Maximum size of log file until rolling
        sLog4jConfigurator.setMaxFileSize(maxFileSize);
        // configure
        sLog4jConfigurator.configure();
    }
    
    /**
     * Configures the logger with the given settings.
     *
     * @param fileName
     * @param filePattern
     * @param maxBackupFiles
     * @param maxFileSize
     */
    public static void log4jConfigure(String fileName, String filePattern, int maxBackupFiles, long maxFileSize) {
        log4jConfigure(fileName, filePattern, maxBackupFiles, maxFileSize, Level.toLevel(LogHelper.getLogType().toString()));
    }
    
    /**
     * Creates the logs in the given logFileName under the parentFolder.
     *
     * @param parentFolder
     * @param logFileName
     */
    public static void log4jConfigure(final String parentFolder, final String logFileName) {
        if(IOHelper.isNullOrEmpty(parentFolder)) {
            throw new IllegalArgumentException("Parent folder is either NULL or EMPTY!");
        }
        
        String logFilePath = IOHelper.pathString(parentFolder, logFileName);
        AndroidLogger.log4jConfigure(logFilePath, LOG_PATTERN, MAX_BACKUP_FILES, MAX_FILE_SIZE);
    }
    
    /**
     * Creates the logs in the given logFolderPath under the /Android/data folder.
     *
     * @param logFolderPath
     */
    public static void log4jConfigure(final String logFolderPath) {
        log4jConfigure(logFolderPath, LOG_FILE_NAME);
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
     * @param klass
     * @return
     */
    public static Logger getLogger(Class<?> klass) {
        return Logger.getLogger(klass);
    }
    
    /**
     * Returns true if the log-level is >= Level.INFO otherwise false.
     *
     * @return
     */
    public static boolean isProductionLogLevel() {
        return (sLog4jConfigurator.getRootLevel().toInt() >= Level.INFO.toInt());
    }
    
}
