/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.quickfixj.xml.dictionary.DataDictionary;
import org.quickfixj.xml.dictionary.Engine;
import org.quickfixj.xml.dictionary.ObjectFactory;
import org.xml.sax.SAXException;

/**
 * DictionaryServiceSupport - convenience superclass for jar/bundle services.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class DictionaryServiceSupport implements DictionaryService {

	private final Engine engine;
	private final Map<String, DataDictionary> dictionaries;

	protected DictionaryServiceSupport(String resource) throws SAXException,
			JAXBException, IOException {

		URL engineURL = getClass().getResource(resource);
		this.engine = loadFactory(engineURL);
		this.dictionaries = new HashMap<String, DataDictionary>();

		for (Engine.Dictionary dictionary : engine.getDictionary()) {

			URL urlDictionary = new URL(engineURL, dictionary.getLocation());

			dictionaries.put(dictionary.getId(),
					loadDataDictionary(urlDictionary));
		}
	}

	/**
	 * Get the engine property.
	 *
	 * @return Returns the engine.
	 * @since 2.0
	 */
	@Override
	public Engine getEngine() {
		return engine;
	}

	/**
	 * Get the dictionaries property.
	 *
	 * @return Returns the dictionaries.
	 * @since 2.0
	 */
	@Override
	public Map<String, DataDictionary> getDictionaries() {
		return dictionaries;
	}

	public static Engine loadFactory(URL url) throws SAXException,
			JAXBException, IOException {

		SchemaFactory sf = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		Schema schema = sf.newSchema(DictionaryMetadata.class
				.getResource("/META-INF/xsd/fix-dictionary.xsd"));

		JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

		Unmarshaller unmarshaller = jc.createUnmarshaller();

		unmarshaller.setSchema(schema);
		unmarshaller.setEventHandler(new DefaultValidationEventHandler());

		InputStream is = null;
		try {
			is = url.openStream();
			return unmarshaller.unmarshal(new StreamSource(is), Engine.class)
					.getValue();
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public static DataDictionary loadDataDictionary(URL url)
			throws SAXException, JAXBException, IOException {

		SchemaFactory sf = SchemaFactory
				.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		Schema schema = sf.newSchema(DictionaryMetadata.class
				.getResource("/META-INF/xsd/fix-dictionary.xsd"));

		JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

		Unmarshaller unmarshaller = jc.createUnmarshaller();

		unmarshaller.setSchema(schema);
		unmarshaller.setEventHandler(new DefaultValidationEventHandler());

		InputStream is = null;
		try {
			is = url.openStream();
			return unmarshaller.unmarshal(new StreamSource(is),
					DataDictionary.class).getValue();
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

}
