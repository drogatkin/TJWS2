package com.rslakra.android.framework.events;

import java.io.NotActiveException;

/**
 *
 */
public class InActiveEventHandlerException extends NotActiveException {
    private static final long serialVersionUID = 4536567900065477325L;
    
    public InActiveEventHandlerException() {
        super("Event Manager is not running. Restart it!");
    }
}
