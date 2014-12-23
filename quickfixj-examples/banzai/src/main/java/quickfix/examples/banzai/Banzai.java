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

package quickfix.examples.banzai;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.LogFactory;
import org.quickfixj.engine.MessageStoreFactory;
import org.quickfixj.jmx.mbean.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.DefaultEngine;
import quickfix.FileStoreFactory;
import quickfix.Initiator;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.examples.banzai.ui.BanzaiFrame;
import quickfix.mina.initiator.SocketInitiator;

/**
 * Entry point for the Banzai application.
 */
public class Banzai {
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

    private static Logger log = LoggerFactory.getLogger(Banzai.class);
    private static Banzai banzai;
    private boolean initiatorStarted = false;
    private Initiator initiator = null;
    private JFrame frame = null;

    private FIXEngine engine;

    public Banzai(String[] args) throws Exception {
        InputStream inputStream = null;
        if (args.length == 0) {
            inputStream = Banzai.class.getResourceAsStream("banzai.cfg");
        } else if (args.length == 1) {
            inputStream = new FileInputStream(args[0]);
        }
        if (inputStream == null) {
            System.out.println("usage: " + Banzai.class.getName() + " [configFile].");
            return;
        }
        SessionSettings settings = new SessionSettings(inputStream);
        inputStream.close();

        boolean logHeartbeats = Boolean.valueOf(System.getProperty("logHeartbeats", "true"));

        engine = DefaultEngine.getDefaultEngine();

        BanzaiApplication application = new BanzaiApplication(engine);
        MessageStoreFactory messageStoreFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new ScreenLogFactory(true, true, true, logHeartbeats);

        initiator = new SocketInitiator(application, messageStoreFactory, settings, logFactory,
                engine);

        frame = new BanzaiFrame(application);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JmxExporter exporter = new JmxExporter();
        exporter.register(initiator);
    }

    public synchronized void logon() {
        if (!initiatorStarted) {
            try {
                initiator.start();
                initiatorStarted = true;
            } catch (Exception e) {
                log.error("Logon failed", e);
            }
        } else {
            for (FIXSessionID sessionId : initiator.getSessions()) {
                engine.lookupSession(sessionId).logon();
            }
        }
    }

    public void logout() {
        for (FIXSessionID sessionId : initiator.getSessions()) {
            engine.lookupSession(sessionId).logout();
        }
    }

    public void stop() {
        shutdownLatch.countDown();
    }

    public JFrame getFrame() {
        return frame;
    }

    public static Banzai get() {
        return banzai;
    }

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }
        banzai = new Banzai(args);
        if (!System.getProperties().containsKey("openfix")) {
            banzai.logon();
        }
        shutdownLatch.await();
    }

}
