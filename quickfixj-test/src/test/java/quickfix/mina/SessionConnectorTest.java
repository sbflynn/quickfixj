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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quickfixj.FIXBeginString;
import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXSession;
import org.quickfixj.engine.FIXSession.FIXSessionID;

import quickfix.Acceptor;
import quickfix.ConfigError;
import quickfix.DefaultEngine;
import quickfix.DefaultSessionFactory;
import quickfix.MemoryStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SessionState;
import quickfix.UnitTestApplication;
import junit.framework.TestCase;

public class SessionConnectorTest extends TestCase {
    private List<PropertyChangeEvent> propertyChangeEvents = new ArrayList<PropertyChangeEvent>();

    public void testConnector() throws Exception {
        SessionID sessionID = new SessionID(FIXBeginString.FIX40, "TW", "ISLD");
        SessionSettings settings = setUpSessionSettings(sessionID);
        DefaultSessionFactory sessionFactory = new DefaultSessionFactory(new UnitTestApplication(),
                new MemoryStoreFactory(), new ScreenLogFactory(true, true, true));

        SessionConnector connector = new SessionConnectorUnderTest(
                DefaultEngine.getDefaultEngine(), settings, sessionFactory);

        connector.addPropertyChangeListener(new SessionConnectorListener());

        Session session = connector.createSession(sessionID);
        assertNotNull(session);

        Map<FIXSessionID, Session> sessions = Collections.singletonMap(session.getSessionID(),
                session);
        connector.setSessions(sessions);

        assertEquals(1, propertyChangeEvents.size());

        assertEquals(1, connector.getManagedSessions().size());
        assertEquals(session, connector.getManagedSessions().get(0));

        assertFalse(connector.isLoggedOn());

        Field stateField = session.getClass().getDeclaredField("state");
        stateField.setAccessible(true);
        SessionState state = (SessionState) stateField.get(session);

        state.setLogonSent(true);
        state.setLogonReceived(true);
        assertTrue(connector.isLoggedOn());

        assertTrue(session.isEnabled());
        connector.logoutAllSessions(true);
        assertFalse(session.isEnabled());

        assertEquals(9999, connector.getIntSetting(Acceptor.SETTING_SOCKET_ACCEPT_PORT));

        assertNotNull(connector.getScheduledExecutorService());
        assertEquals(settings, connector.getSettings());
    }

    public void testOneSessionLoggedOnOneSessionNotLoggedOne() throws Exception {
        SessionID sessionID1 = new SessionID(FIXBeginString.FIX40, "TW", "ISLD");
        SessionSettings settings = setUpSessionSettings(sessionID1);
        DefaultSessionFactory sessionFactory = new DefaultSessionFactory(new UnitTestApplication(),
                new MemoryStoreFactory(), new ScreenLogFactory(true, true, true));

        SessionConnector connector = new SessionConnectorUnderTest(
                DefaultEngine.getDefaultEngine(), settings, sessionFactory);

        Session session1 = connector.createSession(sessionID1);
        assertNotNull(session1);

        // test add/remove
        SessionConnectorListener connectorListener = new SessionConnectorListener();
        connector.addPropertyChangeListener(connectorListener);
        connector.removePropertyChangeListener(connectorListener);

        Map<FIXSessionID, Session> sessions = new HashMap<FIXSessionID, Session>();
        sessions.put(session1.getSessionID(), session1);
        connector.setSessions(sessions);

        assertEquals(0, propertyChangeEvents.size());

        assertEquals(1, connector.getManagedSessions().size());
        assertEquals(session1, connector.getManagedSessions().get(0));

        assertFalse(connector.isLoggedOn());

        Field stateField = session1.getClass().getDeclaredField("state");
        stateField.setAccessible(true);
        SessionState state = (SessionState) stateField.get(session1);

        state.setLogonSent(true);
        state.setLogonReceived(true);
        assertTrue(connector.isLoggedOn());

        SessionID sessionID2 = new SessionID(FIXBeginString.FIX40, "TW", "ISLD1");
        settings.setString(sessionID2, SessionFactory.SETTING_CONNECTION_TYPE,
                SessionFactory.ACCEPTOR_CONNECTION_TYPE);
        Session session2 = connector.createSession(sessionID2);
        assertNotNull(session2);
        sessions.put(session2.getSessionID(), session2);
        assertFalse(connector.isLoggedOn());
    }

