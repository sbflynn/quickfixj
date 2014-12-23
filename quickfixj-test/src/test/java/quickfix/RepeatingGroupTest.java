/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.SessionRejectReason;
import org.quickfixj.field.FieldException;
import org.quickfixj.field.GenericField;
import org.quickfixj.messages.bd.fix44.Quote;
import org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSym;
import org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup;
import org.quickfixj.messages.bd.fix44.field.LegSymbol;
import org.quickfixj.messages.bd.fix44.field.OrderID;
import org.quickfixj.messages.bd.fix50.field.QuoteReqID;
import org.quickfixj.messages.bd.fix50.field.SettlDate2;
import org.quickfixj.messages.bd.fix50.field.Symbol;
import org.quickfixj.spi.DictionaryService;
import org.quickfixj.spi.DictionaryServiceSupport;
import org.quickfixj.spi.MetadataRegistry;

import junit.framework.Assert;

public class RepeatingGroupTest {

    // Testing Message validation
    private static FIXMessageDictionaryFactory systemDataDictionary44;
    private static FIXMessageDictionaryFactory systemDataDictionary50;
    private static FIXMessageDictionaryFactory customDataDictionary44;

    private static DefaultValidator validator;
    private static DefaultValidator validatorWithIgnoreOutOfOrder;

    @BeforeClass
    public static void beforeClass() throws Exception {

        DictionaryService builderService44 = new org.quickfixj.messages.bd.fix44.DictionaryService();
        DictionaryService builderServiceFixt = new org.quickfixj.messages.fixt11.DictionaryService();
        DictionaryService builderService50 = new org.quickfixj.messages.bd.fix50.DictionaryService();
        DictionaryService builderService50sp2 = new org.quickfixj.messages.bd.fix50sp2.DictionaryService();
        DictionaryService customService = new DictionaryServiceSupport(
                "/META-INF/quickfix/FIX44.custom.xml") {
            //no-op
        };

        MetadataRegistry registry = new MetadataRegistry();

        registry.register(builderService44.getEngine(), builderService44.getDictionaries());
        registry.register(builderServiceFixt.getEngine(), builderServiceFixt.getDictionaries());
        registry.register(builderService50.getEngine(), builderService50.getDictionaries());
        registry.register(builderService50sp2.getEngine(), builderService50sp2.getDictionaries());
        registry.register(customService.getEngine(), customService.getDictionaries());
        registry.build();

        systemDataDictionary44 = registry.getMessageDictionaryFactory(FIXBeginString.FIX44,
                "org.quickfixj.messages.bd");
        systemDataDictionary50 = registry.getMessageDictionaryFactory(FIXBeginString.FIXT11,
                "org.quickfixj.messages.bd");
        customDataDictionary44 = registry.getMessageDictionaryFactory(FIXBeginString.FIXT11,
                "CUSTOM");

        //        customDataDictionary = new MetadataDictionaryFactory(
        //                DictionaryMetadata.getTransportMetadata("/FIX44.xml"));
        //        customDataDictionary.add(DictionaryMetadata
        //                .getApplicationMetadata("/FIX44_Custom_Test.xml"));

        validator = new DefaultValidator(FIXBeginString.FIX44);
        validatorWithIgnoreOutOfOrder = new DefaultValidator(FIXBeginString.FIX44);
        validatorWithIgnoreOutOfOrder.setCheckUnorderedGroupFields(false);

        //        defaultDataDictionary.addTransportDictionary(FIXBeginString.FIX44, dictionary);
        //        defaultDataDictionary.addApplicationDictionary(FIXApplication.FIX44, dictionary);
        // dictionary = new DataDictionary("FIX50SP2.xml");
        //        defaultDataDictionary.addTransportDictionary(FIXBeginString.FIXT11, dictionary);
        //        defaultDataDictionary.addApplicationDictionary(FIXApplication.FIX50SP2, dictionary);

        // set up default dictionary ignoring out of order
        //        defaultDataDictionaryWithIgnoreOutOfOrder = new DefaultDataDictionaryProvider(false);
        //        dictionary = new DataDictionary("FIX44.xml");
        //        dictionary.setCheckUnorderedGroupFields(false);
        //        defaultDataDictionaryWithIgnoreOutOfOrder.addTransportDictionary(FIXBeginString.FIX44,
        //                dictionary);
        //        defaultDataDictionaryWithIgnoreOutOfOrder.addApplicationDictionary(FIXApplication.FIX44,
        //                dictionary);

        // set up customer dictionary
        //        customDataDictionary = new MetadataDictionaryFactory(
        //                DictionaryMetadata.getTransportMetadata("/FIX44.xml"));
        //        customDataDictionary.add(DictionaryMetadata
        //                .getApplicationMetadata("/FIX44_Custom_Test.xml"));
        //        dictionary = new DataDictionary("FIX44_Custom_Test.xml");
        //        customDataDictionary.addTransportDictionary(FIXBeginString.FIX44, dictionary);
        //        customDataDictionary.addApplicationDictionary(FIXApplication.FIX44, dictionary);
        //

        //  messageFactory = SystemServiceLoader.getMessageBuilderFactory();
    }

