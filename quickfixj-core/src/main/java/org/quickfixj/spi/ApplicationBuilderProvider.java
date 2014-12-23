/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 21 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.util.ArrayList;
import java.util.List;

import org.quickfixj.FIXApplication;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.spi.DictionaryMetadata.ApplicationMetadata;
import org.quickfixj.spi.DictionaryMetadata.MessageMetadata;
import org.quickfixj.xml.dictionary.Engine;

/**
 * Internal management store used by {@link MetadataRegistry}.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class ApplicationBuilderProvider {

    private final List<FIXMessageBuilder> messageBuilders = new ArrayList<FIXMessageBuilder>();

    private final ApplicationMetadata metadata;

    ApplicationBuilderProvider(ApplicationMetadata metadata, Engine.MessageFactory messageFactory) {

        this.metadata = metadata;

        for (MessageMetadata md : metadata.getMessageMetadata()) {
            messageBuilders.add(new LazyMessageBuilder(metadata.getApplication(), md,
                    messageFactory));
        }
    }

    /**
     * @since 2.0
     */
    public ApplicationMetadata getApplicationMetadata() {
        return metadata;
    }

    /**
     * @since 2.0
     */
    public List<FIXMessageBuilder> getMessageBuilders() {
        return messageBuilders;
    }

    /**
     * @since 2.0
     */
    public FIXApplication getApplication() {
        return metadata.getApplication();
    }
}