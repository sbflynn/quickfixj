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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.quickfixj.xml.dictionary.DataDictionaryConfig;
import org.quickfixj.xml.dictionary.Engine;
import org.xml.sax.SAXException;

/**
 * DictionaryServiceSupport - convenience superclass for jar/bundle services.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class DictionaryServiceSupport implements DictionaryService {

    private final Engine engine;
    private final Map<String, DataDictionaryConfig> dictionaries;

    protected DictionaryServiceSupport(String resource) throws SAXException, JAXBException,
            IOException {

        URL engineURL = getClass().getResource(resource);
        this.engine = DictionaryServiceLoader.loadFactory(engineURL);
        this.dictionaries = new HashMap<String, DataDictionaryConfig>();

        for (Engine.Dictionary dictionary : engine.getDictionary()) {

            URL urlDictionary = new URL(engineURL, dictionary.getLocation());

            dictionaries.put(dictionary.getId(),
                    DictionaryServiceLoader.loadDataDictionaryConfig(urlDictionary));
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
    public Map<String, DataDictionaryConfig> getDictionaries() {
        return dictionaries;
    }
}
