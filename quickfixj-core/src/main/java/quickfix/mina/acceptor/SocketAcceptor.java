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

package quickfix.mina.acceptor;

import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.LogFactory;
import org.quickfixj.engine.MessageStoreFactory;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.RuntimeError;
import quickfix.Session;
import quickfix.SessionSettings;
import quickfix.mina.EventHandlingStrategy;
import quickfix.mina.SingleThreadedEventHandlingStrategy;

/**
 * Accepts connections and uses a single thread to process messages for all
 * sessions.
 */
public class SocketAcceptor extends AbstractSocketAcceptor {
    private Boolean isStarted = Boolean.FALSE;
    private final Object lock = new Object();
    private final SingleThreadedEventHandlingStrategy eventHandlingStrategy;

    public SocketAcceptor(Application application, MessageStoreFactory messageStoreFactory,
            SessionSettings settings, LogFactory logFactory, FIXEngine engine, int queueCapacity)
            throws ConfigError {
        super(application, messageStoreFactory, settings, logFactory, engine);
        eventHandlingStrategy = new SingleThreadedEventHandlingStrategy(this, queueCapacity);
    }

    public SocketAcceptor(Application application, MessageStoreFactory messageStoreFactory,
            SessionSettings settings, LogFactory logFactory, FIXEngine engine) throws ConfigError {
        super(application, messageStoreFactory, settings, logFactory, engine);
        eventHandlingStrategy = new SingleThreadedEventHandlingStrategy(this,
                DEFAULT_QUEUE_CAPACITY);
    }

    public SocketAcceptor(Application application, MessageStoreFactory messageStoreFactory,
            SessionSettings settings, FIXEngine engine, int queueCapacity) throws ConfigError {
        super(application, messageStoreFactory, settings, engine);
        eventHandlingStrategy = new SingleThreadedEventHandlingStrategy(this, queueCapacity);
    }

    public SocketAcceptor(Application application, MessageStoreFactory messageStoreFactory,
            SessionSettings settings, FIXEngine engine) throws ConfigError {
        super(application, messageStoreFactory, settings, engine);
        eventHandlingStrategy = new SingleThreadedEventHandlingStrategy(this,
                DEFAULT_QUEUE_CAPACITY);
    }

    //    public SocketAcceptor(SessionFactory sessionFactory, SessionSettings settings, int queueCapacity)
    //            throws ConfigError {
    //        super(settings, sessionFactory);
    //        eventHandlingStrategy = new SingleThreadedEventHandlingStrategy(this, queueCapacity);
    //    }
    //
    //    public SocketAcceptor(SessionFactory sessionFactory, SessionSettings settings)
    //            throws ConfigError {
    //        super(settings, sessionFactory);
    //        eventHandlingStrategy = new SingleThreadedEventHandlingStrategy(this,
    //                DEFAULT_QUEUE_CAPACITY);
    //    }

    @Override
    public void block() throws ConfigError, RuntimeError {
        initialize();
        eventHandlingStrategy.block();
    }

    @Override
    public void start() throws ConfigError, RuntimeError {
        initialize();
        eventHandlingStrategy.blockInThread();
    }

    private void initialize() throws ConfigError {
        synchronized (lock) {
            if (isStarted.equals(Boolean.FALSE)) {
                startAcceptingConnections();
            }
            isStarted = Boolean.TRUE;
        }
    }

    @Override
    public void stop() {
        stop(false);
    }

    @Override
    public void stop(boolean forceDisconnect) {
        try {
            eventHandlingStrategy.stopHandlingMessages();
            try {
                stopAcceptingConnections();
            } catch (Exception e) {
                log.error("Error when stopping acceptor.", e);
            }
            logoutAllSessions(forceDisconnect);
            stopSessionTimer();
        } finally {
            Session.unregisterSessions(getSessions());
            synchronized (lock) {
                isStarted = Boolean.FALSE;
            }
        }
    }

    @Override
    protected EventHandlingStrategy getEventHandlingStrategy() {
        return eventHandlingStrategy;
    }
}
