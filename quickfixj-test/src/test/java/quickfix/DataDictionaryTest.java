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

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;

import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXFieldType;

import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.field.MsgType;
import quickfix.test.util.ExpectedTestFailure;
import junit.framework.TestCase;

public class DataDictionaryTest extends TestCase {

    public DataDictionaryTest(String arg0) {

        super(arg0);
    }

    public void testDictionary() throws Exception {

        DataDictionary dd = getDictionary();

        assertEquals("wrong field name", "Currency", dd.getFieldName(15));
        assertEquals("wrong value description", "BUY", dd.getValueName(4, "B"));
        assertEquals("wrong value type", FIXFieldType.STRING, dd.getFieldType(1));
        assertEquals("wrong version", FixVersions.BEGINSTRING_FIX44, dd.getVersion());
        assertFalse("unexpected field values existence", dd.hasFieldValue(1));
        assertTrue("unexpected field values nonexistence", dd.hasFieldValue(4));
        assertFalse("unexpected field existence", dd.isField(9999));
        assertTrue("unexpected field nonexistence", dd.isField(4));
        assertTrue("unexpected field value existence", !dd.isFieldValue(4, "C"));
        assertTrue("unexpected field value nonexistence", dd.isFieldValue(4, "B"));
        assertTrue("wrong group info", dd.isGroup("A", 384));
        assertFalse("wrong group info", dd.isGroup("A", 1));
        assertNotNull("wrong group info", dd.getGroup("6", 232));
        assertTrue("incorrect header field", dd.isHeaderField(8));
        assertFalse("incorrect header field", dd.isHeaderField(1));
        assertTrue("incorrect trailer field", dd.isTrailerField(89));
        assertFalse("incorrect trailer field", dd.isTrailerField(1));
        assertTrue("incorrect message field", dd.isMsgField("A", 98));
        assertFalse("incorrect message field", dd.isMsgField("A", 1));
        // component field
        assertTrue("incorrect message field", dd.isMsgField("6", 235));
        // group->component field
        // assertTrue("incorrect message field", dd.isMsgField("6", 311));
        assertTrue("incorrect message type", dd.isMsgType("A"));
        assertFalse("incorrect message type", dd.isMsgType("%"));
        assertTrue("incorrect field requirement", dd.isRequiredField("A", 98));
        assertFalse("incorrect field requirement", dd.isRequiredField("A", 95));
        assertEquals("incorrect field name", "Account", dd.getFieldName(1));
        assertEquals("incorrect msg type", "0", dd.getMsgType("Heartbeat"));
        assertEquals("incorrect msg type", "B", dd.getMsgType("News"));
        assertFalse(dd.isMsgField("UNKNOWN_TYPE", 1));
    }

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