    /**
     * Test that adding/removing dynamic sessions works correctly
     */
    public void testAddingRemovingDymaicSessions() throws Exception {
        FIXSessionID sessionID = new SessionID(FIXBeginString.FIX40, "TW", "ISLD");
        FIXSessionID sessionID2 = new SessionID(FIXBeginString.FIX40, "me", "you");
        SessionSettings settings = setUpSessionSettings(sessionID);
        DefaultSessionFactory sessionFactory = new DefaultSessionFactory(new UnitTestApplication(),
                new MemoryStoreFactory(), new ScreenLogFactory(true, true, true));

        SessionConnector connector = new SessionConnectorUnderTest(
                DefaultEngine.getDefaultEngine(), settings, sessionFactory);
        connector.setSessions(new HashMap<FIXSessionID, Session>());
        Session session = connector.createSession(sessionID);

        // one-time use connector to create a slightly different session
        SessionSettings settings2 = setUpSessionSettings(sessionID2);
        SessionConnector connector2 = new SessionConnectorUnderTest(
                DefaultEngine.getDefaultEngine(), settings2, sessionFactory);
        connector.setSessions(new HashMap<FIXSessionID, Session>());
        Session session2 = connector2.createSession(sessionID2);
        assertNotNull(session);
        assertNotNull(session2);

        assertEquals(0, connector.getManagedSessions().size());
        connector.addDynamicSession(session);
        assertEquals(1, connector.getManagedSessions().size());
        connector.addDynamicSession(session2);
        assertEquals(2, connector.getManagedSessions().size());
        // the list can be in arbitrary order so let's make sure that we get both
        HashMap<FIXSessionID, FIXSession> map = new HashMap<FIXSessionID, FIXSession>();
        for (FIXSession s : connector.getManagedSessions()) {
            map.put(s.getSessionID(), s);
        }
        assertEquals(session, map.get(session.getSessionID()));
        assertEquals(session2, map.get(session2.getSessionID()));

        connector.removeDynamicSession(session.getSessionID());
        assertEquals(1, connector.getManagedSessions().size());
        assertEquals(session2, connector.getManagedSessions().get(0));
        connector.removeDynamicSession(session2.getSessionID());
        assertEquals(0, connector.getManagedSessions().size());
    }

    private SessionSettings setUpSessionSettings(FIXSessionID sessionID) {
        SessionSettings settings = new SessionSettings();
        settings.setString(Session.SETTING_USE_DATA_DICTIONARY, "N");
        settings.setString(Session.SETTING_START_TIME, "00:00:00");
        settings.setString(Session.SETTING_END_TIME, "00:00:00");
        settings.setString(Acceptor.SETTING_SOCKET_ACCEPT_PORT, "9999");
        settings.setString(sessionID, SessionFactory.SETTING_CONNECTION_TYPE,
                SessionFactory.ACCEPTOR_CONNECTION_TYPE);
        return settings;
    }

    private final class SessionConnectorListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (SessionConnector.SESSIONS_PROPERTY.equals(event.getPropertyName())) {
                propertyChangeEvents.add(event);
            }
        }
    }

    private static class SessionConnectorUnderTest extends SessionConnector {

        public SessionConnectorUnderTest(FIXEngine engine, SessionSettings settings,
                SessionFactory sessionFactory) throws ConfigError {
            super(engine, settings, sessionFactory);
        }

        @Override
        public void start() throws ConfigError, RuntimeError {
            //no-op
        }

        @Override
        public void stop() {
            //no-op
        }

        @Override
        public void stop(boolean force) {
            //no-op
        }

        @Override
        public void block() throws ConfigError, RuntimeError {
            //no-op
        }
    }
}
