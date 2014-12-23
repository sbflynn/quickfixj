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

import static quickfix.SessionSettings.BEGINSTRING;
import static quickfix.SessionSettings.SENDERCOMPID;
import static quickfix.SessionSettings.SENDERLOCID;
import static quickfix.SessionSettings.SENDERSUBID;
import static quickfix.SessionSettings.TARGETCOMPID;
import static quickfix.SessionSettings.TARGETLOCID;
import static quickfix.SessionSettings.TARGETSUBID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.quickfixj.FIXBeginString;
import org.quickfixj.QFJException;
import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.MessageStoreFactory;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.LogFactory;

import quickfix.ConfigError;
import quickfix.DefaultSessionFactory;
import quickfix.Session;
import quickfix.SessionFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.mina.SessionConnector;

/**
 * Dynamically defines sessions for an acceptor. This can be useful for
 * applications like simulators that want to accept any connection and
 * dynamically create an associated session.
 *
 * For more complex situations, you can use this class as a starting
 * point for implementing your own AcceptorSessionProvider.
 */
public class DynamicAcceptorSessionProvider implements AcceptorSessionProvider {
    public static final String WILDCARD = "*";
    private static final TemplatePattern ANY_SESSION = new TemplatePattern();

    private final List<TemplateMapping> templateMappings;
    protected final SessionSettings settings;
    protected final SessionFactory sessionFactory;

    /**
     * Mapping from a sessionID pattern to a session template ID.
     */
    public static class TemplateMapping {
        private final TemplatePattern pattern;
        private final FIXSessionID templateID;

        public TemplateMapping(TemplatePattern pattern, FIXSessionID templateID) {
            super();
            this.pattern = pattern;
            this.templateID = templateID;
        }

        public TemplatePattern getPattern() {
            return pattern;
        }

        public FIXSessionID getTemplateID() {
            return templateID;
        }

        @Override
        public String toString() {
            return "<" + pattern + "," + templateID + ">";
        }
    }

    /**
     * Mapping from a sessionID pattern to a session template ID.
     */
    public static class TemplatePattern {
        private final Collection<FIXBeginString> allowedBeginStrings;
        private final String senderCompID;
        private final String senderSubID;
        private final String senderLocationID;
        private final String targetCompID;
        private final String targetSubID;
        private final String targetLocationID;
        private final String sessionQualifier;

        public TemplatePattern() {
            this.allowedBeginStrings = Arrays.asList(FIXBeginString.values());
            this.senderCompID = WILDCARD;
            this.senderSubID = WILDCARD;
            this.senderLocationID = WILDCARD;
            this.targetCompID = WILDCARD;
            this.targetSubID = WILDCARD;
            this.targetLocationID = WILDCARD;
            this.sessionQualifier = WILDCARD;
        }

        public TemplatePattern(Collection<FIXBeginString> allowedBeginStrings, String senderCompID,
                String targetCompID) {
            this.allowedBeginStrings = allowedBeginStrings;
            this.senderCompID = senderCompID;
            this.senderSubID = WILDCARD;
            this.senderLocationID = WILDCARD;
            this.targetCompID = targetCompID;
            this.targetSubID = WILDCARD;
            this.targetLocationID = WILDCARD;
            this.sessionQualifier = WILDCARD;
        }

        public TemplatePattern(Collection<FIXBeginString> allowedBeginStrings, String senderCompID,
                String senderSubID, String senderLocationID, String targetCompID,
                String targetSubID, String targetLocationID, String sessionQualifier) {
            this.allowedBeginStrings = allowedBeginStrings;
            this.senderCompID = senderCompID;
            this.senderSubID = senderSubID;
            this.senderLocationID = senderLocationID;
            this.targetCompID = targetCompID;
            this.targetSubID = targetSubID;
            this.targetLocationID = targetLocationID;
            this.sessionQualifier = sessionQualifier;
        }

        public String getSenderCompID() {
            return senderCompID;
        }

        private boolean isMatching(FIXSessionID sessionID) {
            return allowedBeginStrings.contains(sessionID.getBeginString())
                    && isMatching(senderCompID, sessionID.getSenderCompID())
                    && isMatching(senderSubID, sessionID.getSenderSubID())
                    && isMatching(senderLocationID, sessionID.getSenderLocationID())
                    && isMatching(targetCompID, sessionID.getTargetCompID())
                    && isMatching(targetSubID, sessionID.getTargetSubID())
                    && isMatching(targetLocationID, sessionID.getTargetLocationID());
        }

