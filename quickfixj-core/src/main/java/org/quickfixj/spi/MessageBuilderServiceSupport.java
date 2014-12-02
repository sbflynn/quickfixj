/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.MessageBuilder;
import org.quickfixj.MessageBuilder.ComponentBuilder;
import org.quickfixj.MessageBuilder.FieldBuilder;
import org.quickfixj.MessageBuilder.GroupBuilder;
import org.quickfixj.xml.dictionary.ComponentDefinition;
import org.quickfixj.xml.dictionary.ComponentReference;
import org.quickfixj.xml.dictionary.DataDictionaryConfig;
import org.quickfixj.xml.dictionary.EnumMessageCategory;
import org.quickfixj.xml.dictionary.FieldDefinition;
import org.quickfixj.xml.dictionary.FieldReference;
import org.quickfixj.xml.dictionary.GroupDefinition;
import org.quickfixj.xml.dictionary.MessageDefinition;
import org.quickfixj.xml.dictionary.MessageParticle;
import org.quickfixj.xml.dictionary.ObjectFactory;
import org.quickfixj.xml.dictionary.Provides.Application;
import org.quickfixj.xml.dictionary.Provides.Transport;
import org.xml.sax.SAXException;

import quickfix.FieldConvertError;
import quickfix.FixTags;
import quickfix.Message;
import quickfix.StringField;

