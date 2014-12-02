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

import org.quickfixj.field.AbstractField;

/**
 * A string-valued message field.
 */
public class StringField extends AbstractField<String> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private final int tag;

    private final String object;

    public StringField(int tag, String object) {

        this.tag = tag;
        this.object = object;
    }

    public StringField(int tag, int value) {

        this.tag = tag;
        this.object = String.valueOf(value);
    }

    public StringField(int tag, CharSequence message, int start, int end) {

        this.tag = tag;
        this.object = message.subSequence(start, end).toString();
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public int getTag() {
        return tag;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.0
     */
    @Override
    public String getValue() {
        return object;
    }
}