        private boolean isMatching(String pattern, String value) {
            return WILDCARD.equals(pattern) || (pattern != null && pattern.equals(value));
        }
    }

    /**
     * @param settings session settings
     * @param templateID this is a session ID for a session definition in the session
     *        settings that will be used for default dynamic session values. The BeginString,
     *        SenderCompID, and TargetCompID settings will be replaced with those in the
     *        received logon message.
     * @param application application for the dynamic sessions
     * @param messageStoreFactory message store factory for the dynamic sessions
     * @param logFactory log factory for the dynamic sessions
     * @param messageFactory message factory for the dynamic sessions
     */
    public DynamicAcceptorSessionProvider(final SessionSettings settings,
            final SessionID templateID, quickfix.Application application,
            MessageStoreFactory messageStoreFactory, LogFactory logFactory, FIXEngine engine) {
        this(settings, Collections.singletonList(new TemplateMapping(ANY_SESSION, templateID)),
                application, messageStoreFactory, logFactory, engine);
    }

    /**
     * @param settings session settings
     * @param templateMappings this is a list of session ID patterns mapped to session IDs in
     *        the settings file. The session IDs represent the template for a specified session ID
     *        pattern. The template is used to dynamically create acceptor sessions.
     *        Use "*" to represent a wildcard for a pattern element. For example,
     *        new SessionID("FIX.4.2", "*", "*") would match for any FIX 4.2 session ID.
     *        This allows separate template session configurations for FIX versions (or
     *        CompIDs) being accepted dynamically on a single TCP port.
     * @param application application for the dynamic sessions
     * @param messageStoreFactory message store factory for the dynamic sessions
     * @param logFactory log factory for the dynamic sessions
     * @param messageFactory message factory for the dynamic sessions
     * @see TemplateMapping
     */
    public DynamicAcceptorSessionProvider(final SessionSettings settings,
            List<TemplateMapping> templateMappings, quickfix.Application application,
            MessageStoreFactory messageStoreFactory, LogFactory logFactory, FIXEngine engine) {
        this.settings = settings;
        this.templateMappings = templateMappings;
        sessionFactory = new DefaultSessionFactory(application, messageStoreFactory, logFactory,
                engine);
    }

    @Override
    public synchronized Session getSession(FIXSessionID sessionID, SessionConnector sessionConnector) {
        Session s = Session.lookupSession(sessionID);
        if (s == null) {
            try {
                FIXSessionID templateID = lookupTemplateID(sessionID);
                if (templateID == null) {
                    throw new ConfigError("Unable to find a session template for " + sessionID);
                }
                SessionSettings dynamicSettings = new SessionSettings();
                copySettings(dynamicSettings, settings.getDefaultProperties());
                copySettings(dynamicSettings, settings.getSessionProperties(templateID));
                dynamicSettings.setString(BEGINSTRING, sessionID.getBeginString().getValue());
                dynamicSettings.setString(SENDERCOMPID, sessionID.getSenderCompID());
                optionallySetValue(dynamicSettings, SENDERSUBID, sessionID.getSenderSubID());
                optionallySetValue(dynamicSettings, SENDERLOCID, sessionID.getSenderLocationID());
                dynamicSettings.setString(TARGETCOMPID, sessionID.getTargetCompID());
                optionallySetValue(dynamicSettings, TARGETSUBID, sessionID.getTargetSubID());
                optionallySetValue(dynamicSettings, TARGETLOCID, sessionID.getTargetLocationID());
                s = sessionFactory.create(sessionID, dynamicSettings);
                if (sessionConnector != null) {
                    sessionConnector.addDynamicSession(s);
                }
            } catch (ConfigError e) {
                throw new QFJException(e);
            }
        }
        return s;
    }

    protected void optionallySetValue(SessionSettings dynamicSettings, String key, String value) {
        dynamicSettings.setString(key, value);
    }

    protected FIXSessionID lookupTemplateID(FIXSessionID sessionID) {
        for (TemplateMapping mapping : templateMappings) {
            if (mapping.getPattern().isMatching(sessionID)) {
                return mapping.getTemplateID();
            }
        }
        return null;
    }

    protected void copySettings(SessionSettings settings, Properties properties) {
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            settings.setString((String) e.getKey(), e.getValue().toString());
        }
    }
}
