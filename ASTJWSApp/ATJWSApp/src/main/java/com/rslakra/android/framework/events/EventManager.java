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

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.rslakra.android.logger.LogHelper;

import java.util.HashMap;
import java.util.HashSet;

/*
 * class EventManager:
 * This class handles all events sending and receiving across all components/activities in the application.
 * This is a singleton, available to the whole app.
 * The events receiving thread starts when the object is first created (in the constructor).
 * Typically the activities call getEventManager() and use the EventManager object to 
 * 	send events and subscribe and unsubscribe events.
 * 
 * Make sure to call shutdown() before the application exits for clean shutdown.
 */
public class EventManager {
    
    /** LOG_TAG */
    private static final String LOG_TAG = "EventManager";
    /** NO_EVENT */
    private static final int NO_EVENT = -1;
    
    // Note that this declaration must come last; it uses the objects above.
    private static EventManager sInstance;
    
    /** mLock */
    private final Object mLock = new Object();
    /** mEventHandlerLooper */
    private Looper mEventHandlerLooper;
    /** mEventHandler */
    private EventHandler mEventHandler;
    /** mEventHandlerThread */
    private HandlerThread mEventHandlerThread;
    /** mSubscribers */
    private final HashMap<EventType, HashSet<EventListener>> mSubscribers = new HashMap<EventType, HashSet<EventListener>>();
    /** mEventTypes */
    private final EventType[] mEventTypes = EventType.values();
    
    /**
     * Singleton
     */
    private EventManager() {
        this.start();
    }
    
    /**
     * @return
     */
    public static EventManager getInstance() {
        if(sInstance == null) {
            synchronized(EventManager.class) {
                if(sInstance == null) {
                    sInstance = new EventManager();
                }
            }
        }
        
        return sInstance;
    }
    
    /**
     * @return
     */
    private boolean isStarted() {
        return (mEventHandlerLooper != null);
    }
    
    /**
     *
     */
    public void start() {
        synchronized(mLock) {
            // Start only if it was not started before.
            if(!isStarted()) {
                initSubscribers(false);
                mEventHandlerThread = new HandlerThread("EventHandlerThread", Process.THREAD_PRIORITY_BACKGROUND);
                mEventHandlerThread.start();
                mEventHandlerLooper = mEventHandlerThread.getLooper();
                mEventHandler = new EventHandler(mEventHandlerLooper);
            }
        }
    }
    
    
    // Call shutdown when exiting the application.
    public void shutdown() throws InActiveEventHandlerException {
        synchronized(mLock) {
            if(!isStarted()) {
                throw new InActiveEventHandlerException();
            }
            if(mEventHandlerLooper != null) {
                mEventHandlerLooper.quit();
            }
            mEventHandlerThread = null;
            mEventHandler = null;
            mEventHandlerLooper = null;
            initSubscribers(true);
        }
    }
    
    
    /**
     * @param unInitialize
     */
    private void initSubscribers(final boolean unInitialize) {
        if(unInitialize) {
            mSubscribers.clear();
        } else {
            for(EventType eventType : mEventTypes) {
                mSubscribers.put(eventType, new HashSet<EventListener>());
            }
        }
    }
    
    // ----------------------------------------------------------------
    // Boardcast Events
    // ----------------------------------------------------------------
    
    /**
     * Broadcasts an event with the specified Type. <p/>
     *
     * Note:  Android's underlying Message object allows us to send more
     * parameters for "free," and we can certainly embed more into
     * the Object (or a Bundle).  We're evolving this system as we
     * need it.
     */
    public static void sendEvent(EventType type) {
        getInstance().sendEvent(type, EventReason.UNKNOWN, null, null);
    }
    
    /**
     * Broadcasts an event with the specified Type and Reason. <p/>
     *
     * Note:  Android's underlying Message object allows us to send more
     * parameters for "free," and we can certainly embed more into
     * the Object (or a Bundle).  We're evolving this system as we
     * need it.
     */
    public static void sendEvent(EventType type, EventReason reason) {
        getInstance().sendEvent(type, reason, null, null);
    }
    
