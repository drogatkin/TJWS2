package com.rslakra.android.framework.events;

import android.os.Message;

/**
 * The Framework specific wrapper around an Android
 * Message object. Presumes we shoved our own data
 * into that Message using the static methods in {@link EventManager}.
 *
 * We're not using all the fields of a Message object
 * (currently ignoring the "arg2" and "data" fields).
 * We will probably use more of those fields as we
 * evolve this event system.
 */
public class AndroidEvent {
    
    private EventType type = EventType.UNKNOWN;
    private EventReason reason = EventReason.UNKNOWN;
    private Object payload = null;
    
    /**
     * @param mMessage
     */
    public AndroidEvent(final Message mMessage) {
        this.type = EventType.toEventType(mMessage.what);
        this.reason = EventReason.toEventReason(mMessage.arg1);
        this.payload = mMessage.obj;
    }
    
    /** The purpose of this event. */
    public EventType getType() {
        return type;
    }
    
    /**
     * Explanation for why the event was sent. Allows more
     * granular information than just a "type." For example,
     * the "logout" event happens for several possible
     * reasons.
     */
    public EventReason getReason() {
        return reason;
    }
    
    /**
     * Binary data sent along with the event.
     *
     * @return
     */
    public Object getPayload() {
        return payload;
    }
}
