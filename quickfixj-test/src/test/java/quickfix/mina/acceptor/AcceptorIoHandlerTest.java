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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import org.apache.mina.core.session.IoSession;
import org.junit.Test;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.messages.bd.fix44.Logout;
import org.quickfixj.messages.bd.fix44.field.BeginString;
import org.quickfixj.messages.bd.fix44.field.MsgType;
import org.quickfixj.messages.fixt11.Logon;
import org.quickfixj.messages.fixt11.field.DefaultApplVerID;
import org.quickfixj.messages.fixt11.field.EncryptMethod;
import org.quickfixj.messages.fixt11.field.HeartBtInt;
import org.quickfixj.messages.fixt11.field.MsgSeqNum;
import org.quickfixj.messages.fixt11.field.SenderCompID;
import org.quickfixj.messages.fixt11.field.SendingTime;
import org.quickfixj.messages.fixt11.field.TargetCompID;

import quickfix.Session;
import quickfix.SessionFactoryTestSupport;
import quickfix.SessionID;
import quickfix.UnitTestApplication;
import quickfix.mina.EventHandlingStrategy;
import quickfix.mina.NetworkingOptions;
import quickfix.mina.acceptor.AbstractSocketAcceptor.StaticAcceptorSessionProvider;

public class AcceptorIoHandlerTest {

    /**
     * QFJ-592 We need to make sure that the targetDefaultApplVerID gets set as
     * early as possible, hence we set it before the Logon message is put to the
     * event queue.
     */
    @Test
    public void testFIXTLogonAndApplVerID() throws Exception {

        EventHandlingStrategy mockEventHandlingStrategy = mock(EventHandlingStrategy.class);
        IoSession mockIoSession = mock(IoSession.class);

        final SessionID sessionID = new SessionID(FIXBeginString.FIXT11, "SENDER", "TARGET");
        final Session session = SessionFactoryTestSupport.createSession(sessionID,
                new UnitTestApplication(), false);
        stub(mockIoSession.getAttribute("QF_SESSION")).toReturn(null); // to
                                                                       // create
                                                                       // a new
                                                                       // Session

        final HashMap<FIXSessionID, Session> acceptorSessions = new HashMap<FIXSessionID, Session>();
        acceptorSessions.put(sessionID, session);
        final StaticAcceptorSessionProvider sessionProvider = createSessionProvider(acceptorSessions);

        final AcceptorIoHandler handler = new AcceptorIoHandler(sessionProvider,
                new NetworkingOptions(new Properties()), mockEventHandlingStrategy);

        final DefaultApplVerID defaultApplVerID = new DefaultApplVerID(
                String.valueOf(FIXApplication.FIX50SP2.getId()));
        final Logon message = new Logon(EncryptMethod.NONE_OTHER, new HeartBtInt(30),
                defaultApplVerID);
        message.getHeader().setField(new TargetCompID(sessionID.getSenderCompID()));
        message.getHeader().setField(new SenderCompID(sessionID.getTargetCompID()));
        message.getHeader().setField(new SendingTime(new Date()));
        message.getHeader().setField(new MsgSeqNum(1));

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(MsgType.LOGON);
        message.getHeader().setField(new BeginString(FIXBeginString.FIXT11.getValue()));

        handler.processMessage(mockIoSession, message);
        assertEquals(defaultApplVerID.getValue(),
                String.valueOf(session.getTargetDefaultApplicationVersionID().getId()));
    }

    @Test
    public void testMessageBeforeLogon() throws Exception {

        IoSession mockIoSession = mock(IoSession.class);
        stub(mockIoSession.getAttribute("QF_SESSION")).toReturn(null);

        EventHandlingStrategy mockEventHandlingStrategy = mock(EventHandlingStrategy.class);

        HashMap<FIXSessionID, Session> acceptorSessions = new HashMap<FIXSessionID, Session>();

        AcceptorIoHandler handler = new AcceptorIoHandler(createSessionProvider(acceptorSessions),
                new NetworkingOptions(new Properties()), mockEventHandlingStrategy);

        Logout logout = new Logout();

        // Added - TODO these should be acquired via a MessageBuilder
        logout.getHeader().setField(MsgType.LOGOUT);
        logout.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));

        handler.processMessage(mockIoSession, logout);

        verify(mockIoSession).getAttribute("QF_SESSION");
        verifyNoMoreInteractions(mockEventHandlingStrategy);
    }

    private StaticAcceptorSessionProvider createSessionProvider(
            HashMap<FIXSessionID, Session> acceptorSessions) {

        return new AbstractSocketAcceptor.StaticAcceptorSessionProvider(acceptorSessions);
    }

    @Test
    public void testMessageBeforeLogonWithBoundSession() throws Exception {

        IoSession mockIoSession = mock(IoSession.class);

        Session qfSession = SessionFactoryTestSupport.createSession();
        stub(mockIoSession.getAttribute("QF_SESSION")).toReturn(qfSession);

        EventHandlingStrategy mockEventHandlingStrategy = mock(EventHandlingStrategy.class);

        Logout logout = new Logout();
        logout.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.SenderCompID(qfSession.getSessionID()
                        .getSenderCompID()));
        logout.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.TargetCompID(qfSession.getSessionID()
                        .getTargetCompID()));

        HashMap<FIXSessionID, Session> acceptorSessions = new HashMap<FIXSessionID, Session>();

        AcceptorIoHandler handler = new AcceptorIoHandler(createSessionProvider(acceptorSessions),
                new NetworkingOptions(new Properties()), mockEventHandlingStrategy);

        handler.processMessage(mockIoSession, logout);

        verify(mockIoSession).getAttribute("QF_SESSION");
        verify(mockEventHandlingStrategy).onMessage(qfSession, logout);
    }

    @Test
    public void testMessageBeforeLogonWithKnownButUnboundSession() throws Exception {

        IoSession mockIoSession = mock(IoSession.class);

        stub(mockIoSession.getAttribute("QF_SESSION")).toReturn(null);

        EventHandlingStrategy mockEventHandlingStrategy = mock(EventHandlingStrategy.class);

        Session qfSession = SessionFactoryTestSupport.createSession();

        Logout logout = new Logout();
        logout.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.SenderCompID(qfSession.getSessionID()
                        .getSenderCompID()));
        logout.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.TargetCompID(qfSession.getSessionID()
                        .getTargetCompID()));

        // Added - TODO these should be acquired via a MessageBuilder
        logout.getHeader().setField(MsgType.LOGOUT);
        logout.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));

        // Expect that onMessage will not be called
        // mockEventHandlingStrategy.onMessage(qfSession, logout);

        HashMap<FIXSessionID, Session> acceptorSessions = new HashMap<FIXSessionID, Session>();
        acceptorSessions.put(qfSession.getSessionID(), qfSession);
        AcceptorIoHandler handler = new AcceptorIoHandler(createSessionProvider(acceptorSessions),
                new NetworkingOptions(new Properties()), mockEventHandlingStrategy);

        handler.processMessage(mockIoSession, logout);

        verify(mockIoSession).getAttribute("QF_SESSION");
        verifyNoMoreInteractions(mockEventHandlingStrategy);
    }
}
