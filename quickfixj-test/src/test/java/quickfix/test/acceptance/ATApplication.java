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

package quickfix.test.acceptance;

import java.io.IOException;

import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession;
import org.quickfixj.engine.FIXSession.FIXSessionID;

import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.MessageCracker;
import quickfix.Session;
import quickfix.UnsupportedMessageType;
import junit.framework.Assert;

public class ATApplication implements Application {
    private ATMessageCracker inboundCracker = new ATMessageCracker();
    private MessageCracker outboundCracker = new MessageCracker(new Object());
    private boolean isLoggedOn;

    @Override
    public void onCreate(FIXSessionID sessionID) {
        try {
            assertNoSessionLock(sessionID);
            Session.lookupSession(sessionID).reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void onLogon(FIXSessionID sessionID) {
        assertNoSessionLock(sessionID);
        Assert.assertFalse("Already logged on", isLoggedOn);
        isLoggedOn = true;
    }

    private void assertNoSessionLock(FIXSessionID sessionID) {
        FIXSession session = Session.lookupSession(sessionID);
        Assert.assertNotNull("Can not find session: " + Thread.currentThread(), session);
        Assert.assertFalse("Application is holding session lock", Thread.holdsLock(session));
    }

    @Override
    public synchronized void onLogout(FIXSessionID sessionID) {
        assertNoSessionLock(sessionID);
        inboundCracker.reset();
        Assert.assertTrue("No logged on when logout is received", isLoggedOn);
        isLoggedOn = false;
    }

    @Override
    public void toAdmin(FIXMessage message, FIXSessionID sessionID) {
        assertNoSessionLock(sessionID);
    }

    @Override
    public void toApp(FIXMessage message, FIXSessionID sessionID) throws DoNotSend {
        assertNoSessionLock(sessionID);
        try {
            outboundCracker.crack(message, sessionID);
        } catch (ClassCastException e) {
            throw e;
        } catch (Exception e) {
            // ignore
        }
    }

    @Override
    public void fromAdmin(FIXMessage message, FIXSessionID sessionID) {
        assertNoSessionLock(sessionID);
    }

    @Override
    public void fromApp(FIXMessage message, FIXSessionID sessionID) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        assertNoSessionLock(sessionID);
        inboundCracker.crack(message, sessionID);
    }
}
