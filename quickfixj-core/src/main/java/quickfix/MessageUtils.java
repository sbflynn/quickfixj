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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.quickfixj.CharsetSupport;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXFieldGraph;
import org.quickfixj.FIXMessage;
import org.quickfixj.QFJException;
import org.quickfixj.engine.FIXMessageBuilderFactory;
import org.quickfixj.engine.FIXMessageDictionaryFactory;
import org.quickfixj.engine.FIXSession;
import org.quickfixj.engine.FIXSession.FIXSessionID;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.engine.Validator;
import org.quickfixj.field.BigDecimalConverter;
import org.quickfixj.field.BooleanConverter;
import org.quickfixj.field.CharConverter;
import org.quickfixj.field.DoubleConverter;
import org.quickfixj.field.GenericField;
import org.quickfixj.field.IntConverter;
import org.quickfixj.field.UtcTimestampConverter;

public class MessageUtils {

    private static final char FIELD_SEPARATOR = '\001';

    private static final Map<FIXBeginString, FIXApplication> BEGIN_STRING_TO_APPL_VER_ID;

    static {

        // populate maps with supported FIX versions
        // No support for earlier versions of FIX
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
        BEGIN_STRING_TO_APPL_VER_ID.put(beginString, applVerID);
    }

    public static SessionID getSessionID(FIXMessage fixMessage) {

        FIXFieldGraph header = fixMessage.getHeader();
        return new SessionID(getBeginString(header), getFieldOrDefault(header,
                FIXTag.SENDER_COMP_ID, null),
                getFieldOrDefault(header, FIXTag.SENDER_SUB_ID, null), getFieldOrDefault(header,
                        FIXTag.SENDER_LOCATION_ID, null), getFieldOrDefault(header,
                        FIXTag.TARGET_COMP_ID, null), getFieldOrDefault(header,
                        FIXTag.TARGET_SUB_ID, null), getFieldOrDefault(header,
                        FIXTag.TARGET_LOCATION_ID, null), null);
    }

    public static SessionID getSessionID(String messageString) {

        return new SessionID(getBeginString(messageString), getStringField(messageString,
                FIXTag.SENDER_COMP_ID), getStringField(messageString, FIXTag.SENDER_SUB_ID),
                getStringField(messageString, FIXTag.SENDER_LOCATION_ID), getStringField(
                        messageString, FIXTag.TARGET_COMP_ID), getStringField(messageString,
                        FIXTag.TARGET_SUB_ID), getStringField(messageString,
                        FIXTag.TARGET_LOCATION_ID), null);
    }

    public static FIXSessionID getReverseSessionID(FIXMessage fixMessage) {

        FIXFieldGraph header = fixMessage.getHeader();
        return new SessionID(getBeginString(header), getFieldOrDefault(header,
                FIXTag.TARGET_COMP_ID, null),
                getFieldOrDefault(header, FIXTag.TARGET_SUB_ID, null), getFieldOrDefault(header,
                        FIXTag.TARGET_LOCATION_ID, null), getFieldOrDefault(header,
                        FIXTag.SENDER_COMP_ID, null), getFieldOrDefault(header,
                        FIXTag.SENDER_SUB_ID, null), getFieldOrDefault(header,
                        FIXTag.SENDER_LOCATION_ID, null), null);
    }

