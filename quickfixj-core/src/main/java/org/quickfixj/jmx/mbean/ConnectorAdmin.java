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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import org.quickfixj.QFJException;
import org.quickfixj.engine.FIXSession;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.jmx.openmbean.TabularDataAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Acceptor;
import quickfix.Connector;
import quickfix.Initiator;
import quickfix.Session;
import quickfix.SessionSettings;
import quickfix.mina.SessionConnector;

abstract class ConnectorAdmin implements ConnectorAdminMBean, MBeanRegistration {
    private Logger log = LoggerFactory.getLogger(getClass());

    public final static String ACCEPTOR_ROLE = "ACCEPTOR";

    public final static String INITIATOR_ROLE = "INITIATOR";

    private final Connector connector;

    private static final TabularDataAdapter tabularDataAdapter = new TabularDataAdapter();

    private final SessionJmxExporter sessionExporter;

    private final JmxExporter jmxExporter;

    private final ObjectName connectorName;

    private final List<ObjectName> sessionNames = new ArrayList<ObjectName>();

    private final SessionSettings settings;

    private String role = "N/A";

    private MBeanServer mbeanServer;

    public ConnectorAdmin(JmxExporter jmxExporter, Connector connector, ObjectName connectorName,
            SessionSettings settings, SessionJmxExporter sessionExporter) {
        this.jmxExporter = jmxExporter;
        this.connectorName = connectorName;
        this.settings = settings;
        this.sessionExporter = sessionExporter;
        if (connector instanceof Acceptor) {
            role = ACCEPTOR_ROLE;
        } else if (connector instanceof Initiator) {
            role = INITIATOR_ROLE;
        }
        this.connector = connector;
    }

    @Override
    public String getRole() {
        return role;
    }

    public static class ConnectorSession {
        private FIXSession session;
        private ObjectName sessionName;

        public ConnectorSession(FIXSession session, ObjectName sessionName) {
            this.session = session;
            this.sessionName = sessionName;
        }

        public boolean isLoggedOn() {
            return session.isLoggedOn();
        }

        public FIXSessionID getSessionID() {
            return session.getSessionID();
        }

        public ObjectName getSessionName() {
            return sessionName;
        }

        public String getRemoteAddress() {
            String remoteAddress = session.getRemoteAddress();
            return remoteAddress != null ? remoteAddress : "N/A";
        }
    }

    @Override
    public TabularData getSessions() throws IOException {
        List<ConnectorSession> sessions = new ArrayList<ConnectorSession>();
        for (FIXSessionID sessionID : connector.getSessions()) {
            FIXSession session = Session.lookupSession(sessionID);
            sessions.add(new ConnectorSession(session, sessionExporter.getSessionName(sessionID)));
        }
        try {
            return tabularDataAdapter.fromBeanList("Sessions", "Session", "sessionID", sessions);
        } catch (OpenDataException e) {
            throw JmxSupport.toIOException(e);
        }
    }

    public TabularData getLoggedOnSessions() throws OpenDataException {
        List<ObjectName> names = new ArrayList<ObjectName>();
        for (FIXSessionID sessionID : connector.getSessions()) {
            FIXSession session = Session.lookupSession(sessionID);
            if (session.isLoggedOn()) {
                names.add(sessionExporter.getSessionName(sessionID));
            }
        }
        return tabularDataAdapter.fromArray("Sessions", "SessionID", toObjectNameArray(names));
    }

    private ObjectName[] toObjectNameArray(List<ObjectName> sessions) {
        return sessions.toArray(new ObjectName[sessions.size()]);
    }

    @Override
    public void stop(boolean force) {
        log.info("JMX operation: stop " + getRole() + " " + this);
        connector.stop(force);
    }

    @Override
    public String getHostName() {
        try {
            return InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            return "[UNKNOWN]";
        }
    }

    @Override
    public void stop() {
        stop(false);
    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.mbeanServer = server;
        return name;
    }

    @Override
    public void postRegister(Boolean registrationDone) {
        if (connector instanceof SessionConnector) {
            ((SessionConnector) connector).addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (SessionConnector.SESSIONS_PROPERTY.equals(evt.getPropertyName())) {
                        registerSessions();
                    }
                }
            });
        }
        registerSessions();
    }

    private void registerSessions() {
        for (FIXSessionID sessionID : connector.getSessions()) {
            if (sessionExporter.getSessionName(sessionID) == null) {
                try {
                    final ObjectName name = sessionExporter.register(jmxExporter,
                            Session.lookupSession(sessionID), connectorName, settings);
                    sessionNames.add(name);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new QFJException("Connector MBean postregistration failed", e);
                }
            }
        }
    }

    @Override
    public void preDeregister() throws Exception {
        //no-op
    }

    @Override
    public void postDeregister() {
        for (ObjectName sessionName : sessionNames) {
            try {
                mbeanServer.unregisterMBean(sessionName);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new QFJException("Connector MBean postregistration failed", e);
            }
        }
    }
}
