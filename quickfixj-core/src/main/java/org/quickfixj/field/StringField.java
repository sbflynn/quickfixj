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

/**
 * A string-valued message field.
 */
public abstract class StringField extends AbstractField<String> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private final String value;

    protected StringField(String value) {
        this.value = value;
    }

    protected StringField(char[] value, int offset, int count) {

        this(new String(value, offset, count));
    }

    @Override
    public String getValue() {
        return value;
    }
}
