package quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static quickfix.SessionFactoryTestSupport.createSession;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.Test;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.spi.MessageBuilderServiceLoader;

import quickfix.field.converter.UtcTimestampConverter;
import quickfix.fix40.field.Text;
import quickfix.fix44.Heartbeat;
import quickfix.fix44.Logon;
import quickfix.fix44.Logout;
import quickfix.fix44.News;
import quickfix.fix44.Reject;
import quickfix.fix44.ResendRequest;
import quickfix.fix44.SequenceReset;
import quickfix.fix44.TestRequest;
import quickfix.fix44.field.BeginString;
import quickfix.fix44.field.EncryptMethod;
import quickfix.fix44.field.Headline;
import quickfix.fix44.field.MsgSeqNum;
import quickfix.fix44.field.MsgType;
import quickfix.fix44.field.SenderCompID;
import quickfix.fix44.field.SendingTime;
import quickfix.fix44.field.TargetCompID;
import quickfix.fix44.field.TestReqID;
import quickfix.fixt11.field.ApplVerID;
import quickfix.fixt11.field.DefaultApplVerID;
import quickfix.fixt11.field.SessionStatus;
import quickfix.test.util.ReflectionUtil;

/**
 * Note: most session tests are in the form of acceptance tests.
 */
public class SessionTest {

    @AfterClass
    public static void cleanup() {

        SystemTime.setTimeSource(null);
    }

    @Test
    public void testDisposalOfFileResources() throws Exception {

        final Application application = new UnitTestApplication();

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");

        final MessageStoreFactory mockMessageStoreFactory = mock(MessageStoreFactory.class);
        final CloseableMessageStore mockMessageStore = mock(CloseableMessageStore.class);
        stub(mockMessageStoreFactory.create(sessionID)).toReturn(mockMessageStore);

        final LogFactory mockLogFactory = mock(LogFactory.class);
        final CloseableLog mockLog = mock(CloseableLog.class);
        stub(mockLogFactory.create(sessionID)).toReturn(mockLog);

        final Session session = new Session(application, mockMessageStoreFactory, sessionID, null,
                null, mockLogFactory, MessageBuilderServiceLoader.getMessageBuilderFactory(), 30,
                false, 30, true, true, false, false, false, false, false, true, false, 1.5, null,
                true, new int[] { 5 }, false, false, false, true, false, true, false, null, true,
                0, false, false);

        // Simulate socket disconnect
        session.setResponder(null);

        session.close();

        verify(mockMessageStore).close();
        verifyNoMoreInteractions(mockMessageStore);

        verify(mockLog, atLeastOnce()).onEvent(anyString());
        verify(mockLog).close();
        verifyNoMoreInteractions(mockLog);
    }

    /**
     * This is a smoke test for handling noncloseable resources. Obviously,
     * these resources should not be closed. If they are, it will generate an
     * error (probably a class cast exception).
     *
     * @throws Exception
     */
    @Test
    public void testNondisposableFileResources() throws Exception {

        final Application application = new UnitTestApplication();

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");

        final MessageStoreFactory mockMessageStoreFactory = mock(MessageStoreFactory.class);
        final MessageStore mockMessageStore = mock(MessageStore.class);
        stub(mockMessageStoreFactory.create(sessionID)).toReturn(mockMessageStore);

        final LogFactory mockLogFactory = mock(LogFactory.class);
        final Log mockLog = mock(Log.class);
        stub(mockLogFactory.create(sessionID)).toReturn(mockLog);

        final Session session = new Session(application, mockMessageStoreFactory, sessionID, null,
                null, mockLogFactory, MessageBuilderServiceLoader.getMessageBuilderFactory(), 30,
                false, 30, true, true, false, false, false, false, false, true, false, 1.5, null,
                true, new int[] { 5 }, false, false, false, true, false, true, false, null, true,
                0, false, false);

        // Simulate socket disconnect
        session.setResponder(null);

        verifyNoMoreInteractions(mockMessageStore);

        verify(mockLog, atLeastOnce()).onEvent(anyString());
        verifyNoMoreInteractions(mockLog);

        session.close();
    }

    private interface CloseableMessageStore extends MessageStore, Closeable {
        //no-op
    }

    private interface CloseableLog extends Log, Closeable {
        //no-op
    }

    @Test
    public void testSessionWithoutValidateSequenceNumbers() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final Session session = createSession(sessionID, application, true, true, false);

