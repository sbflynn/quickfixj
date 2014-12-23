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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.quickfixj.FIXMessageHeader;
import org.quickfixj.FIXMessageTrailer;
import org.quickfixj.engine.FIXTag;

/**
 * Represents a generic FIX message.
 */
public class Message extends AbstractMessage {

    private static final long serialVersionUID = -3193357271891865972L;

    private String msgType;

    public Message() {
        this(new Header(), new Trailer(), null);
    }

    public Message(String msgType) {
        this(new Header(), new Trailer(), msgType);
    }

    Message(FIXMessageHeader header, FIXMessageTrailer trailer, String msgType) {
        super(header, trailer);
        this.msgType = msgType;
    }

    @Override
    public String getMsgType() {

        return msgType;
    }

    /**
     * Set the msgType property.
     *
     * @param msgType The msgType to set.
     * @since 2.0
     */
    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public static class Header extends AbstractFieldGraph implements FIXMessageHeader {

        private static final long serialVersionUID = -3193357271891865972L;

        public Header() {

            super();
        }

        public Header(List<Integer> fieldOrder) {

            super(fieldOrder);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXMessageHeader clone() {
            return (FIXMessageHeader) super.clone();
        }
    }

    public static class Trailer extends AbstractFieldGraph implements FIXMessageTrailer {

        private static final long serialVersionUID = -3193357271891865972L;

        private static final List<Integer> TRAILER_FIELD_ORDER = Collections
                .unmodifiableList(Arrays.asList(FIXTag.SIGNATURE_LENGTH, FIXTag.SIGNATURE,
                        FIXTag.CHECK_SUM));

        public Trailer() {

            super(TRAILER_FIELD_ORDER);
        }

        public Trailer(List<Integer> fieldOrder) {

            super(fieldOrder);
        }

        /**
         * {@inheritDoc}
         *
         * @since 2.0
         */
        @Override
        public FIXMessageTrailer clone() {
            return (FIXMessageTrailer) super.clone();
        }
    }
}
