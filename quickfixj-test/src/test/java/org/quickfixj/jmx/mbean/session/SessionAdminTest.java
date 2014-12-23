package org.quickfixj.jmx.mbean.session;

import java.util.ArrayList;

import javax.management.ObjectName;

import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.SessionNotFoundException;
import org.quickfixj.jmx.mbean.SessionAdmin;
import org.quickfixj.jmx.mbean.SessionAdminMBean;

import quickfix.MessageUtils;
import quickfix.Session;
import quickfix.SessionFactoryTestSupport;
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
        assertEquals(25, MessageUtils.coerceToInt(admin.sentMessages.get(0), FIXTag.NEW_SEQ_NO));
    }

    private class MockSessionAdmin extends SessionAdmin {
        ArrayList<FIXMessage> sentMessages = new ArrayList<FIXMessage>();

        public MockSessionAdmin(Session session, ObjectName connectorName, ObjectName settingsName) {
            super(session, connectorName, settingsName);
        }

        @Override
        protected void doSend(FIXMessage message, FIXSessionID sessionID) throws SessionNotFoundException {
            sentMessages.add(message);
        }
    }

}
