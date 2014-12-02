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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.quickfixj.CharsetSupport;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXDataDictionary;
import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXGroup;
import org.quickfixj.FIXGroupField;
import org.quickfixj.FIXMessage;
import org.quickfixj.MessageBuilder;
import org.quickfixj.MessageBuilderFactory;
import org.quickfixj.QFJException;

import quickfix.Message.Header;
import quickfix.field.converter.IntConverter;

public class MessageUtils {

    private static final char FIELD_SEPARATOR = '\001';

    private static final Map<FIXApplication, FIXBeginString> APPL_VER_ID_TO_BEGIN_STRING;

    private static final Map<FIXBeginString, FIXApplication> BEGIN_STRING_TO_APPL_VER_ID;

    static {

        // populate maps with supported FIX versions
        // No support for earlier versions of FIX
        APPL_VER_ID_TO_BEGIN_STRING = new HashMap<FIXApplication, FIXBeginString>();
        BEGIN_STRING_TO_APPL_VER_ID = new HashMap<FIXBeginString, FIXApplication>();

        addSupportedVersion(FIXApplication.FIX40, FIXBeginString.FIX40);
        addSupportedVersion(FIXApplication.FIX41, FIXBeginString.FIX41);
        addSupportedVersion(FIXApplication.FIX42, FIXBeginString.FIX42);
        addSupportedVersion(FIXApplication.FIX43, FIXBeginString.FIX43);
        addSupportedVersion(FIXApplication.FIX44, FIXBeginString.FIX44);
        addSupportedVersion(FIXApplication.FIX50, FIXBeginString.FIXT11);
        addSupportedVersion(FIXApplication.FIX50SP1, FIXBeginString.FIXT11);
        addSupportedVersion(FIXApplication.FIX50SP2, FIXBeginString.FIXT11);
    }

    private MessageUtils() {
        //prevent construction
    }

    private static void addSupportedVersion(FIXApplication applVerID, FIXBeginString beginString) {
        APPL_VER_ID_TO_BEGIN_STRING.put(applVerID, beginString);
        BEGIN_STRING_TO_APPL_VER_ID.put(beginString, applVerID);
    }

    public static SessionID getSessionID(Message fixMessage) {

        final Header header = fixMessage.getHeader();
        return new SessionID(getBeginString(header), getFieldOrDefault(header,
                FixTags.SENDER_COMP_ID, null), getFieldOrDefault(header, FixTags.SENDER_SUB_ID,
                null), getFieldOrDefault(header, FixTags.SENDER_LOCATION_ID, null),
                getFieldOrDefault(header, FixTags.TARGET_COMP_ID, null), getFieldOrDefault(header,
                        FixTags.TARGET_SUB_ID, null), getFieldOrDefault(header,
                        FixTags.TARGET_LOCATION_ID, null), null);
    }

    public static SessionID getSessionID(String messageString) {

        return new SessionID(getBeginString(messageString), getStringField(messageString,
                FixTags.SENDER_COMP_ID), getStringField(messageString, FixTags.SENDER_SUB_ID),
                getStringField(messageString, FixTags.SENDER_LOCATION_ID), getStringField(
                        messageString, FixTags.TARGET_COMP_ID), getStringField(messageString,
                        FixTags.TARGET_SUB_ID), getStringField(messageString,
                        FixTags.TARGET_LOCATION_ID), null);
    }

    public static SessionID getReverseSessionID(Message fixMessage) {

        final Header header = fixMessage.getHeader();
        return new SessionID(getBeginString(header), getFieldOrDefault(header,
                FixTags.TARGET_COMP_ID, null), getFieldOrDefault(header, FixTags.TARGET_SUB_ID,
                null), getFieldOrDefault(header, FixTags.TARGET_LOCATION_ID, null),
                getFieldOrDefault(header, FixTags.SENDER_COMP_ID, null), getFieldOrDefault(header,
                        FixTags.SENDER_SUB_ID, null), getFieldOrDefault(header,
                        FixTags.SENDER_LOCATION_ID, null), null);
    }

