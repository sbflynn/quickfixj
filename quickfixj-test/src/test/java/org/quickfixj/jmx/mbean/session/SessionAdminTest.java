package org.quickfixj.jmx.mbean.session;

import java.util.ArrayList;

import javax.management.ObjectName;

import quickfix.FixTags;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionFactoryTestSupport;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import junit.framework.TestCase;

/**
 * Test the {@link SessionAdminMBean} class
 *
 * @author toli
 * @version $Id$
 */
public class SessionAdminTest extends TestCase {

    public void testResetSequence() throws Exception {
        Session session = SessionFactoryTestSupport.createSession();
        MockSessionAdmin admin = new MockSessionAdmin(session, null, null);
        admin.resetSequence(25);
        assertEquals(1, admin.sentMessages.size());
        assertEquals(25, admin.sentMessages.get(0).getInt(FixTags.NEW_SEQ_NO));
    }

    private class MockSessionAdmin extends SessionAdmin {
        ArrayList<Message> sentMessages = new ArrayList<Message>();

        public MockSessionAdmin(Session session, ObjectName connectorName, ObjectName settingsName) {
            super(session, connectorName, settingsName);
        }

        @Override
        protected void doSend(Message message, SessionID sessionID) throws SessionNotFound {
            sentMessages.add(message);
        }
    }

}
