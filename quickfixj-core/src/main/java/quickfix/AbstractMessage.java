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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.quickfixj.FIXField;
import org.quickfixj.FIXMessage;
import org.quickfixj.FIXMessageHeader;
import org.quickfixj.FIXMessageTrailer;
import org.quickfixj.engine.FIXTag;
import org.quickfixj.field.GenericField;

/**
 * AbstractMessage - provides support for generic and type safe messages.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
public abstract class AbstractMessage extends AbstractFieldGraph implements FIXMessage {

    private static final long serialVersionUID = -3193357271891865972L;

    private static final DecimalFormat CHECKSUM_FORMAT = new DecimalFormat("000");

    private static final List<Integer> HEADER_PRE_FIELDS = Collections.unmodifiableList(Arrays
            .asList(FIXTag.BEGIN_STRING, FIXTag.BODY_LENGTH, FIXTag.MSG_TYPE));

    private static final List<Integer> TRAILER_POST_FIELDS = Collections.unmodifiableList(Arrays
            .asList(FIXTag.CHECK_SUM));

    private FIXMessageHeader header;

    private FIXMessageTrailer trailer;

    @Deprecated
    // TODO : parse/validation exception shouldn't really be stored with data ? 
    private RuntimeException parseException;

    protected AbstractMessage() {
        this(new Message.Header(), new Message.Trailer());
    }

    protected AbstractMessage(FIXMessageHeader header, FIXMessageTrailer trailer) {
        this.header = header;
        this.trailer = trailer;
    }

    @Override
    public FIXMessage clone() {

        final AbstractMessage message = (AbstractMessage) super.clone();

        if (getHeader() != null) {
            message.setHeader(getHeader().clone());
        }

        if (getTrailer() != null) {
            message.setTrailer(getTrailer().clone());
        }

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

        int bodyLength = bodyLength();

        header.setField(new GenericField(FIXTag.BODY_LENGTH, bodyLength));
        trailer.setField(new GenericField(FIXTag.CHECK_SUM, checksum()));

        StringBuilder sb = new StringBuilder(bodyLength + 32);
        calculateString(getHeader(), sb, HEADER_PRE_FIELDS, null);
        calculateString(this, sb, null, null);
        calculateString(getTrailer(), sb, null, TRAILER_POST_FIELDS);

        return sb.toString();
    }

    public int bodyLength() {

        return calculateLength(getHeader()) + calculateLength(this) + calculateLength(getTrailer());
    }

    private String checksum() {

        return CHECKSUM_FORMAT
                .format((calculateChecksum(getHeader()) + calculateChecksum(this) + calculateChecksum(getTrailer())) & 0xFF);
    }

    @Override
    public final FIXMessageHeader getHeader() {

        return header;
    }

    /**
     * Set the header property.
     *
     * @param header The header to set.
     * @since 2.0
     */
    public void setHeader(FIXMessageHeader header) {
        this.header = header;
    }

    @Override
    public final FIXMessageTrailer getTrailer() {

        return trailer;
    }

    /**
     * Set the trailer property.
     *
     * @param trailer The trailer to set.
     * @since 2.0
     */
    public void setTrailer(FIXMessageTrailer trailer) {
        this.trailer = trailer;
    }

    public boolean isAdmin() {

        return MessageUtils.isAdminMessage(getMsgType());
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

    @Override
    public String getMsgType() {

        try {
            return header.getFieldValue(FIXTag.MSG_TYPE);
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
        case FIXTag.BEGIN_STRING:
        case FIXTag.BODY_LENGTH:
        case FIXTag.MSG_TYPE:
        case FIXTag.SENDER_COMP_ID:
        case FIXTag.TARGET_COMP_ID:
        case FIXTag.ON_BEHALF_OF_COMP_ID:
        case FIXTag.DELIVER_TO_COMP_ID:
        case FIXTag.SECURE_DATA_LENGTH:
        case FIXTag.SECURE_DATA:
        case FIXTag.MSG_SEQ_NUM:
        case FIXTag.SENDER_SUB_ID:
        case FIXTag.SENDER_LOCATION_ID:
        case FIXTag.TARGET_SUB_ID:
        case FIXTag.TARGET_LOCATION_ID:
        case FIXTag.ON_BEHALF_OF_SUB_ID:
        case FIXTag.ON_BEHALF_OF_LOCATION_ID:
            //       case OnBehalfOfSendingTime.TAG:
        case FIXTag.DELIVER_TO_SUB_ID:
        case FIXTag.DELIVER_TO_LOCATION_ID:
        case FIXTag.POSS_DUP_FLAG:
        case FIXTag.POSS_RESEND:
        case FIXTag.SENDING_TIME:
        case FIXTag.ORIG_SENDING_TIME:
        case FIXTag.XML_DATA_LENGTH:
        case FIXTag.XML_DATA:
        case FIXTag.MESSAGE_ENCODING:
        case FIXTag.LAST_MSG_SEQ_NUM_PROCESSED:
        case FIXTag.APPL_VER_ID:
        case FIXTag.CSTM_APPL_VER_ID:
        case FIXTag.NO_HOPS:
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
    public synchronized boolean hasValidStructure() {

        return parseException == null;
    }

    @Override
    @Deprecated
    public synchronized RuntimeException getParseException() {

        return parseException;
    }

    @Override
    @Deprecated
    public synchronized void setParseException(RuntimeException exception) {
        this.parseException = exception;
    }
}
