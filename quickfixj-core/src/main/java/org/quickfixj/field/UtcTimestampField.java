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
 * A timestamp-valued message field (a timestamp has both a date and a time).
 */
public abstract class UtcTimestampField extends DateField {
    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private boolean includeMilliseconds;

    protected UtcTimestampField(Date value) {
        this(value, true);
    }

    protected UtcTimestampField(Date value, boolean includeMilliseconds) {
        // REVIEW : this is ugly - at the very least the super class should store the time as a long
        super(value.getTime() - (includeMilliseconds ? 0 : value.getTime() % 1000));
        this.includeMilliseconds = includeMilliseconds;
    }

    protected UtcTimestampField(char[] value, int offset, int count) {
        this(UtcTimestampConverter.convert(new String(value, offset, count)), true);
    }

    public boolean showMilliseconds() {
        return includeMilliseconds;
    }

    /**
     * {@inheritDoc}
     *
     * @since 2.1
     */
    @Override
    public CharSequence getCharacters() {
        return UtcTimestampConverter.convert(getValue(), showMilliseconds());
    }
}
