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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

import java.math.BigDecimal;

import org.junit.Ignore;
import org.junit.Test;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.field.FieldException;
import org.quickfixj.field.GenericField;
import org.quickfixj.messages.bd.fix40.Logon;
import org.quickfixj.messages.bd.fix42.field.BeginString;
import org.quickfixj.messages.bd.fix44.field.AccruedInterestAmt;
import org.quickfixj.messages.bd.fix50.Email;
import org.quickfixj.messages.bd.fix50.field.ApplVerID;
import org.quickfixj.messages.bd.fix50.field.EmailThreadID;
import org.quickfixj.messages.bd.fix50.field.EmailType;
import org.quickfixj.messages.bd.fix50.field.MsgType;
import org.quickfixj.messages.bd.fix50.field.SenderCompID;
import org.quickfixj.messages.bd.fix50.field.Subject;
import org.quickfixj.messages.bd.fix50.field.TargetCompID;
import org.quickfixj.messages.fixt11.field.DefaultApplVerID;
import org.quickfixj.messages.fixt11.field.EncryptMethod;
import org.quickfixj.messages.fixt11.field.HeartBtInt;

public class MessageUtilsTest {

    @Test
    public void testGetStringField() throws Exception {

        String messageString = "8=FIX.4.2\0019=12\00135=X\001108=30\00110=049\001";
        assertEquals("wrong value", "FIX.4.2",
                MessageUtils.getStringField(messageString, FIXTag.BEGIN_STRING));
        assertEquals("wrong value", "X",
                MessageUtils.getStringField(messageString, FIXTag.MSG_TYPE));
        assertNull(messageString, MessageUtils.getStringField(messageString, SenderCompID.TAG));
    }

    @Test
    public void testSessionIdFromMessage() throws Exception {

        FIXMessage message = new org.quickfixj.messages.bd.fix40.Logon();

        message.getHeader().setField(new org.quickfixj.messages.bd.fix40.field.SenderCompID("TW"));
        message.getHeader()
                .setField(new org.quickfixj.messages.bd.fix40.field.TargetCompID("ISLD"));

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(org.quickfixj.messages.bd.fix40.field.MsgType.LOGON);
        message.getHeader().setField(new BeginString(FIXBeginString.FIX40.getValue()));

        SessionID sessionID = MessageUtils.getSessionID(message);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals("TW", sessionID.getSenderCompID());
        assertEquals("ISLD", sessionID.getTargetCompID());
    }

    @Test
    public void testReverseSessionIdFromMessage() throws Exception {

        FIXMessage message = new org.quickfixj.messages.bd.fix40.Logon();

        message.getHeader().setField(new org.quickfixj.messages.bd.fix40.field.SenderCompID("TW"));
        message.getHeader()
                .setField(new org.quickfixj.messages.bd.fix40.field.TargetCompID("ISLD"));

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(org.quickfixj.messages.bd.fix40.field.MsgType.LOGON);
        message.getHeader().setField(new BeginString(FIXBeginString.FIX40.getValue()));

        FIXSessionID sessionID = MessageUtils.getReverseSessionID(message);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals("ISLD", sessionID.getSenderCompID());
        assertEquals("TW", sessionID.getTargetCompID());
    }

    @Test
    public void testReverseSessionIdFromMessageWithMissingFields() throws Exception {

        FIXMessage message = new Logon();

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(org.quickfixj.messages.bd.fix40.field.MsgType.LOGON);
        message.getHeader().setField(new BeginString(FIXBeginString.FIX40.getValue()));

        FIXSessionID sessionID = MessageUtils.getReverseSessionID(message);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals(sessionID.getSenderCompID(), SessionID.NOT_SET);
        assertEquals(sessionID.getTargetCompID(), SessionID.NOT_SET);
    }