    /**
     * Broadcasts an event with the specified Type and payload. <p/>
     *
     * Note:  Android's underlying Message object allows us to send more
     * parameters for "free," and we can certainly embed more into
     * the Object (or a Bundle).  We're evolving this system as we
     * need it.
     */
    public static void sendEvent(EventType type, Object payload) {
        getInstance().sendEvent(type, EventReason.UNKNOWN, payload, null);
    }
    
    /**
     * Broadcasts an event with the specified Type, Reason, and payload. <p/>
     *
     * Note:  Android's underlying Message object allows us to send more
     * parameters for "free," and we can certainly embed more into
     * the Object (or a Bundle).  We're evolving this system as we
     * need it.
     */
    public static void sendEvent(EventType type, EventReason reason, Object payload) {
        getInstance().sendEvent(type, reason, payload, null);
    }
    
    /**
     * Internal method that translates from "our language" (types,
     * reasons, etc.) to Android's built-in "message" language
     * (raw integers, an object, and a bundle).
     */
    private void sendEvent(EventType type, EventReason reason, Object objectPayload, Bundle bundlePayload) {
        this.sendEvent(type.ordinal(), reason.ordinal(), NO_EVENT, objectPayload, bundlePayload);
    }
    
    /**
     * This method illustrates the superset of all parameters we can
     * send through an Android Message:  three integers, an Object,
     * and a Bundle.  We'll create wrapper methods as we need them.
     */
    private void sendEvent(int what, int arg1, int arg2, Object obj, Bundle bundle) {
        synchronized(mLock) {
            if(isStarted()) {
                Message msg = mEventHandler.obtainMessage(what, arg1, arg2, obj);
                msg.setData(bundle);
                mEventHandler.sendMessage(msg);
            } else {
                LogHelper.e(LOG_TAG, new InActiveEventHandlerException());
            }
        }
    }
    
    
    // ----------------------------------------------------------------
    // Subscribe and Unsubscribe
    // ----------------------------------------------------------------
    
    /**
     * Subscribe to one or more events.  For example:
     *
     * <pre>
     *   EventManager.subscribe (this, EventType.Logout);
     *   EventManager.subscribe (this, EventType.Online, EventType.Offline, EventType.OpenDocument);
     * </pre>
     *
     * @param listener
     * @param types
     */
    public static void subscribe(EventListener listener, EventType... types) {
        for(EventType type : types) {
            try {
                getInstance().registerSubscriber(type, listener);
            } catch(InActiveEventHandlerException e) {
                LogHelper.e(LOG_TAG, e);
            }
        }
    }
    
    /**
     * Unsubscribe from one or more events.  For example:
     *
     * <pre>
     *   EventManager.unsubscribe (this, EventType.Logout);
     *   EventManager.unsubscribe (this, EventType.Online, EventType.Offline,
     * EventType.OpenDocument);
     * </pre>
     *
     * @param listener
     * @param types
     */
    public static void unsubscribe(EventListener listener, EventType... types) {
        for(EventType type : types) {
            try {
                getInstance().unregisterSubscriber(type, listener);
            } catch(InActiveEventHandlerException e) {
                LogHelper.e(LOG_TAG, e);
            }
        }
    }
    
    
    /**
     * Register an event subscriber.
     *
     * @param eventType
     * @param eventListener
     * @return
     * @throws InActiveEventHandlerException
     */
    private boolean registerSubscriber(EventType eventType, EventListener eventListener) throws InActiveEventHandlerException {
        boolean subscriberRegistered = false;
        synchronized(mLock) {
            if(!isStarted()) {
                throw new InActiveEventHandlerException();
            }
            
            HashSet<EventListener> eventListeners = mSubscribers.get(eventType);
            if(eventListeners != null) {
                synchronized(eventListeners) {
                    subscriberRegistered = eventListeners.add(eventListener);
                }
            }
        }
        
        return subscriberRegistered;
    }
    
    
    /**
     * Unregister an event subscriber.
     *
     * @param eventType
     * @param eventListener
     * @return
     * @throws InActiveEventHandlerException
     */
    private boolean unregisterSubscriber(EventType eventType, EventListener eventListener) throws InActiveEventHandlerException {
        boolean subscriberUnRegistered = false;
        synchronized(mLock) {
            if(!isStarted()) {
                throw new InActiveEventHandlerException();
            }
            
            HashSet<EventListener> eventListeners = mSubscribers.get(eventType);
            if(eventListeners != null) {
                synchronized(eventListeners) {
                    subscriberUnRegistered = eventListeners.remove(eventListener);
                }
            }
        }
        
        return subscriberUnRegistered;
    }
    
