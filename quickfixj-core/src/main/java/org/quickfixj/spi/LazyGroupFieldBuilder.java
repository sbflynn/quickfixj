/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 21 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.lang.reflect.Constructor;
import java.util.List;

import org.quickfixj.FIXField;
import org.quickfixj.engine.GraphBuilder.FieldBuilder;
import org.quickfixj.spi.DictionaryMetadata.GroupMetadata;
import org.quickfixj.spi.DictionaryMetadata.Metadata;
import org.quickfixj.xml.dictionary.Engine.MessageFactory;

/**
 * LazyBuilderSupport provides support for reflection based message prticle creation factories.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class LazyGroupFieldBuilder implements FieldBuilder {

    private final GroupMetadata metadata;
    private final MessageFactory factory;

    private Constructor<?> constructor;

    LazyGroupFieldBuilder(GroupMetadata metadata, MessageFactory messageFactory) {
        this.metadata = metadata;
        this.factory = messageFactory;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public int getTag() {
        return metadata.getTag();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0   
     */
    @Override
    public String getName() {
        return metadata.getName();
    }

    /**
     *
     * @since 2.0   
     */
    public String getClassName() {

        StringBuilder builder = new StringBuilder();

        if (metadata.isDefinedByComponent()) {
            builder.append(factory.getComponents().getPackage());
        } else {
            builder.append(factory.getMessages().getPackage());
        }
        builder.append('.');

        List<Metadata> path = metadata.getPath();
        for (int i = 0; i < path.size(); i++) {
            if (i != 0) {
                builder.append('$');
            }
            builder.append(path.get(i).getName());
            if (i != 0 && i + 1 < path.size()) {
                builder.append("Group");
            }
        }

        return builder.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXField<?> create(char[] value, int offset, int count) {
        try {

            if (constructor == null) {
                Class<?> type = Class.forName(getClassName());
                constructor = type.getConstructor(char[].class, int.class, int.class);
            }

            return (FIXField<?>) constructor.newInstance(value, offset, count);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}