        try {
            new DataDictionary(new ByteArrayInputStream(data.getBytes()));
        } catch (ConfigError e) {
            // Expected
            assertTrue(e.getMessage().contains("does not have a 'required'"));
        }
    }

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

        DataDictionary dd = new DataDictionary(new ByteArrayInputStream(data.getBytes()));
        assertTrue("BeginString should be required", dd.isRequiredHeaderField(8));
        assertFalse("OnBehalfOfCompID should not be required", dd.isRequiredHeaderField(115));
        assertTrue("Checksum should be required", dd.isRequiredTrailerField(10));
        assertFalse("Signature should not be required", dd.isRequiredTrailerField(89));

        // now tests for fields that aren't actually in the dictionary - should
        // come back false
        assertFalse("Unknown header field shows up as required", dd.isRequiredHeaderField(666));
        assertFalse("Unknown trailer field shows up as required", dd.isRequiredTrailerField(666));
    }

    public void testHeaderGroupField() throws Exception {

        DataDictionary dd = getDictionary();
        assertTrue(dd.isHeaderGroup(quickfix.fix44.field.NoHops.TAG));
    }

    public void testMessageValidateBodyOnly() throws Exception {

        final quickfix.fix44.NewOrderSingle newSingle = new quickfix.fix44.NewOrderSingle(
                new quickfix.fix44.field.ClOrdID("123"), new quickfix.fix44.field.Side('1'),
                new quickfix.fix44.field.TransactTime(new Date()),
                new quickfix.fix44.field.OrdType(quickfix.fix44.field.OrdType.LIMIT.getValue()));
        newSingle.setField(new quickfix.fix44.field.OrderQty(42));
        newSingle.setField(new quickfix.fix44.field.Price(42.37));
        newSingle.setField(new quickfix.fix44.field.Symbol("QFJ"));
        newSingle.setField(quickfix.fix44.field.HandlInst.MANUAL_ORDER);
        newSingle.setField(new quickfix.fix44.field.TimeInForce(
                quickfix.fix44.field.TimeInForce.DAY.getValue()));
        newSingle.setField(new quickfix.fix44.field.Account("testAccount"));

        // Added - TODO these should be acquired via a MessageBuilder
        newSingle.getHeader().setField(quickfix.fix44.field.MsgType.ORDER_SINGLE);
        newSingle.getHeader().setField(
                new quickfix.fix44.field.BeginString(FIXBeginString.FIX44.getValue()));

        final DataDictionary dd = getDictionary();
        new ExpectedTestFailure(FieldException.class, "field=") {

            @Override
            protected void execute() throws Throwable {

                dd.validate(newSingle);
            }
        }.run();

        dd.validate(newSingle, true);
    }

    public void testMessageDataDictionaryMismatch() throws Exception {

        final quickfix.fix43.NewOrderSingle newSingle = new quickfix.fix43.NewOrderSingle(
                new quickfix.fix43.field.ClOrdID("123"), new quickfix.fix43.field.HandlInst(
                        quickfix.fix43.field.HandlInst.MANUAL_ORDER.getValue()),
                new quickfix.fix43.field.Side(quickfix.fix43.field.Side.BUY.getValue()),
                new quickfix.fix43.field.TransactTime(new Date()),
                new quickfix.fix43.field.OrdType(quickfix.fix43.field.OrdType.LIMIT.getValue()));
        newSingle.setField(new quickfix.fix43.field.OrderQty(42));
        newSingle.setField(new quickfix.fix43.field.Price(42.37));
        newSingle.setField(new quickfix.fix43.field.Symbol("QFJ"));
        newSingle.setField(quickfix.fix50sp2.field.TimeInForce.DAY);
        newSingle.setField(new quickfix.fix43.field.Account("testAccount"));

        // Added - TODO these should be acquired via a MessageBuilder
        newSingle.getHeader().setField(quickfix.fix43.field.MsgType.ORDER_SINGLE);
        newSingle.getHeader().setField(
                new quickfix.fix43.field.BeginString(FIXBeginString.FIX43.getValue()));

        final DataDictionary dd = getDictionary();
        new ExpectedTestFailure(UnsupportedVersion.class,
                "Message version 'FIX.4.3' does not match the data dictionary version 'FIX.4.4'") {

            @Override
            protected void execute() throws Throwable {

                dd.validate(newSingle);
            }
        }.run();

        // TODO: This is unexpected for pre-FIX 5.0 messages:
        // If bodyOnly is true, the correct data dictionary is not checked.
        dd.validate(newSingle, true);
    }

    // QF C++ treats the string argument as a filename although it's
    // named 'url'. QFJ string argument can be either but this test
    // ensures the DD works correctly with a regular file path.
    public void testDictionaryWithFilename() throws Exception {

        DataDictionary dd = new DataDictionary("FIX40.xml");
        assertEquals("wrong field name", "Currency", dd.getFieldName(15));
        // It worked!
    }

    // Support finding DD in classpath
    public void testDictionaryInClassPath() throws Exception {

        URLClassLoader customClassLoader = new URLClassLoader(new URL[] { new URL("file:etc") },
                getClass().getClassLoader());
        Thread currentThread = Thread.currentThread();
        ClassLoader previousContextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(customClassLoader);
        try {
            DataDictionary dd = new DataDictionary("FIX40.xml");
            assertEquals("wrong field name", "Currency", dd.getFieldName(15));
            // It worked!
        } finally {
            currentThread.setContextClassLoader(previousContextClassLoader);
        }
    }

    // QFJ-235
    public void testWildcardEnumValue() throws Exception {

        DataDictionary dd = getDictionary();
        assertTrue(dd.isFieldValue(65, "FOO"));
    }

    public void testMessageCategory() throws Exception {

        DataDictionary dd = getDictionary();
        assertTrue(dd.isAdminMessage(FixMessageTypes.LOGON));
        assertFalse(dd.isAppMessage(FixMessageTypes.LOGON));
        assertFalse(dd.isAdminMessage(MsgType.ORDER_SINGLE.getValue()));
        assertTrue(dd.isAppMessage(MsgType.ORDER_SINGLE.getValue()));
    }

    public void testAllowUnknownFields() throws Exception {

        final quickfix.fix44.NewOrderSingle newSingle = new quickfix.fix44.NewOrderSingle(
                new quickfix.fix44.field.ClOrdID("123"), quickfix.fix44.field.Side.BUY,
                new quickfix.fix44.field.TransactTime(new Date()),
                quickfix.fix44.field.OrdType.LIMIT);

        // Added - TODO these should be acquired via a MessageBuilder
        newSingle.getHeader().setField(quickfix.fix44.field.MsgType.ORDER_SINGLE);
        newSingle.getHeader().setField(
                new quickfix.fix44.field.BeginString(FIXBeginString.FIX44.getValue()));

        newSingle.getHeader().setField(new quickfix.fix44.field.SenderCompID("SENDER"));
        newSingle.getHeader().setField(new quickfix.fix44.field.TargetCompID("TARGET"));
        newSingle.getHeader().setField(new quickfix.fix44.field.BodyLength(100));
        newSingle.getHeader().setField(new quickfix.fix44.field.MsgSeqNum(25));
        newSingle.getHeader().setField(new quickfix.fix44.field.SendingTime(new Date()));
        newSingle.getTrailer().setField(new quickfix.fix44.field.CheckSum("100"));
        newSingle.setField(new quickfix.fix44.field.OrderQty(42));
        newSingle.setField(new quickfix.fix44.field.Price(42.37));
        newSingle.setField(new quickfix.fix44.field.Symbol("QFJ"));
        newSingle.setField(quickfix.fix44.field.HandlInst.MANUAL_ORDER);
        newSingle.setField(new quickfix.fix44.field.TimeInForce(
                quickfix.fix44.field.TimeInForce.DAY.getValue()));
        newSingle.setField(new quickfix.fix44.field.Account("testAccount"));

        // Invalid field for this message
        newSingle.setField(new quickfix.fix44.field.LastMkt("FOO"));

        final DataDictionary dictionary = new DataDictionary(getDictionary());

        new ExpectedTestFailure(FieldException.class, "field=") {

            @Override
            protected void execute() throws Throwable {

                dictionary.validate(newSingle);
            }
        }.run();

        dictionary.setAllowUnknownMessageFields(true);
        dictionary.validate(newSingle);
    }

    // QFJ-535
    public void testValidateFieldsOutOfOrderForGroups() throws Exception {

        final DataDictionary dictionary = new DataDictionary(getDictionary());
        dictionary.setCheckUnorderedGroupFields(false);
        Message messageWithGroupLevel1 = new Message(
                "8=FIX.4.4\0019=185\00135=D\00134=25\00149=SENDER\00156=TARGET\00152=20110412-13:43:00\001"
                        + "60=20110412-13:43:00\0011=testAccount\00111=123\00121=3\00138=42\00140=2\00144=42.37\001"
                        + "54=1\00155=QFJ\00159=0\00178=1\00179=allocAccount\001736=currency\001661=1\00110=130\001",
                dictionary);
        dictionary.validate(messageWithGroupLevel1);

        Message messageWithGroupLevel2 = new Message(
                "8=FIX.4.4\0019=185\00135=D\00134=25\00149=SENDER\00156=TARGET\00152=20110412-13:43:00\001"
                        + "60=20110412-13:43:00\0011=testAccount\00111=123\00121=3\00138=42\00140=2\00144=42.37\001"
                        + "54=1\00155=QFJ\00159=0\00178=1\00179=allocAccount\001539=1\001524=1\001538=1\001525=a\00110=145\001",
                dictionary);
        dictionary.validate(messageWithGroupLevel2);
    }

    // QFJ-535
    public void testNewOrderSingleWithCorrectTag50() throws Exception {

        final DataDictionary dataDictionary = new DataDictionary(getDictionary());
        dataDictionary.setCheckFieldsOutOfOrder(true);

        String correctFixMessage = "8=FIX.4.4\0019=218\00135=D\00149=cust\00150=trader\001"
                + "56=FixGateway\00134=449\00152=20110420-09:17:40\00111=clordid\00154=1\00138=50\001"
                + "59=6\00140=2\00144=77.1\001432=20110531\00115=CHF\00122=8\00155=symbol\001"
                + "48=CH1234.CHF\00121=1\00160=20110420-11:17:39.000\00163=0\001207=VX\00110=009\001";

        // in any case, it must be validated as the message is correct
        // doValidation and checkFieldsOutOfOrder
        final NewOrderSingle nos1 = new NewOrderSingle();
        MessageUtils.parse(nos1, correctFixMessage, dataDictionary, true);
        dataDictionary.validate(nos1);
        assertTrue(nos1.getHeader().isSetField(new quickfix.fix44.field.SenderSubID("trader")));

        // doNotValidation and checkFieldsOutOfOrder
        final NewOrderSingle nos2 = new NewOrderSingle();
        MessageUtils.parse(nos2, correctFixMessage, dataDictionary, false);
        dataDictionary.validate(nos2);
        assertTrue(nos2.getHeader().isSetField(new quickfix.fix44.field.SenderSubID("trader")));

        dataDictionary.setCheckFieldsOutOfOrder(false);

        // doValidation and no checkFieldsOutOfOrder
        final NewOrderSingle nos3 = new NewOrderSingle();
        MessageUtils.parse(nos3, correctFixMessage, dataDictionary, true);
        dataDictionary.validate(nos3);
        assertTrue(nos3.getHeader().isSetField(new quickfix.fix44.field.SenderSubID("trader")));

        // doNotValidation and no checkFieldsOutOfOrder
        final NewOrderSingle nos4 = new NewOrderSingle();
        MessageUtils.parse(nos4, correctFixMessage, dataDictionary, false);
        dataDictionary.validate(nos4);
        assertTrue(nos4.getHeader().isSetField(new quickfix.fix44.field.SenderSubID("trader")));
    }

    public void testNewOrderSingleWithMisplacedTag50() throws Exception {

        final DataDictionary dataDictionary = new DataDictionary(getDictionary());
        dataDictionary.setCheckFieldsOutOfOrder(true);

        String incorrectFixMessage = "8=FIX.4.4\0019=218\00135=D\00149=cust\00156=FixGateway\001"
                + "34=449\00152=20110420-09:17:40\00111=clordid\00154=1\00138=50\00159=6\00140=2\001"
                + "44=77.1\001432=20110531\00115=CHF\00122=8\00155=symbol\00148=CH1234.CHF\00121=1\001"
                + "60=20110420-11:17:39.000\00163=0\001207=VX\00150=trader\00110=009\001";

        // doValidation and checkFieldsOutOfOrder -> should fail
        final NewOrderSingle nos1 = new NewOrderSingle();
        try {
            MessageUtils.parse(nos1, incorrectFixMessage, dataDictionary, true);
        } catch (FieldException fe) {
            // expected exception
        }

        // doNotValidation and checkFieldsOutOfOrder -> should NOT fail
        final NewOrderSingle nos2 = new NewOrderSingle();
        MessageUtils.parse(nos2, incorrectFixMessage, dataDictionary, false);
        dataDictionary.validate(nos2);
        assertTrue(nos2.getHeader().isSetField(new quickfix.fix44.field.SenderSubID("trader")));

        dataDictionary.setCheckFieldsOutOfOrder(false);

        // doValidation and no checkFieldsOutOfOrder -> should NOT fail
        final NewOrderSingle nos3 = new NewOrderSingle();
        MessageUtils.parse(nos3, incorrectFixMessage, dataDictionary, true);
        dataDictionary.validate(nos3);
        assertTrue(nos3.getHeader().isSetField(new quickfix.fix44.field.SenderSubID("trader")));

        // doNotValidation and no checkFieldsOutOfOrder -> should NOT fail
        final NewOrderSingle nos4 = new NewOrderSingle();
        MessageUtils.parse(nos4, incorrectFixMessage, dataDictionary, false);
        dataDictionary.validate(nos4);
        assertTrue(nos4.getHeader().isSetField(new quickfix.fix44.field.SenderSubID("trader")));
    }

    //
    // Group Validation Tests in RepeatingGroupTest
    //

    private static DataDictionary testDataDictionary;

    /**
     * Returns a singleton FIX 4.4 data dictionary. NOTE: the returned
     * dictionary must not be modified in any way (e.g. by calling any of its
     * setter methods). If it needs to be modified, it can be cloned by using
     * the {@link DataDictionary#DataDictionary(DataDictionary) DataDictionary
     * copy constructor}.
     *
     * @return a singleton FIX 4.4 data dictionary
     * @throws Exception if the data dictionary cannot be loaded
     */
    public static DataDictionary getDictionary() throws Exception {

        if (testDataDictionary == null) {
            testDataDictionary = getDictionary("/FIX44.xml");
        }
        return testDataDictionary;
    }

    /**
     * Loads and returns the named data dictionary.
     *
     * @param fileName the data dictionary file name (e.g. "FIX44.xml")
     * @return a new data dictionary instance
     * @throws Exception if the named data dictionary cannot be loaded
     */
    public static DataDictionary getDictionary(String fileName) throws Exception {

        return new DataDictionary(DataDictionaryTest.class.getResourceAsStream(fileName));
    }
}