    public static SessionID getReverseSessionID(String messageString) {

        return new SessionID(getBeginString(messageString), getStringField(messageString,
                FixTags.TARGET_COMP_ID), getStringField(messageString, FixTags.TARGET_SUB_ID),
                getStringField(messageString, FixTags.TARGET_LOCATION_ID), getStringField(
                        messageString, FixTags.SENDER_COMP_ID), getStringField(messageString,
                        FixTags.SENDER_SUB_ID), getStringField(messageString,
                        FixTags.SENDER_LOCATION_ID), null);
    }

    private static String getFieldOrDefault(FieldMap fields, int tag, String defaultValue) {

        if (fields.isFieldSet(tag)) {
            try {
                return fields.getString(tag);
            } catch (final FieldNotFound e) {
                // ignore, should never happen
                return null;
            }
        }

        return defaultValue;
    }

    //    /**
    //     * Utility method for parsing a mesasge. This should only be used for
    //     * parsing messages from FIX versions 4.4 or earlier.
    //     *
    //     * @param messageFactory
    //     * @param dataDictionary
    //     * @param messageString
    //     * @return the parsed message
    //     * @throws InvalidMessage
    //     */
    //    public static Message parse(MessageFactory messageFactory, DataDictionary dataDictionary,
    //            String messageString) throws InvalidMessage {
    //
    //        final int index = messageString.indexOf(FIELD_SEPARATOR);
    //        if (index < 0) {
    //            throw new InvalidMessage("Message does not contain any field separator");
    //        }
    //        final String beginString = messageString.substring(2, index);
    //        final String messageType = getMessageType(messageString);
    //        final quickfix.Message message = messageFactory.create(beginString, messageType);
    //        message.fromString(messageString, dataDictionary, dataDictionary != null);
    //        return message;
    //    }

    /**
     * NOTE: This method is intended for internal use.
     *
     * @param session the Session that will process the message
     * @param messageString
     * @return the parsed message
     * @throws InvalidMessage
     */
    public static Message parse(Session session, String messageString) throws InvalidMessage {

        final FIXBeginString beginString = getBeginString(messageString);
        final String msgType = getMessageType(messageString);

        FIXApplication applVerID;

        if (FIXBeginString.FIXT11 == beginString) {
            applVerID = getApplVerID(session, messageString);
        } else {
            applVerID = toApplVerID(beginString);
        }

        final MessageBuilderFactory messageFactory = session.getMessageFactory();

        final DataDictionaryProvider ddProvider = session.getDataDictionaryProvider();
        final DataDictionary sessionDataDictionary = ddProvider == null ? null : ddProvider
                .getSessionDataDictionary(beginString);
        final DataDictionary applicationDataDictionary = ddProvider == null ? null : ddProvider
                .getApplicationDataDictionary(applVerID);

        MessageBuilder builder = messageFactory.getMessageBuilder(beginString, applVerID, msgType);

        //       final quickfix.Message message = messageFactory.create(beginString, msgType);
        final DataDictionary payloadDictionary = MessageUtils.isAdminMessage(msgType)
                ? sessionDataDictionary
                : applicationDataDictionary;

        Parser parser = new Parser(builder, messageString);

        return parser.parse(sessionDataDictionary, payloadDictionary, payloadDictionary != null);
    }

    public static Message parse(Message message, String messageData, DataDictionary dd,
            boolean doValidation) throws InvalidMessage {

        MessageBuilder builder = new DefaultMessageBuilder();

        Parser parser = new Parser(builder, messageData, message);

        return parser.parse(dd, dd, doValidation);
    }

    public static Message parse(Message message, String messageData,
            DataDictionary sessionDictionary, DataDictionary applicationDictionary,
            boolean doValidation) throws InvalidMessage {

        if (sessionDictionary.isAdminMessage(MessageUtils.getMessageType(messageData))) {
            applicationDictionary = sessionDictionary;
        }

        MessageBuilder builder = new DefaultMessageBuilder();

        Parser parser = new Parser(builder, messageData, message);

        return parser.parse(sessionDictionary, applicationDictionary, doValidation);
    }

