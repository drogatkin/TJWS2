package com.rslakra.mobile.logger;

import android.util.Log;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * An android log4j appender.
 *
 * @author Rohtash Singh Lakra (Rohtash.Lakra@nasaq.com)
 * @version 1.0.0
 * @since 05/30/2017 11:23:09 AM
 */
public final class AndroidAppender extends AppenderSkeleton {
	
	/* DEFAULT_TAG_LAYOUT */
	private static final String DEFAULT_TAG_LAYOUT = "%c";
	
	/* tagLayout */
	private Layout tagLayout;
	
	/**
	 * @param messageLayout
	 * @param tagLayout
	 */
	public AndroidAppender(final Layout messageLayout, final Layout tagLayout) {
		this.tagLayout = tagLayout;
		setLayout(messageLayout);
	}
	
	/**
	 * @param messageLayout
	 */
	public AndroidAppender(final Layout messageLayout) {
		this(messageLayout, new PatternLayout(DEFAULT_TAG_LAYOUT));
	}
	
	/**
	 *
	 */
	public AndroidAppender() {
		this(new PatternLayout(AndroidConfigurator.DEFAULT_LOG_PATTERN));
	}
	
	/**
	 * @param logEvent
	 */
	@Override
	protected void append(final LoggingEvent logEvent) {
		switch(logEvent.getLevel().toInt()) {
			case Level.TRACE_INT:
				if(logEvent.getThrowableInformation() != null) {
					Log.v(getTagLayout().format(logEvent), getLayout().format(logEvent), logEvent.getThrowableInformation().getThrowable());
				} else {
					Log.v(getTagLayout().format(logEvent), getLayout().format(logEvent));
				}
				break;
			case Level.DEBUG_INT:
				if(logEvent.getThrowableInformation() != null) {
					Log.d(getTagLayout().format(logEvent), getLayout().format(logEvent), logEvent.getThrowableInformation().getThrowable());
				} else {
					Log.d(getTagLayout().format(logEvent), getLayout().format(logEvent));
				}
				break;
			case Level.INFO_INT:
				if(logEvent.getThrowableInformation() != null) {
					Log.i(getTagLayout().format(logEvent), getLayout().format(logEvent), logEvent.getThrowableInformation().getThrowable());
				} else {
					Log.i(getTagLayout().format(logEvent), getLayout().format(logEvent));
				}
				break;
			case Level.WARN_INT:
				if(logEvent.getThrowableInformation() != null) {
					Log.w(getTagLayout().format(logEvent), getLayout().format(logEvent), logEvent.getThrowableInformation().getThrowable());
				} else {
					Log.w(getTagLayout().format(logEvent), getLayout().format(logEvent));
				}
				break;
			case Level.ERROR_INT:
				if(logEvent.getThrowableInformation() != null) {
					Log.e(getTagLayout().format(logEvent), getLayout().format(logEvent), logEvent.getThrowableInformation().getThrowable());
				} else {
					Log.e(getTagLayout().format(logEvent), getLayout().format(logEvent));
				}
				break;
			case Level.FATAL_INT:
				if(logEvent.getThrowableInformation() != null) {
					Log.wtf(getTagLayout().format(logEvent), getLayout().format(logEvent), logEvent.getThrowableInformation().getThrowable());
				} else {
					Log.wtf(getTagLayout().format(logEvent), getLayout().format(logEvent));
				}
				break;
		}
	}
	
	/**
	 *
	 */
	@Override
	public void close() {
	}
	
	/**
	 * @return
	 */
	@Override
	public boolean requiresLayout() {
		return true;
	}
	
	/**
	 * Returns the tagLayout.
	 *
	 * @return
	 */
	public Layout getTagLayout() {
		return tagLayout;
	}
	
	/**
	 * The tagLayout to be set.
	 *
	 * @param tagLayout
	 */
	public void setTagLayout(final Layout tagLayout) {
		this.tagLayout = tagLayout;
	}
}