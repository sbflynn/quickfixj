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

package org.quickfixj.field;

import java.io.UnsupportedEncodingException;

import org.quickfixj.QFJException;

/**
 * BytesField enables better handling of binary data. With BytesFields binary data can
 * be directly put into FIX messages without casting them into Strings.
 */
public abstract class BytesField extends AbstractField<byte[]> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private final byte[] value;

    protected BytesField(byte[] value) {
        this.value = value;
    }

    protected BytesField(char[] value, int offset, int count) {
        try {
            this.value = new String(value, offset, count).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new QFJException(e);
        }
    }

    @Override
    public byte[] getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public CharSequence getCharacters() {
        try {
            return new String(getValue(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new QFJException(e);
        }
    }
}