    private static FIXApplication getApplVerID(Session session, String messageString)
            throws InvalidMessage {

        FIXApplication applVerID = null;

        final String applVerIdString = getStringField(messageString, FixTags.APPL_VER_ID);
        if (applVerIdString != null) {
            applVerID = FIXApplication.parseId(applVerIdString);
        }

        if (applVerID == null) {
            applVerID = session.getTargetDefaultApplicationVersionID();
        }

        if (applVerID == null && isLogon(messageString)) {
            final String defaultApplVerIdString = getStringField(messageString,
                    FixTags.DEFAULT_APPL_VER_ID);
            if (defaultApplVerIdString != null) {
                applVerID = FIXApplication.parseId(defaultApplVerIdString);
            }
        }

        if (applVerID == null) {
            throw new InvalidMessage("Can't determine ApplVerID for message");
        }

        return applVerID;
    }

    public static boolean isAdminMessage(String msgType) {

        return msgType.length() == 1 && "0A12345".contains(msgType);
    }

    public static boolean isHeartbeat(String message) {

        return isMessageType(message, FixMessageTypes.HEARTBEAT);
    }

    public static boolean isLogon(String message) {

        return isMessageType(message, FixMessageTypes.LOGON);
    }

    private static boolean isMessageType(String message, String msgType) {

        try {
            return msgType.equals(getMessageType(message));
        } catch (final InvalidMessage e) {
            return false;
        }
    }

    public static String getMessageType(String messageString) throws InvalidMessage {

        final String value = getStringField(messageString, 35);
        if (value == null) {
            throw new InvalidMessage("Missing or garbled message type in " + messageString);
        }
        return value;
    }

    public static FIXBeginString getBeginString(String messageString) {

        String value = getStringField(messageString, FixTags.BEGIN_STRING);

        return FIXBeginString.parse(value);
    }

    public static FIXBeginString getBeginString(Header header) {

        String value = getFieldOrDefault(header, FixTags.BEGIN_STRING, null);

        return FIXBeginString.parse(value);
    }

    public static String getStringField(String messageString, int tag) {

        String value = null;
        final String tagString = Integer.toString(tag);
        int start = messageString.indexOf(tagString, 0);
        while (start != -1 && value == null) {
            if ((start == 0 || messageString.charAt(start - 1) == FIELD_SEPARATOR)) {
                int end = start + tagString.length();
                if ((end + 1) < messageString.length() && messageString.charAt(end) == '=') {
                    // found tag, get value
                    start = end = (end + 1);
                    while (end < messageString.length()
                            && messageString.charAt(end) != FIELD_SEPARATOR) {
                        end++;
                    }
                    if (end == messageString.length()) {
                        return null;
                    } else {
                        value = messageString.substring(start, end);
                    }
                }
            }
            start = messageString.indexOf(tagString, start + 1);
        }
        return value;
    }

    /**
     * Convert an Appl Ver ID to a "begin string"
     *
     * @param appl Ver ID
     * @return the begin string for the specified Appl Ver ID.
     * @throws QFJException if conversion fails.
     */
    public static FIXBeginString toBeginString(FIXApplication applVerID) throws QFJException {

        final FIXBeginString beginString = APPL_VER_ID_TO_BEGIN_STRING.get(applVerID);
        if (beginString == null) {
            throw new QFJException("Unknown or unsupported ApplVerID: " + applVerID);
        }
        return beginString;
    }

    /**
     * Convert a begin string to a valid 'Appl Ver ID' value.
     *
     * @param beginString
     * @return the 'Appl Ver ID' for the specified begin string.
     * @throws QFJException if conversion fails.
     */
    public static FIXApplication toApplVerID(FIXBeginString beginString) throws QFJException {

        final FIXApplication applVerID = BEGIN_STRING_TO_APPL_VER_ID.get(beginString);
        if (applVerID == null) {
            throw new QFJException("Can't convert to ApplVerID: " + beginString);
        }
        return applVerID;
    }

