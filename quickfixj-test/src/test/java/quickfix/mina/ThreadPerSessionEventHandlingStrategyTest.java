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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.field.UtcTimestampConverter;
import org.quickfixj.messages.bd.fix40.Logon;
import org.quickfixj.messages.bd.fix40.field.BeginString;
import org.quickfixj.messages.bd.fix40.field.EncryptMethod;
import org.quickfixj.messages.bd.fix40.field.HeartBtInt;
import org.quickfixj.messages.bd.fix40.field.MsgType;
import org.quickfixj.messages.bd.fix40.field.SendingTime;

import quickfix.ConfigError;
import quickfix.DefaultSessionFactory;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.MemoryStoreFactory;
import quickfix.RejectLogon;
import quickfix.Responder;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnitTestApplication;

public class ThreadPerSessionEventHandlingStrategyTest {

    private final static class ThreadPerSessionEventHandlingStrategyUnderTest extends
            ThreadPerSessionEventHandlingStrategy {

        public ThreadPerSessionEventHandlingStrategyUnderTest() {

            super(null, SessionConnector.DEFAULT_QUEUE_CAPACITY);
        }

        public Exception getNextMessageException;

        public int getMessageCount = 1;

        @Override
        protected void startDispatcherThread(
                ThreadPerSessionEventHandlingStrategy.MessageDispatchingThread dispatcher) {
            // no-op
        }

        @Override
        protected FIXMessage getNextMessage(BlockingQueue<FIXMessage> messages)
                throws InterruptedException {

            if (getMessageCount-- == 0) {
                throw new InterruptedException("END COUNT");
            }
            if (getNextMessageException != null) {
                if (getNextMessageException instanceof InterruptedException) {
                    throw (InterruptedException) getNextMessageException;
                }
                throw (RuntimeException) getNextMessageException;
            }
            return super.getNextMessage(messages);
        }
    }

    /**
     * Verifies that messages are dispatched via dispatcher threads and that the
     * threads die when the strategy is shut down. See QFJ-410.
     *
     * @throws Exception
     */
    @Test
    public void testEventHandling() throws Exception {

        final SessionID sessionID = new SessionID(FIXBeginString.FIX40, "TW", "ISLD");
        final CountDownLatch latch = new CountDownLatch(1);

        final UnitTestApplication application = new UnitTestApplication() {

            @Override
            public void fromAdmin(FIXMessage message, FIXSessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, RejectLogon {

                super.fromAdmin(message, sessionId);
                latch.countDown();
            }
        };

        Session session = setUpSession(sessionID, application);

        Logon message = new org.quickfixj.messages.bd.fix40.Logon();

        message.getHeader()
                .setField(new org.quickfixj.messages.bd.fix40.field.SenderCompID("ISLD"));
        message.getHeader().setField(new org.quickfixj.messages.bd.fix40.field.TargetCompID("TW"));
        message.getHeader().setField(
                new org.quickfixj.messages.bd.fix40.field.SendingTime(UtcTimestampConverter
                        .convert(new Date(), false)));
        message.getHeader().setField(new org.quickfixj.messages.bd.fix40.field.MsgSeqNum(1));
        message.setEncryptMethod(EncryptMethod.NONE_OTHER);
        message.setHeartBtInt(new org.quickfixj.messages.bd.fix40.field.HeartBtInt(30));

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(MsgType.LOGON);
        message.getHeader().setField(new BeginString(FIXBeginString.FIX40.getValue()));

        final ThreadPerSessionEventHandlingStrategy strategy = new ThreadPerSessionEventHandlingStrategy(
                null, SessionConnector.DEFAULT_QUEUE_CAPACITY);

        strategy.onMessage(session, message);

        // Wait for a received message
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Timeout");
        }

        assertEquals(1, application.fromAdminMessages.size());

        Thread[] threads = new Thread[1024];
        Thread.enumerate(threads);

        Thread dispatcherThread = null;
        for (final Thread thread : threads) {
            if (thread.getName().startsWith("QF/J Session dispatcher")) {
                dispatcherThread = thread;
                // Dispatcher threads are not daemon threads
                assertThat(dispatcherThread.isDaemon(), is(false));
                break;
            }
        }

        // We should have found the dispatcher thread
        assertThat(dispatcherThread, notNullValue());

        // Stop the threads and then check the thread state
        strategy.stopDispatcherThreads();

        for (int i = 0; i < 10; i++) {
            Thread.sleep(100);
            if (!dispatcherThread.isAlive()) {
                break;
            }
        }

        // Dispatcher thread should be dead
        assertThat(dispatcherThread.isAlive(), is(false));
        assertNull(strategy.getDispatcher(sessionID));
    }

