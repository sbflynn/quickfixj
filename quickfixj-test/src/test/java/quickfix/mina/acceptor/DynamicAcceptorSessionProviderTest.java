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

import static quickfix.mina.acceptor.DynamicAcceptorSessionProvider.WILDCARD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.quickfixj.FIXBeginString;
import org.quickfixj.QFJException;
import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXSession;
import org.quickfixj.engine.MessageStoreFactory;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.LogFactory;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultEngine;
import quickfix.MemoryStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnitTestApplication;
import quickfix.mina.SessionConnector;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider.TemplateMapping;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider.TemplatePattern;
import junit.framework.TestCase;

public class DynamicAcceptorSessionProviderTest extends TestCase {
    private DynamicAcceptorSessionProvider provider;
    private SessionSettings settings;
    private List<TemplateMapping> templateMappings;
    private Application application;
    private MessageStoreFactory messageStoreFactory;
    private LogFactory logFactory;

    @Override
    protected void setUp() throws Exception {
        settings = new SessionSettings();
        templateMappings = new ArrayList<TemplateMapping>();
        application = new UnitTestApplication();
        messageStoreFactory = new MemoryStoreFactory();
        logFactory = new ScreenLogFactory();

        SessionID templateId1 = new SessionID(FIXBeginString.FIX42, "ANY", "ANY");
        templateMappings.add(new TemplateMapping(new TemplatePattern(Arrays.asList(FIXBeginString
                .values()), "S1", WILDCARD), templateId1));
        setUpSettings(templateId1, "ResetOnLogout", "Y");

        SessionID templateId2 = new SessionID(FIXBeginString.FIX44, "S1", "ANY");
        templateMappings.add(new TemplateMapping(new TemplatePattern(Collections
                .singletonList(FIXBeginString.FIX44), WILDCARD, WILDCARD), templateId2));
        setUpSettings(templateId2, "RefreshOnLogon", "Y");

        SessionID templateId3 = new SessionID(FIXBeginString.FIX44, "ANY", "ANY");
        templateMappings.add(new TemplateMapping(new TemplatePattern(Collections
                .singletonList(FIXBeginString.FIX42), WILDCARD, WILDCARD, WILDCARD, WILDCARD,
                WILDCARD, WILDCARD, WILDCARD), templateId3));
        setUpSettings(templateId3, "CheckCompID", "N");

        provider = new DynamicAcceptorSessionProvider(settings, templateMappings, application,
                messageStoreFactory, logFactory, DefaultEngine.getDefaultEngine());
    }

    public void testSessionCreation() throws Exception {

        FIXSession session1 = provider.getSession(new SessionID(FIXBeginString.FIX42, "SENDER",
                "SENDERSUB", "SENDERLOC", "TARGET", "TARGETSUB", "TARGETLOC", null), null);
        FIXSessionID sessionID1 = session1.getSessionID();
        assertEquals("wrong FIX version", FIXBeginString.FIX42, sessionID1.getBeginString());
        assertEquals("wrong sender", "SENDER", sessionID1.getSenderCompID());
        assertEquals("wrong senderSub", "SENDERSUB", sessionID1.getSenderSubID());
        assertEquals("wrong senderLoc", "SENDERLOC", sessionID1.getSenderLocationID());
        assertEquals("wrong target", "TARGET", sessionID1.getTargetCompID());
        assertEquals("wrong targetSub", "TARGETSUB", sessionID1.getTargetSubID());
        assertEquals("wrong targetLoc", "TARGETLOC", sessionID1.getTargetLocationID());
        assertEquals("wrong setting", false, session1.getResetOnLogout());
        assertEquals("wrong setting", false, session1.getRefreshOnLogon());
        assertEquals("wrong setting", false, session1.getCheckCompID());

        FIXSession session2 = provider.getSession(new SessionID(FIXBeginString.FIX44, "S1", "T"),
                null);
        FIXSessionID sessionID2 = session2.getSessionID();
        assertEquals("wrong FIX version", FIXBeginString.FIX44, sessionID2.getBeginString());
        assertEquals("wrong sender", "S1", sessionID2.getSenderCompID());
        assertEquals("wrong target", "T", sessionID2.getTargetCompID());
        assertEquals("wrong setting", true, session2.getResetOnLogout());
        assertEquals("wrong setting", false, session2.getRefreshOnLogon());
        assertEquals("wrong setting", true, session2.getCheckCompID());

        FIXSession session3 = provider.getSession(new SessionID(FIXBeginString.FIX44, "X", "Y"),
                null);
        FIXSessionID sessionID3 = session3.getSessionID();
        assertEquals("wrong FIX version", FIXBeginString.FIX44, sessionID3.getBeginString());
        assertEquals("wrong sender", "X", sessionID3.getSenderCompID());
        assertEquals("wrong target", "Y", sessionID3.getTargetCompID());
        assertEquals("wrong setting", false, session3.getResetOnLogout());
        assertEquals("wrong setting", true, session3.getRefreshOnLogon());
        assertEquals("wrong setting", true, session3.getCheckCompID());
    }