    /**
     * Calculates the checksum for the given data.
     *
     * @param charset the charset used in encoding the data
     * @param data the data to calculate the checksum on
     * @param isEntireMessage specifies whether the data is an entire message;
     *        if true, and it ends with a checksum field, that checksum field is
     *        excluded from the current checksum calculation
     * @return the calculated checksum
     */
    public static int checksum(Charset charset, String data, boolean isEntireMessage) {

        int sum = 0;
        if (CharsetSupport.isStringEquivalent(charset)) { // optimization - skip
                                                          // encoding
            int end = isEntireMessage ? data.lastIndexOf("\00110=") : -1;
            int len = end > -1 ? end + 1 : data.length();
            for (int i = 0; i < len; i++) {
                sum += data.charAt(i);
            }
        } else {
            byte[] bytes = data.getBytes(charset);
            int len = bytes.length;
            if (isEntireMessage && bytes[len - 8] == '\001' && bytes[len - 7] == '1'
                    && bytes[len - 6] == '0' && bytes[len - 5] == '=')
                len = len - 7;
            for (int i = 0; i < len; i++) {
                sum += (bytes[i] & 0xFF);
            }
        }
        return sum & 0xFF; // better than sum % 256 since it avoids overflow
                           // issues
    }

    /**
     * Calculates the checksum for the given message (excluding existing
     * checksum field, if one exists). The {@link CharsetSupport#setCharset
     * global charset} is used.
     *
     * @param message the message to calculate the checksum on
     * @return the calculated checksum
     */
    public static int checksum(String message) {

        return checksum(CharsetSupport.getCharsetInstance(), message, true);
    }

    /**
     * Calculates the length of the byte representation of the given string in
     * the given charset.
     *
     * @param charset the charset used in encoding the data
     * @param data the data to calculate the length on
     * @return the calculated length
     */
    public static int length(Charset charset, String data) {

        return CharsetSupport.isStringEquivalent(charset)
                ? data.length()
                : data.getBytes(charset).length;
    }

    private static class Parser {

        private final MessageBuilder builder;
        private final String messageData;
        private final FIXMessage message;

        private int position;
        private FIXField<?> pushedBackField;

        Parser(MessageBuilder builder, String messageData) {
            this(builder, messageData, builder.create());
        }

        Parser(MessageBuilder builder, String messageData, FIXMessage message) {
            this.builder = builder;
            this.messageData = messageData;
            this.message = message;
        }

        public Message parse(DataDictionary sessionDataDictionary,
                DataDictionary applicationDataDictionary, boolean doValidation)
                throws InvalidMessage {

            try {
                parseHeader(sessionDataDictionary, doValidation);
                parseBody(applicationDataDictionary, doValidation);
                parseTrailer(sessionDataDictionary);
                if (doValidation) {
                    validateCheckSum(messageData);
                }
            } catch (final FieldException e) {
                // TODO - shouldn't really be storing exceptions
                ((Message) message).setException(e);
            }

            return (Message) message;
        }

        private void parseHeader(DataDictionary dd, boolean doValidation) throws InvalidMessage {

            if (doValidation) {
                final boolean validHeaderFieldOrder = isNextField(dd, message.getHeader(),
                        FixTags.BEGIN_STRING)
                        && isNextField(dd, message.getHeader(), FixTags.BODY_LENGTH)
                        && isNextField(dd, message.getHeader(), FixTags.MSG_TYPE);
                if (!validHeaderFieldOrder) {
                    // Invalid message preamble (first three fields) is a serious
                    // condition and is handled differently from other message
                    // parsing errors.
                    throw new InvalidMessage("Header fields out of order in " + messageData);
                }
            }

            FIXField<?> field = extractField(DataDictionary.HEADER_ID, dd, message.getHeader());

            while (field != null && isHeaderField(field, dd)) {
                message.getHeader().setField(field);

                if (dd != null && dd.isGroup(DataDictionary.HEADER_ID, field.getTag())) {
                    parseGroup(DataDictionary.HEADER_ID, field, dd, (Header) message.getHeader());
                }

                field = extractField(null, dd, message.getHeader());
            }
            pushBack(field);
        }

