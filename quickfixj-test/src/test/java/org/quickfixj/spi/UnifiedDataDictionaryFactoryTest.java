/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 12 Dec 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.GraphDictionary.GroupDictionary;
import org.xml.sax.SAXException;

import quickfix.DefaultEngine;

/**
 * UnifiedDataDictionaryFactoryTest - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class UnifiedDataDictionaryFactoryTest {

    /**
     * Test method for {@link org.quickfixj.spi.MetadataDictionaryFactory#getMessageDictionary(org.quickfixj.FIXBeginString, org.quickfixj.FIXApplication, java.lang.String)}.
     * @throws JAXBException 
     * @throws SAXException 
     */
    @Test
    public void testGetMessageDictionary() throws SAXException, JAXBException {

        FIXEngine engine = DefaultEngine.getDefaultEngine();
        FIXMessageDictionaryFactory provider = engine.getMessageDictionaryFactory(
                FIXBeginString.FIX44, "org.quickfixj.messages.bd");

        FIXMessageDictionary messageDictionary;
        GroupDictionary groupDictionary;

        messageDictionary = provider.getMessageDictionary(FIXApplication.FIX44, "i");

        assertNotNull(messageDictionary);
        assertTrue(messageDictionary.isHeaderField(8));
        assertTrue(messageDictionary.isTrailerField(10));
        assertTrue(messageDictionary.isField(296));
        assertTrue(messageDictionary.isGroupField(296));

        // check immediate group
        groupDictionary = messageDictionary.getGroupDictionary(296);
        assertNotNull(groupDictionary);
        assertTrue(groupDictionary.isField(295));
        assertTrue(groupDictionary.isGroupField(295));

        // check nested group
        groupDictionary = groupDictionary.getGroupDictionary(295);
        assertNotNull(groupDictionary);
        assertTrue(groupDictionary.isField(299));
        assertFalse(groupDictionary.isGroupField(299));
        assertTrue(groupDictionary.isField(454));
        assertTrue(groupDictionary.isGroupField(454));

        // check nested group
        groupDictionary = groupDictionary.getGroupDictionary(454);
        assertNotNull(groupDictionary);
        assertTrue(groupDictionary.isField(455));
        assertFalse(groupDictionary.isGroupField(455));
        assertTrue(groupDictionary.isField(456));
        assertFalse(groupDictionary.isGroupField(456));
    }

    /**
     * Test method for {@link org.quickfixj.spi.MetadataDictionaryFactory#getMessageDictionary(org.quickfixj.FIXBeginString, org.quickfixj.FIXApplication, java.lang.String)}.
     * @throws JAXBException 
     * @throws SAXException 
     */
    @Test
    public void testMessageDictionaryHeaderGroup() throws SAXException, JAXBException {

        FIXEngine engine;
        FIXMessageDictionaryFactory provider;
        FIXMessageDictionary messageDictionary;
        GroupDictionary groupDictionary;

        engine = DefaultEngine.getDefaultEngine();
        provider = engine.getMessageDictionaryFactory(FIXBeginString.FIX44,
                "org.quickfixj.messages.bd");
        messageDictionary = provider.getMessageDictionary(FIXApplication.FIX44, "i");

        assertNotNull(messageDictionary);
        assertTrue(messageDictionary.isHeaderField(8));
        assertTrue(messageDictionary.isTrailerField(10));
        assertTrue(messageDictionary.isHeaderField(627));
        assertTrue(messageDictionary.isGroupField(627));

        // check immediate group
        groupDictionary = messageDictionary.getGroupDictionary(296);
        assertNotNull(groupDictionary);
        assertTrue(groupDictionary.isField(295));
        assertTrue(groupDictionary.isGroupField(295));

        // check nested group
        groupDictionary = groupDictionary.getGroupDictionary(295);
        assertNotNull(groupDictionary);
        assertTrue(groupDictionary.isField(299));
        assertFalse(groupDictionary.isGroupField(299));
        assertTrue(groupDictionary.isField(454));
        assertTrue(groupDictionary.isGroupField(454));

        // check nested group
        groupDictionary = groupDictionary.getGroupDictionary(454);
        assertNotNull(groupDictionary);
        assertTrue(groupDictionary.isField(455));
        assertFalse(groupDictionary.isGroupField(455));
        assertTrue(groupDictionary.isField(456));
        assertFalse(groupDictionary.isGroupField(456));
    }

    /**
     * Test method for {@link org.quickfixj.spi.MetadataDictionaryFactory#getMessageDictionary(org.quickfixj.FIXBeginString, org.quickfixj.FIXApplication, java.lang.String)}.
     * @throws JAXBException 
     * @throws SAXException 
     */
    @Test
    public void testGetMessageDictionaryFIXT() throws SAXException, JAXBException {

        FIXEngine engine = DefaultEngine.getDefaultEngine();
        FIXMessageDictionaryFactory factory = engine.getMessageDictionaryFactory(
                FIXBeginString.FIXT11, "org.quickfixj.messages.bd");

        FIXMessageDictionary messageDictionary;
        GroupDictionary groupDictionary;

        messageDictionary = factory.getMessageDictionary(FIXApplication.FIX50, "i");

        assertNotNull(messageDictionary);
        assertTrue(messageDictionary.isHeaderField(8));
        assertTrue(messageDictionary.isTrailerField(10));
        assertTrue(messageDictionary.isField(296));
        assertTrue(messageDictionary.isGroupField(296));

        // check immediate group
        groupDictionary = messageDictionary.getGroupDictionary(296);
        assertNotNull(groupDictionary);
        assertTrue(groupDictionary.isField(295));
        assertTrue(groupDictionary.isGroupField(295));

        // check nested group
        groupDictionary = groupDictionary.getGroupDictionary(295);
        assertNotNull(groupDictionary);
        assertTrue(groupDictionary.isField(299));
        assertFalse(groupDictionary.isGroupField(299));
        assertTrue(groupDictionary.isField(454));
        assertTrue(groupDictionary.isGroupField(454));

        // check nested group
        groupDictionary = groupDictionary.getGroupDictionary(454);
        assertNotNull(groupDictionary);
        assertTrue(groupDictionary.isField(455));
        assertFalse(groupDictionary.isGroupField(455));
        assertTrue(groupDictionary.isField(456));
        assertFalse(groupDictionary.isGroupField(456));
    }
}
