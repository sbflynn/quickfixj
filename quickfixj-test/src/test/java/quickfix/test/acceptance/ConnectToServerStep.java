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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.mina.ProtocolFactory;
import junit.framework.Assert;
import junit.framework.TestResult;

public class ConnectToServerStep implements TestStep {
    private Logger log = LoggerFactory.getLogger(getClass());
    private static final Pattern CONNECT_PATTERN = Pattern.compile("i(\\d+)*,?CONNECT");
    private String command;
    private int clientId = 0;
    private int transportType = ProtocolFactory.SOCKET;
    private final int port;

    public ConnectToServerStep(String command, int transportType, int port) {
        this.command = command;
        this.transportType = transportType;
        this.port = port;
    }

    @Override
    public void run(TestResult result, TestConnection connection) {
        Matcher matcher = CONNECT_PATTERN.matcher(command);
        if (matcher.lookingAt()) {
            if (matcher.group(1) != null) {
                clientId = Integer.parseInt(matcher.group(1));
            } else {
                clientId = 1;
            }
        } else {
            Assert.fail("incorrect connect command: " + command);
        }
        log.debug("connecting to client " + clientId);
        long reconnectDelay = Long.getLong("atest.reconnectDelay", 50L);
        if (reconnectDelay > 0) {
            try {
                Thread.sleep(reconnectDelay);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        try {
            connection.connect(clientId, transportType, port);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "connect to server: " + command;
    }
}
