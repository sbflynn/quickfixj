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

import java.math.BigDecimal;

/**
 * A double-values message field.
 */
public abstract class BigDecimalField extends AbstractField<BigDecimal> {

    /**
     * The serialVersionUID property.
     */
    private static final long serialVersionUID = 1L;

    private final BigDecimal value;

    private final int padding;

    protected BigDecimalField(BigDecimal value) {
        this(value, 0);
    }

    protected BigDecimalField(BigDecimal value, int padding) {
        this.value = value;
        this.padding = padding;
    }

    protected BigDecimalField(char[] value, int offset, int count) {

        this(new BigDecimal(value, offset, count), 0);
    }

    @Override
    public BigDecimal getValue() {
        return value;
    }

    public int getPadding() {
        return padding;
    }
}
