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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.quickfixj.CharsetSupport;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXEngine;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionary;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.SessionRejectReason;
import org.quickfixj.field.FieldException;
import org.quickfixj.field.GenericField;
import org.quickfixj.field.GenericGroupField;
import org.quickfixj.messages.bd.fix42.NewOrderSingle;
import org.quickfixj.messages.bd.fix42.NewOrderSingle.NoAllocs;
import org.quickfixj.messages.bd.fix42.field.AllocAccount;
import org.quickfixj.messages.bd.fix42.field.AllocShares;
import org.quickfixj.messages.bd.fix42.field.BeginString;
import org.quickfixj.messages.bd.fix42.field.ClOrdID;
import org.quickfixj.messages.bd.fix42.field.HandlInst;
import org.quickfixj.messages.bd.fix42.field.IOIid;
import org.quickfixj.messages.bd.fix42.field.OrdType;
import org.quickfixj.messages.bd.fix42.field.Side;
import org.quickfixj.messages.bd.fix42.field.Signature;
import org.quickfixj.messages.bd.fix42.field.SignatureLength;
import org.quickfixj.messages.bd.fix42.field.Symbol;
import org.quickfixj.messages.bd.fix42.field.TransactTime;
import org.quickfixj.messages.bd.fix44.AllocationInstruction;
import org.quickfixj.messages.bd.fix44.ExecutionReport;
import org.quickfixj.messages.bd.fix44.IndicationOfInterest;
import org.quickfixj.messages.bd.fix44.IndicationOfInterest.NoUnderlyings;
import org.quickfixj.messages.bd.fix44.Logon;
import org.quickfixj.messages.bd.fix44.NewOrderCross;
import org.quickfixj.messages.bd.fix44.NewOrderCross.NoSides;
import org.quickfixj.messages.bd.fix44.NewOrderList;
import org.quickfixj.messages.bd.fix44.NewOrderList.NoOrders;
import org.quickfixj.messages.bd.fix44.News;
import org.quickfixj.messages.bd.fix44.component.Instrument;
import org.quickfixj.messages.bd.fix44.component.Parties;
import org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDs;
import org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDsGroup;
import org.quickfixj.messages.bd.fix44.field.Account;
import org.quickfixj.messages.bd.fix44.field.AvgPx;
import org.quickfixj.messages.bd.fix44.field.BidType;
import org.quickfixj.messages.bd.fix44.field.CountryOfIssue;
import org.quickfixj.messages.bd.fix44.field.CrossID;
import org.quickfixj.messages.bd.fix44.field.CrossPrioritization;
import org.quickfixj.messages.bd.fix44.field.CrossType;
import org.quickfixj.messages.bd.fix44.field.CumQty;
import org.quickfixj.messages.bd.fix44.field.EncodedTextLen;
import org.quickfixj.messages.bd.fix44.field.ExecID;
import org.quickfixj.messages.bd.fix44.field.ExecType;
import org.quickfixj.messages.bd.fix44.field.Headline;
import org.quickfixj.messages.bd.fix44.field.HopCompID;
import org.quickfixj.messages.bd.fix44.field.LeavesQty;
import org.quickfixj.messages.bd.fix44.field.ListID;
import org.quickfixj.messages.bd.fix44.field.ListSeqNo;
import org.quickfixj.messages.bd.fix44.field.MsgSeqNum;
import org.quickfixj.messages.bd.fix44.field.MsgType;
import org.quickfixj.messages.bd.fix44.field.OrdStatus;
import org.quickfixj.messages.bd.fix44.field.OrderID;
import org.quickfixj.messages.bd.fix44.field.OrderQty;
import org.quickfixj.messages.bd.fix44.field.PartyID;
import org.quickfixj.messages.bd.fix44.field.PartyIDSource;
import org.quickfixj.messages.bd.fix44.field.PartyRole;
import org.quickfixj.messages.bd.fix44.field.Price;
import org.quickfixj.messages.bd.fix44.field.RawData;
import org.quickfixj.messages.bd.fix44.field.RawDataLength;
import org.quickfixj.messages.bd.fix44.field.SecurityID;
import org.quickfixj.messages.bd.fix44.field.SecurityIDSource;
import org.quickfixj.messages.bd.fix44.field.SecurityType;
import org.quickfixj.messages.bd.fix44.field.SenderCompID;
import org.quickfixj.messages.bd.fix44.field.TargetCompID;
import org.quickfixj.messages.bd.fix44.field.TotNoOrders;
import org.quickfixj.messages.bd.fix50.MarketDataSnapshotFullRefresh;
import org.quickfixj.messages.bd.fix50.component.MDFullGrp;
import org.quickfixj.messages.bd.fix50.field.MDEntryPx;
import org.quickfixj.messages.fixt11.field.EncodedText;

public class MessageTest {

	@Test
	public void testRepeatingField() throws Exception {

		org.quickfixj.messages.bd.fix40.NewOrderSingle m = (org.quickfixj.messages.bd.fix40.NewOrderSingle) MessageUtils
				.parse(FIXApplication.FIX40,
						"8=FIX.4.0\0019=100\00135=D\00134=2\00149=TW\00156=ISLD\00111=ID\00121=1\001"
								+ "40=1\00154=1\00140=2\00138=200\00155=INTC\00110=160\001");
		assertFalse("message should be invalid", m.hasValidStructure());
		FieldException exception = (FieldException) m.getParseException();
		assertEquals("wrong invalid tag", 40, exception.getField());
	}

	@Test
	public void testTrailerFieldOrdering() throws Exception {

		final NewOrderSingle order = createNewOrderSingle();

		order.getTrailer().setField(new Signature("FOO".getBytes()));
		order.getTrailer().setField(new SignatureLength(3));

		assertTrue(order.toString().contains("93=3\00189=FOO\001"));
	}

	private NewOrderSingle createNewOrderSingle() {

		NewOrderSingle orderSingle = new NewOrderSingle(
				new ClOrdID("CLIENT"),
				HandlInst.AUTOMATED_EXECUTION_ORDER_PUBLIC_BROKER_INTERVENTION_OK,
				new Symbol("ORCL"), Side.BUY, new TransactTime(new Date(0)),
				OrdType.LIMIT);

		// Added - TODO these should be acquired via a MessageBuilder
		orderSingle.getHeader().setField(
				org.quickfixj.messages.bd.fix42.field.MsgType.ORDER_SINGLE);
		orderSingle.getHeader().setField(
				new BeginString(FIXBeginString.FIX42.getValue()));

		return orderSingle;
	}

	@Test
	public void testHeaderCustomFieldOrdering() throws Exception {

		class MyMessage extends Message {

			/**
			 * The serialVersionUID property.
			 */
			private static final long serialVersionUID = 1L;

			final List<Integer> headerFieldOrder = Arrays.asList(
					FIXTag.BEGIN_STRING, FIXTag.BODY_LENGTH, FIXTag.MSG_TYPE,
					FIXTag.TARGET_SUB_ID, FIXTag.SENDING_TIME,
					FIXTag.MSG_SEQ_NUM, FIXTag.SENDER_COMP_ID,
					FIXTag.TARGET_COMP_ID);

			public MyMessage() {

				setHeader(new Header(headerFieldOrder));
			}
		}

		final MyMessage myMessage = new MyMessage();

		myMessage.getHeader().setField(new SenderCompID("foo"));
		myMessage.getHeader().setField(new MsgSeqNum(22));
		myMessage.getHeader().setField(
				new GenericField(FIXTag.SENDING_TIME, "20120922-11:00:00"));
		myMessage.getHeader().setField(new TargetCompID("bar"));

		assertTrue(myMessage.toString().contains(
				"52=20120922-11:00:00\00134=22\00149=foo\00156=bar"));
	}

	@Test
	public void testTrailerCustomFieldOrdering() throws Exception {

		class MyMessage extends Message {

			/**
			 * The serialVersionUID property.
			 */
			private static final long serialVersionUID = 1L;

			final List<Integer> trailerFieldOrder = Arrays
					.asList(FIXTag.SIGNATURE, FIXTag.SIGNATURE_LENGTH,
							FIXTag.CHECK_SUM);

			public MyMessage() {

				setTrailer(new Trailer(trailerFieldOrder));
			}
		}

		final MyMessage myMessage = new MyMessage();

		myMessage.getTrailer().setField(new Signature("FOO".getBytes()));
		myMessage.getTrailer().setField(new SignatureLength(3));
		assertTrue(myMessage.toString().contains("89=FOO\00193=3\001"));
	}

	@Test
	public void testHeaderGroupParsing() throws Exception {

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);

		Message message = (Message) MessageUtils
				.parse(FIXApplication.FIX44,
						new GenericMessageBuilderFactory(),
						provider,
						"8=FIX.4.4\0019=40\00135=A\001"
								+ "627=2\001628=FOO\001628=BAR\001"
								+ "98=0\001384=2\001372=D\001385=R\001372=8\001385=S\00110=230\001",
						true);