    @AfterClass
    public static void afterClass() {
        systemDataDictionary44 = null;
        systemDataDictionary50 = null;
        customDataDictionary44 = null;
        validator = null;
        validatorWithIgnoreOutOfOrder = null;
        //   messageFactory = null;
    }

    // In this testcase we use only FIX4.4 message, but we could use the others
    // FIX version. Indeed the group
    // management is independent from the version

    // NON NESTED Groups
    private Quote.NoLegsGroup buildGroupWithStandardFields(String settingValue) {

        final Quote.NoLegsGroup grp = new Quote.NoLegsGroup();
        grp.setLegSymbol(new LegSymbol(settingValue));
        return grp;
    }

    @Test
    public void testSettingGettingGroupWithStandardFields() throws FieldNotFound {

        final String settingValue = "SETTING_VALUE";

        final Quote.NoLegsGroup grp = buildGroupWithStandardFields(settingValue);

        final LegSymbol gotField = (LegSymbol) grp.getField(LegSymbol.TAG);

        // We assume that checksum equality s enough - DV
        assertEquals("GettingValue is not the same the SettingValue", settingValue,
                gotField.getValue());
    }

    private Quote.NoLegsGroup buildGroupWithCustomFields(String settingValue) {

        final Quote.NoLegsGroup grp = new Quote.NoLegsGroup();
        grp.setField(new org.quickfixj.field.GenericField(9001, settingValue)); // Custom
        // tag is
        // 9001
        return grp;
    }

    @Test
    public void testSettingGettingGroupWithCustomFields() throws FieldNotFound {

        final String settingValue = "SETTING_VALUE";

        final Quote.NoLegsGroup grp = buildGroupWithCustomFields(settingValue);

        final FIXField<?> gotField = grp.getField(9001);

        // We assume that checksum equality s enough - DV
        assertEquals("GettingValue is not the same the SettingValue", settingValue,
                gotField.getValue());
    }

    private Quote.NoLegsGroup buildGroupWithCustomAndStandardFields(String settingValue) {

        final Quote.NoLegsGroup grp = new Quote.NoLegsGroup();
        grp.setField(new GenericField(9001, settingValue)); // Custom tag is
        // 9001
        grp.setLegSymbol(new LegSymbol(settingValue));
        return grp;
    }

    @Test
    public void testSettingGettingGroupWithCustomAndStandardFields() throws FieldNotFound {

        final String settingValue = "SETTING_VALUE";

        final Quote.NoLegsGroup grp = buildGroupWithCustomAndStandardFields(settingValue);

        final FIXField<?> gotField = grp.getField(9001); // Custom tag is 9001
        final LegSymbol gotFieldStd = (LegSymbol) grp.getField(LegSymbol.TAG);

        assertEquals("GettingValue is not the same the SettingValue", settingValue,
                gotField.getValue());

        assertEquals("GettingValue is not the same the SettingValue", settingValue,
                gotFieldStd.getValue());
    }

