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
import java.util.HashMap;
import java.util.Map;

import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.engine.GraphBuilder;
import org.quickfixj.engine.GraphBuilder.FieldBuilder;
import org.quickfixj.field.GenericField;
import org.quickfixj.spi.DictionaryMetadata.FieldMetadata;
import org.quickfixj.spi.DictionaryMetadata.GroupMetadata;
import org.quickfixj.xml.dictionary.Engine;

/**
 * LazyBuilderSupport provides support for reflection based message prticle creation factories.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
abstract class LazyBuilderSupport<T extends FIXFieldGraph> {

    private final Map<Integer, FieldBuilder> fieldBuilders = new HashMap<Integer, FieldBuilder>();
    private final Map<Integer, GraphBuilder<? extends FIXGroup>> groupBuilders = new HashMap<Integer, GraphBuilder<? extends FIXGroup>>();
    protected final Engine.MessageFactory messageFactory;

    private Constructor<T> constructor;

    LazyBuilderSupport(Engine.MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    /**
     *
     * @since 2.0
     */
    public T create() {
        try {

            if (constructor == null) {
                Class<T> type = (Class<T>) Class.forName(getClassName());
                constructor = type.getConstructor();
            }

            return constructor.newInstance();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @since 2.0
     */
    public FIXField<?> create(int tag, char[] value, int offset, int count) {
        FieldBuilder builder = fieldBuilders.get(tag);
        if (builder != null) {
            return builder.create(value, offset, count);
        }

        // create generic string for any unknown tag
        // and let post-parse validation decide what 
        // to do with unknown fields
        return new GenericField(tag, new String(value, offset, count));
    }

    /**
     *
     * @since 2.0
     */
    public <G extends FIXGroup> GraphBuilder<G> create(FIXGroupField<G> groupField) {
        return (GraphBuilder<G>) groupBuilders.get(groupField.getTag());
    }

    protected void addParticle(Map<Integer, FieldMetadata> particles) {
        for (FieldMetadata particle : particles.values()) {
            if (particle instanceof GroupMetadata) {
                addParticle((GroupMetadata) particle);
            } else {
                addParticle(particle);
            }
        }
    }

    protected void addParticle(FieldMetadata metadata) {
        fieldBuilders.put(metadata.getTag(),
                new LazyFieldBuilder(metadata, messageFactory.getFields()));
    }

    protected void addParticle(GroupMetadata metadata) {
        fieldBuilders.put(metadata.getTag(), new LazyGroupFieldBuilder(metadata, messageFactory));
        groupBuilders.put(metadata.getTag(), new LazyGroupBuilder(metadata, messageFactory));
    }

    /**
     * Get the fieldBuilders property.
     *
     * @return Returns the fieldBuilders.
     * @since 2.0
     */
    public Map<Integer, FieldBuilder> getFieldBuilders() {
        return fieldBuilders;
    }

    /**
     * Get the groupBuilders property.
     *
     * @return Returns the groupBuilders.
     * @since 2.0
     */
    public Map<Integer, GraphBuilder<? extends FIXGroup>> getGroupBuilders() {
        return groupBuilders;
    }

    /**
     * Get the constructor property.
     *
     * @return Returns the constructor.
     * @since 2.0
     */
    protected abstract String getClassName();
}