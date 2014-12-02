/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.MessageBuilder;
import org.quickfixj.MessageBuilder.FieldBuilder;
import org.quickfixj.MessageBuilderFactory;
import org.quickfixj.QFJException;

import quickfix.FieldException;
import quickfix.FixTags;
import quickfix.Message;
import quickfix.SessionRejectReasonText;
import quickfix.StringField;

/**
 * MessageBuilderServiceLoader - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class MessageBuilderServiceLoader {

    private static final MessageBuilderFactory FACTORY = new ServiceLoaderFactory();
    private static final Map<Key, MessageBuilder> BUILDERS = new HashMap<Key, MessageBuilder>();

    static {

        ServiceLoader<MessageBuilderService> serviceLoader = ServiceLoader
                .load(MessageBuilderService.class);

        Map<FIXApplication, ApplicationBuilderProvider> appMap = new HashMap<FIXApplication, ApplicationBuilderProvider>();

        // stash message builders
        for (MessageBuilderService service : serviceLoader) {

            for (ApplicationBuilderProvider provider : service.getApplicationBuilderProviders()) {

                appMap.put(provider.getApplication(), provider);
            }
        }

        // discover and load session message builder services 
        for (MessageBuilderService service : serviceLoader) {

            for (SessionBuilderProvider provider : service.getSessionBuilderProviders()) {

                for (FIXApplication application : provider.getSupportedApplications()) {

                    ApplicationBuilderProvider appProvider = appMap.get(application);

                    if (appProvider != null) {

                        for (MessageBuilder builder : provider.getMessageBuilders()) {

                            Map<Integer, FieldBuilder> fieldBuilders = new HashMap<Integer, FieldBuilder>(
                                    builder.getFieldBuilders());
                            fieldBuilders.putAll(provider.getHeaderFieldBuilders());
                            fieldBuilders.putAll(provider.getTrailerFieldBuilders());

                            BUILDERS.put(
                                    new Key(provider.getBeginString(), application, builder
                                            .getMsgType()), new FIXTMessageBuilder(fieldBuilders,
                                            provider.getBeginString(), builder));
                        }

                        for (MessageBuilder builder : appProvider.getMessageBuilders()) {

                            Map<Integer, FieldBuilder> fieldBuilders = new HashMap<Integer, FieldBuilder>(
                                    builder.getFieldBuilders());
                            fieldBuilders.putAll(provider.getHeaderFieldBuilders());
                            fieldBuilders.putAll(provider.getTrailerFieldBuilders());

                            BUILDERS.put(
                                    new Key(provider.getBeginString(), application, builder
                                            .getMsgType()), new FIXTMessageBuilder(fieldBuilders,
                                            provider.getBeginString(), builder));
                        }
                    }
                }
            }
        }

        System.out.println("MessageBuilderServiceLoader.enclosing_method() " + BUILDERS.size());
    }

    public static final MessageBuilderFactory getMessageBuilderFactory() {

        return FACTORY;
    }

    private static class ServiceLoaderFactory implements MessageBuilderFactory {

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public MessageBuilder getMessageBuilder(FIXBeginString beginString,
                FIXApplication application, String msgType) {

            return BUILDERS.get(new Key(beginString, application, msgType));
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public MessageBuilder getMessageBuilder(FIXBeginString beginString, String msgType) {
            switch (beginString) {
            case FIX40:
                return getMessageBuilder(beginString, FIXApplication.FIX40, msgType);
            case FIX41:
                return getMessageBuilder(beginString, FIXApplication.FIX41, msgType);
            case FIX42:
                return getMessageBuilder(beginString, FIXApplication.FIX42, msgType);
            case FIX43:
                return getMessageBuilder(beginString, FIXApplication.FIX43, msgType);
            case FIX44:
                return getMessageBuilder(beginString, FIXApplication.FIX44, msgType);
            case FIXT11:
                return getMessageBuilder(beginString, FIXApplication.FIX50, msgType);

            default:
                throw new QFJException("Unknown begin string " + beginString);
            }
        }
    }

    private static class FIXTMessageBuilder implements MessageBuilder {

        private final Map<Integer, FieldBuilder> fieldBuilders;

        private final FIXBeginString beginString;

        private final MessageBuilder delegate;

        FIXTMessageBuilder(Map<Integer, FieldBuilder> fieldBuilders, FIXBeginString beginString,
                MessageBuilder delegate) {
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
        public Message create() {
            Message message = delegate.create();
            message.getHeader().setField(new StringField(FixTags.MSG_TYPE, getMsgType()));
            return message;
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
        public Map<Integer, GroupBuilder<?>> getGroupBuilders() {
            return delegate.getGroupBuilders();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXField<?> create(int tag, CharSequence chars) {

            if (chars.length() == 0) {

                throw new FieldException(SessionRejectReasonText.TAG_SPECIFIED_WITHOUT_A_VALUE, tag);
            }

            FieldBuilder builder = fieldBuilders.get(tag);
            if (builder != null) {
                return builder.create(chars);
            }

            throw new FieldException(SessionRejectReasonText.TAG_NOT_DEFINED_FOR_THIS_MESSAGE_TYPE,
                    tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public <G extends FIXGroup> GroupBuilder<G> create(FIXGroupField<G> groupField) {
            return delegate.create(groupField);
        }
    }

    private static class Key {

        private final FIXBeginString beginString;
        private final FIXApplication application;
        private final String msgType;
        private final int hashcode;

        Key(FIXBeginString beginString, FIXApplication application, String msgType) {
            this.beginString = beginString;
            this.application = application;
            this.msgType = msgType;

            int hash = 1;
            if (beginString != null) {
                hash = hash * 17 + beginString.hashCode();
            }
            if (application != null) {
                hash = hash * 31 + application.hashCode();
            }
            if (msgType != null) {
                hash = hash * 13 + msgType.hashCode();
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

            return (this.beginString == key.beginString) && (this.application == key.application)
                    && equals(this.msgType, key.msgType);
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
}
