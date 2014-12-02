/*******************************************************************************
 * Copyright (c) quickfixj.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixj.org
 * license as defined by quickfixj.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixj.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package quickfix;

import static quickfix.LogUtil.logThrowable;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.MessageBuilder;
import org.quickfixj.MessageBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Message.Header;
import quickfix.SessionState.ResendRange;

/**
 * The Session is the primary FIX abstraction for message communication. It
 * performs sequencing and error recovery and represents a communication channel
 * to a counterparty. Sessions are independent of specific communication layer
 * connections. A Session is defined as starting with message sequence number of
 * 1 and ending when the session is reset. The Session could span many
 * sequential connections (it cannot operate on multiple connection
 * simultaneously).
 */
public class Session implements Closeable {

    /**
     * Session setting for heartbeat interval (in seconds).
     */
    public static final String SETTING_HEARTBTINT = "HeartBtInt";

    /**
     * Session setting for enabling message latency checks. Values are "Y" or
     * "N".
     */
    public static final String SETTING_CHECK_LATENCY = "CheckLatency";

    /**
     * If set to Y, messages must be received from the counterparty with the
     * correct SenderCompID and TargetCompID. Some systems will send you
     * different CompIDs by design, so you must set this to N.
     */
    public static final String SETTING_CHECK_COMP_ID = "CheckCompID";

    /**
     * Session setting for maximum message latency (in seconds).
     */
    public static final String SETTING_MAX_LATENCY = "MaxLatency";

    /**
     * Session setting for the test delay multiplier (0-1, as fraction of
     * Heartbeat interval)
     */
    public static final String SETTING_TEST_REQUEST_DELAY_MULTIPLIER = "TestRequestDelayMultiplier";

    /**
     * Session scheduling setting to specify that session never reset
     */
    public static final String SETTING_NON_STOP_SESSION = "NonStopSession";

    /**
     * Session scheduling setting to specify first day of trading week.
     */
    public static final String SETTING_START_DAY = "StartDay";

    /**
     * Session scheduling setting to specify last day of trading week.
     */
    public static final String SETTING_END_DAY = "EndDay";

    /**
     * Session scheduling setting to specify time zone for the session.
     */
    public static final String SETTING_TIMEZONE = "TimeZone";

    /**
     * Session scheduling setting to specify starting time of the trading day.
     */
    public static final String SETTING_START_TIME = "StartTime";

    /**
     * Session scheduling setting to specify end time of the trading day.
     */
    public static final String SETTING_END_TIME = "EndTime";

    /**
     * Session setting to indicate whether a data dictionary should be used. If
     * a data dictionary is not used then message validation is not possble.
     */
    public static final String SETTING_USE_DATA_DICTIONARY = "UseDataDictionary";

    /**
     * Session setting specifying the path to the data dictionary to use for
     * this session. This setting supports the possibility of a custom data
     * dictionary for each session. Normally, the default data dictionary for a
     * specific FIX version will be specified.
     */
    public static final String SETTING_DATA_DICTIONARY = "DataDictionary";

    /**
     * Session setting specifying the path to the transport data dictionary.
     * This setting supports the possibility of a custom transport data
     * dictionary for each session. This setting would only be used with FIXT
     * 1.1 and new transport protocols.
     */
    public static final String SETTING_TRANSPORT_DATA_DICTIONARY = "TransportDataDictionary";

    /**
     * Session setting specifying the path to the application data dictionary to
     * use for this session. This setting supports the possibility of a custom
     * application data dictionary for each session. This setting would only be
     * used with FIXT 1.1 and new transport protocols. This setting can be used
     * as a prefix to specify multiple application dictionaries for the FIXT
     * transport. For example:
     * 
     * <pre>
     * <code>
     * DefaultApplVerID=FIX.4.2
     * AppDataDictionary=FIX42.xml
     * AppDataDictionary.FIX.4.4=FIX44.xml
     * </code>
     * </pre>
     * 
     * This would use FIX42.xml for the default application version ID and
     * FIX44.xml for any FIX 4.4 messages.
     */
    public static final String SETTING_APP_DATA_DICTIONARY = "AppDataDictionary";

    /**
     * Default is "Y". If set to N, fields that are out of order (i.e. body
     * fields in the header, or header fields in the body) will not be rejected.
     */
    public static final String SETTING_VALIDATE_FIELDS_OUT_OF_ORDER = "ValidateFieldsOutOfOrder";

    /**
     * Session validation setting for enabling whether field ordering is
     * validated. Values are "Y" or "N". Default is "Y".
     */
    public static final String SETTING_VALIDATE_UNORDERED_GROUP_FIELDS = "ValidateUnorderedGroupFields";

    /**
     * Session validation setting for enabling whether field values are
     * validated. Empty fields values are not allowed. Values are "Y" or "N".
     * Default is "Y".
     */
    public static final String SETTING_VALIDATE_FIELDS_HAVE_VALUES = "ValidateFieldsHaveValues";

    /**
     * Allow to bypass the message validation. Default is "Y".
     */
    public static final String SETTING_VALIDATE_INCOMING_MESSAGE = "ValidateIncomingMessage";

    /**
     * Session setting for logon timeout (in seconds).
     */
    public static final String SETTING_LOGON_TIMEOUT = "LogonTimeout";

    /**
     * Session setting for logout timeout (in seconds).
     */
    public static final String SETTING_LOGOUT_TIMEOUT = "LogoutTimeout";

    /**
     * Session setting for doing an automatic sequence number reset on logout.
     * Valid values are "Y" or "N". Default is "N".
     */
    public static final String SETTING_RESET_ON_LOGOUT = "ResetOnLogout";

    /**
     * Check the next expected target SeqNum against the received SeqNum.
     * Default is "Y". If a mismatch is detected, apply the following logic:
     * <ul>
     * <li>if lower than expected SeqNum , logout</li>
     * <li>if higher, send a resend request</li>
     * </ul>
     */
    public static final String SETTING_VALIDATE_SEQUENCE_NUMBERS = "ValidateSequenceNumbers";

    /**
     * Session setting for doing an automatic sequence number reset on
     * disconnect. Valid values are "Y" or "N". Default is "N".
     */
    public static final String SETTING_RESET_ON_DISCONNECT = "ResetOnDisconnect";

    /**
     * Session setting for doing an automatic reset when an error occurs. Valid
     * values are "Y" or "N". Default is "N". A reset means disconnect, sequence
     * numbers reset, store cleaned and reconnect, as for a daily reset.
     */
    public static final String SETTING_RESET_ON_ERROR = "ResetOnError";

    /**
     * Session setting for doing an automatic disconnect when an error occurs.
     * Valid values are "Y" or "N". Default is "N".
     */
    public static final String SETTING_DISCONNECT_ON_ERROR = "DisconnectOnError";

    /**
     * Session setting to enable milliseconds in message timestamps. Valid
     * values are "Y" or "N". Default is "Y". Only valid for FIX version >= 4.2.
     */
    public static final String SETTING_MILLISECONDS_IN_TIMESTAMP = "MillisecondsInTimeStamp";

    /**
     * Controls validation of user-defined fields.
     */
    public static final String SETTING_VALIDATE_USER_DEFINED_FIELDS = "ValidateUserDefinedFields";

    /**
     * Session setting that causes the session to reset sequence numbers when
     * initiating a logon (>= FIX 4.2).
     */
    public static final String SETTING_RESET_ON_LOGON = "ResetOnLogon";

    /**
     * Session description. Used by external tools.
     */
    public static final String SETTING_DESCRIPTION = "Description";

    /**
     * Requests that state and message data be refreshed from the message store
     * at logon, if possible. This supports simple failover behavior for
     * acceptors
     */
    public static final String SETTING_REFRESH_ON_LOGON = "RefreshOnLogon";

    /**
     * Configures the session to send redundant resend requests (off, by
     * default).
     */
    public static final String SETTING_SEND_REDUNDANT_RESEND_REQUEST = "SendRedundantResendRequests";

    /**
     * Persist messages setting (true, by default). If set to false this will
     * cause the Session to not persist any messages and all resend requests
     * will be answered with a gap fill.
     */
    public static final String SETTING_PERSIST_MESSAGES = "PersistMessages";

    /**
     * Use actual end of sequence gap for resend requests rather than using
     * "infinity" as the end sequence of the gap. Not recommended by the FIX
     * specification, but needed for some counterparties.
     */
    public static final String SETTING_USE_CLOSED_RESEND_INTERVAL = "ClosedResendInterval";

    /**
     * Allow unknown fields in messages. This is intended for unknown fields
     * with tags < 5000 (not user defined fields)
     */
    public static final String SETTING_ALLOW_UNKNOWN_MSG_FIELDS = "AllowUnknownMsgFields";

    public static final String SETTING_DEFAULT_APPL_VER_ID = "DefaultApplVerID";

    /**
     * Allow to disable heart beat failure detection
     */
    public static final String SETTING_DISABLE_HEART_BEAT_CHECK = "DisableHeartBeatCheck";

    /**
     * Return the last msg seq number processed (optional tag 369). Valid values
     * are "Y" or "N". Default is "N".
     */
    public static final String SETTING_ENABLE_LAST_MSG_SEQ_NUM_PROCESSED = "EnableLastMsgSeqNumProcessed";

    /**
     * Return the next expected message sequence number (optional tag 789 on
     * Logon) on sent Logon message and use value of tag 789 on received Logon
     * message to synchronize session. Valid values are "Y" or "N". Default is
     * "N". This should not be enabled for FIX versions lower than 4.4
     */
    public static final String SETTING_ENABLE_NEXT_EXPECTED_MSG_SEQ_NUM = "EnableNextExpectedMsgSeqNum";

    public static final String SETTING_REJECT_INVALID_MESSAGE = "RejectInvalidMessage";

    public static final String SETTING_REJECT_MESSAGE_ON_UNHANDLED_EXCEPTION = "RejectMessageOnUnhandledException";

    public static final String SETTING_REQUIRES_ORIG_SENDING_TIME = "RequiresOrigSendingTime";

    public static final String SETTING_FORCE_RESEND_WHEN_CORRUPTED_STORE = "ForceResendWhenCorruptedStore";

    public static final String SETTING_ALLOWED_REMOTE_ADDRESSES = "AllowedRemoteAddresses";

    /**
     * Setting to limit the size of a resend request in case of missing
     * messages. This is useful when the remote FIX engine does not allow to ask
     * for more than n message for a ResendRequest
     */
    public static final String SETTING_RESEND_REQUEST_CHUNK_SIZE = "ResendRequestChunkSize";

    private static final ConcurrentMap<SessionID, Session> sessions = new ConcurrentHashMap<SessionID, Session>();

    private final Application application;

    private final SessionID sessionID;

    private final SessionSchedule sessionSchedule;

    private final MessageBuilderFactory messageFactory;

    // @GuardedBy(this)
    private final SessionState state;

    private boolean enabled;

    private final String responderSync = new String("SessionResponderSync"); // unique
                                                                             // instance

    // @GuardedBy(responderSync)
    private Responder responder;

    // The session time checks were causing performance problems
    // so we are checking only once per second.
    private long lastSessionTimeCheck = 0;

    private int logonAttempts = 0;

    private long lastSessionLogon = 0;

    private final DataDictionaryProvider dataDictionaryProvider;

    private final boolean checkLatency;

    private final int maxLatency;

    private int resendRequestChunkSize = 0;

    private final boolean resetOnLogon;

    private final boolean resetOnLogout;

    private final boolean resetOnDisconnect;

    private final boolean resetOnError;

    private final boolean disconnectOnError;

    private final boolean millisecondsInTimeStamp;

    private final boolean refreshMessageStoreAtLogon;

    private final boolean redundantResentRequestsAllowed;

    private final boolean persistMessages;

    private final boolean checkCompID;

    private final boolean useClosedRangeForResend;

    private boolean disableHeartBeatCheck = false;

    private boolean rejectInvalidMessage = false;

    private boolean rejectMessageOnUnhandledException = false;

    private boolean requiresOrigSendingTime = false;

    private boolean forceResendWhenCorruptedStore = false;

    private boolean enableNextExpectedMsgSeqNum = false;

    private boolean enableLastMsgSeqNumProcessed = false;

    private final AtomicBoolean isResetting = new AtomicBoolean();

    private final ListenerSupport stateListeners = new ListenerSupport(SessionStateListener.class);

    private final SessionStateListener stateListener = (SessionStateListener) stateListeners
            .getMulticaster();

    private final AtomicReference<FIXApplication> targetDefaultApplVerID = new AtomicReference<FIXApplication>();

    private final FIXApplication senderDefaultApplVerID;

