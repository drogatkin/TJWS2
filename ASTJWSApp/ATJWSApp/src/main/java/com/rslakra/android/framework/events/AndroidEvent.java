// Copyright (C)2018 by Rohtash Singh Lakra <rohtash.singh@gmail.com>.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the https://github.com/rslakra/TJWS2 page for up-to-date versions of
// this and other fine Java utilities.
//
// All enhancements Copyright (C)2018 by Rohtash Singh Lakra
// This version is compatible with JSDK 2.5
// https://github.com/rslakra/TJWS2
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