        final UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);

        session.logon();
        session.next();

        final Message logonRequest = new Message(responder.sentMessageData);
        session.next(createLogonResponse(sessionID, logonRequest, 1));

        assertEquals(1, application.lastToAdminMessage().getHeader().getInt(FixTags.MSG_SEQ_NUM));
        assertEquals(2, session.getStore().getNextTargetMsgSeqNum());
        assertEquals(2, session.getStore().getNextSenderMsgSeqNum());

        session.next(createHeartbeatMessage(1002));
        assertFalse(ResendRequest.MSGTYPE.equals(application.lastToAdminMessage().getHeader()
                .getString(FixTags.MSG_TYPE)));

        session.next(createHeartbeatMessage(1003));
        assertFalse(ResendRequest.MSGTYPE.equals(application.lastToAdminMessage().getHeader()
                .getString(FixTags.MSG_TYPE)));

        session.next(createHeartbeatMessage(1001));
        assertFalse(ResendRequest.MSGTYPE.equals(application.lastToAdminMessage().getHeader()
                .getString(FixTags.MSG_TYPE)));

        session.close();
    }

    // QFJ-703
    @Test
    public void testPossDupMessageWithoutOrigSendingTime() throws Exception {

        // test default behaviour, i.e. that the message is rejected
        // when not setting 122/OrigSendingTime
        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        UnitTestApplication application = new UnitTestApplication();
        Session session = createSession(sessionID, application, true, true, true);
        UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);

        session.logon();
        session.next();

        Message logonRequest = new Message(responder.sentMessageData);
        session.next(createLogonResponse(sessionID, logonRequest, 1));

        assertEquals(1, application.lastToAdminMessage().getHeader().getInt(FixTags.MSG_SEQ_NUM));
        assertEquals(2, session.getStore().getNextTargetMsgSeqNum());
        assertEquals(2, session.getStore().getNextSenderMsgSeqNum());

        News newsMessage = createAppMessage(2);
        newsMessage.getHeader().setBoolean(FixTags.POSS_DUP_FLAG, true);
        session.next(newsMessage);
        assertTrue(Reject.MSGTYPE.equals(application.lastToAdminMessage().getHeader()
                .getString(FixTags.MSG_TYPE)));
        assertNull(application.lastFromAppMessage());

        // test that the message is NOT rejected when
        // setting requiresOrigSendingTime=false
        // and not setting 122/OrigSendingTime
        application = new UnitTestApplication();
        session = createSession(sessionID, application, true, true, true);
        responder = new UnitTestResponder();
        session.setRequiresOrigSendingTime(false);

        session.setResponder(responder);

        session.logon();
        session.next();

        logonRequest = new Message(responder.sentMessageData);
        session.next(createLogonResponse(sessionID, logonRequest, 1));

        assertEquals(1, application.lastToAdminMessage().getHeader().getInt(FixTags.MSG_SEQ_NUM));
        assertEquals(2, session.getStore().getNextTargetMsgSeqNum());
        assertEquals(2, session.getStore().getNextSenderMsgSeqNum());

        newsMessage = createAppMessage(2);
        newsMessage.getHeader().setBoolean(FixTags.POSS_DUP_FLAG, true);
        session.next(newsMessage);
        assertTrue(Logon.MSGTYPE.equals(application.lastToAdminMessage().getHeader()
                .getString(FixTags.MSG_TYPE)));
        assertNull(application.lastToAppMessage());
        assertTrue(News.MSGTYPE.equals(application.lastFromAppMessage().getHeader()
                .getString(FixTags.MSG_TYPE)));

        session.close();
    }

    @Test
    public void testInferResetSeqNumAcceptedWithNonInitialSequenceNumber() throws Exception {

        final Application application = new UnitTestApplication();

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final Session session = createSession(sessionID, application, true, true);

        final UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);

        session.logon();
        session.next();

        final Message logonRequest = new Message(responder.sentMessageData);
        session.next(createLogonResponse(sessionID, logonRequest, 2));

        assertTrue("Should not infer a reset when the sequence number is not one",
                responder.disconnectCalled);

        session.close();
    }

    @Test
    public void testInferResetSeqNumAccepted() throws Exception {

        final Application application = new UnitTestApplication();

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final Session session = createSession(sessionID, application, true, true);

        final UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);

        session.logon();
        session.next();

        final Message logonRequest = new Message(responder.sentMessageData);
        final Message logonResponse = createLogonResponse(sessionID, logonRequest, 1);
        session.next(logonResponse);

        assertFalse("Should not disconnect when an accepted reset is inferred",
                responder.disconnectCalled);

        session.close();
    }

    @Test
    // QFJ-603
    public void testUnsupportedVersion() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final Session session = createSession(sessionID, application, true, true);

        final UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);

        session.logon();
        session.next();

        final Message logonRequest = new Message(responder.sentMessageData);
        final Message logonResponse = createLogonResponse(sessionID, logonRequest, 1);
        session.next(logonResponse);

        final News newsMessage = createAppMessage(2);
        // set a BeginString unsupported by the session
        newsMessage.getHeader().setString(FixTags.BEGIN_STRING, FixVersions.BEGINSTRING_FIX40);
        session.next(newsMessage);
        final Message lastToAdminMessage = application.lastToAdminMessage();
        assertEquals(FixMessageTypes.LOGOUT,
                lastToAdminMessage.getHeader().getString(FixTags.MSG_TYPE));
        assertEquals(
                "Incorrect BeginString: Message version 'FIX.4.0' does not match the session version 'FIX.4.4'",
                lastToAdminMessage.getString(quickfix.fix44.field.Text.TAG));
        assertTrue(responder.disconnectCalled);

        session.close();
    }

    // QFJ-650
    @Test
    public void testLogoutOnMissingMsgSeqNum() throws Exception {

        final Application application = new UnitTestApplication();
        final Session session = setUpSession(application, false, new UnitTestResponder());
        final SessionState state = getSessionState(session);

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        logonTo(session);

        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(2, state.getNextTargetMsgSeqNum());

        final TestRequest testRequest = (TestRequest) createAdminMessage(2);
        session.next(testRequest);

        assertEquals(3, state.getNextSenderMsgSeqNum());
        assertEquals(3, state.getNextTargetMsgSeqNum());

        testRequest.getHeader().removeField(FixTags.MSG_SEQ_NUM);
        // this should disconnect the session due to the missing MsgSeqNum
        session.next(testRequest);
        assertFalse("Session should be disconnected", session.isLoggedOn());

        // make sure that the target seq num has not been incremented
        assertEquals(4, state.getNextSenderMsgSeqNum());
        assertEquals(3, state.getNextTargetMsgSeqNum());
        session.setResponder(new UnitTestResponder());
        logonTo(session, 3);
        assertEquals(5, state.getNextSenderMsgSeqNum());
        assertEquals(4, state.getNextTargetMsgSeqNum());
        assertTrue("Session should be connected", session.isLoggedOn());

        session.close();
    }

    // QFJ-750
    @Test
    public void testLogoutMsgSeqNumTooHighOrLow() throws Exception {

        final Application application = new UnitTestApplication();
        final Session session = setUpSession(application, false, new UnitTestResponder());
        final SessionState state = getSessionState(session);

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        logonTo(session);

        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(2, state.getNextTargetMsgSeqNum());

        final TestRequest testRequest = (TestRequest) createAdminMessage(2);

        session.next(testRequest);

        assertEquals(3, state.getNextSenderMsgSeqNum());
        assertEquals(3, state.getNextTargetMsgSeqNum());

        logoutFrom(session, 100);
        assertFalse("Session should be disconnected", session.isLoggedOn());

        // make sure that the target seq num has not been incremented
        assertEquals(4, state.getNextSenderMsgSeqNum());
        assertEquals(3, state.getNextTargetMsgSeqNum());
        session.setResponder(new UnitTestResponder());
        logonTo(session, 3);
        assertEquals(5, state.getNextSenderMsgSeqNum());
        assertEquals(4, state.getNextTargetMsgSeqNum());
        assertTrue("Session should be connected", session.isLoggedOn());

        logoutFrom(session, 1);
        // make sure that the target seq num has not been incremented
        assertEquals(6, state.getNextSenderMsgSeqNum());
        assertEquals(4, state.getNextTargetMsgSeqNum());

        session.close();
    }

    @Test
    public void testRejectMsgSeqNumTooHighOrLow() throws Exception {

        final Application application = new UnitTestApplication();
        final Session session = setUpSession(application, false, new UnitTestResponder());
        final SessionState state = getSessionState(session);

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        logonTo(session);

        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(2, state.getNextTargetMsgSeqNum());

        processMessage(session, createReject(2, 100));
        assertEquals(3, state.getNextTargetMsgSeqNum());

        // Reject with unexpected seqnum should not increment target seqnum
        processMessage(session, createReject(50, 100));
        assertEquals(3, state.getNextTargetMsgSeqNum());

        // Reject with unexpected seqnum should not increment target seqnum
        processMessage(session, createReject(1, 100));
        assertEquals(3, state.getNextTargetMsgSeqNum());

        session.close();
    }

    /**
     * QFJ-357 Until QF/J 1.5.1 the behaviour was observed that a Logout message
     * was always sent as first message. This could be be provoked by altering
     * the Session file to contain an old timestamp and deleting the filestore
     * files to set the sequence numbers to 1. On the next Logon attempt, the
     * Session would get reset and a Logout message would get sent. On versions
     * newer than 1.5.1 this test should pass.
     */
    @Test
    public void testLogonIsFirstMessageOnAcceptor() throws Exception {

        // set up some basic stuff
        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final SessionSettings settings = SessionSettingsTest.setUpSession(null);

        setupFileStoreForQFJ357(sessionID, settings);

        // Session gets constructed, triggering a reset
        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpFileStoreSession(application, false, new UnitTestResponder(),
                settings, sessionID);
        final SessionState state = getSessionState(session);

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        logonTo(session);

        // we should only answer with a Logon message
        assertEquals(1, application.toAdminMessages.size());
        assertEquals(FixMessageTypes.LOGON, application.toAdminMessages.get(0).getHeader()
                .getString(FixTags.MSG_TYPE));

        // no reset should have been triggered by QF/J after the Logon attempt
        assertEquals(0, application.sessionResets);
        assertTrue("Session should be connected", session.isLoggedOn());

        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(2, state.getNextTargetMsgSeqNum());

        session.close();
    }

    // QFJ-773
    @Test
    public void testLogonLogoutOnAcceptor() throws Exception {

        final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        final Date now = new Date();
        final MockSystemTimeSource systemTimeSource = new MockSystemTimeSource(now.getTime());
        SystemTime.setTimeSource(systemTimeSource);
        // set up some basic stuff
        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final SessionSettings settings = SessionSettingsTest.setUpSession(null);
        settings.setString("StartTime", dateFormat.format(now.getTime() - 100000));
        settings.setString("EndTime", dateFormat.format(now.getTime() + 3600000));
        settings.setString("TimeZone", TimeZone.getDefault().getID());
        setupFileStoreForQFJ357(sessionID, settings);

        // Session gets constructed, triggering a reset
        final UnitTestApplication application = new UnitTestApplication();
        final UnitTestResponder responder = new UnitTestResponder();
        final Session session = setUpFileStoreSession(application, false, responder, settings,
                sessionID);
        session.addStateListener(application);
        final SessionState state = getSessionState(session);

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        logonTo(session);

        // we should only answer with a Logon message
        assertEquals(1, application.toAdminMessages.size());
        assertEquals(FixMessageTypes.LOGON, application.toAdminMessages.get(0).getHeader()
                .getString(FixTags.MSG_TYPE));

        // no reset should have been triggered by QF/J after the Logon attempt
        assertEquals(0, application.sessionResets);
        assertTrue("Session should be connected", session.isLoggedOn());

        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(2, state.getNextTargetMsgSeqNum());

        session.next();
        // increment time to force logout and reset
        systemTimeSource.increment(3700000);
        session.next();
        assertEquals(SystemTime.getDate(), state.getCreationTime());
        systemTimeSource.increment(10000);
        session.next();
        systemTimeSource.increment(10000);
        session.next();
        systemTimeSource.increment(10000);
        session.next();
        systemTimeSource.increment(10000);

        // we should only reset once outside of the session time window
        assertEquals(1, application.sessionResets);
        assertFalse("Session should be disconnected", session.isLoggedOn());

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        session.setResponder(responder);
        // this should get rejected since we are outside of the session time
        // window
        logonTo(session);
        assertFalse("Session should be disconnected", session.isLoggedOn());

        // if we now logon to the session, it will be considered new
        // and a reset will be done
        session.setResponder(responder);
        session.next();
        assertEquals(2, application.sessionResets);
        systemTimeSource.increment(86100000); // jump one day but stay inside
                                              // session time
        session.next();
        logonTo(session);
        assertTrue("Session should be connected", session.isLoggedOn());
        assertEquals(SystemTime.getDate(), state.getCreationTime());

        // check that the creation time is not updated inside of the session
        // time window
        int delta = 60000;
        systemTimeSource.increment(delta);
        assertTrue(SystemTime.getDate().getTime() - state.getCreationTime().getTime() == delta);
        session.next();
        assertTrue("Session should be connected", session.isLoggedOn());

        session.close();
    }

    @Test
    // QFJ-716
    public void testStartOfInitiatorOutsideOfSessionTime() throws Exception {

        final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        final Date now = new Date();
        final MockSystemTimeSource systemTimeSource = new MockSystemTimeSource(now.getTime());
        SystemTime.setTimeSource(systemTimeSource);
        // set up some basic stuff
        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final SessionSettings settings = SessionSettingsTest.setUpSession(null);
        // we want to start the initiator before the StartTime
        settings.setString("StartTime", dateFormat.format(now.getTime() + 1800000)); // add 30 minutes
        settings.setString("EndTime", dateFormat.format(now.getTime() + 3600000));
        settings.setString("TimeZone", TimeZone.getDefault().getID());
        setupFileStoreForQFJ357(sessionID, settings);

        // Session gets constructed, triggering a reset
        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpFileStoreSession(application, true, new UnitTestResponder(),
                settings, sessionID);
        session.addStateListener(application);
        final SessionState state = getSessionState(session);

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        session.next();
        systemTimeSource.increment(10000);
        session.next();
        systemTimeSource.increment(10000);
        session.next();

        // we should send no messages since we are outside of session time
        assertEquals(0, application.toAdminMessages.size());
        // no reset should have been triggered by QF/J (since we were not logged
        // on)
        assertEquals(0, application.sessionResets);
        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        // increase time to be within session time
        systemTimeSource.increment(1900000);
        session.next();
        session.next();
        // we should have sent a Logon since the StartTime has been reached now
        assertEquals(1, application.toAdminMessages.size());
        Message logon = application.toAdminMessages.get(0);
        assertEquals(FixMessageTypes.LOGON, logon.getHeader().getString(FixTags.MSG_TYPE));
        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());
        Message createLogonResponse = createLogonResponse(new SessionID(FIXBeginString.FIX44,
                "TARGET", "SENDER"), logon, 1);
        session.next(createLogonResponse);
        assertTrue(session.isLoggedOn());
        assertEquals(1, application.sessionResets);

        // increase time to be out of session time
        systemTimeSource.increment(1900000);
        session.next();
        Message logout = application.lastToAdminMessage();
        assertEquals(FixMessageTypes.LOGOUT, logout.getHeader().getString(FixTags.MSG_TYPE));
        assertFalse(session.isLoggedOn());
        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());
        assertEquals(2, application.sessionResets);

        session.close();
    }

    @Test
    // QFJ-716 - we need to make sure that the first message sent is a Logon
    public void testStartOfInitiatorInsideOfSessionTime() throws Exception {

        final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        final Date now = new Date();
        final MockSystemTimeSource systemTimeSource = new MockSystemTimeSource(now.getTime());
        SystemTime.setTimeSource(systemTimeSource);
        // set up some basic stuff
        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final SessionSettings settings = SessionSettingsTest.setUpSession(null);
        // we want to start the initiator before the StartTime
        settings.setString("StartTime", dateFormat.format(now.getTime() - 2000)); // make
                                                                                  // sure
                                                                                  // we
                                                                                  // start
                                                                                  // inside
                                                                                  // the
                                                                                  // Session
                                                                                  // time
        settings.setString("EndTime", dateFormat.format(now.getTime() + 3600000));
        settings.setString("TimeZone", TimeZone.getDefault().getID());
        setupFileStoreForQFJ357(sessionID, settings);

        // Session gets constructed, triggering a reset
        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpFileStoreSession(application, true, new UnitTestResponder(),
                settings, sessionID);
        final SessionState state = getSessionState(session);

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        session.next();
        systemTimeSource.increment(1000);
        session.next();
        systemTimeSource.increment(1000);
        session.next();

        // we should have sent a Logon since we are inside of the SessionTime
        assertEquals(1, application.toAdminMessages.size());
        assertEquals(FixMessageTypes.LOGON, application.toAdminMessages.get(0).getHeader()
                .getString(FixTags.MSG_TYPE));
        // no reset should have been triggered by QF/J
        assertEquals(0, application.sessionResets);

        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        session.close();
    }

    /**
     * QFJ-357 This test should make sure that outside the Session time _only_ a
     * Logout message is sent to the counterparty. Formerly it could be observed
     * sometimes that there was a Logon message with a Logout message
     * immediately following.
     */
    @Test
    public void testLogonOutsideSessionTimeIsRejected() throws Exception {

        final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        final Date now = new Date();
        final MockSystemTimeSource systemTimeSource = new MockSystemTimeSource(now.getTime());
        SystemTime.setTimeSource(systemTimeSource);
        // set up some basic stuff
        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final SessionSettings settings = SessionSettingsTest.setUpSession(null);
        // construct a session schedule which is not active at the moment
        settings.setString("StartTime", dateFormat.format(now.getTime() + 1800000)); // add 30 minutes
        settings.setString("EndTime", dateFormat.format(now.getTime() + 3600000));
        settings.setString("TimeZone", TimeZone.getDefault().getID());
        setupFileStoreForQFJ357(sessionID, settings);

        // Session gets constructed, triggering a reset
        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpFileStoreSession(application, false, new UnitTestResponder(),
                settings, sessionID);
        final SessionState state = getSessionState(session);

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        logonTo(session);
        // we should only answer with a Logout message
        assertEquals(1, application.toAdminMessages.size());
        assertEquals(FixMessageTypes.LOGOUT, application.toAdminMessages.get(0).getHeader()
                .getString(FixTags.MSG_TYPE));
        assertFalse("Session should not be connected", session.isLoggedOn());
        assertTrue(application.toAdminMessages.get(0).getString(Text.TAG)
                .contains("Logon attempt not within session time"));
        // Normally, next() is called periodically; we only do it here to reset
        // the seqNums.
        // The seqNums should be reset because it was tried to establish a
        // connection
        // outside of the session schedule.
        session.next();
        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        session.close();
    }

    // QFJ-357
    private void setupFileStoreForQFJ357(final SessionID sessionID, final SessionSettings settings)
            throws ConfigError, FieldConvertError, FileNotFoundException, IOException {

        // construct the path to the filestore (mostly c&p from FileStore class)
        settings.setString(FileStoreFactory.SETTING_FILE_STORE_PATH,
                System.getProperty("java.io.tmpdir"));
        final String path = settings.getString(FileStoreFactory.SETTING_FILE_STORE_PATH);
        final String fullPath = new File(path == null ? "." : path).getAbsolutePath();
        final String sessionName = FileUtil.sessionIdFileName(sessionID);
        final String prefix = FileUtil.fileAppendPath(fullPath, sessionName + ".");
        final String sessionFileName = prefix + "session";
        final DataOutputStream sessionTimeOutput = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(sessionFileName, false)));
        try {
            // removing the file does NOT trigger the reset in the Session
            // constructor, so we fake an outdated session
            sessionTimeOutput.writeUTF(UtcTimestampConverter.convert(new Date(0), true));
        } finally {
            sessionTimeOutput.close();
        }

        // delete files to have the message store reset seqNums to 1
        // (on QF/J 1.5.1 this triggered the needReset() method to return false)
        final String msgFileName = prefix + "body";
        final String headerFileName = prefix + "header";
        final String senderSeqNumFileName = prefix + "senderseqnums";
        final String targetSeqNumFileName = prefix + "targetseqnums";

        String[] fileNamesToDelete = new String[] { msgFileName, headerFileName,
                senderSeqNumFileName, targetSeqNumFileName };

        for (String fileNameToDelete : fileNamesToDelete) {
            File fileToDelete = new File(fileNameToDelete);
            if (fileToDelete.exists() && !fileToDelete.delete()) {
                throw new RuntimeException("Error during cleanup : file " + fileNameToDelete
                        + " can't be deleted");
            }
        }
    }

    // QFJ-60
    @Test
    public void testRejectLogon() throws Exception {

        // Create application that rejects all logons
        final Application application = new UnitTestApplication() {

            @Override
            public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, RejectLogon {

                super.fromAdmin(message, sessionId);
                throw new RejectLogon("FOR TEST");
            }
        };

        final Session session = setUpSession(application, false, new UnitTestResponder());
        final SessionState state = getSessionState(session);

        assertEquals(1, state.getNextSenderMsgSeqNum());
        assertEquals(1, state.getNextTargetMsgSeqNum());

        logonTo(session);

        assertEquals(false, state.isLogonSent());
        assertEquals(false, state.isLogonReceived());
        assertEquals(false, state.isLogonAlreadySent());
        assertEquals(false, state.isLogonSendNeeded());
        assertEquals(false, state.isLogonTimedOut());
        assertEquals(false, state.isLogoutSent());
        assertEquals(false, state.isLogoutReceived());
        assertEquals(false, state.isLogoutTimedOut());

        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(2, state.getNextTargetMsgSeqNum());

        session.close();
    }

    // QFJ-696
    @Test
    public void testRejectLogonWithSessionStatus() throws Exception {

        // Create application that rejects all logons
        Application application = new UnitTestApplication() {

            @Override
            public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, RejectLogon {

                super.fromAdmin(message, sessionId);
                throw new RejectLogon("FOR TEST", SessionStatus.SESSION_ACTIVE.getValue());
            }
        };

        logonTo(setUpSession(application, false, new UnitTestResponder()));
        assertEquals((int) SessionStatus.SESSION_ACTIVE.getValue(),
                ((UnitTestApplication) application).lastToAdminMessage().getInt(SessionStatus.TAG));

        application = new UnitTestApplication() {

            @Override
            public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, RejectLogon {

                super.fromAdmin(message, sessionId);
                throw new RejectLogon("FOR TEST", -1);
            }
        };

        logonTo(setUpSession(application, false, new UnitTestResponder()));
        assertFalse(((UnitTestApplication) application).lastToAdminMessage().isFieldSet(
                FixTags.SESSION_STATUS));
    }

    @Test
    // QFJ-339
    public void testSendingTimeRejectBeforeLogon() throws Exception {

        final Session session = setUpSession(new UnitTestApplication(), false,
                new UnitTestResponder());

        final Message message = new Logon();
        message.getHeader().setString(FixTags.SENDER_COMP_ID, "SENDER");
        message.getHeader().setString(FixTags.TARGET_COMP_ID, "TARGET");
        message.getHeader().setString(FixTags.SENDING_TIME,
                UtcTimestampConverter.convert(new Date(0), false));
        message.getHeader().setInt(FixTags.MSG_SEQ_NUM, 1);

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        message.getHeader().setField(MsgType.LOGON);

        final SessionStateListener mockStateListener = mock(SessionStateListener.class);
        session.addStateListener(mockStateListener);

        session.next(message);

        verify(mockStateListener).onDisconnect();
        verifyNoMoreInteractions(mockStateListener);

        session.close();
    }

    @Test
    // QFJ-339
    public void testCorruptLogonReject() throws Exception {

        final Session session = setUpSession(new UnitTestApplication(), false,
                new UnitTestResponder());

        final Message message = new Logon();
        message.getHeader().setString(FixTags.SENDER_COMP_ID, "SENDER");
        message.getHeader().setString(FixTags.TARGET_COMP_ID, "TARGET");
        message.getHeader().setString(FixTags.SENDING_TIME,
                UtcTimestampConverter.convert(new Date(), false));
        message.getHeader().setInt(FixTags.MSG_SEQ_NUM, 100);

        // Added - TODO these should be acquired via a MessageBuilder
        message.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        message.getHeader().setField(MsgType.LOGON);

        final SessionStateListener mockStateListener = mock(SessionStateListener.class);
        session.addStateListener(mockStateListener);

        session.next(message);

        verify(mockStateListener).onDisconnect();
        verifyNoMoreInteractions(mockStateListener);

        session.close();
    }

    @Test
    public void testSequenceRollbackOnCallbackException() throws Exception {

        // Create application that rejects all logons
        final Application application = new UnitTestApplication() {

            @Override
            public void fromApp(Message message, SessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

                super.fromApp(message, sessionId);
                throw new RuntimeException("TEST");
            }

            @Override
            public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, RejectLogon {

                super.fromAdmin(message, sessionId);
                if (message.getHeader().getString(FixTags.MSG_TYPE).equals(FixMessageTypes.LOGON)) {
                    return;
                }
                throw new RuntimeException("TEST");
            }
        };

        final Session session = setUpSession(application, false, new UnitTestResponder());
        final SessionState state = getSessionState(session);

        logonTo(session);
        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(2, state.getNextTargetMsgSeqNum());

        processMessage(session, createAppMessage(2));

        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(2, state.getNextTargetMsgSeqNum());

        // To avoid resendRequest
        state.setNextTargetMsgSeqNum(3);

        processMessage(session, createAdminMessage(3));

        assertEquals(2, state.getNextSenderMsgSeqNum());
        assertEquals(3, state.getNextTargetMsgSeqNum());

        session.close();
    }

    // QFJ-271
    @Test
    public void testSequenceResetStackOverflow() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpSession(application, false, new UnitTestResponder());
        final SessionState state = getSessionState(session);

        logonTo(session, 1);

        assertTrue(session.isLoggedOn());
        assertEquals(2, state.getNextTargetMsgSeqNum());

        for (int i = 2; i <= 41; i++) {
            processMessage(session, createAppMessage(i));
        }
        assertEquals(42, state.getNextTargetMsgSeqNum());

        processMessage(session, createAppMessage(50));
        processMessage(session, createSequenceReset(51, 51, true));

        for (int i = 42; i <= 49; i++) {
            processMessage(session, createAppMessage(i));
        }

        assertEquals(51, state.getNextTargetMsgSeqNum());
        processMessage(session, createHeartbeatMessage(51));
        assertEquals(52, state.getNextTargetMsgSeqNum());
        assertTrue(session.isLoggedOn());
        assertFalse(state.isResendRequested());
        assertTrue(state.getQueuedSeqNums().isEmpty());

        session.close();
    }

    // QFJ-626
    @Test
    public void testResendMessagesWithIncorrectChecksum() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final Session session = SessionFactoryTestSupport.createSession(sessionID, application,
                false, false, true, true, null);
        UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);
        final SessionState state = getSessionState(session);

        assertTrue(session.isUsingDataDictionary());

        final Logon logonToSend = new Logon();
        setUpHeader(session.getSessionID(), logonToSend, true, 1);
        logonToSend.setInt(FixTags.HEART_BT_INT, 30);
        logonToSend.setEncryptMethod(EncryptMethod.NONE_OTHER);
        logonToSend.toString(); // calculate length/checksum

        // Added - TODO these should be acquired via a MessageBuilder
        logonToSend.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        logonToSend.getHeader().setField(MsgType.LOGON);

        session.next(logonToSend);

        session.send(createAppMessage(2));
        final News createAppMessage = createAppMessage(3);
        createAppMessage.setString(11, "ÄÖÜäöü?ß");
        session.send(createAppMessage);
        session.send(createAppMessage(4));
        session.send(createAppMessage(5));

        // ugly hack: alter the store to get an invalid checksum
        String toString = createAppMessage.toString();
        final String replace = toString.replace("10=", "10=1");
        state.set(3, replace);

        Message createResendRequest = createResendRequest(2, 1);
        createResendRequest.toString(); // calculate length/checksum
        processMessage(session, createResendRequest);

        Message createAdminMessage = createAdminMessage(3);
        createAdminMessage.toString(); // calculate length/checksum
        session.next(createAdminMessage);

        // all messages should have been resent
        assertEquals(5, application.lastToAppMessage().getHeader().getInt(FixTags.MSG_SEQ_NUM));
        assertFalse(state.isResendRequested());

        session.close();
    }

    // QFJ-493
    @Test
    public void testGapFillSatisfiesResendRequest() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpSession(application, false, new UnitTestResponder());
        final SessionState state = getSessionState(session);

        session.setNextTargetMsgSeqNum(684);
        logonTo(session, 687);

        assertTrue(state.isResendRequested());
        assertEquals(684, state.getNextTargetMsgSeqNum());
        processMessage(session, createResendRequest(688, 1));

        processMessage(session, createSequenceReset(684, 688, true));

        processMessage(session, createHeartbeatMessage(689));

        assertFalse(state.isResendRequested());

        session.close();
    }

    // QFJ-673
    @Test
    public void testResendRequestIsProcessedAndQueued() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpSession(application, false, new UnitTestResponder());
        final SessionState state = getSessionState(session);

        session.setNextSenderMsgSeqNum(1006);
        logonTo(session, 6);

        assertTrue(state.isResendRequested());
        assertEquals(1, state.getNextTargetMsgSeqNum());
        processMessage(session, createResendRequest(7, 1005));
        assertEquals(1, state.getNextTargetMsgSeqNum());
        processMessage(session, createSequenceReset(1, 6, true));
        assertEquals(8, state.getNextTargetMsgSeqNum());
        // we need to satisfy the resendrequest of the opposing side
        assertTrue(FixMessageTypes.SEQUENCE_RESET.equals(MessageUtils.getMessageType(application
                .lastToAdminMessage().toString())));
        assertTrue(state.isResendRequested());
        processMessage(session, createHeartbeatMessage(8));
        assertFalse(state.isResendRequested());
        processMessage(session, createHeartbeatMessage(9));
        assertFalse(state.isResendRequested());
        assertTrue(session.isLoggedOn());
        assertEquals(10, state.getNextTargetMsgSeqNum());

        session.close();
    }

    // QFJ-658
    @Test
    public void testResendRequestMsgSeqNum() throws Exception {

        // test seqnum too low
        final Application application = new UnitTestApplication() {
        };
        Session session = setUpSession(application, false, new UnitTestResponder());
        SessionState state = getSessionState(session);

        assertEquals(1, state.getNextTargetMsgSeqNum());
        logonTo(session, 1);
        assertEquals(2, state.getNextTargetMsgSeqNum());
        processMessage(session, createResendRequest(1, 100));
        assertFalse("Session should be logged out since seqnum too low!", session.isLoggedOn());

        // test seqnum too high
        session = setUpSession(application, false, new UnitTestResponder());
        state = getSessionState(session);

        assertEquals(1, state.getNextTargetMsgSeqNum());
        logonTo(session, 1);
        assertEquals(2, state.getNextTargetMsgSeqNum());

        assertFalse(state.isResendRequested());
        processMessage(session, createHeartbeatMessage(8));
        assertTrue(state.isResendRequested());
        processMessage(session, createResendRequest(10, 100));

        // satisfy ResendRequest
        processMessage(session, createSequenceReset(2, 11, true));
        assertEquals(11, state.getNextTargetMsgSeqNum());
        processMessage(session, createHeartbeatMessage(11));
        assertFalse(state.isResendRequested());
        processMessage(session, createHeartbeatMessage(12));
        assertTrue(session.isLoggedOn());

        session.close();
    }

    // QFJ-658 and acceptance test 20_SimultaneousResendRequests.def
    @Test
    public void testSimultaneousResendRequests() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        Session session = setUpSession(application, false, new UnitTestResponder());
        SessionState state = getSessionState(session);

        assertEquals(1, state.getNextTargetMsgSeqNum());
        logonTo(session, 1);
        assertEquals(2, state.getNextTargetMsgSeqNum());
        assertFalse(state.isResendRequested());
        assertTrue(session.isLoggedOn());

        processMessage(session, createAppMessage(2));
        session.send(createAppMessage(2));
        assertFalse(state.isResendRequested());
        assertTrue(session.isLoggedOn());

        processMessage(session, createAppMessage(3));
        session.send(createAppMessage(3));
        assertFalse(state.isResendRequested());
        assertTrue(session.isLoggedOn());

        processMessage(session, createHeartbeatMessage(7));
        assertTrue(state.isResendRequested());
        assertTrue(session.isLoggedOn());
        processMessage(session, createResendRequest(8, 2));
        assertTrue(state.isResendRequested());
        assertTrue(session.isLoggedOn());

        processMessage(session, createHeartbeatMessage(4));
        assertTrue(state.isResendRequested());
        processMessage(session, createHeartbeatMessage(5));
        assertTrue(state.isResendRequested());
        processMessage(session, createHeartbeatMessage(6));
        assertFalse(state.isResendRequested());
        assertTrue(session.isLoggedOn());

        // we need to satisfy the resendrequest of the opposing side
        assertTrue(FixMessageTypes.SEQUENCE_RESET.equals(MessageUtils.getMessageType(application
                .lastToAdminMessage().toString())));
        assertEquals(9, state.getNextTargetMsgSeqNum());
        processMessage(session, createHeartbeatMessage(9));
        processMessage(session, createHeartbeatMessage(10));
        assertEquals(11, state.getNextTargetMsgSeqNum());

        session.close();
    }

    // QFJ-750
    @Test
    public void testRemoveQueuedMessagesOnSequenceReset() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpSession(application, false, new UnitTestResponder());
        final SessionState state = getSessionState(session);

        final int from = 10;
        int numberOfMsgs = 200;
        int to = from + numberOfMsgs;

        logonTo(session, 1);
        assertEquals(2, state.getNextTargetMsgSeqNum());
        for (int i = from; i < to; i++) {
            processMessage(session, createAppMessage(i));
        }
        for (int i = from; i < to; i++) {
            assertTrue(state.getQueuedSeqNums().contains(i));
        }

        assertTrue(state.getQueuedSeqNums().size() == numberOfMsgs);
        assertTrue(application.fromAppMessages.isEmpty());
        // Create a sequence reset which will cause deletion of almost all
        // messages
        // from the sessionState queue since former messages are skipped.
        // The remaining two messages will then be dequeued and processed by the
        // app.
        final int two = 2;
        processMessage(session, createSequenceReset(2, to - two, true));
        assertTrue(application.fromAppMessages.size() == two);
        assertFalse(state.isResendRequested());
        assertTrue(session.isLoggedOn());
        assertTrue(state.getQueuedSeqNums().isEmpty());

        session.close();
    }

    /**
     * QFJ-721: Receiving a non-Logon message after having sent a Logon on a
     * non-FIXT session formerly lead to a NPE since the field
     * targetDefaultApplVerID was not initialized in all cases.
     */
    @Test
    public void testNonLogonMessageNonFIXT() throws Exception {

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final UnitTestApplication application = new UnitTestApplication();
        final Session session = SessionFactoryTestSupport.createSession(sessionID, application,
                true, false, true, true, null);
        session.setResponder(new UnitTestResponder());

        assertTrue(session.isUsingDataDictionary());
        assertEquals(FIXApplication.FIX44, session.getTargetDefaultApplicationVersionID());
        session.next();
        session.next();
        Message createHeartbeatMessage = createHeartbeatMessage(1);
        createHeartbeatMessage.toString(); // calculate checksum, length
        processMessage(session, createHeartbeatMessage);
        assertEquals(FIXApplication.FIX44, session.getTargetDefaultApplicationVersionID());
        assertFalse(session.isLoggedOn());

        session.close();
    }

    /**
     * QFJ-721: For FIXT sessions the targetDefaultApplVerID should have been
     * set after the Logon.
     */
    @Test
    public void testNonLogonMessageFIXT() throws Exception {

        final SessionID sessionID = new SessionID(FIXBeginString.FIXT11, "SENDER", "TARGET");
        final UnitTestApplication application = new UnitTestApplication();
        final Session session = SessionFactoryTestSupport.createSession(sessionID, application,
                true, false, true, true, FIXApplication.FIX50SP2);
        session.setResponder(new UnitTestResponder());

        // construct example messages
        final quickfix.fixt11.Heartbeat heartbeat = new quickfix.fixt11.Heartbeat();
        setUpHeader(session.getSessionID(), heartbeat, true, 1);

        // Added - TODO these should be acquired via a MessageBuilder
        heartbeat.getHeader().setField(new BeginString(FIXBeginString.FIXT11.getValue()));
        heartbeat.getHeader().setField(quickfix.fixt11.field.MsgType.HEARTBEAT);
        heartbeat.toString(); // calculate checksum, length

        final quickfix.fixt11.Logon logon = new quickfix.fixt11.Logon();
        setUpHeader(session.getSessionID(), logon, true, 1);
        logon.setInt(FixTags.HEART_BT_INT, 30);
        logon.setEncryptMethod(quickfix.fixt11.field.EncryptMethod.NONE_OTHER);
        logon.setField(new DefaultApplVerID(ApplVerID.FIX50SP2.getCharacters()));

        // Added - TODO these should be acquired via a MessageBuilder
        logon.getHeader().setField(new BeginString(FIXBeginString.FIXT11.getValue()));
        logon.getHeader().setField(quickfix.fixt11.field.MsgType.LOGON);

        logon.toString(); // calculate checksum, length

        assertTrue(session.isUsingDataDictionary());
        assertNull(session.getTargetDefaultApplicationVersionID());
        session.next();
        session.next();
        session.next(heartbeat);
        assertNull(session.getTargetDefaultApplicationVersionID());
        assertFalse(session.isLoggedOn());

        // retry Logon
        session.setResponder(new UnitTestResponder());
        session.next();
        session.next();
        assertNull(session.getTargetDefaultApplicationVersionID());
        session.next(logon);
        assertEquals(FIXApplication.FIX50SP2, session.getTargetDefaultApplicationVersionID());
        assertTrue(session.isLoggedOn());

        session.close();
    }

    private void processMessage(Session session, Message message) {

        try {
            session.next(message);
        } catch (final Throwable e) {
            // This simulated the behavior of the QFJ connectors
            // They will just discard a message with an error (without
            // incrementing the sequence number).
        }
    }

    private Message createAdminMessage(int sequence) {

        final TestRequest msg = new TestRequest(new TestReqID("ID"));
        msg.getHeader().setString(FixTags.SENDER_COMP_ID, "TARGET");
        msg.getHeader().setString(FixTags.TARGET_COMP_ID, "SENDER");
        msg.getHeader().setInt(FixTags.MSG_SEQ_NUM, sequence);
        msg.getHeader().setUtcTimeStamp(FixTags.SENDING_TIME, new Date());

        // Added - TODO these should be acquired via a MessageBuilder
        msg.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        msg.getHeader().setField(MsgType.TEST_REQUEST);

        return msg;
    }

    private Message createHeartbeatMessage(int sequence) {

        final Heartbeat msg = new Heartbeat();
        msg.getHeader().setString(FixTags.SENDER_COMP_ID, "TARGET");
        msg.getHeader().setString(FixTags.TARGET_COMP_ID, "SENDER");
        msg.getHeader().setInt(FixTags.MSG_SEQ_NUM, sequence);
        msg.getHeader().setUtcTimeStamp(FixTags.SENDING_TIME, new Date());

        // Added - TODO these should be acquired via a MessageBuilder
        msg.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        msg.getHeader().setField(MsgType.HEARTBEAT);

        return msg;
    }

    private Message createReject(int sequence, int refSeqNum) {

        final Reject msg = new Reject();
        msg.getHeader().setString(FixTags.SENDER_COMP_ID, "TARGET");
        msg.getHeader().setString(FixTags.TARGET_COMP_ID, "SENDER");
        msg.getHeader().setInt(FixTags.MSG_SEQ_NUM, sequence);
        msg.getHeader().setUtcTimeStamp(FixTags.SENDING_TIME, new Date());
        msg.setInt(FixTags.REF_SEQ_NUM, refSeqNum);

        // Added - TODO these should be acquired via a MessageBuilder
        msg.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        msg.getHeader().setField(MsgType.REJECT);

        return msg;
    }

    private Message createResendRequest(int sequence, int from) {

        final ResendRequest msg = new ResendRequest();
        msg.getHeader().setString(FixTags.SENDER_COMP_ID, "TARGET");
        msg.getHeader().setString(FixTags.TARGET_COMP_ID, "SENDER");
        msg.getHeader().setInt(FixTags.MSG_SEQ_NUM, sequence);
        msg.getHeader().setUtcTimeStamp(FixTags.SENDING_TIME, new Date());
        msg.setInt(FixTags.BEGIN_SEQ_NO, from);
        msg.setInt(FixTags.END_SEQ_NO, 0);

        // Added - TODO these should be acquired via a MessageBuilder
        msg.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        msg.getHeader().setField(MsgType.RESEND_REQUEST);

        return msg;
    }

    private Message createSequenceReset(int sequence, int to, boolean gapFill) {

        final SequenceReset msg = new SequenceReset();
        msg.getHeader().setString(FixTags.SENDER_COMP_ID, "TARGET");
        msg.getHeader().setString(FixTags.TARGET_COMP_ID, "SENDER");
        msg.getHeader().setInt(FixTags.MSG_SEQ_NUM, sequence);
        msg.getHeader().setUtcTimeStamp(FixTags.SENDING_TIME, new Date());
        msg.getHeader().setBoolean(FixTags.POSS_DUP_FLAG, true);
        msg.getHeader().setUtcTimeStamp(FixTags.ORIG_SENDING_TIME, new Date());
        msg.setBoolean(FixTags.GAP_FILL_FLAG, gapFill);
        msg.setInt(FixTags.NEW_SEQ_NO, to);

        // Added - TODO these should be acquired via a MessageBuilder
        msg.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        msg.getHeader().setField(MsgType.SEQUENCE_RESET);

        return msg;
    }

    @Test
    public void testDontCatchErrorsFromCallback() throws Exception {

        final Application application = new UnitTestApplication() {

            @Override
            public void fromApp(Message message, SessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

                super.fromApp(message, sessionId);
                throw new Error("TEST");
            }
        };

        final Session session = setUpSession(application, false, new UnitTestResponder());
        logonTo(session);

        try {
            session.next(createHeartbeatMessage(2)); // should increment target
                                                     // seqnum
            session.next(createHeartbeatMessage(3)); // should increment target
                                                     // seqnum
            session.next(createHeartbeatMessage(4)); // should increment target
                                                     // seqnum
            assertEquals(5, session.getExpectedTargetNum());
            session.next(createAppMessage(5)); // should NOT increment target
                                               // seqnum
            fail("No error thrown");
        } catch (final Throwable t) {
            assertEquals("java.lang.Error: TEST", t.getMessage());
            assertEquals(5, session.getExpectedTargetNum());
            assertEquals(2, session.getExpectedSenderNum());
            session.next(createHeartbeatMessage(5)); // should increment target
                                                     // seqnum
            assertEquals(6, session.getExpectedTargetNum());
            assertEquals(2, session.getExpectedSenderNum());
        }

        session.close();
    }

    // QFJ-572
    @Test
    public void testCatchErrorsFromCallbackAndSendReject() throws Exception {

        final UnitTestApplication application = new UnitTestApplication() {

            @Override
            public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, RejectLogon {

                super.fromAdmin(message, sessionId);
                final String msgType = message.getHeader().getString(FixTags.MSG_TYPE);
                if (FixMessageTypes.HEARTBEAT.equals(msgType)) {
                    throw new Error("TESTAdmin");
                }
            }

            @Override
            public void fromApp(Message message, SessionID sessionId) throws FieldNotFound,
                    IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

                super.fromApp(message, sessionId);
                throw new Error("TEST");
            }
        };

        final Session session = setUpSession(application, false, new UnitTestResponder());
        session.setRejectMessageOnUnhandledException(true);
        logonTo(session);

        try {
            session.next(createAppMessage(2));
            assertEquals(3, session.getExpectedTargetNum());
            assertEquals(3, session.getExpectedSenderNum());
            assertEquals("B",
                    application.lastFromAppMessage().getHeader().getString(FixTags.MSG_TYPE));
            assertEquals(FixMessageTypes.BUSINESS_MESSAGE_REJECT, application.lastToAppMessage()
                    .getHeader().getString(FixTags.MSG_TYPE));

            session.next(createHeartbeatMessage(3));
            assertEquals(4, session.getExpectedTargetNum());
            assertEquals(4, session.getExpectedSenderNum());
            assertEquals(FixMessageTypes.HEARTBEAT, application.lastFromAdminMessage().getHeader()
                    .getString(FixTags.MSG_TYPE));
            assertEquals(FixMessageTypes.REJECT, application.lastToAdminMessage().getHeader()
                    .getString(FixTags.MSG_TYPE));

            session.next(createAdminMessage(4));
            assertEquals(5, session.getExpectedTargetNum());
            assertEquals(5, session.getExpectedSenderNum());
            assertEquals(FixMessageTypes.TEST_REQUEST, application.lastFromAdminMessage()
                    .getHeader().getString(FixTags.MSG_TYPE));
            assertEquals(FixMessageTypes.HEARTBEAT, application.lastToAdminMessage().getHeader()
                    .getString(FixTags.MSG_TYPE));
        } catch (final Throwable t) {
            fail("Error was thrown: " + t.getMessage());
        }

        session.close();
    }

    private News createAppMessage(int sequence) {

        final News news = new News(new Headline("Headline"));
        news.getHeader().setString(FixTags.SENDER_COMP_ID, "TARGET");
        news.getHeader().setString(FixTags.TARGET_COMP_ID, "SENDER");
        news.getHeader().setInt(FixTags.MSG_SEQ_NUM, sequence);
        news.getHeader().setUtcTimeStamp(FixTags.SENDING_TIME, new Date());

        // Added - TODO these should be acquired via a MessageBuilder
        news.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        news.getHeader().setField(MsgType.NEWS);

        return news;
    }

    private SessionState getSessionState(Session session) throws NoSuchFieldException,
            IllegalAccessException {

        final Field stateField = session.getClass().getDeclaredField("state");
        stateField.setAccessible(true);
        return (SessionState) stateField.get(session);
    }

    /**
     * Verifies that the session has been registered before the logger tries
     * accessing it Use case: JdbcLogger not setup correctly, barfs during
     * Session creation, tries to log and can't find the session in global
     * session list yet.
     */
    @Test
    public void testSessionRegisteredCorrectly() throws Exception {

        final SessionSettings settings = SessionSettingsTest.setUpSession(null);
        settings.setString(Session.SETTING_USE_DATA_DICTIONARY, "N");
        JdbcTestSupport.setHypersonicSettings(settings);
        // do not initialize the SQL tables so that the JdbcLog will fail
        final SessionID sessionID = new SessionID(FIXBeginString.FIX42, "SENDER-sessionRegister",
                "TARGET-sessionRegister");
        settings.setString(sessionID, "ConnectionType", "acceptor");
        final DefaultSessionFactory factory = new DefaultSessionFactory(new UnitTestApplication(),
                new MemoryStoreFactory(), new JdbcLogFactory(settings));
        try {
            final Session session = factory.create(sessionID, settings);
            assertNotNull(session);
            session.close();
        } catch (final NullPointerException nex) {
            fail("Session not registering correctly so JdbcLog fails while printing an error: "
                    + nex.getMessage());
        } catch (final NoClassDefFoundError e) {
            // this test does not work in Eclipse but we
            // catch the Exception in order to ignore the test
        }
    }

    @Test
    public void testNonpersistedGapFill() throws Exception {

        final SessionID sessionID = new SessionID("FIX.4.4:SENDER->TARGET");
        final Session session = SessionFactoryTestSupport.createNonpersistedSession(sessionID,
                new UnitTestApplication(), false);
        session.getStore().setNextTargetMsgSeqNum(200);
        final SessionState state = ReflectionUtil.getField(session, "state", SessionState.class);
        state.setLogonReceived(true);
        final ResendRequest resendRequest = new ResendRequest();
        resendRequest.getHeader().setField(new SenderCompID(sessionID.getTargetCompID()));
        resendRequest.getHeader().setField(new TargetCompID(sessionID.getSenderCompID()));
        resendRequest.getHeader().setField(new SendingTime(new Date()));
        resendRequest.getHeader().setField(new MsgSeqNum(200));
        resendRequest.setBeginSeqNo(new quickfix.fix44.field.BeginSeqNo(1));
        resendRequest.setEndSeqNo(new quickfix.fix44.field.EndSeqNo(100));

        // Added - TODO these should be acquired via a MessageBuilder
        resendRequest.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        resendRequest.getHeader().setField(MsgType.RESEND_REQUEST);

        session.next(resendRequest);
        assertEquals(201, state.getNextTargetMsgSeqNum());
        session.close();
    }

    @Test
    // QFJ-457
    public void testAcceptorRelogon() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpSession(application, false, new UnitTestResponder());

        logonTo(session);
        assertTrue(session.isEnabled());
        assertTrue(session.isLoggedOn());

        session.logout();
        session.next();

        final Message logout = new Logout();
        logout.getHeader().setString(FixTags.SENDER_COMP_ID, "TARGET");
        logout.getHeader().setString(FixTags.TARGET_COMP_ID, "SENDER");
        logout.getHeader().setString(FixTags.SENDING_TIME,
                UtcTimestampConverter.convert(new Date(), false));
        logout.getHeader().setInt(FixTags.MSG_SEQ_NUM, 2);

        // Added - TODO these should be acquired via a MessageBuilder
        logout.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        logout.getHeader().setField(MsgType.LOGOUT);

        session.next(logout);

        // session.reset();
        assertFalse(session.isLoggedOn());
        logonTo(session, 3);
        Message lastToAdminMessage = application.lastToAdminMessage();
        assertFalse(Logout.MSGTYPE.equals(lastToAdminMessage.getHeader()
                .getString(FixTags.MSG_TYPE)));
        session.close();
    }

    @Test
    // QFJ-444
    public void testStateFlagsAreResetOnLogout() throws Exception {

        final UnitTestApplication application = new UnitTestApplication();
        final Session session = setUpSession(application, false, new UnitTestResponder());
        final Message logout = new Logout();
        logout.getHeader().setString(FixTags.SENDER_COMP_ID, "TARGET");
        logout.getHeader().setString(FixTags.TARGET_COMP_ID, "SENDER");
        logout.getHeader().setString(FixTags.SENDING_TIME,
                UtcTimestampConverter.convert(new Date(), false));
        logout.getHeader().setInt(FixTags.MSG_SEQ_NUM, 2);

        // Added - TODO these should be acquired via a MessageBuilder
        logout.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        logout.getHeader().setField(MsgType.LOGOUT);

        logonTo(session);
        assertFalse(session.isLogoutSent());
        assertFalse(session.isLogoutReceived());
        assertTrue(session.isLogonReceived());
        assertTrue(session.isLogonSent());

        /*
         * Setting the responder to NULL here was formerly causing that the
         * flags logoutReceived and logoutSent (amongst others) were not reset
         * to false because the Session.disconnect() method returned too early
         * since no responder was set anymore.
         */
        session.setResponder(null);
        session.next(logout);

        assertFalse(session.isLogoutReceived());
        assertFalse(session.isLogoutSent());
        assertFalse(session.isLogonReceived());
        assertFalse(session.isLogonSent());

        session.setResponder(new UnitTestResponder());
        logonTo(session, 3);
        assertFalse(session.isLogoutSent());
        assertFalse(session.isLogoutReceived());
        assertTrue(session.isLogonReceived());
        assertTrue(session.isLogonSent());

        session.disconnect("Forced by UnitTest", true);
        assertFalse(session.isLogoutReceived());
        assertFalse(session.isLogoutSent());
        assertFalse(session.isLogonReceived());
        assertFalse(session.isLogonSent());

        // onLogout was called
        assertTrue(application.logoutSessions.size() == 1);

        session.close();
    }

    @Test
    // QFJ-557
    public void testGenerateRejectAndTargetSeqNum() throws Exception {

        SessionID sessionID = new SessionID(FIXBeginString.FIX42, "US", "THEM");
        SessionSettings settings = new SessionSettings();
        settings.setString(SessionFactory.SETTING_CONNECTION_TYPE,
                SessionFactory.INITIATOR_CONNECTION_TYPE);
        settings.setLong(Session.SETTING_HEARTBTINT, 30L);
        settings.setString(Session.SETTING_TIMEZONE, "EST");
        settings.setString(Session.SETTING_START_TIME, "00:00:00");
        settings.setString(Session.SETTING_END_TIME, "00:00:00");
        settings.setBool(Session.SETTING_CHECK_LATENCY, false);

        Session session = new DefaultSessionFactory(new ApplicationAdapter(),
                new MemoryStoreFactory(), new ScreenLogFactory(settings)).create(sessionID,
                settings);

        session.setResponder(new UnitTestResponder());

        session.setNextSenderMsgSeqNum(177);
        session.setNextTargetMsgSeqNum(223);
        session.next();
        String[] messages = {
                "8=FIX.4.2\0019=0081\00135=A\00149=THEM\00156=US\001369=177\00152=20100908-17:59:30.551\00134=227\00198=0\001108=30\00110=36\001",
                "8=FIX.4.2\0019=0107\00135=z\001115=THEM\00149=THEM\00156=US\001369=177\00152=20100908-17:59:30.551\00134=228\001336=1\001340=2\00176=US\001439=USS\00110=133\001",
                "8=FIX.4.2\0019=0113\00135=4\00134=223\00143=Y\001122=20100908-17:59:30.642\00149=THEM\00156=US\001369=178\00152=20100908-17:59:30.642\001123=Y\00136=225\00110=110\001",
                "8=FIX.4.2\0019=0246\00135=8\001115=THEM\00134=225\00143=Y\001122=20100908-17:52:37.920\00149=THEM\00156=US\001369=178\00152=20100908-17:59:30.642\00137=10118506\00111=a00000052.1\00117=17537743\00120=0\001150=4\00139=4\00155=ETFC\00154=1\00138=500000\00144=0.998\00132=0\00131=0\001151=0\00114=0\0016=0\00160=20100908-17:52:37.920\00110=80\001" };
        for (String message : messages)
            session.next(MessageUtils.parse(session, message));

        assertEquals(226, session.getStore().getNextTargetMsgSeqNum());

        session.close();
    }

    @Test
    // QFJ-776
    public void testLogonWithoutTargetCompID() throws Exception {

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        UnitTestApplication application = new UnitTestApplication();
        SessionSettings sessionSettings = SessionSettingsTest.setUpSession(null);
        SessionFactoryTestSupport factoryTestSupport = new SessionFactoryTestSupport();
        Session session = factoryTestSupport.create(sessionID, sessionSettings);
        UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);
        session.logon();
        assertTrue(session.getCheckCompID());

        session.next();
        Logon logonRequest = new Logon();
        setUpHeader(session.getSessionID(), logonRequest, true, 1);
        logonRequest.setInt(FixTags.HEART_BT_INT, 30);
        logonRequest.getHeader().removeField(FixTags.TARGET_COMP_ID);

        // Added - TODO these should be acquired via a MessageBuilder
        logonRequest.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        logonRequest.getHeader().setField(MsgType.LOGON);

        session.next(logonRequest);
        // session should not be logged on due to missing TargetCompID
        assertFalse(session.isLoggedOn());

        // create session which has checkCompID=N
        session = createSession(sessionID, application, false, true);
        session.setResponder(responder);
        session.logon();
        assertFalse(session.getCheckCompID());
        logonRequest = new Logon();
        setUpHeader(session.getSessionID(), logonRequest, true, 1);
        logonRequest.setInt(FixTags.HEART_BT_INT, 30);
        logonRequest.getHeader().removeField(FixTags.TARGET_COMP_ID);

        // Added - TODO these should be acquired via a MessageBuilder
        logonRequest.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        logonRequest.getHeader().setField(MsgType.LOGON);

        session.next(logonRequest);
        assertTrue(session.isLoggedOn());

        assertEquals(1, application.lastToAdminMessage().getHeader().getInt(FixTags.MSG_SEQ_NUM));
        assertEquals(2, session.getStore().getNextTargetMsgSeqNum());
        assertEquals(2, session.getStore().getNextSenderMsgSeqNum());

        session.close();
    }

    // QFJ-751
    @Test
    public void testSequenceResetGapFillWithZeroChunkSize() throws Exception {

        testSequenceResetGapFillWithChunkSize(0);
    }

    // QFJ-751
    @Test
    public void testSequenceResetGapFillWithChunkSize5() throws Exception {

        testSequenceResetGapFillWithChunkSize(5);
    }

    // QFJ-751
    @Test
    public void testSequenceResetGapFillWithChunkSize8() throws Exception {

        testSequenceResetGapFillWithChunkSize(8);
    }

    // QFJ-751
    @Test
    public void testSequenceResetGapFillWithChunkSize10() throws Exception {

        testSequenceResetGapFillWithChunkSize(10);
    }

    // QFJ-751
    private void testSequenceResetGapFillWithChunkSize(int chunkSize) throws Exception {

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");

        boolean isInitiator = true, resetOnLogon = false, validateSequenceNumbers = true;

        Session session = new Session(new UnitTestApplication(), new MemoryStoreFactory(),
                sessionID, null, null, new ScreenLogFactory(true, true, true),
                MessageBuilderServiceLoader.getMessageBuilderFactory(), isInitiator ? 30 : 0,
                false, 30, true, resetOnLogon, false, false, false, false, false, true, false, 1.5,
                null, validateSequenceNumbers, new int[] { 5 }, false, false, false, true, false,
                true, false, null, true, chunkSize, false, false);

        UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);
        final SessionState state = getSessionState(session);

        session.logon();
        session.next();

        assertEquals(1, session.getStore().getNextTargetMsgSeqNum());

        Message logonRequest = new Message(responder.sentMessageData);

        // Deliver Logon response with too high sequence 20 instead of 1.
        session.next(createLogonResponse(sessionID, logonRequest, 20));

        assertTrue(state.isResendRequested());
        // The expected target sequence should still be 1.
        assertEquals(1, session.getStore().getNextTargetMsgSeqNum());

        // Deliver the missing message #1.
        session.next(createAppMessage(1));
        assertEquals(2, session.getStore().getNextTargetMsgSeqNum());

        // Deliver the missing message #2.
        session.next(createAppMessage(2));
        assertEquals(3, session.getStore().getNextTargetMsgSeqNum());

        // Deliver SequenceReset-GapFill from 3 to 5
        session.next(createSequenceReset(3, 5, true));

        // Deliver the missing message #5.
        session.next(createAppMessage(5));
        /*
         * The expected target sequence number should be 6 now.
         */
        assertEquals(6, session.getStore().getNextTargetMsgSeqNum());
        assertTrue(session.isLoggedOn());
        assertTrue(state.isResendRequested());
        for (int i = 6; i <= 19; i++) {
            session.next(createAppMessage(i));
        }
        assertFalse(state.isResendRequested());
        assertTrue(session.isLoggedOn());
        // seqnum 20 will be retrieved from the queue, so we should be at 21 now
        assertEquals(21, session.getStore().getNextTargetMsgSeqNum());

        session.close();
    }

    @Test
    // QFJ-795
    public void testMsgSeqNumTooHighWithDisconnectOnError() throws Exception {

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final boolean isInitiator = true, resetOnLogon = false, validateSequenceNumbers = true;

        final boolean disconnectOnError = true;

        Session session = new Session(new UnitTestApplication(), new MemoryStoreFactory(),
                sessionID, null, null, new ScreenLogFactory(true, true, true),
                MessageBuilderServiceLoader.getMessageBuilderFactory(), isInitiator ? 30 : 0,
                false, 30, true, resetOnLogon, false, false, false, false, false, true, false, 1.5,
                null, validateSequenceNumbers, new int[] { 5 }, false, disconnectOnError, false,
                true, false, true, false, null, true, 0, false, false);

        UnitTestResponder responder = new UnitTestResponder();
        session.setResponder(responder);

        session.logon();
        session.next();

        // Deliver Logon response with too high sequence number 100
        Message logonRequest = new Message(responder.sentMessageData);
        session.next(createLogonResponse(sessionID, logonRequest, 100));

        // Deliver application message with too high sequence number 101
        session.next(createAppMessage(101));
        // Check, if session is still connected.
        assertEquals(true, session.hasResponder());

        session.close();
    }

    private Session setUpSession(Application application, boolean isInitiator, Responder responder)
            throws NoSuchFieldException, IllegalAccessException {

        final SessionID sessionID = new SessionID(FIXBeginString.FIX44, "SENDER", "TARGET");
        final Session session = SessionFactoryTestSupport.createSession(sessionID, application,
                isInitiator);
        session.setResponder(responder);
        final SessionState state = getSessionState(session);
        assertEquals(isInitiator, state.isInitiator());
        assertEquals(false, state.isLogonSent());
        assertEquals(false, state.isLogonReceived());
        assertEquals(false, state.isLogonAlreadySent());
        assertEquals(isInitiator, state.isLogonSendNeeded());
        assertEquals(false, state.isLogonTimedOut());
        assertEquals(false, state.isLogoutSent());
        assertEquals(false, state.isLogoutReceived());
        assertEquals(false, state.isLogoutTimedOut());
        return session;
    }

    private Session setUpFileStoreSession(Application application, boolean isInitiator,
            Responder responder, SessionSettings settings, SessionID sessionID)
            throws NoSuchFieldException, IllegalAccessException, ConfigError, FieldConvertError {

        final SessionSchedule sessionSchedule = new SessionSchedule(settings, sessionID);
        final Session session = SessionFactoryTestSupport.createFileStoreSession(sessionID,
                application, isInitiator, settings, sessionSchedule);
        session.setResponder(responder);
        final SessionState state = getSessionState(session);
        assertEquals(isInitiator, state.isInitiator());
        assertEquals(false, state.isLogonSent());
        assertEquals(false, state.isLogonReceived());
        assertEquals(false, state.isLogonAlreadySent());
        assertEquals(isInitiator, state.isLogonSendNeeded());
        assertEquals(false, state.isLogonTimedOut());
        assertEquals(false, state.isLogoutSent());
        assertEquals(false, state.isLogoutReceived());
        assertEquals(false, state.isLogoutTimedOut());
        return session;
    }

    private Message createLogonResponse(final SessionID sessionID, final Message logonRequest,
            int responseSequenceNumber) throws FieldNotFound {

        final Message logonResponse = MessageBuilderServiceLoader.getMessageBuilderFactory()
                .getMessageBuilder(sessionID.getBeginString(), FixMessageTypes.LOGON).create();
        logonResponse.setInt(FixTags.ENCRYPT_METHOD, 0);
        logonResponse.setInt(FixTags.HEART_BT_INT, logonRequest.getInt(FixTags.HEART_BT_INT));
        final Message.Header header = logonResponse.getHeader();
        header.setString(FixTags.BEGIN_STRING, sessionID.getBeginString().getValue());
        header.setString(FixTags.SENDER_COMP_ID, sessionID.getSenderCompID());
        header.setString(FixTags.TARGET_COMP_ID, sessionID.getTargetCompID());
        header.setInt(FixTags.MSG_SEQ_NUM, responseSequenceNumber);
        header.setUtcTimeStamp(FixTags.SENDING_TIME, SystemTime.getDate(), true);
        return logonResponse;
    }

    private void logonTo(Session session) throws FieldNotFound, RejectLogon, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {

        logonTo(session, 1);
    }

    private void logonTo(Session session, int sequence) throws FieldNotFound, RejectLogon,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType, IOException,
            InvalidMessage {

        final Logon receivedLogon = new Logon();
        setUpHeader(session.getSessionID(), receivedLogon, true, sequence);
        receivedLogon.setInt(FixTags.HEART_BT_INT, 30);

        // Added - TODO these should be acquired via a MessageBuilder
        receivedLogon.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        receivedLogon.getHeader().setField(MsgType.LOGON);

        session.next(receivedLogon);
    }

    private void logoutFrom(Session session, int sequence) throws FieldNotFound, RejectLogon,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType, IOException,
            InvalidMessage {

        final Logout receivedLogout = new Logout();
        setUpHeader(session.getSessionID(), receivedLogout, true, sequence);

        // Added - TODO these should be acquired via a MessageBuilder
        receivedLogout.getHeader().setField(new BeginString(FIXBeginString.FIX44.getValue()));
        receivedLogout.getHeader().setField(MsgType.LOGOUT);

        session.next(receivedLogout);
    }

    private void setUpHeader(SessionID sessionID, Message message, boolean reversed, int sequence) {

        message.getHeader().setString(FixTags.TARGET_COMP_ID,
                reversed ? sessionID.getSenderCompID() : sessionID.getTargetCompID());
        message.getHeader().setString(FixTags.SENDER_COMP_ID,
                reversed ? sessionID.getTargetCompID() : sessionID.getSenderCompID());
        message.getHeader().setField(new SendingTime(SystemTime.getDate()));
        message.getHeader().setInt(FixTags.MSG_SEQ_NUM, sequence);
    }

    private class UnitTestResponder implements Responder {

        public String sentMessageData;

        public boolean disconnectCalled;

        @Override
        public boolean send(String data) {

            sentMessageData = data;
            return true;
        }

        @Override
        public String getRemoteAddress() {

            return null;
        }

        @Override
        public void disconnect() {

            disconnectCalled = true;
        }
    }

}