    // NESTED Groups outside messages
    private org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup buildNestedGroupWithStandardFields(
            String settingValue) {

        // The root group
        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup gNoRelatedSym = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup();

        // The nested group
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs noLegs;
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup nestedgroup;

        noLegs = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs();

        nestedgroup = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup();
        nestedgroup.setField(new LegSymbol(settingValue));
        noLegs.add(nestedgroup);

        // Adding a second fake nested group to avoid being the case of having
        // one element which is not relevant :-)
        nestedgroup = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup();
        nestedgroup.setField(new LegSymbol("Donald"));
        noLegs.add(nestedgroup);

        gNoRelatedSym.setNoLegs(noLegs);

        return gNoRelatedSym;
    }

    private org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSymGroup buildNestedGroupWithStandardFieldsFIX50(
            String settingValue) {

        // The root group
        org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSymGroup noRelatedSymGroup = new org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSymGroup();

        // The nested group
        org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSymGroup.NoLegs noLegs;
        org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSymGroup.NoLegsGroup nestedgroup;

        noLegs = new org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSymGroup.NoLegs();
        nestedgroup = new org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSymGroup.NoLegsGroup();
        nestedgroup.setField(new LegSymbol(settingValue));
        noLegs.add(nestedgroup);

        // Adding a second fake nested group to avoid being the case of having
        // one element which is not relevant :-)
        nestedgroup = new org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSymGroup.NoLegsGroup();
        nestedgroup.setField(new LegSymbol("Donald"));
        noLegs.add(nestedgroup);

        // set nested group field
        noRelatedSymGroup.setNoLegs(noLegs);

        return noRelatedSymGroup;
    }

    @Test
    public void testSettingGettingNestedGroupWithStandardFields() throws FieldNotFound {

        final String settingValue = "SETTING_VALUE";

        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup noRelatedSymGroup;
        noRelatedSymGroup = buildNestedGroupWithStandardFields(settingValue);

        // Getting part
        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup getgrp;
        getgrp = noRelatedSymGroup.getNoLegs().get(0);
        final LegSymbol gotFieldStd = (LegSymbol) getgrp.getField(LegSymbol.TAG);

        assertEquals("GettingValue is not the same the SettingValue", settingValue,
                gotFieldStd.getValue());
    }

    private org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup buildNestedGroupWithCustomFields(
            String settingValue) {

        // The root group
        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup gNoRelatedSym = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup();

        // The nested group
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs noLegs;
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup nestedgroup;

        noLegs = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs();
        nestedgroup = noLegs
                .add(new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup());
        nestedgroup.setField(new GenericField(9001, settingValue));

        // Adding a second fake nested group to avoid being the case of having
        // one element which is not relevant :-)
        nestedgroup = noLegs
                .add(new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup());
        nestedgroup.setField(new GenericField(9001, "Donald"));

        gNoRelatedSym.setField(noLegs);

        return gNoRelatedSym;
    }

    @Test
    public void testSettingGettingNestedGroupWithCustomFields() throws FieldNotFound {

        final String settingValue = "SETTING_VALUE";

        // The root group
        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup gNoRelatedSym = buildNestedGroupWithCustomFields(settingValue);

        // Getting part
        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup getgrp;
        getgrp = gNoRelatedSym.getNoLegs().get(0);
        final FIXField<?> gotFieldStd = getgrp.getField(9001);

        assertEquals("GettingValue is not the same the SettingValue", settingValue,
                gotFieldStd.getValue());
    }

