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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXTag;

import quickfix.FieldNotFound;
import quickfix.FixMessageTypes;
import quickfix.LogUtil;
import quickfix.Session;
import quickfix.SystemTime;

/**
 * Processes messages for all sessions in a single thread.
 */
public class SingleThreadedEventHandlingStrategy implements EventHandlingStrategy {

    private static final String MESSAGE_PROCESSOR_THREAD_NAME = "QFJ Message Processor";

    private final BlockingQueue<SessionMessageEvent> eventQueue;

    private final SessionConnector sessionConnector;

    private boolean isStopped;

    private long stopTime = 0L;

    public SingleThreadedEventHandlingStrategy(SessionConnector connector, int queueCapacity) {

        sessionConnector = connector;
        eventQueue = new LinkedBlockingQueue<SessionMessageEvent>(queueCapacity);
    }

    @Override
    public void onMessage(Session quickfixSession, FIXMessage message) {

        try {
            eventQueue.put(new SessionMessageEvent(quickfixSession, message));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SessionConnector getSessionConnector() {

        return sessionConnector;
    }

    public void block() {

        while (true) {
            synchronized (this) {
                if (isStopped) {
                    if (stopTime == 0) {
                        stopTime = SystemTime.currentTimeMillis();
                    }
                    if (!sessionConnector.isLoggedOn()
                            || SystemTime.currentTimeMillis() - stopTime > 5000L) {
                        sessionConnector.stopSessionTimer();
                        // reset the stoptime
                        stopTime = 0;
                        return;
                    }
                }
            }
            try {
                SessionMessageEvent event = getMessage();
                if (event != null) {
                    event.processMessage();
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    private SessionMessageEvent getMessage() throws InterruptedException {

        return eventQueue.poll(1000L, TimeUnit.MILLISECONDS);
    }

    public void blockInThread() {

        startHandlingMessages();
        Thread messageProcessingThread = new Thread(new Runnable() {

            @Override
            public void run() {

                block();
            }
        }, MESSAGE_PROCESSOR_THREAD_NAME);
        messageProcessingThread.setDaemon(true);
        messageProcessingThread.start();
    }

    private static class SessionMessageEvent {

        private final Session session;

        private final FIXMessage message;

        SessionMessageEvent(Session session, FIXMessage message) {
            this.message = message;
            this.session = session;
        }

        public void processMessage() {

            try {
                if (session.hasResponder()) {
                    session.next(message);
                } else {
                    try {
                        final String msgType = message.getHeader().getFieldValue(FIXTag.MSG_TYPE);
                        if (msgType.equals(FixMessageTypes.LOGOUT)) {
                            session.next(message);
                        }
                    } catch (FieldNotFound ex) {
                        // ignore
                    }
                }
            } catch (Throwable e) {
                LogUtil.logThrowable(session.getSessionID(), e.getMessage(), e);
            }
        }
    }

    private synchronized void startHandlingMessages() {

        isStopped = false;
    }

    public synchronized void stopHandlingMessages() {

        isStopped = true;
    }

    @Override
    public int getQueueSize() {

        return eventQueue.size();
    }
}
