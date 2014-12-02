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

import java.text.DecimalFormat;

import org.quickfixj.FIXBeginString;
import org.quickfixj.FIXField;
import org.quickfixj.FIXMessage;

/**
 * Represents a FIX message.
 */
public class Message extends FieldMap implements FIXMessage {

    private static final long serialVersionUID = -3193357271891865972L;

    private Header header = new Header();

    private Trailer trailer = new Trailer();

    @Deprecated
    // TODO : parse/validation exception shouldn't really be stored with data ? 
    private FieldException exception;

    public Message() {
        this(new Header(), new Trailer());
    }

    public Message(Header header, Trailer trailer) {
        this.header = header;
        this.trailer = trailer;
    }

    protected Message(int[] fieldOrder) {
        super(fieldOrder);
    }

    @Deprecated
    public Message(String string) throws InvalidMessage {
        MessageUtils.parse(this, string, null, true);
    }

    @Deprecated
    public Message(String string, boolean validate) throws InvalidMessage {
        MessageUtils.parse(this, string, null, validate);
    }

    @Deprecated
    public Message(String string, DataDictionary dd) throws InvalidMessage {
        MessageUtils.parse(this, string, dd, true);
    }

    @Deprecated
    public Message(String string, DataDictionary dd, boolean validate) throws InvalidMessage {
        MessageUtils.parse(this, string, dd, validate);
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

        final StringBuilder sb = new StringBuilder(bodyLength + 32);
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

    @Override
    public final Header getHeader() {

        return header;
    }

    /**
     * Set the header property.
     *
     * @param header The header to set.
     * @since 2.0
     */
    public void setHeader(Header header) {
        this.header = header;
    }

    @Override
    public final Trailer getTrailer() {

        return trailer;
    }

    /**
     * Set the trailer property.
     *
     * @param trailer The trailer to set.
     * @since 2.0
     */
    public void setTrailer(Trailer trailer) {
        this.trailer = trailer;
    }

    public boolean isAdmin() {

        if (header.isFieldSet(FixTags.MSG_TYPE)) {
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

        return super.isEmpty() && header.isEmpty() && trailer.isEmpty();
    }

    @Override
    public void clear() {

        super.clear();
        header.clear();
        trailer.clear();
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

        if (header.isFieldSet(FixTags.BEGIN_STRING)) {
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

            if (MessageUtils.getBeginString(header).gte(FIXBeginString.FIX41)) {
                copyField(header, FixTags.ON_BEHALF_OF_LOCATION_ID, FixTags.DELIVER_TO_LOCATION_ID);
                copyField(header, FixTags.DELIVER_TO_LOCATION_ID, FixTags.ON_BEHALF_OF_LOCATION_ID);
            }
        }
    }

    private void copyField(Header header, int fromField, int toField) throws FieldNotFound {

        if (header.isFieldSet(fromField)) {
            final String value = header.getString(fromField);
            if (value.length() > 0) {
                this.header.setString(toField, value);
            }
        }
    }

    void setSessionID(SessionID sessionID) {

        header.setString(FixTags.BEGIN_STRING, sessionID.getBeginString().getValue());
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

    @Override
    public String getMsgType() {

        try {
            return header.getString(FixTags.MSG_TYPE);
        } catch (FieldNotFound e) {
            throw new InvalidMessage("No message type found");
        }
    }

    @Deprecated
    static boolean isHeaderField(FIXField<?> field) {
        return isHeaderField(field.getTag());
    }

    @Deprecated
    static boolean isHeaderField(int tag) {

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

    /**
     * Queries message structural validity.
     *
     * @return flag indicating whether the message has a valid structure
     */
    @Deprecated
    synchronized boolean hasValidStructure() {

        return exception == null;
    }

    @Deprecated
    public synchronized FieldException getException() {

        return exception;
    }

    @Deprecated
    public synchronized void setException(FieldException exception) {
        this.exception = exception;
    }

    /**
     * Returns the first invalid tag, which is all that can be reported in the
     * resulting FIX reject message.
     *
     * @return the first invalid tag
     */
    @Deprecated
    synchronized int getInvalidTag() {

        return exception != null ? exception.getField() : 0;
    }
}
