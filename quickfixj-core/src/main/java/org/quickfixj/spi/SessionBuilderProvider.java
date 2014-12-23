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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quickfixj.FIXBeginString;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.engine.GraphBuilder.FieldBuilder;
import org.quickfixj.spi.DictionaryMetadata.FieldMetadata;
import org.quickfixj.spi.DictionaryMetadata.GroupMetadata;
import org.quickfixj.spi.DictionaryMetadata.MessageMetadata;
import org.quickfixj.spi.DictionaryMetadata.TransportMetadata;
import org.quickfixj.xml.dictionary.Engine;

/**
 * Internal management store used by {@link MetadataRegistry}.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class SessionBuilderProvider {

    private final List<FIXMessageBuilder> messageBuilders = new ArrayList<FIXMessageBuilder>();
    private final Map<Integer, FieldBuilder> headerFieldBuilders = new HashMap<Integer, FieldBuilder>();
    private final Map<Integer, FieldBuilder> trailerFieldBuilders = new HashMap<Integer, FieldBuilder>();

    private final TransportMetadata metadata;
    private final Engine.MessageFactory messageFactory;

    SessionBuilderProvider(TransportMetadata metadata, Engine.MessageFactory messageFactory) {

        this.metadata = metadata;
        this.messageFactory = messageFactory;

        for (MessageMetadata md : metadata.getMessageMetadata()) {
            messageBuilders.add(new LazyMessageBuilder(null, md, messageFactory));
        }

        for (FieldMetadata header : metadata.getHeaderMetadata().values()) {
            if (header instanceof GroupMetadata) {
                headerFieldBuilders.put(header.getTag(), new LazyGroupFieldBuilder(
                        (GroupMetadata) header, messageFactory));
            } else {
                headerFieldBuilders.put(header.getTag(), new LazyFieldBuilder(header,
                        messageFactory.getFields()));
            }
        }

        for (FieldMetadata trailer : metadata.getTrailerMetadata().values()) {
            trailerFieldBuilders.put(trailer.getTag(),
                    new LazyFieldBuilder(trailer, messageFactory.getFields()));
        }
    }

    /**
     * @since 2.0
     */
    public FIXBeginString getBeginString() {
        return metadata.getBeginString();
    }

    /**
     * @since 2.0
     */
    public TransportMetadata getTransportMetadata() {
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
    public Map<Integer, FieldBuilder> getHeaderFieldBuilders() {
        return headerFieldBuilders;
    }

    /**
     * @since 2.0
     */
    public Map<Integer, FieldBuilder> getTrailerFieldBuilders() {
        return trailerFieldBuilders;
    }
}