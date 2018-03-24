package com.rslakra.android.logger;

import android.util.Log;

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
 * @author Rohtash Singh Lakra
 * @version 1.0.0
 * @since 05/30/2017 11:23:09 AM
 */
public final class AndroidConfigurator {
    
    /**
     * LOG_TAG
     */
    public static final String LOG_TAG = "AndroidConfigurator".intern();
    
    /** mRootLogger */
    private Logger mRootLogger;
    private Level mLogLevel = Level.INFO;
    private String mLogPattern;
    private String mLogsFolder;
    private String mFileName;
    private int mMaxBackupFiles;
    private long mMaxFileSize;
    private boolean mImmediateFlush = true;
    private boolean mUseFileAppender = true;
    private boolean mUseAndroidAppender = true;
    private boolean mResetConfiguration = true;
    private boolean mInternalLogging = false;
    
    
    /**
     * Default Constructor.
     */
    public AndroidConfigurator() {
    }
    
    /**
     * Returns the root logger.
     *
     * @return
     */
    private final Logger getRootLogger() {
        if(mRootLogger == null) {
            mRootLogger = Logger.getRootLogger();
        }
        
        return mRootLogger;
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
     * Returns true if the current logLevel is >= the given logLevel otherwise false.
     *
     * @param logLevel
     * @return
     */
    public boolean isLogEnabledFor(final Level logLevel) {
        return (logLevel != null && getLogLevel().toInt() >= logLevel.toInt());
    }
    
    /**
     * Configures the file appender.
     */
    private void configureFileAppender() {
        final RollingFileAppender rollingFileAppender;
        
        try {
            final File mFile = new File(getLogFilePath());
            /** Check logs file exists or not. */
            if(!mFile.exists()) {
                /** Create logs folder, if it does not exist. */
                if(!mFile.getParentFile().exists()) {
                    if(!mFile.getParentFile().mkdirs()) {
                        Log.w(LOG_TAG, "Unable to create parent logs folder:" + mFile.getParentFile().getAbsolutePath());
                    }
                }
                
                /** Create log file, if it does not exist. */
                if(!mFile.createNewFile()) {
                    Log.w(LOG_TAG, "Unable to create logs file:" + mFile.getAbsolutePath());
                }
            }
            rollingFileAppender = new RollingFileAppender(new PatternLayout(getLogPattern()), getLogFilePath());
        } catch(final IOException ex) {
            Log.e(LOG_TAG, ex.getLocalizedMessage(), ex);
            throw new RuntimeException("Error while configuring system logger!", ex);
        }
        
        rollingFileAppender.setMaxBackupIndex(getMaxBackupFiles());
        rollingFileAppender.setMaximumFileSize(getMaxFileSize());
        rollingFileAppender.setImmediateFlush(isImmediateFlush());
        
        /** set file appender to root logger. */
        getRootLogger().addAppender(rollingFileAppender);
    }
    
    /**
     * Configures the logger for file appender and android.
     */
    public void configure() {
        if(isResetConfiguration()) {
            LogManager.getLoggerRepository().resetConfiguration();
        }
        
        LogLog.setInternalDebugging(isInternalLogging());
        
        if(isUseAndroidAppender()) {
            getRootLogger().addAppender(new AndroidAppender(new PatternLayout(getLogPattern())));
        }
        
        if(isUseFileAppender()) {
            configureFileAppender();
        }
        
        getRootLogger().setLevel(getLogLevel());
    }
    
    /**
     * @param logsFolder
     */
    public void configure(final String logsFolder) {
        // set the the log folder
        if(LogHelper.isNullOrEmpty(logsFolder)) {
            throw new IllegalArgumentException("logsFolder is either NULL or EMPTY!");
        }
        setLogsFolder(logsFolder);
        configure();
    }
    
    /**
     * @param logsFolder
     * @param fileName Name of the log file
     */
    public void configure(final String logsFolder, final String fileName) {
        // set the the log file name
        if(LogHelper.isNullOrEmpty(fileName)) {
            throw new IllegalArgumentException("fileName is either NULL or EMPTY!");
        }
        setFileName(fileName);
        configure(logsFolder);
    }
    
    /**
     * @param logsFolder
     * @param fileName Name of the log file
     * @param logLevel Log level for the root logger
     */
    public void configure(final String logsFolder, final String fileName, final Level logLevel) {
        // set the the log level
        if(logLevel == null) {
            throw new NullPointerException("The root logLevel should not be NULL!");
        }
        setLogLevel(logLevel);
        configure(logsFolder, fileName);
    }
    
    
    /**
     * @param logsFolder
     * @param fileName Name of the log file
     * @param logLevel Log level for the root logger
     * @param logPattern Log pattern for the file appender
     */
    public void configure(final String logsFolder, final String fileName, final Level logLevel, final String logPattern) {
        setLogPattern(logPattern);
        configure(logsFolder, fileName, logLevel);
    }
    
    /**
     * @param logsFolder
     * @param fileName Name of the log file
     * @param logLevel Log level for the root logger
     * @param logPattern Log pattern for the file appender
     * @param maxBackupFiles Maximum number of backed up log files
     */
    public void configure(final String logsFolder, final String fileName, final Level logLevel, final String logPattern, final int maxBackupFiles) {
        setMaxBackupFiles(maxBackupFiles);
        configure(logsFolder, fileName, logLevel, logPattern);
    }
    
    /**
     * @param logsFolder
     * @param fileName Name of the log file
     * @param logLevel Log level for the root logger
     * @param logPattern Log pattern for the file appender
     * @param maxBackupFiles Maximum number of backed up log files
     * @param maxFileSize Maximum size of log file until rolling
     */
    public void configure(final String logsFolder, final String fileName, final Level logLevel, final String logPattern, final int maxBackupFiles, final long maxFileSize) {
        setMaxFileSize(maxFileSize);
        configure(logsFolder, fileName, logLevel, logPattern, maxBackupFiles);
    }
    
    /**
     * Return the log level of the root logger
     *
     * @return Log level of the root logger
     */
    public Level getLogLevel() {
        return mLogLevel;
    }
    
    /**
     * Sets log level for the root logger
     *
     * @param logLevel Log level for the root logger
     */
    public void setLogLevel(final Level logLevel) {
        this.mLogLevel = logLevel;
    }
    
    /**
     * Returns the log pattern.
     *
     * @return
     */
    public String getLogPattern() {
        return mLogPattern;
    }
    
    /**
     * The logPattern to be set.
     *
     * @param logPattern
     */
    public void setLogPattern(final String logPattern) {
        this.mLogPattern = logPattern;
    }
    
    /**
     * Returns the path of the logsFolder.
     *
     * @return
     */
    public String getLogsFolder() {
        return mLogsFolder;
    }
    
    /**
     * Returns the log file path.
     *
     * @return
     */
    private String getLogFilePath() {
        return LogHelper.pathString(getLogsFolder(), getFileName());
    }
    
    /**
     * Sets the name of the log file
     *
     * @param logsFolder path of the logs folder.
     */
    public void setLogsFolder(final String logsFolder) {
        this.mLogsFolder = logsFolder;
    }
    
    /**
     * Returns the name of the log file
     *
     * @return the name of the log file
     */
    public String getFileName() {
        return mFileName;
    }
    
    /**
     * Sets the name of the log file
     *
     * @param fileName Name of the log file
     */
    public void setFileName(final String fileName) {
        this.mFileName = fileName;
    }
    
    /**
     * Returns the maximum number of backed up log files
     *
     * @return Maximum number of backed up log files
     */
    public int getMaxBackupFiles() {
        return mMaxBackupFiles;
    }
    
    /**
     * Sets the maximum number of backed up log files
     *
     * @param maxBackupFiles Maximum number of backed up log files
     */
    public void setMaxBackupFiles(final int maxBackupFiles) {
        this.mMaxBackupFiles = maxBackupFiles;
    }
    
    /**
     * Returns the maximum size of log file until rolling
     *
     * @return Maximum size of log file until rolling
     */
    public long getMaxFileSize() {
        return mMaxFileSize;
    }
    
    /**
     * Sets the maximum size of log file until rolling
     *
     * @param maxFileSize Maximum size of log file until rolling
     */
    public void setMaxFileSize(final long maxFileSize) {
        this.mMaxFileSize = maxFileSize;
    }
    
    /**
     * Returns the immediateFlush.
     *
     * @return
     */
    public boolean isImmediateFlush() {
        return mImmediateFlush;
    }
    
    /**
     * The immediateFlush to be set.
     *
     * @param immediateFlush
     */
    public void setImmediateFlush(final boolean immediateFlush) {
        this.mImmediateFlush = immediateFlush;
    }
    
    /**
     * Returns true, if FileAppender is used for logging
     *
     * @return True, if FileAppender is used for logging
     */
    public boolean isUseFileAppender() {
        return mUseFileAppender;
    }
    
    /**
     * The useFileAppender to be set.
     *
     * @param useFileAppender
     */
    public void setUseFileAppender(final boolean useFileAppender) {
        this.mUseFileAppender = useFileAppender;
    }
    
    /**
     * Returns true, if LogcatAppender should be used
     *
     * @return True, if LogcatAppender should be used
     */
    public boolean isUseAndroidAppender() {
        return mUseAndroidAppender;
    }
    
    /**
     * The useAndroidAppender to be set.
     *
     * @param useAndroidAppender If true, LogCatAppender will be used for
     * logging
     */
    public void setUseAndroidAppender(final boolean useAndroidAppender) {
        this.mUseAndroidAppender = useAndroidAppender;
    }
    
    /**
     * Resets the log4j configuration before applying this configuration.
     * Default is true.
     *
     * @return True, if the log4j configuration should be reset before applying
     * this configuration.
     */
    public boolean isResetConfiguration() {
        return mResetConfiguration;
    }
    
    /**
     * The resetConfiguration to be set.
     *
     * @param resetConfiguration
     */
    public void setResetConfiguration(final boolean resetConfiguration) {
        this.mResetConfiguration = resetConfiguration;
    }
    
    /**
     * Returns the internalLogging.
     *
     * @return
     */
    public boolean isInternalLogging() {
        return mInternalLogging;
    }
    
    /**
     * The internalLogging to be set.
     *
     * @param internalLogging
     */
    public void setInternalLogging(final boolean internalLogging) {
        this.mInternalLogging = internalLogging;
    }
}