/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 14 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.FIXSession;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.SessionNotFoundException;
import org.quickfixj.spi.MetadataRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultEngine - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class DefaultEngine implements FIXEngine {

    private final static Logger LOG = LoggerFactory.getLogger(DefaultEngine.class);

    private static DefaultEngine DEFAULT;

    private MetadataRegistry registry = new MetadataRegistry();

    private Map<Key, FIXMessageDictionaryFactory> dictionaries = new HashMap<Key, FIXMessageDictionaryFactory>();

    private Map<Key, FIXMessageBuilderFactory> builderFactories = new HashMap<Key, FIXMessageBuilderFactory>();

    public static synchronized FIXEngine getDefaultEngine() {

        if (DEFAULT == null) {
            DEFAULT = new DefaultEngine();
            DEFAULT.getRegistry().loadServices();
            DEFAULT.build();
        }

        return DEFAULT;
    }

    /**
     * Post construct method - should be called before putting engine into service.
     * 
     * @since 2.0
     */
    public void build() {
        getRegistry().build();
        getRegistry().register(this);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageDictionaryFactory getMessageDictionaryFactory(FIXBeginString beginString,
            String name) {
        return dictionaries.get(new Key(beginString, name));
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageBuilderFactory getMessageBuilderFactory(FIXBeginString beginString, String name) {
        return builderFactories.get(new Key(beginString, name));
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public void registerMessageDictionaryFactory(FIXBeginString beginString, String name,
            FIXMessageDictionaryFactory dictionary) {
        dictionaries.put(new Key(beginString, name), dictionary);
        LOG.info(String.format("Registered data dictionary '%s' for begin string '%s'", name,
                beginString));
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public void registerMessageBuilderFactory(FIXBeginString beginString, String name,
            FIXMessageBuilderFactory messageBuilderFactory) {
        builderFactories.put(new Key(beginString, name), messageBuilderFactory);
        LOG.info(String.format("Registered message builder factory '%s' for begin string '%s'",
                name, beginString));
    }

    /**
     * Get the registry property.
     *
     * @return Returns the registry.
     * @since 2.0
     */
    public MetadataRegistry getRegistry() {
        return registry;
    }

    private class Key {

        private final FIXBeginString beginString;
        private final String name;
        private final int hashcode;

        private Key(FIXBeginString beginString, String name) {
            this.beginString = beginString;
            this.name = name;

            int hash = 1;
            if (beginString != null) {
                hash = hash * 17 + beginString.hashCode();
            }

            if (name != null) {
                hash = hash * 13 + name.hashCode();
            }

            hashcode = hash;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public int hashCode() {
            return hashcode;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean equals(Object obj) {

            Key key = (Key) obj;

            return (this.beginString == key.beginString) && equals(this.name, key.name);
        }

        public boolean equals(Object o1, Object o2) {

            if (o1 == o2) {
                return true;
            }

            if (o1 != null) {
                return o1.equals(o2);
            }

            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public void registerSession(FIXSession session) {
        Session.registerSession(session);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXSession lookupSession(FIXSessionID sessionID) {
        return Session.lookupSession(sessionID);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public void unregisterSessions(List<FIXSessionID> sessionIds) {
        Session.unregisterSessions(sessionIds);
    }

    /**
     * {@inheritDoc}
     * @throws SessionNotFoundException 
     *
     * @since TODO
     */
    @Override
    public boolean send(FIXMessage message, FIXSessionID sessionID) throws SessionNotFoundException {
        return Session.sendToTarget(message, sessionID);
    }
}
