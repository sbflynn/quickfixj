/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 7 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package quickfix;

import java.util.Collections;
import java.util.Map;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.GraphBuilder;
import org.quickfixj.field.GenericField;

/**
 * GenericMessageBuilder - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class GenericMessageBuilder implements FIXMessageBuilder {

    private final FIXBeginString beginString;
    private final FIXMessageDictionary dictionary;

    GenericMessageBuilder(FIXBeginString beginString, FIXMessageDictionary dictionary) {
        this.beginString = beginString;
        this.dictionary = dictionary;
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
        return dictionary.getApplication();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public String getMsgType() {
        return dictionary.getMsgType();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public Message create() {
        Message message = new Message();
        message.setMsgType(getMsgType());
        return message;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXField<?> create(int tag, char[] value, int offset, int count) {
        return new GenericField(tag, value, offset, count);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public <T extends FIXGroup> GraphBuilder<T> create(FIXGroupField<T> groupField) {
        return new GenericGroupBuilder<T>(groupField);
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public Map<Integer, FieldBuilder> getFieldBuilders() {
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public Map<Integer, GraphBuilder<? extends FIXGroup>> getGroupBuilders() {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 2.0
     */
    private class GenericGroupBuilder<T extends FIXGroup> implements GraphBuilder<T> {

        private final FIXGroupField<T> field;

        GenericGroupBuilder(FIXGroupField<T> field) {
            this.field = field;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public T create() {
            return (T) new Group(field.getTag(), field.getDelimiterField());
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXField<?> create(int tag, char[] value, int offset, int count) {
            return new GenericField(tag, value, offset, count);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public <T extends FIXGroup> GraphBuilder<T> create(FIXGroupField<T> groupField) {
            return new GenericGroupBuilder<T>(groupField);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Map<Integer, FieldBuilder> getFieldBuilders() {
            return Collections.emptyMap();
        }
    }
}