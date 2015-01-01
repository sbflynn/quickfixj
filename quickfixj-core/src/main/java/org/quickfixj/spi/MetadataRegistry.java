/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 19 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.GraphBuilder.FieldBuilder;
import org.quickfixj.spi.DictionaryMetadata.ApplicationMetadata;
import org.quickfixj.spi.DictionaryMetadata.TransportMetadata;
import org.quickfixj.xml.dictionary.DataDictionary;
import org.quickfixj.xml.dictionary.Engine;
import org.quickfixj.xml.dictionary.Engine.ApplicationDictionary;
import org.quickfixj.xml.dictionary.Engine.ApplicationDictionary.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MetadataRegistry - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class MetadataRegistry {

	private final static Logger LOG = LoggerFactory
			.getLogger(MetadataRegistry.class);

	private final List<Engine> engines = new ArrayList<Engine>();
	private final Map<String, DataDictionary> registeredConfigs = new HashMap<String, DataDictionary>();

	private final Map<BeginStringKey, TransportMetadata> transportMetadata = new HashMap<BeginStringKey, TransportMetadata>();
	private final Map<ApplicationKey, ApplicationMetadata> applicationMetadata = new HashMap<ApplicationKey, ApplicationMetadata>();

	private final Map<BeginStringKey, SessionBuilderProvider> transportBuilderProviders = new HashMap<BeginStringKey, SessionBuilderProvider>();
	private final Map<ApplicationKey, ApplicationBuilderProvider> applicationBuilderProviders = new HashMap<ApplicationKey, ApplicationBuilderProvider>();

	private final Map<BeginStringKey, FIXMessageBuilderFactory> messageBuilderFactories = new HashMap<BeginStringKey, FIXMessageBuilderFactory>();
	private final Map<BeginStringKey, FIXMessageDictionaryFactory> messageDictionaryFactories = new HashMap<BeginStringKey, FIXMessageDictionaryFactory>();

	private boolean built = false;

	public synchronized void register(Engine engine,
			Map<String, DataDictionary> configs) {
		engines.add(engine);
		registeredConfigs.putAll(configs);
	}

	/**
	 * Instruct registry to load any {@link DictionaryService} discovered on the
	 * classpath using the standard Java SE 1.6 service discovery mechanism.
	 */
	public synchronized void loadDictionaryServices() {

		ServiceLoader<DictionaryService> serviceLoader = ServiceLoader
				.load(DictionaryService.class);

		for (DictionaryService service : serviceLoader) {
			register(service.getEngine(), service.getDictionaries());
		}
	}

	/**
	 * Instruct registry to load any {@link FactoryService} discovered on the
	 * classpath using the standard Java SE 1.6 service discovery mechanism.
	 */
	public synchronized void loadFactoryServices() {

		ServiceLoader<FactoryService> serviceLoader = ServiceLoader
				.load(FactoryService.class);

		for (FactoryService service : serviceLoader) {
			for (FIXMessageDictionaryFactory factory : service
					.getMessageDictionaryFactories()) {
				register(factory);
			}
			for (FIXMessageBuilderFactory factory : service
					.getMessageBuilderFactories()) {
				register(factory);
			}
		}
	}

	/**
	 * Should be called after all metadata has been loaded from the classpath
	 * and other configuration resources but before this registry is put into
	 * service.
	 * 
	 * @since 2.0
	 */
	public synchronized void build() {

		if (built) {
			LOG.warn("This metadata registry has already been built");
			return;
		}

		LOG.info(String.format("Building metadata registry"));

		// first build all the required transportMetadata objects
		for (Engine engine : engines) {
			if (engine.getTransportDictionary() != null) {

				BeginStringKey key = new BeginStringKey(
						engine.getTransportDictionary());

				LOG.debug(String.format("Registered transportMetadata %s", key));

				TransportMetadata metadata = new TransportMetadata(engine
						.getTransportDictionary().getBeginString(),
						registeredConfigs.get(engine.getTransportDictionary()
								.getDictionary()));

				transportMetadata.put(key, metadata);
			}
		}

		// then build all the required applicationMetadata objects
		for (Engine engine : engines) {
			if (engine.getApplicationDictionary() != null) {

				ApplicationKey key = new ApplicationKey(
						engine.getApplicationDictionary());

				LOG.debug(String.format("Registered applicationMetadata %s",
						key));

				ApplicationMetadata metadata = new ApplicationMetadata(engine
						.getApplicationDictionary().getApplication(),
						registeredConfigs.get(engine.getApplicationDictionary()
								.getDictionary()));

				applicationMetadata.put(key, metadata);
			}
		}

		// then build all the required intermediate sessionBuilderProviders
		for (Engine engine : engines) {
			if (engine.getMessageFactory() != null) {
				if (engine.getMessageFactory().getTransport() != null) {
					BeginStringKey key = new BeginStringKey(engine
							.getMessageFactory().getNamespace(), engine
							.getMessageFactory().getTransport());

					LOG.debug(String.format(
							"Created transportBuilderProvider %s", key));

					transportBuilderProviders.put(
							key,
							new SessionBuilderProvider(transportMetadata
									.get(key), engine.getMessageFactory()));
				}
			}
		}

		// then build all the required intermediate applicationBuilderProviders
		for (Engine engine : engines) {
			if (engine.getMessageFactory() != null) {
				if (engine.getMessageFactory().getApplication() != null) {
					ApplicationKey key = new ApplicationKey(engine
							.getMessageFactory().getNamespace(), engine
							.getMessageFactory().getApplication());

					LOG.debug(String.format(
							"Created applicationBuilderProvider %s", key));

					applicationBuilderProviders.put(
							key,
							new ApplicationBuilderProvider(applicationMetadata
									.get(key), engine.getMessageFactory()));
				}
			}
		}

		// then build all the required messageBuilderFactories
		for (Engine engine : engines) {
			if (engine.getApplicationDictionary() != null) {
				for (Transport transport : engine.getApplicationDictionary()
						.getTransport()) {
					BeginStringKey key = new BeginStringKey(engine
							.getApplicationDictionary().getNamespace(),
							transport.getBeginString());

					if (!messageBuilderFactories.containsKey(key)) {
						messageBuilderFactories.put(
								key,
								new SystemMessageBuilderFactory(engine
										.getApplicationDictionary()
										.getNamespace(), transport
										.getBeginString()));
						LOG.debug(String.format(
								"Created messageBuilderFactory %s", key));
					}

					if (!messageDictionaryFactories.containsKey(key)) {
						BeginStringKey keyMetadata = new BeginStringKey(
								transport.getNamespace(),
								transport.getBeginString());
						TransportMetadata tMetadata = transportMetadata
								.get(keyMetadata);
						if (tMetadata != null) {
							messageDictionaryFactories.put(key,
									new MetadataDictionaryFactory(
											key.namespace, tMetadata));
							LOG.debug(String
									.format("Created dictionaryFactory %s",
											keyMetadata));
						} else {
							LOG.warn(String
									.format("Unable to create dictionaryFactory %s for application %s",
											keyMetadata, engine
													.getApplicationDictionary()
													.getApplication()));
						}
					}
				}
			}
		}

		for (Engine config : engines) {
			if (config.getApplicationDictionary() != null) {

				ApplicationDictionary application = config
						.getApplicationDictionary();

				ApplicationKey applicationKey = new ApplicationKey(application);

				ApplicationMetadata aMetadata = applicationMetadata
						.get(applicationKey);
				ApplicationBuilderProvider applicationBuilderProvider = applicationBuilderProviders
						.get(applicationKey);

				LOG.info(String.format("Building application %s",
						applicationKey));

				for (Transport transport : application.getTransport()) {

					BeginStringKey applicationBeginStringKey = new BeginStringKey(
							application.getNamespace(),
							transport.getBeginString());
					BeginStringKey transportBeginStringKey = new BeginStringKey(
							transport.getNamespace(),
							transport.getBeginString());

					SystemMessageBuilderFactory factory = (SystemMessageBuilderFactory) messageBuilderFactories
							.get(applicationBeginStringKey);

					SessionBuilderProvider sessionBuilderProvider = transportBuilderProviders
							.get(transportBeginStringKey);
					MetadataDictionaryFactory dictionaryFactory = (MetadataDictionaryFactory) messageDictionaryFactories
							.get(applicationBeginStringKey);

					if (dictionaryFactory != null) {

						dictionaryFactory.add(aMetadata);
					}

					if (sessionBuilderProvider != null
							&& applicationBuilderProvider != null) {

						for (FIXMessageBuilder builder : sessionBuilderProvider
								.getMessageBuilders()) {

							Map<Integer, FieldBuilder> fieldBuilders = new HashMap<Integer, FieldBuilder>(
									builder.getFieldBuilders());
							fieldBuilders.putAll(sessionBuilderProvider
									.getHeaderFieldBuilders());
							fieldBuilders.putAll(sessionBuilderProvider
									.getTrailerFieldBuilders());

							FIXMessageBuilder mb = new SystemMessageBuilder(
									fieldBuilders,
									sessionBuilderProvider.getBeginString(),
									builder);

							factory.addTransportBuilder(builder.getMsgType(),
									mb);
							factory.addMessageBuilder(
									application.getApplication(),
									builder.getMsgType(), mb);
						}

						for (FIXMessageBuilder builder : applicationBuilderProvider
								.getMessageBuilders()) {

							Map<Integer, FieldBuilder> fieldBuilders = new HashMap<Integer, FieldBuilder>(
									builder.getFieldBuilders());
							fieldBuilders.putAll(sessionBuilderProvider
									.getHeaderFieldBuilders());
							fieldBuilders.putAll(sessionBuilderProvider
									.getTrailerFieldBuilders());

							FIXMessageBuilder mb = new SystemMessageBuilder(
									fieldBuilders,
									sessionBuilderProvider.getBeginString(),
									builder);

							factory.addMessageBuilder(
									application.getApplication(),
									builder.getMsgType(), mb);
						}
					}
				}
			}
		}

		built = true;
	}

	public synchronized void register(FIXEngine engine) {

		for (Entry<BeginStringKey, FIXMessageBuilderFactory> entry : messageBuilderFactories
				.entrySet()) {
			engine.registerMessageBuilderFactory(entry.getKey().beginString,
					entry.getKey().namespace, entry.getValue());
		}

		for (Entry<BeginStringKey, FIXMessageDictionaryFactory> entry : messageDictionaryFactories
				.entrySet()) {
			engine.registerMessageDictionaryFactory(entry.getKey().beginString,
					entry.getKey().namespace, entry.getValue());
		}
	}

	public synchronized void register(FIXMessageBuilderFactory factory) {

		messageBuilderFactories.put(new BeginStringKey(factory.getNamespace(),
				factory.getBeginString()), factory);
	}

	public synchronized void register(FIXMessageDictionaryFactory factory) {

		messageDictionaryFactories.put(
				new BeginStringKey(factory.getNamespace(), factory
						.getBeginString()), factory);
	}

	/**
	 * Get the dictionaryFactories property.
	 *
	 * @return Returns the dictionaryFactories.
	 * @since 2.0
	 */
	public synchronized FIXMessageDictionaryFactory getMessageDictionaryFactory(
			FIXBeginString beginString, String namespace) {
		return messageDictionaryFactories.get(new BeginStringKey(namespace,
				beginString));
	}

	/**
	 * Get the dictionaryFactories property.
	 *
	 * @return Returns the dictionaryFactories.
	 * @since 2.0
	 */
	public synchronized FIXMessageBuilderFactory getMessageBuilderFactory(
			FIXBeginString beginString, String namespace) {
		return messageBuilderFactories.get(new BeginStringKey(namespace,
				beginString));
	}

	private final class BeginStringKey {

		private final String namespace;
		private final FIXBeginString beginString;
		private final int hashcode;

		public BeginStringKey(Engine.TransportDictionary transportDictionary) {
			this(transportDictionary.getNamespace(), transportDictionary
					.getBeginString());
		}

		public BeginStringKey(String namespace, FIXBeginString object) {
			this.namespace = namespace.intern();
			this.beginString = object;

			int hash = 17;
			hash = hash * 37 + namespace.hashCode();
			hash = hash * 37 + object.hashCode();
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

			BeginStringKey key = (BeginStringKey) obj;
			return namespace.equals(key.namespace)
					&& beginString == key.beginString;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @since 2.0
		 */
		@Override
		public String toString() {
			return String
					.format("BeginStringKey %s:%s", namespace, beginString);
		}
	}

	private final class ApplicationKey {

		private final String namespace;
		private final FIXApplication application;
		private final int hashcode;

		public ApplicationKey(Engine.ApplicationDictionary applicationDictionary) {
			this(applicationDictionary.getNamespace(), applicationDictionary
					.getApplication());
		}

		public ApplicationKey(String namespace, FIXApplication object) {
			this.namespace = namespace.intern();
			this.application = object;

			int hash = 17;
			hash = hash * 37 + namespace.hashCode();
			hash = hash * 37 + object.hashCode();
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

			ApplicationKey key = (ApplicationKey) obj;

			return namespace.equals(key.namespace)
					&& application == key.application;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @since 2.0
		 */
		@Override
		public String toString() {
			return String
					.format("ApplicationKey %s:%s", namespace, application);
		}
	}
}
