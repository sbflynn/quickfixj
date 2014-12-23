/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 8 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXFieldType;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.GraphDictionary.FieldDictionary;
import org.quickfixj.engine.GraphDictionary.GroupDictionary;
import org.quickfixj.spi.DictionaryMetadata.ApplicationMetadata;
import org.quickfixj.spi.DictionaryMetadata.FieldMetadata;
import org.quickfixj.spi.DictionaryMetadata.GroupMetadata;
import org.quickfixj.spi.DictionaryMetadata.MessageMetadata;
import org.quickfixj.spi.DictionaryMetadata.TransportMetadata;
import org.quickfixj.xml.dictionary.FieldDefinition.Value;

/**
 * SystemDataDictionary - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
class MetadataDictionaryFactory implements FIXMessageDictionaryFactory {

    private final Set<Integer> fields = new HashSet<Integer>();
    private final Map<MessageTypeKey, FIXMessageDictionary> messageDictionaries = new HashMap<MessageTypeKey, FIXMessageDictionary>();
    private final String namespace;
    private final TransportMetadata transportMetadata;

    public MetadataDictionaryFactory(String namespace, TransportMetadata transportMetadata) {
        this.namespace = namespace;
        this.transportMetadata = transportMetadata;
        fields.addAll(transportMetadata.getAllTags());
    }

    public void add(ApplicationMetadata applicationMetadata) {
        fields.addAll(applicationMetadata.getAllTags());

        for (MessageMetadata messageMetadata : transportMetadata.getMessageMetadata()) {

            FIXMessageDictionary dictionary = new MessageDictionaryImpl(applicationMetadata,
                    messageMetadata, transportMetadata);

            messageDictionaries.put(new MessageTypeKey(null, messageMetadata.getMsgType()),
                    dictionary);
            messageDictionaries.put(new MessageTypeKey(applicationMetadata.getApplication(),
                    messageMetadata.getMsgType()), dictionary);

        }

        for (MessageMetadata messageMetadata : applicationMetadata.getMessageMetadata()) {
            messageDictionaries.put(new MessageTypeKey(applicationMetadata.getApplication(),
                    messageMetadata.getMsgType()), new MessageDictionaryImpl(applicationMetadata,
                    messageMetadata, transportMetadata));
        }
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
        return transportMetadata.getBeginString();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public FIXMessageDictionary getMessageDictionary(FIXApplication application, String msgType) {

        if (application == null) {
            switch (getBeginString()) {
            case FIX40:
                application = FIXApplication.FIX40;
                break;
            case FIX41:
                application = FIXApplication.FIX41;
                break;
            case FIX42:
                application = FIXApplication.FIX42;
                break;
            case FIX43:
                application = FIXApplication.FIX43;
                break;
            case FIX44:
                application = FIXApplication.FIX44;
                break;
            default:
                break;
            }
        }

        return messageDictionaries.get(new MessageTypeKey(application, msgType));
    }

    /**
     * Predicate for determining if a field has enumerated values.
     *
     * @param field the tag
     * @return true if field is enumerated, false otherwise
     */
    public boolean hasFieldValue(int field, FieldMetadata metadata) {

        List<Value> values = metadata.getFieldDefinition().getValue();
        return !values.isEmpty();
    }

    /**
     * Predicate for determining if a field value is valid
     *
     * @param field the tag
     * @param value a possible field value
     * @return true if field value is valid, false otherwise
     */
    public boolean isFieldValue(int field, FieldMetadata metadata, String value) {

        List<Value> validValues = metadata.getFieldDefinition().getValue();

        if (validValues == null || validValues.isEmpty()) {
            return false;
        }

        if (metadata.getFieldDefinition().isAllowOtherValues()) {
            return true;
        }

        if (metadata.getFieldDefinition().getType() != FIXFieldType.MULTIPLESTRINGVALUE) {
            return validValues.contains(value);
        }

        // MultipleValueString
        for (String val : value.split(" ")) {
            if (!validValues.contains(val)) {
                return false;
            }
        }

        return true;
    }

    private class MessageDictionaryImpl implements FIXMessageDictionary {
        private final Set<Integer> requiredFields = new HashSet<Integer>();
        private final Map<Integer, GroupDictionaryImpl> groupDictionaries = new HashMap<Integer, GroupDictionaryImpl>();
        private final Map<Integer, FieldDictionary> fieldDictionaries = new HashMap<Integer, FieldDictionary>();
        private final ApplicationMetadata applicationMetadata;
        private final TransportMetadata transportMetadata;
        private final MessageMetadata metadata;

        MessageDictionaryImpl(ApplicationMetadata applicationMetadata,
                MessageMetadata messageMetadata, TransportMetadata transportMetadata) {
            this.applicationMetadata = applicationMetadata;
            this.metadata = messageMetadata;
            this.transportMetadata = transportMetadata;
            for (FieldMetadata particle : messageMetadata.getParticles().values()) {
                fieldDictionaries.put(particle.getTag(), new FieldDictionaryImpl(particle));
                if (particle instanceof GroupMetadata) {
                    GroupMetadata groupMetadata = (GroupMetadata) particle;
                    groupDictionaries.put(groupMetadata.getTag(), new GroupDictionaryImpl(this,
                            groupMetadata));
                }

                if (particle.isRequired()) {
                    requiredFields.add(particle.getTag());
                }
            }
            for (FieldMetadata particle : transportMetadata.getHeaderMetadata().values()) {
                fieldDictionaries.put(particle.getTag(), new FieldDictionaryImpl(particle));
                if (particle instanceof GroupMetadata) {
                    GroupMetadata groupMetadata = (GroupMetadata) particle;
                    groupDictionaries.put(groupMetadata.getTag(), new GroupDictionaryImpl(this,
                            groupMetadata));
                }
            }
            for (FieldMetadata particle : transportMetadata.getTrailerMetadata().values()) {
                fieldDictionaries.put(particle.getTag(), new FieldDictionaryImpl(particle));
                if (particle instanceof GroupMetadata) {
                    GroupMetadata groupMetadata = (GroupMetadata) particle;
                    groupDictionaries.put(groupMetadata.getTag(), new GroupDictionaryImpl(this,
                            groupMetadata));
                }
            }
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
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXApplication getApplication() {
            return applicationMetadata.getApplication();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Set<Integer> getRequiredFields() {
            return requiredFields;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isValid(int tag) {
            return fields.contains(tag);
        }

        /**
         * @param tag
         * @since 2.0
         */
        @Override
        public boolean isGroupField(int tag) {
            return groupDictionaries.containsKey(tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isField(int tag) {
            return metadata.getParticles().containsKey(tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isTrailerField(int tag) {
            return transportMetadata.getTrailerMetadata().containsKey(tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isHeaderField(int tag) {
            return transportMetadata.getHeaderMetadata().containsKey(tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public GroupDictionaryImpl getGroupDictionary(int tag) {

            return groupDictionaries.get(tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FieldDictionary getFieldDictionary(int tag) {

            return fieldDictionaries.get(tag);
        }
    }

    private class GroupDictionaryImpl implements GroupDictionary {

        private final Set<Integer> requiredFields = new HashSet<Integer>();
        private final Map<Integer, GroupDictionary> groupDictionaries = new HashMap<Integer, GroupDictionary>();
        private final Map<Integer, FieldDictionary> fieldDictionaries = new HashMap<Integer, FieldDictionary>();
        private final List<Integer> orderedFields;
        private final GroupMetadata metadata;

        GroupDictionaryImpl(FIXMessageDictionary messageDictionary, GroupMetadata metadata) {
            this.metadata = metadata;
            for (FieldMetadata particle : metadata.getParticles().values()) {
                fieldDictionaries.put(particle.getTag(), new FieldDictionaryImpl(particle));
                if (particle instanceof GroupMetadata) {
                    GroupMetadata groupMetadata = (GroupMetadata) particle;
                    groupDictionaries.put(groupMetadata.getTag(), new GroupDictionaryImpl(
                            messageDictionary, groupMetadata));
                }

                if (particle.isRequired()) {
                    requiredFields.add(particle.getTag());
                }
            }

            orderedFields = new ArrayList<Integer>(metadata.getParticles().keySet());
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Set<Integer> getRequiredFields() {
            return requiredFields;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isValid(int tag) {
            return fields.contains(tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public int getDelimiterField() {
            return metadata.getParticles().keySet().iterator().next();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean isField(int tag) {
            return metadata.getParticles().containsKey(tag);
        }

        /**
         * @param tag
         * @since 2.0
         */
        @Override
        public boolean isGroupField(int tag) {
            return groupDictionaries.containsKey(tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public GroupDictionary getGroupDictionary(int tag) {
            return groupDictionaries.get(tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FieldDictionary getFieldDictionary(int tag) {
            return fieldDictionaries.get(tag);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public List<Integer> getOrderedFields() {
            return orderedFields;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public String toString() {
            return String.format("GRP DICT : %s (%d)", metadata.getName(), metadata.getParticles()
                    .size());
        }
    }

    private class FieldDictionaryImpl implements FieldDictionary {
        private final FieldMetadata metadata;

        FieldDictionaryImpl(FieldMetadata metadata) {
            this.metadata = metadata;
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
         * {@inheritDoc}
         *
         * @since 2.0
         * @deprecated
         */
        @Deprecated
        @Override
        public boolean isDataField() {
            return getType() == FIXFieldType.DATA;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Set<String> getFieldValues() {
            return metadata.getValues();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public boolean allowOtherFieldValues() {
            return metadata.allowOtherFieldValues();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXFieldType getType() {
            return metadata.getFieldDefinition().getType();
        }
    }
}