    /**
     * See QFJ-686. Verify that thread is stopped if Session has no responder.
     */
    @Test
    public void testEventHandlingOnDisconnect() throws Exception {

        final SessionID sessionID = new SessionID(FIXBeginString.FIX40, "TW", "ISLD");
        final CountDownLatch latch = new CountDownLatch(1);

        final UnitTestApplication application = new UnitTestApplication() {

            @Override
            public void fromAdmin(FIXMessage message, FIXSessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, RejectLogon {

                super.fromAdmin(message, sessionId);
                latch.countDown();
            }
        };

        final Session session = setUpSession(sessionID, application);

        Logon message = new Logon();
        message.getHeader()
                .setField(new org.quickfixj.messages.bd.fix40.field.SenderCompID("ISLD"));
        message.getHeader().setField(new org.quickfixj.messages.bd.fix40.field.TargetCompID("TW"));
        message.getHeader().setField(
                new SendingTime(UtcTimestampConverter.convert(new Date(), false)));
        message.getHeader().setField(new org.quickfixj.messages.bd.fix40.field.MsgSeqNum(1));

        message.setEncryptMethod(EncryptMethod.NONE_OTHER);
        message.setHeartBtInt(new HeartBtInt(30));

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(MsgType.LOGON);
        message.getHeader().setField(new BeginString(FIXBeginString.FIX40.getValue()));

        final ThreadPerSessionEventHandlingStrategy strategy = new ThreadPerSessionEventHandlingStrategy(
                null, SessionConnector.DEFAULT_QUEUE_CAPACITY);

        strategy.onMessage(session, message);

        // Wait for a received message
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("Timeout");
        }

        assertEquals(1, application.fromAdminMessages.size());

        Thread[] threads = new Thread[1024];
        Thread.enumerate(threads);

        Thread dispatcherThread = null;
        for (final Thread thread : threads) {
            if (thread != null && thread.getName().startsWith("QF/J Session dispatcher")) {
                dispatcherThread = thread;
                // Dispatcher threads are not daemon threads
                assertThat(dispatcherThread.isDaemon(), is(false));
                break;
            }
        }

        session.disconnect("test", true);
        assertFalse(session.hasResponder());

        // sleep some time to let the thread stop
        for (int i = 0; i < 20; i++) {
            Thread.sleep(100);
            if (!dispatcherThread.isAlive()) {
                break;
            }
        }
        assertNull(strategy.getDispatcher(sessionID));

        threads = new Thread[1024];
        Thread.enumerate(threads);

        dispatcherThread = null;
        for (final Thread thread : threads) {
            if (thread != null && thread.getName().startsWith("QF/J Session dispatcher")) {
                dispatcherThread = thread;
                // Dispatcher threads are not daemon threads
                assertThat(dispatcherThread.isDaemon(), is(false));
                break;
            }
        }

        // the session dispatcher should be dead and hence not listed in the
        // threads array
        assertNull(dispatcherThread);
    }

    @Test
    public void testEventHandlingInterruptInRun() throws Exception {

        final SessionID sessionID = new SessionID(FIXBeginString.FIX40, "TW", "ISLD");
        final Session session = setUpSession(sessionID);
        FIXMessage message = new Logon();
        message.setField(new HeartBtInt(30));
        final ThreadPerSessionEventHandlingStrategyUnderTest strategy = new ThreadPerSessionEventHandlingStrategyUnderTest();

        strategy.onMessage(session, message);
        strategy.getNextMessageException = new InterruptedException("TEST");
        strategy.getDispatcher(sessionID).run();
    }

    @Test
    public void testEventHandlingRuntimeException() throws Exception {

        SessionID sessionID = new SessionID(FIXBeginString.FIX40, "TW", "ISLD");
        Session session = setUpSession(sessionID);
        FIXMessage message = new Logon();
        message.setField(new HeartBtInt(30));
        ThreadPerSessionEventHandlingStrategyUnderTest strategy = new ThreadPerSessionEventHandlingStrategyUnderTest();

        strategy.onMessage(session, message);
        strategy.getNextMessageException = new NullPointerException("TEST");
        strategy.getDispatcher(sessionID).run();
    }

    // verify the assumption that this always returns null
    @Test
    public void testVerifyGetConnectorAssumption() throws Exception {

        final ThreadPerSessionEventHandlingStrategyUnderTest strategy = new ThreadPerSessionEventHandlingStrategyUnderTest();
        assertNull(strategy.getSessionConnector());
    }

    private Session setUpSession(SessionID sessionID) throws ConfigError {

        final UnitTestApplication application = new UnitTestApplication();
        return setUpSession(sessionID, application);
    }

    private Session setUpSession(SessionID sessionID, UnitTestApplication application)
            throws ConfigError {

        final DefaultSessionFactory sessionFactory = new DefaultSessionFactory(application,
                new MemoryStoreFactory(), new ScreenLogFactory(true, true, true));
        final SessionSettings settings = new SessionSettings();
        settings.setString(SessionFactory.SETTING_CONNECTION_TYPE,
                SessionFactory.ACCEPTOR_CONNECTION_TYPE);
        settings.setString(Session.SETTING_USE_DATA_DICTIONARY, "N");
        settings.setString(Session.SETTING_START_TIME, "00:00:00");
        settings.setString(Session.SETTING_END_TIME, "00:00:00");
        final Session session = sessionFactory.create(sessionID, settings);
        session.setResponder(mock(Responder.class));
        return session;
    }
}
