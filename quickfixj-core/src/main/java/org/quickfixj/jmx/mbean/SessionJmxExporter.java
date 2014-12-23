/*******************************************************************************
 * Copyright (c) quickfixj.org  All rights reserved.
 *
 * This file is part of the QuickFIX/J FIX Engine
 *
 * This file may be distributed under the terms of the quickfixj.org
 * license as defined by quickfixj.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixj.org/LICENSE for licensing information.
 *
 ******************************************************************************/

package org.quickfixj.jmx.mbean;

import static quickfix.SessionID.NOT_SET;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.quickfixj.engine.FIXSession;
import org.quickfixj.engine.FIXSession.FIXSessionID;

import quickfix.ConfigError;
import quickfix.Session;
import quickfix.SessionSettings;

public class SessionJmxExporter {
    private Map<FIXSessionID, ObjectName> sessionObjectNames = new HashMap<FIXSessionID, ObjectName>();

    public ObjectName register(JmxExporter jmxExporter, FIXSession session,
            ObjectName connectorName, SessionSettings settings) throws JMException, ConfigError {

        if (session instanceof Session) {
            Session sessionImpl = (Session) session;
            ObjectNameFactory settingsNameFactory = new ObjectNameFactory();
            settingsNameFactory.addProperty("type", "Settings");
            addSessionIdProperties(session.getSessionID(), settingsNameFactory);
            ObjectName settingsName = settingsNameFactory.createName();
            jmxExporter.registerMBean(
                    new SessionSettingsAdmin(sessionImpl.getSessionID(), settings), settingsName);

            ObjectName sessionName = createSessionName(session.getSessionID());
            sessionObjectNames.put(session.getSessionID(), sessionName);
            SessionAdmin sessionAdmin = new SessionAdmin(sessionImpl, connectorName, settingsName);
            sessionImpl.addStateListener(sessionAdmin);
            jmxExporter.registerMBean(sessionAdmin, sessionName);
            return sessionName;
        }

        return null;
    }

    public ObjectName getSessionName(FIXSessionID sessionID) {
        return sessionObjectNames.get(sessionID);
    }

    public ObjectName createSessionName(FIXSessionID sessionID) throws MalformedObjectNameException {
        TreeMap<String, String> properties = new TreeMap<String, String>();
        properties.put("type", "Session");
        ObjectNameFactory nameFactory = new ObjectNameFactory();
        nameFactory.addProperty("type", "Session");
        addSessionIdProperties(sessionID, nameFactory);
        return nameFactory.createName();
    }

    private void addSessionIdProperties(FIXSessionID sessionID, ObjectNameFactory nameFactory) {
        nameFactory.addProperty("beginString", sessionID.getBeginString().getValue());
        nameFactory.addProperty("senderCompID", sessionID.getSenderCompID());
        optionallyAddProperty(nameFactory, "senderSubID", sessionID.getSenderSubID());
        optionallyAddProperty(nameFactory, "senderLocationID", sessionID.getSenderLocationID());
        nameFactory.addProperty("targetCompID", sessionID.getTargetCompID());
        optionallyAddProperty(nameFactory, "targetSubID", sessionID.getTargetSubID());
        optionallyAddProperty(nameFactory, "targetLocationID", sessionID.getTargetLocationID());
        optionallyAddProperty(nameFactory, "qualifier", sessionID.getSessionQualifier());
    }

    private void optionallyAddProperty(ObjectNameFactory nameFactory, String key, String value) {
        if (!value.equals(NOT_SET)) {
            nameFactory.addProperty(key, value);
        }
    }

}