    private org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup buildNestedGroupWithCustomAndStandardFields(
            String settingValue) {

        // The root group
        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup gNoRelatedSym = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup();

        // The nested group
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs noLegs;
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup nestedgroup;

        noLegs = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs();

        nestedgroup = noLegs
                .add(new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup());
        nestedgroup.setField(new LegSymbol(settingValue));
        nestedgroup.setField(new GenericField(9001, settingValue));

        // Adding a second fake nested group to avoid being the case of having
        // one element which is not relevant :-)
        nestedgroup = noLegs
                .add(new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup());
        nestedgroup.setField(new LegSymbol("Donald"));
        nestedgroup.setField(new GenericField(9001, "Donald"));

        gNoRelatedSym.setField(noLegs);

        return gNoRelatedSym;
    }

    @Test
    public void testSettingGettingNestedGroupWithCustomAndStandardFields() throws FieldNotFound {

        final String settingValue = "SETTING_VALUE";

        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup gNoRelatedSym = buildNestedGroupWithCustomAndStandardFields(settingValue);

        // Getting part
        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup getgrp;
        getgrp = gNoRelatedSym.getNoLegs().get(0);

        final FIXField<?> gotField = getgrp.getField(9001);
        final LegSymbol gotFieldStd = (LegSymbol) getgrp.getField(LegSymbol.TAG);

        assertEquals("GettingValue is not the same the SettingValue", settingValue,
                gotField.getValue());

        assertEquals("GettingValue is not the same the SettingValue", settingValue,
                gotFieldStd.getValue());
    }

    // Testing group re-usability when setting values
    @Test
    public void testSettingGettingGroupByReusingGroup() throws FieldNotFound {

        // The root group
        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup gNoRelatedSym = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup();

        // Create the initial group
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs noLegs;
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup noLegsGroup;

        noLegs = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs();

        noLegsGroup = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup();
        final String notOverridenFieldValue = "Value1.1";
        noLegsGroup.setField(new LegSymbol(notOverridenFieldValue));
        noLegsGroup.setField(new GenericField(9001, "Value1.2"));
        noLegs.add(noLegsGroup);

        // Create the second group by cloning the first group and changing one
        // value of only one field
        noLegsGroup = (NoLegsGroup) noLegsGroup.clone();
        final String overridenFieldValue = "Value2.2";
        noLegsGroup.setField(new GenericField(9001, overridenFieldValue));
        noLegs.add(noLegsGroup);

        // Getting part
        final org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup getgrp;
        getgrp = noLegs.get(1);
        // gNoRelatedSym.getGroup(2, getgrp);

        final FIXField<?> gotField = getgrp.getField(9001);
        final LegSymbol gotFieldStd = (LegSymbol) getgrp.getField(LegSymbol.TAG);

        // Ensures that the field overriden has been set correctly
        assertEquals("GettingValue is not the same the SettingValue", overridenFieldValue,
                gotField.getValue());

        // Ensures that the field not overriden has been set correctly
        assertEquals("GettingValue is not the same the SettingValue", notOverridenFieldValue,
                gotFieldStd.getValue());
    }

    private FIXMessage buildValidatedMessage(FIXMessageDictionaryFactory dictionaryFactory,
            FIXApplication application, String messageString) {

        FIXMessageBuilderFactory factory = DefaultEngine.getDefaultEngine()
                .getMessageBuilderFactory(dictionaryFactory.getBeginString(),
                        "org.quickfixj.messages.bd");

        return MessageUtils.parse(application, factory, dictionaryFactory, messageString, true);
    }

    @Test
    public void testValidationWithNestedGroupAndStandardFields() throws InvalidMessage {

        final org.quickfixj.messages.bd.fix44.QuoteRequest quoteRequest = new org.quickfixj.messages.bd.fix44.QuoteRequest();

        // Added - TODO these should be acquired via a MessageBuilder
        quoteRequest.getHeader().setField(
                org.quickfixj.messages.bd.fix44.field.MsgType.QUOTE_REQUEST);
        quoteRequest.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.BeginString(FIXBeginString.FIX44
                        .getValue()));

