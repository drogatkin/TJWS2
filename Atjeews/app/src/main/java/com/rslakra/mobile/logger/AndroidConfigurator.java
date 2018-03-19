package com.rslakra.mobile.logger;

import android.util.Log;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.LogLog;

import java.io.File;
import java.io.IOException;

/**
 * This class handles the log4j configuration for Android.
 * <p>
 * Configures the Log4j logging framework.
 * See <a href=
 * "http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html">Patterns</a>
 * for pattern layout.
 *
 * @author Rohtash Singh Lakra (Rohtash.Lakra@nasaq.com)
 * @version 1.0.0
 * @since 05/30/2017 11:23:09 AM
 */
public final class AndroidConfigurator {
    
    /** DEFAULT_LOG_FILE_NAME */
    public static final String DEFAULT_LOG_FILE_NAME = "log4JAndroid.log";
    /** DEFAULT_LOG_PATTERN */
    public static final String DEFAULT_LOG_PATTERN = "%d - [%p::%c::%C] - %m%n";
    /* maximum 3 files */
    public final static int MAX_BACKUP_FILES = 3;
    /* 5 MB */
    public final static long MAX_FILE_SIZE = 1024 * 1024 * 5;
    
    private Level rootLevel = Level.INFO;
    private String filePattern = DEFAULT_LOG_PATTERN;
    private String fileName = DEFAULT_LOG_FILE_NAME;
    private int maxBackupSize = MAX_BACKUP_FILES;
    private long maxFileSize = MAX_FILE_SIZE;
    private boolean immediateFlush = true;
    private boolean useFileAppender = true;
    private boolean useAndroidAppender = true;
    private boolean resetConfiguration = true;
    private boolean internalLogHelperging = false;
    
    /**
     *
     */
    public AndroidConfigurator() {
    }
    
    /**
     * @param fileName Name of the log file
     */
    public AndroidConfigurator(final String fileName) {
        setFileName(fileName);
    }
    
    /**
     * @param fileName Name of the log file
     * @param rootLevel Log level for the root logger
     */
    public AndroidConfigurator(final String fileName, final Level rootLevel) {
        this(fileName);
        setRootLevel(rootLevel);
    }
    
    /**
     * @param fileName Name of the log file
     * @param rootLevel Log level for the root logger
     * @param filePattern Log pattern for the file appender
     */
    public AndroidConfigurator(final String fileName, final Level rootLevel, final String filePattern) {
        this(fileName, rootLevel);
        setFilePattern(filePattern);
    }
    
    /**
     * @param fileName Name of the log file
     * @param maxBackupSize Maximum number of backed up log files
     * @param maxFileSize Maximum size of log file until rolling
     * @param filePattern Log pattern for the file appender
     * @param rootLevel Log level for the root logger
     */
    public AndroidConfigurator(final String fileName, final int maxBackupSize, final long maxFileSize, final String filePattern, final Level rootLevel) {
        this(fileName, rootLevel, filePattern);
        setMaxBackupSize(maxBackupSize);
        setMaxFileSize(maxFileSize);
    }
    
    /**
     * Configures the logger for file appender and android.
     */
    public void configure() {
        final Logger root = Logger.getRootLogger();
        
        if(isResetConfiguration()) {
            LogManager.getLoggerRepository().resetConfiguration();
        }
        
        LogLog.setInternalDebugging(isInternalLogHelperging());
        
        if(isUseAndroidAppender()) {
            configureAndroidAppender();
        }
        
        if(isUseFileAppender()) {
            configureFileAppender();
        }
        
        root.setLevel(getRootLevel());
    }
    
    /**
     * Sets the level of logger with name <code>loggerName</code>.
     * Corresponds to log4j.properties
     * <code>log4j.logger.org.apache.what.ever=ERROR</code>
     *
     * @param loggerName
     * @param level
     */
    public void setLevel(final String loggerName, final Level level) {
        Logger.getLogger(loggerName).setLevel(level);
    }
    