        private void parseBody(DataDictionary dd, boolean doValidation) throws InvalidMessage {

            FIXField<?> field = extractField(message.getMsgType(), dd, message);

            while (field != null) {
                if (isTrailerField(field.getTag())) {
                    pushBack(field);
                    return;
                }

                if (isHeaderField(field)) {
                    // An acceptance test requires the sequence number to
                    // be available even if the related field is out of order
                    setField(message.getHeader(), field);
                    // Group case
                    if (dd != null && dd.isGroup(DataDictionary.HEADER_ID, field.getTag())) {
                        parseGroup(DataDictionary.HEADER_ID, field, dd,
                                (FieldMap) message.getHeader());
                    }
                    if (doValidation && dd != null && dd.isCheckFieldsOutOfOrder())
                        throw new FieldException(
                                SessionRejectReasonText.TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER,
                                field.getTag());
                } else {
                    setField(message, field);
                    // Group case
                    if (dd != null && dd.isGroup(message.getMsgType(), field.getTag())) {
                        parseGroup(message.getMsgType(), field, dd, (FieldMap) message);
                    }
                }

                field = extractField(message.getMsgType(), dd, message);
            }
        }

        private void parseTrailer(DataDictionary dd) throws InvalidMessage {

            FIXField<?> field = extractField(null, dd, message.getTrailer());
            while (field != null) {
                if (!isTrailerField(field, dd)) {
                    throw new FieldException(
                            SessionRejectReasonText.TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER,
                            field.getTag());
                }
                setField(message.getTrailer(), field);
                field = extractField(null, dd, message.getTrailer());
            }
        }

        private void setField(FIXFieldGraph fields, FIXField<?> field) {

            if (fields.isFieldSet(field.getTag())) {
                throw new FieldException(SessionRejectReasonText.TAG_APPEARS_MORE_THAN_ONCE,
                        field.getTag());
            }
            fields.setField(field);
        }

        private boolean isNextField(DataDictionary dd, FIXFieldGraph fields, int tag)
                throws InvalidMessage {

            final FIXField<?> field = extractField(null, dd, message.getHeader());
            if (field == null || field.getTag() != tag) {
                return false;
            }
            fields.setField(field);
            return true;
        }

        private FIXField<?> extractField(String msgType, FIXDataDictionary dataDictionary,
                FIXFieldGraph fields) throws InvalidMessage {

            if (pushedBackField != null) {
                final FIXField<?> f = pushedBackField;
                pushedBackField = null;
                return f;
            }

            if (position >= messageData.length()) {
                return null;
            }

            final int equalsOffset = messageData.indexOf('=', position);
            if (equalsOffset == -1) {
                throw new InvalidMessage("Equal sign not found in field" + " in " + messageData);
            }

            int tag;
            try {
                tag = Integer.parseInt(messageData.substring(position, equalsOffset));
            } catch (final NumberFormatException e) {
                position = messageData.indexOf('\001', position + 1) + 1;
                throw new InvalidMessage("Bad tag format: " + e.getMessage() + " in " + messageData);
            }

            int sohOffset = messageData.indexOf('\001', equalsOffset + 1);
            if (sohOffset == -1) {
                throw new InvalidMessage("SOH not found at end of field: " + tag + " in "
                        + messageData);
            }

            if (dataDictionary != null && dataDictionary.isDataField(tag)) {
                /* Assume length field is 1 less. */
                int lengthField = tag - 1;
                /* Special case for Signature which violates above assumption. */
                if (tag == 89) {
                    lengthField = 93;
                }
                int fieldLength;
                try {
                    fieldLength = getInt(fields, lengthField);
                } catch (final FieldNotFound e) {
                    throw new InvalidMessage("Tag " + e.field + " not found in " + messageData);
                }

                // since length is in bytes but data is a string, and it may also
                // contain an SOH,
                // we find the real field-ending SOH by checking the encoded bytes
                // length
                // (we avoid re-encoding when the chars length equals the bytes
                // length, e.g. ASCII text,
                // by assuming the chars length is always smaller than the encoded
                // bytes length)
                while (sohOffset - equalsOffset - 1 < fieldLength
                        && messageData.substring(equalsOffset + 1, sohOffset).getBytes(
                                CharsetSupport.getCharsetInstance()).length < fieldLength) {
                    sohOffset = messageData.indexOf('\001', sohOffset + 1);
                    if (sohOffset == -1) {
                        throw new InvalidMessage("SOH not found at end of field: " + tag + " in "
                                + messageData);
                    }
                }
            }

            position = sohOffset + 1;

            if (dataDictionary != null) {

                return dataDictionary.produceField(builder, msgType, tag, messageData,
                        equalsOffset + 1, sohOffset);
            }

            return builder.create(tag, messageData.subSequence(equalsOffset + 1, sohOffset));
        }

