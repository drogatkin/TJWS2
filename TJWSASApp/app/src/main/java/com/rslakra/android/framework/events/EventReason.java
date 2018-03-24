package com.rslakra.android.framework.events;

/**
 * @author Rohtash Singh
 * @version 1.0.0
 * @since Sep 21, 2015 12:54:22 PM
 */
public enum EventReason {
    
    UNKNOWN, SERVICE_STARTED, SERVICE_STOPPPED, LOCAL_SERVER_STARTED, LOCAL_SERVER_STOPPED,;
    
    /**
     * Returns the EventReason corresponding to an integer you acquired by
     * calling {@link #ordinal()}.
     *
     * @param eventReason
     * @return
     */
    public static EventReason toEventReason(final int eventReason) {
        EventReason mEventReason = UNKNOWN;
        EventReason[] eventReasons = EventReason.values();
        
        if(eventReason >= 0 && eventReason < eventReasons.length) {
            mEventReason = eventReasons[eventReason];
        }
        
        return mEventReason;
    }
}
