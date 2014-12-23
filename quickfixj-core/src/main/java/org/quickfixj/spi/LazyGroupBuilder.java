/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 21 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.util.List;

import org.quickfixj.FIXGroup;
import org.quickfixj.engine.GraphBuilder;
import org.quickfixj.spi.DictionaryMetadata.GroupMetadata;
import org.quickfixj.spi.DictionaryMetadata.Metadata;
import org.quickfixj.xml.dictionary.Engine;

/**
 * LazyBuilderSupport provides support for reflection based message prticle creation factories.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class LazyGroupBuilder extends LazyBuilderSupport<FIXGroup> implements
        GraphBuilder<FIXGroup> {
    private final GroupMetadata metadata;

    LazyGroupBuilder(GroupMetadata metadata, Engine.MessageFactory messageFactory) {
        super(messageFactory);
        this.metadata = metadata;
        addParticle(metadata.getParticles());
    }

    /**
     *
     * @since 2.0   
     */
    @Override
    public String getClassName() {

        StringBuilder builder = new StringBuilder();

        if (metadata.isDefinedByComponent()) {
            builder.append(messageFactory.getComponents().getPackage());
        } else {
            builder.append(messageFactory.getMessages().getPackage());
        }
        builder.append('.');

        List<Metadata> path = metadata.getPath();
        for (int i = 0; i < path.size(); i++) {
            if (i != 0) {
                builder.append('$');
            }
            builder.append(path.get(i).getName());
            if (i != 0) {
                builder.append("Group");
            }
        }

        return builder.toString();
    }
}