        private void pushBack(FIXField<?> field) {

            pushedBackField = field;
        }

        private boolean isHeaderField(FIXField<?> field, DataDictionary dd) {

            return isHeaderField(field) || (dd != null && dd.isHeaderField(field.getTag()));
        }

        @Deprecated
        private boolean isHeaderField(FIXField<?> field) {
            return isHeaderField(field.getTag());
        }

        @Deprecated
        private boolean isHeaderField(int tag) {

            switch (tag) {
            case FixTags.BEGIN_STRING:
            case FixTags.BODY_LENGTH:
            case FixTags.MSG_TYPE:
            case FixTags.SENDER_COMP_ID:
            case FixTags.TARGET_COMP_ID:
            case FixTags.ON_BEHALF_OF_COMP_ID:
            case FixTags.DELIVER_TO_COMP_ID:
            case FixTags.SECURE_DATA_LENGTH:
            case FixTags.SECURE_DATA:
            case FixTags.MSG_SEQ_NUM:
            case FixTags.SENDER_SUB_ID:
            case FixTags.SENDER_LOCATION_ID:
            case FixTags.TARGET_SUB_ID:
            case FixTags.TARGET_LOCATION_ID:
            case FixTags.ON_BEHALF_OF_SUB_ID:
            case FixTags.ON_BEHALF_OF_LOCATION_ID:
                //       case OnBehalfOfSendingTime.TAG:
            case FixTags.DELIVER_TO_SUB_ID:
            case FixTags.DELIVER_TO_LOCATION_ID:
            case FixTags.POSS_DUP_FLAG:
            case FixTags.POSS_RESEND:
            case FixTags.SENDING_TIME:
            case FixTags.ORIG_SENDING_TIME:
            case FixTags.XML_DATA_LENGTH:
            case FixTags.XML_DATA:
            case FixTags.MESSAGE_ENCODING:
            case FixTags.LAST_MSG_SEQ_NUM_PROCESSED:
            case FixTags.APPL_VER_ID:
            case FixTags.CSTM_APPL_VER_ID:
            case FixTags.NO_HOPS:
                return true;
            default:
                return false;
            }
        }

        private boolean isTrailerField(FIXField<?> field, DataDictionary dd) {

            return isTrailerField(field.getTag())
                    || (dd != null && dd.isTrailerField(field.getTag()));
        }

        private boolean isTrailerField(int field) {

            switch (field) {
            case FixTags.SIGNATURE_LENGTH:
            case FixTags.SIGNATURE:
            case FixTags.CHECK_SUM:
                return true;
            default:
                return false;
            }
        }