    private boolean validateSequenceNumbers = true;

    private boolean validateIncomingMessage = true;

    private final int[] logonIntervals;

    private final Set<InetAddress> allowedRemoteAddresses;

    public static final int DEFAULT_MAX_LATENCY = 120;

    public static final int DEFAULT_RESEND_RANGE_CHUNK_SIZE = 0; // no resend
                                                                 // range

    public static final double DEFAULT_TEST_REQUEST_DELAY_MULTIPLIER = 0.5;

    protected final static Logger log = LoggerFactory.getLogger(Session.class);

    Session(Application application, MessageStoreFactory messageStoreFactory, SessionID sessionID,
            DataDictionaryProvider dataDictionaryProvider, SessionSchedule sessionSchedule,
            LogFactory logFactory, MessageBuilderFactory messageFactory, int heartbeatInterval) {

        this(application, messageStoreFactory, sessionID, dataDictionaryProvider, sessionSchedule,
                logFactory, messageFactory, heartbeatInterval, true, DEFAULT_MAX_LATENCY, true,
                false, false, false, false, true, false, true, false,
                DEFAULT_TEST_REQUEST_DELAY_MULTIPLIER, null, true, new int[] { 5 }, false, false,
                false, true, false, true, false, null, true, DEFAULT_RESEND_RANGE_CHUNK_SIZE,
                false, false);
    }

    Session(Application application, MessageStoreFactory messageStoreFactory, SessionID sessionID,
            DataDictionaryProvider dataDictionaryProvider, SessionSchedule sessionSchedule,
            LogFactory logFactory, MessageBuilderFactory messageFactory, int heartbeatInterval,
            boolean checkLatency, int maxLatency, boolean millisecondsInTimeStamp,
            boolean resetOnLogon, boolean resetOnLogout, boolean resetOnDisconnect,
            boolean refreshMessageStoreAtLogon, boolean checkCompID,
            boolean redundantResentRequestsAllowed, boolean persistMessages,
            boolean useClosedRangeForResend, double testRequestDelayMultiplier,
            FIXApplication senderDefaultApplVerID, boolean validateSequenceNumbers,
            int[] logonIntervals, boolean resetOnError, boolean disconnectOnError,
            boolean disableHeartBeatCheck, boolean rejectInvalidMessage,
            boolean rejectMessageOnUnhandledException, boolean requiresOrigSendingTime,
            boolean forceResendWhenCorruptedStore, Set<InetAddress> allowedRemoteAddresses,
            boolean validateIncomingMessage, int resendRequestChunkSize,
            boolean enableNextExpectedMsgSeqNum, boolean enableLastMsgSeqNumProcessed) {

        this.application = application;
        this.sessionID = sessionID;
        this.sessionSchedule = sessionSchedule;
        this.checkLatency = checkLatency;
        this.maxLatency = maxLatency;
        this.resetOnLogon = resetOnLogon;
        this.resetOnLogout = resetOnLogout;
        this.resetOnDisconnect = resetOnDisconnect;
        this.millisecondsInTimeStamp = millisecondsInTimeStamp;
        this.refreshMessageStoreAtLogon = refreshMessageStoreAtLogon;
        this.dataDictionaryProvider = dataDictionaryProvider;
        this.messageFactory = messageFactory;
        this.checkCompID = checkCompID;
        this.redundantResentRequestsAllowed = redundantResentRequestsAllowed;
        this.persistMessages = persistMessages;
        this.useClosedRangeForResend = useClosedRangeForResend;
        this.senderDefaultApplVerID = senderDefaultApplVerID;
        this.logonIntervals = logonIntervals;
        this.resetOnError = resetOnError;
        this.disconnectOnError = disconnectOnError;
        this.disableHeartBeatCheck = disableHeartBeatCheck;
        this.rejectInvalidMessage = rejectInvalidMessage;
        this.rejectMessageOnUnhandledException = rejectMessageOnUnhandledException;
        this.requiresOrigSendingTime = requiresOrigSendingTime;
        this.forceResendWhenCorruptedStore = forceResendWhenCorruptedStore;
        this.allowedRemoteAddresses = allowedRemoteAddresses;
        this.validateIncomingMessage = validateIncomingMessage;
        this.validateSequenceNumbers = validateSequenceNumbers;
        this.resendRequestChunkSize = resendRequestChunkSize;
        this.enableNextExpectedMsgSeqNum = enableNextExpectedMsgSeqNum;
        this.enableLastMsgSeqNumProcessed = enableLastMsgSeqNumProcessed;

        final Log engineLog = (logFactory != null) ? logFactory.create(sessionID) : null;
        if (engineLog instanceof SessionStateListener) {
            addStateListener((SessionStateListener) engineLog);
        }

        final MessageStore messageStore = messageStoreFactory.create(sessionID);
        if (messageStore instanceof SessionStateListener) {
            addStateListener((SessionStateListener) messageStore);
        }

        state = new SessionState(this, engineLog, heartbeatInterval, heartbeatInterval != 0,
                messageStore, testRequestDelayMultiplier);

        registerSession(this);

        getLog().onEvent("Session " + sessionID + " schedule is " + sessionSchedule);
        try {
            resetIfSessionNotCurrent(sessionID, SystemTime.currentTimeMillis());
        } catch (final IOException e) {
            LogUtil.logThrowable(getLog(), "error during session construction", e);
        }

        // QFJ-721: for non-FIXT sessions we do not need to set
        // targetDefaultApplVerID from Logon
        if (!sessionID.isFIXT()) {
            targetDefaultApplVerID.set(MessageUtils.toApplVerID(sessionID.getBeginString()));
        }

        setEnabled(true);

        getLog().onEvent("Created session: " + sessionID);
    }

    public MessageBuilderFactory getMessageFactory() {

        return messageFactory;
    }

    /**
     * Registers a responder with the session. This is used by the acceptor and
     * initiator implementations.
     *
     * @param responder a responder implementation
     */
    public void setResponder(Responder responder) {

        synchronized (responderSync) {
            this.responder = responder;
            if (responder != null) {
                stateListener.onConnect();
            } else {
                stateListener.onDisconnect();
            }
        }
    }

    public Responder getResponder() {

        synchronized (responderSync) {
            return responder;
        }
    }

    /**
     * This should not be used by end users.
     *
     * @return the Session's connection responder
     */
    public boolean hasResponder() {

        return getResponder() != null;
    }

    /**
     * Provides remote address of the session connection, if any.
     *
     * @return remote address (host:port) if connected, null if not.
     */
    public String getRemoteAddress() {

        Responder responder = getResponder();
        if (responder != null) {
            return responder.getRemoteAddress();
        }
        return null;
    }

    private boolean isCurrentSession(final long time) throws IOException {

        return sessionSchedule == null
                || sessionSchedule.isSameSession(SystemTime.getUtcCalendar(time),
                        SystemTime.getUtcCalendar(state.getCreationTime()));
    }

    /**
     * Send a message to the session specified in the message's target
     * identifiers.
     *
     * @param message a FIX message
     * @return true is send was successful, false otherwise
     * @throws SessionNotFound if session could not be located
     */
    public static boolean sendToTarget(Message message) throws SessionNotFound {

        return sendToTarget(message, "");
    }

    /**
     * Send a message to the session specified in the message's target
     * identifiers. The session qualifier is used to distinguish sessions with
     * the same target identifiers.
     *
     * @param message a FIX message
     * @param qualifier a session qualifier
     * @return true is send was successful, false otherwise
     * @throws SessionNotFound if session could not be located
     */
    public static boolean sendToTarget(Message message, String qualifier) throws SessionNotFound {

        try {
            final String senderCompID = message.getHeader().getString(FixTags.SENDER_COMP_ID);
            final String targetCompID = message.getHeader().getString(FixTags.TARGET_COMP_ID);
            return sendToTarget(message, senderCompID, targetCompID, qualifier);
        } catch (final FieldNotFound e) {
            throw new SessionNotFound("missing sender or target company ID");
        }
    }

    /**
     * Send a message to the session specified by the provided target company
     * ID. The sender company ID is provided as an argument rather than from the
     * message.
     *
     * @param message a FIX message
     * @param senderCompID the sender's company ID
     * @param targetCompID the target's company ID
     * @return true is send was successful, false otherwise
     * @throws SessionNotFound if session could not be located
     */
    public static boolean sendToTarget(Message message, String senderCompID, String targetCompID)
            throws SessionNotFound {

        return sendToTarget(message, senderCompID, targetCompID, "");
    }

    /**
     * Send a message to the session specified by the provided target company
     * ID. The sender company ID is provided as an argument rather than from the
     * message. The session qualifier is used to distinguish sessions with the
     * same target identifiers.
     *
     * @param message a FIX message
     * @param senderCompID the sender's company ID
     * @param targetCompID the target's company ID
     * @param qualifier a session qualifier
     * @return true is send was successful, false otherwise
     * @throws SessionNotFound if session could not be located
     */
    public static boolean sendToTarget(Message message, String senderCompID, String targetCompID,
            String qualifier) throws SessionNotFound {

        try {
            return sendToTarget(message,
                    new SessionID(MessageUtils.getBeginString(message.getHeader()), senderCompID,
                            targetCompID, qualifier));
        } catch (final SessionNotFound e) {
            throw e;
        } catch (final Exception e) {
            throw new SessionException(e);
        }
    }

    /**
     * Send a message to the session specified by the provided session ID.
     *
     * @param message a FIX message
     * @param sessionID the target SessionID
     * @return true is send was successful, false otherwise
     * @throws SessionNotFound if session could not be located
     */
    public static boolean sendToTarget(Message message, SessionID sessionID) throws SessionNotFound {

        final Session session = lookupSession(sessionID);
        if (session == null) {
            throw new SessionNotFound();
        }
        message.setSessionID(sessionID);
        return session.send(message);
    }

    static void registerSession(Session session) {

        sessions.put(session.getSessionID(), session);
    }

    static void unregisterSessions(List<SessionID> sessionIds) {

        for (final SessionID sessionId : sessionIds) {
            final Session session = sessions.remove(sessionId);
            if (session != null) {
                try {
                    session.close();
                } catch (final IOException e) {
                    log.error("Failed to close session resources", e);
                }
            }
        }
    }

    /**
     * Locates a session specified by the provided session ID.
     *
     * @param sessionID the session ID
     * @return the session, if found, or null otherwise
     */
    public static Session lookupSession(SessionID sessionID) {

        return sessions.get(sessionID);
    }

    /**
     * This method can be used to manually logon to a FIX session.
     */
    public void logon() {

        state.clearLogoutReason();
        setEnabled(true);
    }

    private synchronized void setEnabled(boolean enabled) {

        this.enabled = enabled;
    }

    private Message createAdminMessage(FIXBeginString beginString, String msgType) {

        MessageBuilder builder = messageFactory.getMessageBuilder(beginString, msgType);
        Message message = builder.create();
        Message.Header header = message.getHeader();

        state.setLastSentTime(SystemTime.currentTimeMillis());
        header.setField(builder.create(FixTags.BEGIN_STRING, sessionID.getBeginString().getValue()));
        header.setField(builder.create(FixTags.SENDER_COMP_ID, sessionID.getSenderCompID()));
        header.setField(builder.create(FixTags.TARGET_COMP_ID, sessionID.getTargetCompID()));
        optionallySetID(builder, header, FixTags.SENDER_SUB_ID, sessionID.getSenderSubID());
        optionallySetID(builder, header, FixTags.SENDER_LOCATION_ID,
                sessionID.getSenderLocationID());
        optionallySetID(builder, header, FixTags.TARGET_SUB_ID, sessionID.getTargetSubID());
        optionallySetID(builder, header, FixTags.TARGET_LOCATION_ID,
                sessionID.getTargetLocationID());
        header.setInt(FixTags.MSG_SEQ_NUM, getExpectedSenderNum());
        insertSendingTime(header);

        return message;
    }

    private void optionallySetID(MessageBuilder builder, FIXFieldGraph header, int field,
            String value) {

        if (!value.equals(SessionID.NOT_SET)) {
            header.setField(builder.create(field, value));
        }
    }

    private void initializeHeader(Message.Header header) {

        state.setLastSentTime(SystemTime.currentTimeMillis());
        header.setString(FixTags.BEGIN_STRING, sessionID.getBeginString().getValue());
        header.setString(FixTags.SENDER_COMP_ID, sessionID.getSenderCompID());
        optionallySetID(header, FixTags.SENDER_SUB_ID, sessionID.getSenderSubID());
        optionallySetID(header, FixTags.SENDER_LOCATION_ID, sessionID.getSenderLocationID());
        header.setString(FixTags.TARGET_COMP_ID, sessionID.getTargetCompID());
        optionallySetID(header, FixTags.TARGET_SUB_ID, sessionID.getTargetSubID());
        optionallySetID(header, FixTags.TARGET_LOCATION_ID, sessionID.getTargetLocationID());
        header.setInt(FixTags.MSG_SEQ_NUM, getExpectedSenderNum());
        insertSendingTime(header);
    }