    public static FIXSessionID getReverseSessionID(String messageString) {

        return new SessionID(getBeginString(messageString), getStringField(messageString,
                FIXTag.TARGET_COMP_ID), getStringField(messageString, FIXTag.TARGET_SUB_ID),
                getStringField(messageString, FIXTag.TARGET_LOCATION_ID), getStringField(
                        messageString, FIXTag.SENDER_COMP_ID), getStringField(messageString,
                        FIXTag.SENDER_SUB_ID), getStringField(messageString,
                        FIXTag.SENDER_LOCATION_ID), null);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    public static boolean coerceToBoolean(FIXFieldGraph graph, int tag) {

        FIXField<?> field = graph.getField(tag);
        return BooleanConverter.convert(field);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    public static char coerceToChar(FIXFieldGraph graph, int tag) {

        FIXField<?> field = graph.getField(tag);
        return CharConverter.convert(field);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    public static int coerceToInt(FIXFieldGraph graph, int tag) {

        FIXField<?> field = graph.getField(tag);
        return IntConverter.convert(field);
    }

    /**
     * @author stephen.flynn@jftechnology.com
     * @since 2.0
     */
    public static double coerceToDouble(FIXFieldGraph graph, int tag) {

        FIXField<?> field = graph.getField(tag);
        return DoubleConverter.convert(field);
    }

    public static BigDecimal coerceToBigDecimal(FIXFieldGraph graph, int tag) {

        FIXField<?> field = graph.getField(tag);
        return BigDecimalConverter.convert(field);
    }

    public static byte[] coerceToByteArray(FIXFieldGraph graph, int tag, Charset charset) {

        FIXField<?> field = graph.getField(tag);

        return coerceToByteArray(field, charset);
    }

    public static byte[] coerceToByteArray(FIXField<?> field, Charset charset) {

        if (field.getValue() instanceof byte[]) {
            return (byte[]) field.getValue();
        }

        return field.getCharacters().toString().getBytes(charset);
    }

    private static String getFieldOrDefault(FIXFieldGraph graph, int tag, String defaultValue) {

        if (graph.isFieldSet(tag)) {
            try {
                return graph.getFieldValue(tag);
            } catch (final FieldNotFound e) {
                // ignore, should never happen
                return null;
            }
        }

        return defaultValue;
    }

    public static void set(FIXFieldGraph graph, int tag, String value) {

        graph.setField(new GenericField(tag, value));
    }

    public static void set(FIXFieldGraph graph, int tag, int value) {

        graph.setField(new GenericField(tag, value));
    }

    public static void set(FIXFieldGraph graph, int tag, boolean value) {

        graph.setField(new GenericField(tag, value));
    }

    public static void setUtcTimeStamp(FIXFieldGraph graph, int tag, Date value) {

        setUtcTimeStamp(graph, tag, value, false);
    }

    public static void setUtcTimeStamp(FIXFieldGraph graph, int tag, Date value,
            boolean includeMilliseconds) {

        graph.setField(new GenericField(tag, UtcTimestampConverter.convert(value,
                includeMilliseconds)));
    }

    /**
     * NOTE: This method is intended for internal use.
     *
     * @param session the Session that will process the message
     * @param messageString
     * @return the parsed message
     */
    public static FIXMessage parse(FIXSession session, String messageString) {

        FIXBeginString beginString = getBeginString(messageString);

        FIXApplication applVerID;

        if (FIXBeginString.FIXT11 == beginString) {
            applVerID = getApplVerID(session, messageString);
        } else {
            applVerID = toApplVerID(beginString);
        }

        FIXMessageBuilderFactory messageFactory = session.getMessageFactory();
        FIXMessageDictionaryFactory dataDictionary = session.getDataDictionary();

        return parse(applVerID, messageFactory, dataDictionary, messageString, true);
    }

    /**
     * NOTE: This method is intended for internal use.
     *
     * @param application
     * @param builderFactory
     * @param dictionaryProvider
     * @param messageString
     * @param validate
     * @since 2.0
     */
    public static FIXMessage parse(FIXApplication application,
            FIXMessageBuilderFactory builderFactory, FIXMessageDictionaryFactory dataDictionary,
            String messageString, boolean validate) {

        FIXBeginString beginString = getBeginString(messageString);

        MessageParser parser = new MessageParser(builderFactory, dataDictionary,
                new DefaultValidator(beginString));

        return parser.parse(application, messageString, validate);
    }

    /**
     * FOR TEST ONLY
     * @param application
     * @param message
     * @param dictionaryProvider
     * @param messageString
     * @param validate
     * @since 2.0
     */
    public static <T extends FIXMessage> T parse(FIXApplication application, T message,
            FIXMessageDictionaryFactory dataDictionary, Validator validator, String messageString,
            boolean validate) {

        MessageParser parser = new MessageParser(new GenericMessageBuilderFactory(),
                dataDictionary, validator);

        return parser.parse(application, messageString, validate, message);
    }

    public static FIXMessage parse(FIXApplication application, String messageString)
            throws InvalidMessage {
        return parse(application, messageString, true);
    }

    public static FIXMessage parse(FIXApplication application, String messageString,
            boolean doValidation) throws InvalidMessage {

        FIXBeginString beginString = getBeginString(messageString);

        MessageParser parser = new MessageParser(DefaultEngine.getDefaultEngine()
                .getMessageBuilderFactory(beginString, "org.quickfixj.messages.bd"), DefaultEngine
                .getDefaultEngine().getMessageDictionaryFactory(beginString,
                        "org.quickfixj.messages.bd"), new DefaultValidator(beginString));

        return parser.parse(application, messageString, doValidation);
    }

    private static FIXApplication getApplVerID(FIXSession session, String messageString) {

        FIXApplication applVerID = null;

        final String applVerIdString = getStringField(messageString, FIXTag.APPL_VER_ID);
        if (applVerIdString != null) {
            applVerID = FIXApplication.parseId(applVerIdString);
        }

        if (applVerID == null) {
            applVerID = session.getTargetDefaultApplicationVersionID();
        }

        if (applVerID == null && isLogon(messageString)) {
            final String defaultApplVerIdString = getStringField(messageString,
                    FIXTag.DEFAULT_APPL_VER_ID);
            if (defaultApplVerIdString != null) {
                applVerID = FIXApplication.parseId(defaultApplVerIdString);
            }
        }

        if (applVerID == null) {
            throw new InvalidMessage("Can't determine ApplVerID for message");
        }

        return applVerID;
    }

    public static boolean isAppMessage(FIXMessage message) {

        return !isAdminMessage(message);
    }

    public static boolean isAdminMessage(FIXMessage message) {

        FIXFieldGraph header = message.getHeader();

        if (header.isFieldSet(FIXTag.MSG_TYPE)) {
            try {
                final String msgType = header.getField(FIXTag.MSG_TYPE).getCharacters().toString();
                return isAdminMessage(msgType);
            } catch (final FieldNotFound e) {
                // shouldn't happen
            }
        }
        return false;
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

    public static String getMessageType(String messageString) {

        final String value = getStringField(messageString, 35);
        if (value == null) {
            throw new InvalidMessage("Missing or garbled message type in " + messageString);
        }
        return value;
    }

    public static FIXBeginString getBeginString(String messageString) {

        String value = getStringField(messageString, FIXTag.BEGIN_STRING);

        return FIXBeginString.parse(value);
    }

    public static FIXBeginString getBeginString(FIXFieldGraph header) {

        String value = getFieldOrDefault(header, FIXTag.BEGIN_STRING, null);

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
     * Returns the checksum of this field's FIX-encoded bytes (tag=value),
     * including the trailing SOH byte.
     *
     * @return the checksum of this field's encoded bytes
     */
    public static int checksum(Charset charset, FIXField<?> field) {

        Appendable buffer = field.serialize(new StringBuilder());

        return checksum(charset, buffer.toString(), false);
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
     * Returns the checksum of this field's FIX-encoded bytes (tag=value),
     * including the trailing SOH byte.
     * 
     * @param charset The charset used in encoding the data.
     * @param field The field to calculate the length of.
     * @return The calculated length.
     * @throws IOException 
     */
    public static int length(Charset charset, FIXField<?> field) {

        Appendable buffer = field.serialize(new StringBuilder());

        return length(charset, buffer.toString());
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