    /**
     * Configures the file appender.
     */
    private void configureFileAppender() {
        final Logger root = Logger.getRootLogger();
        final Layout patternLayout = new PatternLayout(getFilePattern());
        final RollingFileAppender rollingFileAppender;
        
        try {
            final File mFile = new File(getFileName());
            if(!mFile.exists()) {
                mFile.createNewFile();
            }
            rollingFileAppender = new RollingFileAppender(patternLayout, getFileName());
        } catch(final IOException ex) {
            Log.e(this.getClass().getSimpleName(), ex.getLocalizedMessage(), ex);
            throw new RuntimeException("Exception configuring system logger!", ex);
        }
        
        rollingFileAppender.setMaxBackupIndex(getMaxBackupSize());
        rollingFileAppender.setMaximumFileSize(getMaxFileSize());
        rollingFileAppender.setImmediateFlush(isImmediateFlush());
        
        root.addAppender(rollingFileAppender);
    }
    
    /**
     * Configures an android appender.
     */
    private void configureAndroidAppender() {
        final Logger root = Logger.getRootLogger();
        final Layout patternLayout = new PatternLayout(getFilePattern());
        root.addAppender(new AndroidAppender(patternLayout));
    }
    
    /**
     * Return the log level of the root logger
     *
     * @return Log level of the root logger
     */
    public Level getRootLevel() {
        return rootLevel;
    }
    
    /**
     * Sets log level for the root logger
     *
     * @param level Log level for the root logger
     */
    public void setRootLevel(final Level level) {
        this.rootLevel = level;
    }
    
    /**
     * @return
     */
    public String getFilePattern() {
        return filePattern;
    }
    
    /**
     * @param filePattern
     */
    public void setFilePattern(final String filePattern) {
        this.filePattern = filePattern;
    }
    
    /**
     * Returns the name of the log file
     *
     * @return the name of the log file
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Sets the name of the log file
     *
     * @param fileName Name of the log file
     */
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }
    
    /**
     * Returns the maximum number of backed up log files
     *
     * @return Maximum number of backed up log files
     */
    public int getMaxBackupSize() {
        return maxBackupSize;
    }
    
    /**
     * Sets the maximum number of backed up log files
     *
     * @param maxBackupSize Maximum number of backed up log files
     */
    public void setMaxBackupSize(final int maxBackupSize) {
        this.maxBackupSize = maxBackupSize;
    }
    
    /**
     * Returns the maximum size of log file until rolling
     *
     * @return Maximum size of log file until rolling
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    /**
     * Sets the maximum size of log file until rolling
     *
     * @param maxFileSize Maximum size of log file until rolling
     */
    public void setMaxFileSize(final long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    /**
     * Returns the immediateFlush.
     *
     * @return
     */
    public boolean isImmediateFlush() {
        return immediateFlush;
    }
    
    /**
     * The immediateFlush to be set.
     *
     * @param immediateFlush
     */
    public void setImmediateFlush(final boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }
    
    /**
     * Returns true, if FileAppender is used for logging
     *
     * @return True, if FileAppender is used for logging
     */
    public boolean isUseFileAppender() {
        return useFileAppender;
    }
    
    /**
     * @param useFileAppender the useFileAppender to set
     */
    public void setUseFileAppender(final boolean useFileAppender) {
        this.useFileAppender = useFileAppender;
    }
    
    /**
     * Returns true, if LogcatAppender should be used
     *
     * @return True, if LogcatAppender should be used
     */
    public boolean isUseAndroidAppender() {
        return useAndroidAppender;
    }
    
    /**
     * The useAndroidAppender to be set.
     *
     * @param useAndroidAppender If true, LogCatAppender will be used for
     * logging
     */
    public void setUseAndroidAppender(final boolean useAndroidAppender) {
        this.useAndroidAppender = useAndroidAppender;
    }
    
    /**
     * Resets the log4j configuration before applying this configuration.
     * Default is true.
     *
     * @return True, if the log4j configuration should be reset before applying
     * this configuration.
     */
    public boolean isResetConfiguration() {
        return resetConfiguration;
    }
    
    /**
     * @param resetConfiguration
     */
    public void setResetConfiguration(boolean resetConfiguration) {
        this.resetConfiguration = resetConfiguration;
    }
    
    /**
     * Returns the InternalLogHelperging.
     *
     * @return
     */
    public boolean isInternalLogHelperging() {
        return internalLogHelperging;
    }
    
    /**
     * The InternalLogHelperging to be set.
     *
     * @param internalLogHelperging
     */
    public void setInternalLogHelperging(boolean internalLogHelperging) {
        this.internalLogHelperging = internalLogHelperging;
    }
    
}