        private void parseGroup(String msgType, FIXField<?> field, DataDictionary dd,
                FieldMap parent) throws InvalidMessage {

            final DataDictionary.GroupInfo rg = dd.getGroup(msgType, field.getTag());
            final DataDictionary groupDataDictionary = rg.getDataDictionary();
            final int[] fieldOrder = groupDataDictionary.getOrderedFields();
            int previousOffset = -1;
            final int groupCountTag = field.getTag();
            final int declaredGroupCount = Integer.parseInt(field.getCharacters().toString());
            parent.setField(groupCountTag, field);
            final int firstField = rg.getDelimiterField();
            boolean firstFieldFound = false;
            Group group = null;
            boolean inGroupParse = true;
            while (inGroupParse) {
                field = extractField(msgType, dd, group != null ? group : parent);
                if (field == null) {
                    // QFJ-760: stop parsing since current position is greater than
                    // message length
                    break;
                }
                int tag = field.getTag();
                if (tag == firstField) {
                    if (group != null) {
                        parent.addGroup(group);
                    }
                    group = new Group(groupCountTag, firstField,
                            groupDataDictionary.getOrderedFields());
                    group.setField(field);
                    firstFieldFound = true;
                    previousOffset = -1;
                    // QFJ-742
                    if (groupDataDictionary.isGroup(msgType, tag)) {
                        parseGroup(msgType, field, groupDataDictionary, group);
                    }
                } else if (groupDataDictionary.isGroup(msgType, tag)) {
                    if (!firstFieldFound) {
                        throw new InvalidMessage("The group " + groupCountTag
                                + " must set the delimiter field " + firstField + " in "
                                + messageData);
                    }
                    parseGroup(msgType, field, groupDataDictionary, group);
                } else if (groupDataDictionary.isField(tag)) {
                    if (!firstFieldFound) {
                        throw new FieldException(
                                SessionRejectReasonText.REPEATING_GROUP_FIELDS_OUT_OF_ORDER, tag);
                    }

                    if (fieldOrder != null && dd.isCheckUnorderedGroupFields()) {
                        final int offset = indexOf(tag, fieldOrder);
                        if (offset > -1) {
                            if (offset <= previousOffset) {
                                throw new FieldException(
                                        SessionRejectReasonText.REPEATING_GROUP_FIELDS_OUT_OF_ORDER,
                                        tag);
                            }
                            previousOffset = offset;
                        }
                    }
                    group.setField(field);
                } else {
                    pushBack(field);
                    inGroupParse = false;
                }
            }
            // add what we've already got and leave the rest to the validation (if
            // enabled)
            if (group != null) {
                parent.addGroup(group);
            }
            // For later validation that the group size matches the parsed group
            // count
            parent.setGroupCount(groupCountTag, declaredGroupCount);
        }

        private int indexOf(int field, int[] fieldOrder) {

            if (fieldOrder != null) {
                for (int i = 0; i < fieldOrder.length; i++) {
                    if (field == fieldOrder[i]) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private void validateCheckSum(String messageData) throws InvalidMessage {

            try {
                // Body length is checked at the protocol layer
                final int checksum = getInt(message.getTrailer(), FixTags.CHECK_SUM);
                if (checksum != MessageUtils.checksum(messageData)) {
                    // message will be ignored if checksum is wrong or missing
                    throw new InvalidMessage("Expected CheckSum="
                            + MessageUtils.checksum(messageData) + ", Received CheckSum="
                            + checksum + " in " + messageData);
                }
            } catch (final FieldNotFound e) {
                throw new InvalidMessage("Field not found: " + e.field + " in " + messageData);
            }
        }

        private int getInt(FIXFieldGraph graph, int tag) throws FieldNotFound {

            try {
                return IntConverter.convert(graph.getField(tag).getCharacters().toString());
            } catch (final FieldConvertError e) {

                throw new FieldException(SessionRejectReasonText.INCORRECT_DATA_FORMAT_FOR_VALUE,
                        e.getMessage(), tag);
            }
        }
    }

    private static class DefaultMessageBuilder implements MessageBuilder {

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXBeginString getBeginString() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXApplication getApplication() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public String getMsgType() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Message create() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXField<?> create(int tag, CharSequence chars) {
            return new StringField(tag, chars.toString());
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public <T extends FIXGroup> GroupBuilder<T> create(FIXGroupField<T> groupField) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Map<Integer, FieldBuilder> getFieldBuilders() {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public Map<Integer, GroupBuilder<?>> getGroupBuilders() {
            throw new UnsupportedOperationException();
        }
    }
}