		// final org.quickfixj.messages.bd.fix44.MessageHeader.NoHopsGroup hops
		// = new org.quickfixj.messages.bd.fix44.MessageHeader.NoHopsGroup();
		// message.getHeader().getGroup(1, hops);
		// assertEquals("FOO", hops.getString(HopCompID.TAG));
		// message.getHeader().getGroup(2, hops);
		// assertEquals("BAR", hops.getString(HopCompID.TAG));
		assertEquals("FOO",
				((FIXGroupField<?>) message.getHeader().getField(627)).get(0)
						.getField(HopCompID.TAG).getValue());
		assertEquals("BAR",
				((FIXGroupField<?>) message.getHeader().getField(627)).get(1)
						.getField(HopCompID.TAG).getValue());
	}

	@Test
	public void testEmbeddedMessage() throws Exception {

		ExecutionReport report = new ExecutionReport(new OrderID("ORDER"),
				new ExecID("EXEC"), ExecType.FILL, OrdStatus.FILLED,
				org.quickfixj.messages.bd.fix44.field.Side.BUY, new LeavesQty(
						100), new CumQty(100), new AvgPx(50));

		report.getHeader().setField(
				new org.quickfixj.messages.bd.fix44.field.BeginString(
						FIXBeginString.FIX44.getValue()));
		report.getHeader().setField(
				org.quickfixj.messages.bd.fix44.field.MsgType.EXECUTION_REPORT);

		final NewOrderSingle order = createNewOrderSingle();
		String text = order.toString();
		report.setEncodedTextLen(new EncodedTextLen(text.length()));
		report.setEncodedText(new org.quickfixj.messages.bd.fix44.field.EncodedText(
				text.getBytes()));

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);

		System.out
				.println("MessageTest.testEmbeddedMessage() " + text.length());
		System.out.println("MessageTest.testEmbeddedMessage() "
				+ text.getBytes().length);
		System.out.println("MessageTest.testEmbeddedMessage() "
				+ report.toString());

		FIXMessage msg = MessageUtils.parse(FIXApplication.FIX44,
				new GenericMessageBuilderFactory(), provider,
				report.toString(), true);
		assertEquals(
				"embedded order",
				text,
				msg.getFieldValue(org.quickfixj.messages.bd.fix44.field.EncodedText.TAG));
	}

	@Test
	public void testMessageWithEncodedField() throws Exception {

		String text = "\u6D4B\u9A8C\001\u6570\u636E"; // Chinese with SOH in the
														// middle

		doTestMessageWithEncodedField("UTF-8", text);
		doTestMessageWithEncodedField("EUC-JP", text);
		doTestMessageWithEncodedField("ISO-2022-JP", text);
		doTestMessageWithEncodedField("Shift_JIS", text);
		doTestMessageWithEncodedField("GBK", text);
		// doTestMessageWithEncodedField("UTF-16", text); // double-byte charset
		// not supported yet
	}

	private void doTestMessageWithEncodedField(String charset, String text)
			throws Exception {

		FIXEngine engine = DefaultEngine.getDefaultEngine();

		CharsetSupport.setCharset(charset);
		try {
			NewOrderSingle order = createNewOrderSingle();

			Charset cset = Charset.forName(charset);

			int byteLength = MessageUtils.length(cset, text);

			System.out.println("MessageTest.doTestMessageWithEncodedField() "
					+ byteLength);

			org.quickfixj.messages.bd.fix42.field.EncodedText encodedText = new org.quickfixj.messages.bd.fix42.field.EncodedText(
					text.getBytes());

			order.setEncodedText(encodedText);
			order.setEncodedTextLen(new org.quickfixj.messages.bd.fix42.field.EncodedTextLen(
					byteLength));

			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIX42,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);

			FIXMessage msg = MessageUtils.parse(FIXApplication.FIX42,
					new GenericMessageBuilderFactory(), provider,
					order.toString(), true);

			assertEquals(charset + " encoded field", text,
					msg.getFieldValue(EncodedText.TAG));

		} finally {
			CharsetSupport.setCharset(CharsetSupport.getDefaultCharset());
		}
	}

	@Test
	public void testParsing() throws Exception {

		FIXEngine engine;
		FIXMessageDictionaryFactory dictionaryFactory;
		FIXMessageBuilderFactory builderFactory;

		engine = DefaultEngine.getDefaultEngine();
		dictionaryFactory = engine.getMessageDictionaryFactory(
				FIXBeginString.FIX42, "org.quickfixj.messages.bd");
		builderFactory = engine.getMessageBuilderFactory(FIXBeginString.FIX42,
				"org.quickfixj.messages.bd");

		assertNotNull(dictionaryFactory);
		assertNotNull(builderFactory);

		// checksum is not verified in these tests
		FIXMessage message = MessageUtils
				.parse(FIXApplication.FIX42,
						builderFactory,
						dictionaryFactory,
						"8=FIX.4.2\0019=40\00135=A\001"
								+ "98=0\001384=2\001372=D\001385=R\001372=8\001385=S\00110=96\001",
						true);

		assertHeaderField(message, "FIX.4.2", FIXTag.BEGIN_STRING);
		assertHeaderField(message, "40", FIXTag.BODY_LENGTH);
		assertEquals("wrong field value", 40, MessageUtils.coerceToInt(
				message.getHeader(), FIXTag.BODY_LENGTH));
		assertHeaderField(message, "A", FIXTag.MSG_TYPE);
		assertBodyField(message, "0", FIXTag.ENCRYPT_METHOD);
		assertTrailerField(message, "96", FIXTag.CHECK_SUM);

		org.quickfixj.messages.bd.fix42.Logon.NoMsgTypes noMsgTypes;
		org.quickfixj.messages.bd.fix42.Logon.NoMsgTypesGroup valueMessageType;

		noMsgTypes = (org.quickfixj.messages.bd.fix42.Logon.NoMsgTypes) message
				.getField(384);

		valueMessageType = noMsgTypes.get(0);
		assertEquals("wrong value", "D",
				valueMessageType.getField(FIXTag.REF_MSG_TYPE).getValue());
		assertEquals("wrong value", "R", valueMessageType.getFieldValue(385));
		assertEquals("wrong value", 'R', valueMessageType.getField(385)
				.getValue());

		valueMessageType = noMsgTypes.get(1);
		assertEquals("wrong value", "8",
				valueMessageType.getField(FIXTag.REF_MSG_TYPE).getValue());
		assertEquals("wrong value", "S", valueMessageType.getFieldValue(385));
		assertEquals("wrong value", 'S', valueMessageType.getField(385)
				.getValue());
	}

	@Test
	public void testParsing2() throws Exception {

		// checksum is not verified in these tests
		String data = "8=FIX.4.4\0019=76\001";
		data += "35=6\001";
		data += "23=IDENTIFIER\001";
		data += "28=N\001";
		data += "55=MSFT\001";
		data += "54=1\001";
		data += "711=2\001";
		data += "311=DELL\001";
		data += "318=USD\001";
		data += "311=IBM\001";
		data += "318=CAD\001";
		data += "10=039\001";

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		FIXMessageBuilderFactory factory = engine.getMessageBuilderFactory(
				FIXBeginString.FIX44, "org.quickfixj.messages.bd");
		assertNotNull(provider);

		IndicationOfInterest message = (IndicationOfInterest) MessageUtils
				.parse(FIXApplication.FIX44, factory, provider, data, true);

		assertHeaderField(message, "FIX.4.4", FIXTag.BEGIN_STRING);
		assertHeaderField(message, "76", FIXTag.BODY_LENGTH);
		assertHeaderField(message, MsgType.INDICATION_OF_INTEREST.getValue(),
				FIXTag.MSG_TYPE);
		assertBodyField(message, "IDENTIFIER", IOIid.TAG);
		assertTrailerField(message, "039", FIXTag.CHECK_SUM);
		NoUnderlyings noUnderlyings = message.getNoUnderlyings();

		assertEquals("wrong value", "DELL", noUnderlyings.get(0)
				.getUnderlyingSymbol().getValue());
		assertEquals("wrong value", "USD", noUnderlyings.get(0)
				.getUnderlyingCurrency().getValue());
		assertEquals("wrong value", "IBM", noUnderlyings.get(1)
				.getUnderlyingSymbol().getValue());
		assertEquals("wrong value", "CAD", noUnderlyings.get(1)
				.getUnderlyingCurrency().getValue());
	}

	@SuppressWarnings("unused")
	@Test(expected = InvalidMessage.class)
	public void testParseEmptyStringWithValidation() throws Exception {
		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		MessageUtils.parse(FIXApplication.FIX44,
				new GenericMessageBuilderFactory(), provider, "", true);
	}

	@SuppressWarnings("unused")
	@Test(expected = InvalidMessage.class)
	public void testParseEmptyStringWithoutValidation() throws Exception {
		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		MessageUtils.parse(FIXApplication.FIX44,
				new GenericMessageBuilderFactory(), provider, "", false);
	}

	@Test
	public void testValidation() throws Exception {

		String data = "8=FIX.4.4\0019=309\00135=8\00149=ASX\00156=CL1_FIX44\00134=4\001"
				+ "52=20060324-01:05:58\00117=X-B-WOW-1494E9A0:58BD3F9D-1109\001150=D\00139=0\001"
				+ "11=184271\00138=200\001198=1494E9A0:58BD3F9D\001526=4324\00137=B-WOW-1494E9A0:58BD3F9D\001"
				+ "55=WOW\00154=1\001151=200\00114=0\00140=2\00144=15\00159=1\0016=0\001453=3\001448=AAA35791\001"
				+ "447=D\001452=3\001448=8\001447=D\001452=4\001448=FIX11\001"
				+ "447=D\001452=36\00160=20060320-03:34:29\00110=169\001";

		// ExecutionReport executionReport = new ExecutionReport();
		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		DefaultValidator validator = new DefaultValidator(FIXBeginString.FIX44);

		ExecutionReport executionReport = MessageUtils.parse(
				FIXApplication.FIX44, new ExecutionReport(), provider,
				validator, data, true);

		FIXMessageDictionary dictionary = provider.getMessageDictionary(
				FIXApplication.FIX44, "8");
		assertNotNull(dictionary);
		validator.validate(dictionary, executionReport);
	}

	@Test
	// QFJ-675: Message.clear() should reset position field to zero to enable
	// Message to be reused
	public void testParseTwice() throws Exception {

		final String data1 = "8=FIX.4.4\0019=309\00135=8\00149=ASX\00156=CL1_FIX44\00134=4\001"
				+ "52=20060324-01:05:58\00117=X-B-WOW-1494E9A0:58BD3F9D-1109\001150=D\00139=0\001"
				+ "11=184271\00138=200\001198=1494E9A0:58BD3F9D\001526=4324\00137=B-WOW-1494E9A0:58BD3F9D\001"
				+ "55=WOW\00154=1\001151=200\00114=0\00140=2\00144=15\00159=1\0016=0\001453=3\001448=AAA35791\001"
				+ "447=D\001452=3\001448=8\001447=D\001452=4\001448=FIX11\001"
				+ "447=D\001452=36\00160=20060320-03:34:29\00110=169\001";

		final String data2 = "8=FIX.4.4\0019=309\00135=8\00149=ASX\00156=CL1_FIX44\00134=4\001"
				+ "52=20060324-01:05:58\00117=X-B-WOW-1494E9A0:58BD3F9D-1109\001150=D\00139=0\00111=123456\001"
				+ "38=200\001198=1494E9A0:58BD3F9D\001526=4324\00137=B-WOW-1494E9A0:58BD3F9D\00155=WOW\00154=1\001"
				+ "151=200\00114=0\00140=2\00144=15\00159=1\0016=0\001453=3\001448=AAA35791\001447=D\001452=3\001"
				+ "448=8\001447=D\001452=4\001448=FIX11\001447=D\001452=36\00160=20060320-03:34:29\00110=167\001";

		ExecutionReport executionReport = new ExecutionReport();
		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		FIXMessageDictionary dictionary = provider.getMessageDictionary(
				FIXApplication.FIX44, "8");
		assertNotNull(dictionary);
		DefaultValidator validator = new DefaultValidator(FIXBeginString.FIX44);

		MessageUtils.parse(FIXApplication.FIX44, executionReport, provider,
				validator, data1, true);
		validator.validate(dictionary, executionReport);

		executionReport.clear();
		MessageUtils.parse(FIXApplication.FIX44, executionReport, provider,
				validator, data2, true);
		validator.validate(dictionary, executionReport);
	}

	@Test
	// QFJ-426 Message header will not validate when containing 'Hop' group
	public void testValidationWithHops() throws Exception {

		final String data = "8=FIX.4.4\0019=309\00135=8\00149=ASX\00156=CL1_FIX44\00134=4\001"
				+ "52=20060324-01:05:58\00117=X-B-WOW-1494E9A0:58BD3F9D-1109\001150=D\00139=0\00111=184271\001"
				+ "38=200\001198=1494E9A0:58BD3F9D\001526=4324\00137=B-WOW-1494E9A0:58BD3F9D\00155=WOW\00154=1\001"
				+ "151=200\00114=0\00140=2\00144=15\00159=1\0016=0\001453=3\001448=AAA35791\001447=D\001452=3\001"
				+ "448=8\001447=D\001452=4\001448=FIX11\001447=D\001452=36\00160=20060320-03:34:29\00110=169\001";

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		FIXMessageDictionary dictionary = provider.getMessageDictionary(
				FIXApplication.FIX44, "8");
		assertNotNull(dictionary);
		DefaultValidator validator = new DefaultValidator(FIXBeginString.FIX44);

		ExecutionReport executionReport = MessageUtils.parse(
				FIXApplication.FIX44, new ExecutionReport(), provider,
				validator, data, true);
		final org.quickfixj.messages.bd.fix44.MessageHeader.NoHops noHops;
		final org.quickfixj.messages.bd.fix44.MessageHeader.NoHopsGroup noHopsGroup;

		noHops = new org.quickfixj.messages.bd.fix44.MessageHeader.NoHops();
		noHopsGroup = noHops
				.add(new org.quickfixj.messages.bd.fix44.MessageHeader.NoHopsGroup());
		noHopsGroup.setHopCompID(new HopCompID("FOO"));

		executionReport.getHeader().setField(noHops);

		validator.validate(dictionary, executionReport);
	}

	@Test
	public void testAppMessageValidation() throws Exception {

		String data = "8=FIXT.1.1\0019=234\00135=W\00134=2\00149=ABFX\00152=20080722-16:37:11.234\001"
				+ "56=X2RV1\00155=EUR/USD\001262=CAP0000011\001268=2\001269=0\001270=1.57844\00115=EUR\001"
				+ "271=500000\001272=20080724\001269=1\001270=1.57869\00115=EUR\001271=500000\001272=20080724\00110=097\001";
		// final MarketDataSnapshotFullRefresh mdsfr = new
		// MarketDataSnapshotFullRefresh();
		// final DataDictionary sessDictionary =
		// DataDictionaryTest.getDictionary("/FIXT11.xml");
		// final DataDictionary appDictionary =
		// DataDictionaryTest.getDictionary("/FIX50.xml");
		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIXT11,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		DefaultValidator validator = new DefaultValidator(FIXBeginString.FIXT11);

		MarketDataSnapshotFullRefresh mdsfr = MessageUtils.parse(
				FIXApplication.FIX50, new MarketDataSnapshotFullRefresh(),
				provider, validator, data, true);
		// DataDictionary.validate(mdsfr, sessDictionary, appDictionary);

		FIXMessageDictionary dictionary = provider.getMessageDictionary(
				FIXApplication.FIX50, "W");
		assertNotNull(dictionary);

		validator.validate(dictionary, mdsfr);
	}

	@Test
	public void testAdminMessageValidation() throws Exception {

		String data = "8=FIXT.1.1\0019=84\00135=A\00149=EXEC\00156=BANZAI\00134=1\001"
				+ "52=20080811-13:26:12.409\001108=1\001141=Y\00198=0\0011137=7\00110=102\001";

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIXT11,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		DefaultValidator validator = new DefaultValidator(FIXBeginString.FIXT11);

		Logon logon = MessageUtils.parse(FIXApplication.FIX50, new Logon(),
				provider, validator, data, true);

		FIXMessageDictionary dictionary = provider.getMessageDictionary(
				FIXApplication.FIX50, "A");
		assertNotNull(dictionary);

		validator.validate(dictionary, logon);
	}

	@Test
	public void testGroupDelimOrdering() throws Exception {

		// Test the generic group constructor (QFJ-95)
		final org.quickfixj.messages.bd.fix44.NewOrderSingle order = new org.quickfixj.messages.bd.fix44.NewOrderSingle();

		FIXGroupField<FIXGroup> field = new GenericGroupField<FIXGroup>(453,
				PartyID.TAG);
		FIXGroup partyGroup = new Group(453, PartyID.TAG);
		partyGroup.setField(new PartyID("TraderName"));
		partyGroup
				.setField(PartyIDSource.GENERALLY_ACCEPTED_MARKET_PARTICIPANT_IDENTIFIER);
		partyGroup.setField(new PartyRole(11));
		field.add(partyGroup);
		order.setField(field);
		final String data = order.toString();

		assertTrue("wrong field order",
				data.contains("453=1\001448=TraderName"));
	}

	@Test
	// SBF : no longer meaningful
	public void testComponentGroupExtraction() throws Exception {

		org.quickfixj.messages.bd.fix44.NewOrderSingle order = new org.quickfixj.messages.bd.fix44.NewOrderSingle();
		org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDs noPartyIDs = new NoPartyIDs();
		org.quickfixj.messages.bd.fix44.component.Parties.NoPartyIDsGroup noPartyIDsGroup;

		noPartyIDsGroup = new NoPartyIDsGroup();
		noPartyIDsGroup.setPartyID(new PartyID("PARTY_ID_1"));
		noPartyIDs.add(noPartyIDsGroup);

		noPartyIDsGroup = new NoPartyIDsGroup();
		noPartyIDsGroup.setPartyID(new PartyID("PARTY_ID_2"));
		noPartyIDs.add(noPartyIDsGroup);

		order.setField(noPartyIDs);

		// final Parties parties = order.getParties();
		final Parties parties = order;
		assertEquals("wrong # of party IDs", (Integer) 2, parties
				.getNoPartyIDs().getValue());
	}

	@Test
	public void testComponentGroupInsertion() throws Exception {

		Parties parties = new AllocationInstruction();
		NoPartyIDs partyIds;
		NoPartyIDsGroup partyIdsGroup;

		partyIds = new NoPartyIDs();

		partyIdsGroup = partyIds.add(new NoPartyIDsGroup());
		partyIdsGroup.setPartyID(new PartyID("PARTY_ID_1"));

		partyIdsGroup = partyIds.add(new NoPartyIDsGroup());
		partyIdsGroup.setPartyID(new PartyID("PARTY_ID_2"));

		parties.setNoPartyIDs(partyIds);

		final org.quickfixj.messages.bd.fix44.NewOrderSingle order = new org.quickfixj.messages.bd.fix44.NewOrderSingle();
		order.copyValues(parties);

		assertEquals("wrong # of party IDs", (Integer) 2, order.getNoPartyIDs()
				.getValue());
	}

	// QFJ-66 Should not throw exception when parsing data field in header
	@Test
	public void testHeaderDataField() throws Exception {

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX42,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);

		FIXMessage m = MessageUtils
				.parse(FIXApplication.FIX42,
						new GenericMessageBuilderFactory(),
						provider,
						"8=FIX.4.2\0019=53\00135=A\00190=4\00191=ABCD\001"
								+ "98=0\001384=2\001372=D\001385=R\001372=8\001385=S\00110=241\001",
						true);

		assertEquals("ABCD", m.getHeader().getField(FIXTag.SECURE_DATA)
				.getCharacters().toString());
	}

	// QFJ-52
	@Test
	public void testInvalidFirstFieldInGroup() throws Exception {

		final News news = new News();
		news.setHeadline(new Headline("Test"));

		News.NoRelatedSym noRelatedSym;
		News.NoRelatedSymGroup noRelatedSymGroup;

		noRelatedSym = new News.NoRelatedSym();
		noRelatedSymGroup = new News.NoRelatedSymGroup();

		noRelatedSymGroup.setSecurityID(new SecurityID("SECID"));
		noRelatedSymGroup.setSecurityIDSource(new SecurityIDSource(
				"SECID_SOURCE"));
		noRelatedSym.add(noRelatedSymGroup);

		news.setField(noRelatedSym);

		try {
			FIXEngine engine = DefaultEngine.getDefaultEngine();
			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIX44,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);

			MessageUtils.parse(FIXApplication.FIX44,
					new GenericMessageBuilderFactory(), provider,
					news.toString(), true);
		} catch (final InvalidMessage e) {
			// expected
		} catch (final NullPointerException e) {
			fail("Should not throw NPE when first field is missing.");
		}
	}

	@Test
	public void testRequiredGroupValidation() throws Exception {

		final News news = new News();
		news.setHeadline(new Headline("Test"));

		// Added - TODO these should be acquired via a MessageBuilder
		news.getHeader().setField(MsgType.NEWS);
		news.getHeader().setField(
				new org.quickfixj.messages.bd.fix44.field.BeginString(
						FIXBeginString.FIX44.getValue()));

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		FIXMessageDictionary dictionary = provider.getMessageDictionary(
				FIXApplication.FIX44, "B");

		// final DataDictionary dictionary = DataDictionaryTest.getDictionary();
		try {
			new DefaultValidator(FIXBeginString.FIX44).validate(dictionary,
					news);
			fail("no field exception for missing lines group");
		} catch (final FieldException e) {
			// expected
		}
	}

	/**
	 * Test for data fields with SOH. This test is based on report from a user
	 * on the QuickFIX mailing list. The problem was the user's configuration
	 * but this seems like a good unit test to keep in the suite.
	 */
	@Test
	public void testDataFieldParsing() throws Exception {

		final String data = "10001=Canonical.1.00\00110002=001058\00125001=01\00110003=SAPI_ADMRESP\00110004=SUBSCRIBE_RESP\001"
				+ "10009=705\00110012=01\00110005=SPGW\00110006=SAPI\00110007=0\00110010=16:25:11.537\001"
				+ "10045=SDQADL:01:/SDB/ENT/@/@/STKSDLL:7\00110955=Y\00110963=043\00110961=03\00111285=N\001"
				+ "11339=823,980\00110919=N\00111111=86795696\00110898=043\00110920=~\00110938=N\00111340=5-  9.99\001"
				+ "11343=0.20\00111344=~\00111341=~\00111342=0.15\00111345=10- 14.99\00111348=0.25\00111349=~\00111346=~\001"
				+ "11347=0.15\00111350=15- 19.99\00111353=0.30\00111354=~\00111351=~\00111352=0.20\00111338=23SEP05\001"
				+ "10981=0\00110485=N\00110761=0\00111220=~\00111224=N\00110808=N\00110921=~\00110960=N\00110957=N\00111329=N\001"
				+ "11286=0\00111214=USA\00110917=Y\00111288=0\00110906=N\00110737=0.01\00110956=~\00110967=~\00110965=~\00110809=0\001"
				+ "10762=N\00110763=N\00110712=1\00110905=09:30:00\00110918=YA0101\00110951=Y\00110469=1\00110949=1\00110487=Q\00110950=Y\001"
				+ "10899=N\00110380=N\00110696=03\00111082=18.41\00110217=12\00110954=N\00110708=E\00110958=N\00111213=US \00111334=N\001"
				+ "11332=N\00111331=N\00111330=N\00111335=N\00111333=N\00110767=3\00110974=~\00110980=AIRTRAN HOLDINGS                \00111289=N\001"
				+ "10912=4\00110915=0501\00110914=0501\00110975=N\00110913=SLK\00110698=055\00110666=AAI\00110903=S\00111328=N\001"
				+ "10624=L\00111287=0\00110699=0\00110962=L\00111227=SUB1\00111229=5\00111228=1\00111236=16:24:41.521\00111277=16:25:11.630\001";

		try {
			FIXEngine engine = DefaultEngine.getDefaultEngine();
			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIX44,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);
			Message m = (Message) MessageUtils
					.parse(FIXApplication.FIX44,
							new GenericMessageBuilderFactory(),
							provider,
							("8=FIX.4.4\0019=1144\00135=A\001"
									+ "98=0\001384=2\001372=D\001385=R\001372=8\001385=S\00195=1092\001"
									+ "96=" + data + "\00110=5\001"), true);
			assertEquals(1144, m.bodyLength());
			Message m2 = (Message) MessageUtils.parse(FIXApplication.FIX44,
					new GenericMessageBuilderFactory(), provider, m.toString(),
					true);
			assertEquals(1144, m2.bodyLength());
		} catch (final InvalidMessage e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test for data fields with SOH. This test is based on report from a user
	 * on the QuickFIX mailing list. The problem was the user's configuration
	 * but this seems like a good unit test to keep in the suite.
	 */
	@Test
	public void testDataFieldWithManualFieldInsertion() throws Exception {

		final String data = "10001=Canonical.1.00\00110002=001058\00125001=01\00110003=SAPI_ADMRESP\00110004=SUBSCRIBE_RESP\001"
				+ "10009=705\00110012=01\00110005=SPGW\00110006=SAPI\00110007=0\00110010=16:25:11.537\001"
				+ "10045=SDQADL:01:/SDB/ENT/@/@/STKSDLL:7\00110955=Y\00110963=043\00110961=03\00111285=N\001"
				+ "11339=823,980\00110919=N\00111111=86795696\00110898=043\00110920=~\00110938=N\00111340=5-  9.99\001"
				+ "11343=0.20\00111344=~\00111341=~\00111342=0.15\00111345=10- 14.99\00111348=0.25\00111349=~\00111346=~\001"
				+ "11347=0.15\00111350=15- 19.99\00111353=0.30\00111354=~\00111351=~\00111352=0.20\00111338=23SEP05\001"
				+ "10981=0\00110485=N\00110761=0\00111220=~\00111224=N\00110808=N\00110921=~\00110960=N\00110957=N\00111329=N\001"
				+ "11286=0\00111214=USA\00110917=Y\00111288=0\00110906=N\00110737=0.01\00110956=~\00110967=~\00110965=~\00110809=0\001"
				+ "10762=N\00110763=N\00110712=1\00110905=09:30:00\00110918=YA0101\00110951=Y\00110469=1\00110949=1\00110487=Q\00110950=Y\001"
				+ "10899=N\00110380=N\00110696=03\00111082=18.41\00110217=12\00110954=N\00110708=E\00110958=N\00111213=US \00111334=N\001"
				+ "11332=N\00111331=N\00111330=N\00111335=N\00111333=N\00110767=3\00110974=~\00110980=AIRTRAN HOLDINGS                \00111289=N\001"
				+ "10912=4\00110915=0501\00110914=0501\00110975=N\00110913=SLK\00110698=055\00110666=AAI\00110903=S\00111328=N\001"
				+ "10624=L\00111287=0\00110699=0\00110962=L\00111227=SUB1\00111229=5\00111228=1\00111236=16:24:41.521\00111277=16:25:11.630\001";

		try {
			FIXEngine engine = DefaultEngine.getDefaultEngine();
			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIX44,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);
			final Message m = new Message();
			m.getHeader().setField(
					new GenericField(FIXTag.BEGIN_STRING, FIXBeginString.FIX44
							.getValue()));
			final MsgType msgType = new MsgType("B");
			m.getHeader().setField(msgType);
			m.setField(new GenericField(RawDataLength.TAG, data.length()));
			m.setField(new GenericField(RawData.TAG, data));
			assertEquals(1108 + msgType.getValue().length(), m.bodyLength());
			Message m2 = (Message) MessageUtils.parse(FIXApplication.FIX44,
					new GenericMessageBuilderFactory(), provider, m.toString(),
					true);
			assertEquals(m.bodyLength(), m2.bodyLength());
		} catch (final InvalidMessage e) {
			fail(e.getMessage());
		}
	}

	@Test
	@Ignore
	// Method under test no longer exists
	public void testFix5HeaderFields() {

		// assertTrue(Message.isHeaderField(FIXTag.APPL_VER_ID));
		// assertTrue(Message.isHeaderField(FIXTag.CSTM_APPL_VER_ID));
	}

	@Test
	public void testCalculateStringWithNestedGroups() throws Exception {

		NewOrderCross noc = new NewOrderCross();

		noc.getHeader().setField(
				new org.quickfixj.messages.bd.fix44.field.BeginString(
						FIXBeginString.FIX44.getValue()));
		noc.getHeader()
				.setField(
						new org.quickfixj.messages.bd.fix44.field.SenderCompID(
								"sender"));
		noc.getHeader()
				.setField(
						new org.quickfixj.messages.bd.fix44.field.TargetCompID(
								"target"));
		noc.getHeader().setField(
				new org.quickfixj.messages.bd.fix44.field.MsgSeqNum(5));
		noc.getHeader().setField(
				new GenericField(FIXTag.SENDING_TIME, "20060319-09:08:20.881"));

		noc.setString(SecurityIDSource.TAG,
				SecurityIDSource.EXCHANGE_SYMBOL.getValue());
		noc.setOrdType(org.quickfixj.messages.bd.fix44.field.OrdType.LIMIT);
		noc.setPrice(new Price(9.00));
		noc.setString(SecurityID.TAG, "ABC");
		noc.setString(Symbol.TAG, "ABC");
		noc.setString(TransactTime.TAG, "20060319-09:08:19");
		noc.setString(CrossID.TAG, "184214");
		noc.setCrossType(CrossType.CROSS_TRADE_WHICH_IS_EXECUTED_PARTIALLY_AND_THE_REST_IS_CANCELLED);
		noc.setCrossPrioritization(CrossPrioritization.NONE);

		NewOrderCross.NoSides noSides;
		NewOrderCross.NoSidesGroup noSidesGroup;

		Parties.NoPartyIDs noPartyIDs;
		Parties.NoPartyIDsGroup noPartyIDsGroup;

		noSides = new NoSides();
		noc.setNoSides(noSides);

		// parent group #1
		noSidesGroup = new NewOrderCross.NoSidesGroup();
		noSidesGroup.setSide(org.quickfixj.messages.bd.fix44.field.Side.BUY);
		noSidesGroup.setOrderQty(new OrderQty(9));
		noSides.add(noSidesGroup);

		noPartyIDs = new NoPartyIDs();
		noSidesGroup.setNoPartyIDs(noPartyIDs);

		// nested group #1
		noPartyIDsGroup = new Parties.NoPartyIDsGroup();
		noPartyIDsGroup.setString(PartyID.TAG, "8");
		noPartyIDsGroup.setPartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE);
		noPartyIDsGroup.setPartyRole(PartyRole.CLEARING_FIRM);
		noPartyIDs.add(noPartyIDsGroup);

		// nested group #2
		noPartyIDsGroup = new Parties.NoPartyIDsGroup();
		noPartyIDsGroup.setString(PartyID.TAG, "AAA35777");
		noPartyIDsGroup.setPartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE);
		noPartyIDsGroup.setPartyRole(PartyRole.CLIENT_ID);
		noPartyIDs.add(noPartyIDsGroup);

		// parent group #2
		noSidesGroup = new NewOrderCross.NoSidesGroup();
		noSidesGroup.setSide(org.quickfixj.messages.bd.fix44.field.Side.SELL);
		noSidesGroup.setOrderQty(new OrderQty(9));
		noSides.add(noSidesGroup);

		noPartyIDs = new NoPartyIDs();
		noSidesGroup.setNoPartyIDs(noPartyIDs);

		// nested group #1
		noPartyIDsGroup = new Parties.NoPartyIDsGroup();
		noPartyIDsGroup.setString(PartyID.TAG, "8");
		noPartyIDsGroup.setPartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE);
		noPartyIDsGroup.setPartyRole(PartyRole.CLEARING_FIRM);
		noPartyIDs.add(noPartyIDsGroup);

		// nested group #2
		noPartyIDsGroup = new Parties.NoPartyIDsGroup();
		noPartyIDsGroup.setString(PartyID.TAG, "aaa");
		noPartyIDsGroup.setPartyIDSource(PartyIDSource.PROPRIETARY_CUSTOM_CODE);
		noPartyIDsGroup.setPartyRole(PartyRole.CLIENT_ID);
		noPartyIDs.add(noPartyIDsGroup);

		// Added - TODO these should be acquired via a MessageBuilder
		noc.getHeader().setField(
				org.quickfixj.messages.bd.fix44.field.MsgType.NEW_ORDER_CROSS);
		noc.getHeader().setField(
				new org.quickfixj.messages.bd.fix44.field.BeginString(
						FIXBeginString.FIX44.getValue()));

		final String expectedMessage = "8=FIX.4.4\0019=247\00135=s\00134=5\00149=sender\00152=20060319-09:08:20.881\001"
				+ "56=target\00122=8\00140=2\00144=9\00148=ABC\00155=ABC\00160=20060319-09:08:19\001548=184214\001549=2\001"
				+ "550=0\001552=2\00154=1\001453=2\001448=8\001447=D\001452=4\001448=AAA35777\001447=D\001452=3\00138=9\00154=2\001"
				+ "453=2\001448=8\001447=D\001452=4\001448=aaa\001447=D\001452=3\00138=9\00110=056\001";
		assertEquals("wrong message", expectedMessage, noc.toString());
	}

	@Test
	public void testFieldOrdering() throws Exception {

		String expectedMessageString = "8=FIX.4.4\0019=171\00135=D\00149=SenderCompId\00156=TargetCompId\001"
				+ "11=183339\00122=8\00138=1\00140=2\00144=12\00148=BHP\00154=2\00155=BHP\00159=1\00160=20060223-22:38:33\001"
				+ "526=3620\001453=2\001448=8\001447=D\001452=4\001448=AAA35354\001447=D\001452=3\00110=168\001";

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		FIXMessage message = MessageUtils.parse(FIXApplication.FIX44,
				new GenericMessageBuilderFactory(), provider,
				expectedMessageString, false);
		String actualMessageString = message.toString();
		assertTrue(
				"wrong field ordering",
				actualMessageString
						.contains("453=2\001448=8\001447=D\001452=4\001448=AAA35354\001447=D\001452=3"));
	}

	@Test
	public void testHeaderFieldsMissing() throws Exception {

		try {
			MessageUtils.parse(FIXApplication.FIX42, "1=FIX.4.2");
		} catch (final InvalidMessage e) {
			// expected
		}
	}

	@Test
	public void testHeaderFieldInBody() throws Exception {

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX42,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);

		Message message = (Message) MessageUtils
				.parse(FIXApplication.FIX42,
						new GenericMessageBuilderFactory(),
						provider,
						"8=FIX.4.2\0019=40\00135=A\001"
								+ "98=0\001212=4\001384=2\001372=D\001385=R\001372=8\001385=S\00110=103\001",
						true);

		assertFalse(message.hasValidStructure());

		assertTrue(message.getHeader().isFieldSet(212));

		FieldException exception = (FieldException) message.getParseException();
		assertEquals(SessionRejectReason.TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER,
				exception.getSessionRejectReason());
		assertEquals(212, exception.getField());
	}

	@Test
	public void testTrailerFieldInBody() throws Exception {

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX42,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);

		Message message = (Message) MessageUtils
				.parse(FIXApplication.FIX42,
						new GenericMessageBuilderFactory(),
						provider,
						"8=FIX.4.2\0019=40\00135=A\001"
								+ "98=0\00193=5\001384=2\001372=D\001385=R\001372=8\001385=S\00110=63\001",
						true);

		assertFalse(message.hasValidStructure());

		FIXField<?> signatureLength = message.getTrailer().getField(
				SignatureLength.TAG);
		assertEquals("5", signatureLength.getCharacters());
	}

	@Test
	public void testMessageFromString() {

		FIXMessage message = MessageUtils.parse(FIXApplication.FIX42,
				"8=FIX.4.2\0019=12\00135=A\001108=30\00110=026\001");
		assertEquals("8=FIX.4.2\0019=12\00135=A\001108=30\00110=026\001",
				message.toString());
	}

	@SuppressWarnings("unused")
	@Test(expected = InvalidMessage.class)
	public void testMessageFromStringInvalid() {

		MessageUtils.parse(FIXApplication.FIX42,
				"8=FIX.4.2\0019=12\00135=A\001108=30\00110=036\001");
	}

	@Test
	@Ignore
	@Deprecated
	// Method no longer exists
	public void testMessageGroups() {

		final Message message = new Message();
		final NewOrderSingle.NoAllocs nAllocs = setUpGroups(message);

		assertGroupContent(message, nAllocs);
	}

	// Includes test for QFJ-413. Repeating group check for size = 0
	@Test
	public void testMessageGroupCountValidation() throws Exception {

		final String data = "8=FIX.4.4\0019=222\00135=D\00149=SenderCompId\00156=TargetCompId\00134=37\001"
				+ "52=20070223-22:28:33\00111=183339\00122=8\00138=1\00140=2\00144=12\00148=BHP\00154=2\001"
				+ "55=BHP\00159=1\00160=20060223-22:38:33\001526=3620\00178=0\00179=AllocACC1\00180=1010.1\001"
				+ "79=AllocACC2\00180=2020.2\001453=2\001448=8\001447=D\001452=4\001448=AAA35354\001447=D\001452=3\00110=079\001";

		FIXEngine engine = DefaultEngine.getDefaultEngine();
		FIXMessageDictionaryFactory provider = engine
				.getMessageDictionaryFactory(FIXBeginString.FIX44,
						"org.quickfixj.messages.bd");
		assertNotNull(provider);
		FIXMessageDictionary dictionary = provider.getMessageDictionary(
				FIXApplication.FIX44, "D");

		// final DataDictionary dd = DataDictionaryTest.getDictionary();
		FIXMessage message = MessageUtils.parse(FIXApplication.FIX44,
				new GenericMessageBuilderFactory(), provider, data, true);
		try {
			new DefaultValidator(FIXBeginString.FIX44).validate(dictionary,
					message);
			fail("No exception thrown");
		} catch (final FieldException e) {
			final String emsg = e.getMessage();
			assertNotNull("No exception message", emsg);
			assertTrue(emsg.startsWith("Incorrect NumInGroup"));
		}
	}

	/**
	 * QFJ-760
	 */
	@Test
	public void testMessageWithMissingChecksumField() throws Exception {

		// checksum is "merged" into field 452, i.e. SOH is missing between
		// field 452 and 10
		String badMessage = "8=FIX.4.4\0019=275\00135=D\00134=3\00149=441000-XXXXX-X-XXXX-001\001"
				+ "52=20131113-10:22:31.567\00156=XXXXX\0011=A1\00111=9fef3663330e209e1bce\00118=H\001"
				+ "22=4\00138=200\00140=M\00148=XX0005519XXXX\00154=1\00155=[N/A]\00158=MassTest\00159=0\001"
				+ "60=20131113-10:22:31.567\001100=XXXX\001526=9fef3663330e209e1bce\001453=1\001"
				+ "448=XXXXXXXX030\001447=D\001452=3610=016\001";

		try {
			FIXEngine engine = DefaultEngine.getDefaultEngine();
			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIX44,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);
			MessageUtils.parse(FIXApplication.FIX44,
					new GenericMessageBuilderFactory(), provider, badMessage,
					true);
			fail();
		} catch (final InvalidMessage e) {
			final String emsg = e.getMessage();
			assertNotNull("No exception message", emsg);
			assertTrue(emsg.startsWith("Field not found"));
		}
	}

	@Test
	public void testMessageCloneWithGroups() {

		Message message = new Message();
		NewOrderSingle.NoAllocs noAllocs = setUpGroups(message);

		Message clonedMessage = (Message) message.clone();
		NewOrderSingle.NoAllocs clonedNoAllocs = (NewOrderSingle.NoAllocs) clonedMessage
				.getField(78);

		assertEquals(message, clonedMessage);
		assertNotSame(message, clonedMessage);

		assertEquals(noAllocs, clonedNoAllocs);
		assertNotSame(noAllocs, clonedNoAllocs);

		// assertGroupContent(clonedMessage, numAllocs);
	}

	@Test
	public void testFieldOrderAfterClone() {

		FIXMessage message = new org.quickfixj.messages.bd.fix44.NewOrderSingle();

		// Added - TODO these should be acquired via a MessageBuilder
		message.getHeader().setField(MsgType.ORDER_SINGLE);
		message.getHeader().setField(
				new org.quickfixj.messages.bd.fix44.field.BeginString(
						FIXBeginString.FIX44.getValue()));

		NoPartyIDs noPartyIDs = new NoPartyIDs();
		NoPartyIDsGroup partyIdGroup = new NoPartyIDsGroup();
		partyIdGroup.setPartyID(new PartyID("PARTY_1"));
		partyIdGroup.setPartyIDSource(PartyIDSource.DIRECTED_BROKER);
		partyIdGroup.setPartyRole(PartyRole.INTRODUCING_FIRM);
		noPartyIDs.add(partyIdGroup);
		message.setField(noPartyIDs);

		FIXMessage clonedMessage = (FIXMessage) message.clone();

		assertEquals("wrong field order", message.toString(),
				clonedMessage.toString());
		assertEquals(
				"wrong field order",
				"8=FIX.4.4\0019=35\00135=D\001453=1\001448=PARTY_1\001447=I\001452=6\00110=040\001",
				clonedMessage.toString());
	}

	@Test
	@Ignore
	@Deprecated
	// Method no longer exists
	public void testMessageGroupRemovalUsingGroupObject() {
		//
		// final Message message = new Message();
		//
		// NewOrderSingle.NoAllocs numAllocs = setUpGroups(message);
		//
		// // Remove all
		//
		// assertEquals("wrong # of group members", 2,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// message.removeField(numAllocs.getTag());
		//
		// assertEquals("wrong # of group members", 0,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// assertNoZeroLengthGroupTag(message);
		// assertEquals("wrong message length", 0, message.calculateLength());
		//
		// // Remove one at a time
		//
		// numAllocs = setUpGroups(message);
		// assertEquals("wrong # of group members", 2,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// message.removeGroup(2, numAllocs.getTag());
		//
		// assertEquals("wrong # of group members", 1,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// message.removeGroup(1, numAllocs.getTag());
		//
		// assertEquals("wrong # of group members", 0,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// assertNoZeroLengthGroupTag(message);
		// assertEquals("wrong message length", 0, message.calculateLength());
	}

	@Test
	@Ignore
	@Deprecated
	// Method no longer exists
	public void testMessageGroupRemovalUsingGroupFieldTag() {
		//
		// final Message message = new Message();
		//
		// NewOrderSingle.NoAllocs numAllocs = setUpGroups(message);
		//
		// // Remove all
		//
		// assertEquals("wrong # of group members", 2,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// message.removeField(numAllocs.getTag());
		//
		// assertEquals("wrong # of group members", 0,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// assertNoZeroLengthGroupTag(message);
		// assertEquals("wrong message length", 0, message.calculateLength());
		//
		// assertEquals("wrong checksum", 0, message.calculateChecksum());
		//
		// // Remove one at a time
		//
		// numAllocs = setUpGroups(message);
		//
		// assertEquals("wrong # of group members", 2,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// message.removeGroup(2, numAllocs.getTag());
		//
		// assertEquals("wrong # of group members", 1,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// message.removeGroup(1, numAllocs.getTag());
		//
		// assertEquals("wrong # of group members", 0,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// assertNoZeroLengthGroupTag(message);
		// assertEquals("wrong message length", 0, message.calculateLength());
		//
		// assertEquals("wrong checksum", 0, message.calculateChecksum());
	}

	@Test
	@Ignore
	@Deprecated
	// Method no longer exists
	public void testMessageGroupRemovalFromEmptyGroup() {

		// final Message message = new Message();
		// final NewOrderSingle.NoAllocs numAllocs = setUpGroups(message);
		// message.removeField(numAllocs.getTag());
		//
		// // ensure no exception when groups are empty
		// message.removeGroup(1, numAllocs.getTag());
		//
		// assertEquals("wrong # of group members", 0,
		// message.getGroupCount(numAllocs.getTag()));
		//
		// assertNoZeroLengthGroupTag(message);
		// assertEquals("wrong message length", 0, message.calculateLength());
	}

	private void assertNoZeroLengthGroupTag(final Message message) {

		assertFalse("Zero-length group tag exists in message string", message
				.toString().contains("\00178="));
	}

	@Test
	@Ignore
	@Deprecated
	// Method no longer exists
	public void testHasGroup() {

		// final Message message = new Message();
		// final NewOrderSingle.NoAllocs numAllocs = setUpGroups(message);
		//
		// assertFalse("wrong value", message.hasGroup(654));
		// assertTrue("wrong value", message.hasGroup(numAllocs.getTag()));
		// // assertTrue("wrong value", message.hasGroup(numAllocs));
		// // assertTrue("wrong value", message.hasGroup(1, numAllocs));
		// assertTrue("wrong value", message.hasGroup(1, numAllocs.getTag()));
		// // assertTrue("wrong value", message.hasGroup(2, numAllocs));
		// assertTrue("wrong value", message.hasGroup(2, numAllocs.getTag()));
		// // assertFalse("wrong value", message.hasGroup(3, numAllocs));
		// assertFalse("wrong value", message.hasGroup(3, numAllocs.getTag()));
	}

	@Test
	public void testIsEmpty() {

		final Message message = new Message();
		assertTrue("Message should be empty on construction", message.isEmpty());
		message.getHeader().setField(
				new GenericField(FIXTag.BEGIN_STRING, "FIX.4.2"));
		assertFalse("Header should contain a field", message.isEmpty());
		message.clear();
		assertTrue("Message should be empty after clear", message.isEmpty());
		message.setField(new Symbol("MSFT"));
		assertFalse("Body should contain a field", message.isEmpty());
		message.clear();
		assertTrue("Message should be empty after clear", message.isEmpty());
		message.getTrailer().setField(new GenericField(FIXTag.CHECK_SUM, "10"));
		assertFalse("Trailer should contain a field", message.isEmpty());
		message.clear();
		assertTrue("Message should be empty after clear", message.isEmpty());
	}

	@Test
	public void testMessageSetGetString() {

		final Message message = new Message();

		try {
			message.getFieldValue(5);
			assertTrue("exception not thrown", false);
		} catch (FieldNotFound e) {
			// ignore
		}

		message.setString(5, "string5");

		try {
			assertEquals("string5", message.getFieldValue(5));
		} catch (final FieldNotFound e) {
			assertTrue("exception thrown", false);
		}

		try {
			message.setString(100, null);
			assertTrue("exception not thrown", false);
		} catch (NullPointerException e) {
			// ignore
		}
	}

	@Test
	@Ignore
	// tested method replaced by util coerce equivalent
	// - tests added to MessageUtilsTest suite.
	public void testMessagesetGetBoolean() {
		//
		// final Message message = new Message();
		//
		// try {
		// message.getBoolean(7);
		// assertTrue("exception not thrown", false);
		// } catch (FieldNotFound e) {
		// // ignore
		// }
		//
		// message.setBoolean(7, true);
		//
		// try {
		// assertEquals(true, message.getBoolean(7));
		// } catch (final FieldNotFound e) {
		// assertTrue("exception thrown", false);
		// }
	}

	@Test
	@Ignore
	// tested method replaced by util coerce equivalent
	// - tests added to MessageUtilsTest suite.
	public void testMessageSetGetChar() {
		//
		// final Message message = new Message();
		//
		// try {
		// message.getChar(12);
		// assertTrue("exception not thrown", false);
		// } catch (final FieldNotFound e) {
		// // ignore
		// }
		//
		// message.setChar(12, 'a');
		//
		// try {
		// assertEquals('a', message.getChar(12));
		// } catch (final FieldNotFound e) {
		// assertTrue("exception thrown", false);
		// }
	}

	@Test
	@Ignore
	// tested method replaced by util coerce equivalent
	// - tests added to MessageUtilsTest suite.
	public void testMessageSetGetInt() {
		//
		// final Message message = new Message();
		//
		// try {
		// message.getInt(56);
		// assertTrue("exception not thrown", false);
		// } catch (final FieldNotFound e) {
		// // ignore
		// }
		//
		// message.setInt(56, 23);
		//
		// try {
		// assertEquals(23, message.getInt(56));
		// } catch (final FieldNotFound e) {
		// assertTrue("exception thrown", false);
		// }
	}

	@Test
	@Ignore
	// tested method replaced by util coerce equivalent
	// - tests added to MessageUtilsTest suite.
	public void testMessageSetGetDouble() {
		//
		// final Message message = new Message();
		//
		// try {
		// message.getDouble(9812);
		// assertTrue("exception not thrown", false);
		// } catch (final FieldNotFound e) {
		// // ignore
		// }
		//
		// message.setDouble(9812, 12.3443);
		//
		// try {
		// assertEquals(12.3443, message.getDouble(9812), 1e-10);
		// } catch (final FieldNotFound e) {
		// assertTrue("exception thrown", false);
		// }
	}

	@Test
	@Ignore
	// tested method removed
	public void testMessageSetGetUtcTimeStamp() {
		//
		// final Message message = new Message();
		//
		// try {
		// message.getUtcTimeStamp(8);
		// assertTrue("exception not thrown", false);
		// } catch (final FieldNotFound e) {
		// // ignore
		// }
		//
		// final TimeZone timezone = TimeZone.getTimeZone("GMT+0");
		// final Calendar calendar = Calendar.getInstance(timezone);
		// calendar.set(2002, 8, 6, 12, 34, 56);
		// calendar.set(Calendar.MILLISECOND, 0);
		//
		// final Date time = calendar.getTime();
		// message.setUtcTimeStamp(8, time);
		//
		// try {
		// assertEquals(message.getUtcTimeStamp(8).getTime(), time.getTime());
		// } catch (final FieldNotFound e) {
		// assertTrue("exception thrown", false);
		// }
	}

	@Test
	public void testRemoveField() {

		final Message message = new Message();
		message.setField(new GenericField(12, "value"));
		assertTrue(message.isFieldSet(12));
		message.removeField(12);
		assertTrue(!message.isFieldSet(12));
	}

	@Test
	public void testMessageIterator() {

		FIXMessage message = new Message();
		java.util.Iterator<FIXField<?>> i = message.iterator();
		assertEquals(false, i.hasNext());
		try {
			assertNull(i.next());
			fail("exception not thrown");
		} catch (final java.util.NoSuchElementException e) {
			// ignore
		}

		try {
			message = MessageUtils.parse(FIXApplication.FIX42,
					"8=FIX.4.2\0019=12\00135=A\001108=30\00110=026\001");
			i = message.iterator();
			assertTrue(i.hasNext());
			FIXField<?> field = i.next();
			assertEquals(108, field.getTag());
			assertEquals("30", field.getCharacters());

			assertEquals(false, i.hasNext());
			try {
				assertNull(i.next());
				fail("exception not thrown");
			} catch (final java.util.NoSuchElementException e) {
				// ignore
			}

			final java.util.Iterator<FIXField<?>> j = message.getHeader()
					.iterator();
			assertTrue(j.hasNext());
			field = j.next();
			assertEquals(8, field.getTag());
			assertEquals("FIX.4.2", field.getCharacters());
			field = j.next();
			assertEquals(9, field.getTag());
			assertEquals("12", field.getCharacters());
			field = j.next();
			assertEquals(35, field.getTag());
			assertEquals("A", field.getCharacters());

			assertEquals(false, j.hasNext());
			try {
				assertNull(j.next());
				fail("exception not thrown");
			} catch (final java.util.NoSuchElementException e) {
				// ignore
			}
		} catch (final InvalidMessage e) {
			fail("exception thrown");
		}
	}

	@Test
	public void testIsAdmin() {

		assertTrue(new Message(FixMessageTypes.HEARTBEAT).isAdmin());
		assertTrue(new Message(FixMessageTypes.LOGON).isAdmin());
		assertTrue(new Message(FixMessageTypes.LOGOUT).isAdmin());
		assertTrue(new Message(FixMessageTypes.SEQUENCE_RESET).isAdmin());
		assertTrue(new Message(FixMessageTypes.RESEND_REQUEST).isAdmin());
		assertTrue(new Message(FixMessageTypes.TEST_REQUEST).isAdmin());
		assertTrue(new Message(FixMessageTypes.REJECT).isAdmin());
		assertFalse(new Message("D").isAdmin());
		assertFalse(new Message("AJ").isAdmin());
	}

	@Test
	public void testComponent() throws Exception {

		final Instrument instrument = new org.quickfixj.messages.bd.fix44.NewOrderSingle();
		instrument.setSymbol(new org.quickfixj.messages.bd.fix44.field.Symbol(
				"DELL"));
		instrument.setCountryOfIssue(new CountryOfIssue("USA"));
		instrument.setSecurityType(SecurityType.COMMON_STOCK);

		final org.quickfixj.messages.bd.fix44.NewOrderSingle newOrderSingle = new org.quickfixj.messages.bd.fix44.NewOrderSingle();
		newOrderSingle.setCountryOfIssue(new CountryOfIssue("GB"));
		newOrderSingle.copyValues(instrument);
		newOrderSingle.setOrderQty(new OrderQty(100));
		newOrderSingle.setPrice(new Price(45));

		assertEquals(new Symbol("DELL"), newOrderSingle.getSymbol());
		assertEquals(new CountryOfIssue("USA"),
				newOrderSingle.getCountryOfIssue());
		assertEquals(SecurityType.COMMON_STOCK,
				newOrderSingle.getSecurityType());

		newOrderSingle
				.setClOrdID(new org.quickfixj.messages.bd.fix44.field.ClOrdID(
						"CLIENT_ORDER_ID"));
		final org.quickfixj.messages.bd.fix44.NewOrderSingle instrument2 = new org.quickfixj.messages.bd.fix44.NewOrderSingle();
		instrument2.copyValues(instrument);
		assertEquals(new Symbol("DELL"), instrument2.getSymbol());
		assertEquals(new CountryOfIssue("USA"), instrument2.getCountryOfIssue());
		assertEquals(SecurityType.COMMON_STOCK, instrument2.getSecurityType());
		try {
			instrument2.getField(ClOrdID.TAG);
			fail("should have thrown exception");
		} catch (final FieldNotFound e) {
			// expected
		}
	}

	@Test
	public void testReplaceGroup() throws Exception {

		final Message message = new Message();
		message.setField(new ListID("1"));
		message.setField(new BidType(0));
		message.setField(new TotNoOrders(3));

		NewOrderList.NoOrders noOrders;
		NewOrderList.NoOrdersGroup noOrdersGroup;

		noOrders = new NewOrderList.NoOrders();

		noOrdersGroup = new NewOrderList.NoOrdersGroup();
		noOrdersGroup
				.setClOrdID(new org.quickfixj.messages.bd.fix44.field.ClOrdID(
						"A"));
		noOrdersGroup.setListSeqNo(new ListSeqNo(1));
		noOrdersGroup
				.setSymbol(new org.quickfixj.messages.bd.fix44.field.Symbol(
						"DELL"));
		noOrdersGroup.setSide(new org.quickfixj.messages.bd.fix44.field.Side(
				'1'));
		noOrders.add(noOrdersGroup);

		noOrdersGroup = new NewOrderList.NoOrdersGroup();
		noOrdersGroup
				.setClOrdID(new org.quickfixj.messages.bd.fix44.field.ClOrdID(
						"B"));
		noOrdersGroup.setListSeqNo(new ListSeqNo(2));
		noOrdersGroup
				.setSymbol(new org.quickfixj.messages.bd.fix44.field.Symbol(
						"LNUX"));
		noOrdersGroup.setSide(new org.quickfixj.messages.bd.fix44.field.Side(
				'2'));
		noOrders.add(noOrdersGroup);

		noOrdersGroup = new NewOrderList.NoOrdersGroup();
		noOrdersGroup
				.setClOrdID(new org.quickfixj.messages.bd.fix44.field.ClOrdID(
						"C"));
		noOrdersGroup.setListSeqNo(new ListSeqNo(3));
		noOrdersGroup
				.setSymbol(new org.quickfixj.messages.bd.fix44.field.Symbol(
						"RHAT"));
		noOrdersGroup.setSide(new org.quickfixj.messages.bd.fix44.field.Side(
				'3'));
		noOrders.add(noOrdersGroup);

		noOrdersGroup = new NewOrderList.NoOrdersGroup();
		noOrdersGroup
				.setClOrdID(new org.quickfixj.messages.bd.fix44.field.ClOrdID(
						"D"));
		noOrdersGroup.setListSeqNo(new ListSeqNo(4));
		noOrdersGroup
				.setSymbol(new org.quickfixj.messages.bd.fix44.field.Symbol(
						"AAPL"));
		noOrdersGroup.setSide(new org.quickfixj.messages.bd.fix44.field.Side(
				'4'));
		noOrders.set(1, noOrdersGroup);

		message.setField(noOrders);

		assertNotNull(noOrders.get(0));
		assertNotNull(noOrders.get(1));
		assertNotNull(noOrders.get(0));

		assertEquals(3, noOrders.size());
		assertEquals(3, (int) noOrders.getValue());
		noOrders = (NoOrders) message.getField(noOrdersGroup.getFieldTag());
		assertEquals(3, (int) noOrders.getValue());

		// final ClOrdID clOrdID = new ClOrdID();
		noOrdersGroup = noOrders.get(0);
		assertEquals("A", noOrdersGroup.getField(ClOrdID.TAG).getCharacters());
		noOrdersGroup = noOrders.get(1);
		assertEquals("D", noOrdersGroup.getField(ClOrdID.TAG).getCharacters());
		noOrdersGroup = noOrders.get(2);
		assertEquals("C", noOrdersGroup.getField(ClOrdID.TAG).getCharacters());
	}

	@Test
	public void testFalseMessageStructureException() {

		try {
			// duplicated tag 98
			// QFJ-65
			FIXEngine engine = DefaultEngine.getDefaultEngine();
			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIX44,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);
			MessageUtils
					.parse(FIXApplication.FIX44,
							new GenericMessageBuilderFactory(),
							provider,
							"8=FIX.4.4\0019=22\00135=A\00198=0\00198=0\001108=30\00110=223\001",
							true);
			// For now, this will not cause an exception if the length and
			// checksum are correct
		} catch (final Exception e) {
			final String text = e.getMessage();
			assertTrue("Wrong exception message: " + text,
					!text.contains("Actual body length"));
		}
	}

	@Test
	public void testComponentInGroup() {

		try {
			// duplicated tag 98
			// QFJ-65
			// 8=FIX.4.4\0019=941\00135=AE\00149=ICE\00134=63\00152=20091117-18:59:04.780\00156=XXXX\001
			// 57=X\001571=219449\001487=0\001856=0\001828=0\001150=F\00117=44750544433\00139=2\001
			// 570=N\00155=480120\00148=WBS FMG0010-BRN
			// FMG0010\00122=8\001461=FXXXXX\001916=20100201\001
			// 917=20100228\00132=1.0\00131=0.69\0019018=1\0019022=1\00175=20091117\00160=20091117-18:59:04.775\001
			// 552=1\00154=2\00137=41296064\00111=557859232\001453=7\001448=trader\001447=D\001452=11\001
			// 448=Trading
			// Corp\001447=D\001452=13\001448=2757\001447=D\001452=56\001448=805\001447=D\001
			// 452=4\001448=11122556\001447=D\001452=51\001448=FCM\001447=D\001452=60\001448=U\001447=D\001
			// 452=5 4\00158=41293051\001555=2\001600=460130\001602=WBS
			// FMG0010!\001603=8\001608=FXXXXX\001
			// 624=2\001637=80.78\001687=1.0\001654=41296074\0019019=1\0019023=1\0019020=20100201\001
			// 9021=20100228\001539=4\001524=805\001525=D\001538=4\001524=11122556\001525=D\001538=51\001
			// 524=FCM\001525=D\001538=60
			// 524=U\001525=D\001538=54\001600=217927\001
			// 602=BRN FMG0010! 63=8 608-FXXXXX 624=1 637=80.09 687=1.0
			// 654=41296073 9019=1 9023=1 9020=20100201 9021=20100228 539=4
			// 524=805\001
			// 525=D\001538=4\001524=11122556 525=D\001538=51 524=Newedge 525=D
			// 538=60 524=U 525=D 538=54 10=112
			FIXEngine engine = DefaultEngine.getDefaultEngine();
			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIX44,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);
			MessageUtils
					.parse(FIXApplication.FIX44,
							new GenericMessageBuilderFactory(),
							provider,
							"8=FIX.4.4\0019=941\00135=AE\00149=ICE\00134=63\00152=20091117-18:59:04.780\00156=XXXX\001"
									+ "57=X\001571=219449\001487=0\001856=0\001828=0\001150=F\00117=44750544433\00139=2\001"
									+ "570=N\00155=480120\00148=WBS FMG0010-BRN FMG0010\00122=8\001461=FXXXXX\001916=20100201\001"
									+ "917=20100228\00132=1.0\00131=0.69\0019018=1\0019022=1\00175=20091117\00160=20091117-18:59:04.775\001"
									+ "552=1\00154=2\00137=41296064\00111=557859232\001453=7\001448=trader\001447=D\001452=11\001"
									+ "448=Trading Corp\001447=D\001452=13\001448=2757\001447=D\001452=56\001448=805\001447=D\001"
									+ "452=4\001448=11122556\001447=D\001452=51\001448=FCM\001447=D\001452=60\001448=U\001447=D\001"
									+ "452=5 4\00158=41293051\001555=2\001600=460130\001602=WBS FMG0010!\001603=8\001608=FXXXXX\001"
									+ "624=2\001637=80.78\001687=1.0\001654=41296074\0019019=1\0019023=1\0019020=20100201\001"
									+ "9021=20100228\001539=4\001524=805\001525=D\001538=4\001524=11122556\001525=D\001538=51\001"
									+ "524=FCM\001525=D\001538=60 524=U\001525=D\001538=54\001600=217927\001602=BRN FMG0010!\001"
									+ "63=8 608-FXXXXX\001624=1\001637=80.09\001687=1.0\001654=41296073\0019019=1\0019023=1\001"
									+ "9020=20100201\001021=20100228\001539=4\001524=805\001525=D\001538=4\001524=11122556\001"
									+ "525=D\001538=51\001524=FCM\001525=D\001538=60 524=U\001525=D\001538=54\001600=217927\001"
									+ "602=BRN FMG0010!\00163=8 608-FXXXXX\001624=1\001637=80.09\001687=1.0\001654=41296073\001"
									+ "9019=1\0019023=1\0019020=20100201\001021=20100228\001",
							true);
			// For now, this will not cause an exception if the length and
			// checksum are correct
		} catch (final Exception e) {
			final String text = e.getMessage();
			assertTrue("Wrong exception message: " + text,
					!text.contains("Actual body length"));
		}
	}

	@Test
	public void testFalseMessageStructureException2() {

		try {
			// duplicated raw data length
			// QFJ-121
			FIXEngine engine = DefaultEngine.getDefaultEngine();
			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIX44,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);
			MessageUtils
					.parse(FIXApplication.FIX44,
							new GenericMessageBuilderFactory(),
							provider,
							"8=FIX.4.4\0019=22\00135=A\00196=X\001108=30\00110=223\001",
							true);
		} catch (final Exception e) {
			final String text = e.getMessage();
			assertTrue("Wrong exception message: " + text, text != null
					&& !text.contains("Actual body length"));
		}
	}

	@Test
	public void testFieldWithEqualsCharacter() {

		try {
			FIXEngine engine = DefaultEngine.getDefaultEngine();
			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIXT11,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);
			Message m = (Message) MessageUtils
					.parse(FIXApplication.FIX44,
							new GenericMessageBuilderFactory(),
							provider,
							"8=FIXT.1.1\0019=369\00135=W\00149=I\00156=F\00134=4\00152=20111021-15:09:16.535\001"
									+ "262=1319209757316210\00121=2\00155=EUR/USD\001461=RCSXX=0\001268=8\001"
									+ "269=0\001270=1.38898\001271=2000000\001269=0\001270=1.38897\001271=8000000\001"
									+ "269=0\001270=1.38854\001271=2000000\001269=1\001270=1.38855\001271=6000000\001"
									+ "269=1\001270=1.38856\001271=7000000\001269=1\001270=1.38857\001271=3000000\001"
									+ "269=1\001270=1.38858\001271=9000000\001269=1\001270=1.38859\001271=100000000\00110=51\001",
							true);
			assertEquals(m.getFieldValue(461), "RCSXX=0");

			MDFullGrp.NoMDEntriesGroup group;
			group = ((org.quickfixj.messages.bd.fix50.component.MDFullGrp.NoMDEntries) m
					.getField(268)).get(0);
			FIXField<?> px = group.getField(MDEntryPx.TAG);
			assertEquals(px.getCharacters(), "1.38898");

		} catch (final Exception e) {
			String text = e.getMessage();
			assertTrue("Wrong exception message: " + text, text != null
					&& !text.contains("Actual body length"));
		}
	}

	@Test
	public void testMiscFeeType() {

		try {
			FIXEngine engine = DefaultEngine.getDefaultEngine();
			FIXMessageDictionaryFactory provider = engine
					.getMessageDictionaryFactory(FIXBeginString.FIXT11,
							"org.quickfixj.messages.bd");
			assertNotNull(provider);
			Message m = (Message) MessageUtils
					.parse(FIXApplication.FIX44,
							new GenericMessageBuilderFactory(),
							provider,
							"8=FIXT.1.1\0019=369\00135=W\00149=I\00156=F\00134=4\00152=20111021-15:09:16.535\001"
									+ "262=1319209757316210\00121=2\00155=EUR/USD\001461=RCSXX=0\001268=8\001"
									+ "269=0\001270=1.38898\001271=2000000\001269=0\001270=1.38897\001271=8000000\001"
									+ "269=0\001270=1.38854\001271=2000000\001269=1\001270=1.38855\001271=6000000\001"
									+ "269=1\001270=1.38856\001271=7000000\001269=1\001270=1.38857\001271=3000000\001"
									+ "269=1\001270=1.38858\001271=9000000\001269=1\001270=1.38859\001271=100000000\00110=51\001",
							true);
			assertEquals(m.getFieldValue(461), "RCSXX=0");

			MDFullGrp.NoMDEntriesGroup group;
			group = ((org.quickfixj.messages.bd.fix50.component.MDFullGrp.NoMDEntries) m
					.getField(268)).get(0);
			FIXField<?> px = group.getField(MDEntryPx.TAG);
			assertEquals(px.getCharacters(), "1.38898");

		} catch (final Exception e) {
			final String text = e.getMessage();
			assertTrue("Wrong exception message: " + text, text != null
					&& !text.contains("Actual body length"));
		}
	}

	/**
	 * Verify that an empty message can still be "printed" and doesn't result in
	 * any exceptions
	 */
	@Test
	public void testEmptyMessageToString() throws Exception {

		final Message msg = new quickfix.Message();
		assertNotNull(msg.toString());
		assertTrue("empty message contains no checksum", msg.toString()
				.length() > 0);
	}

	@Test
	public void testMessageBytesField() throws Exception {

		final Logon logon = new Logon();

		// Added - TODO these should be acquired via a MessageBuilder
		logon.getHeader().setField(MsgType.LOGON);
		logon.getHeader().setField(
				new org.quickfixj.messages.bd.fix44.field.BeginString(
						FIXBeginString.FIX44.getValue()));

		final String data = "rawdata";
		logon.setRawDataLength(new RawDataLength(data.length()));
		logon.setRawData(new RawData(data.getBytes()));

		assertEquals(
				"8=FIX.4.4\0019=21\00135=A\00195=7\00196=rawdata\00110=086\001",
				logon.toString());
	}

	// QFJ-786
	@Test
	@Ignore
	// SBF - I cannot figure out what this is meant to test
	public void testInvalidHeaderFields() throws Exception {

		String message = "1=03100~11=140605JWI4440~15=USD~21=1~35=D~38=6~40=1~47=A~54=1~55=UNP~59=0~60=20140605-13:58:51.016~63=0~79=03100631~336=1~377=N~581=A~6020=COMPUS~86028=JWI~86029=4440~";
		message = message.replace('~', (char) 1);

		System.out.println("MessageTest.assertHeaderField() " + message);

		FIXMessage msg = MessageUtils.parse(FIXApplication.FIX42, message,
				false);
		assertTrue(msg.isFieldSet(Account.TAG));
	}

	private void assertHeaderField(FIXMessage message, String expectedValue,
			int field) throws FieldNotFound {

		assertEquals(expectedValue, message.getHeader().getFieldValue(field));
		assertFieldNotFound(message, field);
		assertFieldNotFound(message.getTrailer(), field);
	}

	private void assertFieldNotFound(FIXFieldGraph message, int field) {

		try {
			message.getField(field);
			fail("field shouldn't be here: " + field);
		} catch (final FieldNotFound e) {
			// expected
		}
	}

	private void assertBodyField(FIXMessage message, String expectedValue,
			int field) throws FieldNotFound {

		assertEquals(expectedValue, message.getFieldValue(field));
		assertFieldNotFound(message.getHeader(), field);
		assertFieldNotFound(message.getTrailer(), field);
	}

	private void assertTrailerField(FIXMessage message, String expectedValue,
			int field) throws FieldNotFound {

		assertEquals(expectedValue, message.getTrailer().getFieldValue(field));
		assertFieldNotFound(message, field);
		assertFieldNotFound(message.getHeader(), field);
	}

	@Deprecated
	// Method no longer exists
	private void assertGroupContent(Message message,
			NewOrderSingle.NoAllocs numAllocs) {
		//
		// FIXField<?> field;
		// final java.util.Iterator<FIXField<?>> i =
		// numAllocs.get(1).iterator();
		// assertTrue(i.hasNext());
		// field = i.next();
		// assertEquals("AllocACC2", field.getCharacters());
		// assertTrue(i.hasNext());
		// field = i.next();
		// assertEquals("2020.20", field.getCharacters());
		// assertTrue(!i.hasNext());
		//
		// try {
		// final String accountId =
		// numAllocs.get(1).getAllocAccount().getValue();
		// final Object shares = numAllocs.get(1).getAllocShares().getValue();
		// // message.getGroup(1, numAllocs.get(1));
		// assertAllocation(accountId, ((NoAllocs)
		// message.getField(78)).get(0).getAllocAccount()
		// .getValue());
		// message.getGroup(2, numAllocs.get(1));
		// assertEquals("AllocACC2", accountId);
		// assertAllocation(accountId, shares);
		// } catch (final FieldNotFound e) {
		// fail("no exception should be thrown");
		// }
		//
		// try {
		// message.getGroup(3, numAllocs.get(1));
		// fail("exception should be thrown");
		// } catch (final FieldNotFound ignored) {
		// // ignore
		// }
	}

	private void assertAllocation(String accountId, Object shares) {

		if (accountId.equals("AllocACC1")) {
			assertEquals("got shares: " + shares, 0,
					new BigDecimal("1010.10").compareTo(new BigDecimal(shares
							.toString())));
		} else if (accountId.equals("AllocACC2")) {
			assertEquals("got shares: " + shares, 0,
					new BigDecimal("2020.20").compareTo(new BigDecimal(shares
							.toString())));
		} else {
			fail("Unknown account");
		}
	}

	private NewOrderSingle.NoAllocs setUpGroups(Message message) {

		NewOrderSingle.NoAllocs noAllocs;
		NewOrderSingle.NoAllocsGroup numAllocs;

		noAllocs = new NoAllocs();

		// create group with type safe setters
		numAllocs = noAllocs.add(new NewOrderSingle.NoAllocsGroup());
		numAllocs.setAllocAccount(new AllocAccount("AllocACC1"));
		numAllocs.setAllocShares(new AllocShares(new BigDecimal("1010.10")));

		// create group with generic setters
		numAllocs = noAllocs.add(new NewOrderSingle.NoAllocsGroup());
		numAllocs.setField(new AllocAccount("AllocACC2"));
		numAllocs.setField(new AllocShares(new BigDecimal("2020.20")));

		message.setField(noAllocs);

		return noAllocs;
	}
}
