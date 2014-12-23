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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.xml.dictionary.ComponentDefinition;
import org.quickfixj.xml.dictionary.ComponentReference;
import org.quickfixj.xml.dictionary.DataDictionaryConfig;
import org.quickfixj.xml.dictionary.EnumBoolean;
import org.quickfixj.xml.dictionary.FieldDefinition;
import org.quickfixj.xml.dictionary.FieldDefinition.Value;
import org.quickfixj.xml.dictionary.FieldReference;
import org.quickfixj.xml.dictionary.GroupDefinition;
import org.quickfixj.xml.dictionary.MessageDefinition;
import org.quickfixj.xml.dictionary.MessageParticle;

/**
 * DictionaryMetadata - presents the contents of a {@link DataDictionaryConfig} expanded into a graph of field and group metadata objects.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
abstract class DictionaryMetadata {

    private final Map<String, MessageMetadata> messages = new HashMap<String, MessageMetadata>();

    // REVIEW : shouldn't really need to be an instance variable
    private final Map<String, FieldDefinition> fieldDefinitions;
    // REVIEW : shouldn't really need to be an instance variable
    private final Map<String, ComponentMetadata> componentDefinitions;

    private final DataDictionaryConfig config;

    public DictionaryMetadata(DataDictionaryConfig config) {

        this.config = config;

        fieldDefinitions = new HashMap<String, FieldDefinition>(config.getFields().getField()
                .size() * 2, 1.0F);

        for (FieldDefinition definition : config.getFields().getField()) {
            fieldDefinitions.put(definition.getName(), definition);
        }

        if (config.getComponents() != null) {
            componentDefinitions = new HashMap<String, ComponentMetadata>(config.getComponents()
                    .getComponent().size() * 2, 1.0F);

            for (ComponentDefinition definition : config.getComponents().getComponent()) {
                componentDefinitions.put(definition.getName(), new ComponentMetadata(definition));
            }

        } else {

            componentDefinitions = Collections.emptyMap();
        }
    }

    /**
     * Get the messages property.
     *
     * @return Returns the messages.
     * @since 2.0
     */
    protected Map<String, MessageMetadata> getMessages() {
        return messages;
    }

    public Collection<MessageMetadata> getMessageMetadata() {

        return messages.values();
    }

    /**
    * the set of all tags defined in this {@link DictionaryMetadata}.
    *
    * @return Returns the fieldDefinitions.
    * @since 2.0
    */
    public Set<Integer> getAllTags() {
        Set<Integer> tags = new HashSet<Integer>(fieldDefinitions.size(), 1.0F);
        for (FieldDefinition definition : fieldDefinitions.values()) {
            tags.add(definition.getNumber());
        }
        return tags;
    }

    protected Map<Integer, FieldMetadata> createParticleMap(List<MessageParticle> particles,
            Metadata parent) {

        Map<Integer, FieldMetadata> map = new LinkedHashMap<Integer, FieldMetadata>(
                particles.size() * 2, 1.0F);

        for (MessageParticle particle : particles) {

            if (particle instanceof FieldReference) {
                addToParticleMap(map, (FieldReference) particle, parent);
            }

            if (particle instanceof GroupDefinition) {
                addToParticleMap(map, (GroupDefinition) particle, parent);
            }

            if (particle instanceof ComponentReference) {
                addToParticleMap(map, (ComponentReference) particle);
            }
        }

        return map;
    }

    private void addToParticleMap(Map<Integer, FieldMetadata> map, FieldReference reference,
            Metadata parent) {
        FieldDefinition fd = fieldDefinitions.get(reference.getName());
        map.put(fd.getNumber(), new FieldMetadata(fd, parent,
                reference.getRequired() == EnumBoolean.Y));
    }

    private void addToParticleMap(Map<Integer, FieldMetadata> map, GroupDefinition definition,
            Metadata parent) {
        FieldDefinition fd = fieldDefinitions.get(definition.getName());
        map.put(fd.getNumber(), new GroupMetadata(fd, parent, definition));
    }

    private void addToParticleMap(Map<Integer, FieldMetadata> map, ComponentReference reference) {
        ComponentMetadata metadata = componentDefinitions.get(reference.getName());
        for (MessageParticle particle : metadata.definition.getParticles()) {
            if (particle instanceof FieldReference) {
                addToParticleMap(map, (FieldReference) particle, metadata);
            }
            if (particle instanceof ComponentReference) {
                addToParticleMap(map, (ComponentReference) particle);
            }
            if (particle instanceof GroupDefinition) {
                addToParticleMap(map, (GroupDefinition) particle, metadata);
            }
        }
    }

    /**
     * Get the config property.
     *
     * @return Returns the config.
     * @since 2.0
     */
    public DataDictionaryConfig getConfig() {
        return config;
    }

    public interface Metadata {

        String getName();

        Metadata getRoot();

        List<Metadata> getPath();

        boolean isDefinedByComponent();
    }

    public class FieldMetadata implements Metadata {

        private final FieldDefinition fieldDefinition;

        private final Metadata parent;

        private final boolean required;

        private final Set<String> values;

        FieldMetadata(FieldDefinition definition, Metadata parent, boolean required) {
            this.fieldDefinition = definition;
            this.parent = parent;
            this.required = required;

            if (fieldDefinition.getValue().isEmpty()) {
                values = Collections.emptySet();
            } else {
                values = new HashSet<String>(fieldDefinition.getValue().size(), 1.0F);
                for (Value value : fieldDefinition.getValue()) {
                    values.add(value.getEnum());
                }
            }
        }

        public FieldDefinition getFieldDefinition() {
            return fieldDefinition;
        }

        public int getTag() {
            return fieldDefinition.getNumber();
        }

        @Override
        public String getName() {
            return fieldDefinition.getName();
        }

        /**
         * @since 2.0
         */
        public Set<String> getValues() {
            return values;
        }

        /**
         * @since 2.0
         */
        @Override
        public Metadata getRoot() {
            return parent.getRoot();
        }

        /**
         * Get the required property.
         *
         * @return Returns the required.
         * @since 2.0
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * Get the required property.
         *
         * @return Returns the required.
         * @since 2.0
         */
        public boolean allowOtherFieldValues() {
            return fieldDefinition.isAllowOtherValues();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public List<Metadata> getPath() {
            List<Metadata> path = parent.getPath();
            path.add(this);
            return path;
        }

        /**
         * @since 2.0
         */
        @Override
        public boolean isDefinedByComponent() {
            return parent.isDefinedByComponent();
        }
    }

    public class GroupMetadata extends FieldMetadata {

        private final Map<Integer, FieldMetadata> particles;

        GroupMetadata(FieldDefinition fieldDefinition, Metadata parent,
                GroupDefinition groupDefinition) {
            super(fieldDefinition, parent, groupDefinition.getRequired() == EnumBoolean.Y);

            particles = createParticleMap(groupDefinition.getParticles(), this);
        }

        /**
         * Get the particles property.
         *
         * @return Returns the particles.
         * @since 2.0
         */
        public Map<Integer, FieldMetadata> getParticles() {
            return particles;
        }
    }

    public class MessageMetadata implements Metadata {

        private final MessageDefinition definition;

        private final Map<Integer, FieldMetadata> particles;

        MessageMetadata(MessageDefinition definition) {

            this.definition = definition;

            particles = createParticleMap(definition.getParticles(), this);
        }

        public String getMsgType() {
            return definition.getMsgtype();
        }

        @Override
        public String getName() {
            return definition.getName();
        }

        /**
         * Get the particles property.
         *
         * @return Returns the particles.
         * @since 2.0
         */
        public Map<Integer, FieldMetadata> getParticles() {
            return particles;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Metadata getRoot() {
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public List<Metadata> getPath() {
            List<Metadata> path = new ArrayList<Metadata>();
            path.add(this);
            return path;
        }

        /**
         *
         * @since 2.0
         */
        @Override
        public boolean isDefinedByComponent() {
            return false;
        }
    }

    public class ComponentMetadata implements Metadata {

        private final ComponentDefinition definition;

        ComponentMetadata(ComponentDefinition definition) {

            this.definition = definition;
        }

        /**
         *
         * @since 2.0
         */
        @Override
        public String getName() {
            return definition.getName();
        }

        /**
         *
         * @since 2.0
         */
        @Override
        public Metadata getRoot() {
            return this;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public List<Metadata> getPath() {
            List<Metadata> path = new ArrayList<Metadata>();
            path.add(this);
            return path;
        }

        /**
         *
         * @since 2.0
         */
        @Override
        public boolean isDefinedByComponent() {
            return true;
        }
    }

    public static class TransportMetadata extends DictionaryMetadata {

        private final FIXBeginString beginString;
        private final Map<Integer, FieldMetadata> headerMetadata;
        private final Map<Integer, FieldMetadata> trailerMetadata;

        public TransportMetadata(FIXBeginString beginString, DataDictionaryConfig config) {

            super(config);

            this.beginString = beginString;

            headerMetadata = createParticleMap(config.getHeader().getParticles(), new Metadata() {

                @Override
                public boolean isDefinedByComponent() {
                    return false;
                }

                @Override
                public Metadata getRoot() {
                    return null;
                }

                @Override
                public List<Metadata> getPath() {
                    return null;
                }

                @Override
                public String getName() {
                    return "HEADER";
                }
            });

            trailerMetadata = createParticleMap(config.getTrailer().getParticles(), new Metadata() {

                @Override
                public boolean isDefinedByComponent() {
                    return false;
                }

                @Override
                public Metadata getRoot() {
                    return null;
                }

                @Override
                public List<Metadata> getPath() {
                    return null;
                }

                @Override
                public String getName() {
                    return "TRAILER";
                }
            });

            for (MessageDefinition definition : config.getMessages().getMessage()) {

                MessageMetadata graph = new MessageMetadata(definition);

                switch (definition.getMsgcat()) {
                case ADMIN:
                    getMessages().put(definition.getMsgtype(), graph);
                    break;
                default:
                    break;
                }
            }
        }

        /**
         * Get the beginString property.
         *
         * @return Returns the beginString.
         * @since 2.0
         */
        public FIXBeginString getBeginString() {
            return beginString;
        }

        /**
         * Get the headerMetadata property.
         *
         * @return Returns the headerMetadata.
         * @since 2.0
         */
        public Map<Integer, FieldMetadata> getHeaderMetadata() {
            return headerMetadata;
        }

        /**
         * Get the trailerMetadata property.
         *
         * @return Returns the trailerMetadata.
         * @since 2.0
         */
        public Map<Integer, FieldMetadata> getTrailerMetadata() {
            return trailerMetadata;
        }
    }

    public static class ApplicationMetadata extends DictionaryMetadata {

        private final FIXApplication application;

        public ApplicationMetadata(FIXApplication application, DataDictionaryConfig config) {

            super(config);

            this.application = application;

            for (MessageDefinition definition : config.getMessages().getMessage()) {

                MessageMetadata graph = new MessageMetadata(definition);

                switch (definition.getMsgcat()) {
                case APP:
                    getMessages().put(definition.getMsgtype(), graph);
                    break;
                default:
                    break;
                }
            }
        }

        /**
        * Get the application property.
        *
        * @return Returns the application.
        * @since 2.0
        */
        public FIXApplication getApplication() {
            return application;
        }
    }
}
