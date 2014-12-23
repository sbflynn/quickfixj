/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package quickfix.mina;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;

import quickfix.LogUtil;
import quickfix.Session;
import quickfix.SessionID;

/**
 * Processes messages in a session-specific thread.
 */
public class ThreadPerSessionEventHandlingStrategy implements EventHandlingStrategy {

    /**
     * Constant indicating how long we wait for an incoming message. After this thread has been asked
     * to stop, it can take up to this long to terminate.
     */
    private static final long THREAD_WAIT_FOR_MESSAGE_MS = 250;
    private final ConcurrentMap<FIXSessionID, MessageDispatchingThread> dispatchers = new ConcurrentHashMap<FIXSessionID, MessageDispatchingThread>();
    private final SessionConnector sessionConnector;
    private final int queueCapacity;

    public ThreadPerSessionEventHandlingStrategy(SessionConnector connector, int queueCapacity) {
        sessionConnector = connector;
        this.queueCapacity = queueCapacity;
    }

    @Override
    public void onMessage(Session quickfixSession, FIXMessage message) {
        MessageDispatchingThread dispatcher = dispatchers.get(quickfixSession.getSessionID());
        if (dispatcher == null) {
            final MessageDispatchingThread temp = new MessageDispatchingThread(quickfixSession,
                    queueCapacity);
            dispatcher = dispatchers.putIfAbsent(quickfixSession.getSessionID(), temp);
            if (dispatcher == null) {
                dispatcher = temp;
            }
            startDispatcherThread(dispatcher);
        }
        dispatcher.enqueue(message);
    }

    /**
     * The SessionConnector is not directly required for thread-per-session handler - we don't multiplex
     * between multiple sessions here.
     * However it is made available here for other callers (such as SessionProviders wishing to register dynamic sessions).
     */
    @Override
    public SessionConnector getSessionConnector() {
        return sessionConnector;
    }

    protected void startDispatcherThread(MessageDispatchingThread dispatcher) {
        dispatcher.start();
    }

    public void stopDispatcherThreads() {
        // dispatchersToShutdown is backed by the map itself so changes in one are reflected in the other
        final Collection<MessageDispatchingThread> dispatchersToShutdown = dispatchers.values();
        for (final MessageDispatchingThread dispatcher : dispatchersToShutdown) {
            dispatcher.stopDispatcher();
        }

        // wait for threads to stop
        while (!dispatchersToShutdown.isEmpty()) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            for (final Iterator<MessageDispatchingThread> iterator = dispatchersToShutdown
                    .iterator(); iterator.hasNext();) {
                final MessageDispatchingThread messageDispatchingThread = iterator.next();
                if (messageDispatchingThread.isStopped()) {
                    iterator.remove();
                }
            }
        }
    }

    protected class MessageDispatchingThread extends Thread {
        private final Session quickfixSession;
        private final BlockingQueue<FIXMessage> messages;
        private volatile boolean stopped;
        private volatile boolean stopping;

        private MessageDispatchingThread(Session session, int queueCapacity) {
            super("QF/J Session dispatcher: " + session.getSessionID());
            quickfixSession = session;
            messages = new LinkedBlockingQueue<FIXMessage>(queueCapacity);
        }

        public void enqueue(FIXMessage message) {
            try {
                messages.put(message);
            } catch (final InterruptedException e) {
                quickfixSession.getLog().onErrorEvent(e.toString());
            }
        }

        public int getQueueSize() {
            return messages.size();
        }

        @Override
        public void run() {
            while (!stopping) {
                try {
                    if (quickfixSession.hasResponder()) {
                        final FIXMessage message = getNextMessage(messages);
                        if (message != null && quickfixSession.hasResponder()) {
                            quickfixSession.next(message);
                        }
                    } else {
                        stopping = true;
                    }
                } catch (final InterruptedException e) {
                    LogUtil.logThrowable(quickfixSession.getSessionID(),
                            "Message dispatcher interrupted", e);
                    stopping = true;
                } catch (final Throwable e) {
                    LogUtil.logThrowable(quickfixSession.getSessionID(),
                            "Error during message processing", e);
                }
            }
            dispatchers.remove(quickfixSession.getSessionID());
            stopped = true;
        }

        public void stopDispatcher() {
            stopping = true;
            stopped = true;
        }

        public boolean isStopped() {
            return stopped;
        }
    }

    protected MessageDispatchingThread getDispatcher(SessionID sessionID) {
        return dispatchers.get(sessionID);
    }

    /**
     * Get the next message from the messages {@link java.util.concurrent.BlockingQueue}.
     * <p>
     * We do not block indefinitely as that would prevent this thread from ever stopping
     *
     * @see #THREAD_WAIT_FOR_MESSAGE_MS
     * @param messages
     * @return next message or null if nothing arrived within the timeout period
     * @throws InterruptedException
     */
    protected FIXMessage getNextMessage(BlockingQueue<FIXMessage> messages)
            throws InterruptedException {
        return messages.poll(THREAD_WAIT_FOR_MESSAGE_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public int getQueueSize() {
        int ret = 0;
        for (final MessageDispatchingThread mdt : dispatchers.values()) {
            ret += mdt.getQueueSize();
        }
        return ret;
    }

}
