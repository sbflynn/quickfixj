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
import org.quickfixj.FIXField;
import org.quickfixj.QFJException;

import quickfix.Message.Header;
import quickfix.field.StringField;

public class MessageUtils {

    private static final char FIELD_SEPARATOR = '\001';

    public static SessionID getSessionID(Message fixMessage) {

        final Header header = fixMessage.getHeader();
        return new SessionID(getFieldOrDefault(header, FixTags.BEGIN_STRING, null),
                getFieldOrDefault(header, FixTags.SENDER_COMP_ID, null), getFieldOrDefault(header,
                        FixTags.SENDER_SUB_ID, null), getFieldOrDefault(header,
                        FixTags.SENDER_LOCATION_ID, null), getFieldOrDefault(header,
                        FixTags.TARGET_COMP_ID, null), getFieldOrDefault(header,
                        FixTags.TARGET_SUB_ID, null), getFieldOrDefault(header,
                        FixTags.TARGET_LOCATION_ID, null), null);
    }

    public static SessionID getSessionID(String messageString) {

        return new SessionID(getStringField(messageString, FixTags.BEGIN_STRING), getStringField(
                messageString, FixTags.SENDER_COMP_ID), getStringField(messageString,
                FixTags.SENDER_SUB_ID), getStringField(messageString, FixTags.SENDER_LOCATION_ID),
                getStringField(messageString, FixTags.TARGET_COMP_ID), getStringField(
                        messageString, FixTags.TARGET_SUB_ID), getStringField(messageString,
                        FixTags.TARGET_LOCATION_ID), null);
    }

    public static SessionID getReverseSessionID(Message fixMessage) {

        final Header header = fixMessage.getHeader();
        return new SessionID(getFieldOrDefault(header, FixTags.BEGIN_STRING, null),
                getFieldOrDefault(header, FixTags.TARGET_COMP_ID, null), getFieldOrDefault(header,
                        FixTags.TARGET_SUB_ID, null), getFieldOrDefault(header,
                        FixTags.TARGET_LOCATION_ID, null), getFieldOrDefault(header,
                        FixTags.SENDER_COMP_ID, null), getFieldOrDefault(header,
                        FixTags.SENDER_SUB_ID, null), getFieldOrDefault(header,
                        FixTags.SENDER_LOCATION_ID, null), null);
    }

    public static SessionID getReverseSessionID(String messageString) {

        return new SessionID(getStringField(messageString, FixTags.BEGIN_STRING), getStringField(
                messageString, FixTags.TARGET_COMP_ID), getStringField(messageString,
                FixTags.TARGET_SUB_ID), getStringField(messageString, FixTags.TARGET_LOCATION_ID),
                getStringField(messageString, FixTags.SENDER_COMP_ID), getStringField(
                        messageString, FixTags.SENDER_SUB_ID), getStringField(messageString,
                        FixTags.SENDER_LOCATION_ID), null);
    }

    private static String getFieldOrDefault(FieldMap fields, int tag, String defaultValue) {

        if (fields.isSetField(tag)) {
            try {
                return fields.getString(tag);
            } catch (final FieldNotFound e) {
                // ignore, should never happen
                return null;
            }
        } else {
            return defaultValue;
        }
    }

    /**
     * Utility method for parsing a mesasge. This should only be used for
     * parsing messages from FIX versions 4.4 or earlier.
     *
     * @param messageFactory
     * @param dataDictionary
     * @param messageString
     * @return the parsed message
     * @throws InvalidMessage
     */
    public static Message parse(MessageFactory messageFactory, DataDictionary dataDictionary,
            String messageString) throws InvalidMessage {

        final int index = messageString.indexOf(FIELD_SEPARATOR);
        if (index < 0) {
            throw new InvalidMessage("Message does not contain any field separator");
        }
        final String beginString = messageString.substring(2, index);
        final String messageType = getMessageType(messageString);
        final quickfix.Message message = messageFactory.create(beginString, messageType);
        message.fromString(messageString, dataDictionary, dataDictionary != null);
        return message;
    }

    /**
     * NOTE: This method is intended for internal use.
     *
     * @param session the Session that will process the message
     * @param messageString
     * @return the parsed message
     * @throws InvalidMessage
     */
    public static Message parse(Session session, String messageString) throws InvalidMessage {

        final String beginString = getStringField(messageString, FixTags.BEGIN_STRING);
        final String msgType = getMessageType(messageString);

        StringField applVerID;

        if (FixVersions.BEGINSTRING_FIXT11.equals(beginString)) {
            applVerID = getApplVerID(session, messageString);
        } else {
            applVerID = toApplVerID(beginString);
        }

        final MessageFactory messageFactory = session.getMessageFactory();

        final DataDictionaryProvider ddProvider = session.getDataDictionaryProvider();
        final DataDictionary sessionDataDictionary = ddProvider == null ? null : ddProvider
                .getSessionDataDictionary(beginString);
        final DataDictionary applicationDataDictionary = ddProvider == null ? null : ddProvider
                .getApplicationDataDictionary(applVerID);

        final quickfix.Message message = messageFactory.create(beginString, msgType);
        final DataDictionary payloadDictionary = MessageUtils.isAdminMessage(msgType)
                ? sessionDataDictionary
                : applicationDataDictionary;

        message.parse(messageString, sessionDataDictionary, payloadDictionary,
                payloadDictionary != null);

        return message;
    }