    private void setUpSettings(SessionID templateID, String key, String value) {
        settings.setString(templateID, "ConnectionType", "acceptor");
        settings.setString(templateID, "StartTime", "00:00:00");
        settings.setString(templateID, "EndTime", "00:00:00");
        settings.setString(templateID, key, value);
    }

    public void testSessionTemplateNotFound() throws Exception {
        try {
            provider.getSession(new SessionID(FIXBeginString.FIX43, "S", "T"), null);
            fail("No exception thrown");
        } catch (QFJException e) {
            // Expected
        }
    }

    public void testToString() throws Exception {
        templateMappings.toString(); // be sure there are no NPEs, etc.
    }

    public void testSimpleConstructor() throws Exception {
        provider = new DynamicAcceptorSessionProvider(settings, new SessionID(FIXBeginString.FIX42,
                "ANY", "ANY"), application, messageStoreFactory, logFactory,
                DefaultEngine.getDefaultEngine());

        // Should actually throw an exception if it fails (see previous test)
        assertNotNull(provider.getSession(new SessionID(FIXBeginString.FIX42, "S", "T"), null));
    }

    /**
     * Verify that if a new session comes in it gets added to the list in session connector
     */
    public void testDynamicSessionIsAddedToSessionConnector() throws Exception {
        MySessionConnector connector = new MySessionConnector(DefaultEngine.getDefaultEngine(),
                settings, null);

        SessionID id1 = new SessionID(FIXBeginString.FIX42, "me", "SENDERSUB", "SENDERLOC", "you",
                "TARGETSUB", "TARGETLOC", null);
        provider.getSession(id1, connector);
        assertEquals(1, connector.sessions.size());
        // try again with same sesionID - should still be 1
        provider.getSession(id1, connector);
        assertEquals(1, connector.sessions.size());

        SessionID id2 = new SessionID(FIXBeginString.FIX42, "SENDER2", "SENDERSUB", "SENDERLOC",
                "TARGET2", "TARGETSUB", "TARGETLOC", null);
        provider.getSession(id2, connector);
        assertEquals(2, connector.sessions.size());
    }

    private static class MySessionConnector extends SessionConnector {
        private HashMap<FIXSessionID, FIXSession> sessions = new HashMap<FIXSessionID, FIXSession>();

        public MySessionConnector(FIXEngine engine, SessionSettings settings,
                SessionFactory sessionFactory) throws ConfigError {
            super(engine, settings, sessionFactory);
        }

        @Override
        public void addDynamicSession(Session inSession) {
            sessions.put(inSession.getSessionID(), inSession);
        }

        @Override
        public void removeDynamicSession(FIXSessionID inSessionID) {
            sessions.remove(inSessionID);
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
