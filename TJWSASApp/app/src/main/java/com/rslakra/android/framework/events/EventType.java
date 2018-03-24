package com.rslakra.android.framework.events;

/**
 * TODO: Change/add/delete and make sense out of this enum.
 *
 * @author John, Ron
 * @author Rohtash Singh
 */
public enum EventType {
    UNKNOWN, ERROR, SERVICE_CONNECTED, SERVICE_DISCONNECTED, SERVER_STARTED, SERVER_STOPPED,;
    
    /**
     * Returns the EventType corresponding to an integer you acquired by calling
     * {@link #ordinal()}.
     *
     * @param eventType
     * @return
     */
    public static EventType toEventType(int eventType) {
        EventType mEventType = UNKNOWN;
        final EventType[] eventTypes = EventType.values();
        if(eventType >= 0 && eventType < eventTypes.length) {
            mEventType = eventTypes[eventType];
        }
        
        return mEventType;
    }
}