    private void optionallySetID(Header header, int field, String value) {

        if (!value.equals(SessionID.NOT_SET)) {
            header.setString(field, value);
        }
    }

    private void insertSendingTime(Message.Header header) {

        header.setUtcTimeStamp(FixTags.SENDING_TIME, SystemTime.getDate(), includeMillis());
    }

    private boolean includeMillis() {

        return millisecondsInTimeStamp
                && sessionID.getBeginString().ordinal() >= FIXBeginString.FIX42.ordinal();
    }

    /**
     * This method can be used to manually logout of a FIX session.
     */
    public void logout() {

        setEnabled(false);
    }

    /**
     * This method can be used to manually logout of a FIX session.
     *
     * @param reason this will be included in the logout message
     */
    public void logout(String reason) {

        state.setLogoutReason(reason);
        logout();
    }

    /**
     * Used internally
     *
     * @return true if session is enabled, false otherwise.
     */
    public synchronized boolean isEnabled() {

        return enabled;
    }

    /**
     * Predicate indicating whether a logon message has been sent. (QF
     * Compatibility)
     *
     * @return true if logon message was sent, false otherwise.
     */
    public boolean sentLogon() {

        return state.isLogonSent();
    }

    /**
     * Predicate indicating whether a logon message has been received. (QF
     * Compatibility)
     *
     * @return true if logon message was received, false otherwise.
     */
    public boolean receivedLogon() {

        return state.isLogonReceived();
    }

    /**
     * Predicate indicating whether a logout message has been sent. (QF
     * Compatibility)
     *
     * @return true if logout message was sent, false otherwise.
     */
    public boolean sentLogout() {

        return state.isLogoutSent();
    }

    /**
     * Predicate indicating whether a logout message has been received. This can
     * be used to determine if a session ended with an unexpected disconnect.
     *
     * @return true if logout message has been received, false otherwise.
     */
    public boolean receivedLogout() {

        return state.isLogoutReceived();
    }

    /**
     * Is the session logged on.
     *
     * @return true if logged on, false otherwise.
     */
    public boolean isLoggedOn() {

        return sentLogon() && receivedLogon();
    }

    private boolean isResetNeeded() {

        return sessionID.getBeginString().ordinal() >= FIXBeginString.FIX41.ordinal()
                && (resetOnLogon || resetOnLogout || resetOnDisconnect)
                && getExpectedSenderNum() == 1 && getExpectedTargetNum() == 1;
    }

    /**
     * Logs out and disconnects session (if logged on) and then resets session
     * state.
     *
     * @throws IOException IO error
     * @see SessionState#reset()
     */
    public void reset() throws IOException {

        if (!isResetting.compareAndSet(false, true)) {
            return;
        }
        try {
            if (hasResponder() && isLoggedOn()) {
                if (application instanceof ApplicationExtended) {
                    ((ApplicationExtended) application).onBeforeSessionReset(sessionID);
                }
                generateLogout();
                disconnect("Session reset", false);
            }
            resetState();
        } finally {
            isResetting.set(false);
        }
    }

    /**
     * Set the next outgoing message sequence number. This method is not
     * synchronized.
     *
     * @param num next outgoing sequence number
     * @throws IOException IO error
     */
    public void setNextSenderMsgSeqNum(int num) throws IOException {

        state.getMessageStore().setNextSenderMsgSeqNum(num);
    }

    /**
     * Set the next expected target message sequence number. This method is not
     * synchronized.
     *
     * @param num next expected target sequence number
     * @throws IOException IO error
     */
    public void setNextTargetMsgSeqNum(int num) throws IOException {

        state.getMessageStore().setNextTargetMsgSeqNum(num);
    }

