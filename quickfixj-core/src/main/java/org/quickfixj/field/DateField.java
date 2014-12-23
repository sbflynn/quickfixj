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
public abstract class DateField extends AbstractField<Date> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private final long value;

    protected DateField(Date value) {
        this(value.getTime());
    }

    protected DateField(long value) {
        this.value = value;
    }

    @Override
    public Date getValue() {
        return new Date(value);
    }
}
