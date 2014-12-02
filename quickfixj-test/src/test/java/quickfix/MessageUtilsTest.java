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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

import org.junit.Ignore;
import org.quickfixj.FIXBeginString;
import org.quickfixj.spi.MessageBuilderServiceLoader;

import quickfix.fix40.Logon;
import quickfix.fix42.field.BeginString;
import quickfix.fix50.Email;
import quickfix.fix50.field.ApplVerID;
import quickfix.fix50.field.EmailThreadID;
import quickfix.fix50.field.EmailType;
import quickfix.fix50.field.MsgType;
import quickfix.fix50.field.SenderCompID;
import quickfix.fix50.field.Subject;
import quickfix.fix50.field.TargetCompID;
import quickfix.fixt11.field.DefaultApplVerID;
import quickfix.fixt11.field.EncryptMethod;
import quickfix.fixt11.field.HeartBtInt;
import junit.framework.TestCase;

public class MessageUtilsTest extends TestCase {

    public void testGetStringField() throws Exception {

        String messageString = "8=FIX.4.2\0019=12\00135=X\001108=30\00110=049\001";
        assertEquals("wrong value", "FIX.4.2",
                MessageUtils.getStringField(messageString, FixTags.BEGIN_STRING));
        assertEquals("wrong value", "X",
                MessageUtils.getStringField(messageString, FixTags.MSG_TYPE));
        assertNull(messageString, MessageUtils.getStringField(messageString, SenderCompID.TAG));
    }

    public void testSessionIdFromMessage() throws Exception {

        Message message = new Logon();
        message.getHeader().setString(SenderCompID.TAG, "TW");
        message.getHeader().setString(TargetCompID.TAG, "ISLD");

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(quickfix.fix40.field.MsgType.LOGON);
        message.getHeader().setField(new BeginString(FIXBeginString.FIX40.getValue()));

        SessionID sessionID = MessageUtils.getSessionID(message);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals("TW", sessionID.getSenderCompID());
        assertEquals("ISLD", sessionID.getTargetCompID());
    }

    public void testReverseSessionIdFromMessage() throws Exception {

        Message message = new Logon();
        message.getHeader().setString(SenderCompID.TAG, "TW");
        message.getHeader().setString(TargetCompID.TAG, "ISLD");

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(quickfix.fix40.field.MsgType.LOGON);
        message.getHeader().setField(new BeginString(FIXBeginString.FIX40.getValue()));

        SessionID sessionID = MessageUtils.getReverseSessionID(message);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals("ISLD", sessionID.getSenderCompID());
        assertEquals("TW", sessionID.getTargetCompID());
    }

    public void testReverseSessionIdFromMessageWithMissingFields() throws Exception {

        Message message = new Logon();

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(quickfix.fix40.field.MsgType.LOGON);
        message.getHeader().setField(new BeginString(FIXBeginString.FIX40.getValue()));

        SessionID sessionID = MessageUtils.getReverseSessionID(message);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals(sessionID.getSenderCompID(), SessionID.NOT_SET);
        assertEquals(sessionID.getTargetCompID(), SessionID.NOT_SET);
    }