    // ----------------------------------------------------------------
    // Distributing Events
    // ----------------------------------------------------------------
    
    // Event handler class
    private final class EventHandler extends Handler {
        
        /**
         * @param looper
         */
        public EventHandler(Looper looper) {
            super(looper);
        }
        
        // TODO:	Message object is derived from Parcelable class (which is used for interprocess communication).
        //			Although I still believe that event sending/handling happens within our processes' address space,
        //			research more and make sure it is the case. If there is any potential danger, then re-write the
        //			message handling part.
        @Override
        public void handleMessage(final Message mMessage) {
            if(mMessage.what >= 0 && mMessage.what < mSubscribers.size()) {
                final HashSet<EventListener> mEventListeners = mSubscribers.get(mEventTypes[mMessage.what]);
                if(mEventListeners != null) {
                    synchronized(mEventListeners) {
                    
						/*
                         *  Publish event to all subscribers.
						 *  TODO:  Let's keep an eye on this (algorithm for publishing events).  Two issues:
						 *  
						 *  - Each message is sent sequentially, on the same thread.
						 *  So if any one of the subscribers hangs or crashes, this
						 *  method will do the same, and the remaining subscribers
						 *  won't get their messages.  That situation will also block
						 *  all messages from getting sent for the rest of the life
						 *  of the application, because ALL messages are sent on this
						 *  thread.
						 *  
						 *  - This algorithm requires the subscribers to exist.
						 *  Another implementation might be:  use WeakReferences to
						 *  those subscribers, and if one happens to be gone (null)
						 *  by the time we get to it, purge it from the list.
						 */
                        /*
                         * Comments by John to the above observations:
						 * 1. 	It is expected of the subscribers to finish quickly (start a new thread if necessary).
						 * 		Only other way I can think of is, span a thread for each subscriber, which opens up 
						 * 		whole lot of complications (creating a thread for each event and each subscriber will be a nightmare).
						 * 		Even 3rd party APIs usually suggest that the subscriber finish quickly and return. If not the app will 
						 *		behave strangely. Subscribers need to follow the rules. And also we're not talking about 100s of 
						 *		subscribers for each message (at-the most 2 or 3 subscribers for one event ocationally, so not a problem).
						 *
						 * 2.	As for dangling subscriptions, if some activity doesn't unregister, it is bad programming, and we should not 
						 *		push the bad code under the carpet by weak-references. At-the most we could probably enforce unregister by
						 *		making abstract function in BvActivity and make each activity to unregister there. Or even better keep a list 
						 *		of all registered events in BvActivity and unregister them on onDestroy, but this will take the freedom away
						 *		from the child Activities to unregister where they want to (for eg, some Activities might want to unregister 
						 *		in onPause, and some one onDestroy).
						 *		
						 *		Over all, this is not a distributed system to build "fault tolerant" systems. Publisher is us and Subscriber 
						 *		is our code too. We should make both robust enough instead of one component trying to cover up silly mistakes 
						 *		of other components. 
						 */
                        if(!mEventListeners.isEmpty()) {
                            for(EventListener eventListener : mEventListeners) {
                                try {
                                    eventListener.onEvent(new AndroidEvent(mMessage));
                                } catch(Throwable ex) {
                                    LogHelper.e(LOG_TAG, ex, "Unable to send event to subscriber:" + eventListener);
                                }
                            }
                        }
                    }
                }
            } else {
                //Undefined event received
                throw new InvalidEventException();
            }
        }
    }
}