    private static StringField getApplVerID(Session session, String messageString)
            throws InvalidMessage {

        StringField applVerID = null;

        final String applVerIdString = getStringField(messageString, FixTags.APPL_VER_ID);
        if (applVerIdString != null) {
            applVerID = new StringField(FixTags.APPL_VER_ID, applVerIdString);
        }

        if (applVerID == null) {
            applVerID = session.getTargetDefaultApplicationVersionID();
        }

        if (applVerID == null && isLogon(messageString)) {
            final String defaultApplVerIdString = getStringField(messageString,
                    FixTags.DEFAULT_APPL_VER_ID);
            if (defaultApplVerIdString != null) {
                applVerID = new StringField(FixTags.APPL_VER_ID, defaultApplVerIdString);
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

    private static Map<String, String> applVerIDtoBeginString = new HashMap<String, String>() {

        {
            // No support for earlier versions of FIX
            put(FixVersions.APPL_VER_ID_FIX40, FixVersions.BEGINSTRING_FIX40);
            put(FixVersions.APPL_VER_ID_FIX41, FixVersions.BEGINSTRING_FIX41);
            put(FixVersions.APPL_VER_ID_FIX42, FixVersions.BEGINSTRING_FIX42);
            put(FixVersions.APPL_VER_ID_FIX43, FixVersions.BEGINSTRING_FIX43);
            put(FixVersions.APPL_VER_ID_FIX44, FixVersions.BEGINSTRING_FIX44);
            put(FixVersions.APPL_VER_ID_FIX50, FixVersions.FIX50);
            put(FixVersions.APPL_VER_ID_FIX50SP1, FixVersions.FIX50SP1);
            put(FixVersions.APPL_VER_ID_FIX50SP2, FixVersions.FIX50SP2);
        }
    };

    /**
     * Convert an ApplVerID to a "begin string"
     *
     * @param applVerID
     * @return the begin string for the specified ApplVerID.
     * @throws QFJException if conversion fails.
     * @see ApplVerID
     */
    public static String toBeginString(FIXField<?> applVerID) throws QFJException {

        final String beginString = applVerIDtoBeginString.get(applVerID.getCharacters());
        if (beginString == null) {
            throw new QFJException("Unknown or unsupported ApplVerID: " + applVerID.getCharacters());
        }
        return beginString;
    }

    private static Map<String, StringField> beginStringToApplVerID = new HashMap<String, StringField>() {

        {
            // No support for earlier versions of FIX
            put(FixVersions.BEGINSTRING_FIX40, new StringField(FixTags.APPL_VER_ID,
                    FixVersions.APPL_VER_ID_FIX40));
            put(FixVersions.BEGINSTRING_FIX41, new StringField(FixTags.APPL_VER_ID,
                    FixVersions.APPL_VER_ID_FIX41));
            put(FixVersions.BEGINSTRING_FIX42, new StringField(FixTags.APPL_VER_ID,
                    FixVersions.APPL_VER_ID_FIX42));
            put(FixVersions.BEGINSTRING_FIX43, new StringField(FixTags.APPL_VER_ID,
                    FixVersions.APPL_VER_ID_FIX43));
            put(FixVersions.BEGINSTRING_FIX44, new StringField(FixTags.APPL_VER_ID,
                    FixVersions.APPL_VER_ID_FIX44));
            put(FixVersions.FIX50, new StringField(FixTags.APPL_VER_ID,
                    FixVersions.APPL_VER_ID_FIX50));
            put(FixVersions.FIX50SP1, new StringField(FixTags.APPL_VER_ID,
                    FixVersions.APPL_VER_ID_FIX50SP1));
            put(FixVersions.FIX50SP2, new StringField(FixTags.APPL_VER_ID,
                    FixVersions.APPL_VER_ID_FIX50SP2));
        }
    };

    /**
     * Convert a begin string to an ApplVerID
     *
     * @param beginString
     * @return the ApplVerID for the specified begin string.
     * @throws QFJException if conversion fails.
     * @see FixVersions
     */
    public static StringField toApplVerID(String beginString) throws QFJException {

        final StringField applVerID = beginStringToApplVerID.get(beginString);
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
}
