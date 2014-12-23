/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 21 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.spi.DictionaryMetadata.MessageMetadata;
import org.quickfixj.xml.dictionary.Engine;

/**
 * LazyBuilderSupport provides support for reflection based message prticle creation factories.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class LazyMessageBuilder extends LazyBuilderSupport<FIXMessage> implements FIXMessageBuilder {

    private final FIXApplication application;

    private final MessageMetadata metadata;

    LazyMessageBuilder(FIXApplication application, MessageMetadata metadata,
            Engine.MessageFactory messageFactory) {
        super(messageFactory);
        this.application = application;
        this.metadata = metadata;
        addParticle(metadata.getParticles());
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXBeginString getBeginString() {
        return null;
    }

    /**
    * {@inheritDoc}
    *
    * @since 2.0
    */
    @Override
    public FIXApplication getApplication() {
        return application;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public String getMsgType() {
        return metadata.getMsgType();
    }

    /**
     *
     * @since 2.0
     */
    public String getName() {
        return metadata.getName();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    protected String getClassName() {
        return messageFactory.getMessages().getPackage() + '.' + getName();
    }
}