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

import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.MessageStoreFactory;
import org.quickfixj.messages.bd.fix44.ListStatusRequest;
import org.quickfixj.messages.bd.fix44.TestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultEngine;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Initiator;
import quickfix.MemoryStoreFactory;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.mina.initiator.SocketInitiator;

/**
 * @author <a href="mailto:jhensley@bonddesk.com">John Hensley</a>
 */
public class TimerTestClient extends MessageCracker implements Application {
    private final Logger log = LoggerFactory.getLogger(TimerTestServer.class);
    private final SessionSettings settings = new SessionSettings();
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private boolean failed;

    @Override
    public void fromAdmin(FIXMessage message, FIXSessionID sessionId) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        // no-op
    }

    @Override
    public void fromApp(FIXMessage message, FIXSessionID sessionID) throws FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    @Override
    public void onCreate(FIXSessionID sessionId) {
        // no-op
    }

    @Override
    public void onLogon(FIXSessionID sessionId) {
        // no-op
    }

    @Override
    public void onLogout(FIXSessionID sessionId) {
        // no-op
    }

    public void onMessage(ListStatusRequest message, FIXSessionID sessionID) {
        log.info("got ListStatusRequest");
    }

    private void stop(boolean failed) {
        this.failed = failed;
        shutdownLatch.countDown();
    }

    public void run() throws ConfigError {
        HashMap<Object, Object> defaults = new HashMap<Object, Object>();
        defaults.put("ConnectionType", "initiator");
        defaults.put("HeartBtInt", "2");
        defaults.put("SocketConnectHost", "localhost");
        defaults.put("SocketConnectPort", "9888");
        defaults.put("SocketTcpNoDelay", "Y");
        defaults.put("StartTime", "00:00:00");
        defaults.put("EndTime", "00:00:00");
        defaults.put("SenderCompID", "TW");
        defaults.put("TargetCompID", "ISLD");
        defaults.put("FileStorePath", "target/data/timer_test");
        defaults.put("ValidateUserDefinedFields", "Y");
        settings.set(defaults);

        SessionID sessionID = new SessionID(FIXBeginString.FIX44, "TW", "ISLD");
        settings.setString(sessionID, "BeginString", FIXBeginString.FIX44.getValue());
        settings.setString(sessionID, "DataDictionary", "FIX44.xml");

        MessageStoreFactory storeFactory = new MemoryStoreFactory();
        Initiator initiator = new SocketInitiator(this, storeFactory, settings,
                new ScreenLogFactory(settings), DefaultEngine.getDefaultEngine());
        initiator.start();

        try {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    stop(false);
                }
            }, 5000);

            try {
                shutdownLatch.await();
            } catch (InterruptedException e) {
                // ignore
            }

            if (failed) {
                String message = "TimerTestClient had to send a test request, indicating that the test server was not reliably sending heartbeats.";
                log.error(message);
                throw new RuntimeError(message);
            }
        } finally {
            initiator.stop();
        }
    }

    @Override
    public void toAdmin(FIXMessage message, FIXSessionID sessionId) {
        if (message instanceof TestRequest) {
            stop(true);
        }
    }

    @Override
    public void toApp(FIXMessage message, FIXSessionID sessionId) throws DoNotSend {
        // no-op
    }

    public static void main(String[] args) throws ConfigError {
        TimerTestClient ttc = new TimerTestClient();
        ttc.run();
    }
}
