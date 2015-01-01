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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

import java.io.InvalidObjectException;

import org.junit.Before;
import org.junit.Test;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXMessage;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.messages.fixt11.field.ApplVerID;
import org.quickfixj.messages.fixt11.field.DefaultApplVerID;
import org.quickfixj.messages.fixt11.field.EncryptMethod;
import org.quickfixj.messages.fixt11.field.HeartBtInt;

import quickfix.MessageCracker.RedundantHandlerException;

public class MessageCrackerTest {

	private int messageCracked;

	private Session mockSession;

	@Before
	public void setUp() throws Exception {

		mockSession = mock(Session.class);
		stub(mockSession.getTargetDefaultApplicationVersionID()).toReturn(
				FIXApplication.FIX50SP2);
	}

	@Test(expected = UnsupportedMessageType.class)
	public void testInvokerException1() throws Exception {

		org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
				EncryptMethod.NONE_OTHER, new HeartBtInt(30),
				new DefaultApplVerID(ApplVerID.FIX42.getValue()));

		MessageCracker cracker = new MessageCracker() {

			@Handler
			public void handle(org.quickfixj.messages.fixt11.Logon logon,
					FIXSessionID sessionID) throws UnsupportedMessageType {

				throw new UnsupportedMessageType();
			}
		};

