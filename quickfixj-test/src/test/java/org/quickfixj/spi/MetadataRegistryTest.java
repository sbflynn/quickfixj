/*
 * Copyright (c) 1999-2014 by JF Technology (UK) Ltd. All Rights Reserved.
 *
 * This software is the proprietary information of JF Technology (UK) Ltd.
 * Use is subject to license terms.
 *
 * Created on 28 Nov 2014 by stephen.flynn@jftechnology.com.
 */
package org.quickfixj.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXMessageBuilder;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.GraphBuilder;
import org.xml.sax.SAXException;

import quickfix.FixMessageTypes;

/**
 * SystemServiceLoaderTest - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class MetadataRegistryTest {

    private static MetadataRegistry registry;

    @BeforeClass
    public static void beforeClass() throws SAXException, JAXBException, IOException {
        registry = new MetadataRegistry();
        registry.loadDictionaryServices();

        DictionaryService customService = new DictionaryServiceSupport(
                "/META-INF/quickfix/FIX44.custom.xml") {
            //no-op
        };

        registry.register(customService.getEngine(), customService.getDictionaries());
        registry.build();
    }

    @AfterClass
    public static void afterClass() {
        registry = null;
    }

    @Test
    public void testGetMessageBuilder_MessageDictionary() {

        FIXMessageDictionaryFactory provider;
        FIXMessageDictionary dictionary;
        FIXMessageBuilder builder;

        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIX40,
                "org.quickfixj.messages.bd");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIX40,
                "org.quickfixj.messages.bd").getMessageBuilder(
                provider.getMessageDictionary(FIXApplication.FIX40, "0"));

        assertBuilderCreateMessage(builder, org.quickfixj.messages.bd.fix40.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.0",
                org.quickfixj.messages.bd.fix40.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.bd.fix40.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9",
                org.quickfixj.messages.bd.fix40.field.TestReqID.class);

        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIX41,
                "org.quickfixj.messages.bd");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIX41,
                "org.quickfixj.messages.bd").getMessageBuilder(
                provider.getMessageDictionary(FIXApplication.FIX41, "0"));

        assertBuilderCreateMessage(builder, org.quickfixj.messages.bd.fix41.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.1",
                org.quickfixj.messages.bd.fix41.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.bd.fix41.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9",
                org.quickfixj.messages.bd.fix41.field.TestReqID.class);

        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIX42,
                "org.quickfixj.messages.bd");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIX42,
                "org.quickfixj.messages.bd").getMessageBuilder(
                provider.getMessageDictionary(FIXApplication.FIX42, "0"));

        assertBuilderCreateMessage(builder, org.quickfixj.messages.bd.fix42.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.2",
                org.quickfixj.messages.bd.fix42.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.bd.fix42.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9",
                org.quickfixj.messages.bd.fix42.field.TestReqID.class);

        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIX42,
                "org.quickfixj.messages.bd");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIX42,
                "org.quickfixj.messages.bd").getMessageBuilder(
                provider.getMessageDictionary(FIXApplication.FIX42, "E"));

        assertBuilderCreateMessage(builder, org.quickfixj.messages.bd.fix42.NewOrderList.class);
        assertBuilderCreateField(builder, 8, "FIX.4.2",
                org.quickfixj.messages.bd.fix42.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.bd.fix42.field.SenderCompID.class);
        assertBuilderCreateField(builder, 73, "1",
                org.quickfixj.messages.bd.fix42.NewOrderList.NoOrders.class);

        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIX43,
                "org.quickfixj.messages.bd");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIX43,
                "org.quickfixj.messages.bd").getMessageBuilder(
                provider.getMessageDictionary(FIXApplication.FIX43, "0"));

        assertBuilderCreateMessage(builder, org.quickfixj.messages.bd.fix43.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.3",
                org.quickfixj.messages.bd.fix43.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.bd.fix43.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9",
                org.quickfixj.messages.bd.fix43.field.TestReqID.class);

        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIX44,
                "org.quickfixj.messages.bd");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIX44,
                "org.quickfixj.messages.bd").getMessageBuilder(
                provider.getMessageDictionary(FIXApplication.FIX44, "0"));

        assertBuilderCreateMessage(builder, org.quickfixj.messages.bd.fix44.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.4",
                org.quickfixj.messages.bd.fix44.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.bd.fix44.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9",
                org.quickfixj.messages.bd.fix44.field.TestReqID.class);

        // check we can provide FIXT/FIX40
        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd").getMessageBuilder(
                provider.getMessageDictionary(FIXApplication.FIX40, "D"));

        assertBuilderCreateMessage(builder, org.quickfixj.messages.bd.fix40.NewOrderSingle.class);
        assertBuilderCreateField(builder, 8, "FIXT1.1",
                org.quickfixj.messages.fixt11.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.fixt11.field.SenderCompID.class);
        assertBuilderCreateField(builder, 1, "Test",
                org.quickfixj.messages.bd.fix40.field.Account.class);

        // check we can provide FIXT/FIX44
        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd").getMessageBuilder(
                provider.getMessageDictionary(FIXApplication.FIX44, "o"));

        assertBuilderCreateMessage(builder,
                org.quickfixj.messages.bd.fix44.RegistrationInstructions.class);
        assertBuilderCreateField(builder, 8, "FIXT1.1",
                org.quickfixj.messages.fixt11.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.fixt11.field.SenderCompID.class);
        assertBuilderCreateField(builder, 1, "Test",
                org.quickfixj.messages.bd.fix44.field.Account.class);
        assertBuilderCreateField(builder, 453, "2",
                org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDs.class);

        // check we can provide FIXT/FIX50
        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd");
        dictionary = provider.getMessageDictionary(FIXApplication.FIX50, "o");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd").getMessageBuilder(dictionary);

        assertBuilderCreateMessage(builder,
                org.quickfixj.messages.bd.fix50.RegistrationInstructions.class);
        assertBuilderCreateField(builder, 8, "FIXT1.1",
                org.quickfixj.messages.fixt11.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.fixt11.field.SenderCompID.class);
        assertBuilderCreateField(builder, 1, "Test",
                org.quickfixj.messages.bd.fix50.field.Account.class);
        assertBuilderCreateField(builder, 453, "2",
                org.quickfixj.messages.bd.fix50.component.Parties.NoPartyIDs.class);

        // check we can provide FIXT/FIX50SP1
        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd");
        dictionary = provider.getMessageDictionary(FIXApplication.FIX50SP1, "o");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd").getMessageBuilder(dictionary);

        assertBuilderCreateMessage(builder,
                org.quickfixj.messages.bd.fix50sp1.RegistrationInstructions.class);
        assertBuilderCreateField(builder, 8, "FIXT1.1",
                org.quickfixj.messages.fixt11.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.fixt11.field.SenderCompID.class);
        assertBuilderCreateField(builder, 1, "Test",
                org.quickfixj.messages.bd.fix50sp1.field.Account.class);
        assertBuilderCreateField(builder, 453, "2",
                org.quickfixj.messages.bd.fix50sp1.component.Parties.NoPartyIDs.class);

        // check we can provide FIXT/FIX50SP2
        provider = registry.getMessageDictionaryFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd");
        dictionary = provider.getMessageDictionary(FIXApplication.FIX50SP2, "o");
        builder = registry.getMessageBuilderFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd").getMessageBuilder(dictionary);

        assertBuilderCreateMessage(builder,
                org.quickfixj.messages.bd.fix50sp2.RegistrationInstructions.class);
        assertBuilderCreateField(builder, 8, "FIXT1.1",
                org.quickfixj.messages.fixt11.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.fixt11.field.SenderCompID.class);
        assertBuilderCreateField(builder, 1, "Test",
                org.quickfixj.messages.bd.fix50sp2.field.Account.class);
        assertBuilderCreateField(builder, 453, "2",
                org.quickfixj.messages.bd.fix50sp2.component.Parties.NoPartyIDs.class);
    }

    @Test
    public void testFIXTAdminMessages_System() {

        FIXMessageDictionaryFactory dictionaryFactory;
        FIXMessageBuilderFactory builderFactory;
        FIXMessageDictionary dictionary;
        FIXMessageBuilder builder;

        // check we can provide every FIXT admin message for every application/namespace combination
        for (String msgType : new String[] { FixMessageTypes.HEARTBEAT, FixMessageTypes.LOGON,
                FixMessageTypes.LOGOUT, FixMessageTypes.REJECT, FixMessageTypes.RESEND_REQUEST,
                FixMessageTypes.SEQUENCE_RESET, FixMessageTypes.TEST_REQUEST }) {

            for (FIXApplication application : new FIXApplication[] { FIXApplication.FIX40,
                    FIXApplication.FIX41, FIXApplication.FIX42, FIXApplication.FIX43,
                    FIXApplication.FIX44, FIXApplication.FIX50, FIXApplication.FIX50SP1,
                    FIXApplication.FIX50SP2 }) {

                for (String namespace : new String[] { "org.quickfixj.messages.bd",
                        "org.quickfixj.messages.fp" }) {

                    dictionaryFactory = registry.getMessageDictionaryFactory(FIXBeginString.FIXT11,
                            namespace);
                    builderFactory = registry.getMessageBuilderFactory(FIXBeginString.FIXT11,
                            namespace);

                    assertNotNull(dictionaryFactory);
                    assertEquals(namespace, dictionaryFactory.getNamespace());
                    assertEquals(FIXBeginString.FIXT11, dictionaryFactory.getBeginString());

                    assertNotNull(builderFactory);
                    assertEquals(namespace, builderFactory.getNamespace());
                    assertEquals(FIXBeginString.FIXT11, builderFactory.getBeginString());

                    dictionary = dictionaryFactory.getMessageDictionary(application, msgType);
                    builder = registry.getMessageBuilderFactory(FIXBeginString.FIXT11, namespace)
                            .getMessageBuilder(dictionary);

                    assertNotNull(dictionary);
                    assertNotNull(builder);
                    assertEquals(application, dictionary.getApplication());
                    assertNull(builder.getApplication());

                    FIXMessage message = builder.create();

                    assertNotNull(message);
                    assertEquals(msgType, message.getMsgType());
                }
            }
        }
    }

    @Test
    public void testFIXTAdminMessages_Custom() {

        FIXMessageDictionaryFactory provider;
        FIXMessageDictionary dictionary;
        FIXMessageBuilder builder;

        // check registry provides custom FIXT admin dictioanries for every app type
        for (FIXBeginString beginString : new FIXBeginString[] { FIXBeginString.FIX44,
                FIXBeginString.FIXT11 }) {

            for (FIXApplication application : new FIXApplication[] { FIXApplication.FIX44 }) {

                for (String msgType : new String[] { FixMessageTypes.HEARTBEAT,
                        FixMessageTypes.LOGON, FixMessageTypes.LOGOUT, FixMessageTypes.REJECT,
                        FixMessageTypes.RESEND_REQUEST, FixMessageTypes.SEQUENCE_RESET,
                        FixMessageTypes.TEST_REQUEST }) {

                    provider = registry.getMessageDictionaryFactory(beginString, "CUSTOM");
                    dictionary = provider.getMessageDictionary(application, msgType);

                    builder = registry.getMessageBuilderFactory(beginString,
                            "org.quickfixj.messages.bd").getMessageBuilder(dictionary);

                    assertNotNull(dictionary);
                    assertNotNull(builder);
                    assertEquals(application, dictionary.getApplication());
                    assertNull(builder.getApplication());

                    FIXMessage message = builder.create();

                    assertNotNull(message);
                    assertEquals(msgType, message.getMsgType());
                }
            }
        }
    }

    @Test
    public void testNestedGroupBuilder() {

        FIXMessageDictionaryFactory dictionaryFactory;
        FIXMessageBuilderFactory builderFactory;
        FIXMessageBuilder builder;
        GraphBuilder<?> groupBuilder;

        dictionaryFactory = registry.getMessageDictionaryFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd");
        builderFactory = registry.getMessageBuilderFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd");

        assertNotNull(dictionaryFactory);
        assertNotNull(builderFactory);

        builder = builderFactory.getMessageBuilder(dictionaryFactory.getMessageDictionary(
                FIXApplication.FIX44, "o"));

        assertBuilderCreateMessage(builder,
                org.quickfixj.messages.bd.fix44.RegistrationInstructions.class);
        assertBuilderCreateField(builder, 8, "FIXT1.1",
                org.quickfixj.messages.fixt11.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.fixt11.field.SenderCompID.class);
        assertBuilderCreateField(builder, 1, "Test",
                org.quickfixj.messages.bd.fix44.field.Account.class);
        assertBuilderCreateField(builder, 453, "2",
                org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDs.class);

        groupBuilder = builder
                .create((org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDs) builder
                        .create(453, "2".toCharArray(), 0, 1));
        assertSame(groupBuilder.create().getClass(),
                org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDsGroup.class);
        assertSame(groupBuilder.create(448, "Test".toCharArray(), 0, 4).getClass(),
                org.quickfixj.messages.bd.fix44.field.PartyID.class);

        groupBuilder = groupBuilder
                .create((org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDsGroup.NoPartySubIDs) groupBuilder
                        .create(802, "2".toCharArray(), 0, 1));
        assertSame(
                groupBuilder.create().getClass(),
                org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDsGroup.NoPartySubIDsGroup.class);
        assertSame(groupBuilder.create(523, "Test".toCharArray(), 0, 4).getClass(),
                org.quickfixj.messages.bd.fix44.field.PartySubID.class);
    }

    @Test
    public void testGetMessageBuilder_MessageType() {

        FIXMessageBuilder builder;

        builder = registry.getMessageBuilderFactory(FIXBeginString.FIX40,
                "org.quickfixj.messages.bd").getMessageBuilder("0");

        assertBuilderCreateMessage(builder, org.quickfixj.messages.bd.fix40.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.0",
                org.quickfixj.messages.bd.fix40.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.bd.fix40.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9",
                org.quickfixj.messages.bd.fix40.field.TestReqID.class);

        builder = registry.getMessageBuilderFactory(FIXBeginString.FIX41,
                "org.quickfixj.messages.bd").getMessageBuilder("0");

        assertBuilderCreateMessage(builder, org.quickfixj.messages.bd.fix41.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.0",
                org.quickfixj.messages.bd.fix41.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST",
                org.quickfixj.messages.bd.fix41.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9",
                org.quickfixj.messages.bd.fix41.field.TestReqID.class);

    }

    private void assertBuilderCreateMessage(FIXMessageBuilder builder,
            Class<? extends FIXMessage> expectedType) {

        FIXMessage message = builder.create();

        assertSame(message.getClass(), expectedType);
    }

    private void assertBuilderCreateField(FIXMessageBuilder builder, int tag, String value,
            Class<? extends FIXField<?>> expectedType) {

        FIXField<?> field = builder.create(tag, value.toCharArray(), 0, value.length());

        assertEquals(field.getTag(), tag);
        assertEquals(field.getCharacters().toString(), value);
        assertSame(field.getClass(), expectedType);
    }
}
