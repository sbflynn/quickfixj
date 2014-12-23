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

package quickfix.test.acceptance.timer;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import org.apache.mina.util.AvailablePortFinder;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.MessageStoreFactory;
import org.quickfixj.engine.SessionNotFoundException;
import org.quickfixj.messages.bd.fix44.ListStatusRequest;
import org.quickfixj.messages.bd.fix44.Logon;
import org.quickfixj.messages.bd.fix44.field.ListID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.DefaultEngine;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.MemoryStoreFactory;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.mina.acceptor.SocketAcceptor;

/**
 * @author <a href="mailto:jhensley@bonddesk.com">John Hensley</a>
 */
public class TimerTestServer extends MessageCracker implements Application, Runnable {
    SocketAcceptor acceptor;
    private final CountDownLatch initializationLatch = new CountDownLatch(1);
    private final Logger log = LoggerFactory.getLogger(TimerTestServer.class);
    private final SessionSettings settings = new SessionSettings();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private class DelayedTestRequest extends TimerTask {
        FIXSessionID session;

        DelayedTestRequest(FIXSessionID sessionID) {
            this.session = sessionID;
        }

        @Override
        public void run() {
            try {
                log.info("Sending offset message");
                ListStatusRequest lsr = new ListStatusRequest(new ListID("somelist"));
                Session.sendToTarget(lsr, this.session);
            } catch (SessionNotFoundException sessionNotFound) {
                // not going to happen
            }
        }
    }

    @Override
    public void fromAdmin(FIXMessage message, FIXSessionID sessionId) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // sleep to move our timer off from the client's
        if (message instanceof Logon) {
            new Timer().schedule(new DelayedTestRequest(sessionId), 3000);
        }
    }

    @Override
    public void fromApp(FIXMessage message, FIXSessionID sessionId) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        //no-op
    }

    @Override
    public void onCreate(FIXSessionID sessionId) {
        //no-op
    }

    @Override
    public void onLogon(FIXSessionID sessionId) {
        //no-op
    }

    @Override
    public void onLogout(FIXSessionID sessionId) {
        log.info("logout");
        shutdownLatch.countDown();
    }

    @Override
    public void run() {
        try {
            HashMap<Object, Object> defaults = new HashMap<Object, Object>();
            defaults.put("ConnectionType", "acceptor");
            defaults.put("SocketAcceptPort",
                    Integer.toString(AvailablePortFinder.getNextAvailable(10000)));
            defaults.put("StartTime", "00:00:00");
            defaults.put("EndTime", "00:00:00");
            defaults.put("SenderCompID", "ISLD");
            defaults.put("TargetCompID", "TW");
            defaults.put("FileStorePath", "target/data/server");
            defaults.put("ValidateUserDefinedFields", "Y");
            defaults.put("ResetOnDisconnect", "Y");
            settings.set(defaults);

            SessionID sessionID = new SessionID(FIXBeginString.FIX44, "ISLD", "TW");
            settings.setString(sessionID, "BeginString", FIXBeginString.FIX44.getValue());
            settings.setString(sessionID, "DataDictionary", FIXBeginString.FIX44.getValue()
                    .replaceAll("\\.", "") + ".xml");

            MessageStoreFactory factory = new MemoryStoreFactory();
            acceptor = new SocketAcceptor(this, factory, settings, new ScreenLogFactory(settings),
                    DefaultEngine.getDefaultEngine());
            acceptor.start();
            try {
                //acceptor.waitForInitialization();
                initializationLatch.countDown();

                try {
                    shutdownLatch.await();
                } catch (InterruptedException e) {
                    // ignore
                }

                log.info("TimerTestServer shutting down.");
            } finally {
                acceptor.stop();
            }
        } catch (Throwable e) {
            log.error("Error in TimerTestServer server: ", e);
            initializationLatch.countDown();
        }
    }

    @Override
    public void toAdmin(FIXMessage message, FIXSessionID sessionId) {
        //no-op
    }

    @Override
    public void toApp(FIXMessage message, FIXSessionID sessionId) throws DoNotSend {
        //no-op
    }

    public void waitForInitialization() throws InterruptedException {
        initializationLatch.await();
    }

    public static void main(String[] args) {
        TimerTestServer server = new TimerTestServer();
        server.run();
    }
}
