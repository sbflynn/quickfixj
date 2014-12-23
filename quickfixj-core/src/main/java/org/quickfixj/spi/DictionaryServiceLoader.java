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
import java.util.ServiceLoader;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.xml.dictionary.DataDictionaryConfig;
import org.quickfixj.xml.dictionary.Engine;
import org.quickfixj.xml.dictionary.ObjectFactory;
import org.xml.sax.SAXException;

/**
 * SystemServiceLoader - responsible for registering any 
 * {@link FIXMessageBuilderFactory} and {@link FIXMessageDictionaryFactory} instances 
 * available on the classpath with a {@link FIXEngine}.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class DictionaryServiceLoader {

    public static final void registerSystemFactories(MetadataRegistry registry) {

        ServiceLoader<DictionaryService> serviceLoader = ServiceLoader
                .load(DictionaryService.class);

        // stash message builders
        for (DictionaryService service : serviceLoader) {
            registry.register(service.getEngine(), service.getDictionaries());
        }
    }

    public static Engine loadFactory(URL url) throws SAXException, JAXBException, IOException {

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema = sf.newSchema(DictionaryMetadata.class
                .getResource("/META-INF/xsd/fix-dictionary.xsd"));

        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        unmarshaller.setSchema(schema);
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());

        InputStream is = null;
        try {
            is = url.openStream();
            return unmarshaller.unmarshal(new StreamSource(is), Engine.class).getValue();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static DataDictionaryConfig loadDataDictionaryConfig(URL url) throws SAXException,
            JAXBException, IOException {

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema = sf.newSchema(DictionaryMetadata.class
                .getResource("/META-INF/xsd/fix-dictionary.xsd"));

        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        unmarshaller.setSchema(schema);
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());

        InputStream is = null;
        try {
            is = url.openStream();
            return unmarshaller.unmarshal(new StreamSource(is), DataDictionaryConfig.class)
                    .getValue();
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}
