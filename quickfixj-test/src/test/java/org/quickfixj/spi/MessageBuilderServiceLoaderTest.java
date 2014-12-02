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
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.MessageBuilder;
import org.quickfixj.MessageBuilder.GroupBuilder;

import quickfix.Message;
import quickfix.fix44.component.Parties.NoPartyIDs;
import quickfix.fix44.component.Parties.NoPartyIDsGroup;
import quickfix.fix44.field.PartyID;
import quickfix.fix44.field.PartySubID;

/**
 * MessageBuilderServiceLoaderTest - provides...
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public class MessageBuilderServiceLoaderTest {

    @Test
    public void testMessageBuilder() {

        MessageBuilder builder;

        builder = MessageBuilderServiceLoader.getMessageBuilderFactory().getMessageBuilder(
                FIXBeginString.FIX40, FIXApplication.FIX40, "0");

        assertBuilderCreateMessage(builder, quickfix.fix40.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.0", quickfix.fix40.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST", quickfix.fix40.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9", quickfix.fix40.field.TestReqID.class);

        builder = MessageBuilderServiceLoader.getMessageBuilderFactory().getMessageBuilder(
                FIXBeginString.FIX41, FIXApplication.FIX41, "0");

        assertBuilderCreateMessage(builder, quickfix.fix41.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.0", quickfix.fix41.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST", quickfix.fix41.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9", quickfix.fix41.field.TestReqID.class);

        builder = MessageBuilderServiceLoader.getMessageBuilderFactory().getMessageBuilder(
                FIXBeginString.FIX42, FIXApplication.FIX42, "0");

        assertBuilderCreateMessage(builder, quickfix.fix42.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.0", quickfix.fix42.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST", quickfix.fix42.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9", quickfix.fix42.field.TestReqID.class);

        builder = MessageBuilderServiceLoader.getMessageBuilderFactory().getMessageBuilder(
                FIXBeginString.FIX42, FIXApplication.FIX42, "E");

        assertBuilderCreateMessage(builder, quickfix.fix42.NewOrderList.class);
        assertBuilderCreateField(builder, 8, "FIX.4.0", quickfix.fix42.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST", quickfix.fix42.field.SenderCompID.class);
        assertBuilderCreateField(builder, 73, "1", quickfix.fix42.NewOrderList.NoOrders.class);

        builder = MessageBuilderServiceLoader.getMessageBuilderFactory().getMessageBuilder(
                FIXBeginString.FIX43, FIXApplication.FIX43, "0");

        assertBuilderCreateMessage(builder, quickfix.fix43.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.0", quickfix.fix43.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST", quickfix.fix43.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9", quickfix.fix43.field.TestReqID.class);

        builder = MessageBuilderServiceLoader.getMessageBuilderFactory().getMessageBuilder(
                FIXBeginString.FIX44, FIXApplication.FIX44, "0");

        assertBuilderCreateMessage(builder, quickfix.fix44.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIX.4.0", quickfix.fix44.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST", quickfix.fix44.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9", quickfix.fix44.field.TestReqID.class);

        builder = MessageBuilderServiceLoader.getMessageBuilderFactory().getMessageBuilder(
                FIXBeginString.FIXT11, FIXApplication.FIX44, "0");

        assertBuilderCreateMessage(builder, quickfix.fixt11.Heartbeat.class);
        assertBuilderCreateField(builder, 8, "FIXT1.1", quickfix.fixt11.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST", quickfix.fixt11.field.SenderCompID.class);
        assertBuilderCreateField(builder, 112, "9", quickfix.fixt11.field.TestReqID.class);

        builder = MessageBuilderServiceLoader.getMessageBuilderFactory().getMessageBuilder(
                FIXBeginString.FIXT11, FIXApplication.FIX44, "o");

        assertBuilderCreateMessage(builder, quickfix.fix44.RegistrationInstructions.class);
        assertBuilderCreateField(builder, 8, "FIXT1.1", quickfix.fixt11.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST", quickfix.fixt11.field.SenderCompID.class);
        assertBuilderCreateField(builder, 1, "Test", quickfix.fix44.field.Account.class);
        assertBuilderCreateField(builder, 453, "2", NoPartyIDs.class);
    }

    @Test
    public void testNestedGroupBuilder() {

        MessageBuilder builder;
        GroupBuilder<?> groupBuilder;

        builder = MessageBuilderServiceLoader.getMessageBuilderFactory().getMessageBuilder(
                FIXBeginString.FIXT11, FIXApplication.FIX44, "o");

        assertBuilderCreateMessage(builder, quickfix.fix44.RegistrationInstructions.class);
        assertBuilderCreateField(builder, 8, "FIXT1.1", quickfix.fixt11.field.BeginString.class);
        assertBuilderCreateField(builder, 49, "TEST", quickfix.fixt11.field.SenderCompID.class);
        assertBuilderCreateField(builder, 1, "Test", quickfix.fix44.field.Account.class);
        assertBuilderCreateField(builder, 453, "2", NoPartyIDs.class);

        groupBuilder = builder.create((NoPartyIDs) builder.create(453, "2"));
        assertSame(groupBuilder.create().getClass(), NoPartyIDsGroup.class);
        assertSame(groupBuilder.create(448, "Test").getClass(), PartyID.class);

        groupBuilder = groupBuilder.create((NoPartyIDsGroup.NoPartySubIDs) groupBuilder.create(802,
                "2"));
        assertSame(groupBuilder.create().getClass(), NoPartyIDsGroup.NoPartySubIDsGroup.class);
        assertSame(groupBuilder.create(523, "Test").getClass(), PartySubID.class);
    }

    private void assertBuilderCreateMessage(MessageBuilder builder,
            Class<? extends Message> expectedType) {

        Message message = builder.create();

        System.out.println("MessageBuilderServiceLoaderTest.test() " + message.getClass());

        assertSame(message.getClass(), expectedType);
    }

    private void assertBuilderCreateField(MessageBuilder builder, int tag, String value,
            Class<? extends FIXField<?>> expectedType) {

        FIXField<?> field = builder.create(tag, value);

        System.out.printf("MessageBuilderServiceLoaderTest.test() %s %s %s %n", field.getClass(),
                field.getTag(), field.getCharacters());

        assertEquals(field.getTag(), tag);
        assertEquals(field.getCharacters().toString(), value);
        assertSame(field.getClass(), expectedType);
    }
}