    /**
     * Retrieves the expected sender sequence number. This method is not
     * synchronized.
     *
     * @return next expected sender sequence number
     */
    public int getExpectedSenderNum() {

        try {
            return state.getMessageStore().getNextSenderMsgSeqNum();
        } catch (final IOException e) {
            getLog().onErrorEvent("getNextSenderMsgSeqNum failed: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Retrieves the expected target sequence number. This method is not
     * synchronized.
     *
     * @return next expected target sequence number
     */
    public int getExpectedTargetNum() {

        try {
            return state.getMessageStore().getNextTargetMsgSeqNum();
        } catch (final IOException e) {
            getLog().onErrorEvent("getNextTargetMsgSeqNum failed: " + e.getMessage());
            return -1;
        }
    }

    public Log getLog() {

        return state.getLog();
    }

    /**
     * Get the message store. (QF Compatibility)
     *
     * @return the message store
     */
    public MessageStore getStore() {

        return state.getMessageStore();
    }

    /**
     * (Internal use only)
     */
    public void next(Message message) throws FieldNotFound, RejectLogon, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {

        final Header header = message.getHeader();
        final String msgType = header.getString(FixTags.MSG_TYPE);

        // QFJ-650
        if (!header.isFieldSet(FixTags.MSG_SEQ_NUM)) {
            generateLogout("Received message without MsgSeqNum");
            disconnect("Received message without MsgSeqNum: " + message, true);
            return;
        }

        final FIXBeginString sessionBeginString = sessionID.getBeginString();
        try {
            final FIXBeginString beginString = MessageUtils.getBeginString(header);
            if (!beginString.equals(sessionBeginString)) {
                throw new UnsupportedVersion("Message version '" + beginString.getValue()
                        + "' does not match the session version '" + sessionBeginString.getValue()
                        + "'");
            }

            if (msgType.equals(FixMessageTypes.LOGON)) {
                if (sessionID.isFIXT()) {
                    targetDefaultApplVerID.set(FIXApplication.parseId(message
                            .getString(FixTags.DEFAULT_APPL_VER_ID)));
                }

                // QFJ-648
                if (message.isFieldSet(FixTags.HEART_BT_INT)) {
                    if (message.getInt(FixTags.HEART_BT_INT) < 0) {
                        throw new RejectLogon("HeartBtInt must not be negative");
                    }
                }
            }

            if (validateIncomingMessage && dataDictionaryProvider != null) {

                final DataDictionary sessionDataDictionary = dataDictionaryProvider
                        .getSessionDataDictionary(beginString);

                final FIXApplication applVerID = header.isFieldSet(FixTags.APPL_VER_ID)
                        ? FIXApplication.parseId(header.getString(FixTags.APPL_VER_ID))
                        : targetDefaultApplVerID.get();

                final DataDictionary applicationDataDictionary = MessageUtils
                        .isAdminMessage(msgType) ? dataDictionaryProvider
                        .getSessionDataDictionary(beginString) : dataDictionaryProvider
                        .getApplicationDataDictionary(applVerID);

                // related to QFJ-367 : just warn invalid incoming field/tags
                try {
                    DataDictionary.validate(message, sessionDataDictionary,
                            applicationDataDictionary);
                } catch (final IncorrectTagValue e) {
                    if (rejectInvalidMessage) {
                        throw e;
                    }
                    getLog().onErrorEvent("Warn: incoming message with " + e + ": " + message);

                } catch (final FieldException e) {
                    if (message.isFieldSet(e.getField())) {
                        if (rejectInvalidMessage) {
                            throw e;
                        }

                        getLog().onErrorEvent(
                                "Warn: incoming message with incorrect field: "
                                        + message.getField(e.getField()) + ": " + message);

                    } else {
                        if (rejectInvalidMessage) {
                            throw e;
                        }

                        getLog().onErrorEvent(
                                "Warn: incoming message with missing field: " + e.getField() + ": "
                                        + e.getMessage() + ": " + message);
                    }
                } catch (final FieldNotFound e) {
                    if (rejectInvalidMessage) {
                        throw e;
                    }

                    getLog().onErrorEvent("Warn: incoming " + e + ": " + message);
                }
            }

            if (msgType.equals(FixMessageTypes.LOGON)) {
                nextLogon(message);
            } else if (msgType.equals(FixMessageTypes.HEARTBEAT)) {
                nextHeartBeat(message);
            } else if (msgType.equals(FixMessageTypes.TEST_REQUEST)) {
                nextTestRequest(message);
            } else if (msgType.equals(FixMessageTypes.SEQUENCE_RESET)) {
                nextSequenceReset(message);
            } else if (msgType.equals(FixMessageTypes.LOGOUT)) {
                nextLogout(message);
            } else if (msgType.equals(FixMessageTypes.RESEND_REQUEST)) {
                nextResendRequest(message);
            } else if (msgType.equals(FixMessageTypes.REJECT)) {
                nextReject(message);
            } else {
                if (!verify(message)) {
                    return;
                }
                state.incrNextTargetMsgSeqNum();
            }
        } catch (final FieldException e) {
            getLog().onErrorEvent("Rejecting invalid message: " + e + ": " + message);
            if (resetOrDisconnectIfRequired(message)) {
                return;
            }
            generateReject(message, e.getSessionRejectReason(), e.getField());
        } catch (final FieldNotFound e) {
            getLog().onErrorEvent("Rejecting invalid message: " + e + ": " + message);
            if (resetOrDisconnectIfRequired(message)) {
                return;
            }
            if (sessionBeginString.gte(FIXBeginString.FIX42) && message.isApp()) {
                generateBusinessReject(message,
                        BusinessRejectReasonText.CONDITIONALLY_REQUIRED_FIELD_MISSING, e.field);
            } else {
                if (msgType.equals(FixMessageTypes.LOGON)) {
                    getLog().onErrorEvent("Required field missing from logon");
                    disconnect("Required field missing from logon", true);
                } else {
                    generateReject(message, SessionRejectReasonText.REQUIRED_TAG_MISSING, e.field);
                }
            }
        } catch (final IncorrectDataFormat e) {
            getLog().onErrorEvent("Rejecting invalid message: " + e + ": " + message);
            if (resetOrDisconnectIfRequired(message)) {
                return;
            }
            generateReject(message, SessionRejectReasonText.INCORRECT_DATA_FORMAT_FOR_VALUE,
                    e.field);
        } catch (final IncorrectTagValue e) {
            getLog().onErrorEvent("Rejecting invalid message: " + e + ": " + message);
            generateReject(message, SessionRejectReasonText.VALUE_IS_INCORRECT, e.field);
        } catch (final InvalidMessage e) {
            getLog().onErrorEvent("Skipping invalid message: " + e + ": " + message);
            if (resetOrDisconnectIfRequired(message)) {
                return;
            }
        } catch (final RejectLogon e) {
            final String rejectMessage = e.getMessage() != null ? (": " + e) : "";
            getLog().onErrorEvent("Logon rejected" + rejectMessage);
            if (e.isLogoutBeforeDisconnect()) {
                if (e.getSessionStatus() > -1) {
                    generateLogout(e.getMessage(),
                            new StringField(FixTags.SESSION_STATUS, e.getSessionStatus()));
                } else {
                    generateLogout(e.getMessage());
                }
            }
            state.incrNextTargetMsgSeqNum();
            disconnect("Logon rejected: " + e, true);
        } catch (final UnsupportedMessageType e) {
            getLog().onErrorEvent("Rejecting invalid message: " + e + ": " + message);
            if (resetOrDisconnectIfRequired(message)) {
                return;
            }
            if (sessionBeginString.gte(FIXBeginString.FIX42)) {
                generateBusinessReject(message, BusinessRejectReasonText.UNSUPPORTED_MESSAGE_TYPE,
                        0);
            } else {
                generateReject(message, "Unsupported message type");
            }
        } catch (final UnsupportedVersion e) {
            getLog().onErrorEvent("Rejecting invalid message: " + e + ": " + message);
            if (resetOrDisconnectIfRequired(message)) {
                return;
            }
            if (msgType.equals(FixMessageTypes.LOGOUT)) {
                nextLogout(message);
            } else {
                generateLogout("Incorrect BeginString: " + e.getMessage());
                state.incrNextTargetMsgSeqNum();
                // 1d_InvalidLogonWrongBeginString.def appears to require
                // a disconnect although the C++ didn't appear to be doing it.
                // ???
                disconnect("Incorrect BeginString: " + e, true);
            }
        } catch (final IOException e) {
            LogUtil.logThrowable(sessionID, "Error processing message: " + message, e);
            if (resetOrDisconnectIfRequired(message)) {
                return;
            }
        } catch (Throwable t) { // QFJ-572
            // If there are any other Throwables we might catch them here if
            // desired.
            // They were most probably thrown out of fromCallback().
            if (rejectMessageOnUnhandledException) {
                getLog().onErrorEvent("Rejecting message: " + t + ": " + message);
                if (resetOrDisconnectIfRequired(message)) {
                    return;
                }
                if (!(MessageUtils.isAdminMessage(msgType))
                        && sessionBeginString.gte(FIXBeginString.FIX42)) {
                    generateBusinessReject(message,
                            BusinessRejectReasonText.APPLICATION_NOT_AVAILABLE, 0);
                } else {
                    if (msgType.equals(FixMessageTypes.LOGON)) {
                        disconnect("Problem processing Logon message", true);
                    } else {
                        generateReject(message, SessionRejectReasonText.OTHER, 0);
                    }
                }
            } else {
                // Re-throw as quickfix.RuntimeError to keep close to the former
                // behaviour
                // and to have a clear notion of what is thrown out of this
                // method.
                // Throwing RuntimeError here means that the target seqnum is
                // not incremented
                // and a resend will be triggered by the next incoming message.
                throw new RuntimeError(t);
            }
        }

        nextQueued();
        if (isLoggedOn()) {
            next();
        }
    }

    private boolean resetOrDisconnectIfRequired(Message msg) {

        if (!resetOnError && !disconnectOnError) {
            return false;
        }
        if (!isLoggedOn()) {
            return false;
        }
        // do not interfere in admin and logon/logout messages etc.
        if (msg != null && msg.isAdmin()) {
            return false;
        }
        if (resetOnError) {
            try {
                getLog().onErrorEvent("Auto reset");
                reset();
            } catch (final IOException e) {
                log.error("Failed reseting: " + e);
            }
            return true;
        }
        if (disconnectOnError) {
            try {
                disconnect("Auto disconnect", false);
            } catch (final IOException e) {
                log.error("Failed disconnecting: " + e);
            }
            return true;
        }
        return false;
    }

    private boolean isStateRefreshNeeded(String msgType) {

        return refreshMessageStoreAtLogon && !state.isInitiator()
                && msgType.equals(FixMessageTypes.LOGON);
    }

    private void nextReject(Message reject) throws FieldNotFound, RejectLogon, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {

        if (!verify(reject, false, validateSequenceNumbers)) {
            return;
        }
        if (getExpectedTargetNum() == reject.getHeader().getInt(FixTags.MSG_SEQ_NUM)) {
            state.incrNextTargetMsgSeqNum();
        }
        nextQueued();
    }

    private void nextResendRequest(Message resendRequest) throws IOException, RejectLogon,
            FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType,
            InvalidMessage {

        // QFJ-653: Check if seqnums are too low.
        // QFJ-673: Do not check if seqnums are too high in verify() since in
        // case of a seqnum mismatch this will
        // enqueue the ResendRequest for later processing. This might lead to a
        // deadlock situation in
        // which the counterparty waits for our messages to be resent and we are
        // also waiting for our
        // ResendRequest to be satisfied in order to process the queued
        // ResendRequest of the counterparty.
        // Instead, send out the requested messages and afterwards enqueue the
        // ResendRequest in order to
        // later increase the target seqnum in method nextQueued(int).
        if (!verify(resendRequest, false, validateSequenceNumbers)) {
            return;
        }
        final int msgSeqNum = resendRequest.getHeader().getInt(FixTags.MSG_SEQ_NUM);
        if (validateSequenceNumbers && isTargetTooHigh(msgSeqNum)) {
            enqueueMessage(resendRequest, msgSeqNum);
        }

        final int beginSeqNo = resendRequest.getInt(FixTags.BEGIN_SEQ_NO);
        final int endSeqNo = resendRequest.getInt(FixTags.END_SEQ_NO);
        getLog().onEvent(
                "Received ResendRequest FROM: " + beginSeqNo + " TO: " + formatEndSeqNum(endSeqNo));
        manageGapFill(resendRequest, beginSeqNo, endSeqNo);
    }

    /**
     * A Gap has been request to be filled by either a resend request or on a
     * logon message
     *
     * @param messageOutSync the message that caused the gap to be filled
     * @param beginSeqNo the seqNum of the first missing message
     * @param endSeqNo the seqNum of the last missing message
     * @throws FieldNotFound
     * @throws IOException
     * @throws InvalidMessage
     */
    private void manageGapFill(Message messageOutSync, int beginSeqNo, int endSeqNo)
            throws FieldNotFound, IOException, InvalidMessage {

        // Adjust the ending sequence number for older versions of FIX
        final FIXBeginString beginString = sessionID.getBeginString();
        final int expectedSenderNum = getExpectedSenderNum();
        if (beginString.gte(FIXBeginString.FIX42) && endSeqNo == 0
                || beginString.lte(FIXBeginString.FIX42) && endSeqNo == 999999
                || endSeqNo >= expectedSenderNum) {
            endSeqNo = expectedSenderNum - 1;
        }

        // Just do a gap fill when messages aren't persisted
        if (!persistMessages) {
            endSeqNo += 1;
            final int next = state.getNextSenderMsgSeqNum();
            if (endSeqNo > next) {
                endSeqNo = next;
            }
            generateSequenceReset(messageOutSync, beginSeqNo, endSeqNo);
        } else {
            resendMessages(messageOutSync, beginSeqNo, endSeqNo);
        }
        final int resendRequestMsgSeqNum = messageOutSync.getHeader().getInt(FixTags.MSG_SEQ_NUM);
        if (getExpectedTargetNum() == resendRequestMsgSeqNum) {
            state.incrNextTargetMsgSeqNum();
        }
    }

    private String formatEndSeqNum(int seqNo) {

        return (seqNo == 0 ? "infinity" : Integer.toString(seqNo));
    }

    private Message parseMessage(String messageData) throws InvalidMessage {

        return MessageUtils.parse(this, messageData);
    }

    private boolean isTargetTooLow(int msgSeqNum) throws IOException {

        return msgSeqNum < state.getNextTargetMsgSeqNum();
    }

    /**
     * @param receivedMessage if not null, it is the message received and upon
     *        which the resend request is generated
     * @param beginSeqNo
     * @param endSeqNo
     * @throws FieldNotFound
     */
    private void generateSequenceReset(Message receivedMessage, int beginSeqNo, int endSeqNo)
            throws FieldNotFound {

        final Message sequenceReset = createAdminMessage(sessionID.getBeginString(),
                FixMessageTypes.SEQUENCE_RESET);
        final int newSeqNo = endSeqNo;
        final Header header = sequenceReset.getHeader();
        header.setBoolean(FixTags.POSS_DUP_FLAG, true);
        initializeHeader(header);
        header.setUtcTimeStamp(FixTags.ORIG_SENDING_TIME,
                header.getUtcTimeStamp(FixTags.SENDING_TIME), includeMillis());
        header.setInt(FixTags.MSG_SEQ_NUM, beginSeqNo);
        sequenceReset.setInt(FixTags.NEW_SEQ_NO, newSeqNo);
        sequenceReset.setBoolean(FixTags.GAP_FILL_FLAG, true);
        if (receivedMessage != null && enableLastMsgSeqNumProcessed) {
            try {
                sequenceReset.getHeader().setInt(FixTags.LAST_MSG_SEQ_NUM_PROCESSED,
                        receivedMessage.getHeader().getInt(FixTags.MSG_SEQ_NUM));
            } catch (final FieldNotFound e) {
                // should not happen as MsgSeqNum must be present
                getLog().onErrorEvent("Received message without MsgSeqNum " + receivedMessage);
            }
        }
        sendRaw(sequenceReset, beginSeqNo);
        getLog().onEvent("Sent SequenceReset TO: " + newSeqNo);
    }

    private boolean resendApproved(Message message) {

        try {
            application.toApp(message, sessionID);
        } catch (final DoNotSend e) {
            return false;
        } catch (final Throwable t) {
            // Any exception other than DoNotSend will not stop the message from
            // being resent
            logApplicationException("toApp() during resend", t);
        }

        return true;
    }

    private void initializeResendFields(Message message) throws FieldNotFound {

        final Message.Header header = message.getHeader();
        final Date sendingTime = header.getUtcTimeStamp(FixTags.SENDING_TIME);
        header.setUtcTimeStamp(FixTags.ORIG_SENDING_TIME, sendingTime, includeMillis());
        header.setBoolean(FixTags.POSS_DUP_FLAG, true);
        insertSendingTime(header);
    }

    private void logApplicationException(String location, Throwable t) {

        logThrowable(getLog(), "Application exception in " + location, t);
    }

    private void nextLogout(Message logout) throws IOException, RejectLogon, FieldNotFound,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

        if (!verify(logout, false, false)) {
            return;
        }

        String msg;
        if (!state.isLogoutSent()) {
            msg = "Received logout request";
            if (logout.isFieldSet(FixTags.TEXT)) {
                msg += ": " + logout.getString(FixTags.TEXT);
            }
            getLog().onEvent(msg);
            generateLogout(logout);
            getLog().onEvent("Sent logout response");
        } else {
            msg = "Received logout response";
            getLog().onEvent(msg);
        }

        state.setLogoutReceived(true);

        // QFJ-750
        if (getExpectedTargetNum() == logout.getHeader().getInt(FixTags.MSG_SEQ_NUM)) {
            state.incrNextTargetMsgSeqNum();
        }
        if (resetOnLogout) {
            resetState();
        }

        disconnect(msg, false);
    }

    public void generateLogout() {

        generateLogout(null, null, null);
    }

    private void generateLogout(Message otherLogout) {

        generateLogout(otherLogout, null, null);
    }

    private void generateLogout(String reason) {

        generateLogout(null, reason, null);
    }

    private void generateLogout(String reason, FIXField<?> sessionStatus) {

        generateLogout(null, reason, sessionStatus);
    }

    /**
     * To generate a logout message
     *
     * @param otherLogout if not null, the logout message that is causing a
     *        logout to be sent
     * @param text
     */
    private void generateLogout(Message otherLogout, String text, FIXField<?> sessionStatus) {

        final Message logout = createAdminMessage(sessionID.getBeginString(),
                FixMessageTypes.LOGOUT);
        initializeHeader(logout.getHeader());
        if (text != null && !"".equals(text)) {
            logout.setString(FixTags.TEXT, text);
        }
        if (sessionStatus != null) {
            logout.setField(FixTags.SESSION_STATUS, sessionStatus);
        }
        if (otherLogout != null && enableLastMsgSeqNumProcessed) {
            try {
                logout.getHeader().setInt(FixTags.LAST_MSG_SEQ_NUM_PROCESSED,
                        otherLogout.getHeader().getInt(FixTags.MSG_SEQ_NUM));
            } catch (final FieldNotFound e) {
                // should not happen as MsgSeqNum must be present
                getLog().onErrorEvent("Received logout without MsgSeqNum");
            }
        }
        sendRaw(logout, 0);
        state.setLogoutSent(true);
    }

    private void nextSequenceReset(Message sequenceReset) throws IOException, RejectLogon,
            FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

        boolean isGapFill = false;
        if (sequenceReset.isFieldSet(FixTags.GAP_FILL_FLAG)) {
            isGapFill = sequenceReset.getBoolean(FixTags.GAP_FILL_FLAG) && validateSequenceNumbers;
        }

        if (!verify(sequenceReset, isGapFill, isGapFill)) {
            return;
        }

        if (validateSequenceNumbers && sequenceReset.isFieldSet(FixTags.NEW_SEQ_NO)) {
            final int newSequence = sequenceReset.getInt(FixTags.NEW_SEQ_NO);

            getLog().onEvent(
                    "Received SequenceReset FROM: " + getExpectedTargetNum() + " TO: "
                            + newSequence);
            if (newSequence > getExpectedTargetNum()) {
                state.setNextTargetMsgSeqNum(newSequence);
                final ResendRange range = state.getResendRange();
                if (range.isChunkedResendRequest()) {
                    if (newSequence >= range.getCurrentEndSeqNo()
                            && newSequence < range.getEndSeqNo()) {
                        // If new seq no is beyond the range of the current
                        // chunk
                        // and if we are not done with all resend chunks,
                        // we send out a ResendRequest at once.
                        // Alternatively, we could also wait for the next
                        // incoming message
                        // which would trigger another resend.
                        final FIXBeginString beginString = MessageUtils
                                .getBeginString(sequenceReset.getHeader());
                        sendResendRequest(beginString, range.getEndSeqNo() + 1, newSequence + 1,
                                range.getEndSeqNo());
                    }
                }
                // QFJ-728: newSequence will be the seqnum of the next message
                // so we
                // delete all older messages from the queue since they are
                // effectively skipped.
                state.dequeueMessagesUpTo(newSequence);
            } else if (newSequence < getExpectedTargetNum()) {

                getLog().onErrorEvent(
                        "Invalid SequenceReset: newSequence=" + newSequence + " < expected="
                                + getExpectedTargetNum());
                if (resetOrDisconnectIfRequired(sequenceReset)) {
                    return;
                }
                generateReject(sequenceReset, SessionRejectReasonText.VALUE_IS_INCORRECT,
                        FixTags.NEW_SEQ_NO);
            }
        }
    }

    private void generateReject(Message message, String str) throws FieldNotFound, IOException {

        final FIXBeginString beginString = sessionID.getBeginString();
        final Message reject = createAdminMessage(beginString, FixMessageTypes.REJECT);
        final Header header = message.getHeader();

        reject.reverseRoute(header);
        initializeHeader(reject.getHeader());

        final String msgType = header.getString(FixTags.MSG_TYPE);
        final String msgSeqNum = header.getString(FixTags.MSG_SEQ_NUM);
        if (beginString.gte(FIXBeginString.FIX42)) {
            reject.setString(FixTags.REF_MSG_TYPE, msgType);
        }
        reject.setString(FixTags.REF_SEQ_NUM, msgSeqNum);

        // QFJ-557: Only advance the sequence number if we are at the expected
        // number.
        if (!msgType.equals(FixMessageTypes.LOGON)
                && !msgType.equals(FixMessageTypes.SEQUENCE_RESET)
                && Integer.valueOf(msgSeqNum) == getExpectedTargetNum()) {
            state.incrNextTargetMsgSeqNum();
        }

        reject.setString(FixTags.TEXT, str);
        sendRaw(reject, 0);
        getLog().onErrorEvent("Reject sent for Message " + msgSeqNum + ": " + str);
    }

    private boolean isPossibleDuplicate(Message message) throws FieldNotFound {

        final Header header = message.getHeader();
        return header.isFieldSet(FixTags.POSS_DUP_FLAG) && header.getBoolean(FixTags.POSS_DUP_FLAG);
    }

    private void generateReject(Message message, int err, int field) throws IOException,
            FieldNotFound {

        final String reason = SessionRejectReasonText.getMessage(err);
        if (!state.isLogonReceived()) {
            final String errorMessage = "Tried to send a reject while not logged on: " + reason
                    + " (field " + field + ")";
            throw new SessionException(errorMessage);
        }

        final FIXBeginString beginString = sessionID.getBeginString();
        final Message reject = createAdminMessage(beginString, FixMessageTypes.REJECT);
        final Header header = message.getHeader();

        reject.reverseRoute(header);
        initializeHeader(reject.getHeader());

        String msgType = "";
        if (header.isFieldSet(FixTags.MSG_TYPE)) {
            msgType = header.getString(FixTags.MSG_TYPE);
        }

        int msgSeqNum = 0;
        if (header.isFieldSet(FixTags.MSG_SEQ_NUM)) {
            msgSeqNum = header.getInt(FixTags.MSG_SEQ_NUM);
            reject.setInt(FixTags.REF_SEQ_NUM, msgSeqNum);
        }

        if (beginString.gte(FIXBeginString.FIX42)) {
            if (!msgType.equals("")) {
                reject.setString(FixTags.REF_MSG_TYPE, msgType);
            }
            if (beginString.gt(FIXBeginString.FIX44)) {
                reject.setInt(FixTags.SESSION_REJECT_REASON, err);
            } else if (beginString == FIXBeginString.FIX44) {
                if (err == SessionRejectReasonText.OTHER
                        || err <= SessionRejectReasonText.NON_DATA_VALUE_INCLUDES_FIELD_DELIMITER) {
                    reject.setInt(FixTags.SESSION_REJECT_REASON, err);
                }
            } else if (beginString == FIXBeginString.FIX43) {
                if (err <= SessionRejectReasonText.NON_DATA_VALUE_INCLUDES_FIELD_DELIMITER) {
                    reject.setInt(FixTags.SESSION_REJECT_REASON, err);
                }
            } else if (beginString == FIXBeginString.FIX42) {
                if (err <= SessionRejectReasonText.INVALID_MSGTYPE) {
                    reject.setInt(FixTags.SESSION_REJECT_REASON, err);
                }
            }
        }

        // This is a set and increment of target msg sequence number, the
        // sequence
        // number must be locked to guard against race conditions.

        state.lockTargetMsgSeqNum();
        try {
            // QFJ-557: Only advance the sequence number if we are at the
            // expected number.
            if (!msgType.equals(FixMessageTypes.LOGON)
                    && !msgType.equals(FixMessageTypes.SEQUENCE_RESET)
                    && msgSeqNum == getExpectedTargetNum()) {
                state.incrNextTargetMsgSeqNum();
            }
        } finally {
            state.unlockTargetMsgSeqNum();
        }

        if (reason != null && (field > 0 || err == SessionRejectReasonText.INVALID_TAG_NUMBER)) {
            setRejectReason(reject, field, reason, true);
            getLog().onErrorEvent(
                    "Reject sent for Message " + msgSeqNum + ": " + reason + ":" + field);
        } else if (reason != null) {
            setRejectReason(reject, reason);
            getLog().onErrorEvent("Reject sent for Message " + msgSeqNum + ": " + reason);
        } else {
            getLog().onErrorEvent("Reject sent for Message " + msgSeqNum);
        }

        if (enableLastMsgSeqNumProcessed) {
            reject.getHeader().setInt(FixTags.LAST_MSG_SEQ_NUM_PROCESSED,
                    message.getHeader().getInt(FixTags.MSG_SEQ_NUM));
        }

        sendRaw(reject, 0);
    }

    private void setRejectReason(Message reject, String reason) {

        reject.setString(FixTags.TEXT, reason);
    }

    private void setRejectReason(Message reject, int field, String reason,
            boolean includeFieldInReason) {

        boolean isRejectMessage;
        try {
            isRejectMessage = FixMessageTypes.REJECT.equals(reject.getHeader().getString(
                    FixTags.MSG_TYPE));
        } catch (final FieldNotFound e) {
            isRejectMessage = false;
        }
        if (isRejectMessage
                && sessionID.getBeginString().ordinal() >= FIXApplication.FIX42.ordinal()) {
            reject.setInt(FixTags.REF_TAG_ID, field);
            reject.setString(FixTags.TEXT, reason);
        } else {
            reject.setString(FixTags.TEXT, reason
                    + (includeFieldInReason ? " (" + field + ")" : ""));
        }
    }

    private void generateBusinessReject(Message message, int err, int field) throws FieldNotFound,
            IOException {

        MessageBuilder builder = messageFactory.getMessageBuilder(sessionID.getBeginString(),
                getSenderDefaultApplicationVersionID(), FixMessageTypes.BUSINESS_MESSAGE_REJECT);

        final Message reject = builder.create();
        final Header header = message.getHeader();
        reject.reverseRoute(header);
        initializeHeader(reject.getHeader());

        final String msgType = header.getString(FixTags.MSG_TYPE);
        final String msgSeqNum = header.getString(FixTags.MSG_SEQ_NUM);
        reject.setString(FixTags.REF_MSG_TYPE, msgType);
        reject.setString(FixTags.REF_SEQ_NUM, msgSeqNum);
        reject.setInt(FixTags.BUSINESS_REJECT_REASON, err);
        state.incrNextTargetMsgSeqNum();

        final String reason = BusinessRejectReasonText.getMessage(err);
        setRejectReason(reject, field, reason, field != 0);
        getLog().onErrorEvent(
                "Reject sent for Message " + msgSeqNum + (reason != null ? (": " + reason) : "")
                        + (field != 0 ? (": tag=" + field) : ""));

        sendRaw(reject, 0);
    }

    private void nextTestRequest(Message testRequest) throws FieldNotFound, RejectLogon,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType, IOException,
            InvalidMessage {

        if (!verify(testRequest)) {
            return;
        }
        generateHeartbeat(testRequest);
        state.incrNextTargetMsgSeqNum();
        nextQueued();
    }

    private void generateHeartbeat(Message testRequest) throws FieldNotFound {

        final Message heartbeat = createAdminMessage(sessionID.getBeginString(),
                FixMessageTypes.HEARTBEAT);
        initializeHeader(heartbeat.getHeader());
        if (testRequest.isFieldSet(FixTags.TEST_REQ_ID)) {
            heartbeat.setString(FixTags.TEST_REQ_ID, testRequest.getString(FixTags.TEST_REQ_ID));
        }
        if (enableLastMsgSeqNumProcessed) {
            heartbeat.getHeader().setInt(FixTags.LAST_MSG_SEQ_NUM_PROCESSED,
                    testRequest.getHeader().getInt(FixTags.MSG_SEQ_NUM));
        }

        sendRaw(heartbeat, 0);
    }

    private void nextHeartBeat(Message heartBeat) throws FieldNotFound, RejectLogon,
            IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType, IOException,
            InvalidMessage {

        if (!verify(heartBeat)) {
            return;
        }
        state.incrNextTargetMsgSeqNum();
        nextQueued();
    }

    private boolean verify(Message msg, boolean checkTooHigh, boolean checkTooLow)
            throws RejectLogon, FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
            UnsupportedMessageType, IOException {

        state.setLastReceivedTime(SystemTime.currentTimeMillis());
        state.clearTestRequestCounter();

        String msgType;
        try {
            final Message.Header header = msg.getHeader();
            msgType = header.getString(FixTags.MSG_TYPE);
            int msgSeqNum = 0;
            if (checkTooHigh || checkTooLow) {
                msgSeqNum = header.getInt(FixTags.MSG_SEQ_NUM);
            }

            if (!validLogonState(msgType)) {
                throw new SessionException("Logon state is not valid for message (MsgType="
                        + msgType + ")");
            }

            if (!isGoodTime(msg)) {
                doBadTime(msg);
                return false;
            }

            if (!isCorrectCompID(msg)) {
                doBadCompID(msg);
                return false;
            }

            if (checkTooHigh && isTargetTooHigh(msgSeqNum)) {
                doTargetTooHigh(msg);
                return false;
            } else if (checkTooLow && isTargetTooLow(msgSeqNum)) {
                doTargetTooLow(msg);
                return false;
            }

            // Handle poss dup where msgSeq is as expected
            // FIX 4.4 Vol 2, test case 2f&g
            if (isPossibleDuplicate(msg) && !validatePossDup(msg)) {
                return false;
            }

            if ((checkTooHigh) && state.isResendRequested()) {
                final ResendRange range;
                synchronized (state.getLock()) {
                    range = state.getResendRange();
                    if (msgSeqNum >= range.getEndSeqNo()) {
                        getLog().onEvent(
                                "ResendRequest for messages FROM " + range.getBeginSeqNo() + " TO "
                                        + range.getEndSeqNo() + " has been satisfied.");
                        state.setResendRange(0, 0, 0);
                    }
                }
                if (msgSeqNum < range.getEndSeqNo() && range.isChunkedResendRequest()
                        && msgSeqNum >= range.getCurrentEndSeqNo()) {
                    final FIXBeginString beginString = MessageUtils.getBeginString(header);
                    sendResendRequest(beginString, range.getEndSeqNo() + 1, msgSeqNum + 1,
                            range.getEndSeqNo());
                }
            }
        } catch (final FieldNotFound e) {
            throw e;
        } catch (final Exception e) {
            getLog().onErrorEvent(e.getClass().getName() + " " + e.getMessage());
            disconnect("Verifying message failed: " + e, true);
            return false;
        }

        fromCallback(msgType, msg, sessionID);
        return true;
    }

    private boolean doTargetTooLow(Message msg) throws FieldNotFound, IOException {

        if (!isPossibleDuplicate(msg)) {
            final int msgSeqNum = msg.getHeader().getInt(FixTags.MSG_SEQ_NUM);

            final String text = "MsgSeqNum too low, expecting " + getExpectedTargetNum()
                    + " but received " + msgSeqNum;
            generateLogout(text);
            throw new SessionException(text);
        }
        return validatePossDup(msg);
    }

    private void doBadCompID(Message msg) throws IOException, FieldNotFound {

        generateReject(msg, SessionRejectReasonText.COMPID_PROBLEM, 0);
        generateLogout();
    }

    private void doBadTime(Message msg) throws IOException, FieldNotFound {

        try {
            generateReject(msg, SessionRejectReasonText.SENDINGTIME_ACCURACY_PROBLEM, 0);
            generateLogout();
        } catch (final SessionException ex) {
            generateLogout(ex.getMessage());
            throw ex;
        }
    }

    private boolean isGoodTime(Message message) throws FieldNotFound {

        if (!checkLatency) {
            return true;
        }
        final Date sendingTime = message.getHeader().getUtcTimeStamp(FixTags.SENDING_TIME);
        return Math.abs(SystemTime.currentTimeMillis() - sendingTime.getTime()) / 1000 <= maxLatency;
    }

    private void fromCallback(String msgType, Message msg, SessionID sessionID2)
            throws RejectLogon, FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
            UnsupportedMessageType {

        // Application exceptions will prevent the incoming sequence number from
        // being incremented
        // and may result in resend requests and the next startup. This way, a
        // buggy application
        // can be fixed and then reprocess previously sent messages.
        // QFJ-572: Behaviour depends on the setting of flag
        // rejectMessageOnUnhandledException.
        if (MessageUtils.isAdminMessage(msgType)) {
            application.fromAdmin(msg, sessionID);
        } else {
            application.fromApp(msg, sessionID);
        }
    }

    private synchronized boolean validLogonState(String msgType) {

        if (msgType.equals(FixMessageTypes.LOGON) && state.isResetSent() || state.isResetReceived()) {
            return true;
        }
        if (msgType.equals(FixMessageTypes.LOGON) && !state.isLogonReceived()
                || !msgType.equals(FixMessageTypes.LOGON) && state.isLogonReceived()) {
            return true;
        }
        if (msgType.equals(FixMessageTypes.LOGOUT) && state.isLogonSent()) {
            return true;
        }
        if (!msgType.equals(FixMessageTypes.LOGOUT) && state.isLogoutSent()) {
            return true;
        }
        if (msgType.equals(FixMessageTypes.SEQUENCE_RESET)) {
            return true;
        }
        if (msgType.equals(FixMessageTypes.REJECT)) {
            return true;
        }
        return false;
    }

    private boolean verify(Message message) throws RejectLogon, FieldNotFound, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType, IOException {

        return verify(message, validateSequenceNumbers, validateSequenceNumbers);
    }

    /**
     * Called from the timer-related code in the acceptor/initiator
     * implementations. This is not typically called from application code.
     *
     * @throws IOException IO error
     */
    public void next() throws IOException {

        if (!isEnabled()) {
            if (isLoggedOn()) {
                if (!state.isLogoutSent()) {
                    getLog().onEvent("Initiated logout request");
                    generateLogout(state.getLogoutReason());
                }
            } else {
                return;
            }
        }

        if (sessionSchedule != null && !sessionSchedule.isNonStopSession()) {
            // Only check the session time once per second at most. It isn't
            // necessary to do for every message received.
            final long now = SystemTime.currentTimeMillis();
            if ((now - lastSessionTimeCheck) >= 1000L) {
                lastSessionTimeCheck = now;
                if (!isSessionTime()) {
                    if (state.isResetNeeded()) {
                        reset(); // only reset if seq nums are != 1
                    }
                    return; // since we are outside of session time window
                }

                resetIfSessionNotCurrent(sessionID, now);
            }
        }

        // Return if we are not connected
        if (!hasResponder()) {
            return;
        }

        if (!state.isLogonReceived()) {
            if (state.isLogonSendNeeded()) {
                if (isTimeToGenerateLogon()) {
                    // ApplicationExtended can prevent the automatic login
                    if (application instanceof ApplicationExtended) {
                        if (!((ApplicationExtended) application).canLogon(sessionID)) {
                            return;
                        }
                    }
                    if (generateLogon()) {
                        getLog().onEvent("Initiated logon request");
                    } else {
                        getLog().onErrorEvent("Error during logon request initiation");
                    }
                }
            } else if (state.isLogonAlreadySent() && state.isLogonTimedOut()) {
                disconnect("Timed out waiting for logon response", true);
            }
            return;
        }

        if (state.getHeartBeatInterval() == 0) {
            return;
        }

        if (state.isLogoutTimedOut()) {
            disconnect("Timed out waiting for logout response", true);
        }

        if (state.isTimedOut()) {
            if (!disableHeartBeatCheck) {
                disconnect("Timed out waiting for heartbeat", true);
                stateListener.onHeartBeatTimeout();
            } else {
                log.warn("Heartbeat failure detected but deactivated");
            }
        } else {
            if (state.isTestRequestNeeded()) {
                generateTestRequest("TEST");
                getLog().onEvent("Sent test request TEST");
                stateListener.onMissedHeartBeat();
            } else if (state.isHeartBeatNeeded()) {
                generateHeartbeat();
            }
        }
    }

    private long computeNextLogonDelayMillis() {

        int index = logonAttempts - 1;
        if (index < 0) {
            index = 0;
        }
        long secs;
        if (index >= logonIntervals.length) {
            secs = logonIntervals[logonIntervals.length - 1];
        } else {
            secs = logonIntervals[index];
        }
        return secs * 1000L;
    }

    private boolean isTimeToGenerateLogon() {

        return SystemTime.currentTimeMillis() - lastSessionLogon >= computeNextLogonDelayMillis();
    }

    public void generateHeartbeat() {

        final Message heartbeat = createAdminMessage(sessionID.getBeginString(),
                FixMessageTypes.HEARTBEAT);
        initializeHeader(heartbeat.getHeader());
        sendRaw(heartbeat, 0);
    }

    public void generateTestRequest(String id) {

        state.incrementTestRequestCounter();
        final Message testRequest = createAdminMessage(sessionID.getBeginString(),
                FixMessageTypes.TEST_REQUEST);
        initializeHeader(testRequest.getHeader());
        testRequest.setString(FixTags.TEST_REQ_ID, id);
        sendRaw(testRequest, 0);
    }

    private boolean generateLogon() throws IOException {

        final Message logon = createAdminMessage(sessionID.getBeginString(), FixMessageTypes.LOGON);
        logon.setInt(FixTags.ENCRYPT_METHOD, 0);
        logon.setInt(FixTags.HEART_BT_INT, state.getHeartBeatInterval());
        if (sessionID.isFIXT()) {
            logon.setInt(FixTags.DEFAULT_APPL_VER_ID, senderDefaultApplVerID.getId());
        }
        if (isStateRefreshNeeded(FixMessageTypes.LOGON)) {
            getLog().onEvent("Refreshing message/state store at logon");
            getStore().refresh();
            stateListener.onRefresh();
        }
        if (resetOnLogon) {
            resetState();
        }
        if (isResetNeeded()) {
            logon.setBoolean(FixTags.RESET_SEQ_NUM_FLAG, true);
        }
        state.setLastReceivedTime(SystemTime.currentTimeMillis());
        state.clearTestRequestCounter();
        state.setLogonSent(true);
        logonAttempts++;

        if (enableNextExpectedMsgSeqNum) {
            final int nextExpectedMsgNum = getExpectedTargetNum();
            logon.setInt(FixTags.NEXT_EXPECTED_MSG_SEQ_NUM, nextExpectedMsgNum);
            state.setLastExpectedLogonNextSeqNum(nextExpectedMsgNum);
        }
        return sendRaw(logon, 0);
    }

    /**
     * Use disconnect(reason, logError) instead.
     *
     * @deprecated
     */
    @Deprecated
    public void disconnect() throws IOException {

        disconnect("Other reason", true);
    }

    /**
     * Logs out from session and closes the network connection.
     *
     * @param reason the reason why the session is disconnected
     * @param logError set to true if this disconnection is an error
     * @throws IOException IO error
     */
    public void disconnect(String reason, boolean logError) throws IOException {

        try {
            synchronized (responderSync) {
                if (!hasResponder()) {
                    getLog().onEvent("Already disconnected: " + reason);
                    return;
                }
                final String msg = "Disconnecting: " + reason;
                if (logError) {
                    getLog().onErrorEvent(msg);
                } else {
                    log.info("[" + getSessionID() + "] " + msg);
                }
                responder.disconnect();
                setResponder(null);
            }

            final boolean logonReceived = state.isLogonReceived();
            final boolean logonSent = state.isLogonSent();
            if (logonReceived || logonSent) {
                try {
                    application.onLogout(sessionID);
                } catch (final Throwable t) {
                    logApplicationException("onLogout()", t);
                }

                stateListener.onLogout();
            }
            // QFJ-457 now enabled again if acceptor
            if (!state.isInitiator()) {
                setEnabled(true);
            }
        } finally {
            state.setLogonReceived(false);
            state.setLogonSent(false);
            state.setLogoutSent(false);
            state.setLogoutReceived(false);
            state.setResetReceived(false);
            state.setResetSent(false);
            state.clearQueue();
            state.clearLogoutReason();
            state.setResendRange(0, 0);

            if (resetOnDisconnect) {
                resetState();
            }
        }
    }

    private void nextLogon(Message logon) throws FieldNotFound, RejectLogon, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {

        // QFJ-357
        // If this check is not done here, the Logon would be accepted and
        // immediately followed by a Logout (due to check in Session.next()).
        if (!isSessionTime()) {
            throw new RejectLogon("Logon attempt not within session time");
        }

        if (isStateRefreshNeeded(FixMessageTypes.LOGON)) {
            getLog().onEvent("Refreshing message/state store at logon");
            getStore().refresh();
            stateListener.onRefresh();
        }

        if (logon.isFieldSet(FixTags.RESET_SEQ_NUM_FLAG)) {
            state.setResetReceived(logon.getBoolean(FixTags.RESET_SEQ_NUM_FLAG));
        } else if (state.isResetSent() && logon.getHeader().getInt(FixTags.MSG_SEQ_NUM) == 1) { // QFJ-383
            getLog().onEvent(
                    "Inferring ResetSeqNumFlag as sequence number is 1 in response to reset request");
            state.setResetReceived(true);
        }

        if (state.isResetReceived()) {
            getLog().onEvent("Logon contains ResetSeqNumFlag=Y, resetting sequence numbers to 1");
            if (!state.isResetSent()) {
                resetState();
            }
        }

        if (state.isLogonSendNeeded() && !state.isResetReceived()) {
            disconnect("Received logon response before sending request", true);
            return;
        }

        if (!state.isInitiator() && resetOnLogon) {
            resetState();
        }

        if (!verify(logon, false, validateSequenceNumbers)) {
            return;
        }

        // reset logout messages
        state.setLogoutReceived(false);
        state.setLogoutSent(false);
        state.setLogonReceived(true);

        // remember the expected sender sequence number of any logon response
        // for future use
        final int nextSenderMsgNumAtLogonReceived = state.getMessageStore()
                .getNextSenderMsgSeqNum();
        final int sequence = logon.getHeader().getInt(FixTags.MSG_SEQ_NUM);

        /*
         * We test here that it's not too high (which would result in a resend)
         * and that we are not resetting on logon 34=1
         */
        final boolean isLogonInNormalSequence = !(isTargetTooHigh(sequence) && !resetOnLogon);
        // if we have a tag 789 sent to us...
        if (logon.isFieldSet(FixTags.NEXT_EXPECTED_MSG_SEQ_NUM) && enableNextExpectedMsgSeqNum) {

            final int targetWantsNextSeqNumToBe = logon.getInt(FixTags.NEXT_EXPECTED_MSG_SEQ_NUM);
            final int actualNextNum = state.getMessageStore().getNextSenderMsgSeqNum();
            // Is the 789 we received too high ??
            if (targetWantsNextSeqNumToBe > actualNextNum) {
                // barf! we can't resend what we never sent! something
                // unrecoverable has happened.
                final String err = "Tag " + FixTags.NEXT_EXPECTED_MSG_SEQ_NUM
                        + " (NextExpectedMsgSeqNum) is higher than expected. Expected "
                        + actualNextNum + ", Received " + targetWantsNextSeqNumToBe;
                generateLogout(err);
                disconnect(err, true);
                return;
            }
        }
        getLog().onEvent("Received logon");
        if (!state.isInitiator()) {
            final int nextMsgFromTargetWeExpect = state.getMessageStore().getNextTargetMsgSeqNum();
            /*
             * If we got one too high they need messages resent use the first
             * message they missed (as we gap fill with that). If we reset on
             * logon, the current value will be 1 and we always send 2 (we
             * haven't inc'd for current message yet +1) If happy path (we
             * haven't inc'd for current message yet so its current +1)
             */
            int nextExpectedTargetNum = nextMsgFromTargetWeExpect;
            // we increment for the logon later (after Logon response sent) in
            // this method if and only if in sequence
            if (isLogonInNormalSequence) {
                // logon was fine take account of it in 789
                nextExpectedTargetNum++;
            }
            generateLogon(logon, nextExpectedTargetNum);
        }

        // Check for proper sequence reset response
        if (state.isResetSent() && !state.isResetReceived()) {
            disconnect("Received logon response before sending request", true);
        }

        state.setResetSent(false);
        state.setResetReceived(false);

        // Looking at the sequence number of the incoming Logon, is it too high
        // indicating possible missed messages ? ..
        if (!isLogonInNormalSequence) {
            // if 789 was sent then we effectively have already sent a resend
            // request
            if (state.isExpectedLogonNextSeqNumSent()) {
                // Mark state as if we have already sent a resend request from
                // the logon's 789 (we sent) to infinity.
                // This will supress the resend request in doTargetTooHigh ...
                state.setResetRangeFromLastExpectedLogonNextSeqNumLogon();
                getLog().onEvent("Required resend will be suppressed as we are setting tag 789");
            }
            if (validateSequenceNumbers) {
                doTargetTooHigh(logon);
            }
        } else {
            state.incrNextTargetMsgSeqNum();
            nextQueued();
        }

        // Do we have a 789
        if (logon.isFieldSet(FixTags.NEXT_EXPECTED_MSG_SEQ_NUM) && enableNextExpectedMsgSeqNum) {
            final int targetWantsNextSeqNumToBe = logon.getInt(FixTags.NEXT_EXPECTED_MSG_SEQ_NUM);
            final int actualNextNum = nextSenderMsgNumAtLogonReceived;

            // is the 789 lower (we checked for higher previously) than our next
            // message after receiving the logon
            if (targetWantsNextSeqNumToBe != actualNextNum) {
                int endSeqNo = actualNextNum;

                // Just do a gap fill when messages aren't persisted
                if (!persistMessages) {
                    endSeqNo += 1;
                    final int next = state.getNextSenderMsgSeqNum();
                    if (endSeqNo > next) {
                        endSeqNo = next;
                    }
                    getLog().onEvent(
                            "Received implicit ResendRequest via Logon FROM: "
                                    + targetWantsNextSeqNumToBe + " TO: " + actualNextNum
                                    + " will be reset");
                    generateSequenceReset(logon, targetWantsNextSeqNumToBe, // 34=
                            endSeqNo); // (NewSeqNo 36=)
                } else {
                    // resend missed messages
                    getLog().onEvent(
                            "Received implicit ResendRequest via Logon FROM: "
                                    + targetWantsNextSeqNumToBe + " TO: " + actualNextNum
                                    + " will be resent");
                    resendMessages(logon, targetWantsNextSeqNumToBe, endSeqNo);
                }
            }
        }
        if (isLoggedOn()) {
            try {
                application.onLogon(sessionID);
            } catch (final Throwable t) {
                logApplicationException("onLogon()", t);
            }
            stateListener.onLogon();
            lastSessionLogon = SystemTime.currentTimeMillis();
            logonAttempts = 0;
        }
    }

    private void resendMessages(Message receivedMessage, int beginSeqNo, int endSeqNo)
            throws IOException, FieldNotFound {

        final ArrayList<String> messages = new ArrayList<String>();
        try {
            state.get(beginSeqNo, endSeqNo, messages);
        } catch (final IOException e) {
            if (forceResendWhenCorruptedStore) {
                log.error("Cannot read messages from stores, resend HeartBeats", e);
                for (int i = beginSeqNo; i < endSeqNo; i++) {
                    final Message heartbeat = createAdminMessage(sessionID.getBeginString(),
                            FixMessageTypes.HEARTBEAT);
                    initializeHeader(heartbeat.getHeader());
                    heartbeat.getHeader().setInt(FixTags.MSG_SEQ_NUM, i);
                    messages.add(heartbeat.toString());
                }
            } else {
                throw e;
            }
        }

        int msgSeqNum = 0;
        int begin = 0;
        int current = beginSeqNo;

        for (final String message : messages) {
            final Message msg;
            try {
                // QFJ-626
                msg = parseMessage(message);
                msgSeqNum = msg.getHeader().getInt(FixTags.MSG_SEQ_NUM);
            } catch (final Exception e) {
                getLog().onErrorEvent(
                        "Error handling ResendRequest: failed to parse message (" + e.getMessage()
                                + "): " + message);
                // Note: a SequenceReset message will be generated to fill the
                // gap
                continue;
            }

            if ((current != msgSeqNum) && begin == 0) {
                begin = current;
            }

            final String msgType = msg.getHeader().getString(FixTags.MSG_TYPE);

            if (MessageUtils.isAdminMessage(msgType) && !forceResendWhenCorruptedStore) {
                if (begin == 0) {
                    begin = msgSeqNum;
                }
            } else {
                initializeResendFields(msg);
                if (resendApproved(msg)) {
                    if (begin != 0) {
                        generateSequenceReset(receivedMessage, begin, msgSeqNum);
                    }
                    getLog().onEvent("Resending Message: " + msgSeqNum);
                    send(msg.toString());
                    begin = 0;
                } else {
                    if (begin == 0) {
                        begin = msgSeqNum;
                    }
                }
            }
            current = msgSeqNum + 1;
        }
        if (enableNextExpectedMsgSeqNum) {
            if (begin != 0) {
                generateSequenceReset(receivedMessage, begin, msgSeqNum + 1);
            } else
                /*
                 * I've added an else here as I managed to fail this without it
                 * in a unit test, however the unit test data may not have been
                 * realistic to production on the other hand. Apart from the
                 * else
                 */
                generateSequenceResetIfNeeded(receivedMessage, beginSeqNo, endSeqNo, msgSeqNum);
        } else {
            if (begin != 0) {
                generateSequenceReset(receivedMessage, begin, msgSeqNum + 1);
            }
            generateSequenceResetIfNeeded(receivedMessage, beginSeqNo, endSeqNo, msgSeqNum);
        }
    }

    private void generateSequenceResetIfNeeded(Message receivedMessage, int beginSeqNo,
            int endSeqNo, int msgSeqNum) throws IOException, FieldNotFound {

        if (endSeqNo > msgSeqNum) {
            endSeqNo = endSeqNo + 1;
            final int next = state.getNextSenderMsgSeqNum();
            if (endSeqNo > next) {
                endSeqNo = next;
            }
            generateSequenceReset(receivedMessage, beginSeqNo, endSeqNo);
        }
    }

    private void nextQueued() throws FieldNotFound, RejectLogon, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {

        while (nextQueued(getExpectedTargetNum())) {
            // continue
        }
    }

    private boolean nextQueued(int num) throws FieldNotFound, RejectLogon, IncorrectDataFormat,
            IncorrectTagValue, UnsupportedMessageType, IOException, InvalidMessage {

        final Message msg = state.dequeue(num);
        if (msg != null) {
            getLog().onEvent("Processing queued message: " + num);

            final String msgType = msg.getHeader().getString(FixTags.MSG_TYPE);
            if (msgType.equals(FixMessageTypes.LOGON)
                    || msgType.equals(FixMessageTypes.RESEND_REQUEST)) {
                // Logon and ResendRequest processing has already been done, so
                // we just need to increment the target seqnum.
                state.incrNextTargetMsgSeqNum();
            } else {
                nextQueued(msg, msgType);
            }
            return true;
        }
        return false;
    }

    private void nextQueued(Message msg, String msgType) throws InvalidMessage, FieldNotFound,
            RejectLogon, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType,
            IOException {

        try {
            next(msg);
        } catch (final InvalidMessage e) {
            final String message = "Invalid message: " + e;
            if (FixMessageTypes.LOGON.equals(msgType)) {
                disconnect(message, true);
            } else {
                getLog().onErrorEvent(message);
                if (resetOrDisconnectIfRequired(null)) {
                    return;
                }
            }
            throw e;
        }
    }

    private void doTargetTooHigh(Message msg) throws FieldNotFound {

        final Message.Header header = msg.getHeader();
        final FIXBeginString beginString = MessageUtils.getBeginString(header);
        final int msgSeqNum = header.getInt(FixTags.MSG_SEQ_NUM);

        getLog().onEvent(
                "MsgSeqNum too high, expecting " + getExpectedTargetNum() + " but received "
                        + msgSeqNum + ": " + msg);
        enqueueMessage(msg, msgSeqNum);

        if (state.isResendRequested()) {
            final ResendRange range = state.getResendRange();

            if (!redundantResentRequestsAllowed && msgSeqNum >= range.getBeginSeqNo()) {
                getLog().onEvent(
                        "Already sent ResendRequest FROM: " + range.getBeginSeqNo() + " TO: "
                                + range.getEndSeqNo() + ".  Not sending another.");
                return;
            }
        }

        generateResendRequest(beginString, msgSeqNum);
    }

    private void generateResendRequest(FIXBeginString beginString, int msgSeqNum) {

        final int beginSeqNo = getExpectedTargetNum();
        final int endSeqNo = msgSeqNum - 1;
        sendResendRequest(beginString, msgSeqNum, beginSeqNo, endSeqNo);
    }

    private void sendResendRequest(FIXBeginString beginString, int msgSeqNum, int beginSeqNo,
            int endSeqNo) {

        int lastEndSeqNoSent = resendRequestChunkSize == 0 ? endSeqNo : beginSeqNo
                + resendRequestChunkSize - 1;
        if (lastEndSeqNoSent > endSeqNo) {
            lastEndSeqNoSent = endSeqNo;
        }
        if (lastEndSeqNoSent == endSeqNo && !useClosedRangeForResend) {
            if (beginString.gte(FIXBeginString.FIX42)) {
                endSeqNo = 0;
            } else if (beginString.lte(FIXBeginString.FIX41)) {
                endSeqNo = 999999;
            }
        } else {
            endSeqNo = lastEndSeqNoSent;
        }

        final Message resendRequest = createAdminMessage(beginString,
                FixMessageTypes.RESEND_REQUEST);
        resendRequest.setInt(FixTags.BEGIN_SEQ_NO, beginSeqNo);
        resendRequest.setInt(FixTags.END_SEQ_NO, endSeqNo);
        initializeHeader(resendRequest.getHeader());
        sendRaw(resendRequest, 0);
        getLog().onEvent("Sent ResendRequest FROM: " + beginSeqNo + " TO: " + lastEndSeqNoSent);
        state.setResendRange(beginSeqNo, msgSeqNum - 1, resendRequestChunkSize == 0
                ? 0
                : lastEndSeqNoSent);
    }

    private boolean validatePossDup(Message msg) throws FieldNotFound, IOException {

        final Message.Header header = msg.getHeader();
        final String msgType = header.getString(FixTags.MSG_TYPE);

        if (!msgType.equals(FixMessageTypes.SEQUENCE_RESET)) {
            if (header.isFieldSet(FixTags.ORIG_SENDING_TIME)) {
                final Date origSendingTime = header.getUtcTimeStamp(FixTags.ORIG_SENDING_TIME);
                final Date sendingTime = header.getUtcTimeStamp(FixTags.SENDING_TIME);
                if (origSendingTime.compareTo(sendingTime) > 0) {
                    generateReject(msg, SessionRejectReasonText.SENDINGTIME_ACCURACY_PROBLEM, 0);
                    generateLogout();
                    return false;
                }
            } else {
                // QFJ-703
                if (requiresOrigSendingTime) {
                    generateReject(msg, SessionRejectReasonText.REQUIRED_TAG_MISSING,
                            FixTags.ORIG_SENDING_TIME);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isTargetTooHigh(int sequence) throws IOException {

        return sequence > state.getNextTargetMsgSeqNum();
    }

    /**
     * Outgoing Logon in response to Logon received
     *
     * @param otherLogon the one we are responding to with a Logon (response)
     * @param expectedTargetNum value for 789 tag (used only if enabled in
     *        properties)
     * @throws FieldNotFound expected message field of Logon not present.
     */
    private void generateLogon(Message otherLogon, int expectedTargetNum) throws FieldNotFound {

        final Message logon = createAdminMessage(sessionID.getBeginString(), FixMessageTypes.LOGON);
        logon.setInt(FixTags.ENCRYPT_METHOD, 0);
        if (state.isResetReceived()) {
            logon.setBoolean(FixTags.RESET_SEQ_NUM_FLAG, true);
        }
        logon.setInt(FixTags.HEART_BT_INT, otherLogon.getInt(FixTags.HEART_BT_INT));
        if (sessionID.isFIXT()) {
            logon.setInt(FixTags.DEFAULT_APPL_VER_ID, senderDefaultApplVerID.getId());
        }
        if (enableLastMsgSeqNumProcessed) {
            logon.getHeader().setInt(FixTags.LAST_MSG_SEQ_NUM_PROCESSED,
                    otherLogon.getHeader().getInt(FixTags.MSG_SEQ_NUM));
        }
        initializeHeader(logon.getHeader());

        if (enableNextExpectedMsgSeqNum) {
            getLog().onEvent("Responding to Logon request with tag 789=" + expectedTargetNum);
            logon.setInt(FixTags.NEXT_EXPECTED_MSG_SEQ_NUM, expectedTargetNum);
            state.setLastExpectedLogonNextSeqNum(expectedTargetNum);
        } else {
            getLog().onEvent("Responding to Logon request");
        }
        sendRaw(logon, 0);
        state.setLogonSent(true);
    }

    /**
     * Send the message
     *
     * @param message is the message to send
     * @param num is the seq num of the message to send, if 0, the next expected
     *        sender seqnum is used.
     * @return
     */
    private boolean sendRaw(Message message, int num) {

        // sequence number must be locked until application
        // callback returns since it may be effectively rolled
        // back if the callback fails.
        state.lockSenderMsgSeqNum();
        try {
            boolean result = false;
            final Message.Header header = message.getHeader();
            final String msgType = header.getString(FixTags.MSG_TYPE);

            initializeHeader(header);

            if (num > 0) {
                header.setInt(FixTags.MSG_SEQ_NUM, num);
            }

            if (enableLastMsgSeqNumProcessed) {
                if (!header.isFieldSet(FixTags.LAST_MSG_SEQ_NUM_PROCESSED)) {
                    header.setInt(FixTags.LAST_MSG_SEQ_NUM_PROCESSED, getExpectedTargetNum() - 1);
                }
            }

            String messageString;

            if (message.isAdmin()) {
                try {
                    application.toAdmin(message, sessionID);
                } catch (final Throwable t) {
                    logApplicationException("toAdmin()", t);
                }

                if (msgType.equals(FixMessageTypes.LOGON)) {
                    if (!state.isResetReceived()) {
                        boolean resetSeqNumFlag = false;
                        if (message.isFieldSet(FixTags.RESET_SEQ_NUM_FLAG)) {
                            resetSeqNumFlag = message.getBoolean(FixTags.RESET_SEQ_NUM_FLAG);
                        }
                        if (resetSeqNumFlag) {
                            resetState();
                            message.getHeader().setInt(FixTags.MSG_SEQ_NUM, getExpectedSenderNum());
                        }
                        state.setResetSent(resetSeqNumFlag);
                    }
                }

                messageString = message.toString();
                if (msgType.equals(FixMessageTypes.LOGON) || msgType.equals(FixMessageTypes.LOGOUT)
                        || msgType.equals(FixMessageTypes.RESEND_REQUEST)
                        || msgType.equals(FixMessageTypes.SEQUENCE_RESET) || isLoggedOn()) {
                    result = send(messageString);
                }
            } else {
                try {
                    application.toApp(message, sessionID);
                } catch (final DoNotSend e) {
                    return false;
                } catch (final Throwable t) {
                    logApplicationException("toApp()", t);
                }
                messageString = message.toString();
                if (isLoggedOn()) {
                    result = send(messageString);
                }
            }

            if (num == 0) {
                final int msgSeqNum = header.getInt(FixTags.MSG_SEQ_NUM);
                if (persistMessages) {
                    state.set(msgSeqNum, messageString);
                }
                state.incrNextSenderMsgSeqNum();
            }

            return result;
        } catch (final IOException e) {
            logThrowable(getLog(), "Error Reading/Writing in MessageStore", e);
            return false;
        } catch (final FieldNotFound e) {
            logThrowable(state.getLog(), "Error accessing message fields", e);
            return false;
        } finally {
            state.unlockSenderMsgSeqNum();
        }
    }

    private void enqueueMessage(final Message msg, final int msgSeqNum) {

        state.enqueue(msgSeqNum, msg);
        getLog().onEvent("Enqueued at pos " + msgSeqNum + ": " + msg);
    }

    private void resetState() {

        state.reset();
        stateListener.onReset();
    }

    /**
     * Send a message to a counterparty. Sequence numbers and information about
     * the sender and target identification will be added automatically (or
     * overwritten if that information already is present). The returned status
     * flag is included for compatibility with the JNI API but it's usefulness
     * is questionable. In QuickFIX/J, the message is transmitted using
     * asynchronous network I/O so the boolean only indicates the message was
     * successfully queued for transmission. An error could still occur before
     * the message data is actually sent.
     *
     * @param message the message to send
     * @return a status flag indicating whether the write to the network layer
     *         was successful.
     */
    public boolean send(Message message) {

        message.getHeader().removeField(FixTags.POSS_DUP_FLAG);
        message.getHeader().removeField(FixTags.ORIG_SENDING_TIME);
        return sendRaw(message, 0);
    }

    private boolean send(String messageString) {

        getLog().onOutgoing(messageString);
        Responder responder;
        synchronized (responderSync) {
            responder = this.responder;
        }
        if (responder == null) {
            getLog().onEvent("No responder, not sending message: " + messageString);
            return false;
        }
        return responder.send(messageString);
    }

    private boolean isCorrectCompID(Message message) throws FieldNotFound {

        if (!checkCompID) {
            return true;
        }
        final String senderCompID = message.getHeader().getString(FixTags.SENDER_COMP_ID);
        final String targetCompID = message.getHeader().getString(FixTags.TARGET_COMP_ID);
        return sessionID.getSenderCompID().equals(targetCompID)
                && sessionID.getTargetCompID().equals(senderCompID);
    }

    /**
     * Set the data dictionary. (QF Compatibility)
     *
     * @deprecated
     * @param dataDictionary
     */
    @Deprecated
    public void setDataDictionary(DataDictionary dataDictionary) {

        throw new UnsupportedOperationException(
                "Modification of session dictionary is not supported in QFJ");
    }

    public DataDictionary getDataDictionary() {

        if (!sessionID.isFIXT()) {
            // For pre-FIXT sessions, the session data dictionary is the same as
            // the application
            // data dictionary.
            return dataDictionaryProvider.getSessionDataDictionary(sessionID.getBeginString());
        }

        throw new SessionException("No default data dictionary for FIXT 1.1 and newer");
    }

    public DataDictionaryProvider getDataDictionaryProvider() {

        return dataDictionaryProvider;
    }

    public SessionID getSessionID() {

        return sessionID;
    }

    /**
     * Predicate for determining if the session should be active at the current
     * time.
     *
     * @return true if session should be active, false otherwise.
     */
    public boolean isSessionTime() {

        return sessionSchedule == null || sessionSchedule.isSessionTime();
    }

    /**
     * Determine if a session exists with the given ID.
     *
     * @param sessionID
     * @return true if session exists, false otherwise.
     */
    public static boolean doesSessionExist(SessionID sessionID) {

        return sessions.containsKey(sessionID);
    }

    /**
     * Return the session count.
     *
     * @return the number of sessions
     */
    public static int numSessions() {

        return sessions.size();
    }

    /**
     * Sets the timeout for waiting for a logon response.
     *
     * @param seconds the timeout in seconds
     */
    public void setLogonTimeout(int seconds) {

        state.setLogonTimeout(seconds);
    }

    /**
     * Sets the timeout for waiting for a logout response.
     *
     * @param seconds the timeout in seconds
     */
    public void setLogoutTimeout(int seconds) {

        state.setLogoutTimeout(seconds);
    }

    /**
     * Internal use by acceptor code.
     *
     * @param heartbeatInterval
     */
    public void setHeartBeatInterval(int heartbeatInterval) {

        state.setHeartBeatInterval(heartbeatInterval);
    }

    public boolean getCheckCompID() {

        return checkCompID;
    }

    public int getLogonTimeout() {

        return state.getLogonTimeout();
    }

    public int getLogoutTimeout() {

        return state.getLogoutTimeout();
    }

    public boolean getRedundantResentRequestsAllowed() {

        return redundantResentRequestsAllowed;
    }

    public boolean getRefreshOnLogon() {

        return refreshMessageStoreAtLogon;
    }

    public boolean getResetOnDisconnect() {

        return resetOnDisconnect;
    }

    public boolean getResetOnLogout() {

        return resetOnLogout;
    }

    public boolean isLogonAlreadySent() {

        return state.isLogonAlreadySent();
    }

    public boolean isLogonReceived() {

        return state.isLogonReceived();
    }

    public boolean isLogonSendNeeded() {

        return state.isLogonSendNeeded();
    }

    public boolean isLogonSent() {

        return state.isLogonSent();
    }

    public boolean isLogonTimedOut() {

        return state.isLogonTimedOut();
    }

    public boolean isLogoutReceived() {

        return state.isLogoutReceived();
    }

    public boolean isLogoutSent() {

        return state.isLogoutSent();
    }

    public boolean isLogoutTimedOut() {

        return state.isLogoutTimedOut();
    }

    public boolean isUsingDataDictionary() {

        return dataDictionaryProvider != null;
    }

    public Date getStartTime() throws IOException {

        return state.getCreationTime();
    }

    public double getTestRequestDelayMultiplier() {

        return state.getTestRequestDelayMultiplier();
    }

    @Override
    public String toString() {

        String s = sessionID.toString();
        try {
            s += "[in:" + state.getNextTargetMsgSeqNum() + ",out:" + state.getNextSenderMsgSeqNum()
                    + "]";
        } catch (final IOException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        }
        return s;
    }

    public void addStateListener(SessionStateListener listener) {

        stateListeners.addListener(listener);
    }

    public void removeStateListener(SessionStateListener listener) {

        stateListeners.removeListener(listener);
    }

    /**
     * @return the default application version ID for messages sent from this
     *         session
     */
    public FIXApplication getSenderDefaultApplicationVersionID() {

        return senderDefaultApplVerID;
    }

    /**
     * @return the default application version ID for messages received by this
     *         session
     */
    public FIXApplication getTargetDefaultApplicationVersionID() {

        return targetDefaultApplVerID.get();
    }

    /**
     * Sets the default application version ID for messages received by this
     * session. This is called by the AcceptorIoHandler upon reception of a
     * Logon message and should not be called by user code.
     *
     * @param applVerID
     */
    public void setTargetDefaultApplicationVersionID(FIXApplication applVerID) {

        targetDefaultApplVerID.set(applVerID);
    }

    private static String extractNumber(String txt, int from) {

        String ret = "";
        for (int i = from; i != txt.length(); ++i) {
            final char c = txt.charAt(i);
            if (c >= '0' && c <= '9') {
                ret += c;
            } else {
                if (ret.length() != 0) {
                    break;
                }
            }
        }
        return ret.trim();
    }

    protected static Integer extractExpectedSequenceNumber(String txt) {

        if (txt == null) {
            return null;
        }
        String keyword = "expecting";
        int pos = txt.indexOf(keyword);
        if (pos < 0) {
            keyword = "expected";
            pos = txt.indexOf("expected");
        }
        if (pos < 0) {
            return null;
        }
        final int from = pos + keyword.length();
        final String val = extractNumber(txt, from);
        if (val.length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(val);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    public void setIgnoreHeartBeatFailure(boolean ignoreHeartBeatFailure) {

        disableHeartBeatCheck = ignoreHeartBeatFailure;
    }

    public void setRejectInvalidMessage(boolean rejectInvalidMessage) {

        this.rejectInvalidMessage = rejectInvalidMessage;
    }

    public void setRejectMessageOnUnhandledException(boolean rejectMessageOnUnhandledException) {

        this.rejectMessageOnUnhandledException = rejectMessageOnUnhandledException;
    }

    public void setRequiresOrigSendingTime(boolean requiresOrigSendingTime) {

        this.requiresOrigSendingTime = requiresOrigSendingTime;
    }

    public void setForceResendWhenCorruptedStore(boolean forceResendWhenCorruptedStore) {

        this.forceResendWhenCorruptedStore = forceResendWhenCorruptedStore;
    }

    public boolean isAllowedForSession(InetAddress remoteInetAddress) {

        return allowedRemoteAddresses == null || allowedRemoteAddresses.isEmpty()
                || allowedRemoteAddresses.contains(remoteInetAddress);
    }

    /**
     * Closes session resources. This is for internal use and should typically
     * not be called by an user application.
     */
    @Override
    public void close() throws IOException {

        closeIfCloseable(getLog());
        closeIfCloseable(getStore());
    }

    private void closeIfCloseable(Object resource) throws IOException {

        if (resource instanceof Closeable) {
            ((Closeable) resource).close();
        }
    }

    private void resetIfSessionNotCurrent(SessionID sessionID, long time) throws IOException {

        if (!isCurrentSession(time)) {
            getLog().onEvent("Session state is not current; resetting " + sessionID);
            reset();
        }
    }

}
