/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 14 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.util.Map;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.engine.GraphBuilder;
import org.quickfixj.engine.SessionRejectReason;
import org.quickfixj.field.FieldException;
import org.quickfixj.field.GenericField;

/**
 * SystemMessageBuilder - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class SystemMessageBuilder implements FIXMessageBuilder {

    private final Map<Integer, FieldBuilder> fieldBuilders;

    private final FIXBeginString beginString;

    private final FIXMessageBuilder delegate;

    SystemMessageBuilder(Map<Integer, FieldBuilder> fieldBuilders, FIXBeginString beginString,
            FIXMessageBuilder delegate) {
        this.fieldBuilders = fieldBuilders;
        this.beginString = beginString;
        this.delegate = delegate;
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
    public FIXApplication getApplication() {
        return delegate.getApplication();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public String getMsgType() {
        return delegate.getMsgType();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessage create() {
        return delegate.create();
    }

    /**
      * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public Map<Integer, FieldBuilder> getFieldBuilders() {
        return delegate.getFieldBuilders();
    }

    /**
       * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public Map<Integer, GraphBuilder<? extends FIXGroup>> getGroupBuilders() {
        return delegate.getGroupBuilders();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXField<?> create(int tag, char[] value, int offset, int count) {

        if (count == 0) {

            throw new FieldException(SessionRejectReason.TAG_SPECIFIED_WITHOUT_A_VALUE,
                    SessionRejectReason
                            .getMessage(SessionRejectReason.TAG_SPECIFIED_WITHOUT_A_VALUE), tag);
        }

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
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public <G extends FIXGroup> GraphBuilder<G> create(FIXGroupField<G> groupField) {
        return delegate.create(groupField);
    }
}