/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 14 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.util.HashMap;
import java.util.Map;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionary;

/**
 * SystemMessageBuilderFactory - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class SystemMessageBuilderFactory implements FIXMessageBuilderFactory {

    private final Map<String, FIXMessageBuilder> transportBuilders = new HashMap<String, FIXMessageBuilder>();
    private final Map<MessageTypeKey, FIXMessageBuilder> messageBuilders = new HashMap<MessageTypeKey, FIXMessageBuilder>();

    private final String namespace;
    private final FIXBeginString beginString;

    SystemMessageBuilderFactory(String namespace, FIXBeginString beginString) {
        this.namespace = namespace;
        this.beginString = beginString;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public String getNamespace() {
        return namespace;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXBeginString getBeginString() {
        return beginString;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageBuilder getMessageBuilder(String msgType) {
        return transportBuilders.get(msgType);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageBuilder getMessageBuilder(FIXMessageDictionary dictionary) {

        return getMessageBuilder(dictionary.getApplication(), dictionary.getMsgType());
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageBuilder getMessageBuilder(FIXApplication application, String msgType) {

        return messageBuilders.get(new MessageTypeKey(application, msgType));
    }

    void addTransportBuilder(String msgType, FIXMessageBuilder builder) {
        transportBuilders.put(msgType, builder);
    }

    void addMessageBuilder(FIXApplication application, String msgType, FIXMessageBuilder builder) {
        messageBuilders.put(new MessageTypeKey(application, msgType), builder);
    }
}