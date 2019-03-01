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