        final QuoteReqID gQuoteReqID = new QuoteReqID("12342");
        //   gQuoteReqID.setValue("12342");
        quoteRequest.setField(gQuoteReqID);

        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSym noRelatedSym;
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup noRelatedSymGroup;

        noRelatedSym = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSym();

        noRelatedSymGroup = buildNestedGroupWithStandardFields("DEFAULT_VALUE");
        noRelatedSymGroup.setField(new Symbol("SYM00"));
        noRelatedSym.add(noRelatedSymGroup);

        noRelatedSymGroup = buildNestedGroupWithStandardFields("DEFAULT_VALUE");
        noRelatedSymGroup.setField(new Symbol("SYM01"));
        noRelatedSym.add(noRelatedSymGroup);

        quoteRequest.setNoRelatedSym(noRelatedSym);

        final String sourceFIXString = quoteRequest.toString();

        final org.quickfixj.messages.bd.fix44.QuoteRequest validatedMessage = (org.quickfixj.messages.bd.fix44.QuoteRequest) buildValidatedMessage(
                systemDataDictionary44, FIXApplication.FIX44, sourceFIXString);
        String validateFIXString = null;
        if (validatedMessage != null) {
            validateFIXString = validatedMessage.toString();
        }

        assertEquals("Message validation failed", sourceFIXString, validateFIXString);
    }

    @Test
    public void testValidationWithNestedGroupAndStandardFieldsFIX50SP2() {

        final org.quickfixj.messages.bd.fix50.QuoteRequest quoteRequest = new org.quickfixj.messages.bd.fix50.QuoteRequest();

        // Added - TODO these should be acquired via a MessageBuilder
        quoteRequest.getHeader().setField(
                org.quickfixj.messages.bd.fix50.field.MsgType.QUOTE_REQUEST);
        quoteRequest.getHeader().setField(
                new org.quickfixj.messages.bd.fix50.field.BeginString(FIXBeginString.FIXT11
                        .getValue()));

        quoteRequest.setField(new QuoteReqID("12342"));

        org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSym noRelatedSym;
        org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSymGroup noRelatedSymGroup;

        noRelatedSym = new org.quickfixj.messages.bd.fix50.component.QuotReqGrp.NoRelatedSym();

        noRelatedSymGroup = buildNestedGroupWithStandardFieldsFIX50("DEFAULT_VALUE");
        noRelatedSymGroup.setSymbol(new Symbol("SYM00"));
        noRelatedSymGroup.setSettlDate2(new SettlDate2("20120801"));
        noRelatedSym.add(noRelatedSymGroup);

        noRelatedSymGroup = buildNestedGroupWithStandardFieldsFIX50("DEFAULT_VALUE");
        noRelatedSymGroup.setSymbol(new Symbol("SYM00"));
        noRelatedSymGroup.setSettlDate2(new SettlDate2("20120801"));
        noRelatedSym.add(noRelatedSymGroup);

        quoteRequest.setField(noRelatedSym);

        final String sourceFIXString = quoteRequest.toString();

        //    final DataDictionary fix50DataDictionary = new DataDictionary("FIX50SP2.xml");
        final org.quickfixj.messages.bd.fix50sp2.QuoteRequest validatedMessage = (org.quickfixj.messages.bd.fix50sp2.QuoteRequest) buildValidatedMessage(
                systemDataDictionary50, FIXApplication.FIX50SP2, sourceFIXString);

        String validateFIXString = validatedMessage.toString();

        assertEquals("Message validation failed", sourceFIXString, validateFIXString);
        assertEquals(2, validatedMessage.getNoRelatedSym().size());
    }

    @Test(expected = InvalidMessage.class)
    public void testValidationWithNestedGroupAndStandardFieldsWithoutDelimiter() {

        final org.quickfixj.messages.bd.fix44.QuoteRequest quoteRequest = new org.quickfixj.messages.bd.fix44.QuoteRequest();

        final org.quickfixj.messages.bd.fix44.field.QuoteReqID gQuoteReqID = new org.quickfixj.messages.bd.fix44.field.QuoteReqID(
                "12342");
        //  gQuoteReqID.setValue("12342");
        quoteRequest.setField(gQuoteReqID);

        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSym noRelatedSym;
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup noRelatedSymGroup;

        noRelatedSym = new NoRelatedSym();
        noRelatedSymGroup = buildNestedGroupWithStandardFields("DEFAULT_VALUE");

        noRelatedSym.add(noRelatedSymGroup);
        noRelatedSym.add(noRelatedSymGroup);

        quoteRequest.setField(noRelatedSym);

        final String sourceFIXString = quoteRequest.toString();

        buildValidatedMessage(systemDataDictionary50, FIXApplication.FIX44, sourceFIXString);
    }

    @Test
    public void testGroupFieldsOrderWithCustomDataDictionary() throws InvalidMessage {

        final org.quickfixj.messages.bd.fix44.QuoteRequest quoteRequest = new org.quickfixj.messages.bd.fix44.QuoteRequest();

        // Added - TODO these should be acquired via a MessageBuilder
        quoteRequest.getHeader().setField(
                org.quickfixj.messages.bd.fix44.field.MsgType.QUOTE_REQUEST);
        quoteRequest.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.BeginString(FIXBeginString.FIX44
                        .getValue()));

        final org.quickfixj.messages.bd.fix44.field.QuoteReqID gQuoteReqID = new org.quickfixj.messages.bd.fix44.field.QuoteReqID(
                "12342");
        quoteRequest.setField(gQuoteReqID);

        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSym noRelatedSym;
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup noRelatedSymGroup;

        // The root group
        noRelatedSym = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSym();
        noRelatedSymGroup = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup();
        noRelatedSymGroup.setField(new Symbol("SYM00"));
        noRelatedSym.add(noRelatedSymGroup);

        // The nested group
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs noLegs;
        org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup nestedgroup;

        noLegs = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegs();
        noRelatedSymGroup.setNoLegs(noLegs);

        nestedgroup = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup();
        nestedgroup.setField(new LegSymbol("DEFAULT_VALUE"));
        nestedgroup.setField(new OrderID("111")); // The non ordered field
        nestedgroup.setField(new GenericField(9001, "1.9001")); // The custom non rdered field
        noLegs.add(nestedgroup);

        // Adding a second fake nested group to avoid being the case of having
        // one element which is not relevant :-)
        nestedgroup = new org.quickfixj.messages.bd.fix44.QuoteRequest.NoRelatedSymGroup.NoLegsGroup();
        nestedgroup.setField(new LegSymbol("Donald"));
        nestedgroup.setField(new OrderID("112")); // The non ordered field
        nestedgroup.setField(new GenericField(9001, "2.9001")); // The custom non ordered field
        noLegs.add(nestedgroup);

        quoteRequest.setNoRelatedSym(noRelatedSym);

        final String sourceFIXString = quoteRequest.toString();

        org.quickfixj.messages.bd.fix44.QuoteRequest validatedMessage = (org.quickfixj.messages.bd.fix44.QuoteRequest) buildValidatedMessage(
                customDataDictionary44, FIXApplication.FIX44, sourceFIXString);

        assertNull("Invalid message", validatedMessage.getParseException());

        String validatedFIXString = validatedMessage.toString();

        assertEquals("Message validation failed", MessageUtils.checksum(sourceFIXString),
                MessageUtils.checksum(validatedFIXString));
    }

    @Test
    public void testOutOfOrderGroupMembersDelimiterField() throws Exception {

        FIXMessage m = MessageUtils
                .parse(FIXApplication.FIX44,
                        new GenericMessageBuilderFactory(),
                        systemDataDictionary44,
                        "8=FIX.4.4\0019=0\00135=D\00134=2\00149=TW\00152=<TIME>\00156=ISLD\00111=ID\001"
                                + "21=1\00140=1\00154=1\00138=200.00\00155=INTC\00178=2\00180=50\00179=acct1\001"
                                + "80=150\00179=acct2\00160=<TIME>\00110=000\001", false);

        try {
            new DefaultValidator(FIXBeginString.FIX44).validate(
                    systemDataDictionary44.getMessageDictionary(FIXApplication.FIX44, "D"), m);
            Assert.fail("No exception");
        } catch (final FieldException e) {
            // expected
            assertEquals(SessionRejectReason.REPEATING_GROUP_FIELDS_OUT_OF_ORDER,
                    e.getSessionRejectReason());
            assertEquals(80, e.getField());
        }
    }

    @Test
    public void testIgnoreOutOfOrderGroupMembersDelimiterField() throws Exception {

        FIXMessage m = MessageUtils.parse(FIXApplication.FIX44, new GenericMessageBuilderFactory(),
                systemDataDictionary44,
                "8=FIX.4.4\0019=0\00135=D\00134=2\00149=TW\00152=<TIME>\00156=ISLD\00111=ID\001"
                        + "21=1\00140=1\00154=1\00138=200.00\00155=INTC\001"
                        + "78=2\00180=50\00179=acct1\00180=150\00179=acct2\001" + "60=<TIME>\001"
                        + "10=000\001", false);

        try {
            validatorWithIgnoreOutOfOrder.validate(
                    systemDataDictionary44.getMessageDictionary(FIXApplication.FIX44, "D"), m);
            Assert.fail("No exception");
        } catch (final FieldException e) {
            e.printStackTrace();
            // expected
            assertEquals(SessionRejectReason.REPEATING_GROUP_FIELDS_OUT_OF_ORDER,
                    e.getSessionRejectReason());
            assertEquals(80, e.getField());
        }
    }

    @Test
    public void testOutOfOrderGroupMembers() throws Exception {

        FIXMessage m = MessageUtils.parse(FIXApplication.FIX44, new GenericMessageBuilderFactory(),
                systemDataDictionary44,
                "8=FIX.4.4\0019=0\00135=D\00134=2\00149=TW\00152=20080203-00:29:51.453\00156=ISLD\001"
                        + "11=ID\00121=1\00140=1\00154=1\00138=200.00\00155=INTC\00178=2\001"
                        + "79=acct1\00180=50\001661=99\00179=acct2\00180=150\001661=99\001"
                        + "60=20080203-00:29:51.453\00110=000\001", false);

        try {
            validator.validate(
                    systemDataDictionary44.getMessageDictionary(FIXApplication.FIX44, "D"), m);
            Assert.fail("No exception");
        } catch (final FieldException e) {
            // expected
            assertEquals(e.getMessage(), SessionRejectReason.REPEATING_GROUP_FIELDS_OUT_OF_ORDER,
                    e.getSessionRejectReason());
            assertEquals(661, e.getField());
        }
    }

    @Test
    public void testIgnoreOutOfOrderGroupMembers() throws Exception {

        String messageString = "8=FIX.4.4\0019=0\00135=D\00134=2\00149=TW\00152=20080203-00:29:51.453\00156=ISLD\001"
                + "11=ID\00121=1\00140=1\00154=1\00138=200.00\00155=INTC\00178=2\001"
                + "79=acct1\00180=50\001661=10\001"
                + "79=acct2\00180=150\001661=11\001"
                + "60=20080203-00:29:51.453\00110=000\001";

        FIXMessageDictionary dictionary;
        MessageParser parser;
        FIXMessage message;

        dictionary = systemDataDictionary44.getMessageDictionary(FIXApplication.FIX44, "D");

        try {
            parser = new MessageParser(new GenericMessageBuilderFactory(), systemDataDictionary44,
                    validatorWithIgnoreOutOfOrder);
            message = parser.parse(FIXApplication.FIX44, messageString, false);
            validatorWithIgnoreOutOfOrder.validate(dictionary, message);
        } catch (final FieldException e) {
            Assert.fail("Exception");
        }
    }

    @Test
    public void testRequiredGroupMembers() throws Exception {

        // Missing group tag 304
        FIXMessage m = MessageUtils
                .parse(FIXApplication.FIX44,
                        new GenericMessageBuilderFactory(),
                        systemDataDictionary44,
                        "8=FIX.4.4\0019=0\00135=i\00134=2\00149=TW\001"
                                + "52=20080203-00:29:51.453\00156=ISLD\001117=ID\001296=1\001302=X\00110=000\001",
                        false);
        try {
            validator.validate(
                    systemDataDictionary44.getMessageDictionary(FIXApplication.FIX44, "i"), m);
            Assert.fail("No exception");
        } catch (final FieldException e) {
            // expected
            assertEquals(e.getMessage(), SessionRejectReason.REQUIRED_TAG_MISSING,
                    e.getSessionRejectReason());
            assertEquals(304, e.getField());
        }
    }

    @Test
    public void testWrongGroupCount() throws Exception {

        // Excessive group counts in nested group
        FIXMessage m = MessageUtils.parse(FIXApplication.FIX44, new GenericMessageBuilderFactory(),
                systemDataDictionary44, "8=FIX.4.4\0019=0\00135=i\00134=2\00149=TW\001"
                        + "52=20080203-00:29:51.453\00156=ISLD\001117=ID\001296=1\001302=X\001"
                        + "304=5\001295=50\001299=QID\00110=085\001", true);

        try {
            validator.validate(
                    systemDataDictionary44.getMessageDictionary(FIXApplication.FIX44, "i"), m);
            Assert.fail("No exception");
        } catch (final FieldException e) {
            e.printStackTrace();
            // expected
            assertEquals("Wrong reject reason: [" + e.getMessage() + "]",
                    SessionRejectReason.INCORRECT_NUMINGROUP_COUNT_FOR_REPEATING_GROUP,
                    e.getSessionRejectReason());
            assertEquals(295, e.getField());
        }
    }

    @Test
    public void testInvalidEnumFieldInGroup() throws Exception {

        // Excessive group counts
        FIXMessage m = MessageUtils
                .parse(FIXApplication.FIX44,
                        new GenericMessageBuilderFactory(),
                        systemDataDictionary44,
                        "8=FIX.4.4\0019=0\00135=A\00134=2\00152=20080203-00:29:51.453\00156=ISLD\001"
                                + "49=TW\001108=10\001384=1\001372=D\001385=X\00198=0\00110=129\001",
                        false);

        try {
            validator.validate(
                    systemDataDictionary44.getMessageDictionary(FIXApplication.FIX44, "A"), m);
            Assert.fail("No exception");
        } catch (final IncorrectTagValue e) {
            // expected
            assertEquals(385, e.field);
        }
    }

    @Test
    public void testSettingGettingGroupWithStandardFieldsInHeader() throws Exception {

        FIXMessage m = MessageUtils.parse(FIXApplication.FIX44, new GenericMessageBuilderFactory(),
                systemDataDictionary44,
                "8=FIX.4.4\0019=87\00135=0\00134=2\00152=20080203-00:29:51.453\00156=ISLD\00149=TW\001627=2\001"
                        + "628=_TED02A\001629=20090717-13:25:31.896\001"
                        + "628=_GWSURV\001629=20090717-13:25:31.928\00110=012\001", false);

        try {
            validator.validate(
                    systemDataDictionary44.getMessageDictionary(FIXApplication.FIX44, "0"), m);
        } catch (final IncorrectTagValue e) {
            // not expected
            Assert.fail("Exception occured");
        }
    }
}
