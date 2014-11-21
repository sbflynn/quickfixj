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

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.quickfixj.CharsetSupport;
import org.quickfixj.FIXField;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import quickfix.field.StringField;

/**
 * Represents a FIX message.
 */
public class Message extends FieldMap {

    private static final long serialVersionUID = -3193357271891865972L;

    protected Header header = new Header();

    protected Trailer trailer = new Trailer();

    //
    // Extract field
    //
    private String messageData;

    private int position;

    private StringField pushedBackField;

    // @GuardedBy("this")
    private FieldException exception;

    public Message() {

        // empty
    }

    protected Message(int[] fieldOrder) {

        super(fieldOrder);
    }

    public Message(String string) throws InvalidMessage {

        fromString(string, null, true);
    }

    public Message(String string, boolean validate) throws InvalidMessage {

        fromString(string, null, validate);
    }

    public Message(String string, DataDictionary dd) throws InvalidMessage {

        fromString(string, dd, true);
    }

    public Message(String string, DataDictionary dd, boolean validate) throws InvalidMessage {

        fromString(string, dd, validate);
    }

    public static boolean InitializeXML(String url) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Object clone() {

        try {
            final Message message = getClass().newInstance();
            return cloneTo(message);
        } catch (final InstantiationException e) {
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Object cloneTo(Message message) {

        message.initializeFrom(this);
        message.header.initializeFrom(getHeader());
        message.trailer.initializeFrom(getTrailer());
        return message;
    }

    /**
     * Do not call this method concurrently while modifying the contents of the
     * message. This is likely to produce unexpected results or will fail with a
     * ConcurrentModificationException since FieldMap.calculateString() is
     * iterating over the TreeMap of fields.
     */
    @Override
    public String toString() {

        final int bodyLength = bodyLength();
        header.setInt(FixTags.BODY_LENGTH, bodyLength);
        trailer.setString(FixTags.CHECK_SUM, checksum());

        final StringBuilder sb = new StringBuilder(bodyLength);
        header.calculateString(sb, null, null);
        calculateString(sb, null, null);
        trailer.calculateString(sb, null, null);

        return sb.toString();
    }

    public int bodyLength() {

        return header.calculateLength() + calculateLength() + trailer.calculateLength();
    }

    private static DecimalFormat checksumFormat = new DecimalFormat("000");

    private String checksum() {

        return checksumFormat.format((header.calculateChecksum() + calculateChecksum() + trailer
                .calculateChecksum()) & 0xFF);
    }

    public void headerAddGroup(Group group) {

        header.addGroup(group);
    }

    public void headerReplaceGroup(int num, Group group) {

        header.replaceGroup(num, group);
    }

    public Group headerGetGroup(int num, Group group) throws FieldNotFound {

        return header.getGroup(num, group);
    }

    public void headerRemoveGroup(Group group) {

        header.removeGroup(group);
    }

    public boolean headerHasGroup(int field) {

        return header.hasGroup(field);
    }

    public boolean headerHasGroup(int num, int field) {

        return header.hasGroup(num, field);
    }

    public boolean headerHasGroup(int num, Group group) {

        return headerHasGroup(num, group.getFieldTag());
    }

    public boolean headerHasGroup(Group group) {

        return headerHasGroup(group.getFieldTag());
    }

    public void trailerAddGroup(Group group) {

        trailer.addGroup(group);
    }

    public Group trailerGetGroup(int num, Group group) throws FieldNotFound {

        return trailer.getGroup(num, group);
    }

    public void trailerReplaceGroup(int num, Group group) {

        trailer.replaceGroup(num, group);
    }

    public void trailerRemoveGroup(Group group) {

        trailer.removeGroup(group);
    }

    public boolean trailerHasGroup(int field) {

        return trailer.hasGroup(field);
    }

    public boolean trailerHasGroup(int num, int field) {

        return trailer.hasGroup(num, field);
    }

    public boolean trailerHasGroup(int num, Group group) {

        return trailerHasGroup(num, group.getFieldTag());
    }

    public boolean trailerHasGroup(Group group) {

        return trailerHasGroup(group.getFieldTag());
    }

    /**
     * Converts the message into a simple XML format. This format is probably
     * not sufficient for production use, but it more intended for diagnostics
     * and debugging. THIS IS NOT FIXML. To get names instead of tag number, use
     * toXML(DataDictionary) instead.
     *
     * @return an XML representation of the message.
     * @see #toXML(DataDictionary)
     */
    public String toXML() {

        return toXML(null);
    }

    /**
     * Converts the message into a simple XML format. This format is probably
     * not sufficient for production use, but it more intended for diagnostics
     * and debugging. THIS IS NOT FIXML.
     *
     * @param dataDictionary
     * @return the XML representation of the message
     */
    public String toXML(DataDictionary dataDictionary) {

        try {
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .newDocument();
            final Element message = document.createElement("message");
            document.appendChild(message);
            toXMLFields(message, "header", header, dataDictionary);
            toXMLFields(message, "body", this, dataDictionary);
            toXMLFields(message, "trailer", trailer, dataDictionary);
            final DOMSource domSource = new DOMSource(document);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final StreamResult streamResult = new StreamResult(out);
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.transform(domSource, streamResult);
            return out.toString();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void toXMLFields(Element message, String section, FieldMap fieldMap,
            DataDictionary dataDictionary) throws FieldNotFound {

        final Document document = message.getOwnerDocument();
        final Element fields = document.createElement(section);
        message.appendChild(fields);
        final Iterator<FIXField<?>> fieldItr = fieldMap.iterator();
        while (fieldItr.hasNext()) {
            final FIXField<?> field = fieldItr.next();
            final Element fieldElement = document.createElement("field");
            if (dataDictionary != null) {
                final String name = dataDictionary.getFieldName(field.getTag());
                if (name != null) {
                    fieldElement.setAttribute("name", name);
                }
                final String enumValue = dataDictionary.getValueName(field.getTag(), field
                        .getObject().toString());
                if (enumValue != null) {
                    fieldElement.setAttribute("enum", enumValue);
                }
            }
            fieldElement.setAttribute("tag", Integer.toString(field.getTag()));
            final CDATASection value = document.createCDATASection(field.getObject().toString());
            fieldElement.appendChild(value);
            fields.appendChild(fieldElement);
        }
        final Iterator<Integer> groupKeyItr = fieldMap.groupKeyIterator();
        while (groupKeyItr.hasNext()) {
            final int groupKey = groupKeyItr.next();
            final Element groupsElement = document.createElement("groups");
            fields.appendChild(groupsElement);
            if (dataDictionary != null) {
                final String name = dataDictionary.getFieldName(groupKey);
                if (name != null) {
                    groupsElement.setAttribute("name", name);
                }
            }
            groupsElement.setAttribute("tag", Integer.toString(groupKey));
            final List<Group> groups = fieldMap.getGroups(groupKey);
            for (Group group : groups) {
                toXMLFields(groupsElement, "group", group, dataDictionary);
            }
        }
    }

    public final Header getHeader() {

        return header;
    }

    public final Trailer getTrailer() {

        return trailer;
    }

    public boolean isAdmin() {

        if (header.isSetField(FixTags.MSG_TYPE)) {
            try {
                final String msgType = header.getString(FixTags.MSG_TYPE);
                return MessageUtils.isAdminMessage(msgType);
            } catch (final FieldNotFound e) {
                // shouldn't happen
            }
        }
        return false;
    }

    public boolean isApp() {

        return !isAdmin();
    }

    @Override
    public boolean isEmpty() {

        return super.isEmpty() && header.isEmpty() && trailer.isEmpty() && position == 0;
    }

    @Override
    public void clear() {

        super.clear();
        header.clear();
        trailer.clear();
        position = 0;
    }

    public static class Header extends FieldMap {

        private static final long serialVersionUID = -3193357271891865972L;

        private static final int[] EXCLUDED_HEADER_FIELDS = { FixTags.BEGIN_STRING,
                FixTags.BODY_LENGTH, FixTags.MSG_TYPE };

        public Header() {

            super();
        }

        public Header(int[] fieldOrder) {

            super(fieldOrder);
        }

        @Override
        protected void calculateString(StringBuilder buffer, int[] excludedFields, int[] postFields) {

            super.calculateString(buffer, EXCLUDED_HEADER_FIELDS, postFields);
        }
    }

    public static class Trailer extends FieldMap {

        private static final long serialVersionUID = -3193357271891865972L;

        private static final int[] TRAILER_FIELD_ORDER = { FixTags.SIGNATURE_LENGTH,
                FixTags.SIGNATURE, FixTags.CHECK_SUM };

        public Trailer() {

            super(TRAILER_FIELD_ORDER);
        }

        public Trailer(int[] fieldOrder) {

            super(fieldOrder);
        }

        @Override
        protected void calculateString(StringBuilder buffer, int[] excludedFields, int[] postFields) {

            super.calculateString(buffer, null, new int[] { FixTags.CHECK_SUM });
        }
    }

    public void reverseRoute(Header header) throws FieldNotFound {

        this.header.removeField(FixTags.BEGIN_STRING);
        this.header.removeField(FixTags.SENDER_COMP_ID);
        this.header.removeField(FixTags.SENDER_SUB_ID);
        this.header.removeField(FixTags.SENDER_LOCATION_ID);
        this.header.removeField(FixTags.TARGET_COMP_ID);
        this.header.removeField(FixTags.TARGET_SUB_ID);
        this.header.removeField(FixTags.TARGET_LOCATION_ID);

        if (header.isSetField(FixTags.BEGIN_STRING)) {
            copyField(header, FixTags.BEGIN_STRING, FixTags.BEGIN_STRING);

            copyField(header, FixTags.SENDER_COMP_ID, FixTags.TARGET_COMP_ID);
            copyField(header, FixTags.SENDER_SUB_ID, FixTags.TARGET_SUB_ID);
            copyField(header, FixTags.SENDER_LOCATION_ID, FixTags.TARGET_LOCATION_ID);

            copyField(header, FixTags.TARGET_COMP_ID, FixTags.SENDER_COMP_ID);
            copyField(header, FixTags.TARGET_SUB_ID, FixTags.SENDER_SUB_ID);
            copyField(header, FixTags.TARGET_LOCATION_ID, FixTags.SENDER_LOCATION_ID);

            this.header.removeField(FixTags.ON_BEHALF_OF_COMP_ID);
            this.header.removeField(FixTags.ON_BEHALF_OF_SUB_ID);
            this.header.removeField(FixTags.DELIVER_TO_COMP_ID);
            this.header.removeField(FixTags.DELIVER_TO_SUB_ID);

            copyField(header, FixTags.ON_BEHALF_OF_COMP_ID, FixTags.DELIVER_TO_COMP_ID);
            copyField(header, FixTags.ON_BEHALF_OF_SUB_ID, FixTags.DELIVER_TO_SUB_ID);
            copyField(header, FixTags.DELIVER_TO_COMP_ID, FixTags.ON_BEHALF_OF_COMP_ID);
            copyField(header, FixTags.DELIVER_TO_SUB_ID, FixTags.ON_BEHALF_OF_SUB_ID);

            this.header.removeField(FixTags.ON_BEHALF_OF_LOCATION_ID);
            this.header.removeField(FixTags.DELIVER_TO_LOCATION_ID);

            if (header.getString(FixTags.BEGIN_STRING).compareTo(FixVersions.BEGINSTRING_FIX41) >= 0) {
                copyField(header, FixTags.ON_BEHALF_OF_LOCATION_ID, FixTags.DELIVER_TO_LOCATION_ID);
                copyField(header, FixTags.DELIVER_TO_LOCATION_ID, FixTags.ON_BEHALF_OF_LOCATION_ID);
            }
        }
    }

    private void copyField(Header header, int fromField, int toField) throws FieldNotFound {

        if (header.isSetField(fromField)) {
            final String value = header.getString(fromField);
            if (value.length() > 0) {
                this.header.setString(toField, value);
            }
        }
    }

    void setSessionID(SessionID sessionID) {

        header.setString(FixTags.BEGIN_STRING, sessionID.getBeginString());
        header.setString(FixTags.SENDER_COMP_ID, sessionID.getSenderCompID());
        optionallySetID(header, FixTags.SENDER_SUB_ID, sessionID.getSenderSubID());
        optionallySetID(header, FixTags.SENDER_LOCATION_ID, sessionID.getSenderLocationID());
        header.setString(FixTags.TARGET_COMP_ID, sessionID.getTargetCompID());
        optionallySetID(header, FixTags.TARGET_SUB_ID, sessionID.getTargetSubID());
        optionallySetID(header, FixTags.TARGET_LOCATION_ID, sessionID.getTargetLocationID());
    }

    private void optionallySetID(Header header, int field, String value) {

        if (!value.equals(SessionID.NOT_SET)) {
            header.setString(field, value);
        }
    }

    public void fromString(String messageData, DataDictionary dd, boolean doValidation)
            throws InvalidMessage {

        parse(messageData, dd, dd, doValidation);
    }

    public void fromString(String messageData, DataDictionary sessionDictionary,
            DataDictionary applicationDictionary, boolean doValidation) throws InvalidMessage {

        if (sessionDictionary.isAdminMessage(MessageUtils.getMessageType(messageData))) {
            applicationDictionary = sessionDictionary;
        }
        parse(messageData, sessionDictionary, applicationDictionary, doValidation);
    }

    void parse(String messageData, DataDictionary sessionDataDictionary,
            DataDictionary applicationDataDictionary, boolean doValidation) throws InvalidMessage {

        this.messageData = messageData;

        try {
            parseHeader(sessionDataDictionary, doValidation);
            parseBody(applicationDataDictionary, doValidation);
            parseTrailer(sessionDataDictionary);
            if (doValidation) {
                validateCheckSum(messageData);
            }
        } catch (final FieldException e) {
            exception = e;
        }
    }

    private void validateCheckSum(String messageData) throws InvalidMessage {

        try {
            // Body length is checked at the protocol layer
            final int checksum = trailer.getInt(FixTags.CHECK_SUM);
            if (checksum != MessageUtils.checksum(messageData)) {
                // message will be ignored if checksum is wrong or missing
                throw new InvalidMessage("Expected CheckSum=" + MessageUtils.checksum(messageData)
                        + ", Received CheckSum=" + checksum + " in " + messageData);
            }
        } catch (final FieldNotFound e) {
            throw new InvalidMessage("Field not found: " + e.field + " in " + messageData);
        }
    }

    private void parseHeader(DataDictionary dd, boolean doValidation) throws InvalidMessage {

        if (doValidation) {
            final boolean validHeaderFieldOrder = isNextField(dd, header, FixTags.BEGIN_STRING)
                    && isNextField(dd, header, FixTags.BODY_LENGTH)
                    && isNextField(dd, header, FixTags.MSG_TYPE);
            if (!validHeaderFieldOrder) {
                // Invalid message preamble (first three fields) is a serious
                // condition and is handled differently from other message
                // parsing errors.
                throw new InvalidMessage("Header fields out of order in " + messageData);
            }
        }

        StringField field = extractField(dd, header);
        while (field != null && isHeaderField(field, dd)) {
            header.setField(field);

            if (dd != null && dd.isGroup(DataDictionary.HEADER_ID, field.getTag())) {
                parseGroup(DataDictionary.HEADER_ID, field, dd, header);
            }

            field = extractField(dd, header);
        }
        pushBack(field);
    }

    private boolean isNextField(DataDictionary dd, Header fields, int tag) throws InvalidMessage {

        final StringField field = extractField(dd, header);
        if (field == null || field.getTag() != tag) {
            return false;
        }
        fields.setField(field);
        return true;
    }

    private String getMsgType() throws InvalidMessage {

        try {
            return header.getString(FixTags.MSG_TYPE);
        } catch (final FieldNotFound e) {
            throw new InvalidMessage(e.getMessage() + " in " + messageData);
        }
    }

    private void parseBody(DataDictionary dd, boolean doValidation) throws InvalidMessage {

        StringField field = extractField(dd, this);
        while (field != null) {
            if (isTrailerField(field.getTag())) {
                pushBack(field);
                return;
            }

            if (isHeaderField(field.getTag())) {
                // An acceptance test requires the sequence number to
                // be available even if the related field is out of order
                setField(header, field);
                // Group case
                if (dd != null && dd.isGroup(DataDictionary.HEADER_ID, field.getTag())) {
                    parseGroup(DataDictionary.HEADER_ID, field, dd, header);
                }
                if (doValidation && dd != null && dd.isCheckFieldsOutOfOrder())
                    throw new FieldException(
                            SessionRejectReasonText.TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER,
                            field.getTag());
            } else {
                setField(this, field);
                // Group case
                if (dd != null && dd.isGroup(getMsgType(), field.getTag())) {
                    parseGroup(getMsgType(), field, dd, this);
                }
            }

            field = extractField(dd, this);
        }
    }

    private void setField(FieldMap fields, StringField field) {

        if (fields.isSetField(field)) {
            throw new FieldException(SessionRejectReasonText.TAG_APPEARS_MORE_THAN_ONCE,
                    field.getTag());
        }
        fields.setField(field);
    }

    private void parseGroup(String msgType, StringField field, DataDictionary dd, FieldMap parent)
            throws InvalidMessage {

        final DataDictionary.GroupInfo rg = dd.getGroup(msgType, field.getTag());
        final DataDictionary groupDataDictionary = rg.getDataDictionary();
        final int[] fieldOrder = groupDataDictionary.getOrderedFields();
        int previousOffset = -1;
        final int groupCountTag = field.getTag();
        final int declaredGroupCount = Integer.parseInt(field.getValue());
        parent.setField(groupCountTag, field);
        final int firstField = rg.getDelimiterField();
        boolean firstFieldFound = false;
        Group group = null;
        boolean inGroupParse = true;
        while (inGroupParse) {
            field = extractField(dd, group != null ? group : parent);
            if (field == null) {
                // QFJ-760: stop parsing since current position is greater than
                // message length
                break;
            }
            int tag = field.getTag();
            if (tag == firstField) {
                if (group != null) {
                    parent.addGroupRef(group);
                }
                group = new Group(groupCountTag, firstField, groupDataDictionary.getOrderedFields());
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
                            + " must set the delimiter field " + firstField + " in " + messageData);
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
            parent.addGroupRef(group);
        }
        // For later validation that the group size matches the parsed group
        // count
        parent.setGroupCount(groupCountTag, declaredGroupCount);
    }

    private void parseTrailer(DataDictionary dd) throws InvalidMessage {

        StringField field = extractField(dd, trailer);
        while (field != null) {
            if (!isTrailerField(field, dd)) {
                throw new FieldException(
                        SessionRejectReasonText.TAG_SPECIFIED_OUT_OF_REQUIRED_ORDER, field.getTag());
            }
            trailer.setField(field);
            field = extractField(dd, trailer);
        }
    }

    static boolean isHeaderField(FIXField<?> field, DataDictionary dd) {

        return isHeaderField(field.getTag()) || (dd != null && dd.isHeaderField(field.getTag()));
    }

    static boolean isHeaderField(int field) {

        switch (field) {
        case FixTags.BEGIN_STRING:
        case FixTags.BODY_LENGTH:
        case FixTags.MSG_TYPE:
        case FixTags.SENDER_COMP_ID:
        case FixTags.TARGET_COMP_ID:
        case FixTags.ON_BEHALF_OF_COMP_ID:
        case FixTags.DELIVER_TO_COMP_ID:
        case FixTags.SECURE_DATA_LEN:
        case FixTags.MSG_SEQ_NUM:
        case FixTags.SENDER_SUB_ID:
        case FixTags.SENDER_LOCATION_ID:
        case FixTags.TARGET_SUB_ID:
        case FixTags.TARGET_LOCATION_ID:
        case FixTags.ON_BEHALF_OF_SUB_ID:
        case FixTags.ON_BEHALF_OF_LOCATION_ID:
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
        case FixTags.ON_BEHALF_OF_SENDING_TIME:
        case FixTags.APPL_VER_ID:
        case FixTags.CSTM_APPL_VER_ID:
        case FixTags.NO_HOPS:
            return true;
        default:
            return false;
        }
    }

    static boolean isTrailerField(FIXField<?> field, DataDictionary dd) {

        return isTrailerField(field.getTag()) || (dd != null && dd.isTrailerField(field.getTag()));
    }

    static boolean isTrailerField(int field) {

        switch (field) {
        case FixTags.SIGNATURE_LENGTH:
        case FixTags.SIGNATURE:
        case FixTags.CHECK_SUM:
            return true;
        default:
            return false;
        }
    }

    public void pushBack(StringField field) {

        pushedBackField = field;
    }

    private StringField extractField(DataDictionary dataDictionary, FieldMap fields)
            throws InvalidMessage {

        if (pushedBackField != null) {
            final StringField f = pushedBackField;
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
            throw new InvalidMessage("SOH not found at end of field: " + tag + " in " + messageData);
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
                fieldLength = fields.getInt(lengthField);
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
        return new StringField(tag, messageData.substring(equalsOffset + 1, sohOffset));
    }

    /**
     * Queries message structural validity.
     *
     * @return flag indicating whether the message has a valid structure
     */
    synchronized boolean hasValidStructure() {

        return exception == null;
    }

    public synchronized FieldException getException() {

        return exception;
    }

    /**
     * Returns the first invalid tag, which is all that can be reported in the
     * resulting FIX reject message.
     *
     * @return the first invalid tag
     */
    synchronized int getInvalidTag() {

        return exception != null ? exception.getField() : 0;
    }

    /**
     * Returns the msg type specified in a FIX message string.
     *
     * @param message the FIX message string
     * @return the message type
     * @throws MessageParseError (QF JNI compatibility)
     */
    public static StringField identifyType(String message) throws MessageParseError {

        try {
            return new StringField(FixTags.MSG_TYPE, MessageUtils.getMessageType(message));
        } catch (final InvalidMessage e) {
            throw new MessageParseError(e.getMessage(), e);
        }
    }
}