    public void testSessionIdFromRawMessage() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
        SessionID sessionID = MessageUtils.getSessionID(messageString);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals("TW", sessionID.getSenderCompID());
        assertEquals("ISLD", sessionID.getTargetCompID());
    }

    public void testReverseSessionIdFromRawMessage() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\00150=TWS\001"
                + "142=TWL\00152=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
        SessionID sessionID = MessageUtils.getReverseSessionID(messageString);
        assertEquals(FIXBeginString.FIX40, sessionID.getBeginString());
        assertEquals("ISLD", sessionID.getSenderCompID());
        assertEquals("TW", sessionID.getTargetCompID());
        assertEquals("TWS", sessionID.getTargetSubID());
        assertEquals("TWL", sessionID.getTargetLocationID());
    }

    public void testMessageType() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
        assertEquals("A", MessageUtils.getMessageType(messageString));
    }

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

    public void testMessageTypeError2() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00135=1";
        try {
            MessageUtils.getMessageType(messageString);
            fail("expected exception");
        } catch (InvalidMessage e) {
            // expected
        }
    }

    public void testGetNonexistentStringField() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";
        assertNull(MessageUtils.getStringField(messageString, 35));
    }

    public void testGetStringFieldWithBadValue() throws Exception {

        String messageString = "8=FIX.4.0\0019=56\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223";
        assertNull(MessageUtils.getStringField(messageString, 10));
    }

    public void testParse() throws Exception {

        Session mockSession = mock(Session.class);
        DataDictionaryProvider mockDataDictionaryProvider = mock(DataDictionaryProvider.class);
        stub(mockSession.getDataDictionaryProvider()).toReturn(mockDataDictionaryProvider);
        stub(mockSession.getMessageFactory()).toReturn(
                MessageBuilderServiceLoader.getMessageBuilderFactory());
        String messageString = "8=FIX.4.0\0019=56\00135=A\00134=1\00149=TW\001"
                + "52=20060118-16:34:19\00156=ISLD\00198=0\001108=2\00110=223\001";

        Message message = MessageUtils.parse(mockSession, messageString);

        assertThat(message, is(notNullValue()));
    }

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
        //        //        Message message = MessageUtils.parse(new quickfix.fix40.MessageFactory(),
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

    public void testParseFixt() throws Exception {

        Session mockSession = mock(Session.class);
        DataDictionaryProvider mockDataDictionaryProvider = mock(DataDictionaryProvider.class);
        stub(mockSession.getDataDictionaryProvider()).toReturn(mockDataDictionaryProvider);
        stub(mockSession.getMessageFactory()).toReturn(
                MessageBuilderServiceLoader.getMessageBuilderFactory());

        Email email = new Email(new quickfix.fix50.field.EmailThreadID("THREAD_ID"),
                quickfix.fix50.field.EmailType.NEW, new quickfix.fix50.field.Subject("SUBJECT"));
        email.getHeader().setField(
                new quickfix.fix50.field.ApplVerID(ApplVerID.FIX42.getCharacters()));
        email.getHeader().setField(new quickfix.fix50.field.SenderCompID("SENDER"));
        email.getHeader().setField(new quickfix.fix50.field.TargetCompID("TARGET"));

        // Added - TODO these should be acquired via a MessageBuilder
        email.getHeader().setField(quickfix.fixt11.field.MsgType.EMAIL);
        email.getHeader().setField(
                new quickfix.fix50.field.BeginString(FIXBeginString.FIXT11.getValue()));

        Message message = MessageUtils.parse(mockSession, email.toString());

        assertThat(message, is(notNullValue()));
        assertThat(message, is(quickfix.fix40.Email.class));
    }

    public void testParseFixtLogon() throws Exception {

        Session mockSession = mock(Session.class);
        DataDictionaryProvider mockDataDictionaryProvider = mock(DataDictionaryProvider.class);
        stub(mockSession.getDataDictionaryProvider()).toReturn(mockDataDictionaryProvider);
        stub(mockSession.getMessageFactory()).toReturn(
                MessageBuilderServiceLoader.getMessageBuilderFactory());

        quickfix.fixt11.Logon logon = new quickfix.fixt11.Logon(EncryptMethod.NONE_OTHER,
                new HeartBtInt(30), new DefaultApplVerID(ApplVerID.FIX42.getCharacters()));

        // Added - TODO these should be acquired via a MessageBuilder
        logon.getHeader().setField(quickfix.fixt11.field.MsgType.LOGON);
        logon.getHeader().setField(
                new quickfix.fix50.field.BeginString(FIXBeginString.FIXT11.getValue()));

        Message message = MessageUtils.parse(mockSession, logon.toString());

        assertThat(message, is(notNullValue()));
        assertThat(message, is(quickfix.fixt11.Logon.class));
    }

    public void testParseFix50() throws Exception {

        Session mockSession = mock(Session.class);
        DataDictionaryProvider mockDataDictionaryProvider = mock(DataDictionaryProvider.class);
        stub(mockSession.getDataDictionaryProvider()).toReturn(mockDataDictionaryProvider);
        stub(mockSession.getMessageFactory()).toReturn(
                MessageBuilderServiceLoader.getMessageBuilderFactory());

        Email email = new Email(new EmailThreadID("THREAD_ID"), EmailType.NEW, new Subject(
                "SUBJECT"));
        email.getHeader().setField(ApplVerID.FIX50);
        email.getHeader().setField(new SenderCompID("SENDER"));
        email.getHeader().setField(new TargetCompID("TARGET"));

        // Added - TODO these should be acquired via a MessageBuilder
        email.getHeader().setField(MsgType.EMAIL);
        email.getHeader().setField(
                new quickfix.fix50.field.BeginString(FIXBeginString.FIXT11.getValue()));

        Message message = MessageUtils.parse(mockSession, email.toString());

        assertThat(message, is(notNullValue()));
        assertThat(message, is(quickfix.fix50.Email.class));
    }
}