		cracker.crack(logon, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));
	}

	@Test(expected = FieldNotFound.class)
	public void testInvokerException2() throws Exception {

		org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
				EncryptMethod.NONE_OTHER, new HeartBtInt(30),
				new DefaultApplVerID(ApplVerID.FIX42.getValue()));

		MessageCracker cracker = new MessageCracker() {

			@Handler
			public void handle(org.quickfixj.messages.fixt11.Logon logon,
					FIXSessionID sessionID) throws FieldNotFound {

				throw new FieldNotFound(10);
			}
		};

		cracker.crack(logon, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));
	}

	@Test(expected = IncorrectTagValue.class)
	public void testInvokerException3() throws Exception {

		org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
				EncryptMethod.NONE_OTHER, new HeartBtInt(30),
				new DefaultApplVerID(ApplVerID.FIX42.getValue()));

		MessageCracker cracker = new MessageCracker() {

			@Handler
			public void handle(org.quickfixj.messages.fixt11.Logon logon,
					FIXSessionID sessionID) throws IncorrectTagValue {

				throw new IncorrectTagValue("test");
			}
		};

		cracker.crack(logon, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));
	}

	@Test(expected = RuntimeException.class)
	public void testInvokerException4() throws Exception {

		org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
				EncryptMethod.NONE_OTHER, new HeartBtInt(30),
				new DefaultApplVerID(ApplVerID.FIX42.getValue()));

		MessageCracker cracker = new MessageCracker() {

			@Handler
			public void handle(org.quickfixj.messages.fixt11.Logon logon,
					FIXSessionID sessionID) throws InvalidObjectException {

				throw new InvalidObjectException("test");
			}
		};

		cracker.crack(logon, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));
	}

	@Test
	public void testAnnotationBasedCracking() throws Exception {

		org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
				EncryptMethod.NONE_OTHER, new HeartBtInt(30),
				new DefaultApplVerID(ApplVerID.FIX42.getValue()));

		MessageCracker cracker = new MessageCracker() {

			@Handler
			public void handle(org.quickfixj.messages.fixt11.Logon logon,
					FIXSessionID sessionID) {

				messageCracked++;
			}
		};

		cracker.crack(logon, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));

		assertTrue(messageCracked > 0);
	}

	@Test(expected = RedundantHandlerException.class)
	public void testDuplicateCracks() throws Exception {

		try {
			@SuppressWarnings("unused")
			MessageCracker cracker = new MessageCracker() {

				@SuppressWarnings("unused")
				public void onMessage(
						org.quickfixj.messages.fixt11.Logon logon,
						FIXSessionID sessionID) {

					messageCracked++;
				}

				@Handler
				public void handle(org.quickfixj.messages.fixt11.Logon logon,
						FIXSessionID sessionID) {

					messageCracked++;
				}
			};
		} catch (Exception e) {
			// toString smoke test
			assertThat(e.toString(), notNullValue());
			throw e;
		}
	}

	@Test()
	public void testFallback() throws Exception {

		org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
				EncryptMethod.NONE_OTHER, new HeartBtInt(30),
				new DefaultApplVerID(ApplVerID.FIX42.getValue()));

		MessageCracker cracker = new MessageCracker() {

			@Override
			protected void onMessage(FIXMessage message, FIXSessionID sessionID) {

				messageCracked++;
			}
		};

		cracker.crack(logon, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));

		assertTrue(messageCracked > 0);
	}

	@Test(expected = UnsupportedMessageType.class)
	public void testFallbackWithNoOverrid() throws Exception {

		org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
				EncryptMethod.NONE_OTHER, new HeartBtInt(30),
				new DefaultApplVerID(ApplVerID.FIX42.getValue()));

		MessageCracker cracker = new MessageCracker();

		cracker.crack(logon, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));
	}

	@Test()
	public void testExternalDelegation() throws Exception {

		org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
				EncryptMethod.NONE_OTHER, new HeartBtInt(30),
				new DefaultApplVerID(ApplVerID.FIX42.getValue()));

		MessageCracker cracker = new MessageCracker(new MessageHandler());

		cracker.crack(logon, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));

		assertTrue(messageCracked > 0);
	}

	//
	// Legacy Tests
	//

	@Test
	public void testFixT11AppMessageCracking() throws Exception {

		org.quickfixj.messages.bd.fix50.Email message = new org.quickfixj.messages.bd.fix50.Email();

		message.getHeader()
				.setField(
						new org.quickfixj.messages.bd.fix50.field.SenderCompID(
								"SENDER"));
		message.getHeader()
				.setField(
						new org.quickfixj.messages.bd.fix50.field.TargetCompID(
								"TARGET"));
		message.getHeader().setField(
				new org.quickfixj.messages.bd.fix50.field.BeginString(
						FIXBeginString.FIXT11.getValue()));
		message.getHeader().setField(ApplVerID.FIX50SP2);

		MessageCracker cracker = new MessageCracker() {

			@SuppressWarnings("unused")
			public void onMessage(org.quickfixj.messages.bd.fix50.Email email,
					FIXSessionID sessionID) {

				messageCracked++;
			}
		};

		cracker.crack(message, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));

		assertTrue(messageCracked > 0);
	}

	@Test
	public void testFixtMessageCrackingWithNonFix50ApplVerID() throws Exception {

		org.quickfixj.messages.bd.fix44.Email message = createFix44Email();
		message.getHeader().setField(ApplVerID.FIX44);

		MessageCracker cracker = new MessageCracker() {

			@SuppressWarnings("unused")
			public void onMessage(org.quickfixj.messages.bd.fix44.Email email,
					FIXSessionID sessionID) {

				messageCracked++;
			}
		};

		cracker.crack(message, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));

		assertTrue(messageCracked > 0);
	}

	@Test
	public void testFixtMessageCrackingWithSessionDefaultApplVerID()
			throws Exception {

		org.quickfixj.messages.bd.fix44.Email message = createFix44Email();
		stub(mockSession.getTargetDefaultApplicationVersionID()).toReturn(
				FIXApplication.FIX50SP2);

		MessageCracker cracker = new MessageCracker() {

			@SuppressWarnings("unused")
			public void onMessage(org.quickfixj.messages.bd.fix44.Email email,
					FIXSessionID sessionID) {

				messageCracked++;
			}
		};

		cracker.crack(message, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));

		assertTrue(messageCracked > 0);
	}

	@Test
	public void testFixtAdminMessageCracking() throws Exception {

		org.quickfixj.messages.fixt11.Logon logon = new org.quickfixj.messages.fixt11.Logon(
				EncryptMethod.NONE_OTHER, new HeartBtInt(30),
				new DefaultApplVerID(ApplVerID.FIX42.getValue()));

		MessageCracker cracker = new MessageCracker() {

			@SuppressWarnings("unused")
			public void onMessage(org.quickfixj.messages.fixt11.Logon logon,
					FIXSessionID sessionID) {

				messageCracked++;
			}
		};

		cracker.crack(logon, new SessionID(FIXBeginString.FIXT11, "SENDER",
				"TARGET"));

		assertTrue(messageCracked > 0);
	}

	private org.quickfixj.messages.bd.fix44.Email createFix44Email() {

		org.quickfixj.messages.bd.fix44.Email message = new org.quickfixj.messages.bd.fix44.Email();

		message.getHeader()
				.setField(
						new org.quickfixj.messages.bd.fix44.field.SenderCompID(
								"SENDER"));
		message.getHeader()
				.setField(
						new org.quickfixj.messages.bd.fix44.field.TargetCompID(
								"TARGET"));
		message.getHeader().setField(
				new org.quickfixj.messages.bd.fix44.field.BeginString(
						FIXBeginString.FIXT11.getValue()));

		return message;
	}

	private class MessageHandler {

		/**
		 * @param logon
		 * @param sessionID
		 */
		@MessageCracker.Handler
		public void handle(org.quickfixj.messages.fixt11.Logon logon,
				FIXSessionID sessionID) {

			messageCracked++;
		}
	}
}
