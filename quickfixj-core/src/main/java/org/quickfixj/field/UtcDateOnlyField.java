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

import java.util.Date;

/**
 * A date-valued message field.
 */
public abstract class UtcDateOnlyField extends DateField {
    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    protected UtcDateOnlyField(Date value) {
        super(value);
    }

    protected UtcDateOnlyField(char[] value, int offset, int count) {
        super(UtcDateOnlyConverter.convert(new String(value, offset, count)));
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.1
     */
    @Override
    public CharSequence getCharacters() {
        return UtcDateOnlyConverter.convert(getValue());
    }
}