    @Test
    public void testSessionIdFromRawMessage() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
        SessionID sessionID = MessageUtils.getSessionID(messageString);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals("TW", sessionID.getSenderCompID());
        assertEquals("ISLD", sessionID.getTargetCompID());
    }

    @Test
    public void testReverseSessionIdFromRawMessage() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\00150=TWS\001"
                + "142=TWL\00152=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
        FIXSessionID sessionID = MessageUtils.getReverseSessionID(messageString);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals("ISLD", sessionID.getSenderCompID());
        assertEquals("TW", sessionID.getTargetCompID());
        assertEquals("TWS", sessionID.getTargetSubID());
        assertEquals("TWL", sessionID.getTargetLocationID());
    }

    @Test
    public void testMessageType() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
        assertEquals("A", MessageUtils.getMessageType(messageString));
    }

    @Test
    public void testMessageTypeError() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
        try {
            MessageUtils.getMessageType(messageString);
            fail("expected exception");
        } catch (InvalidMessage e) {
            // expected
        }
    }

    @Test
    public void testMessageTypeError2() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00135=1";
        try {
            MessageUtils.getMessageType(messageString);
            fail("expected exception");
        } catch (InvalidMessage e) {
            // expected
        }
    }

    @Test
    public void testGetNonexistentStringField() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
        assertNull(MessageUtils.getStringField(messageString, 35));
    }

    @Test
    public void testGetStringFieldWithBadValue() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223";
        assertNull(MessageUtils.getStringField(messageString, 10));
    }

    @Test
    public void testParse() throws Exception {

        Session mockSession = mock(Session.class);

        FIXMessageDictionaryFactory dataDictionary = DefaultEngine.getDefaultEngine()
                .getMessageDictionaryFactory(FIXBeginString.FIX40, "org.quickfixj.messages.bd");
        FIXMessageBuilderFactory builderFactory = DefaultEngine.getDefaultEngine()
                .getMessageBuilderFactory(FIXBeginString.FIX40, "org.quickfixj.messages.bd");

        stub(mockSession.getDataDictionary()).toReturn(dataDictionary);
        stub(mockSession.getMessageFactory()).toReturn(builderFactory);

        String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";

        FIXMessage message = MessageUtils.parse(mockSession, messageString);

        assertThat(message, is(notNullValue()));
    }

    @Test
    @Ignore
    // deprecated method under test no longer exists
    // TODO remove
    public void testLegacyParse() throws Exception {

        //        String data = "8=FIX.4.4\0019=309\00135=8\00149=ASX\00156=CL1_FIX44\00134=4\001"
        //                + "52=20060324-01:05:58\00117=X-B-WOW-1494E9A0:58BD3F9D-1109\001150=D\001"
        //                + "39=0\00111=184271\00138=200\001198=1494E9A0:58BD3F9D\001526=4324\001"
        //                + "37=B-WOW-1494E9A0:58BD3F9D\00155=WOW\00154=1\001151=200\00114=0\00140=2\001"
        //                + "44=15\00159=1\0016=0\001453=3\001448=AAA35791\001447=D\001452=3\001448=8\001"
        //                + "447=D\001452=4\001448=FIX11\001447=D\001452=36\00160=20060320-03:34:29\00110=169\001";
        //
        //        //        Message message = MessageUtils.parse(new org.quickfixj.messages.bd.fix40.MessageFactory(),
        //        //                DataDictionaryTest.getDictionary(), data);
        //        Message message = MessageUtils.parse(new MessageFactory() {
        //
        //            @Override
        //            public Group create(FIXBeginString beginString, String msgType, int correspondingFieldID) {
        //                return null;
        //            }
        //
        //            @Override
        //            public Message create(FIXBeginString beginString, String msgType) {
        //                System.out
        //                        .println("MessageUtilsTest.testLegacyParse().new MessageFactory() {...}.create() "
        //                                + msgType);
        //                return new Message();
        //            }
        //        }, DataDictionaryTest.getDictionary(), data);
        //        assertThat(message, is(notNullValue()));
    }

    @Test
    public void testParseFixt() throws Exception {

        Session mockSession = mock(Session.class);

        FIXMessageDictionaryFactory dataDictionary = DefaultEngine.getDefaultEngine()
                .getMessageDictionaryFactory(FIXBeginString.FIXT11, "org.quickfixj.messages.bd");
        FIXMessageBuilderFactory builderFactory = DefaultEngine.getDefaultEngine()
                .getMessageBuilderFactory(FIXBeginString.FIXT11, "org.quickfixj.messages.bd");

        stub(mockSession.getDataDictionary()).toReturn(dataDictionary);
        stub(mockSession.getMessageFactory()).toReturn(builderFactory);

        Email email = new Email(
                new org.quickfixj.messages.bd.fix50.field.EmailThreadID("THREAD_ID"),
                org.quickfixj.messages.bd.fix50.field.EmailType.NEW,
                new org.quickfixj.messages.bd.fix50.field.Subject("SUBJECT"));
        email.getHeader().setField(
                new org.quickfixj.messages.bd.fix50.field.ApplVerID(ApplVerID.FIX40.getValue()));
        email.getHeader()
                .setField(new org.quickfixj.messages.bd.fix50.field.SenderCompID("SENDER"));
        email.getHeader()
                .setField(new org.quickfixj.messages.bd.fix50.field.TargetCompID("TARGET"));

        // Added - TODO these should be acquired via a MessageBuilder
        email.getHeader().setField(org.quickfixj.messages.fixt11.field.MsgType.EMAIL);
        email.getHeader().setField(
                new org.quickfixj.messages.bd.fix50.field.BeginString(FIXBeginString.FIXT11
                        .getValue()));

        FIXMessage message = MessageUtils.parse(mockSession, email.toString());

        assertThat(message, is(notNullValue()));
        assertThat(message, is(org.quickfixj.messages.bd.fix40.Email.class));
    }

    @Test
    public void testParseFixtLogon() throws Exception {

        Session mockSession = mock(Session.class);

        FIXMessageDictionaryFactory dataDictionary = DefaultEngine.getDefaultEngine()
                .getMessageDictionaryFactory(FIXBeginString.FIXT11, "org.quickfixj.messages.bd");
        FIXMessageBuilderFactory builderFactory = DefaultEngine.getDefaultEngine()
                .getMessageBuilderFactory(FIXBeginString.FIXT11, "org.quickfixj.messages.bd");

        stub(mockSession.getDataDictionary()).toReturn(dataDictionary);
        stub(mockSession.getMessageFactory()).toReturn(builderFactory);

        org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
                EncryptMethod.NONE_OTHER, new HeartBtInt(30), new DefaultApplVerID(
                        ApplVerID.FIX42.getValue()));

        // Added - TODO these should be acquired via a MessageBuilder
        logon.getHeader().setField(org.quickfixj.messages.fixt11.field.MsgType.LOGON);
        logon.getHeader().setField(
                new org.quickfixj.messages.bd.fix50.field.BeginString(FIXBeginString.FIXT11
                        .getValue()));

        FIXMessage message = MessageUtils.parse(mockSession, logon.toString());

        assertThat(message, is(notNullValue()));
        assertThat(message, is(org.quickfixj.messages.fixt11.Logon.class));
    }

    @Test
    public void testParseFix50() throws Exception {

        Session mockSession = mock(Session.class);

        FIXMessageDictionaryFactory dataDictionary = DefaultEngine.getDefaultEngine()
                .getMessageDictionaryFactory(FIXBeginString.FIXT11, "org.quickfixj.messages.bd");
        FIXMessageBuilderFactory builderFactory = DefaultEngine.getDefaultEngine()
                .getMessageBuilderFactory(FIXBeginString.FIXT11, "org.quickfixj.messages.bd");

        stub(mockSession.getDataDictionary()).toReturn(dataDictionary);
        stub(mockSession.getMessageFactory()).toReturn(builderFactory);

        Email email = new Email(new EmailThreadID("THREAD_ID"), EmailType.NEW, new Subject(
                "SUBJECT"));
        email.getHeader().setField(ApplVerID.FIX50);
        email.getHeader().setField(new SenderCompID("SENDER"));
        email.getHeader().setField(new TargetCompID("TARGET"));

        // Added - TODO these should be acquired via a MessageBuilder
        email.getHeader().setField(MsgType.EMAIL);
        email.getHeader().setField(
                new org.quickfixj.messages.bd.fix50.field.BeginString(FIXBeginString.FIXT11
                        .getValue()));

        FIXMessage message = MessageUtils.parse(mockSession, email.toString());

        assertThat(message, is(notNullValue()));
        assertThat(message, is(org.quickfixj.messages.bd.fix50.Email.class));
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test
    public void testCoerceToBoolean() {

        Message message = new Message();

        message.setField(new GenericField(7, true));
        assertTrue(MessageUtils.coerceToBoolean(message, 7));

        message.setField(new GenericField(7, false));
        assertFalse(MessageUtils.coerceToBoolean(message, 7));
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldNotFound.class)
    public void testCoerceToBoolean_FAIL_FieldNotFound() {

        MessageUtils.coerceToBoolean(new Message(), 7);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldException.class)
    public void testCoerceToBoolean_FAIL_FieldException() {

        Message message = new Message();

        message.setField(new GenericField(7, "BAD"));
        MessageUtils.coerceToBoolean(message, 7);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test
    public void testCoerceToChar() {

        Message message = new Message();

        message.setField(new GenericField(7, "A"));
        assertEquals('A', MessageUtils.coerceToChar(message, 7));
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldNotFound.class)
    public void testCoerceToChar_FAIL_FieldNotFound() {

        MessageUtils.coerceToChar(new Message(), 7);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldException.class)
    public void testCoerceToChar_FAIL_FieldException() {

        Message message = new Message();

        message.setField(new GenericField(7, "BAD"));
        MessageUtils.coerceToChar(message, 7);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test
    public void testCoerceToInt() {

        Message message = new Message();

        message.setField(new GenericField(7, "1"));
        assertEquals(1, MessageUtils.coerceToInt(message, 7));
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldNotFound.class)
    public void testCoerceToInt_FAIL_FieldNotFound() {

        MessageUtils.coerceToInt(new Message(), 7);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldException.class)
    public void testCoerceToInt_FAIL_FieldException() {

        Message message = new Message();

        message.setField(new GenericField(7, "f"));
        MessageUtils.coerceToInt(message, 7);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test
    public void testCoerceToDouble() {

        Message message = new Message();

        message.setField(new GenericField(7, "1.012"));
        assertEquals(1.012, MessageUtils.coerceToDouble(message, 7), 1e-10);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldNotFound.class)
    public void testCoerceToDouble_FAIL_FieldNotFound() {

        MessageUtils.coerceToDouble(new Message(), 7);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldException.class)
    public void testCoerceToDouble_FAIL_FieldException() {

        Message message = new Message();

        message.setField(new GenericField(7, "f"));
        MessageUtils.coerceToDouble(message, 7);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test
    public void testCoerceToBigDecimal() {

        Message message = new Message();

        BigDecimal data = new BigDecimal("1.012");
        message.setField(new AccruedInterestAmt(data));
        assertSame(data, MessageUtils.coerceToBigDecimal(message, 159));

        message.setField(new GenericField(7, "1.012"));
        assertEquals(new BigDecimal("1.012"), MessageUtils.coerceToBigDecimal(message, 7));
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldNotFound.class)
    public void testCoerceToBigDecimal_FAIL_FieldNotFound() {

        MessageUtils.coerceToBigDecimal(new Message(), 7);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    @Test(expected = FieldException.class)
    public void testCoerceToBigDecimal_FAIL_FieldException() {

        Message message = new Message();

        message.setField(new GenericField(7, "f"));
        MessageUtils.coerceToBigDecimal(message, 7);
    }
}