/**
 * MessageBuilderServiceSupport - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class MessageBuilderServiceSupport implements MessageBuilderService {

    private final List<ApplicationBuilderProvider> applicationBuilderProviders = new ArrayList<ApplicationBuilderProvider>();

    private final List<SessionBuilderProvider> sessionBuilderProviders = new ArrayList<SessionBuilderProvider>();

    private final Map<String, FieldDefinition> fieldDefinitions = new HashMap<String, FieldDefinition>();

    private final Map<String, ComponentBuilder> componentBuilders = new HashMap<String, ComponentBuilder>();

    private final Map<Integer, FieldBuilder> fieldBuilders = new HashMap<Integer, FieldBuilder>();

    private final String rootpackage;

    protected MessageBuilderServiceSupport(String resource, String rootpackage)
            throws SAXException, JAXBException {

        this.rootpackage = rootpackage;

        DataDictionaryConfig config = loadJaxb(getClass().getResourceAsStream(resource));

        for (FieldDefinition fd : config.getFields().getField()) {
            fieldDefinitions.put(fd.getName(), fd);
            fieldBuilders.put(fd.getNumber(), new LazyFieldBuilder(fd));
        }

        if (config.getComponents() != null) {
            for (ComponentDefinition cd : config.getComponents().getComponent()) {
                componentBuilders.put(cd.getName(), new LazyComponentBuilder(cd));
            }
        }

        if (config.getProvides() != null) {
            if (config.getProvides().getApplication() != null) {

                ApplicationBuilderProviderImpl provider = new ApplicationBuilderProviderImpl(config
                        .getProvides().getApplication());

                for (MessageDefinition md : config.getMessages().getMessage()) {
                    if (md.getMsgcat() == EnumMessageCategory.APP) {
                        provider.messageBuilders.add(new LazyMessageBuilder(config.getProvides()
                                .getApplication().getApplication(), md));
                    }
                }
                applicationBuilderProviders.add(provider);
            }
            if (config.getProvides().getTransport() != null) {

                SessionBuilderProviderImpl provider = new SessionBuilderProviderImpl(config
                        .getProvides().getTransport());

                for (MessageDefinition md : config.getMessages().getMessage()) {
                    if (md.getMsgcat() == EnumMessageCategory.ADMIN) {
                        provider.messageBuilders.add(new LazyMessageBuilder(null, md));
                    }
                }

                for (MessageParticle particle : config.getHeader().getParticles()) {
                    if (particle instanceof FieldReference) {
                        FieldDefinition fd = fieldDefinitions.get(particle.getName());
                        provider.headerFieldBuilders.put(fd.getNumber(), new LazyFieldBuilder(fd));
                    }
                }

                for (MessageParticle particle : config.getTrailer().getParticles()) {
                    if (particle instanceof FieldReference) {
                        FieldDefinition fd = fieldDefinitions.get(particle.getName());
                        provider.trailerFieldBuilders.put(fd.getNumber(), new LazyFieldBuilder(fd));
                    }
                }

                sessionBuilderProviders.add(provider);
            }
        }
    }

    private DataDictionaryConfig loadJaxb(InputStream inputStream) throws SAXException,
            JAXBException {

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema = sf.newSchema(getClass().getResource("/META-INF/xsd/fix-dictionary.xsd"));

        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        unmarshaller.setSchema(schema);
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());

        return unmarshaller.unmarshal(new StreamSource(inputStream), DataDictionaryConfig.class)
                .getValue();
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
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public List<ApplicationBuilderProvider> getApplicationBuilderProviders() {
        return applicationBuilderProviders;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public List<SessionBuilderProvider> getSessionBuilderProviders() {
        return sessionBuilderProviders;
    }

    private class LazyFieldBuilder implements FieldBuilder {

        private final FieldDefinition fd;

        private Constructor<?> constructor;

        LazyFieldBuilder(FieldDefinition fd) {
            this.fd = fd;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public int getTag() {
            return fd.getNumber();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0   
         */
        @Override
        public String getName() {
            return fd.getName();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXField<?> create(CharSequence chars) {
            try {

                if (constructor == null) {
                    Class<?> type = Class.forName(rootpackage + ".field." + fd.getName());
                    constructor = type.getConstructor(CharSequence.class);
                }

                return (FIXField<?>) constructor.newInstance(chars);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class LazyGroupFieldBuilder implements FieldBuilder {

        private final String parentClassName;

        private final FieldDefinition fd;

        private Constructor<?> constructor;

        LazyGroupFieldBuilder(LazyBuilderSupport<?> parent, FieldDefinition fd) {
            this(parent.getClassName(), fd);
        }

        LazyGroupFieldBuilder(String parent, FieldDefinition fd) {
            this.parentClassName = parent;
            this.fd = fd;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public int getTag() {
            return fd.getNumber();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0   
         */
        @Override
        public String getName() {
            return fd.getName();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXField<?> create(CharSequence chars) {
            try {

                if (constructor == null) {
                    Class<?> type = Class.forName(parentClassName + "$" + fd.getName());
                    constructor = type.getConstructor(CharSequence.class);
                }

                return (FIXField<?>) constructor.newInstance(chars);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private abstract class LazyBuilderSupport<T extends FIXFieldGraph> {

        private final Map<Integer, FieldBuilder> fieldBuilders = new HashMap<Integer, FieldBuilder>();
        private Map<Integer, GroupBuilder<?>> groupBuilders = new HashMap<Integer, MessageBuilder.GroupBuilder<?>>();

        private Constructor<T> constructor;

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

                return (T) constructor.newInstance();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         *
         * @since 2.0
         */
        public FIXField<?> create(int tag, CharSequence chars) {
            FieldBuilder builder = fieldBuilders.get(tag);
            if (builder != null) {
                return builder.create(chars);
            }

            System.out
                    .println("MessageBuilderServiceSupport.LazyBuilderSupport.create() LOOKING FOR "
                            + tag);
            for (FieldBuilder fb : fieldBuilders.values()) {
                System.out.println("MessageBuilderServiceSupport.LazyBuilderSupport.create() "
                        + fb.getTag());
            }
            throw new FieldConvertError("????");
        }

        /**
         *
         * @since 2.0
         */
        public <G extends FIXGroup> GroupBuilder<G> create(FIXGroupField<G> groupField) {
            return (GroupBuilder<G>) groupBuilders.get(groupField.getTag());
        }

        protected void addParticle(List<MessageParticle> particles) {
            for (MessageParticle particle : particles) {
                if (particle instanceof FieldReference) {
                    addParticle((FieldReference) particle);
                }
                if (particle instanceof GroupDefinition) {
                    addParticle((GroupDefinition) particle);
                }
                if (particle instanceof ComponentReference) {
                    addParticle((ComponentReference) particle);
                }
            }
        }

        protected void addParticle(FieldReference reference) {
            FieldDefinition fd = fieldDefinitions.get(reference.getName());
            fieldBuilders.put(fd.getNumber(),
                    MessageBuilderServiceSupport.this.fieldBuilders.get(fd.getNumber()));
        }

        protected void addParticle(GroupDefinition definition) {
            FieldDefinition fd = fieldDefinitions.get(definition.getName());
            fieldBuilders.put(fd.getNumber(), new LazyGroupFieldBuilder(this, fd));
            if (groupBuilders == null) {
                groupBuilders = new HashMap<Integer, MessageBuilder.GroupBuilder<?>>();
            }
            groupBuilders.put(fd.getNumber(), new LazyGroupBuilder(this, definition));
        }

        protected void addParticle(ComponentReference reference) {
            ComponentBuilder builder = componentBuilders.get(reference.getName());
            fieldBuilders.putAll(builder.getFieldBuilders());
            if (groupBuilders == null) {
                groupBuilders = new HashMap<Integer, MessageBuilder.GroupBuilder<?>>();
            }
            groupBuilders.putAll(builder.getGroupBuilders());
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
        public Map<Integer, GroupBuilder<?>> getGroupBuilders() {
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

    private class LazyMessageBuilder extends LazyBuilderSupport<Message> implements MessageBuilder {

        private final FIXApplication application;

        private final MessageDefinition definition;

        LazyMessageBuilder(FIXApplication application, MessageDefinition definition) {
            this.application = application;
            this.definition = definition;
            addParticle(definition.getParticles());
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXBeginString getBeginString() {
            return null;
        }

        /**
        * {@inheritDoc}
        *
        * @since 2.0
        */
        @Override
        public FIXApplication getApplication() {
            return application;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public String getMsgType() {
            return definition.getMsgtype();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        protected String getClassName() {
            return rootpackage + '.' + definition.getName();
        }
    }

    private class LazyGroupBuilder extends LazyBuilderSupport<FIXGroup> implements
            GroupBuilder<FIXGroup> {
        private final String parentClassName;
        private final GroupDefinition definition;

        LazyGroupBuilder(LazyBuilderSupport<?> parent, GroupDefinition definition) {
            this(parent.getClassName(), definition);
        }

        LazyGroupBuilder(String parentClassName, GroupDefinition definition) {
            this.parentClassName = parentClassName;
            this.definition = definition;
            addParticle(definition.getParticles());
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        protected String getClassName() {
            return parentClassName + '$' + definition.getName() + "Group";
        }
    }

    private class LazyComponentBuilder implements ComponentBuilder {

        private final ComponentDefinition definition;
        private Map<Integer, FieldBuilder> fieldBuilders;
        private Map<Integer, GroupBuilder<?>> groupBuilders;

        LazyComponentBuilder(ComponentDefinition definition) {

            this.definition = definition;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Map<Integer, FieldBuilder> getFieldBuilders() {
            if (fieldBuilders == null) {
                fieldBuilders = new HashMap<Integer, FieldBuilder>(
                        definition.getParticles().size(), 1);
                for (MessageParticle particle : definition.getParticles()) {
                    if (particle instanceof FieldReference) {
                        FieldReference reference = (FieldReference) particle;
                        FieldDefinition fd = fieldDefinitions.get(reference.getName());
                        fieldBuilders
                                .put(fd.getNumber(),
                                        MessageBuilderServiceSupport.this.fieldBuilders.get(fd
                                                .getNumber()));
                    }
                    if (particle instanceof GroupDefinition) {
                        GroupDefinition gp = (GroupDefinition) particle;
                        FieldDefinition fd = fieldDefinitions.get(gp.getName());
                        fieldBuilders.put(fd.getNumber(), new LazyGroupFieldBuilder(rootpackage
                                + ".component." + definition.getName(), fd));
                    }
                }
            }
            return fieldBuilders;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Map<Integer, GroupBuilder<?>> getGroupBuilders() {
            if (groupBuilders == null) {
                groupBuilders = new HashMap<Integer, GroupBuilder<?>>(definition.getParticles()
                        .size(), 1);
                for (MessageParticle particle : definition.getParticles()) {
                    if (particle instanceof GroupDefinition) {
                        GroupDefinition gp = (GroupDefinition) particle;
                        FieldDefinition fd = fieldDefinitions.get(gp.getName());
                        groupBuilders.put(fd.getNumber(), new LazyGroupBuilder(rootpackage
                                + ".component." + definition.getName(), gp));
                    }
                }
            }
            return groupBuilders;
        }
    }

    private class SessionBuilderProviderImpl implements SessionBuilderProvider {

        private final List<MessageBuilder> messageBuilders = new ArrayList<MessageBuilder>();
        private final Map<Integer, FieldBuilder> headerFieldBuilders = new HashMap<Integer, FieldBuilder>();
        private final Map<Integer, FieldBuilder> trailerFieldBuilders = new HashMap<Integer, FieldBuilder>();

        private final Transport transport;

        SessionBuilderProviderImpl(Transport transport) {

            this.transport = transport;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXBeginString getBeginString() {
            return transport.getBeginString();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public List<MessageBuilder> getMessageBuilders() {
            return messageBuilders;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Map<Integer, FieldBuilder> getHeaderFieldBuilders() {
            return headerFieldBuilders;
        }

        /**
        * {@inheritDoc}
        *
        * @since 2.0
        */
        @Override
        public Map<Integer, FieldBuilder> getTrailerFieldBuilders() {
            return trailerFieldBuilders;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public List<FIXApplication> getSupportedApplications() {
            return transport.getSupports();
        }
    }

    private class ApplicationBuilderProviderImpl implements ApplicationBuilderProvider {

        private final List<MessageBuilder> messageBuilders = new ArrayList<MessageBuilder>();

        private final Application application;

        ApplicationBuilderProviderImpl(Application application) {

            this.application = application;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public List<MessageBuilder> getMessageBuilders() {
            return messageBuilders;
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXApplication getApplication() {
            return application.getApplication();
        }
    }
}
