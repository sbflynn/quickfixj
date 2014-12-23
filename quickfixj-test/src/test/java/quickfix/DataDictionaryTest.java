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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXFieldType;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.field.FieldException;
import org.quickfixj.field.GenericField;
import org.quickfixj.messages.bd.fix44.NewOrderSingle;

import quickfix.test.util.ExpectedTestFailure;

public class DataDictionaryTest {

    private static FIXMessageDictionaryFactory dataDictionary;

    @BeforeClass
    public static void beforeClass() throws Exception {

        DefaultEngine engine = new DefaultEngine();
        org.quickfixj.messages.bd.fix44.DictionaryService builderService = new org.quickfixj.messages.bd.fix44.DictionaryService();
        engine.getRegistry().register(builderService.getEngine(), builderService.getDictionaries());
        engine.build();

        dataDictionary = engine.getMessageDictionaryFactory(FIXBeginString.FIX44,
                "org.quickfixj.messages.bd");
    }

    @AfterClass
    public static void afterClass() {

        dataDictionary = null;
    }

    @Test
    public void testDictionary() throws Exception {

        FIXMessageDictionary dd = dataDictionary.getMessageDictionary(FIXApplication.FIX44, "7");

        assertEquals("wrong field name", "Text", dd.getFieldDictionary(58).getName());
        assertEquals("wrong field type", FIXFieldType.STRING, dd.getFieldDictionary(58).getType());

        assertEquals("wrong field name", "Currency", dd.getFieldDictionary(15).getName());
        assertEquals("wrong field type", FIXFieldType.CURRENCY, dd.getFieldDictionary(15).getType());

        assertEquals("wrong field name", "Account",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "8")
                        .getFieldDictionary(1).getName());
        assertEquals("wrong field name", FIXFieldType.STRING,
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "8")
                        .getFieldDictionary(1).getType());

        //    assertEquals("wrong value description", "BUY", dd.getValueName(4, "B"));
        //    assertEquals("wrong value type", FIXFieldType.STRING, dd.getFieldDictionary(1).getType());
        assertEquals("wrong version", FIXApplication.FIX44, dd.getApplication());

        assertTrue("unexpected field values existence",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "8")
                        .getFieldDictionary(1).getFieldValues().isEmpty());
        assertFalse("unexpected field values nonexistence", dd.getFieldDictionary(4)
                .getFieldValues().isEmpty());

        assertFalse("unexpected field existence", dd.isField(9999));
        assertTrue("unexpected field nonexistence", dd.isField(4));
        assertTrue("unexpected field value existence", !dd.getFieldDictionary(4).getFieldValues()
                .contains("C"));
        assertTrue("unexpected field value nonexistence", dd.getFieldDictionary(4).getFieldValues()
                .contains("B"));
        assertTrue("wrong group info",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "A").isGroupField(384));
        assertFalse("wrong group info",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "A").isGroupField(1));
        assertNotNull(
                "wrong group info",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "6").getGroupDictionary(
                        232));
        assertTrue("incorrect header field", dd.isHeaderField(8));
        assertFalse("incorrect header field", dd.isHeaderField(1));
        assertTrue("incorrect trailer field", dd.isTrailerField(89));
        assertFalse("incorrect trailer field", dd.isTrailerField(1));
        assertTrue("incorrect message field",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "A").isField(98));
        assertFalse("incorrect message field",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "A").isField(1));
        // component field
        assertTrue("incorrect message field",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "6").isField(235));
        // group->component field
        // assertTrue("incorrect message field", dd.isMsgField("6", 311));
        assertNotNull("incorrect message type",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "A"));
        assertNull("incorrect message type",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "%"));
        assertTrue("incorrect field requirement",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "A").getRequiredFields()
                        .contains(98));
        assertFalse("incorrect field requirement",
                dataDictionary.getMessageDictionary(FIXApplication.FIX44, "A").getRequiredFields()
                        .contains(95));
        //     assertEquals("incorrect msg type", "0", dd.getMsgType("Heartbeat"));
        //    assertEquals("incorrect msg type", "B", dd.getMsgType("News"));
        //    assertFalse(dd.isMsgField("UNKNOWN_TYPE", 1));
    }

    @Test
    @Ignore
    public void testMissingFieldAttributeForRequired() throws Exception {

        String data = "";
        data += "<fix major=\"4\" minor=\"0\">";
        data += "  <header>";
        data += "    <field name=\"BeginString\" required=\"Y\"/>";
        data += "  </header>";
        data += "  <trailer>";
        data += "    <field name=\"CheckSum\" required=\"Y\"/>";
        data += "  </trailer>";
        data += "  <fields>";
        data += "    <field number=\"8\" name=\"BeginString\" type=\"STRING\"/>";
        data += "    <field number=\"10\" name=\"CheckSum\" type=\"STRING\"/>";
        data += "    <field number=\"112\" name=\"TestReqID\" type=\"STRING\"/>";
        data += "  </fields>";
        data += "  <messages>";
        data += "    <message name=\"Heartbeat\" msgtype=\"0\" msgcat=\"admin\">";
        data += "      <field name=\"TestReqID\"/>";
        data += "    </message>";
        data += "  </messages>";
        data += "</fix>";

        assertConfigErrorForMissingAttributeRequired(data);
    }

    private void assertConfigErrorForMissingAttributeRequired(String data) {

        //        try {
        //            new DataDictionary(new ByteArrayInputStream(data.getBytes()));
        //        } catch (ConfigError e) {
        //            // Expected
        //            assertTrue(e.getMessage().contains("does not have a 'required'"));
        //        }
    }

    @Test
    @Ignore
    public void testMissingComponentAttributeForRequired() throws Exception {

        String data = "";
        data += "<fix major=\"4\" minor=\"0\">";
        data += "  <header>";
        data += "    <field name=\"BeginString\" required=\"Y\"/>";
        data += "  </header>";
        data += "  <trailer>";
        data += "    <field name=\"CheckSum\" required=\"Y\"/>";
        data += "  </trailer>";
        data += "  <fields>";
        data += "    <field number=\"8\" name=\"BeginString\" type=\"STRING\"/>";
        data += "    <field number=\"10\" name=\"CheckSum\" type=\"STRING\"/>";
        data += "    <field number=\"112\" name=\"TestReqID\" type=\"STRING\"/>";
        data += "  </fields>";
        data += "  <messages>";
        data += "    <message name=\"Heartbeat\" msgtype=\"0\" msgcat=\"admin\">";
        data += "      <component name=\"Instrument\"/>";
        data += "    </message>";
        data += "  </messages>";
        data += "</fix>";

        assertConfigErrorForMissingAttributeRequired(data);
    }

    @Test
    @Ignore
    public void testMissingGroupAttributeForRequired() throws Exception {

        String data = "";
        data += "<fix major=\"4\" minor=\"0\">";
        data += "  <header>";
        data += "    <field name=\"BeginString\" required=\"Y\"/>";
        data += "  </header>";
        data += "  <trailer>";
        data += "    <field name=\"CheckSum\" required=\"Y\"/>";
        data += "  </trailer>";
        data += "  <fields>";
        data += "    <field number=\"8\" name=\"BeginString\" type=\"STRING\"/>";
        data += "    <field number=\"10\" name=\"CheckSum\" type=\"STRING\"/>";
        data += "    <field number=\"112\" name=\"TestReqID\" type=\"STRING\"/>";
        data += "  </fields>";
        data += "  <messages>";
        data += "    <message name=\"Heartbeat\" msgtype=\"0\" msgcat=\"admin\">";
        data += "      <group name=\"TestReqID\"/>";
        data += "    </message>";
        data += "  </messages>";
        data += "</fix>";

        assertConfigErrorForMissingAttributeRequired(data);
    }

    @Test
    @Ignore
    public void testHeaderTrailerRequired() throws Exception {

        String data = "";
        data += "<fix major=\"4\" minor=\"0\" xmlns=\"http://quickfixj.org/xml/dictionary\">";
        data += "  <header>";
        data += "    <field name=\"BeginString\" required=\"Y\"/>";
        data += "    <field name=\"OnBehalfOfCompID\" required=\"N\"/>";
        data += "  </header>";
        data += "  <trailer>";
        data += "    <field name=\"CheckSum\" required=\"Y\"/>";
        data += "    <field name=\"Signature\" required=\"N\"/>";
        data += "  </trailer>";
        data += "  <messages>";
        data += "    <message name=\"Heartbeat\" msgtype=\"0\" msgcat=\"admin\">";
        data += "      <group name=\"TestReqID\" required=\"N\">";
        data += "           <field name=\"MyField\" required=\"Y\"/>";
        data += "      </group>";
        data += "    </message>";
        data += "  </messages>";
        data += "  <fields>";
        data += "    <field number=\"8\" name=\"BeginString\" type=\"STRING\"/>";
        data += "    <field number=\"115\" name=\"OnBehalfOfCompID\" type=\"STRING\"/>";
        data += "    <field number=\"10\" name=\"CheckSum\" type=\"STRING\"/>";
        data += "    <field number=\"89\" name=\"Signature\" type=\"STRING\"/>";
        data += "    <field number=\"37\" name=\"TestReqID\" type=\"STRING\"/>";
        data += "    <field number=\"4000\" name=\"MyField\" type=\"STRING\"/>";
        data += "  </fields>";
        data += "</fix>";

        //        DataDictionary dd = new DataDictionary(new ByteArrayInputStream(data.getBytes()));
        //        assertTrue("BeginString should be required", dd.isRequiredHeaderField(8));
        //        assertFalse("OnBehalfOfCompID should not be required", dd.isRequiredHeaderField(115));
        //        assertTrue("Checksum should be required", dd.isRequiredTrailerField(10));
        //        assertFalse("Signature should not be required", dd.isRequiredTrailerField(89));
        //
        //        // now tests for fields that aren't actually in the dictionary - should
        //        // come back false
        //        assertFalse("Unknown header field shows up as required", dd.isRequiredHeaderField(666));
        //        assertFalse("Unknown trailer field shows up as required", dd.isRequiredTrailerField(666));
    }

    @Test
    public void testHeaderGroupField() throws Exception {

        FIXMessageDictionary dd = dataDictionary.getMessageDictionary(FIXApplication.FIX44, "D");

        assertTrue(dd.isHeaderField(org.quickfixj.messages.bd.fix44.field.NoHops.TAG));
        assertTrue(dd.isGroupField(org.quickfixj.messages.bd.fix44.field.NoHops.TAG));
    }

    @Test
    public void testMessageValidateBodyOnly() throws Exception {

        final org.quickfixj.messages.bd.fix44.NewOrderSingle newSingle = new org.quickfixj.messages.bd.fix44.NewOrderSingle(
                new org.quickfixj.messages.bd.fix44.field.ClOrdID("123"),
                org.quickfixj.messages.bd.fix44.field.Side.BUY,
                new org.quickfixj.messages.bd.fix44.field.TransactTime(new Date()),
                org.quickfixj.messages.bd.fix44.field.OrdType.LIMIT);
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.OrderQty(42));
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.Price(42.37));
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.Symbol("QFJ"));
        newSingle.setField(org.quickfixj.messages.bd.fix44.field.TimeInForce.DAY);
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.Account("testAccount"));

        // this will fail header validation
        newSingle.getHeader().setField(new GenericField(21, ""));

        // Added - TODO these should be acquired via a MessageBuilder
        newSingle.getHeader().setField(org.quickfixj.messages.bd.fix44.field.MsgType.ORDER_SINGLE);
        newSingle.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.BeginString(FIXBeginString.FIX44
                        .getValue()));

        final FIXMessageDictionary dd = dataDictionary.getMessageDictionary(FIXApplication.FIX44,
                "D");

        new ExpectedTestFailure(FieldException.class, "field=") {

            @Override
            protected void execute() throws Throwable {

                new DefaultValidator(FIXBeginString.FIX44).validate(dd, newSingle);
            }
        }.run();

        new DefaultValidator(FIXBeginString.FIX44).validate(dd, newSingle, true);
    }

    @Test
    public void testMessageDataDictionaryMismatch() throws Exception {

        final org.quickfixj.messages.bd.fix43.NewOrderSingle newSingle = new org.quickfixj.messages.bd.fix43.NewOrderSingle(
                new org.quickfixj.messages.bd.fix43.field.ClOrdID("123"),
                new org.quickfixj.messages.bd.fix43.field.HandlInst(
                        org.quickfixj.messages.bd.fix43.field.HandlInst.MANUAL_ORDER.getValue()),
                new org.quickfixj.messages.bd.fix43.field.Side(
                        org.quickfixj.messages.bd.fix43.field.Side.BUY.getValue()),
                new org.quickfixj.messages.bd.fix43.field.TransactTime(new Date()),
                new org.quickfixj.messages.bd.fix43.field.OrdType(
                        org.quickfixj.messages.bd.fix43.field.OrdType.LIMIT.getValue()));
        newSingle.setField(new org.quickfixj.messages.bd.fix43.field.OrderQty(42));
        newSingle.setField(new org.quickfixj.messages.bd.fix43.field.Price(42.37));
        newSingle.setField(new org.quickfixj.messages.bd.fix43.field.Symbol("QFJ"));
        newSingle.setField(org.quickfixj.messages.bd.fix50sp2.field.TimeInForce.DAY);
        newSingle.setField(new org.quickfixj.messages.bd.fix43.field.Account("testAccount"));

        // Added - TODO these should be acquired via a MessageBuilder
        newSingle.getHeader().setField(org.quickfixj.messages.bd.fix43.field.MsgType.ORDER_SINGLE);
        newSingle.getHeader().setField(
                new org.quickfixj.messages.bd.fix43.field.BeginString(FIXBeginString.FIX43
                        .getValue()));

        final FIXMessageDictionary dd = dataDictionary.getMessageDictionary(FIXApplication.FIX44,
                "D");

        new ExpectedTestFailure(UnsupportedVersion.class,
                "Message version 'FIX.4.3' does not match the data dictionary version 'FIX.4.4'") {

            @Override
            protected void execute() throws Throwable {

                new DefaultValidator(FIXBeginString.FIX44).validate(dd, newSingle);
            }
        }.run();

        // TODO: This is unexpected for pre-FIX 5.0 messages:
        // If bodyOnly is true, the correct data dictionary is not checked.
        new DefaultValidator(FIXBeginString.FIX44).validate(dd, newSingle, true);
    }

    // QF C++ treats the string argument as a filename although it's
    // named 'url'. QFJ string argument can be either but this test
    // ensures the DD works correctly with a regular file path.
    @Test
    @Ignore
    public void testDictionaryWithFilename() throws Exception {
        //        MetadataDictionaryFactory factory = new MetadataDictionaryFactory(
        //                DictionaryMetadata.getTransportMetadata("/FIX40.xml"));
        //        factory.add(DictionaryMetadata.getApplicationMetadata("/FIX40.xml"));
        //        FIXMessageDictionary dictionary = factory.getMessageDictionary(FIXApplication.FIX40, "7");
        //        assertEquals("wrong field name", "Currency", dictionary.getFieldDictionary(15).getName());
        //
        //        //        DataDictionary dd = new DataDictionary("FIX40.xml");
        //        //        assertEquals("wrong field name", "Currency", dd.getFieldName(15));
        //        // It worked!
    }

    // Support finding DD in classpath
    @Test
    @Ignore
    public void testDictionaryInClassPath() throws Exception {
        //
        //        URLClassLoader customClassLoader = new URLClassLoader(new URL[] { new URL("file:etc") },
        //                getClass().getClassLoader());
        //        Thread currentThread = Thread.currentThread();
        //        ClassLoader previousContextClassLoader = currentThread.getContextClassLoader();
        //        currentThread.setContextClassLoader(customClassLoader);
        //        try {
        //            MetadataDictionaryFactory factory = new MetadataDictionaryFactory(
        //                    DictionaryMetadata.getTransportMetadata("/FIX44.xml"));
        //            factory.add(DictionaryMetadata.getApplicationMetadata("/FIX44.xml"));
        //            FIXMessageDictionary dictionary = factory.getMessageDictionary(FIXApplication.FIX44,
        //                    "7");
        //            assertEquals("wrong field name", "Currency", dictionary.getFieldDictionary(15)
        //                    .getName());
        //            // It worked!
        //        } finally {
        //            currentThread.setContextClassLoader(previousContextClassLoader);
        //        }
    }

    // QFJ-235
    @Test
    public void testWildcardEnumValue() throws Exception {

        FIXMessageDictionary dd = dataDictionary.getMessageDictionary(FIXApplication.FIX44, "D");

        assertTrue(dd.getFieldDictionary(65).allowOtherFieldValues());
    }

    @Test
    @Ignore
    public void testMessageCategory() throws Exception {

        //        DataDictionary dd = getDictionary();
        //        assertTrue(dd.isAdminMessage(FixMessageTypes.LOGON));
        //        assertFalse(dd.isAppMessage(FixMessageTypes.LOGON));
        //        assertFalse(dd.isAdminMessage(MsgType.ORDER_SINGLE.getValue()));
        //        assertTrue(dd.isAppMessage(MsgType.ORDER_SINGLE.getValue()));
    }

    @Test
    public void testAllowUnknownFields() throws Exception {

        final org.quickfixj.messages.bd.fix44.NewOrderSingle newSingle = new org.quickfixj.messages.bd.fix44.NewOrderSingle(
                new org.quickfixj.messages.bd.fix44.field.ClOrdID("123"),
                org.quickfixj.messages.bd.fix44.field.Side.BUY,
                new org.quickfixj.messages.bd.fix44.field.TransactTime(new Date()),
                org.quickfixj.messages.bd.fix44.field.OrdType.LIMIT);

        // Added - TODO these should be acquired via a MessageBuilder
        newSingle.getHeader().setField(org.quickfixj.messages.bd.fix44.field.MsgType.ORDER_SINGLE);
        newSingle.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.BeginString(FIXBeginString.FIX44
                        .getValue()));

        newSingle.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.SenderCompID("SENDER"));
        newSingle.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.TargetCompID("TARGET"));
        newSingle.getHeader().setField(new org.quickfixj.messages.bd.fix44.field.BodyLength(100));
        newSingle.getHeader().setField(new org.quickfixj.messages.bd.fix44.field.MsgSeqNum(25));
        newSingle.getHeader().setField(
                new org.quickfixj.messages.bd.fix44.field.SendingTime(new Date()));
        newSingle.getTrailer().setField(new org.quickfixj.messages.bd.fix44.field.CheckSum("100"));
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.OrderQty(42));
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.Price(42.37));
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.Symbol("QFJ"));
        newSingle.setField(org.quickfixj.messages.bd.fix44.field.HandlInst.MANUAL_ORDER);
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.TimeInForce(
                org.quickfixj.messages.bd.fix44.field.TimeInForce.DAY.getValue()));
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.Account("testAccount"));

        // Invalid field for this message
        newSingle.setField(new org.quickfixj.messages.bd.fix44.field.LastMkt("FOO"));

        new ExpectedTestFailure(FieldException.class, "field=") {

            @Override
            protected void execute() throws Throwable {

                DefaultValidator validator = new DefaultValidator(FIXBeginString.FIX44);
                FIXMessageDictionary dictionary = dataDictionary.getMessageDictionary(
                        FIXApplication.FIX44, "D");

                validator.validate(dictionary, newSingle);
            }
        }.run();

        DefaultValidator validator = new DefaultValidator(FIXBeginString.FIX44);
        FIXMessageDictionary dictionary = dataDictionary.getMessageDictionary(FIXApplication.FIX44,
                "D");

        validator.setAllowUnknownMessageFields(true);
        validator.validate(dictionary, newSingle);
    }

    // QFJ-535
    @Test
    public void testValidateFieldsOutOfOrderForGroups() throws Exception {

        DefaultValidator validator = new DefaultValidator(FIXBeginString.FIX44);
        validator.setCheckUnorderedGroupFields(false);

        FIXMessage messageWithGroupLevel1 = MessageUtils
                .parse(FIXApplication.FIX44,
                        new Message(),
                        dataDictionary,
                        validator,
                        "8=FIX.4.4\0019=185\00135=D\00134=25\00149=SENDER\00156=TARGET\00152=20110412-13:43:00\001"
                                + "60=20110412-13:43:00\0011=testAccount\00111=123\00121=3\00138=42\00140=2\00144=42.37\001"
                                + "54=1\00155=QFJ\00159=0\00178=1\00179=allocAccount\001736=currency\001661=1\00110=130\001",
                        true);

        validator.validate(dataDictionary.getMessageDictionary(FIXApplication.FIX44, "D"),
                messageWithGroupLevel1);

        FIXMessage messageWithGroupLevel2 = MessageUtils
                .parse(FIXApplication.FIX44,
                        new Message(),
                        dataDictionary,
                        validator,
                        "8=FIX.4.4\0019=185\00135=D\00134=25\00149=SENDER\00156=TARGET\00152=20110412-13:43:00\001"
                                + "60=20110412-13:43:00\0011=testAccount\00111=123\00121=3\00138=42\00140=2\00144=42.37\001"
                                + "54=1\00155=QFJ\00159=0\00178=1\00179=allocAccount\001539=1\001524=1\001538=1\001525=a\00110=145\001",
                        true);

        validator.validate(dataDictionary.getMessageDictionary(FIXApplication.FIX44, "D"),
                messageWithGroupLevel2);
    }

    // QFJ-535
    @Test
    public void testNewOrderSingleWithCorrectTag50() throws Exception {

        String correctFixMessage = "8=FIX.4.4\0019=218\00135=D\00149=cust\00150=trader\001"
                + "56=FixGateway\00134=449\00152=20110420-09:17:40\00111=clordid\00154=1\00138=50\001"
                + "59=6\00140=2\00144=77.1\001432=20110531\00115=CHF\00122=8\00155=symbol\001"
                + "48=CH1234.CHF\00121=1\00160=20110420-11:17:39.000\00163=0\001207=VX\00110=009\001";

        FIXMessageDictionary dictionary = dataDictionary.getMessageDictionary(FIXApplication.FIX44,
                "D");
        DefaultValidator validator = new DefaultValidator(FIXBeginString.FIX44);

        // in any case, it must be validated as the message is correct

        // CASE #1 : doValidation and checkFieldsOutOfOrder
        validator.setCheckFieldsOutOfOrder(true);
        final NewOrderSingle nos1 = MessageUtils.parse(FIXApplication.FIX44, new NewOrderSingle(),
                dataDictionary, validator, correctFixMessage, true);
        validator.validate(dictionary, nos1);
        assertTrue(nos1.getHeader().isFieldSet(
                org.quickfixj.messages.bd.fix44.field.SenderSubID.TAG));

        // CASE #2 :  doNotValidation and checkFieldsOutOfOrder
        validator.setCheckFieldsOutOfOrder(true);
        final NewOrderSingle nos2 = MessageUtils.parse(FIXApplication.FIX44, new NewOrderSingle(),
                dataDictionary, validator, correctFixMessage, false);
        validator.validate(dictionary, nos2);
        assertTrue(nos2.getHeader().isFieldSet(
                org.quickfixj.messages.bd.fix44.field.SenderSubID.TAG));

        // CASE #3 :  doValidation and no checkFieldsOutOfOrder
        validator.setCheckFieldsOutOfOrder(false);
        final NewOrderSingle nos3 = MessageUtils.parse(FIXApplication.FIX44, new NewOrderSingle(),
                dataDictionary, validator, correctFixMessage, true);
        validator.validate(dictionary, nos3);
        assertTrue(nos3.getHeader().isFieldSet(
                org.quickfixj.messages.bd.fix44.field.SenderSubID.TAG));

        // CASE #4 :  doNotValidation and no checkFieldsOutOfOrder
        validator.setCheckFieldsOutOfOrder(false);
        final NewOrderSingle nos4 = MessageUtils.parse(FIXApplication.FIX44, new NewOrderSingle(),
                dataDictionary, validator, correctFixMessage, false);
        validator.validate(dictionary, nos4);
        assertTrue(nos4.getHeader().isFieldSet(
                org.quickfixj.messages.bd.fix44.field.SenderSubID.TAG));
    }

    @Test
    public void testNewOrderSingleWithMisplacedTag50() throws Exception {

        String incorrectFixMessage = "8=FIX.4.4\0019=218\00135=D\00149=cust\00156=FixGateway\001"
                + "34=449\00152=20110420-09:17:40\00111=clordid\00154=1\00138=50\00159=6\00140=2\001"
                + "44=77.1\001432=20110531\00115=CHF\00122=8\00155=symbol\00148=CH1234.CHF\00121=1\001"
                + "60=20110420-11:17:39.000\00163=0\001207=VX\00150=trader\00110=009\001";

        FIXMessageDictionary dictionary = dataDictionary.getMessageDictionary(FIXApplication.FIX44,
                "D");
        DefaultValidator validator = new DefaultValidator(FIXBeginString.FIX44);

        // doValidation and checkFieldsOutOfOrder -> should fail
        try {
            System.out.println("DataDictionaryTest.testNewOrderSingleWithMisplacedTag50() "
                    + dataDictionary);
            MessageUtils.parse(FIXApplication.FIX44, new NewOrderSingle(), dataDictionary,
                    validator, incorrectFixMessage, true);
        } catch (FieldException fe) {
            fe.printStackTrace();
            // expected exception
        }

        // doNotValidation and checkFieldsOutOfOrder -> should NOT fail
        final NewOrderSingle nos2 = MessageUtils.parse(FIXApplication.FIX44, new NewOrderSingle(),
                dataDictionary, validator, incorrectFixMessage, false);
        validator.validate(dictionary, nos2);
        assertTrue(nos2.getHeader().isFieldSet(
                org.quickfixj.messages.bd.fix44.field.SenderSubID.TAG));

        // doValidation and no checkFieldsOutOfOrder -> should NOT fail
        validator.setCheckFieldsOutOfOrder(false);
        final NewOrderSingle nos3 = MessageUtils.parse(FIXApplication.FIX44, new NewOrderSingle(),
                dataDictionary, validator, incorrectFixMessage, true);
        validator.validate(dictionary, nos3);
        assertTrue(nos3.getHeader().isFieldSet(
                org.quickfixj.messages.bd.fix44.field.SenderSubID.TAG));

        // doNotValidation and no checkFieldsOutOfOrder -> should NOT fail
        validator.setCheckFieldsOutOfOrder(false);
        NewOrderSingle nos4 = MessageUtils.parse(FIXApplication.FIX44, new NewOrderSingle(),
                dataDictionary, validator, incorrectFixMessage, false);
        validator.validate(dictionary, nos4);
        assertTrue(nos4.getHeader().isFieldSet(
                org.quickfixj.messages.bd.fix44.field.SenderSubID.TAG));
    }
}
