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
import quickfix.mina.ThreadPerSessionEventHandlingStrategy;

/**
 * Accepts connections and uses a separate thread per session to process messages.
 */
public class ThreadedSocketAcceptor extends AbstractSocketAcceptor {
    private final ThreadPerSessionEventHandlingStrategy eventHandlingStrategy;

    public ThreadedSocketAcceptor(Application application, MessageStoreFactory messageStoreFactory,
            SessionSettings settings, LogFactory logFactory, FIXEngine engine, int queueCapacity)
            throws ConfigError {
        super(application, messageStoreFactory, settings, logFactory, engine);
        eventHandlingStrategy = new ThreadPerSessionEventHandlingStrategy(this, queueCapacity);
    }

    public ThreadedSocketAcceptor(Application application, MessageStoreFactory messageStoreFactory,
            SessionSettings settings, LogFactory logFactory, FIXEngine engine) throws ConfigError {
        super(application, messageStoreFactory, settings, logFactory, engine);
        eventHandlingStrategy = new ThreadPerSessionEventHandlingStrategy(this,
                DEFAULT_QUEUE_CAPACITY);
    }

    public ThreadedSocketAcceptor(Application application, MessageStoreFactory messageStoreFactory,
            SessionSettings settings, FIXEngine engine, int queueCapacity) throws ConfigError {
        super(application, messageStoreFactory, settings, engine);
        eventHandlingStrategy = new ThreadPerSessionEventHandlingStrategy(this, queueCapacity);
    }

    public ThreadedSocketAcceptor(Application application, MessageStoreFactory messageStoreFactory,
            SessionSettings settings, FIXEngine engine) throws ConfigError {
        super(application, messageStoreFactory, settings, engine);
        eventHandlingStrategy = new ThreadPerSessionEventHandlingStrategy(this,
                DEFAULT_QUEUE_CAPACITY);
    }

    //    public ThreadedSocketAcceptor(SessionFactory sessionFactory, SessionSettings settings,
    //            int queueCapacity) throws ConfigError {
    //        super(settings, sessionFactory);
    //        eventHandlingStrategy = new ThreadPerSessionEventHandlingStrategy(this, queueCapacity);
    //    }
    //
    //    public ThreadedSocketAcceptor(SessionFactory sessionFactory, SessionSettings settings)
    //            throws ConfigError {
    //        super(settings, sessionFactory);
    //        eventHandlingStrategy = new ThreadPerSessionEventHandlingStrategy(this,
    //                DEFAULT_QUEUE_CAPACITY);
    //    }

    @Override
    public void start() throws ConfigError, RuntimeError {
        startAcceptingConnections();
    }

    @Override
    public void stop() {
        stop(false);
    }

    @Override
    public void stop(boolean forceDisconnect) {
        try {
            stopAcceptingConnections();
        } catch (Exception e) {
            log.error("Error when stopping acceptor.", e);
        }
        logoutAllSessions(forceDisconnect);
        stopSessionTimer();
        eventHandlingStrategy.stopDispatcherThreads();
        Session.unregisterSessions(getSessions());
    }

    @Override
    public void block() throws ConfigError, RuntimeError {
        throw new UnsupportedOperationException("Blocking not supported: " + getClass());
    }

    @Override
    protected EventHandlingStrategy getEventHandlingStrategy() {
        return eventHandlingStrategy;
    }

}
